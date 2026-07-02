package com.shijiben.feature.heatmap

import com.google.common.truth.Truth.assertThat
import com.shijiben.ui.theme.Accent
import com.shijiben.ui.theme.Primary
import com.shijiben.ui.theme.RainbowCyan
import com.shijiben.ui.theme.RainbowLime
import com.shijiben.ui.theme.RainbowPink
import com.shijiben.ui.theme.RainbowPurple
import com.shijiben.ui.theme.Secondary
import com.shijiben.ui.theme.Warning
import org.junit.Test

/**
 * TimeAllocationTab 纯函数单测：rankColor（排名色板）+ formatAllocationDuration（时长格式化）。
 *
 * 这两个函数是 Cycle 28（F028）新增的纯函数，此前零覆盖。
 * - rankColor：分支逻辑（4 个固定档 + 冷色循环），改色板或循环顺序会破坏视觉一致性
 * - formatAllocationDuration：边界值（0、整小时、小时+分）影响 UI 显示
 *
 * 纯 JUnit，无 Android 依赖（Color 是 value class，可直接比较）。
 */
class TimeAllocationTabTest {

    // ==================== rankColor ====================

    @Test
    fun rankColor_topRanks_useWarmGradient() {
        // 前 4 名暖色梯度：红 → 橙 → 黄 → 绿
        assertThat(rankColor(0)).isEqualTo(Primary)
        assertThat(rankColor(1)).isEqualTo(Accent)
        assertThat(rankColor(2)).isEqualTo(Warning)
        assertThat(rankColor(3)).isEqualTo(Secondary)
    }

    @Test
    fun rankColor_ranks4to7_cycleThroughCoolPalette() {
        // 5+ 名冷色循环（青/紫/粉/柠檬绿）
        assertThat(rankColor(4)).isEqualTo(RainbowCyan)
        assertThat(rankColor(5)).isEqualTo(RainbowPurple)
        assertThat(rankColor(6)).isEqualTo(RainbowPink)
        assertThat(rankColor(7)).isEqualTo(RainbowLime)
    }

    @Test
    fun rankColor_rank8_cyclesBackToCyan() {
        // rank=8: (8-4) % 4 = 0 → RainbowCyan（循环回起点）
        assertThat(rankColor(8)).isEqualTo(RainbowCyan)
    }

    @Test
    fun rankColor_rank11_cyclesBackToLime() {
        // rank=11: (11-4) % 4 = 3 → RainbowLime
        assertThat(rankColor(11)).isEqualTo(RainbowLime)
    }

    @Test
    fun rankColor_rank12_cyclesBackToCyanAgain() {
        // rank=12: (12-4) % 4 = 0 → RainbowCyan（第二轮循环）
        assertThat(rankColor(12)).isEqualTo(RainbowCyan)
    }

    @Test
    fun rankColor_consecutiveRanks_areDistinctColors() {
        // 视觉分层前提：相邻排名色不同（否则失去"分层"意义）
        val colors = (0..7).map { rankColor(it) }
        for (i in 1 until colors.size) {
            assertThat(colors[i]).isNotEqualTo(colors[i - 1])
        }
    }

    // ==================== formatAllocationDuration ====================

    @Test
    fun formatAllocationDuration_zeroMs_returnsZeroMinutes() {
        assertThat(formatAllocationDuration(0L)).isEqualTo("0m")
    }

    @Test
    fun formatAllocationDuration_oneMinute_returnsOneMinute() {
        assertThat(formatAllocationDuration(60_000L)).isEqualTo("1m")
    }

    @Test
    fun formatAllocationDuration_59Minutes_returnsOnlyMinutes() {
        // < 1 小时：只显示分
        assertThat(formatAllocationDuration(59 * 60_000L)).isEqualTo("59m")
    }

    @Test
    fun formatAllocationDuration_exactlyOneHour_returnsOneHourZeroMinutes() {
        // 边界：整小时 → "1h 0m"
        assertThat(formatAllocationDuration(3600_000L)).isEqualTo("1h 0m")
    }

    @Test
    fun formatAllocationDuration_oneHourOneMinute_returnsOneHourOneMinute() {
        assertThat(formatAllocationDuration(3660_000L)).isEqualTo("1h 1m")
    }

    @Test
    fun formatAllocationDuration_twoHoursZeroMinutes_returnsTwoHoursZeroMinutes() {
        // 边界：刚好 2 小时
        assertThat(formatAllocationDuration(7200_000L)).isEqualTo("2h 0m")
    }

    @Test
    fun formatAllocationDuration_largeValue_formatsAllHoursAndMinutes() {
        // 10h 30m = 37800_000 ms
        assertThat(formatAllocationDuration(37800_000L)).isEqualTo("10h 30m")
    }

    @Test
    fun formatAllocationDuration_subMinuteTruncatesToZeroMinutes() {
        // < 1 分钟：整数除法截断为 0m（文档化截断行为，不四舍五入）
        assertThat(formatAllocationDuration(59_999L)).isEqualTo("0m")
    }
}
