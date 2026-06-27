# Plan 011: Fix `RecordingViewModel.initEdit` data corruption + un-skip the two red tests

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 9da5c72..HEAD -- app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt plans/README.md`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: MED
- **Depends on**: none
- **Category**: bug | tests
- **Planned at**: commit `9da5c72`, 2026-06-27

## Why this matters

This app's entire value is "记录时间去向" (record where your time goes). Two documented bugs in `RecordingViewModel.initEdit` silently rewrite the user's record of reality on the edit path:

1. **Editing an in-progress event converts it to completed.** An in-progress event (`endTime=null`, `status=InProgress`) falls through `initEdit` to a `rawDuration=60` fallback, so `save()` writes a non-null `endTime`, silently turning the still-running event into a scheduled/completed one.
2. **Editing a midnight-spanning completed event drops its end time.** `initEdit` computes `rawDuration` via minute-of-day arithmetic that goes negative when `end < start` (event spans midnight), clamps to 0, so `save()` writes `endTime=null`, erasing the event's duration.

Both bugs are pinned by committed tests in `RecordingViewModelTest.kt` that are currently red and explicitly excluded from the verification gate in `plans/README.md:51-53`. A gate with known-red tests is meaningless — the next real regression hides among the accepted failures. Fixing the bugs makes the tests green and lets us delete the exclusion, restoring a trustworthy CI signal. This is the highest-leverage change in the batch because it unblocks every future edit-path refactor.

## Current state

### `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`

Role: ViewModel backing `RecordingSheet`. Owns the title/start/duration/note state and the `initNew`/`initEdit`/`save` flow. Status is auto-derived in `save()`.

The buggy `initEdit` (lines 60-89):

```kotlin
fun initEdit(event: EventEntity) {
    editingId = event.id
    _title.value = event.title
    _note.value = event.note ?: ""
    val cal = Calendar.getInstance(TimeZone.getDefault())

    // 如果没有设置时间（NotStarted 且无 endTime），默认使用当前时间
    if (event.endTime == null && event.status == EventStatus.NotStarted.value) {
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        _startMinutes.value = ((nowMin / 15) * 15).coerceIn(0, 1440)
        _durationMinutes.value = 0
        return
    }

    cal.timeInMillis = event.startTime
    val start = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(300, 1440)
    _startMinutes.value = start
    val rawDuration = if (event.endTime != null) {
        val cal2 = Calendar.getInstance(TimeZone.getDefault())
        cal2.timeInMillis = event.endTime
        val end = cal2.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        (end - start).coerceAtLeast(0)   // ← BUG B: negative for midnight-spanning, clamps to 0
    } else {
        60                               // ← BUG A: in-progress events hit this, giving them a 60min duration
    }
    _durationMinutes.value = rawDuration.coerceIn(0, DURATION_HARD_CEILING)
    _durationMax.value = maxOf(NEW_EVENT_DURATION_MAX, ((rawDuration + 59) / 60) * 60).coerceAtMost(DURATION_HARD_CEILING)
}
```

The `save()` status computation (lines 95-137) — note the `duration == 0 → NotStarted` branch conflicts with preserving an in-progress event:

```kotlin
suspend fun save(viewingDate: Triple<Int, Int, Int>): Boolean {
    val title = _title.value.trim()
    if (title.isEmpty()) return false
    val (y, m, d) = viewingDate
    val start = minutesToTimestamp(y, m, d, _startMinutes.value)
    val duration = _durationMinutes.value
    val now = System.currentTimeMillis()

    val actualEnd: Long? = if (duration > 0) {
        minutesToTimestamp(y, m, d, _startMinutes.value + duration)
    } else {
        null
    }

    val status = when {
        duration == 0 -> EventStatus.NotStarted.value      // ← for in-progress edit, duration=0 ⇒ NotStarted (WRONG)
        actualEnd != null && now > actualEnd -> EventStatus.Completed.value
        else -> EventStatus.InProgress.value
    }
    // ... updateEvent / createEvent with status ...
}
```

Constants and field declarations (lines 16-39):

```kotlin
private const val NEW_EVENT_DURATION_MAX = 180   // 与 TimeRangeSlider 默认一致
private const val DURATION_HARD_CEILING = 480

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {
    private val _title = MutableStateFlow("")
    // ... _startMinutes (540), _durationMinutes (10), _durationMax (180), _note ...
    private var editingId: Long? = null
    fun onTitleChange(v: String) { _title.value = v }
    // ... onStartChange, onDurationChange ...
}
```

### `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt`

Role: Robolectric + Truth tests. The two red tests (lines 46-100):

```kotlin
@Test
fun save_editingInProgressEvent_preservesNullEndTimeAndStatus() = runTest {
    val now = System.currentTimeMillis()
    val id = eventRepo.createEvent(title = "原标题", startTime = now - 60_000, endTime = null, note = null)
    val event = eventRepo.getEventById(id)!!
    assertThat(event.status).isEqualTo(EventStatus.InProgress.value)
    assertThat(event.endTime).isNull()

    vm.initEdit(event)
    vm.onTitleChange("新标题")
    val today = Calendar.getInstance(TimeZone.getDefault()).let { Triple(it.get(YEAR), it.get(MONTH)+1, it.get(DAY_OF_MONTH)) }
    val ok = vm.save(today)
    assertThat(ok).isTrue()

    val saved = eventRepo.getEventById(id)!!
    assertThat(saved.title).isEqualTo("新标题")
    assertThat(saved.status).isEqualTo(EventStatus.InProgress.value)
    assertThat(saved.endTime).isNull()
}

