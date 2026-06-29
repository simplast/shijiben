package com.shijiben.feature.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    data class HeatmapUiState(
        val yearMonth: YearMonth = YearMonth.now(ZoneId.systemDefault()),
        val cells: List<List<HeatmapCalculator.Cell>> = emptyList(),
        val isCurrentMonth: Boolean = true,
        val canGoNext: Boolean = false   // 是否还能往未来翻（不超过当前月）
    )

    private val currentMonth: YearMonth
        get() = YearMonth.now(ZoneId.systemDefault())

    private val _selectedMonth = MutableStateFlow(currentMonth)
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val state: StateFlow<HeatmapUiState> = _selectedMonth
        .flatMapLatest { ym ->
            eventRepository.getDailyActivityForMonth(ym).map { activities ->
                val today = LocalDate.now(ZoneId.systemDefault())
                HeatmapUiState(
                    yearMonth = ym,
                    cells = HeatmapCalculator.buildGrid(ym, activities, today),
                    isCurrentMonth = ym == currentMonth,
                    canGoNext = ym < currentMonth
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeatmapUiState())

    fun previousMonth() {
        _selectedMonth.value = _selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        // 下一月不超过当前月（未来无意义）
        if (_selectedMonth.value < currentMonth) {
            _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        }
    }

    fun goToCurrentMonth() {
        _selectedMonth.value = currentMonth
    }
}
