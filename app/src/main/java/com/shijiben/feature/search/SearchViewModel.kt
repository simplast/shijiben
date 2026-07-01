package com.shijiben.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    /** 搜索结果统一抽象，与 TimelineItem 同模式 */
    sealed interface SearchItem {
        val sortKey: Long
        data class EventItem(val event: EventEntity) : SearchItem {
            override val sortKey: Long get() = event.startTime
        }
        data class NoteItem(val note: NoteEntity) : SearchItem {
            override val sortKey: Long get() = note.timestamp
        }
    }

    data class SearchUiState(
        val query: String = "",
        val items: List<SearchItem> = emptyList(),
        val isEmpty: Boolean = true   // items 为空（用于区分"无结果"空状态）
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(q: String) { _query.value = q }

    private val eventsFlow: StateFlow<List<EventEntity>> =
        eventRepository.getAllEvents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val notesFlow: StateFlow<List<NoteEntity>> =
        noteRepository.getAllNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 防抖查询流：每次按键触发一次完整 filter+merge+sort（O(N+M) 字符串分配 + 全量排序），
    // 连续输入时浪费严重。debounce(150ms) 等用户停顿再触发；distinctUntilChanged 防止
    // 相同 query 重复触发（如输入后删除回原值）。UI 文本框仍绑定 `query` 即时显示。
    @OptIn(FlowPreview::class)
    private val debouncedQuery: StateFlow<String> = _query
        .debounce(150L)
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val state: StateFlow<SearchUiState> =
        combine(eventsFlow, notesFlow, debouncedQuery) { events, notes, q ->
            val items = filterAndMerge(events, notes, q)
            SearchUiState(query = q, items = items, isEmpty = items.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    companion object {
        const val RECENT_LIMIT = 50
    }
}

/**
 * 纯函数：过滤 + 合并 + 排序。internal 供单测直接调（与 aggregateMonth 同模式）。
 * 无 Android 依赖，纯 JUnit 可测。
 */
internal fun filterAndMerge(
    events: List<EventEntity>,
    notes: List<NoteEntity>,
    query: String,
    recentLimit: Int = SearchViewModel.RECENT_LIMIT
): List<SearchViewModel.SearchItem> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) {
        // 空查询：最近 N 条（事件+随笔混合，按 sortKey 倒序）
        (events.map { SearchViewModel.SearchItem.EventItem(it) } +
         notes.map { SearchViewModel.SearchItem.NoteItem(it) })
            .sortedByDescending { it.sortKey }
            .take(recentLimit)
    } else {
        val q = trimmed.lowercase()
        val matchedEvents = events.filter {
            it.title.lowercase().contains(q) ||
            (it.note?.lowercase()?.contains(q) == true)
        }.map { SearchViewModel.SearchItem.EventItem(it) }
        val matchedNotes = notes.filter {
            it.content.lowercase().contains(q)
        }.map { SearchViewModel.SearchItem.NoteItem(it) }
        (matchedEvents + matchedNotes).sortedByDescending { it.sortKey }
    }
}
