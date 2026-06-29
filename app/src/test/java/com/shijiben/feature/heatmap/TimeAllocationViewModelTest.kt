package com.shijiben.feature.heatmap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.repository.EventRepository
import com.shijiben.feature.recording.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

/**
 * TimeAllocationViewModel 单测。
 *
 * 沿用 HeatmapViewModelTest 的时序控制模式（迭代 9 方案 A）：
 * - StandardTestDispatcher + runTest(mainRule.dispatcher) 统一 TestScope 与 viewModelScope 调度器
 * - setQueryExecutor/setTransactionExecutor 路由 Room 任务到 test dispatcher，消除 teardown 竞态
 * - backgroundScope.launch{ state.collect{} } 保活 WhileSubscribed(5000)
 * - state.first{...} 等待真实值（不依赖 advanceUntilIdle()）
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TimeAllocationViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var vm: TimeAllocationViewModel

    // 固定时钟：2026-06-15T12:00:00Z（周一），UTC
    private val fixedInstant = Instant.parse("2026-06-15T12:00:00Z")
    private val fixedZone = ZoneOffset.UTC
    private val fixedClock = Clock.fixed(fixedInstant, fixedZone)

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // 路由 Room executor 到 test dispatcher（同 HeatmapViewModelTest 方案 A）
        val roomExecutor = java.util.concurrent.Executor { command ->
            mainRule.dispatcher.dispatch(kotlin.coroutines.EmptyCoroutineContext, command)
        }
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .setQueryExecutor(roomExecutor)
            .setTransactionExecutor(roomExecutor)
            .allowMainThreadQueries()
            .build()
        eventRepo = EventRepository(db.eventDao())
        vm = TimeAllocationViewModel(eventRepo, fixedClock)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun selectRange_updatesState() = runTest(mainRule.dispatcher) {
        // selectedRange 是单独的 StateFlow，不依赖 state 的 WhileSubscribed 订阅
        assertThat(vm.selectedRange.value).isEqualTo(TimeAllocationCalculator.TimeRange.MONTH)
        vm.selectRange(TimeAllocationCalculator.TimeRange.WEEK)
        assertThat(vm.selectedRange.value).isEqualTo(TimeAllocationCalculator.TimeRange.WEEK)
        vm.selectRange(TimeAllocationCalculator.TimeRange.ALL)
        assertThat(vm.selectedRange.value).isEqualTo(TimeAllocationCalculator.TimeRange.ALL)
    }

    @Test
    fun state_initial_loadsMonthRange() = runTest(mainRule.dispatcher) {
        // 本月 completed 事件（2026-06-10，在 now=2026-06-15 本月范围内）
        val june10 = Instant.parse("2026-06-10T10:00:00Z").toEpochMilli()
        eventRepo.createEvent(
            title = "阅读",
            startTime = june10,
            endTime = june10 + 3600_000,
            note = null
        )
        // 上月事件（应过滤）
        val may10 = Instant.parse("2026-05-10T10:00:00Z").toEpochMilli()
        eventRepo.createEvent(
            title = "上月事",
            startTime = may10,
            endTime = may10 + 3600_000,
            note = null
        )

        // 保活 WhileSubscribed(5000)，等待 Success 状态落定
        backgroundScope.launch { vm.state.collect {} }
        val state = vm.state.first { it is TimeAllocationViewModel.UiState.Success }
        val success = state as TimeAllocationViewModel.UiState.Success
        assertThat(success.items).hasSize(1)
        assertThat(success.items[0].title).isEqualTo("阅读")
    }
}
