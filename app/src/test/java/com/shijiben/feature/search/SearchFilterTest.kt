package com.shijiben.feature.search

import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * filterAndMerge 纯函数测试（无 Room / Robolectric 依赖，纯 JUnit）。
 * 与 EventRepositoryHeatmapTest 测 aggregateMonth 同模式：直接调 internal 纯函数。
 */
class SearchFilterTest {

    private fun event(
        id: Long = 0,
        title: String = "title",
        startTime: Long = 0L,
        note: String? = null,
        status: Int = 2,
        endTime: Long? = startTime + 1000L
    ): EventEntity = EventEntity(
        id = id, title = title, startTime = startTime, endTime = endTime,
        status = status, note = note, createdAt = startTime, updatedAt = startTime
    )

    private fun note(
        id: Long = 0,
        content: String = "content",
        timestamp: Long = 0L
    ): NoteEntity = NoteEntity(
        id = id, content = content, timestamp = timestamp,
        createdAt = timestamp, updatedAt = timestamp
    )

    @Test
    fun emptyQuery_returnsRecentLimitedAndSorted() {
        // 输入 < recentLimit 条，全部返回，倒序
        val events = listOf(
            event(id = 1, startTime = 1000L),
            event(id = 2, startTime = 3000L)
        )
        val notes = listOf(note(id = 10, timestamp = 2000L))
        val items = filterAndMerge(events, notes, query = "", recentLimit = 50)
        assertThat(items).hasSize(3)
        // 倒序：3000 > 2000 > 1000
        assertThat(items[0].sortKey).isEqualTo(3000L)
        assertThat(items[1].sortKey).isEqualTo(2000L)
        assertThat(items[2].sortKey).isEqualTo(1000L)
    }

    @Test
    fun emptyQuery_takeRecentLimit() {
        // 输入 > recentLimit 条 → 恰好 take recentLimit 条
        val events = (1..30).map { event(id = it.toLong(), startTime = it.toLong()) }
        val notes = (1..30).map { note(id = it.toLong() + 100, timestamp = it.toLong() + 1000) }
        val items = filterAndMerge(events, notes, query = "", recentLimit = 10)
        assertThat(items).hasSize(10)
        // 倒序：最大的 10 条 sortKey
        val sortKeys = items.map { it.sortKey }
        for (i in 1 until sortKeys.size) {
            assertThat(sortKeys[i - 1]).isAtLeast(sortKeys[i])
        }
        // 最大 sortKey 应为 30 + 1000 = 1030
        assertThat(sortKeys.first()).isEqualTo(1030L)
    }

    @Test
    fun titleMatch() {
        val e = event(id = 1, title = "跑步锻炼", startTime = 1000L)
        val items = filterAndMerge(listOf(e), emptyList(), query = "跑步")
        assertThat(items).hasSize(1)
        assertThat(items.first()).isInstanceOf(SearchViewModel.SearchItem.EventItem::class.java)
        assertThat((items.first() as SearchViewModel.SearchItem.EventItem).event.id).isEqualTo(1L)
    }

    @Test
    fun eventNoteMatch() {
        // note 命中（title 不命中）；note null 不崩
        val e1 = event(id = 1, title = "abc", startTime = 1000L, note = "写日记")
        val e2 = event(id = 2, title = "def", startTime = 2000L, note = null) // note null 不匹配，不崩
        val items = filterAndMerge(listOf(e1, e2), emptyList(), query = "日记")
        assertThat(items).hasSize(1)
        assertThat((items.first() as SearchViewModel.SearchItem.EventItem).event.id).isEqualTo(1L)
    }

    @Test
    fun noteContentMatch() {
        val n = note(id = 5, content = "今天心情不错", timestamp = 1000L)
        val items = filterAndMerge(emptyList(), listOf(n), query = "心情")
        assertThat(items).hasSize(1)
        assertThat(items.first()).isInstanceOf(SearchViewModel.SearchItem.NoteItem::class.java)
        assertThat((items.first() as SearchViewModel.SearchItem.NoteItem).note.id).isEqualTo(5L)
    }

    @Test
    fun caseInsensitive() {
        // query "ABC" 命中 title/content 含 "abc" / "ABC" / "Abc"
        val e = event(id = 1, title = "abc task", startTime = 1000L)
        val n1 = note(id = 10, content = "ABC diary", timestamp = 2000L)
        val n2 = note(id = 11, content = "Abc memo", timestamp = 3000L)
        val items = filterAndMerge(listOf(e), listOf(n1, n2), query = "ABC")
        assertThat(items).hasSize(3)
    }

    @Test
    fun noMatch_emptyList() {
        val e = event(id = 1, title = "abc", startTime = 1000L)
        val n = note(id = 10, content = "xyz", timestamp = 2000L)
        val items = filterAndMerge(listOf(e), listOf(n), query = "不存在的词")
        assertThat(items).isEmpty()
    }

