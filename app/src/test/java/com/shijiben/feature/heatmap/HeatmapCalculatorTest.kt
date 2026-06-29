package com.shijiben.feature.heatmap

import com.google.common.truth.Truth.assertThat
import com.shijiben.data.model.DailyActivity
import org.junit.Test
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

/**
 * 热力图纯函数单测：色阶映射 + 月历网格构建（周一开头、补位、今天/未来标记）。
 * 纯 JUnit，无 Android 依赖。
 */
class HeatmapCalculatorTest {

    private val h = 3600_000L
    private val day16h = 16L * h

    // ==================== levelFor ====================

    @Test
    fun levelFor_zero_returns0() {
        assertThat(HeatmapCalculator.levelFor(0L)).isEqualTo(0)
    }

    @Test
    fun levelFor_oneMs_returns1() {
        assertThat(HeatmapCalculator.levelFor(1L)).isEqualTo(1)
    }

    @Test
    fun levelFor_3h_returns1() {
        // ratio = 3/16 = 0.1875 < 0.25
        assertThat(HeatmapCalculator.levelFor(3 * h)).isEqualTo(1)
    }

    @Test
    fun levelFor_4h_returns2() {
        // ratio = 0.25，边界含左
        assertThat(HeatmapCalculator.levelFor(4 * h)).isEqualTo(2)
    }

    @Test
    fun levelFor_8h_returns3() {
        // ratio = 0.50，边界含左
        assertThat(HeatmapCalculator.levelFor(8 * h)).isEqualTo(3)
    }

    @Test
    fun levelFor_12h_returns4() {
        // ratio = 0.75，边界含左
        assertThat(HeatmapCalculator.levelFor(12 * h)).isEqualTo(4)
    }

    @Test
    fun levelFor_16h_returns4() {
        assertThat(HeatmapCalculator.levelFor(day16h)).isEqualTo(4)
    }

    @Test
    fun levelFor_over16h_clampsTo4() {
        // ratio = 1.25，clamp 到 4
        assertThat(HeatmapCalculator.levelFor(20 * h)).isEqualTo(4)
    }

    // ==================== buildGrid ====================

    @Test
    fun buildGrid_2026_06_firstCellIsJune1() {
        // 2026-06-01 是周一
        val ym = YearMonth.of(2026, 6)
        val today = LocalDate.of(2026, 6, 28)
        val grid = HeatmapCalculator.buildGrid(ym, emptyList(), today)
        assertThat(grid).hasSize(6)
        assertThat(grid[0]).hasSize(7)
        // 共 42 格
        assertThat(grid.flatten()).hasSize(42)
        // 第 1 行第 1 列 = 2026-06-01，isInMonth=true
        val first = grid[0][0]
        assertThat(first.date).isEqualTo(LocalDate.of(2026, 6, 1))
        assertThat(first.isInMonth).isTrue()
    }

    @Test
    fun buildGrid_2026_02_firstRowLeadingPaddingIsJanuary() {
        // 2026-02-01 是周日 → 前 6 列是 1 月末补位
        val ym = YearMonth.of(2026, 2)
        val today = LocalDate.of(2026, 2, 15)
        val grid = HeatmapCalculator.buildGrid(ym, emptyList(), today)
        // 第 1 行前 6 列 isInMonth=false（1 月补位）
        for (col in 0 until 6) {
            assertThat(grid[0][col].isInMonth).isFalse()
        }
        // 第 1 行第 7 列 = 2026-02-01
        assertThat(grid[0][6].date).isEqualTo(LocalDate.of(2026, 2, 1))
        assertThat(grid[0][6].isInMonth).isTrue()
    }

    @Test
    fun buildGrid_emptyMonth_allInMonthCellsAreLevel0() {
        val ym = YearMonth.of(2026, 6)
        val today = LocalDate.of(2026, 6, 28)
        val grid = HeatmapCalculator.buildGrid(ym, emptyList(), today)
        for (row in grid) {
            for (cell in row) {
                if (cell.isInMonth) {
                    assertThat(cell.level).isEqualTo(0)
                    assertThat(cell.eventCount).isEqualTo(0)
                    assertThat(cell.durationMs).isEqualTo(0L)
                }
            }
        }
    }

    @Test
    fun buildGrid_todayCellIsMarked() {
        val ym = YearMonth.of(2026, 6)
        val today = LocalDate.of(2026, 6, 28)
        val grid = HeatmapCalculator.buildGrid(ym, emptyList(), today)
        val todayCells = grid.flatten().filter { it.isToday }
        assertThat(todayCells).hasSize(1)
        assertThat(todayCells.first().date).isEqualTo(LocalDate.of(2026, 6, 28))
    }

