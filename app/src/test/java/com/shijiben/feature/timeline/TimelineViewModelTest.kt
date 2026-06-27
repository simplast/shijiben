package com.shijiben.feature.timeline

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.feature.recording.MainCoroutineRule
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
}
