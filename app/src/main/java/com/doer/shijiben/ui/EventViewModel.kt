package com.doer.shijiben.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class EventViewModel(
    private val repository: EventRepository,
) : ViewModel() {

    private val selectedDateFormatter =
        DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", java.util.Locale.CHINA)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val selectedDateLabel: StateFlow<String> =
        _selectedDate.map { it.format(selectedDateFormatter) }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "",
        )

    val eventsForSelectedDay: StateFlow<List<EventEntity>> =
        _selectedDate
            .flatMapLatest { date: LocalDate ->
                val dayKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.observeEventsForDay(dayKey)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )

    val recentDistinctEventNames: StateFlow<List<String>> =
        repository.observeRecentDistinctEventNames().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun upsert(event: EventEntity, onResult: (Throwable?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                require(event.endTimeMillis >= event.startTimeMillis) {
                    "结束时间不能早于开始时间"
                }
                repository.upsert(event)
                onResult(null)
            } catch (t: Throwable) {
                onResult(t)
            }
        }
    }

    suspend fun getEvent(id: Long): EventEntity? = repository.getById(id)

    fun delete(event: EventEntity, onResult: (Throwable?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.delete(event)
                onResult(null)
            } catch (t: Throwable) {
                onResult(t)
            }
        }
    }

    companion object {
        fun factory(repository: EventRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EventViewModel::class.java))
                    return EventViewModel(repository) as T
                }
            }
    }
}
