# Plan — Cycle 24 (correctness, Rotation 2)

## Finding: F024 — `initEdit` clamps Completed events' start time to 5:00 AM, silently corrupting early-morning events on save

## Problem

`RecordingViewModel.initEdit()` has THREE branches for loading an event into the editor:

| Branch | Condition | coerceIn bound |
|---|---|---|
| NotStarted | `endTime == null && status == NotStarted` | `coerceIn(0, 1440)` ✅ |
| InProgress | `endTime == null && status == InProgress` | `coerceIn(0, 1440)` ✅ |
| Completed (or any with endTime) | fallthrough | `coerceIn(300, 1440)` ❌ |

The Completed branch clamps start minutes to `[300, 1440]` = `[5:00 AM, 24:00]`. Any Completed event with `startTime` before 5:00 AM gets its start minutes clamped to 300 (5:00 AM). When the user saves — even if they only changed the title — `save()` calls `minutesToTimestamp(y, m, d, _startMinutes.value)`, which uses the clamped value, silently corrupting the event's `startTime` from e.g. 00:30 to 05:00.

The slider's `ABS_MIN = 300` (`TimeRangeSlider.kt:29`) is the root constraint: the slider cannot represent times before 5:00 AM. The `coerceIn(300, 1440)` in `initEdit` is a defensive clamp to keep the slider within its valid range, but it has the side effect of data corruption on save.

## How early-morning Completed events arise (bug is reachable)

1. **`quickAddEvent`** (`TimelineViewModel.kt:83`): sets `startTime = System.currentTimeMillis()` — any hour.
2. **`markInProgress`** (`TimelineViewModel.kt:98`): sets `startTime = System.currentTimeMillis()` — any hour. Then `markCompleted` sets `endTime = now`, `status = Completed` — startTime untouched (still any hour).
3. **`initEdit` → save with duration>0 and end in past**: A NotStarted event created at 00:30 can be edited (first branch, `coerceIn(0, 1440)` preserves 00:30), given a duration, and saved as Completed — `startTime` = 00:30, `status` = Completed. Re-editing this event hits the third branch, clamping 00:30 → 05:00.

## Evidence

- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt:88` — `coerceIn(300, 1440)` clamps start minutes for Completed events (the bug)
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt:72,80` — NotStarted/InProgress branches use `coerceIn(0, 1440)` (correct, shows the inconsistency)
- `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt:29` — `ABS_MIN = 300` (slider can't represent < 5:00 AM)
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt:83,98` — `quickAddEvent`/`markInProgress` set `startTime = now` (any hour, creating pre-5AM events)
- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt:46` — `createEvent` stores whatever `startTime` is passed

## Fix (3 files)

### 1. `TimeRangeSlider.kt` — widen slider range to allow 0:00–24:00

```diff
-private const val ABS_MIN = 300      // 5:00
+private const val ABS_MIN = 0        // 0:00 (allow early-morning events)
```

This lets the slider represent any time from midnight to midnight. The `StartTimeBar` window logic (`(value - WINDOW_HALF).coerceAtLeast(ABS_MIN)` etc.) already handles any `ABS_MIN` correctly — with `ABS_MIN=0`, a `value=30` (00:30) gives `windowStart=max(-150, 0)=0`, `windowEnd=min(210, 1440)=210` — valid 3-hour window. Previously with `ABS_MIN=300`, a `value=30` gave `windowStart=300`, `windowEnd=210` — **inverted window** (start > end), breaking the slider.

### 2. `RecordingViewModel.kt` — fix Completed branch to match other branches

```diff
-        val start = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(300, 1440)
+        val start = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(0, 1440)
```

Line 88 only. The `initNew` function (line 55) keeps `coerceIn(300, 1440)` — that's a default-value UX choice for new events (default to ≥ 5:00 AM), not a data-corruption path, so it stays unchanged.

### 3. `RecordingViewModelTest.kt` — regression test

```kotlin
@Test
fun save_editingCompletedEventBeforeFiveAM_preservesOriginalStartTime() = runTest {
    // A completed event that started at 00:30 (before the old ABS_MIN=300=5:00 AM).
    // Without the fix, initEdit clamps start to 300 (5:00 AM), and save() silently
    // corrupts the start time from 00:30 to 05:00.
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 30)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startTime = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 1)
    cal.set(Calendar.MINUTE, 0)
    val endTime = cal.timeInMillis

    val id = eventRepo.createEvent(
        title = "深夜事件",
        startTime = startTime,
        endTime = endTime,
        note = null
    )
    val event = eventRepo.getEventById(id)!!
    assertThat(event.status).isEqualTo(EventStatus.Completed.value)

    // Edit only the title — don't touch the slider.
    vm.initEdit(event)
    // initEdit must preserve 00:30 (30 minutes), NOT clamp to 300 (5:00 AM)
    assertThat(vm.startMinutes.value).isEqualTo(30)
    vm.onTitleChange("深夜事件改名")
    val today = Calendar.getInstance(TimeZone.getDefault()).let {
        Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
    }
    val ok = vm.save(today)
    assertThat(ok).isTrue()

    val saved = eventRepo.getEventById(id)!!
    val savedCal = Calendar.getInstance(TimeZone.getDefault())
    savedCal.timeInMillis = saved.startTime
    // The start time must NOT be silently clamped to 5:00 AM.
    assertThat(savedCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
    assertThat(savedCal.get(Calendar.MINUTE)).isEqualTo(30)
    assertThat(saved.title).isEqualTo("深夜事件改名")
}
```

The test FAILS on current code (`startMinutes` = 300, saved HOUR_OF_DAY = 5) and PASSES after fix (`startMinutes` = 30, saved HOUR_OF_DAY = 0).

## In-scope files (3)

1. `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` — line 88: `coerceIn(300, 1440)` → `coerceIn(0, 1440)`
2. `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` — line 29: `ABS_MIN = 300` → `ABS_MIN = 0`
3. `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt` — add regression test

## Verification

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew assembleDebug
./gradlew :app:assembleRelease
```

## Why this is different from F006 / F014

- **F006** (cycle 6): `save()` duration==0 + originalStatus==Completed → illegal Completed+endTime=null. Fix: downgrade to InProgress.
- **F014** (cycle 14): `save()` start>now + duration>0 → illegal InProgress for future events. Fix: mark NotStarted.
- **F024** (this): `initEdit` clamps Completed events' start minutes to [300, 1440], silently corrupting pre-5AM start times on save. Fix: widen coerceIn to [0, 1440] + slider ABS_MIN to 0.

All three are in `RecordingViewModel`, but they are distinct correctness issues in different code paths (save status derivation, save future-start check, initEdit start-time clamping).

## Directories audited

- `feature/recording/` — RecordingViewModel, TimeRangeSlider, RecordingSheet
- `feature/heatmap/` — TimeAllocationCalculator, HeatmapCalculator
- `feature/search/` — SearchViewModel
- `feature/timeline/` — TimelineViewModel
- `data/repository/` — EventRepository, NoteRepository
- `data/export/` — DataExportManager, DataImportManager
- `data/local/` — EventDao, EventEntity

## Directories NOT audited

- `feature/notes/` — NotesScreen, NoteEditorSheet, NotesViewModel
- `feature/settings/` — SettingsScreen, AboutScreen, ExportViewModel, ImportViewModel
- `feature/timeviz/` — TimeVizScreen, TimeVizViewModel, TimeVizCalculator, TimeVizPrefs
- `ui/theme/`, `ui/debug/`
- `navigation/`
- `MainActivity.kt`, `ShiJiBenApplication.kt`
