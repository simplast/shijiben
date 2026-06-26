# Plan 002: Fix duration slider cap inconsistency (3h UI vs 8h ViewModel)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: none
- **Category**: bug (data integrity + UI)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

The duration slider caps at 3 hours (`TimeRangeSlider.kt:33` `DUR_MAX = 180`), but `RecordingViewModel.initEdit` coerces loaded durations to `coerceIn(0, 480)` (8 hours, `RecordingViewModel.kt:88`). Events routinely exceed 3h: `TimelineViewModel.markCompleted` sets `endTime = now`, so a 5-hour session produces a 5h event. When the user edits such an event: the "时长" badge displays the true value (e.g. "5小时"), but the slider thumb is clamped to the 3h position. If the user drags the slider at all, `_durationMinutes` snaps to ≤180 and saving silently truncates the event to 3h — **data loss**. Even without dragging, the badge (5h) and slider position (3h) contradict each other. This plan makes the slider's max adapt to the value being edited so the UI is always truthful and no truncation occurs.

## Current state

Files in scope:

- `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` — the slider UI; `DUR_MAX` is a hardcoded constant.
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` — loads duration in `initEdit` with an 8h ceiling.
- `app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` — calls `TimeRangeSlider(...)`; passes `durationMinutes` through.

Key excerpts (confirm these match live code):

`TimeRangeSlider.kt:32-37` — hardcoded caps:
```kotlin
// ==================== 持续时间参数 ====================
private const val DUR_MAX = 180      // 3 小时

// 方块大小
private val BLOCK_DP = 8.dp
```

`TimeRangeSlider.kt:47-109` — `TimeRangeSlider` signature (no max parameter):
```kotlin
@Composable
fun TimeRangeSlider(
    startMinutes: Int,
    durationMinutes: Int,
    onStartChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        ...
        DurationBar(
            value = durationMinutes,
            onValueChange = onDurationChange
        )
    }
}
```

`TimeRangeSlider.kt:141-153` — `DurationBar` uses `DUR_MAX` directly and has fixed labels:
```kotlin
@Composable
private fun DurationBar(value: Int, onValueChange: (Int) -> Unit) {
    val labels = listOf("0", "30分", "1h", "1h30", "2h", "2h30", "3h")
    Column {
        PixelTrackWithCursor(
            value = value.coerceIn(0, DUR_MAX),
            valueMin = 0,
            valueMax = DUR_MAX,
            onValueChange = { onValueChange(it.coerceIn(0, DUR_MAX)) }
        )
        BarLabels(labels)
    }
}
```

`RecordingViewModel.kt:84-91` — duration load with 480-min ceiling:
```kotlin
_durationMinutes.value = if (event.endTime != null) {
    val cal2 = Calendar.getInstance(TimeZone.getDefault())
    cal2.timeInMillis = event.endTime
    val end = cal2.get(Calendar.HOUR_OF_DAY) * 60 + cal2.get(Calendar.MINUTE)
    (end - start).coerceIn(0, 480)
} else {
    60
}
```

`RecordingSheet.kt:78-84` — the call site:
```kotlin
TimeRangeSlider(
    startMinutes = startMin,
    durationMinutes = durationMin,
    onStartChange = viewModel::onStartChange,
    onDurationChange = viewModel::onDurationChange,
    modifier = Modifier.fillMaxWidth()
)
```

`RecordingViewModel.kt:32-36` — default for *new* events:
```kotlin
private val _durationMinutes = MutableStateFlow(10) // 10 分钟默认
val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()
```

### Design context (must honor)

`docs/2026-06-22-shijiben-design.md:147-148` specifies the *new-event* duration selector as "20分-3小时". This plan **preserves the 3h cap for new events** and only widens it when editing an existing event whose actual duration exceeds 3h. This keeps the new-event UX as designed while fixing the edit-data-loss bug.

### Repo conventions

- Constants at file top in `// ===== comments` blocks (see `TimeRangeSlider.kt:23-37`). Match this style.
- Chinese inline strings (e.g. `"30分"`, `"1h30"` at `TimeRangeSlider.kt:143`). Keep inline.
- `formatDuration` at `TimeRangeSlider.kt:320-328` already formats any minute count correctly — reuse, don't duplicate.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0, all pass |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt`
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`
- `app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt`

**Out of scope**:
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModelTest.kt` — only update if an existing test asserts the old 480 ceiling; otherwise leave.
- Start-time slider (`StartTimeBar`) — its window logic is separate; do not change.
- The `SliderRainbowActive`/`Inactive` color lists — unchanged.