    @Test
    fun buildGrid_futureCellsAfterTodayAreMarked() {
        val ym = YearMonth.of(2026, 6)
        val today = LocalDate.of(2026, 6, 28)
        val grid = HeatmapCalculator.buildGrid(ym, emptyList(), today)
        val june29 = grid.flatten().first { it.date == LocalDate.of(2026, 6, 29) }
        val june28 = grid.flatten().first { it.date == LocalDate.of(2026, 6, 28) }
        val june27 = grid.flatten().first { it.date == LocalDate.of(2026, 6, 27) }
        assertThat(june29.isFuture).isTrue()
        assertThat(june28.isFuture).isFalse()
        assertThat(june27.isFuture).isFalse()
    }

    @Test
    fun buildGrid_trailingPaddingIsNextMonth() {
        val ym = YearMonth.of(2026, 6)
        val today = LocalDate.of(2026, 6, 28)
        val grid = HeatmapCalculator.buildGrid(ym, emptyList(), today)
        // 末尾含 7 月初几天，isInMonth=false
        val trailingOutOfMonth = grid.flatten().filter { !it.isInMonth && it.date.monthValue == 7 }
        assertThat(trailingOutOfMonth).isNotEmpty()
    }

    @Test
    fun buildGrid_activityMapsToLevel() {
        val ym = YearMonth.of(2026, 6)
        val today = LocalDate.of(2026, 6, 28)
        // 6-15 有 5h 记录 → ratio=5/16=0.3125 → level 2
        val activities = listOf(
            DailyActivity(date = LocalDate.of(2026, 6, 15), eventCount = 2, durationMs = 5 * h)
        )
        val grid = HeatmapCalculator.buildGrid(ym, activities, today)
        val cell = grid.flatten().first { it.date == LocalDate.of(2026, 6, 15) }
        assertThat(cell.level).isEqualTo(2)
        assertThat(cell.eventCount).isEqualTo(2)
        assertThat(cell.durationMs).isEqualTo(5 * h)
    }

    // ==================== buildYearGrid ====================

    @Test
    fun buildYearGrid_returns12Months() {
        val year = Year.of(2026)
        val today = LocalDate.of(2026, 6, 15)
        val grid = HeatmapCalculator.buildYearGrid(year, emptyList(), today)
        assertThat(grid.months).hasSize(12)
        for (m in 1..12) {
            val mg = grid.months[m - 1]
            assertThat(mg.monthLabel).isEqualTo("${m}月")
            assertThat(mg.yearMonth).isEqualTo(YearMonth.of(2026, m))
        }
    }

    @Test
    fun buildYearGrid_eachMonthCellsAre6x7() {
        val year = Year.of(2026)
        val today = LocalDate.of(2026, 6, 15)
        val grid = HeatmapCalculator.buildYearGrid(year, emptyList(), today)
        for (mg in grid.months) {
            assertThat(mg.cells).hasSize(6)
            for (row in mg.cells) assertThat(row).hasSize(7)
        }
    }

    @Test
    fun buildYearGrid_paddingCellsHaveIsInMonthFalse() {
        // 2026-01-01 是周四 → 1 月首行有 12 月末补位；2026-12-01 是周二 → 12 月末行有 1 月初补位
        val year = Year.of(2026)
        val today = LocalDate.of(2026, 6, 15)
        val grid = HeatmapCalculator.buildYearGrid(year, emptyList(), today)
        // 1 月：存在 isInMonth=false 且月份=12 的补位格
        val janPadding = grid.months[0].cells.flatten()
            .filter { !it.isInMonth && it.date.monthValue == 12 }
        assertThat(janPadding).isNotEmpty()
        // 12 月：存在 isInMonth=false 且月份=1 的补位格
        val decPadding = grid.months[11].cells.flatten()
            .filter { !it.isInMonth && it.date.monthValue == 1 }
        assertThat(decPadding).isNotEmpty()
    }

    @Test
    fun buildYearGrid_todayMarkedExactlyOnce() {
        val year = Year.of(2026)
        val today = LocalDate.of(2026, 6, 15)
        val grid = HeatmapCalculator.buildYearGrid(year, emptyList(), today)
        val todayCells = grid.months.flatMap { it.cells.flatten() }.filter { it.isToday }
        assertThat(todayCells).hasSize(1)
        assertThat(todayCells.first().date).isEqualTo(LocalDate.of(2026, 6, 15))
        // 该 cell 在 6 月 MonthGrid
        assertThat(todayCells.first().date.monthValue).isEqualTo(6)
    }

