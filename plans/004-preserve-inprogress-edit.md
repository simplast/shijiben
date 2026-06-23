# Plan 004: Editing preserves in-progress event state

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat e850768..HEAD -- app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`
> If this file changed since this plan was written, compare the "Current state"
> excerpts against the live code before proceeding; on a mismatch, treat it as
> a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: plans/002-status-carryover-tests.md (land first so the
  repository-level status logic is covered by tests before this ViewModel
  change)
- **Category**: bug (correctness)
- **Planned at**: commit `e850768`, 2026-06-23

## Why this matters

An "in-progress" event (`status = InProgress`, `endTime = null`) represents
something the user is actively doing — it is the live timer. Today, editing
such an event for any reason (fixing a typo in the title, changing the tag)
silently fills in `endTime` with the time slider's value, because `save()`
always computes a non-null end. The repository's `updateEvent` does not
re-derive status, so the event ends up `status = InProgress` but `endTime !=
null` — an inconsistent state. Worse, `getOngoingEvent()` queries
`endTime IS NULL AND status = 1`, so the event silently drops out of the
"ongoing" indicator while still claiming to be in progress. This corrupts the
core "记录即审视" contract. The fix: when saving an edit to an in-progress
event, preserve `endTime = null` so it stays genuinely in progress. Status
transitions (to completed/未开始) happen via the dedicated status buttons, not
via the time slider.

## Current state

- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` —
  the bug. `save()` always builds a non-null `actualEnd` and passes it to both
  the create and update paths:

```kotlin
// RecordingViewModel.kt (lines 87-118)
/** 保存（新建或更新）。返回 true 表示成功。viewingDate 为查看日期 Triple<年, 月, 日> */
suspend fun save(viewingDate: Triple<Int, Int, Int>): Boolean {
    val title = _title.value.trim()
    if (title.isEmpty()) return false
    val (y, m, d) = viewingDate
    val start = minutesToTimestamp(y, m, d, _startMinutes.value)
    val end = minutesToTimestamp(y, m, d, _endMinutes.value)
    // 若 end <= start，说明跨日，end 设为次日
    val actualEnd = if (end <= start) end + 24L * 3600 * 1000 else end
    val eid = editingId
    if (eid != null) {
        val existing = eventRepository.getEventById(eid) ?: return false
        eventRepository.updateEvent(
            existing.copy(
                title = title,
                startTime = start,
                endTime = actualEnd,
                tagId = _selectedTagId.value,
                note = _note.value.ifBlank { null }
            )
        )
    } else {
        eventRepository.createEvent(
            title = title,
            startTime = start,
            endTime = actualEnd,
            tagId = _selectedTagId.value,
            note = _note.value.ifBlank { null }
        )
    }
    return true
}
```

- `currentStatus` is tracked on the ViewModel (line 45) and updated by the
  status buttons (`markNotStarted`/`markInProgress`/`markCompleted`,
  lines 126-142). For an in-progress event being edited, `currentStatus ==
  EventStatus.InProgress.value`.

```kotlin
// RecordingViewModel.kt (lines 44-45)
private var editingId: Long? = null
private var currentStatus: Int = EventStatus.NotStarted.value
```

```kotlin
// RecordingViewModel.kt (lines 67-85) — initEdit sets currentStatus from the event
fun initEdit(event: EventEntity) {
    editingId = event.id
    currentStatus = event.status
    _title.value = event.title
    ...
}
```

- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` —
  `updateEvent` does NOT re-derive status (only `createEvent` calls
  `determineStatus`), so the status field is preserved from `existing.copy(...)`.
  `getOngoingEvent()` (in `EventDao.kt:19`) is `WHERE endTime IS NULL AND
  status = 1`.

- `app/src/main/java/com/shijiben/data/model/EventStatus.kt`:
  `NotStarted(0)`, `InProgress(1)`, `Completed(2)`.

### Repo conventions to honor

- ViewModel tests: this repo has NO ViewModel tests yet. Plan 002 established
  the repository test pattern (`@RunWith(RobolectricTestRunner::class)`,
  `@Config(sdk = [33])`, in-memory Room, Truth assertions, `runTest`). A
  ViewModel test additionally needs `Dispatchers.setMain(...)` because
  `viewModelScope` uses `Dispatchers.Main`. The standard helper is a
  `MainCoroutineRule` (code provided in Step 2).
- Dependencies already present: `kotlinx-coroutines-test` (provides
  `runTest`, `TestDispatcher`, `UnconfinedTestDispatcher`),
  `androidx.test:core`, `org.robolectric:robolectric`, `com.google.truth:truth`.
  No new dependencies are needed.
- Kotlin conventions: 4-space indent, `internal`/`private` visibility matching
  existing files.

## Commands you will need

| Purpose   | Command                                      | Expected on success |
|-----------|----------------------------------------------|---------------------|
| Build     | `./gradlew assembleDebug`                    | exit 0, BUILD SUCCESSFUL |
| Typecheck | `./gradlew compileDebugKotlin`               | exit 0, no errors |
| Tests     | `./gradlew testDebugUnitTest`                | exit 0, all pass |
| One class | `./gradlew testDebugUnitTest --tests "com.shijiben.feature.recording.RecordingViewModelTest"` | exit 0 |

## Scope

**In scope** (the only files you should modify/create):
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` (fix `save()`)
- `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt` (create)
- `app/src/test/java/com/shijiben/feature/recording/MainCoroutineRule.kt` (create — test helper)

**Out of scope** (do NOT touch):
- `RecordingSheet.kt` — the UI is fine; the slider showing a placeholder end
  for an in-progress event is acceptable (it is not persisted).
- `EventRepository.kt`, `EventDao.kt` — the repository is correct; the bug is
  in the ViewModel.
- The status buttons (`markNotStarted`/`markInProgress`/`markCompleted`) — they
  already work correctly.
- Do NOT change `determineStatus` or add status re-derivation to `updateEvent`.
  The intended invariant is: editing preserves status; only the dedicated
  status buttons change status.
- New-event creation path (`editingId == null`) — new events always get a
  non-null `endTime` from the slider, which is correct (new events are
  `notStarted` or `completed` based on time; in-progress is entered via the
  status button after creation). Do not alter the create path.

## Git workflow

- Branch: `advisor/004-preserve-inprogress-edit`
- Commit style: `fix(recording): editing an in-progress event preserves null endTime`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Fix save() to preserve null endTime for in-progress events

In `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`,
edit the `save()` function. The current code computes `actualEnd` as a non-null
`Long`. Change it so that when the event being edited is in-progress
(`currentStatus == EventStatus.InProgress.value`), `actualEnd` is `null`,
preserving the in-progress state. New-event creation is unaffected because
`currentStatus` defaults to `NotStarted` for new events.

Replace the body of `save()` with:

```kotlin
suspend fun save(viewingDate: Triple<Int, Int, Int>): Boolean {
    val title = _title.value.trim()
    if (title.isEmpty()) return false
    val (y, m, d) = viewingDate
    val start = minutesToTimestamp(y, m, d, _startMinutes.value)
    // 进行中的事件保持无结束时间：编辑（改标题/标签等）不应改变其进行中状态。
    // 结束时间由"完成"按钮（markCompleted）填入，不由时间滑块填入。
    val actualEnd: Long? = if (currentStatus == EventStatus.InProgress.value) {
        null
    } else {
        val end = minutesToTimestamp(y, m, d, _endMinutes.value)
        // 若 end <= start，说明跨日，end 设为次日
        if (end <= start) end + 24L * 3600 * 1000 else end
    }
    val eid = editingId
    if (eid != null) {
        val existing = eventRepository.getEventById(eid) ?: return false
        eventRepository.updateEvent(
            existing.copy(
                title = title,
                startTime = start,
                endTime = actualEnd,
                tagId = _selectedTagId.value,
                note = _note.value.ifBlank { null }
            )
        )
    } else {
        eventRepository.createEvent(
            title = title,
            startTime = start,
            endTime = actualEnd,
            tagId = _selectedTagId.value,
            note = _note.value.ifBlank { null }
        )
    }
    return true
}
```

Note: `createEvent`'s `endTime` parameter is already `Long?`, and
`existing.copy(endTime = actualEnd)` accepts `Long?`, so both call sites
type-check with a nullable `actualEnd`.

