package com.shijiben.feature.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TimelineViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    // 当前查看的日期（年/月/日）
    private val _viewingDate = MutableStateFlow(today())
    val viewingDate: StateFlow<Triple<Int, Int, Int>> = _viewingDate.asStateFlow()

    // 显式刷新触发器：保存事件后由 UI 调用，强制重拉当天事件/随笔流
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

    // 当天事件列表（响应式）。date 与 refreshTrigger 任一变化都重新拉取。
    val events: StateFlow<List<EventEntity>> = combine(_viewingDate, _refreshTrigger) { d, _ -> d }
        .flatMapLatest { (y, m, d) ->
            eventRepository.getEventsByDate(y, m, d)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当天随笔列表（响应式）
    val notes: StateFlow<List<NoteEntity>> = combine(_viewingDate, _refreshTrigger) { d, _ -> d }
        .flatMapLatest { (y, m, d) ->
            val (start, end) = dayRange(y, m, d)
            noteRepository.getNotesByDateRange(start, end)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // App 启动时自动顺延未开始事件到今天
        viewModelScope.launch {
            val (y, m, d) = today()
            eventRepository.carryOverNotStarted(y, m, d)
        }
    }

    fun goToToday() { _viewingDate.value = today() }
    fun goToPreviousDay() { shiftDay(-1) }
    fun goToNextDay() { shiftDay(1) }
    fun setDate(year: Int, month: Int, day: Int) {
        _viewingDate.value = Triple(year, month, day)
    }

    /**
     * 显式刷新当前查看日期的事件/随笔。保存事件/随笔后由 UI 调用，
     * 保证冷启动 / 弱订阅场景下也能立即拉到新数据。
     */
    fun refresh() {
        _refreshTrigger.value = _refreshTrigger.value + 1
    }

    /** 快速添加一条无时间的事件（底部输入框直接确认） */
    fun quickAddEvent(title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            eventRepository.createEvent(
                title = title.trim(),
                startTime = now,
                endTime = null,
                note = null,
                status = com.shijiben.data.model.EventStatus.NotStarted.value
            )
            refresh()
        }
    }

    /** 开始事件：设置 startTime 为当前时间，进入进行中状态 */
    fun markInProgress(eventId: Long) {
        viewModelScope.launch {
            val event = eventRepository.getEventById(eventId) ?: return@launch
            val now = System.currentTimeMillis()
            eventRepository.updateEvent(event.copy(
                startTime = now,
                endTime = null,
                status = com.shijiben.data.model.EventStatus.InProgress.value,
                updatedAt = now
            ))
            refresh()
        }
    }

    /** 停止事件：设置 endTime 为当前时间，标记完成 */
    fun markCompleted(eventId: Long) {
        viewModelScope.launch {
            val event = eventRepository.getEventById(eventId) ?: return@launch
            val now = System.currentTimeMillis()
            eventRepository.updateEvent(event.copy(
                endTime = now,
                status = com.shijiben.data.model.EventStatus.Completed.value,
                updatedAt = now
            ))
            refresh()
        }
    }

    /** 删除事件（列表长按触发） */
    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            eventRepository.deleteEventById(eventId)
            refresh()
        }
    }

    private fun shiftDay(delta: Int) {
        val (y, m, d) = _viewingDate.value
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(y, m - 1, d, 0, 0, 0)
        cal.add(Calendar.DAY_OF_MONTH, delta)
        _viewingDate.value = Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun today(): Triple<Int, Int, Int> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        return Triple(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    private fun dayRange(y: Int, m: Int, d: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(y, m - 1, d, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60 * 60 * 1000
        return Pair(start, end)
    }
}
