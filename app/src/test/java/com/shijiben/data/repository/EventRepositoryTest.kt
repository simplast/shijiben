package com.shijiben.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.local.EventDao
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
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

    @Test
    fun createEvent_withFutureStartTime_isNotStarted() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createEvent(
            title = "未来事件",
            startTime = now + 60_000,
            endTime = now + 120_000,
            note = null
        )
        val event = repo.getEventById(id)!!
        assertThat(event.status).isEqualTo(EventStatus.NotStarted.value)
    }

    @Test
    fun createEvent_withPastStartAndEnd_isCompleted() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createEvent(
            title = "补录事件",
            startTime = now - 3_600_000,
            endTime = now - 1_800_000,
            note = null
        )
        val event = repo.getEventById(id)!!
        assertThat(event.status).isEqualTo(EventStatus.Completed.value)
    }

    @Test
    fun createEvent_withNullEndAndPastStart_isInProgress() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createEvent(
            title = "进行中",
            startTime = now - 1_800_000,
            endTime = null,
            note = null
        )
        val event = repo.getEventById(id)!!
        assertThat(event.status).isEqualTo(EventStatus.InProgress.value)
    }

    @Test
    fun carryOverNotStarted_movesYesterdayNotStartedToToday() = runTest {
        // 用真实日历构造昨天 14:30 的 notStarted 事件
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val todayY = cal.get(Calendar.YEAR)
        val todayM = cal.get(Calendar.MONTH) + 1
        val todayD = cal.get(Calendar.DAY_OF_MONTH)

        // 昨天 14:30
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 14)
        cal.set(Calendar.MINUTE, 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yesterdayStart = cal.timeInMillis
        val yesterdayEnd = yesterdayStart + 60 * 60 * 1000

        // 直接插入一条 notStarted 事件到昨天
        val id = dao.insertEvent(EventEntity(
            title = "昨天预写",
            startTime = yesterdayStart,
            endTime = yesterdayEnd,
            status = EventStatus.NotStarted.value,
            note = null,
            createdAt = yesterdayStart, updatedAt = yesterdayStart
        ))

        // 执行顺延到今天
        val count = repo.carryOverNotStarted(todayY, todayM, todayD)
        assertThat(count).isEqualTo(1)

        // 验证事件已移到今天 14:30
        val moved = repo.getEventById(id)!!
        val movedCal = Calendar.getInstance(TimeZone.getDefault())
        movedCal.timeInMillis = moved.startTime
        assertThat(movedCal.get(Calendar.YEAR)).isEqualTo(todayY)
        assertThat(movedCal.get(Calendar.MONTH) + 1).isEqualTo(todayM)
        assertThat(movedCal.get(Calendar.DAY_OF_MONTH)).isEqualTo(todayD)
        assertThat(movedCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(14)
        assertThat(movedCal.get(Calendar.MINUTE)).isEqualTo(30)
        assertThat(moved.status).isEqualTo(EventStatus.NotStarted.value)
    }

    @Test
    fun carryOverNotStarted_doesNotMoveCompleted() = runTest {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val todayY = cal.get(Calendar.YEAR)
        val todayM = cal.get(Calendar.MONTH) + 1
        val todayD = cal.get(Calendar.DAY_OF_MONTH)

        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yesterdayStart = cal.timeInMillis

        val id = dao.insertEvent(EventEntity(
            title = "昨天完成",
            startTime = yesterdayStart,
            endTime = yesterdayStart + 3600_000,
            status = EventStatus.Completed.value,
            note = null,
            createdAt = yesterdayStart, updatedAt = yesterdayStart
        ))

        val count = repo.carryOverNotStarted(todayY, todayM, todayD)
        assertThat(count).isEqualTo(0)

        val still = repo.getEventById(id)!!
        val stillCal = Calendar.getInstance(TimeZone.getDefault())
        stillCal.timeInMillis = still.startTime
        // 仍然是昨天
        assertThat(stillCal.get(Calendar.DAY_OF_MONTH)).isNotEqualTo(todayD)
    }

    @Test
    fun markCompleted_setsStatusAndFillsEndTime() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createEvent("test", now - 1000, null, null, null)
        repo.markCompleted(id)
        val e = repo.getEventById(id)!!
        assertThat(e.status).isEqualTo(EventStatus.Completed.value)
        assertThat(e.endTime).isNotNull()
    }

    @Test
    fun markInProgress_clearsEndTime() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createEvent("test", now - 1000, now - 500, null, null)
        repo.markInProgress(id)
        val e = repo.getEventById(id)!!
        assertThat(e.status).isEqualTo(EventStatus.InProgress.value)
        assertThat(e.endTime).isNull()
    }

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
            note = null,
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
            status = EventStatus.NotStarted.value, note = null,
            createdAt = y1, updatedAt = y1))
        dao.insertEvent(EventEntity(title = "b", startTime = y2, endTime = y2 + 3600_000,
            status = EventStatus.NotStarted.value, note = null,
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
            status = EventStatus.NotStarted.value, note = null,
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
            status = EventStatus.NotStarted.value, note = null,
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

    // ==================== determineStatus 全分支覆盖（ARCHITECTURE.md §8 状态机不变式） ====================

    @Test
    fun determineStatus_pastStart_nullEnd_isInProgress() = runTest {
        // startTime 严格早于 now + endTime null → InProgress（进行中）
        val now = System.currentTimeMillis()
        val pastStart = now - 3600_000L // 1h 前
        assertThat(repo.determineStatus(pastStart, null, now)).isEqualTo(EventStatus.InProgress)
    }

    @Test
    fun determineStatus_futureStart_nullEnd_isNotStarted() = runTest {
        // startTime 严格晚于 now + endTime null → NotStarted（预写）
        val now = System.currentTimeMillis()
        val futureStart = now + 3600_000L // 1h 后
        assertThat(repo.determineStatus(futureStart, null, now)).isEqualTo(EventStatus.NotStarted)
    }

    @Test
    fun determineStatus_futureStart_withEnd_isNotStarted() = runTest {
        // startTime 严格晚于 now + endTime 已设 → 仍 NotStarted（未来计划事件，即使有预估结束也属预写）
        // 此分支是 cycle 26 修复的 bug 场景：RecordingViewModel.save() 此前误把 duration>0 的未来事件标 InProgress
        val now = System.currentTimeMillis()
        val futureStart = now + 3600_000L
        val futureEnd = now + 7200_000L
        assertThat(repo.determineStatus(futureStart, futureEnd, now)).isEqualTo(EventStatus.NotStarted)
    }

    @Test
    fun determineStatus_pastStart_withEnd_isCompleted() = runTest {
        // startTime 严格早于 now + endTime 已设 → Completed（补录已完成）
        val now = System.currentTimeMillis()
        val pastStart = now - 7200_000L // 2h 前
        val pastEnd = now - 3600_000L   // 1h 前
        assertThat(repo.determineStatus(pastStart, pastEnd, now)).isEqualTo(EventStatus.Completed)
    }

    // ==================== shiftToTargetDay（直接单测，覆盖日历边界）====================

    @Test
    fun shiftToTargetDay_preservesHourAndMinute() {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val todayY = cal.get(Calendar.YEAR)
        val todayM = cal.get(Calendar.MONTH) + 1
        val todayD = cal.get(Calendar.DAY_OF_MONTH)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 14)
        cal.set(Calendar.MINUTE, 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yesterdayStart = cal.timeInMillis

        val event = EventEntity(
            title = "x", startTime = yesterdayStart, endTime = yesterdayStart + 3600_000,
            status = EventStatus.NotStarted.value, note = null,
            createdAt = yesterdayStart, updatedAt = yesterdayStart
        )

        val shifted = repo.shiftToTargetDay(event, todayY, todayM, todayD)!!
        val shiftedCal = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = shifted.first }
        assertThat(shiftedCal.get(Calendar.YEAR)).isEqualTo(todayY)
        assertThat(shiftedCal.get(Calendar.MONTH) + 1).isEqualTo(todayM)
        assertThat(shiftedCal.get(Calendar.DAY_OF_MONTH)).isEqualTo(todayD)
        assertThat(shiftedCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(14)
        assertThat(shiftedCal.get(Calendar.MINUTE)).isEqualTo(30)
    }

    @Test
    fun shiftToTargetDay_crossMonthBoundary() {
        // 1月31日 10:00 → 顺延到 2月1日（跨月边界，Calendar 自动处理 31→1）
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(2026, Calendar.JANUARY, 31, 10, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val jan31Start = cal.timeInMillis

        val event = EventEntity(
            title = "x", startTime = jan31Start, endTime = jan31Start + 3600_000,
            status = EventStatus.NotStarted.value, note = null,
            createdAt = jan31Start, updatedAt = jan31Start
        )

        val shifted = repo.shiftToTargetDay(event, 2026, 2, 1)!!
        val shiftedCal = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = shifted.first }
        assertThat(shiftedCal.get(Calendar.YEAR)).isEqualTo(2026)
        assertThat(shiftedCal.get(Calendar.MONTH)).isEqualTo(Calendar.FEBRUARY)
        assertThat(shiftedCal.get(Calendar.DAY_OF_MONTH)).isEqualTo(1)
        assertThat(shiftedCal.get(Calendar.HOUR_OF_DAY)).isEqualTo(10)
    }

    @Test
    fun shiftToTargetDay_nullEndTime_returnsNullEnd() {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val todayY = cal.get(Calendar.YEAR)
        val todayM = cal.get(Calendar.MONTH) + 1
        val todayD = cal.get(Calendar.DAY_OF_MONTH)
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yesterdayStart = cal.timeInMillis

        val event = EventEntity(
            title = "x", startTime = yesterdayStart, endTime = null,
            status = EventStatus.NotStarted.value, note = null,
            createdAt = yesterdayStart, updatedAt = yesterdayStart
        )

        val shifted = repo.shiftToTargetDay(event, todayY, todayM, todayD)!!
        assertThat(shifted.second).isNull()
    }

    @Test
    fun shiftToTargetDay_alreadyOnTargetDay_returnsNull() {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 11)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayY = cal.get(Calendar.YEAR)
        val todayM = cal.get(Calendar.MONTH) + 1
        val todayD = cal.get(Calendar.DAY_OF_MONTH)
        val todayStart = cal.timeInMillis

        val event = EventEntity(
            title = "x", startTime = todayStart, endTime = todayStart + 3600_000,
            status = EventStatus.NotStarted.value, note = null,
            createdAt = todayStart, updatedAt = todayStart
        )

        // 已在目标日 → 返回 null（无需顺延）
        assertThat(repo.shiftToTargetDay(event, todayY, todayM, todayD)).isNull()
    }

    @Test
    fun shiftToTargetDay_preservesDurationWithEndTime() {
        // 6月10日 10:00→11:00（1h）顺延到 6月15日，时长应保持 1h
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(2026, Calendar.JUNE, 10, 10, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 3600_000

        val event = EventEntity(
            title = "x", startTime = start, endTime = end,
            status = EventStatus.NotStarted.value, note = null,
            createdAt = start, updatedAt = start
        )

        val shifted = repo.shiftToTargetDay(event, 2026, 6, 15)!!
        val duration = shifted.second!! - shifted.first
        assertThat(duration).isEqualTo(3600_000)
    }

    @Test
    fun markNotStarted_validEventId_setsStatusToNotStarted() = runTest {
        val now = System.currentTimeMillis()
        // startTime 过去 + endTime null → determineStatus 返回 InProgress
        val id = repo.createEvent("test", now - 1000, null, null, null)
        repo.markNotStarted(id)
        val e = repo.getEventById(id)!!
        assertThat(e.status).isEqualTo(EventStatus.NotStarted.value)
    }

    @Test
    fun markNotStarted_nonExistentId_isNoOp() = runTest {
        repo.markNotStarted(9999L)
        assertThat(repo.getAllEvents().first()).isEmpty()
    }

    @Test
    fun markCompleted_nonExistentId_isNoOp() = runTest {
        repo.markCompleted(9999L)
        assertThat(repo.getAllEvents().first()).isEmpty()
    }

    @Test
    fun markInProgress_nonExistentId_isNoOp() = runTest {
        repo.markInProgress(9999L)
        assertThat(repo.getAllEvents().first()).isEmpty()
    }

    @Test
    fun deleteEventById_validId_removesEvent() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createEvent("test", now - 1000, now - 500, null, null)
        repo.deleteEventById(id)
        assertThat(repo.getEventById(id)).isNull()
        assertThat(repo.getAllEvents().first()).isEmpty()
    }

    @Test
    fun updateEvent_persistsModifiedFields() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createEvent("原标题", now - 1000, now - 500, null, null)
        val original = repo.getEventById(id)!!
        repo.updateEvent(original.copy(title = "新标题", note = "新备注"))
        val updated = repo.getEventById(id)!!
        assertThat(updated.title).isEqualTo("新标题")
        assertThat(updated.note).isEqualTo("新备注")
        assertThat(updated.startTime).isEqualTo(original.startTime)
        assertThat(updated.endTime).isEqualTo(original.endTime)
        assertThat(updated.status).isEqualTo(original.status)
    }

    @Test
    fun upsertAll_insertsAllEvents() = runTest {
        val e1 = EventEntity(id = 1, title = "a", startTime = 1000L, endTime = 2000L,
            status = EventStatus.Completed.value, note = null, createdAt = 3000L, updatedAt = 4000L)
        val e2 = EventEntity(id = 2, title = "b", startTime = 5000L, endTime = null,
            status = EventStatus.InProgress.value, note = "n", createdAt = 6000L, updatedAt = 7000L)
        val e3 = EventEntity(id = 3, title = "c", startTime = 8000L, endTime = 9000L,
            status = EventStatus.NotStarted.value, note = null, createdAt = 10000L, updatedAt = 11000L)
        repo.upsertAll(listOf(e1, e2, e3))
        assertThat(repo.getAllEvents().first()).hasSize(3)
        assertThat(repo.getEventById(1)).isNotNull()
        assertThat(repo.getEventById(2)).isNotNull()
        assertThat(repo.getEventById(3)).isNotNull()
    }

    @Test
    fun upsertAll_replacesOnIdConflict() = runTest {
        val e1 = EventEntity(id = 1, title = "原", startTime = 1000L, endTime = 2000L,
            status = EventStatus.Completed.value, note = null, createdAt = 3000L, updatedAt = 4000L)
        dao.insertEvent(e1)
        val e1prime = EventEntity(id = 1, title = "新", startTime = 5000L, endTime = null,
            status = EventStatus.InProgress.value, note = "updated", createdAt = 6000L, updatedAt = 7000L)
        repo.upsertAll(listOf(e1prime))
        assertThat(repo.getAllEvents().first()).hasSize(1)
        assertThat(repo.getEventById(1)!!.title).isEqualTo("新")
    }
}
