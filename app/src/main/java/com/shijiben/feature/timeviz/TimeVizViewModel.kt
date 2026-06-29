package com.shijiben.feature.timeviz

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * 时间可视化 ViewModel。注入 TimeVizPrefs，状态以 StateFlow 暴露。
 *
 * refresh 接受可选 now 参数：生产传默认（System.currentTimeMillis()），
 * 测试传死值以精确断言。setBirthday / setLifespan 内部调 refresh() 用默认 now；
 * 测试若需可控时间，可在这两个调用后再调 refresh(fixedNow) 覆盖状态。
 *
 * clock 用于 setBirthday 判今天，避免测试依赖墙钟（M2 修复）。
 */
@HiltViewModel
class TimeVizViewModel @Inject constructor(
    private val prefs: TimeVizPrefs,
    private val clock: Clock
) : ViewModel() {

    data class TimeVizUiState(
        val todayRemaining: String = "",
        val yearRemaining: String = "",
        val lifeResult: LifeResult? = null,   // null 表示未设生日
        val birthdayMillis: Long = 0L,
        val lifespanYears: Int = 80
    )

    private val _state = MutableStateFlow(refreshState(System.currentTimeMillis()))
    val state: StateFlow<TimeVizUiState> = _state.asStateFlow()

    /**
     * 刷新状态。[now] 用于测试注入可控时间，生产环境走默认值。
     */
    fun refresh(now: Long = System.currentTimeMillis()) {
        _state.value = refreshState(now)
    }

    /**
     * 设置生日。校验 birthDate <= today 否则静默忽略（保持极简，不报错）。
     * (year, month, day) → 当地 00:00 millis（M1 修复：统一基准为「当地 00:00」，
     * 与 TimeVizCalculator.lifeRemaining 用 ZoneId.systemDefault() 读取一致）。
     */
    fun setBirthday(year: Int, month: Int, day: Int) {
        val today = LocalDate.now(clock)
        val candidate = LocalDate.of(year, month, day)
        if (candidate.isAfter(today)) {
            // 未来生日：静默忽略，不保存、不报错
            return
        }
        val localMillis = LocalDate.of(year, month, day)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        prefs.setBirthdayMillis(localMillis)
        refresh()
    }

    /**
     * 设置寿命。clamp 到 [60, 120]，双保险（UI 也会 clamp）。
     */
    fun setLifespan(years: Int) {
        val clamped = years.coerceIn(60, 120)
        // 幂等：值相同时也写一次是安全的（SharedPreferences.apply 自动去重）
        prefs.setLifespanYears(clamped)
        refresh()
    }

    private fun refreshState(now: Long): TimeVizUiState {
        val birthdayMillis = prefs.getBirthdayMillis()
        val lifespan = prefs.getLifespanYears()
        val lifeResult = if (birthdayMillis == 0L) {
            null
        } else {
            TimeVizCalculator.lifeRemaining(birthdayMillis, lifespan, now)
        }
        return TimeVizUiState(
            todayRemaining = TimeVizCalculator.todayRemaining(now),
            yearRemaining = TimeVizCalculator.yearRemaining(now),
            lifeResult = lifeResult,
            birthdayMillis = birthdayMillis,
            lifespanYears = lifespan
        )
    }
}
