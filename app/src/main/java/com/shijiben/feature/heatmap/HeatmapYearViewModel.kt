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
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HeatmapYearViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    data class HeatmapYearUiState(
        val year: Year = Year.now(ZoneId.systemDefault()),
        val months: List<HeatmapCalculator.MonthGrid> = emptyList(),
        val isCurrentYear: Boolean = true,
        val canGoNext: Boolean = false   // 是否还能往未来翻（不超过当前年）
    )

    private val currentYear: Year
        get() = Year.now(ZoneId.systemDefault())   // B2 修复模式：计算属性，避免跨年不刷新

    private val _selectedYear = MutableStateFlow(currentYear)
    val selectedYear: StateFlow<Year> = _selectedYear.asStateFlow()

    val state: StateFlow<HeatmapYearUiState> = _selectedYear
        .flatMapLatest { year ->
            eventRepository.getDailyActivityForYear(year).map { activities ->
                val today = LocalDate.now(ZoneId.systemDefault())
                HeatmapYearUiState(
                    year = year,
                    months = HeatmapCalculator.buildYearGrid(year, activities, today).months,
                    isCurrentYear = year == currentYear,
                    canGoNext = year < currentYear
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeatmapYearUiState())

    fun previousYear() {
        _selectedYear.value = _selectedYear.value.minusYears(1)
    }

    fun nextYear() {
        // 下一年不超过当前年（未来无意义）
        if (_selectedYear.value < currentYear) {
            _selectedYear.value = _selectedYear.value.plusYears(1)
        }
    }

    fun goToCurrentYear() {
        _selectedYear.value = currentYear
    }
}
