package com.shijiben.feature.timeline

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.model.EventStatus
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.test.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class TimelineViewModelTest {
    @get:Rule
    val mainRule = MainCoroutineRule()

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var noteRepo: NoteRepository
    private lateinit var vm: TimelineViewModel

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepo = EventRepository(db.eventDao())
        noteRepo = NoteRepository(db.noteDao())
        vm = TimelineViewModel(eventRepo, noteRepo)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun refresh_forceReread_picksUpNewlyCreatedEvent() = runTest {
        // 初始 events 为空
        assertThat(vm.events.value).isEmpty()

        // 写入一个今天的事件（直接走 repo，模拟 RecordingViewModel.save 的副作用）
        val now = System.currentTimeMillis()
        eventRepo.createEvent(
            title = "新事件",
            startTime = now - 60_000,
            endTime = now + 60_000,
            note = null
        )

        // 不调用 refresh 的话，StateFlow 的当前缓存值仍然可能是空
        // 调用 refresh() 后应能拉到新事件
        vm.refresh()
        val updated = vm.events.first { it.isNotEmpty() }
        assertThat(updated).hasSize(1)
        assertThat(updated.first().title).isEqualTo("新事件")
    }

    @Test
    fun refresh_incrementsTrigger() = runTest {
        val before = vm.refreshTrigger.value
        vm.refresh()
        val after = vm.refreshTrigger.value
        assertThat(after).isEqualTo(before + 1)
    }

    @Test
    fun viewingDate_keepsStableAcrossRefresh() = runTest {
        val today = vm.viewingDate.value
        vm.refresh()
        assertThat(vm.viewingDate.value).isEqualTo(today)
    }

    @Test
    fun goToPreviousDay_thenRefresh_stillEmitsEmptyWhenNoEvents() = runTest {
        vm.goToPreviousDay()
        vm.refresh()
        assertThat(vm.events.value).isEmpty()
    }

    @Test
    fun quickAddEvent_blankTitle_createsNoEvent() = runTest {
        vm.quickAddEvent("")
        vm.quickAddEvent("   ")
        assertThat(eventRepo.getAllEvents().first()).isEmpty()
        assertThat(vm.events.value).isEmpty()
    }

    @Test
    fun quickAddEvent_validTitle_createsNotStartedEvent() = runTest {
        vm.quickAddEvent("测试事件")
        val list = vm.events.first { it.isNotEmpty() }
        assertThat(list).hasSize(1)
        val e = list.first()
        assertThat(e.title).isEqualTo("测试事件")
        assertThat(e.status).isEqualTo(EventStatus.NotStarted.value)
        assertThat(e.endTime).isNull()
    }

    @Test
    fun markInProgress_nonExistentEventId_leavesDbUnchanged() = runTest {
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(title = "种子", startTime = now, endTime = null, note = null)
        val before = eventRepo.getEventById(id)!!
        vm.markInProgress(999_999L)
        assertThat(eventRepo.getEventById(999_999L)).isNull()
        val after = eventRepo.getEventById(id)!!
        assertThat(after.status).isEqualTo(before.status)
        assertThat(after.startTime).isEqualTo(before.startTime)
        assertThat(after.endTime).isEqualTo(before.endTime)
        assertThat(eventRepo.getAllEvents().first()).hasSize(1)
    }

    @Test
    fun markInProgress_validEventId_setsInProgressWithNullEndTime() = runTest {
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(
            title = "待开始",
            startTime = now,
            endTime = null,
            note = null,
            status = EventStatus.NotStarted.value
        )
        vm.markInProgress(id)
        val list = vm.events.first { it.any { e -> e.id == id && e.status == EventStatus.InProgress.value } }
        val e = list.first { it.id == id }
        assertThat(e.status).isEqualTo(EventStatus.InProgress.value)
        assertThat(e.endTime).isNull()
        assertThat(Math.abs(e.startTime - now)).isLessThan(5_000L)
    }

    @Test
    fun markCompleted_nonExistentEventId_leavesDbUnchanged() = runTest {
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(title = "种子", startTime = now, endTime = null, note = null)
        val before = eventRepo.getEventById(id)!!
        vm.markCompleted(999_999L)
        assertThat(eventRepo.getEventById(999_999L)).isNull()
        val after = eventRepo.getEventById(id)!!
        assertThat(after.status).isEqualTo(before.status)
        assertThat(after.startTime).isEqualTo(before.startTime)
        assertThat(after.endTime).isEqualTo(before.endTime)
        assertThat(eventRepo.getAllEvents().first()).hasSize(1)
    }

    @Test
    fun markCompleted_validEventId_setsCompletedWithEndTime() = runTest {
        val now = System.currentTimeMillis()
        val originalStart = now - 60_000
        val id = eventRepo.createEvent(
            title = "进行中",
            startTime = originalStart,
            endTime = null,
            note = null,
            status = EventStatus.InProgress.value
        )
        vm.markCompleted(id)
        val list = vm.events.first { it.any { e -> e.id == id && e.status == EventStatus.Completed.value } }
        val e = list.first { it.id == id }
        assertThat(e.status).isEqualTo(EventStatus.Completed.value)
        assertThat(e.endTime).isNotNull()
        assertThat(Math.abs(e.endTime!! - now)).isLessThan(5_000L)
        assertThat(e.startTime).isEqualTo(originalStart)
    }

    @Test
    fun deleteEvent_validEventId_removesEvent() = runTest {
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(title = "待删", startTime = now, endTime = null, note = null)
        vm.deleteEvent(id)
        vm.events.first { it.isEmpty() }
        assertThat(eventRepo.getEventById(id)).isNull()
    }

    @Test
    fun init_carriesOverPastNotStartedEventToToday() = runTest {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.add(Calendar.DAY_OF_MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 10)
        cal.set(Calendar.MINUTE, 30)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val yesterdayStart = cal.timeInMillis
        val id = eventRepo.createEvent(
            title = "待顺延",
            startTime = yesterdayStart,
            endTime = null,
            note = null,
            status = EventStatus.NotStarted.value
        )
        val freshVm = TimelineViewModel(eventRepo, noteRepo)
        val list = freshVm.events.first { it.isNotEmpty() }
        assertThat(list).hasSize(1)
        val e = list.first()
        assertThat(e.id).isEqualTo(id)
        val todayCal = Calendar.getInstance(TimeZone.getDefault())
        val eCal = Calendar.getInstance(TimeZone.getDefault())
        eCal.timeInMillis = e.startTime
        assertThat(eCal.get(Calendar.YEAR)).isEqualTo(todayCal.get(Calendar.YEAR))
        assertThat(eCal.get(Calendar.MONTH)).isEqualTo(todayCal.get(Calendar.MONTH))
        assertThat(eCal.get(Calendar.DAY_OF_MONTH)).isEqualTo(todayCal.get(Calendar.DAY_OF_MONTH))
        assertThat(e.status).isEqualTo(EventStatus.NotStarted.value)
    }

    @Test
    fun dateNavigation_changesAndRestoresViewingDate() = runTest {
        val today = vm.viewingDate.value
        vm.setDate(2025, 1, 15)
        assertThat(vm.viewingDate.value).isEqualTo(Triple(2025, 1, 15))
        vm.goToNextDay()
        assertThat(vm.viewingDate.value).isEqualTo(Triple(2025, 1, 16))
        vm.goToPreviousDay()
        assertThat(vm.viewingDate.value).isEqualTo(Triple(2025, 1, 15))
        vm.goToPreviousDay()
        assertThat(vm.viewingDate.value).isEqualTo(Triple(2025, 1, 14))
        vm.goToToday()
        assertThat(vm.viewingDate.value).isEqualTo(today)
    }
}
