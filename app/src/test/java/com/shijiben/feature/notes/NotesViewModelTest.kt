package com.shijiben.feature.notes

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.repository.NoteRepository
import com.shijiben.feature.recording.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * NotesViewModel 单测。
 *
 * 逐字复用 HeatmapViewModelTest / SearchViewModelTest 的方案 A 范式（迭代 9 flaky 根治）：
 * - rule 传 StandardTestDispatcher，把 WhileSubscribed 订阅抖动队列串行化。
 * - runTest(mainRule.dispatcher) 统一 TestScope 与 viewModelScope 调度器。
 * - 每个测试常驻 backgroundScope.launch{ vm.allNotes.collect{} } 保活 WhileSubscribed(5000)，
 *   让 first{} 临时订阅不触发 grace 抖动。
 * - 用 vm.allNotes.first{...}（suspending wait）等待 Room Flow 初始查询 / invalidation 重发落定。
 * - setup() 把 Room query/transaction executor 路由到 mainRule.dispatcher，
 *   消除 teardown 阶段 invalidation tracker 残留任务竞态。
 *
 * 收集策略差异（NotesViewModel 特有）：
 * - allNotes 是 stateIn(WhileSubscribed(5000)) → 需常驻收集者 + first{} 等待。
 * - editing / sheetOpen 是 asStateFlow() → .value 可同步直读，无需 first{}。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotesViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var noteRepo: NoteRepository
    private lateinit var vm: NotesViewModel

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
        noteRepo = NoteRepository(db.noteDao())
        vm = NotesViewModel(noteRepo)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun startCreate_opensSheetAndEditingNull() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.allNotes.collect {} }
        // 前置断言：初始状态
        assertThat(vm.sheetOpen.value).isFalse()
        assertThat(vm.editing.value).isNull()
        assertThat(vm.allNotes.first { true }).isEmpty()
        // 动作
        vm.startCreate()
        // 后置断言
        assertThat(vm.sheetOpen.value).isTrue()
        assertThat(vm.editing.value).isNull()
    }

    @Test
    fun startEdit_opensSheetAndEditingSet() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.allNotes.collect {} }
        val note = NoteEntity(
            id = 0,
            content = "已有随笔",
            timestamp = 1000L,
            createdAt = 0L,
            updatedAt = 0L
        )
        vm.startEdit(note)
        assertThat(vm.sheetOpen.value).isTrue()
        assertThat(vm.editing.value).isEqualTo(note)
    }

    @Test
    fun closeSheet_closesSheetAndClearsEditing() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.allNotes.collect {} }
        val note = NoteEntity(
            id = 0,
            content = "已有随笔",
            timestamp = 1000L,
            createdAt = 0L,
            updatedAt = 0L
        )
        vm.startEdit(note)
        assertThat(vm.sheetOpen.value).isTrue()
        assertThat(vm.editing.value).isEqualTo(note)
        // 动作
        vm.closeSheet()
        // 后置断言
        assertThat(vm.sheetOpen.value).isFalse()
        assertThat(vm.editing.value).isNull()
    }

    @Test
    fun save_newNote_persistsAndClosesSheet() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.allNotes.collect {} }
        // 空白内容早返回 false，状态不变（sheet 未开仍为 false）
        val blankResult = vm.save("   ")
        assertThat(blankResult).isFalse()
        assertThat(vm.sheetOpen.value).isFalse()
        assertThat(vm.editing.value).isNull()
        // 有效内容走新建分支（editing 为 null）
        val okResult = vm.save("随笔内容")
        assertThat(okResult).isTrue()
        // 等待 Room Flow 重发，列表含一条且 content 匹配
        val list = vm.allNotes.first { it.isNotEmpty() }
        assertThat(list).hasSize(1)
        assertThat(list.first().content).isEqualTo("随笔内容")
        // sheet 关闭、editing 清空
        assertThat(vm.sheetOpen.value).isFalse()
        assertThat(vm.editing.value).isNull()
    }

    @Test
    fun save_editExisting_updatesAndClosesSheet() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.allNotes.collect {} }
        // 先插入一条随笔，取回完整实体
        val id = noteRepo.createNote(content = "原始内容", timestamp = 1000L)
        val existing = noteRepo.getNoteById(id)!!
        vm.allNotes.first { it.isNotEmpty() }
        // 进入编辑态
        vm.startEdit(existing)
        assertThat(vm.sheetOpen.value).isTrue()
        // 动作：保存新内容（走 updateNote 分支）
        val okResult = vm.save("更新内容")
        assertThat(okResult).isTrue()
        // 等待 Room Flow 重发，列表中该 id 的 content 已更新
        val list = vm.allNotes.first {
            it.any { n -> n.id == existing.id && n.content == "更新内容" }
        }
        assertThat(list).hasSize(1)
        assertThat(list.first().content).isEqualTo("更新内容")
        // sheet 关闭、editing 清空
        assertThat(vm.sheetOpen.value).isFalse()
        assertThat(vm.editing.value).isNull()
    }

    @Test
    fun delete_removesFromDbAndClosesSheet() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.allNotes.collect {} }
        // 先插入一条随笔，取回完整实体
        val id = noteRepo.createNote(content = "待删除随笔", timestamp = 1000L)
        val note = noteRepo.getNoteById(id)!!
        vm.allNotes.first { it.isNotEmpty() }
        // 打开 sheet
        vm.startEdit(note)
        assertThat(vm.sheetOpen.value).isTrue()
        // 动作：删除
        vm.delete(note)
        // 等待 Room Flow 重发，列表为空（不含该 id）
        vm.allNotes.first { it.isEmpty() }
        assertThat(vm.allNotes.first { true }).isEmpty()
        // sheet 关闭、editing 清空
        assertThat(vm.sheetOpen.value).isFalse()
        assertThat(vm.editing.value).isNull()
    }
}
