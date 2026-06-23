# Plan 002: Characterization tests for status/carry-over core

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat e850768..HEAD -- app/src/main/java/com/shijiben/data/repository/EventRepository.kt app/src/test/java/com/shijiben/data/repository/EventRepositoryTest.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: tests
- **Planned at**: commit `e850768`, 2026-06-23

## Why this matters

The event status-determination and carry-over (顺延) logic is the heart of the
app's "honest recording" philosophy — `not_started` events carry to today so
nothing disappears from awareness. This logic is fragile (time comparisons,
calendar math, DST) and currently has only 8 tests in a single file, leaving
several edge cases untested: carry-over of events with null `endTime`,
multiple pending events, same-day skip, duration preservation, and the
`TagRepository`/`NoteRepository` CRUD paths. Plan 004 changes ViewModel
behavior that depends on this repository layer; these characterization tests
must land first so a regression in the status logic is caught immediately.

## Current state

- `app/src/test/java/com/shijiben/data/repository/EventRepositoryTest.kt` — the
  ONLY test file in the repo. 8 tests using Robolectric + in-memory Room.
  Setup pattern (use as the template for new tests):

```kotlin
// EventRepositoryTest.kt (lines 22-41)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class EventRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var dao: EventDao
    private lateinit var repo: EventRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.eventDao()
        repo = EventRepository(dao)
    }

    @After
    fun teardown() { db.close() }
```

- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` — the
  logic under test. Key methods:

```kotlin
// EventRepository.kt (lines 89-131) — carry-over + shift helper
suspend fun carryOverNotStarted(targetYear: Int, targetMonth: Int, targetDay: Int): Int {
    val (targetStart, targetEnd) = dayRange(targetYear, targetMonth, targetDay)
    val now = System.currentTimeMillis()
    val pending = eventDao.getEventsByStatusBeforeDate(EventStatus.NotStarted.value, targetStart)
    var count = 0
    for (e in pending) {
        val shifted = shiftToTargetDay(e, targetYear, targetMonth, targetDay, now)
        if (shifted != null) {
            eventDao.updateEventTime(id = e.id, newStart = shifted.first, newEnd = shifted.second, now = now)
            count++
        }
    }
    return count
}