@Test
fun save_editingCompletedEvent_keepsEndTime() = runTest {
    val now = System.currentTimeMillis()
    val id = eventRepo.createEvent(title = "已完成", startTime = now - 7200_000, endTime = now - 3600_000, note = null)
    val event = eventRepo.getEventById(id)!!
    assertThat(event.status).isEqualTo(EventStatus.Completed.value)

    vm.initEdit(event)
    vm.onTitleChange("改标题")
    val today = /* same as above */
    val ok = vm.save(today)
    assertThat(ok).isTrue()

    val saved = eventRepo.getEventById(id)!!
    assertThat(saved.title).isEqualTo("改标题")
    assertThat(saved.endTime).isNotNull()  // completed events keep their end time
}
```

The other three tests in the file (`save_newEvent_hasNonNullEndTime`, `initEdit_fiveHourEvent_setsDurationAndMaxTo300`, `initNew_setsDurationMaxTo180`) currently pass and MUST stay green.

### `plans/README.md`

Lines 49-53 contain the exclusion note to delete once the tests pass:

```markdown
- **Two pre-existing baseline test failures excluded from the gate** (both in `RecordingViewModelTest`, ...):
  1. `save_editingInProgressEvent_preservesNullEndTimeAndStatus` — always-failing bug in `RecordingViewModel.initEdit` ...
  2. `save_editingCompletedEvent_keepsEndTime` — time-of-day flaky bug ...
```

### Repo conventions to match

- **Status enum**: `com.shijiben.data.model.EventStatus` with `NotStarted` / `InProgress` / `Completed` (Int `.value` = 0/1/2). See `EventRepository.determineStatus` at `EventRepository.kt:131-138` for the canonical status rules.
- **Test style**: Robolectric `@RunWith(RobolectricTestRunner)`, `@Config(sdk=[33])`, `MainCoroutineRule`, `runTest`, Truth `assertThat`. Model after the existing tests in `RecordingViewModelTest.kt`.
- **No logger / no Result pattern** — direct returns, exceptions propagate. Don't introduce either.
- **Kotlin style**: `private val` for state, `_mutable`/`public` exposure pattern, `Calendar.getInstance(TimeZone.getDefault())` for time math.

## Commands you will need

| Purpose   | Command                                                                                  | Expected on success |
|-----------|------------------------------------------------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`                                                     | exit 0, no errors   |
| Tests     | `./gradlew :app:testDebugUnitTest -- --tests "com.shijiben.feature.recording.RecordingViewModelTest"` | all 5 tests pass    |
| Full test | `./gradlew :app:testDebugUnitTest`                                                      | exit 0              |
| Build     | `./gradlew assembleDebug`                                                                | exit 0              |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`
- `plans/README.md` (delete the exclusion paragraph at lines 49-53 only)

**Out of scope** (do NOT touch, even though they look related):
- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` — `determineStatus` lives here and is the canonical source of truth, but consolidating `save()` onto it is a separate finding (TD-06, not in this batch). Keep `save()`'s status logic local to `RecordingViewModel` for now.
- `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt` — the tests already pin the desired behavior; do NOT modify them to make them pass. If a test fails after the fix, the fix is wrong.
- `RecordingSheet.kt`, `TimeRangeSlider.kt` — UI is correct; the bug is in the ViewModel.
- Any other `*ViewModel.kt` or repository.

## Git workflow

- Branch: `advisor/011-fix-initedit-data-corruption`
- Commit per logical step; message style: `fix(recording): <short desc>` (matches recent history, e.g. `fix(recording): duration slider truncates long events on edit (plan 002)`).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add an `originalStatus` field to preserve the event's time-state when duration is 0

