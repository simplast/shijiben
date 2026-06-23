# Plan 001: Remove destructive migration, export Room schema

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat e850768..HEAD -- app/src/main/java/com/shijiben/data/DataModule.kt app/src/main/java/com/shijiben/data/local/AppDatabase.kt app/build.gradle.kts`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug (data durability)
- **Planned at**: commit `e850768`, 2026-06-23

## Why this matters

This app's entire value proposition is long-term personal time records ("一段
长期的记录会让我们自然的意识到问题在哪儿"). The production Room database is
configured with `fallbackToDestructiveMigration()`, which means the **first
schema change silently destroys every event, note, and tag the user has ever
recorded**. For this product that is the worst-case bug — the opposite of the
"记录即审视" philosophy. Additionally `exportSchema = false` means no schema
JSON is generated, so future migrations cannot be authored or verified against a
baseline. This plan removes the footgun now (while the DB is still version 1 and
no migration is needed) and turns on schema export so the next schema bump is
forced to go through a real migration.

## Current state

- `app/src/main/java/com/shijiben/data/DataModule.kt` — Hilt module providing
  the Room database. Line 23 calls `.fallbackToDestructiveMigration()`:

```kotlin
// DataModule.kt (lines 19-24)
@Provides
@Singleton
fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
    Room.databaseBuilder(ctx, AppDatabase::class.java, "shijiben.db")
        .fallbackToDestructiveMigration()
        .build()