internal fun shiftToTargetDay(
    e: EventEntity, year: Int, month: Int, day: Int, now: Long
): Pair<Long, Long?>? {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.timeInMillis = e.startTime
    val hour = cal.get(Calendar.HOUR_OF_DAY)
    val minute = cal.get(Calendar.MINUTE)
    cal.set(year, month - 1, day, hour, minute, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val newStart = cal.timeInMillis
    if (newStart == e.startTime) return null // 已在目标日，无需顺延
    val newEnd: Long? = if (e.endTime != null) {
        val cal2 = Calendar.getInstance(TimeZone.getDefault())
        cal2.timeInMillis = e.endTime
        val dur = e.endTime - e.startTime
        newStart + dur
    } else null
    return Pair(newStart, newEnd)
}

internal fun determineStatus(startTime: Long, endTime: Long?, now: Long): EventStatus {
    if (endTime == null && startTime <= now) return EventStatus.InProgress
    if (startTime > now) return EventStatus.NotStarted
    return EventStatus.Completed
}
```

- `app/src/main/java/com/shijiben/data/repository/TagRepository.kt` — Tag CRUD.
  Note: tag-deletion clears event references at the **ViewModel** layer
  (`TagsViewModel.delete` calls `eventRepository.clearTagReference` then
  `tagRepository.deleteTag`); `TagRepository.deleteTag` itself only deletes the
  tag row. So a `TagRepositoryTest` tests tag CRUD only, not the cross-entity
  reference clearing (that is already covered by
  `EventRepositoryTest.clearTagReference_setsEventTagIdNull`).

```kotlin
// TagRepository.kt (lines 9-37)
@Singleton
class TagRepository @Inject constructor(private val tagDao: TagDao) {
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()
    suspend fun getTagById(id: Long): TagEntity? = tagDao.getTagById(id)
    suspend fun createTag(name: String, color: Int, sortOrder: Int = 0): Long { ... }
    suspend fun updateTag(tag: TagEntity) { ... }
    suspend fun deleteTag(tag: TagEntity) { tagDao.deleteTag(tag) }
    suspend fun deleteTagById(id: Long) = tagDao.deleteTagById(id)
}
```

- `app/src/main/java/com/shijiben/data/repository/NoteRepository.kt` — Note CRUD.

```kotlin
// NoteRepository.kt (lines 9-35)
@Singleton
class NoteRepository @Inject constructor(private val noteDao: NoteDao) {
    fun getNotesByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<NoteEntity>>
    fun getAllNotes(): Flow<List<NoteEntity>>
    suspend fun getNoteById(id: Long): NoteEntity?
    suspend fun createNote(content: String, timestamp: Long): Long
    suspend fun updateNote(note: NoteEntity)
    suspend fun deleteNoteById(id: Long)
}
```

- `app/src/main/java/com/shijiben/data/local/TagDao.kt` — `getAllTags()` orders
  by `sortOrder ASC, id ASC` (line 13).
- `app/src/main/java/com/shijiben/data/model/EventStatus.kt` — enum
  `NotStarted(0)`, `InProgress(1)`, `Completed(2)`.

### Repo conventions to honor

- Test runner: `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [33])`
  (matches the existing test; required because Room needs an Android context).
- Assertions: `com.google.common.truth.Truth.assertThat` (already a dependency).
- Coroutines in tests: `kotlinx.coroutines.test.runTest` (already a dependency).
- Flow in tests: read the first emission with `.first()`
  (`import kotlinx.coroutines.flow.first`).
- File naming: PascalCase matching the class under test (`TagRepositoryTest.kt`).
- Package: `com.shijiben.data.repository`.

## Commands you will need

| Purpose   | Command                                      | Expected on success |
|-----------|----------------------------------------------|---------------------|
| Tests     | `./gradlew testDebugUnitTest`                | exit 0, all pass |
| One class | `./gradlew testDebugUnitTest --tests "com.shijiben.data.repository.EventRepositoryTest"` | exit 0 |

## Suggested executor toolkit

- Use the existing `EventRepositoryTest.kt` setup block verbatim as the template
  for `TagRepositoryTest` and `NoteRepositoryTest` (swap the DAO/repo types).

## Scope

**In scope** (the only files you should modify/create):
- `app/src/test/java/com/shijiben/data/repository/EventRepositoryTest.kt` (extend with new test methods)
- `app/src/test/java/com/shijiben/data/repository/TagRepositoryTest.kt` (create)
- `app/src/test/java/com/shijiben/data/repository/NoteRepositoryTest.kt` (create)

**Out of scope** (do NOT touch):
- Any production source file under `app/src/main/`. This plan is tests-only.
  If a test reveals a real bug, STOP and report — do not fix the production
  code in this plan.
- ViewModel tests (`RecordingViewModelTest`, etc.) — deferred to plan 004.
- Do NOT delete or modify the existing 8 tests in `EventRepositoryTest`.

## Git workflow

- Branch: `advisor/002-status-carryover-tests`
- Commit style: `test(data): characterization tests for status/carry-over + tag/note repos`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Extend EventRepositoryTest with carry-over edge cases

Add the following test methods to the existing
`EventRepositoryTest` class (inside the class body, after the existing
`clearTagReference_setsEventTagIdNull` test). Use the same `Calendar`/
`TimeZone` construction pattern as the existing
`carryOverNotStarted_movesYesterdayNotStartedToToday` test (lines 86-126).

Add these tests:

```kotlin
@Test
fun carryOverNotStarted_withNullEndTime_keepsEndNull() = runTest {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val todayY = cal.get(Calendar.YEAR)
    val todayM = cal.get(Calendar.MONTH) + 1
    val todayD = cal.get(Calendar.DAY_OF_MONTH)
    cal.add(Calendar.DAY_OF_MONTH, -1)
    cal.set(Calendar.HOUR_OF_DAY, 9); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val yesterdayStart = cal.timeInMillis

    val id = dao.insertEvent(EventEntity(
        title = "预写无结束", startTime = yesterdayStart, endTime = null,
        status = EventStatus.NotStarted.value,
        tagId = null, note = null,
        createdAt = yesterdayStart, updatedAt = yesterdayStart
    ))

    val count = repo.carryOverNotStarted(todayY, todayM, todayD)
    assertThat(count).isEqualTo(1)

    val moved = repo.getEventById(id)!!
    assertThat(moved.endTime).isNull()
    assertThat(moved.status).isEqualTo(EventStatus.NotStarted.value)
    val movedCal = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = moved.startTime }
    assertThat(movedCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(9)
}

