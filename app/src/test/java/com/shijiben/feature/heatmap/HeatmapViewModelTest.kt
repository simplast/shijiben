package com.shijiben.feature.heatmap

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.repository.EventRepository
import com.shijiben.test.MainCoroutineRule
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
import java.time.YearMonth
import java.time.ZoneId

/**
 * HeatmapViewModel 单测。
 *
 * flaky 修复（迭代 4 spec §4.1 + §8.2 备选）：
 * - rule 传 StandardTestDispatcher（不修改 MainCoroutineRule.kt），把 flatMapLatest 切换 +
 *   WhileSubscribed 订阅抖动从重入式（Unconfined）改为队列串行化，消除 IllegalStateException。
 * - runTest(mainRule.dispatcher) 统一 TestScope 与 viewModelScope 调度器，避免跨队列死等。
 * - 每个测试常驻 backgroundScope.launch{ state.collect{} } 保活 WhileSubscribed(5000)，
 *   让 first{} 临时订阅不触发 grace 抖动。
 * - 用 vm.state.first{...}（suspending wait）替代 advanceUntilIdle() + state.value：
 *   Room Flow 的初始查询在 Room 自己的 executor（真实线程）上异步执行，advanceUntilIdle()
 *   只推进 StandardTestDispatcher 队列不会等待 Room 线程；first{} 挂起测试体后 runBlocking
 *   会处理 Room 线程 dispatch 回来的任务，确定性等待真实 state 落定。
 *   （§4.1.3 原方案 advanceUntilIdle()+state.value 在 Room Flow 下读到占位值；§8.2 备选 first{}。）
 *
 * flaky 根治（迭代 9 spec §4.2 方案 A）：
 * - 迭代 4 解决了"等待阶段"时序，但未解决 teardown 阶段竞态：WhileSubscribed(5000) grace
 *   period 使上游 Room Flow 在测试结束后仍存活 5s，Room invalidation tracker 在真实
 *   executor 线程触发 re-query 并 dispatch 到 viewModelScope（Main），而此时
 *   MainCoroutineRule.finished() 已调 resetMain() 使 Main 变为 NoopDispatcher →
 *   IllegalStateException at TestMainDispatcher.kt:67。
 * - 方案 A：把 Room query/transaction executor 路由到 mainRule.dispatcher
 *   （StandardTestDispatcher）。Room 所有任务（含 invalidation tracker refreshRunnable）
 *   入同一队列，teardown 后队列不再被 advance → 残留任务永不执行 → 竞态窗口从根上消除。
 *   仅改 setup()，不改 MainCoroutineRule / 生产代码。详见 setup() 内联注释。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class HeatmapViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var vm: HeatmapViewModel

    private val zone = ZoneId.systemDefault()

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        // 迭代9 flaky 根治（方案 A）：把 Room 的 query/transaction executor 路由到
        // mainRule.dispatcher（StandardTestDispatcher）。
        // 根因链：WhileSubscribed(5000) grace period 使上游 Room Flow 在测试结束后仍存活 5s，
        // Room invalidation tracker 在真实 executor 线程触发 re-query 并 dispatch 到
        // viewModelScope（Main），而此时 MainCoroutineRule.finished() 已调 resetMain()
        // 使 Main 变为 NoopDispatcher → IllegalStateException at TestMainDispatcher.kt:67。
        // 路由后 Room 所有任务（含 invalidation tracker refreshRunnable）入同一队列，
        // teardown 后队列不再被 advance → 残留任务永不执行 → 竞态窗口从根上消除。
        // （Room 2.6.1 refreshRunnable 经 database.getQueryExecutor().execute(...) 调度，
        // setQueryExecutor/setTransactionExecutor 直接赋值 internalQueryExecutor 字段。）
        val roomExecutor = java.util.concurrent.Executor { command ->
            mainRule.dispatcher.dispatch(kotlin.coroutines.EmptyCoroutineContext, command)
        }
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .setQueryExecutor(roomExecutor)
            .setTransactionExecutor(roomExecutor)
            .allowMainThreadQueries()
            .build()
        eventRepo = EventRepository(db.eventDao())
        vm = HeatmapViewModel(eventRepo)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun initialState_isCurrentMonth() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        // 等待 Room Flow 初始查询落定（占位 state 的 cells 为空）
        vm.state.first { it.cells.isNotEmpty() }
        val s = vm.state.value
        assertThat(s.yearMonth).isEqualTo(YearMonth.now(zone))
        assertThat(s.isCurrentMonth).isTrue()
        assertThat(s.canGoNext).isFalse()
    }

    @Test
    fun previousMonth_decrementsAndEnablesNext() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.cells.isNotEmpty() }
        vm.previousMonth()
        vm.state.first { !it.isCurrentMonth }
        val s = vm.state.value
        assertThat(s.yearMonth).isEqualTo(YearMonth.now(zone).minusMonths(1))
        assertThat(s.isCurrentMonth).isFalse()
        assertThat(s.canGoNext).isTrue()
    }

    @Test
    fun nextMonth_fromPreviousMonth_returnsToCurrent() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.cells.isNotEmpty() }
        vm.previousMonth()
        vm.state.first { !it.isCurrentMonth }
        assertThat(vm.state.value.isCurrentMonth).isFalse()
        vm.nextMonth()
        vm.state.first { it.isCurrentMonth }
        assertThat(vm.state.value.yearMonth).isEqualTo(YearMonth.now(zone))
        assertThat(vm.state.value.canGoNext).isFalse()
    }

    @Test
    fun nextMonth_atCurrentMonth_doesNotAdvance() = runTest(mainRule.dispatcher) {
        // B2：验证 currentMonth 计算属性在操作时取最新——当前月 nextMonth() 不前进
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.cells.isNotEmpty() }
        val before = vm.state.value
        vm.nextMonth()
        advanceUntilIdle()
        val after = vm.state.value
        assertThat(after.yearMonth).isEqualTo(before.yearMonth)
        assertThat(after.canGoNext).isFalse()
    }

    @Test
    fun goToCurrentMonth_fromPrevious_returnsToCurrent() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.cells.isNotEmpty() }
        vm.previousMonth()
        vm.state.first { !it.isCurrentMonth }
        assertThat(vm.state.value.isCurrentMonth).isFalse()
        vm.goToCurrentMonth()
        vm.state.first { it.isCurrentMonth }
        assertThat(vm.state.value.yearMonth).isEqualTo(YearMonth.now(zone))
        assertThat(vm.state.value.canGoNext).isFalse()
    }

    @Test
    fun stateCells_shapeIs6x7AndTodayMarked() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.cells.isNotEmpty() }
        val s = vm.state.value
        assertThat(s.cells).hasSize(6)
        for (row in s.cells) assertThat(row).hasSize(7)
        val todayCells = s.cells.flatten().filter { it.isToday }
        assertThat(todayCells).hasSize(1)
        assertThat(todayCells.first().date).isEqualTo(LocalDate.now(zone))
    }

    @Test
    fun stateCells_updatesWhenRepoEmitsNewData() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        vm.state.first { it.cells.isNotEmpty() }
        // 插入今天 00:00-01:00 的 1h 事件（Completed）
        val today = LocalDate.now(zone)
        val startMs = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val endMs = startMs + 3600_000L
        eventRepo.createEvent("x", startMs, endMs, null)
        // 等待 Room invalidation 触发 Flow 重发，今日格出现非零时长
        vm.state.first { it.cells.flatten().any { c -> c.isToday && c.durationMs > 0 } }

        val s = vm.state.value
        val todayCell = s.cells.flatten().first { it.isToday }
        assertThat(todayCell.level).isEqualTo(1)
        assertThat(todayCell.durationMs).isEqualTo(3600_000L)
    }
}