    @Test
    fun buildYearGrid_todayOutsideYearNotMarked() {
        // year=2025, today=2026-01-15（在 year 之外）→ 全年无 isToday cell
        val year = Year.of(2025)
        val today = LocalDate.of(2026, 1, 15)
        val grid = HeatmapCalculator.buildYearGrid(year, emptyList(), today)
        val todayCells = grid.months.flatMap { it.cells.flatten() }.filter { it.isToday }
        assertThat(todayCells).isEmpty()
    }

    @Test
    fun buildYearGrid_activityMappedToCorrectMonth() {
        val year = Year.of(2026)
        val today = LocalDate.of(2026, 6, 28)
        // 1月/6月/12月各 1 条 5h 活动 → level 2
        val activities = listOf(
            DailyActivity(date = LocalDate.of(2026, 1, 10), eventCount = 1, durationMs = 5 * h),
            DailyActivity(date = LocalDate.of(2026, 6, 15), eventCount = 1, durationMs = 5 * h),
            DailyActivity(date = LocalDate.of(2026, 12, 20), eventCount = 1, durationMs = 5 * h)
        )
        val grid = HeatmapCalculator.buildYearGrid(year, activities, today)
        // 1月 10 日
        val janCell = grid.months[0].cells.flatten().first { it.date == LocalDate.of(2026, 1, 10) }
        assertThat(janCell.durationMs).isEqualTo(5 * h)
        assertThat(janCell.level).isEqualTo(HeatmapCalculator.levelFor(5 * h))
        // 6月 15 日
        val junCell = grid.months[5].cells.flatten().first { it.date == LocalDate.of(2026, 6, 15) }
        assertThat(junCell.durationMs).isEqualTo(5 * h)
        assertThat(junCell.level).isEqualTo(HeatmapCalculator.levelFor(5 * h))
        // 12月 20 日
        val decCell = grid.months[11].cells.flatten().first { it.date == LocalDate.of(2026, 12, 20) }
        assertThat(decCell.durationMs).isEqualTo(5 * h)
        assertThat(decCell.level).isEqualTo(HeatmapCalculator.levelFor(5 * h))
        // 其余月份对应日（如 3 月 15 日）无活动 → durationMs == 0
        val marCell = grid.months[2].cells.flatten().first { it.date == LocalDate.of(2026, 3, 15) }
        assertThat(marCell.durationMs).isEqualTo(0L)
    }

    @Test
    fun buildYearGrid_activityInLeapYearFeb29() {
        // 2024 是闰年，2-29 事件应归属 2 月
        val year = Year.of(2024)
        val today = LocalDate.of(2024, 6, 15)
        val activities = listOf(
            DailyActivity(date = LocalDate.of(2024, 2, 29), eventCount = 1, durationMs = h)
        )
        val grid = HeatmapCalculator.buildYearGrid(year, activities, today)
        val febCell = grid.months[1].cells.flatten().first { it.date == LocalDate.of(2024, 2, 29) }
        assertThat(febCell.durationMs).isEqualTo(h)
        assertThat(febCell.isInMonth).isTrue()
    }

    @Test
    fun buildGrid_todayOutsideYearMonth_notMarked() {
        // yearMonth=2026-07, today=2026-06-28；2026-07-01 是周三（dayOfWeek.value=3）
        // gridStart = 2026-07-01.minusDays(2) = 2026-06-29（周一），网格首格为 6-29
        // today=2026-06-28 不在 [2026-06-29, 2026-08-09] 网格区间内 → 全网格无 isToday cell
        val ym = YearMonth.of(2026, 7)
        val today = LocalDate.of(2026, 6, 28)
        val grid = HeatmapCalculator.buildGrid(ym, emptyList(), today)
        assertThat(grid).hasSize(6)
        for (row in grid) assertThat(row).hasSize(7)
        assertThat(grid.flatten()).hasSize(42)
        // 核心断言：全网格无 isToday cell（today 落在网格外）
        assertThat(grid.flatten().filter { it.isToday }).isEmpty()
        // 确认 gridStart = 2026-06-29，6-28 不在网格内
        assertThat(grid[0][0].date).isEqualTo(LocalDate.of(2026, 6, 29))
    }
}
