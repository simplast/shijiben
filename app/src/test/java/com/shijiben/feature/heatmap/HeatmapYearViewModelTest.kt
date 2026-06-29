package com.shijiben.feature.heatmap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.repository.EventRepository
import com.shijiben.feature.recording.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId

/**
 * HeatmapYearViewModel 单测。
 *
 * 逐字复用 HeatmapViewModelTest 的方案 A 范式（迭代 9 flaky 根治）：
 * - rule 传 StandardTestDispatcher，把 flatMapLatest 切换 + WhileSubscribed 订阅抖动队列串行化。
 * - runTest(mainRule.dispatcher) 统一 TestScope 与 viewModelScope 调度器。
 * - 每个测试常驻 backgroundScope.launch{ state.collect{} } 保活 WhileSubscribed(5000)。
 * - 用 vm.state.first{...}（suspending wait）等待 Room Flow 初始查询落定。
 * - setup() 把 Room query/transaction executor 路由到 mainRule.dispatcher，
 *   消除 teardown 阶段 invalidation tracker 残留任务竞态。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HeatmapYearViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var vm: HeatmapYearViewModel

    private val zone = ZoneId.systemDefault()

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // 方案 A：把 Room 的 query/transaction executor 路由到 mainRule.dispatcher
        // （StandardTestDispatcher）。Room 所有任务（含 invalidation tracker refreshRunnable）
        // 入同一队列，teardown 后队列不再被 advance → 残留任务永不执行 → 竞态窗口消除。
        val roomExecutor = java.util.concurrent.Executor { command ->
            mainRule.dispatcher.dispatch(kotlin.coroutines.EmptyCoroutineContext, command)
        }
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .setQueryExecutor(roomExecutor)
            .setTransactionExecutor(roomExecutor)
            .allowMainThreadQueries()
            .build()
        eventRepo = EventRepository(db.eventDao())
        vm = HeatmapYearViewModel(eventRepo)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun initialState_isCurrentYear() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        val s = vm.state.value
        assertThat(s.year).isEqualTo(Year.now(zone))
        assertThat(s.isCurrentYear).isTrue()
        assertThat(s.canGoNext).isFalse()
    }

    @Test
    fun previousYear_decrementsAndEnablesNext() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        vm.previousYear()
        vm.state.first { !it.isCurrentYear }
        val s = vm.state.value
        assertThat(s.year).isEqualTo(Year.now(zone).minusYears(1))
        assertThat(s.isCurrentYear).isFalse()
        assertThat(s.canGoNext).isTrue()
    }

    @Test
    fun nextYear_fromPreviousYear_returnsToCurrent() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        vm.previousYear()
        vm.state.first { !it.isCurrentYear }
        assertThat(vm.state.value.isCurrentYear).isFalse()
        vm.nextYear()
        vm.state.first { it.isCurrentYear }
        assertThat(vm.state.value.year).isEqualTo(Year.now(zone))
        assertThat(vm.state.value.canGoNext).isFalse()
    }

    @Test
    fun nextYear_atCurrentYear_doesNotAdvance() = runTest(mainRule.dispatcher) {
        // 当前年 nextYear() 不前进
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        val before = vm.state.value
        vm.nextYear()
        advanceUntilIdle()
        val after = vm.state.value
        assertThat(after.year).isEqualTo(before.year)
        assertThat(after.canGoNext).isFalse()
    }

    @Test
    fun goToCurrentYear_fromPrevious_returnsToCurrent() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        vm.previousYear()
        vm.state.first { !it.isCurrentYear }
        assertThat(vm.state.value.isCurrentYear).isFalse()
        vm.goToCurrentYear()
        vm.state.first { it.isCurrentYear }
        assertThat(vm.state.value.year).isEqualTo(Year.now(zone))
        assertThat(vm.state.value.canGoNext).isFalse()
    }

    @Test
    fun yearGrid_shapeIs12Months() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        val s = vm.state.value
        assertThat(s.months).hasSize(12)
        for (mg in s.months) {
            assertThat(mg.cells).hasSize(6)
            for (row in mg.cells) assertThat(row).hasSize(7)
        }
    }

    @Test
    fun yearGrid_todayMarkedExactlyOnce() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        val s = vm.state.value
        val todayCells = s.months.flatMap { it.cells.flatten() }.filter { it.isToday }
        assertThat(todayCells).hasSize(1)
        assertThat(todayCells.first().date).isEqualTo(LocalDate.now(zone))
    }

    @Test
    fun yearGrid_updatesWhenRepoEmitsNewData() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.months.isNotEmpty() }
        // 插入今天 00:00-01:00 的 1h 事件（Completed）
        val today = LocalDate.now(zone)
        val startMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = startMs + 3600_000L
        eventRepo.createEvent("x", startMs, endMs, null)
        // 等待 Room invalidation 触发 Flow 重发，今日格出现非零时长
        vm.state.first {
            it.months.flatMap { mg -> mg.cells.flatten() }
                .any { c -> c.isToday && c.durationMs > 0 }
        }
        val s = vm.state.value
        val todayCell = s.months.flatMap { it.cells.flatten() }.first { it.isToday }
        assertThat(todayCell.level).isEqualTo(1)
        assertThat(todayCell.durationMs).isEqualTo(3600_000L)
    }
}