In `RecordingViewModel.kt`, add a private field next to `editingId` (around line 39):

```kotlin
private var editingId: Long? = null
private var originalStatus: Int? = null   // preserved across initEdit so save() can keep in-progress events null-ended
```

In `initNew()` (around line 47-57), reset it:

```kotlin
fun initNew() {
    editingId = null
    originalStatus = null
    // ... rest unchanged ...
}
```

In `initEdit(event)` (line 60), set it at the top:

```kotlin
fun initEdit(event: EventEntity) {
    editingId = event.id
    originalStatus = event.status
    // ... rest unchanged ...
}
```

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0 (field is wired but not yet read; compile must still pass).

### Step 2: Add an explicit `InProgress + null endTime` branch in `initEdit`

In `RecordingViewModel.kt`, immediately AFTER the existing `NotStarted + null endTime` guard (current lines 67-72), add a parallel branch for in-progress events. The combined block should read:

```kotlin
// 没有设置时间（NotStarted 且无 endTime）：默认使用当前时间
if (event.endTime == null && event.status == EventStatus.NotStarted.value) {
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    _startMinutes.value = ((nowMin / 15) * 15).coerceIn(0, 1440)
    _durationMinutes.value = 0
    _durationMax.value = NEW_EVENT_DURATION_MAX
    return
}
// 进行中（InProgress 且 endTime=null）：保持无结束时间，保留原始 startTime
if (event.endTime == null && event.status == EventStatus.InProgress.value) {
    cal.timeInMillis = event.startTime
    val startMin = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(0, 1440)
    _startMinutes.value = startMin
    _durationMinutes.value = 0
    _durationMax.value = NEW_EVENT_DURATION_MAX
    return
}
```

Note: the `NotStarted` branch above also gained an explicit `_durationMax.value = NEW_EVENT_DURATION_MAX` line (it was previously missing — `initEdit` did not reset `_durationMax` for the NotStarted early-return, which could leave a stale max from a prior edit). This is a defensive fix; mention it in the commit message.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 3: Fix the midnight-spanning duration computation in `initEdit`

In `RecordingViewModel.kt`, replace the minute-of-day arithmetic for `rawDuration` (current lines 77-84) with absolute-timestamp math. The `else` branch (`60` fallback) is no longer reachable after Step 2 (both null-endTime cases return early), but keep it as a defensive default. The new block:

```kotlin
cal.timeInMillis = event.startTime
val start = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(300, 1440)
_startMinutes.value = start
val rawDuration = if (event.endTime != null) {
    // 用绝对时间差计算，避免跨午夜时 end < start 的分钟差为负
    ((event.endTime - event.startTime) / 60_000L).toInt().coerceAtLeast(0)
} else {
    60   // defensive; unreachable after Step 2 (both null-endTime branches return early)
}
_durationMinutes.value = rawDuration.coerceIn(0, DURATION_HARD_CEILING)
_durationMax.value = maxOf(NEW_EVENT_DURATION_MAX, ((rawDuration + 59) / 60) * 60).coerceAtMost(DURATION_HARD_CEILING)
```

Key change: `((event.endTime - event.startTime) / 60_000L).toInt()` replaces the `(end - start).coerceAtLeast(0)` minute-of-day subtraction. This is correct for events spanning midnight (e.g. start 23:00, end 02:00 next day → 180 min, not 0).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 4: Preserve `originalStatus` in `save()` when duration is 0

In `RecordingViewModel.kt`, change the `status` computation in `save()` (current lines 109-113) so that the `duration == 0` branch preserves the original in-progress state instead of forcing `NotStarted`:

```kotlin
val status = when {
    duration == 0 -> originalStatus ?: EventStatus.NotStarted.value
    actualEnd != null && now > actualEnd -> EventStatus.Completed.value
    else -> EventStatus.InProgress.value
}
```

Rationale: when `duration == 0` (→ `actualEnd == null`), the event has no end time. Its status should be whatever it was before the edit: `InProgress` for an in-progress event being title-edited, `NotStarted` for a brand-new untimed event (`originalStatus == null` → `NotStarted`). This matches the canonical rule in `EventRepository.determineStatus:133` (`endTime==null && startTime<=now → InProgress`).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 5: Run the RecordingViewModelTest suite — both red tests must now pass

**Verify**: `./gradlew :app:testDebugUnitTest -- --tests "com.shijiben.feature.recording.RecordingViewModelTest"` → exit 0, **all 5 tests pass** (including `save_editingInProgressEvent_preservesNullEndTimeAndStatus` and `save_editingCompletedEvent_keepsEndTime`).