    @Test
    fun sortedDescending() {
        // 多条命中按 sortKey 倒序
        val e1 = event(id = 1, title = "match 1", startTime = 1000L)
        val e2 = event(id = 2, title = "match 2", startTime = 5000L)
        val n = note(id = 10, content = "match 3", timestamp = 3000L)
        val items = filterAndMerge(listOf(e1, e2), listOf(n), query = "match")
        assertThat(items).hasSize(3)
        assertThat(items[0].sortKey).isEqualTo(5000L)
        assertThat(items[1].sortKey).isEqualTo(3000L)
        assertThat(items[2].sortKey).isEqualTo(1000L)
    }

    @Test
    fun specialCharsLiteral() {
        // "100%" / "a_b" / "\\" 按字面量子串匹配（非 SQL LIKE 通配）
        val e = event(id = 1, title = "完成 100% 目标", startTime = 1000L)
        val n1 = note(id = 10, content = "a_b 命名测试", timestamp = 2000L)
        val n2 = note(id = 11, content = "反斜杠 \\ 路径", timestamp = 3000L)
        assertThat(filterAndMerge(listOf(e), emptyList(), query = "100%")).hasSize(1)
        assertThat(filterAndMerge(emptyList(), listOf(n1), query = "a_b")).hasSize(1)
        assertThat(filterAndMerge(emptyList(), listOf(n2), query = "\\")).hasSize(1)
    }

    @Test
    fun nullEventNoteHandled() {
        // event.note = null 时 title 不匹配 → 不命中，不崩 NPE
        val e = event(id = 1, title = "abc", startTime = 1000L, note = null)
        val items = filterAndMerge(listOf(e), emptyList(), query = "xyz")
        assertThat(items).isEmpty()
    }

    @Test
    fun emptyQuery_emptyInputs_returnsEmptyList() {
        // 空查询 + 空 events + 空 notes → 空 list
        val items = filterAndMerge(emptyList(), emptyList(), query = "", recentLimit = 50)
        assertThat(items).isEmpty()
    }

    @Test
    fun whitespaceOnlyQuery_treatedAsEmpty() {
        // 纯空白 query（纯空格 / 空格+制表符）trim 后 isEmpty() → 走空查询分支
        val events = listOf(event(id = 1, startTime = 1000L))
        val notes = listOf(note(id = 10, timestamp = 2000L))
        val items1 = filterAndMerge(events, notes, query = "   ", recentLimit = 50)
        assertThat(items1).hasSize(2)
        assertThat(items1[0].sortKey).isEqualTo(2000L)
        assertThat(items1[1].sortKey).isEqualTo(1000L)
        val items2 = filterAndMerge(events, notes, query = "  \t  ", recentLimit = 50)
        assertThat(items2).hasSize(2)
        assertThat(items2[0].sortKey).isEqualTo(2000L)
        assertThat(items2[1].sortKey).isEqualTo(1000L)
    }

    @Test
    fun eventAndNoteSameSortKey_stableSort() {
        // EventItem 与 NoteItem 同 sortKey=1000L；events + notes 拼接 + sortedByDescending（TimSort 稳定）
        // → 同 key 保持原序，EventItem（左侧）在 NoteItem（右侧）之前
        val events = listOf(event(id = 1, startTime = 1000L))
        val notes = listOf(note(id = 10, timestamp = 1000L))
        val items = filterAndMerge(events, notes, query = "", recentLimit = 50)
        assertThat(items).hasSize(2)
        assertThat(items[0]).isInstanceOf(SearchViewModel.SearchItem.EventItem::class.java)
        assertThat((items[0] as SearchViewModel.SearchItem.EventItem).event.id).isEqualTo(1L)
        assertThat(items[1]).isInstanceOf(SearchViewModel.SearchItem.NoteItem::class.java)
        assertThat((items[1] as SearchViewModel.SearchItem.NoteItem).note.id).isEqualTo(10L)
    }

    @Test
    fun recentLimit_zero_returnsEmpty() {
        // take(0) 返回 emptyList()（Kotlin stdlib 契约）
        val events = listOf(
            event(id = 1, startTime = 1000L),
            event(id = 2, startTime = 2000L)
        )
        val notes = listOf(note(id = 10, timestamp = 3000L))
        val items = filterAndMerge(events, notes, query = "", recentLimit = 0)
        assertThat(items).isEmpty()
    }

    @Test
    fun recentLimit_negative_throwsIllegalArgumentException() {
        // take(-1) 触发 stdlib require(n >= 0) → 抛 IllegalArgumentException
        // 文档化 filterAndMerge 不 clamp 负数 recentLimit 的设计事实
        val events = listOf(event(id = 1, startTime = 1000L))
        assertThrows(IllegalArgumentException::class.java) {
            filterAndMerge(events, emptyList(), query = "", recentLimit = -1)
        }
    }
}