@Test
fun carryOverNotStarted_multiplePendingEvents_shiftsAll() = runTest {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val todayY = cal.get(Calendar.YEAR)
    val todayM = cal.get(Calendar.MONTH) + 1
    val todayD = cal.get(Calendar.DAY_OF_MONTH)
    cal.add(Calendar.DAY_OF_MONTH, -1)
    cal.set(Calendar.HOUR_OF_DAY, 10); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val y1 = cal.timeInMillis
    cal.set(Calendar.HOUR_OF_DAY, 14)
    val y2 = cal.timeInMillis

    dao.insertEvent(EventEntity(title = "a", startTime = y1, endTime = y1 + 3600_000,
        status = EventStatus.NotStarted.value, tagId = null, note = null,
        createdAt = y1, updatedAt = y1))
    dao.insertEvent(EventEntity(title = "b", startTime = y2, endTime = y2 + 3600_000,
        status = EventStatus.NotStarted.value, tagId = null, note = null,
        createdAt = y2, updatedAt = y2))

    val count = repo.carryOverNotStarted(todayY, todayM, todayD)
    assertThat(count).isEqualTo(2)
}

@Test
fun carryOverNotStarted_eventAlreadyOnTargetDay_isSkipped() = runTest {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val todayY = cal.get(Calendar.YEAR)
    val todayM = cal.get(Calendar.MONTH) + 1
    val todayD = cal.get(Calendar.DAY_OF_MONTH)
    cal.set(Calendar.HOUR_OF_DAY, 11); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis

    val id = dao.insertEvent(EventEntity(
        title = "今天预写", startTime = todayStart, endTime = todayStart + 3600_000,
        status = EventStatus.NotStarted.value, tagId = null, note = null,
        createdAt = todayStart, updatedAt = todayStart
    ))

    val count = repo.carryOverNotStarted(todayY, todayM, todayD)
    assertThat(count).isEqualTo(0) // shiftToTargetDay returns null when newStart == e.startTime

    val unchanged = repo.getEventById(id)!!
    assertThat(unchanged.startTime).isEqualTo(todayStart)
}

@Test
fun carryOverNotStarted_preservesDuration() = runTest {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val todayY = cal.get(Calendar.YEAR)
    val todayM = cal.get(Calendar.MONTH) + 1
    val todayD = cal.get(Calendar.DAY_OF_MONTH)
    cal.add(Calendar.DAY_OF_MONTH, -1)
    cal.set(Calendar.HOUR_OF_DAY, 13); cal.set(Calendar.MINUTE, 15)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val yStart = cal.timeInMillis
    val duration = 5400_000L // 90 min

    val id = dao.insertEvent(EventEntity(
        title = "带时长", startTime = yStart, endTime = yStart + duration,
        status = EventStatus.NotStarted.value, tagId = null, note = null,
        createdAt = yStart, updatedAt = yStart
    ))

    repo.carryOverNotStarted(todayY, todayM, todayD)
    val moved = repo.getEventById(id)!!
    assertThat(moved.endTime!! - moved.startTime).isEqualTo(duration)
}

@Test
fun determineStatus_startTimeEqualsNow_withNullEnd_isInProgress() = runTest {
    val now = System.currentTimeMillis()
    assertThat(repo.determineStatus(now, null, now)).isEqualTo(EventStatus.InProgress)
}