**Verify**: `./gradlew compileDebugKotlin` → exit 0, no errors.

### Step 2: Create the MainCoroutineRule test helper

Create `app/src/test/java/com/shijiben/feature/recording/MainCoroutineRule.kt`.
This is the standard Android coroutine-test rule that swaps `Dispatchers.Main`
for a test dispatcher so `viewModelScope` runs deterministically in tests.

```kotlin
package com.shijiben.feature.recording

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

**Verify**: `./gradlew compileDebugKotlin` → exit 0 (compiles; not yet used).

### Step 3: Create RecordingViewModelTest with the regression test

Create
`app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt`.
It constructs `RecordingViewModel` directly with in-memory repositories (no
Hilt), following the setup pattern from `EventRepositoryTest` (plan 002), plus
the `MainCoroutineRule` for `viewModelScope`.

```kotlin
package com.shijiben.feature.recording

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.model.EventStatus
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.TagRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class RecordingViewModelTest {
    @get:Rule
    val mainRule = MainCoroutineRule()

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var vm: RecordingViewModel

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        eventRepo = EventRepository(db.eventDao())
        tagRepo = TagRepository(db.tagDao())
        vm = RecordingViewModel(eventRepo, tagRepo)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun save_editingInProgressEvent_preservesNullEndTimeAndStatus() = runTest {
        // An in-progress event: past start, null end → determineStatus gives InProgress.
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(
            title = "原标题",
            startTime = now - 60_000,
            endTime = null,
            tagId = null,
            note = null
        )
        val event = eventRepo.getEventById(id)!!
        assertThat(event.status).isEqualTo(EventStatus.InProgress.value)
        assertThat(event.endTime).isNull()

        // Edit it: load into the VM, change only the title, save.
        vm.initEdit(event)
        vm.onTitleChange("新标题")
        val today = Calendar.getInstance(TimeZone.getDefault()).let {
            Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        val ok = vm.save(today)
        assertThat(ok).isTrue()

        // The event must still be in-progress with no end time; only the title changed.
        val saved = eventRepo.getEventById(id)!!
        assertThat(saved.title).isEqualTo("新标题")
        assertThat(saved.status).isEqualTo(EventStatus.InProgress.value)
        assertThat(saved.endTime).isNull()
    }

    @Test
    fun save_editingCompletedEvent_keepsEndTime() = runTest {
        // A completed event with a real end time.
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(
            title = "已完成",
            startTime = now - 7200_000,
            endTime = now - 3600_000,
            tagId = null,
            note = null
        )
        val event = eventRepo.getEventById(id)!!
        assertThat(event.status).isEqualTo(EventStatus.Completed.value)

        vm.initEdit(event)
        vm.onTitleChange("改标题")
        val today = Calendar.getInstance(TimeZone.getDefault()).let {
            Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        val ok = vm.save(today)
        assertThat(ok).isTrue()

        val saved = eventRepo.getEventById(id)!!
        assertThat(saved.title).isEqualTo("改标题")
        assertThat(saved.endTime).isNotNull() // completed events keep their end time
    }

    @Test
    fun save_newEvent_hasNonNullEndTime() = runTest {
        // New events are not in-progress (currentStatus defaults to NotStarted),
        // so they get a real end time from the slider.
        vm.initNew()
        vm.onTitleChange("新事件")
        val today = Calendar.getInstance(TimeZone.getDefault()).let {
            Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        val ok = vm.save(today)
        assertThat(ok).isTrue()

        val all = eventRepo.getAllEvents()
        // Need to collect the flow — use a snapshot via the DAO indirectly.
        // Easiest: read via the repository's getEventById after finding the id.
        // Since createEvent returns the id, re-run with capture:
        // (This assertion re-creates to capture the id; see note below.)
    }
}
```

**Note on the third test**: `vm.save(today)` returns `Boolean`, not the id, and
`eventRepo.getAllEvents()` is a `Flow`. To assert on the persisted event,
replace the body of `save_newEvent_hasNonNullEndTime` with a version that
captures the id. The simplest approach: add a direct DAO insert is not
appropriate; instead, read the first emission of the flow. Replace the
test's tail with:

```kotlin
        val ok = vm.save(today)
        assertThat(ok).isTrue()

        val events = kotlinx.coroutines.flow.first(eventRepo.getAllEvents())
        assertThat(events).hasSize(1)
        assertThat(events.first().endTime).isNotNull()
        assertThat(events.first().title).isEqualTo("新事件")
```

(Use `kotlinx.coroutines.flow.first` — already imported in the repo's other
tests. Add `import kotlinx.coroutines.flow.first` to the test file.)

**Verify**: `./gradlew testDebugUnitTest --tests "com.shijiben.feature.recording.RecordingViewModelTest"` → exit 0, 3 tests pass.

### Step 4: Run the full suite

**Verify**: `./gradlew testDebugUnitTest` → exit 0. All tests from plan 002
(22) + this plan's 3 = 25, all pass. (If plan 002 has not landed yet, the count
differs — confirm with the operator; this plan depends on 002.)

**Verify**: `./gradlew assembleDebug` → exit 0, BUILD SUCCESSFUL.

## Test plan

- `RecordingViewModelTest` (new, 3 tests):
  1. `save_editingInProgressEvent_preservesNullEndTimeAndStatus` — the
     regression test for this bug. Must pass after the fix; would fail before.
  2. `save_editingCompletedEvent_keepsEndTime` — guard against over-fixing
     (completed events must keep their end time).
  3. `save_newEvent_hasNonNullEndTime` — guard the create path (new events get
     a real end time from the slider).
- Structural pattern: model the setup after `EventRepositoryTest` (plan 002),
  adding `MainCoroutineRule` for `viewModelScope`.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -n "currentStatus == EventStatus.InProgress.value" app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` returns one match (inside `save()`)
- [ ] `grep -n "val actualEnd: Long?" app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` returns one match
- [ ] `ls app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt` succeeds
- [ ] `ls app/src/test/java/com/shijiben/feature/recording/MainCoroutineRule.kt` succeeds
- [ ] `./gradlew compileDebugKotlin` exits 0
- [ ] `./gradlew testDebugUnitTest` exits 0 (all tests pass, including the 3 new ones)
- [ ] `./gradlew assembleDebug` exits 0
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 004 updated to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `RecordingViewModel.kt:87-118` (the `save()` body) or
  `RecordingViewModel.kt:44-45,67-85` (`currentStatus` / `initEdit`) doesn't
  match the excerpts above (the codebase has drifted).
- `EventRepository.createEvent`'s `endTime` parameter is NOT `Long?` (the fix
  relies on passing `null` through `createEvent` and `existing.copy`). If it is
  non-nullable, STOP — the signature changed and the plan needs revising.
- `RecordingViewModel` cannot be constructed directly with
  `(eventRepository, tagRepository)` (e.g. Hilt `@Inject` constructor signature
  changed) — report the actual constructor.
- The `MainCoroutineRule` fails to compile because `kotlinx-coroutines-test`
  is missing the expected API (`TestWatcher`/`setMain`/`UnconfinedTestDispatcher`)
  — report the exact unresolved reference; do not invent a different rule.
- A step's verification fails twice after a reasonable fix attempt.

## Maintenance notes

- **Status-transition invariant**: this fix encodes the rule that *editing
  preserves status, and only the dedicated status buttons change status*. Any
  future feature that lets the time slider change an event's status (e.g.
  "drag the end to now to complete") must revisit this `save()` logic —
  currently the slider cannot transition in-progress → completed; that goes
  through `markCompleted()`.
- **New-event in-progress creation (deferred)**: the design doc says
  "进行中的事件通过单独'开始计时'入口创建". The current UI has no "start now"
  button on the new-event sheet (only on edit). If a future plan adds a
  "开始计时" new-event entry, it must set `currentStatus = InProgress` before
  calling `save()` so `actualEnd` is `null` — the fix here already supports
  that path correctly.
- **Reviewer focus**: confirm the create path (`editingId == null`) still
  produces a non-null `endTime` (since `currentStatus` defaults to
  `NotStarted`), and that the completed-event test passes (no over-fix).