If any of the previously-passing tests (`save_newEvent_hasNonNullEndTime`, `initEdit_fiveHourEvent_setsDurationAndMaxTo300`, `initNew_setsDurationMaxTo180`) now fail, STOP — the fix broke a different case.

### Step 6: Delete the test-exclusion paragraph from `plans/README.md`

In `plans/README.md`, delete the entire "Two pre-existing baseline test failures excluded" paragraph (currently lines 49-53, under "## Verification baseline"). The remaining "Verification baseline" section should keep the first two bullets (no Compose UI tests; the gate commands) and drop the third bullet (the exclusions).

**Verify**: `grep -n "pre-existing baseline test failures" plans/README.md` → no matches.

### Step 7: Full gate

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0 (the whole suite, not just RecordingViewModelTest)
- `./gradlew assembleDebug` → exit 0
- `grep -n "rawDuration = 60" app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` → still present (it's the defensive else branch, kept intentionally), BUT it is now unreachable from the null-endTime paths (both return early in Step 2).

## Test plan

- **No new tests required.** The two existing tests pin both bugs exactly:
  - `save_editingInProgressEvent_preservesNullEndTimeAndStatus` pins Bug A (in-progress edit must keep null endTime + InProgress status).
  - `save_editingCompletedEvent_keepsEndTime` pins Bug B (completed edit must keep non-null endTime). This test was previously flaky (time-of-day dependent); after Step 3 it must pass at all hours.
- The three previously-passing tests are regression guards — they must stay green. Together the five tests cover: new-event save, in-progress edit, completed edit, 5h-event duration/max, initNew defaults.
- **Optional hardening** (not required for done): add `initEdit_midnightSpanningEvent_preservesDuration` creating an event with `startTime = 23:00 yesterday` and `endTime = 02:00 today`, asserting `vm.durationMinutes.value == 180` after `initEdit`. Model after `initEdit_fiveHourEvent_setsDurationAndMaxTo300`. If added, it must pass.
- Verification: `./gradlew :app:testDebugUnitTest -- --tests "com.shijiben.feature.recording.RecordingViewModelTest"` → all pass.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0 (full suite, no exclusions)
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `./gradlew :app:testDebugUnitTest -- --tests "com.shijiben.feature.recording.RecordingViewModelTest"` reports 5/5 passing, including the two previously-red tests
- [ ] `grep -n "pre-existing baseline test failures" plans/README.md` returns no matches (exclusion paragraph deleted)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 011 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `RecordingViewModel.kt:60-113` or `RecordingViewModelTest.kt:46-100` doesn't match the excerpts in "Current state" (the codebase has drifted since this plan was written).
- Any of the three previously-passing tests (`save_newEvent_hasNonNullEndTime`, `initEdit_fiveHourEvent_setsDurationAndMaxTo300`, `initNew_setsDurationMaxTo180`) fails after the fix — the fix broke a different case; do not tweak the tests to force green.
- A test asserts on `originalStatus` directly (it's a private field; if a test needs it, the test is wrong — report).
- The fix appears to require touching `EventRepository.kt`, `RecordingSheet.kt`, or any file outside the in-scope list.
- `RecordingViewModelTest` cannot be run because Robolectric/Gradle is broken in the environment — report the exact error.

## Maintenance notes

- **`originalStatus` is a deliberate minimal fix**, not the final shape. The cleaner long-term design is to consolidate `save()`'s status logic onto `EventRepository.determineStatus` (finding TD-06, deferred from this batch). When that consolidation lands, `originalStatus` becomes redundant and should be removed. A reviewer doing TD-06 should look here first.
- **The `rawDuration = 60` else branch is now unreachable** from the null-endTime paths (both return early in Step 2). It is kept as a defensive default in case a future caller adds a third null-endTime status. If you add such a path, add an explicit branch instead of relying on the `60` fallback.
- **The NotStarted `initEdit` branch now sets `_durationMax`** (Step 2 added `_durationMax.value = NEW_EVENT_DURATION_MAX`). This was a latent bug: editing a NotStarted event after editing a long event would inherit the stale large `_durationMax`. The new test for this is optional (see Test plan); a future characterization test would be welcome.
- **`minutesToTimestamp` and `Calendar` arithmetic** still use `viewingDate` (the date the user is currently viewing, not the event's original date). If the user edits an event while viewing a different day, the start/end will be reconstructed on the wrong day. This is a pre-existing issue, NOT introduced by this plan, and out of scope — but a reviewer should know it exists before refactoring further.
