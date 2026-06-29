package com.shijiben.feature.search

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
 * SearchViewModel 单测。
 *
 * 逐字复用 HeatmapViewModelTest 的方案 A 范式（迭代 9 flaky 根治）：
 * - rule 传 StandardTestDispatcher，把 combine 切换 + WhileSubscribed 订阅抖动队列串行化。
 * - runTest(mainRule.dispatcher) 统一 TestScope 与 viewModelScope 调度器。
 * - 每个测试常驻 backgroundScope.launch{ state.collect{} } 保活 WhileSubscribed(5000)。
 * - 用 vm.state.first{...}（suspending wait）等待 Room Flow 初始查询落定。
 * - setup() 把 Room query/transaction executor 路由到 mainRule.dispatcher，
 *   消除 teardown 阶段 invalidation tracker 残留任务竞态。
 * - 注入真实 EventRepository(db.eventDao()) + NoteRepository(db.noteDao())，非 fake——验证端到端 Flow 链路。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SearchViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var noteRepo: NoteRepository
    private lateinit var vm: SearchViewModel

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
        noteRepo = NoteRepository(db.noteDao())
        vm = SearchViewModel(eventRepo, noteRepo)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun emptyQuery_returnsRecentRecordsLimited() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        // 插入 60 条事件 + 60 条随笔 → 空查询应返回最多 50 条
        for (i in 1..60) {
            eventRepo.createEvent("事件$i", startTime = i.toLong(), endTime = i.toLong() + 1, note = null)
            noteRepo.createNote("随笔$i", timestamp = i.toLong() + 500_000L)
        }
        // 等待 state 落定（items 非空 + 不再变化）
        val s = vm.state.first { it.items.isNotEmpty() }
        assertThat(s.items.size).isAtMost(SearchViewModel.RECENT_LIMIT)
        // 倒序验证
        val sortKeys = s.items.map { it.sortKey }
        for (i in 1 until sortKeys.size) {
            assertThat(sortKeys[i - 1]).isAtLeast(sortKeys[i])
        }
    }

    @Test
    fun emptyQuery_mixesEventsAndNotes() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("混合事件", startTime = 1000L, endTime = 2000L, note = null)
        noteRepo.createNote("混合随笔", timestamp = 3000L)
        val s = vm.state.first { it.items.isNotEmpty() }
        val hasEvent = s.items.any { it is SearchViewModel.SearchItem.EventItem }
        val hasNote = s.items.any { it is SearchViewModel.SearchItem.NoteItem }
        assertThat(hasEvent).isTrue()
        assertThat(hasNote).isTrue()
    }

    @Test
    fun titleMatch_returnsEvent() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("跑步锻炼", startTime = 1000L, endTime = 2000L, note = null)
        eventRepo.createEvent("其他事件", startTime = 3000L, endTime = 4000L, note = null)
        // 等待 state 初始落定（含两条事件）
        vm.state.first { it.items.size >= 2 }
        vm.onQueryChange("跑步")
        val s = vm.state.first { it.query == "跑步" && it.items.isNotEmpty() }
        assertThat(s.items).hasSize(1)
        assertThat(s.items.first()).isInstanceOf(SearchViewModel.SearchItem.EventItem::class.java)
        assertThat((s.items.first() as SearchViewModel.SearchItem.EventItem).event.title).isEqualTo("跑步锻炼")
    }

    @Test
    fun eventNoteMatch_returnsEvent() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        // title 不含 "日记"，note 含 "日记"
        eventRepo.createEvent("abc", startTime = 1000L, endTime = 2000L, note = "写日记")
        eventRepo.createEvent("def", startTime = 3000L, endTime = 4000L, note = null)
        vm.state.first { it.items.size >= 2 }
        vm.onQueryChange("日记")
        val s = vm.state.first { it.query == "日记" && it.items.isNotEmpty() }
        assertThat(s.items).hasSize(1)
        assertThat(s.items.first()).isInstanceOf(SearchViewModel.SearchItem.EventItem::class.java)
        assertThat((s.items.first() as SearchViewModel.SearchItem.EventItem).event.note).isEqualTo("写日记")
    }

    @Test
    fun noteContentMatch_returnsNote() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        noteRepo.createNote("今天心情不错", timestamp = 1000L)
        noteRepo.createNote("无关内容", timestamp = 2000L)
        vm.state.first { it.items.size >= 2 }
        vm.onQueryChange("心情")
        val s = vm.state.first { it.query == "心情" && it.items.isNotEmpty() }
        assertThat(s.items).hasSize(1)
        assertThat(s.items.first()).isInstanceOf(SearchViewModel.SearchItem.NoteItem::class.java)
        assertThat((s.items.first() as SearchViewModel.SearchItem.NoteItem).note.content).isEqualTo("今天心情不错")
    }

    @Test
    fun multiFieldMatch_returnsFromMultipleSources() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("match 事件", startTime = 1000L, endTime = 2000L, note = null)
        noteRepo.createNote("match 随笔", timestamp = 3000L)
        vm.state.first { it.items.size >= 2 }
        vm.onQueryChange("match")
        val s = vm.state.first { it.query == "match" && it.items.size >= 2 }
        assertThat(s.items).hasSize(2)
        assertThat(s.items.any { it is SearchViewModel.SearchItem.EventItem }).isTrue()
        assertThat(s.items.any { it is SearchViewModel.SearchItem.NoteItem }).isTrue()
    }

    @Test
    fun caseInsensitive_match() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("abc task", startTime = 1000L, endTime = 2000L, note = null)
        noteRepo.createNote("ABC diary", timestamp = 3000L)
        noteRepo.createNote("Abc memo", timestamp = 4000L)
        vm.state.first { it.items.size >= 3 }
        vm.onQueryChange("ABC")
        val s = vm.state.first { it.query == "ABC" && it.items.size >= 3 }
        assertThat(s.items).hasSize(3)
    }

    @Test
    fun noMatch_emptyState() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("abc", startTime = 1000L, endTime = 2000L, note = null)
        noteRepo.createNote("xyz", timestamp = 3000L)
        vm.state.first { it.items.size >= 2 }
        vm.onQueryChange("不存在的词")
        val s = vm.state.first { it.query == "不存在的词" && it.isEmpty }
        assertThat(s.items).isEmpty()
        assertThat(s.isEmpty).isTrue()
    }

    @Test
    fun resultsSortedDescending() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("match 1", startTime = 1000L, endTime = 2000L, note = null)
        eventRepo.createEvent("match 2", startTime = 5000L, endTime = 6000L, note = null)
        noteRepo.createNote("match 3", timestamp = 3000L)
        vm.state.first { it.items.size >= 3 }
        vm.onQueryChange("match")
        val s = vm.state.first { it.query == "match" && it.items.size >= 3 }
        assertThat(s.items).hasSize(3)
        // 倒序：5000 > 3000 > 1000
        assertThat(s.items[0].sortKey).isEqualTo(5000L)
        assertThat(s.items[1].sortKey).isEqualTo(3000L)
        assertThat(s.items[2].sortKey).isEqualTo(1000L)
    }

    @Test
    fun queryChange_updatesResults() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("xyz 事件", startTime = 1000L, endTime = 2000L, note = null)
        noteRepo.createNote("abc 随笔", timestamp = 3000L)
        vm.state.first { it.items.size >= 2 }
        // 先输入 "xyz" → 只命中事件
        vm.onQueryChange("xyz")
        val s1 = vm.state.first { it.query == "xyz" && it.items.isNotEmpty() }
        assertThat(s1.items).hasSize(1)
        assertThat(s1.items.first()).isInstanceOf(SearchViewModel.SearchItem.EventItem::class.java)
        // 改为 "abc" → 只命中随笔
        vm.onQueryChange("abc")
        val s2 = vm.state.first { it.query == "abc" && it.items.isNotEmpty() }
        assertThat(s2.items).hasSize(1)
        assertThat(s2.items.first()).isInstanceOf(SearchViewModel.SearchItem.NoteItem::class.java)
    }

    @Test
    fun dbChange_updatesResults() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        // 先插入一条匹配 "keyword" 的事件
        eventRepo.createEvent("keyword 事件1", startTime = 1000L, endTime = 2000L, note = null)
        vm.state.first { it.items.isNotEmpty() }
        vm.onQueryChange("keyword")
        val s1 = vm.state.first { it.query == "keyword" && it.items.size >= 1 }
        assertThat(s1.items).hasSize(1)
        // 插入新事件匹配当前 query → state.items 自动新增（Flow 响应式）
        eventRepo.createEvent("keyword 事件2", startTime = 5000L, endTime = 6000L, note = null)
        val s2 = vm.state.first { it.items.size >= 2 }
        assertThat(s2.items).hasSize(2)
    }

    @Test
    fun queryWithSpaces_trimmedBeforeMatch() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("abc 事件", startTime = 1000L, endTime = 2000L, note = null)
        vm.state.first { it.items.isNotEmpty() }
        vm.onQueryChange("  abc  ")
        val s = vm.state.first { it.query == "  abc  " && it.items.isNotEmpty() }
        assertThat(s.items).hasSize(1)
        assertThat((s.items.first() as SearchViewModel.SearchItem.EventItem).event.title).isEqualTo("abc 事件")
    }

    @Test
    fun specialChars_matchedLiterally() = runTest(mainRule.dispatcher) {
        backgroundScope.launch { vm.state.collect {} }
        eventRepo.createEvent("完成 100% 目标", startTime = 1000L, endTime = 2000L, note = null)
        noteRepo.createNote("a_b 命名测试", timestamp = 3000L)
        noteRepo.createNote("反斜杠 \\ 路径", timestamp = 4000L)
        vm.state.first { it.items.size >= 3 }
        // "100%" 按字面量匹配
        vm.onQueryChange("100%")
        val s1 = vm.state.first { it.query == "100%" && it.items.isNotEmpty() }
        assertThat(s1.items).hasSize(1)
        assertThat(s1.items.first()).isInstanceOf(SearchViewModel.SearchItem.EventItem::class.java)
        // "a_b" 按字面量匹配（非 SQL LIKE 通配）
        vm.onQueryChange("a_b")
        val s2 = vm.state.first { it.query == "a_b" && it.items.isNotEmpty() }
        assertThat(s2.items).hasSize(1)
        assertThat(s2.items.first()).isInstanceOf(SearchViewModel.SearchItem.NoteItem::class.java)
        // "\\" 按字面量匹配
        vm.onQueryChange("\\")
        val s3 = vm.state.first { it.query == "\\" && it.items.isNotEmpty() }
        assertThat(s3.items).hasSize(1)
        assertThat(s3.items.first()).isInstanceOf(SearchViewModel.SearchItem.NoteItem::class.java)
    }
}