## Git workflow

- Branch: `advisor/002-duration-cap`
- Commit message example: `Fix duration slider truncating long events on edit`.

## Steps

### Step 1: Add a `durationMaxMinutes` parameter to `TimeRangeSlider` and `DurationBar`

In `TimeRangeSlider.kt`, change the public signature to accept an explicit max (default keeps new-event behavior identical):

```kotlin
@Composable
fun TimeRangeSlider(
    startMinutes: Int,
    durationMinutes: Int,
    onStartChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    durationMaxMinutes: Int = DUR_MAX_DEFAULT   // 默认 180（新建场景）
)
```

Add a renamed constant at the top (keep `DUR_MAX` as the default alias to minimize churn, or rename — pick one and be consistent):
```kotlin
private const val DUR_MAX_DEFAULT = 180      // 3 小时（新建事件默认上限）
private const val DUR_HARD_CEILING = 480     // 8 小时硬上限（防止极端值）
```
Remove the old `private const val DUR_MAX = 180` line if you renamed it; otherwise keep `DUR_MAX` and use it as the default. **Pick the rename approach** (`DUR_MAX_DEFAULT` + `DUR_HARD_CEILING`) for clarity.

Pass the max into `DurationBar`:
```kotlin
DurationBar(
    value = durationMinutes,
    onValueChange = onDurationChange,
    maxMinutes = durationMaxMinutes
)
```

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0 (will fail until Step 2 updates `DurationBar` — that's expected; complete Step 2 then re-run).

### Step 2: Make `DurationBar` use the passed max and generate labels dynamically

Replace `DurationBar` (lines 141-153) with:

```kotlin
@Composable
private fun DurationBar(
    value: Int,
    onValueChange: (Int) -> Unit,
    maxMinutes: Int
) {
    val effectiveMax = maxMinutes.coerceIn(1, DUR_HARD_CEILING)
    val labels = remember(effectiveMax) { buildDurationLabels(effectiveMax) }
    Column {
        PixelTrackWithCursor(
            value = value.coerceIn(0, effectiveMax),
            valueMin = 0,
            valueMax = effectiveMax,
            onValueChange = { onValueChange(it.coerceIn(0, effectiveMax)) }
        )
        BarLabels(labels)
    }
}

/** 生成 0..max 的整点小时标签（每 60 分钟一个，含两端）。 */
private fun buildDurationLabels(maxMinutes: Int): List<String> {
    val hours = maxMinutes / 60
    return buildList {
        add("0")
        for (h in 1..hours) add("${h}h")
    }
}
```

Rationale for label generation: every full hour up to the max, including both endpoints. For 180 → `["0","1h","2h","3h"]`; for 300 → `["0","1h","2h","3h","4h","5h"]`. This is truthful and aligns to real positions (the label-alignment issue is addressed separately in plan 009; here we only fix the cap, labels remain `SpaceBetween`).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 3: Compute the edit-time max in `RecordingViewModel.initEdit`

In `RecordingViewModel.kt`, add a state field for the duration max so the sheet can read it. After `_durationMinutes` (line 36) add:

```kotlin
private val _durationMax = MutableStateFlow(DUR_MAX_DEFAULT)
val durationMax: StateFlow<Int> = _durationMax.asStateFlow()
```

Add a companion/constant import at the top of the file. Since `DUR_MAX_DEFAULT` is `private` in `TimeRangeSlider.kt`, **do not** reference it cross-file. Instead define a parallel constant in the ViewModel file:
```kotlin
private const val NEW_EVENT_DURATION_MAX = 180   // 与 TimeRangeSlider 默认一致
private const val DURATION_HARD_CEILING = 480
```

In `initEdit` (lines 84-91), set `_durationMax` based on the loaded duration:
```kotlin
val rawDuration = if (event.endTime != null) {
    val cal2 = Calendar.getInstance(TimeZone.getDefault())
    cal2.timeInMillis = event.endTime
    val end = cal2.get(Calendar.HOUR_OF_DAY) * 60 + cal2.get(Calendar.MINUTE)
    (end - start).coerceAtLeast(0)
} else {
    60
}
_durationMinutes.value = rawDuration.coerceIn(0, DURATION_HARD_CEILING)
// 编辑时上限 = max(默认 3h, 实际时长向上取整到整点)，保证滑块能表示当前值
_durationMax.value = maxOf(NEW_EVENT_DURATION_MAX, ((rawDuration + 59) / 60) * 60)
        .coerceAtMost(DURATION_HARD_CEILING)
```

In `initNew` (lines 53-63), reset the max to the default:
```kotlin
_durationMax.value = NEW_EVENT_DURATION_MAX
```
(Add this line inside `initNew` alongside the existing resets of `_title`/`_note`/etc.)

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 4: Pass `durationMax` from `RecordingSheet` into `TimeRangeSlider`

In `RecordingSheet.kt`, collect the new state (after line 50 where `durationMin` is collected):
```kotlin
val durationMax by viewModel.durationMax.collectAsStateWithLifecycle()
```

Update the `TimeRangeSlider(...)` call (lines 78-84):
```kotlin
TimeRangeSlider(
    startMinutes = startMin,
    durationMinutes = durationMin,
    onStartChange = viewModel::onStartChange,
    onDurationChange = viewModel::onDurationChange,
    modifier = Modifier.fillMaxWidth(),
    durationMaxMinutes = durationMax
)
```

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 5: Run the full verification suite

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

## Test plan

Existing tests to keep green:
- `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt` — if it asserts `_durationMinutes` after `initEdit`, confirm the asserted value still holds (the `coerceIn(0,480)` is preserved as `coerceIn(0, DURATION_HARD_CEILING)`; 480 unchanged). If a test asserted the max was exactly 480, update it to assert `durationMax` is computed per the new rule. Model any new assertion on the existing test style in that file.

New case to add (in `RecordingViewModelTest.kt`, mirroring an existing `initEdit` test):
- Given an event with start 09:00 and end 14:00 (5h), after `initEdit`: `durationMinutes.value == 300` AND `durationMax.value == 300` (since 300 > 180, rounded up to 300). This is the regression test for the data-loss bug.
- Given a new event (`initNew`): `durationMax.value == 180`.

Verification: `./gradlew :app:testDebugUnitTest --tests "*RecordingViewModelTest*"` → all pass.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0 (incl. new/updated duration-max tests)
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n "DUR_MAX = 180" app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` returns no matches (old constant gone)
- [ ] `grep -n "durationMaxMinutes" app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` returns the new parameter
- [ ] `grep -n "durationMax" app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` returns the new state + the initEdit/initNew assignments
- [ ] No files outside the in-scope list are modified (`git status`) — except test file if updated
- [ ] `plans/README.md` status row for 002 updated to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The excerpts in "Current state" don't match the live code (drift).
- `RecordingViewModelTest.kt` asserts something about duration that contradicts the new behavior and cannot be reconciled by a straightforward update (report the conflict rather than weakening the test).
- The design doc's "20分-3小时" new-event constraint (`docs/2026-06-22-shijiben-design.md:147`) is found to also apply to *editing* (i.e. the maintainer wants editing capped at 3h with a different UX). If so, STOP — the fix approach changes (would need to cap+warn rather than widen).
- `PixelTrackWithCursor`'s "传统模式" block-drawing loop (lines 261-277) produces visually broken output for `effectiveMax > 180` (it should not — it divides by `numBlocks` generically — but verify on device).

## Maintenance notes

- The new-event 3h default is now expressed in two places (`DUR_MAX_DEFAULT` in TimeRangeSlider, `NEW_EVENT_DURATION_MAX` in RecordingViewModel). Keep them numerically equal; if one changes, update both. A future cleanup could centralize in a shared constants file.
- If `markCompleted` ever changes to cap duration, revisit `_durationMax` — the ceiling assumption (events can be up to 8h) lives in `DURATION_HARD_CEILING`.
- Reviewer: on device, edit a >3h completed event, confirm the slider thumb sits at the true position and the badge matches; drag down then back up and confirm no truncation on save.
- Deferred: the label-position misalignment (labels are `SpaceBetween`, not at true tick positions) is plan 009. This plan only ensures the *set* of labels is correct for the max.
