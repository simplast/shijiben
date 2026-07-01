# Plan — Cycle 25 (security, Rotation 2)

## Finding F025 — Import file DoS: unbounded `readBytes()` + unbounded array length

### Problem

`DataImportManager` accepts user-supplied JSON via SAF (any file the user picks, including a maliciously-crafted one shared to them). Two unbounded resource paths lead to OOM / app crash:

1. **`readFromStream` (line 140-141)**: `input.use { it.readBytes().toString(Charsets.UTF_8) }` reads the *entire* stream into a `ByteArray` with no size limit. A 500 MB JSON file → ~500 MB heap allocation → OOM crash. `InputStream.available()` is unreliable (returns 0 for SAF-backed streams), so callers cannot pre-check.

2. **`parseEvents` (line 70) / `parseNotes` (line 96)**: iterate `0 until arr.length()` with no upper bound. A crafted JSON like `{"schemaVersion":1,"events":[10000000 × {...}]}` builds 10M `EventEntity` data objects in memory before any DB write → OOM. Even a 50 MB JSON of tiny event objects can expand to hundreds of MB of parsed objects + JSON parser internal buffers.

### Impact

- **M** (denial-of-service / app crash). Triggered by opening a malicious `.json` file via the Settings → Import flow. No data exfiltration, no persistence — the app simply crashes. Since the file is user-picked, the realistic vector is a user being sent a "backup file" by an attacker or downloading a corrupt/huge file. Lower severity than exfiltration, but it is a real untrusted-input boundary with zero validation today.

### Why different from F008 / F015

- **F008** validated the *value* of `status` (enum range).
- **F015** disabled `allowBackup` (cloud/adb backup leakage).
- **F025** validates the *size* of the input (byte length + array cardinality) — a different trust-boundary dimension (resource exhaustion, not value validity or backup config).

### Fix (surgical, 2 in-scope files)

**File 1: `app/src/main/java/com/shijiben/data/export/DataImportManager.kt`**

Add three constants near the top of the `object`:

```kotlin
/** 导入文件大小上限：50 MB（远超任何合理备份，足以挡住 OOM 攻击）。 */
private const val MAX_IMPORT_BYTES = 50L * 1024 * 1024

/** 单个数组（events / notes）条目上限：10 万条（远超日常使用，挡住数组 DoS）。 */
private const val MAX_ARRAY_ENTRIES = 100_000
```

Rewrite `readFromStream` to read in 8 KB chunks, aborting if total exceeds `MAX_IMPORT_BYTES`:

```kotlin
/** 薄 IO 包装：UTF-8 读取流并关闭。超过 [MAX_IMPORT_BYTES] 抛 IllegalArgumentException 由 VM catch。 */
fun readFromStream(input: InputStream): String {
    return input.use { stream ->
        val out = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(8 * 1024)
        var total = 0L
        while (true) {
            val read = stream.read(chunk)
            if (read == -1) break
            total += read
            if (total > MAX_IMPORT_BYTES) {
                throw IllegalArgumentException("导入文件过大（>$MAX_IMPORT_BYTES 字节）")
            }
            out.write(chunk, 0, read)
        }
        out.toString(Charsets.UTF_8.name())
    }
}
```

Add a length guard at the top of `parseEvents` and `parseNotes` (right after the `if (arr == null) return emptyList()` line):

```kotlin
require(arr.length() <= MAX_ARRAY_ENTRIES) {
    "数组条目过多（>${MAX_ARRAY_ENTRIES}）：${arr.length()}"
}
```

`IllegalArgumentException` is already caught by `ImportViewModel.import`'s `catch (e: Exception)` → user sees "导入失败，请重试" Error state. No UI change needed.

**File 2: `app/src/test/java/com/shijiben/data/export/DataImportManagerTest.kt`**

Add 3 regression tests:

1. `readFromStream_exceedsMaxBytes_throwsIllegalArgumentException` — feed a stream of `MAX_IMPORT_BYTES + 1` zero bytes, expect IllegalArgumentException.

2. `parseJsonString_tooManyEvents_throwsIllegalArgumentException` — build a JSON with `MAX_ARRAY_ENTRIES + 1` minimal events (use raw JSONObject construction, not `buildJsonString` to avoid huge memory in test), expect IllegalArgumentException.

3. `parseJsonString_tooManyNotes_throwsIllegalArgumentException` — same for notes.

### Steps (max 8)

1. Read `DataImportManager.kt` (DONE during plan)
2. Read `DataImportManagerTest.kt` (DONE during plan)
3. Add `MAX_IMPORT_BYTES`, `MAX_ARRAY_ENTRIES` constants + rewrite `readFromStream` + add `require(arr.length() ...)` in `parseEvents`/`parseNotes` in `DataImportManager.kt`
4. Add 3 regression tests in `DataImportManagerTest.kt`
5. Run `./gradlew :app:compileDebugKotlin`
6. Run `./gradlew :app:testDebugUnitTest --rerun-tasks` (filter to `DataImportManagerTest` if full suite is slow)
7. Run `./gradlew assembleDebug` + `:app:assembleRelease` (verification gate)
8. Report

### In-scope files (2, ≤5)

- `app/src/main/java/com/shijiben/data/export/DataImportManager.kt`
- `app/src/test/java/com/shijiben/data/export/DataImportManagerTest.kt`

### Files cited from different directories (audit evidence, 5)

- `app/src/main/java/com/shijiben/data/export/DataImportManager.kt` — vulnerability site (data/export/)
- `app/src/main/java/com/shijiben/feature/settings/ImportViewModel.kt` — caller, confirms `catch (e: Exception)` handles new IAE (feature/settings/)
- `app/src/main/AndroidManifest.xml` — audited, only MainActivity exported (app/src/main/)
- `app/src/main/java/com/shijiben/ui/debug/DebugOverlay.kt` — audited, properly gated with `BuildConfig.DEBUG` (ui/debug/)
- `app/build.gradle.kts` — audited, R8 + shrinkResources enabled for release (app/)

### Directories NOT audited (out of scope for this cycle)

- `app/src/main/java/com/shijiben/data/local/` — Room DB migration safety (would need separate analysis of `AppDatabase` migration paths; not in scope for this finding)
- `app/src/main/java/com/shijiben/data/repository/` — Repository layer; trusted internal, no external input surface
- `app/src/main/java/com/shijiben/data/model/` — pure model enums
- `app/src/main/java/com/shijiben/feature/heatmap/`, `feature/notes/`, `feature/recording/`, `feature/search/`, `feature/timeline/`, `feature/timeviz/` — UI features; consume Repository data, no direct external input
- `app/src/main/java/com/shijiben/ui/theme/` — theme palette, no security surface
- `app/src/main/java/com/shijiben/ui/debug/` — audited (DebugOverlay/DebugLog), no issues
- `app/src/main/java/com/shijiben/navigation/` — internal nav graph
- `app/src/main/java/com/shijiben/di/` — Hilt modules
- `app/src/main/java/com/shijiben/MainActivity.kt`, `ShiJiBenApplication.kt` — entry points; no exported components beyond launcher
- `app/src/test/` (other test files) — not relevant to runtime security

### Risk

- **Behavior change**: only for inputs > 50 MB or arrays > 100K. Realistic backups (a personal year of events: ~1000 events × 200 bytes = ~200 KB) are ~250× under the cap. Zero impact on legitimate use.
- **Error UX**: oversized files surface as the existing "导入失败，请重试" toast — no new UX code needed.
- **Test impact**: existing tests use tiny JSON, all pass unchanged.