@Test
fun determineStatus_startTimeEqualsNow_withEnd_isCompleted() = runTest {
    val now = System.currentTimeMillis()
    assertThat(repo.determineStatus(now, now + 1000, now)).isEqualTo(EventStatus.Completed)
}
```

**Verify**: `./gradlew testDebugUnitTest --tests "com.shijiben.data.repository.EventRepositoryTest"` → exit 0, all tests (8 existing + 6 new = 14) pass.

### Step 2: Create TagRepositoryTest

Create `app/src/test/java/com/shijiben/data/repository/TagRepositoryTest.kt`
with the same Robolectric setup pattern as `EventRepositoryTest`, testing
`TagRepository` against an in-memory DB. Cover: create + read, update, delete,
and `sortOrder` ordering from `getAllTags()`.

```kotlin
package com.shijiben.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.local.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TagRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: TagRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = TagRepository(db.tagDao())
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun createTag_thenGetById_returnsTag() = runTest {
        val id = repo.createTag(name = "阅读", color = 0xFF3B82F6.toInt(), sortOrder = 0)
        val tag = repo.getTagById(id)!!
        assertThat(tag.name).isEqualTo("阅读")
        assertThat(tag.color).isEqualTo(0xFF3B82F6.toInt())
    }

    @Test
    fun getAllTags_ordersBySortOrderThenId() = runTest {
        repo.createTag("c", 1, sortOrder = 2)
        repo.createTag("a", 2, sortOrder = 0)
        repo.createTag("b", 3, sortOrder = 1)
        val tags = repo.getAllTags().first()
        assertThat(tags.map { it.name }).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun updateTag_changesNameAndColor() = runTest {
        val id = repo.createTag("旧", 1, 0)
        val tag = repo.getTagById(id)!!
        repo.updateTag(tag.copy(name = "新", color = 2))
        val updated = repo.getTagById(id)!!
        assertThat(updated.name).isEqualTo("新")
        assertThat(updated.color).isEqualTo(2)
    }

    @Test
    fun deleteTagById_removesTag() = runTest {
        val id = repo.createTag("temp", 1, 0)
        repo.deleteTagById(id)
        assertThat(repo.getTagById(id)).isNull()
    }
}
```

**Verify**: `./gradlew testDebugUnitTest --tests "com.shijiben.data.repository.TagRepositoryTest"` → exit 0, 4 tests pass.

### Step 3: Create NoteRepositoryTest

Create `app/src/test/java/com/shijiben/data/repository/NoteRepositoryTest.kt`
with the same setup pattern, testing `NoteRepository` CRUD and date-range
filtering.

```kotlin
package com.shijiben.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: NoteRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = NoteRepository(db.noteDao())
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun createNote_thenGetById_returnsNote() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createNote("想法", now)
        val note = repo.getNoteById(id)!!
        assertThat(note.content).isEqualTo("想法")
        assertThat(note.timestamp).isEqualTo(now)
    }

    @Test
    fun getNotesByDateRange_filtersToRange() = runTest {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 10); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val inRange = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val outOfRange = cal.timeInMillis

        repo.createNote("今天", inRange)
        repo.createNote("明天", outOfRange)

        val dayStart = inRange
        val dayEnd = inRange + 24L * 3600 * 1000
        val notes = repo.getNotesByDateRange(dayStart, dayEnd).first()
        assertThat(notes).hasSize(1)
        assertThat(notes.first().content).isEqualTo("今天")
    }

    @Test
    fun updateNote_changesContent() = runTest {
        val id = repo.createNote("旧", System.currentTimeMillis())
        val note = repo.getNoteById(id)!!
        repo.updateNote(note.copy(content = "新"))
        assertThat(repo.getNoteById(id)!!.content).isEqualTo("新")
    }

    @Test
    fun deleteNoteById_removesNote() = runTest {
        val id = repo.createNote("temp", System.currentTimeMillis())
        repo.deleteNoteById(id)
        assertThat(repo.getNoteById(id)).isNull()
    }
}
```

**Verify**: `./gradlew testDebugUnitTest --tests "com.shijiben.data.repository.NoteRepositoryTest"` → exit 0, 4 tests pass.

### Step 4: Run the full suite

**Verify**: `./gradlew testDebugUnitTest` → exit 0. Total: 8 (existing) + 6 + 4 + 4 = 22 tests, all pass.

## Test plan

This plan IS the test plan. The new tests are listed in the steps above. The
existing `EventRepositoryTest` is the structural pattern to model after.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./gradlew testDebugUnitTest` exits 0 with 22 total tests (8 existing + 14 new)
- [ ] `ls app/src/test/java/com/shijiben/data/repository/TagRepositoryTest.kt` succeeds
- [ ] `ls app/src/test/java/com/shijiben/data/repository/NoteRepositoryTest.kt` succeeds
- [ ] No production files modified (`git diff --name-only app/src/main/` returns nothing)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 002 updated to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `EventRepository.kt:89-140` (carry-over / shift / determineStatus)
  doesn't match the excerpts above (the codebase has drifted).
- A new test fails against the CURRENT (unmodified) production code — this
  indicates either a real bug (report it; do NOT fix production code in this
  plan) or a test-writing error. Distinguish by re-reading the production logic
  before reporting.
- The Robolectric runner fails to initialize with `@Config(sdk = [33])` — report
  the exact error; do not change the SDK level without confirmation.
- A step's verification fails twice after a reasonable fix attempt.

## Maintenance notes

- **For plan 004**: the `RecordingViewModel` test added in 004 will construct
  `EventRepository`/`TagRepository` the same way (in-memory DB, direct
  construction). The setup pattern established here is the template.
- **Reviewer focus**: confirm no production code was touched, and that the
  carry-over tests use real `Calendar` instances (not hardcoded timestamps) so
  they stay correct across time zones and DST.
- **Deferred**: ViewModel-level tests (TimelineViewModel, TagsViewModel) and
  the tag-delete-clears-events integration (already covered at the repository
  level by `clearTagReference_setsEventTagIdNull`).
