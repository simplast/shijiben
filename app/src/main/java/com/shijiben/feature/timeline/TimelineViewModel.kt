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

    // 当天事件列表（响应式）
    val events: StateFlow<List<EventEntity>> = _viewingDate
        .flatMapLatest { (y, m, d) ->
            eventRepository.getEventsByDate(y, m, d)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 当天随笔列表（响应式）
    val notes: StateFlow<List<NoteEntity>> = _viewingDate
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
        val s = cal.timeInMillis
        return Pair(s, s + 24L * 3600 * 1000)
    }
}
