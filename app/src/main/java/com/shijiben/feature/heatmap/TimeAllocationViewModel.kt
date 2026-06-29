package com.shijiben.feature.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

/**
 * 时间去向 ViewModel。注入 EventRepository + Clock（与 TimeVizViewModel 同款 Clock 注入）。
 *
 * state 按 selectedRange 变化重新加载：getAllEvents().first() 一次性取数，
 * TimeAllocationCalculator 聚合后返回 Success。
 */
@HiltViewModel
class TimeAllocationViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val clock: Clock
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val items: List<TimeAllocationCalculator.TitleDuration>) : UiState
    }

    private val _selectedRange = MutableStateFlow(TimeAllocationCalculator.TimeRange.MONTH)
    val selectedRange: StateFlow<TimeAllocationCalculator.TimeRange> = _selectedRange.asStateFlow()

    val state: StateFlow<UiState> = _selectedRange
        .map { range -> loadAggregation(range) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            UiState.Loading
        )

    fun selectRange(range: TimeAllocationCalculator.TimeRange) {
        _selectedRange.value = range
    }

    private suspend fun loadAggregation(range: TimeAllocationCalculator.TimeRange): UiState {
        val events = eventRepository.getAllEvents().first()
        val now = clock.millis()
        val zone = clock.zone
        val items = TimeAllocationCalculator.aggregate(events, range, now, zone)
        return UiState.Success(items)
    }
}
