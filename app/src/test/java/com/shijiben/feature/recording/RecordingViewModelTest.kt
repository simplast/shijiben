package com.shijiben.feature.recording

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.model.EventStatus
import com.shijiben.data.repository.EventRepository
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
class RecordingViewModelTest {
    

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var vm: RecordingViewModel

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        eventRepo = EventRepository(db.eventDao())
        vm = RecordingViewModel(eventRepo)
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

        val events = eventRepo.getAllEvents().first()
        assertThat(events).hasSize(1)
        assertThat(events.first().endTime).isNotNull()
        assertThat(events.first().title).isEqualTo("新事件")
    }

    @Test
    fun initEdit_fiveHourEvent_setsDurationAndMaxTo300() = runTest {
        // A completed 5h event (09:00 → 14:00): slider must represent the true
        // duration, so both durationMinutes and durationMax equal 300 (no 3h cap).
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 9)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis
        cal.set(Calendar.HOUR_OF_DAY, 14)
        val endTime = cal.timeInMillis

        val id = eventRepo.createEvent(
            title = "5小时事件",
            startTime = startTime,
            endTime = endTime,
            note = null
        )
        val event = eventRepo.getEventById(id)!!

        vm.initEdit(event)
        assertThat(vm.durationMinutes.value).isEqualTo(300)
    }

    @Test
    fun save_blankTitle_returnsFalse() = runTest {
        vm.initNew()
        val today = Calendar.getInstance(TimeZone.getDefault()).let {
            Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        val ok = vm.save(today)
        assertThat(ok).isFalse()
        assertThat(eventRepo.getAllEvents().first()).isEmpty()

        vm.onTitleChange("   ")
        val ok2 = vm.save(today)
        assertThat(ok2).isFalse()
        assertThat(eventRepo.getAllEvents().first()).isEmpty()
    }

    @Test
    fun save_editingDeletedEvent_returnsFalse() = runTest {
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(
            title = "原标题",
            startTime = now - 60_000,
            endTime = null,
            note = null
        )
        val event = eventRepo.getEventById(id)!!
        vm.initEdit(event)
        eventRepo.deleteEventById(id)
        val today = Calendar.getInstance(TimeZone.getDefault()).let {
            Triple(it.get(Calendar.YEAR), it.get(Calendar.MONTH) + 1, it.get(Calendar.DAY_OF_MONTH))
        }
        val ok = vm.save(today)
        assertThat(ok).isFalse()
    }

    @Test
    fun delete_withoutEditingId_returnsFalse() = runTest {
        val ok = vm.delete()
        assertThat(ok).isFalse()
    }

    @Test
    fun delete_afterInitEdit_removesEventAndReturnsTrue() = runTest {
        val now = System.currentTimeMillis()
        val id = eventRepo.createEvent(
            title = "待删",
            startTime = now,
            endTime = null,
            note = null
        )
        val event = eventRepo.getEventById(id)!!
        vm.initEdit(event)
        val ok = vm.delete()
        assertThat(ok).isTrue()
        assertThat(eventRepo.getEventById(id)).isNull()
    }

    @Test
    fun initEdit_notStartedEventWithNullEndTime_usesCurrentTimeAndZeroDuration() = runTest {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 2)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val fixedStart = cal.timeInMillis
        val id = eventRepo.createEvent(
            title = "待办",
            startTime = fixedStart,
            endTime = null,
            note = null,
            status = EventStatus.NotStarted.value
        )
        val event = eventRepo.getEventById(id)!!

        val cal1 = Calendar.getInstance(TimeZone.getDefault())
        val nowMin1 = cal1.get(Calendar.HOUR_OF_DAY) * 60 + cal1.get(Calendar.MINUTE)
        vm.initEdit(event)
        val cal2 = Calendar.getInstance(TimeZone.getDefault())
        val nowMin2 = cal2.get(Calendar.HOUR_OF_DAY) * 60 + cal2.get(Calendar.MINUTE)
        val possibleSnaps = setOf(
            ((nowMin1 / 15) * 15).coerceIn(0, 1440),
            ((nowMin2 / 15) * 15).coerceIn(0, 1440)
        )

        assertThat(vm.durationMinutes.value).isEqualTo(0)
        assertThat(possibleSnaps.contains(vm.startMinutes.value)).isTrue()
    }
}