```

- `app/src/main/java/com/shijiben/data/local/AppDatabase.kt` — Room database
  definition. `exportSchema = false` on line 15:

```kotlin
// AppDatabase.kt (lines 12-17)
@Database(
    entities = [EventEntity::class, NoteEntity::class, TagEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
}
```

- `app/build.gradle.kts` — declares the KSP room compiler plugin (line 6) and
  the room-compiler dependency (line 70), but has **no `ksp { }` block** to
  configure schema location. Relevant excerpt:

```kotlin
// app/build.gradle.kts (lines 1-7)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}
```

```kotlin
// app/build.gradle.kts (lines 68-70)
implementation("androidx.room:room-runtime:${rootProject.extra["room"]}")
implementation("androidx.room:room-ktx:${rootProject.extra["room"]}")
ksp("androidx.room:room-compiler:${rootProject.extra["room"]}")
```

### Repo conventions to honor

- Kotlin DSL Gradle (`*.gradle.kts`); match existing indentation (4 spaces).
- The DB version stays `1` in this plan — do NOT bump it. There is no schema
  change here; we are only removing the destructive fallback and enabling
  schema export so the *next* change is safe.
- Schema JSON files are committed to the repo (standard Room practice) so
  migrations can be authored and tested. Do NOT add `schemas/` to `.gitignore`.

## Commands you will need

| Purpose   | Command                          | Expected on success |
|-----------|----------------------------------|---------------------|
| Build     | `./gradlew assembleDebug`        | exit 0, BUILD SUCCESSFUL |
| Tests     | `./gradlew testDebugUnitTest`    | exit 0, all tests pass |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/com/shijiben/data/DataModule.kt`
- `app/src/main/java/com/shijiben/data/local/AppDatabase.kt`
- `app/build.gradle.kts`

**Out of scope** (do NOT touch):
- Any Entity/DAO/Repository file — no schema change is being made.
- `app/src/test/...` — tests already use `inMemoryDatabaseBuilder` and are
  unaffected.
- Do NOT add a `Migration` class — version is not changing. Writing a migration
  for a non-bump is meaningless and will confuse future readers.
- Do NOT bump `version = 1` in `@Database`.

## Git workflow

- Branch: `advisor/001-remove-destructive-migration`
- Commit style (match repo `git log`): `fix(data): remove destructive migration, export Room schema`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Configure KSP to export the Room schema JSON

In `app/build.gradle.kts`, add a top-level `ksp { }` block immediately after the
`android { }` block (before `dependencies { }`). This tells the Room compiler
where to write the schema JSON.

Add exactly:

```kotlin
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}
```

Place it between the closing `}` of `android { ... }` (line 45) and the
`dependencies {` line (line 47).

**Verify**: `./gradlew tasks` → exit 0 (sanity that the script still parses).
Then `grep -n "room.schemaLocation" app/build.gradle.kts` → one match.

### Step 2: Enable schema export in the Database annotation

In `app/src/main/java/com/shijiben/data/local/AppDatabase.kt`, change
`exportSchema = false` to `exportSchema = true` on line 15.

The `@Database` annotation must read:

```kotlin
@Database(
    entities = [EventEntity::class, NoteEntity::class, TagEntity::class],
    version = 1,
    exportSchema = true
)
```

Do not change `version` — it stays `1`.

**Verify**: `grep -n "exportSchema" app/src/main/java/com/shijiben/data/local/AppDatabase.kt`
→ `15:    exportSchema = true`.

### Step 3: Remove the destructive migration fallback

In `app/src/main/java/com/shijiben/data/DataModule.kt`, delete the
`.fallbackToDestructiveMigration()` line so the builder reads:

```kotlin
@Provides
@Singleton
fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
    Room.databaseBuilder(ctx, AppDatabase::class.java, "shijiben.db")
        .build()
```

**Verify**: `grep -rn "fallbackToDestructiveMigration" app/src/` → no matches.

### Step 4: Build to generate the schema JSON and confirm compilation

Run the debug build. The KSP room compiler will generate
`app/schemas/com.shijiben.data.local.AppDatabase/1.json`.

**Verify**: `./gradlew assembleDebug` → exit 0, `BUILD SUCCESSFUL`.
Then confirm the schema file exists:
`ls app/schemas/com.shijiben.data.local.AppDatabase/1.json` → the file is listed.

### Step 5: Run the test suite

The existing tests use `Room.inMemoryDatabaseBuilder` (fresh DB, no migration
path), so they must still pass.

**Verify**: `./gradlew testDebugUnitTest` → exit 0, all 8 existing tests pass.

## Test plan

No new tests are required for this plan — there is no behavior change to assert
at runtime (the DB is still version 1). The protection this plan provides is
static: a future `version = 2` without a `Migration` will now crash at runtime
instead of silently wiping data, which is the desired fail-loud behavior.

The existing `EventRepositoryTest` (8 tests) serves as the regression gate that
the DB still initializes and CRUD works.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -rn "fallbackToDestructiveMigration" app/src/` returns no matches
- [ ] `grep -n "exportSchema = true" app/src/main/java/com/shijiben/data/local/AppDatabase.kt` returns one match
- [ ] `grep -n "room.schemaLocation" app/build.gradle.kts` returns one match
- [ ] `ls app/schemas/com.shijiben.data.local.AppDatabase/1.json` succeeds
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `./gradlew testDebugUnitTest` exits 0 (all 8 existing tests pass)
- [ ] No files outside the in-scope list are modified (`git status` shows only
      the 3 in-scope files + the generated `app/schemas/.../1.json`)
- [ ] `plans/README.md` status row for 001 updated to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `DataModule.kt:19-24`, `AppDatabase.kt:12-17`, or
  `app/build.gradle.kts:1-7,40-45,68-70` doesn't match the excerpts above
  (the codebase has drifted since this plan was written).
- `./gradlew assembleDebug` fails with a KSP/Room error after adding the
  `ksp { }` block — the schema-location arg syntax may differ by KSP version;
  report the exact error rather than guessing arg names.
- The schema JSON is NOT generated at the expected path after a successful
  build — the `room.schemaLocation` value may need a different resolution;
  report the actual `app/schemas/` tree.
- A step's verification fails twice after a reasonable fix attempt.

## Maintenance notes

- **Future schema changes**: when any Entity field is added/removed/renamed or a
  new Entity is introduced, you MUST (a) bump `version` in `@Database`, (b)
  write a `Migration` from the old to new version, (c) add it via
  `.addMigrations(migration)` in `DataModule.provideAppDatabase`, and (d) commit
  the newly-generated `app/schemas/.../<new-version>.json`. The schema JSON
  files in `app/schemas/` are the source of truth for migration testing.
- **Reviewer focus**: confirm `version` is still `1` (no accidental bump) and
  that no `Migration` class was added prematurely.
- **Follow-up deferred**: a migration test framework (`MigrationTestHelper`)
  can be added when the first real migration is written; not needed yet.
