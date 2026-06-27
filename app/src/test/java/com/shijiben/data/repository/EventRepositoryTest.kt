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
}
