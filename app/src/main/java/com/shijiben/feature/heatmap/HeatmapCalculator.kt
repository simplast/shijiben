package com.shijiben.feature.heatmap

import com.shijiben.data.model.DailyActivity
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth

/**
 * 热力图纯函数工具：色阶映射 + 月历网格构建。无 Android 依赖，普通 JUnit 可测。
 */
object HeatmapCalculator {

    /** 16 小时清醒时间（毫秒），色阶分母 */
    const val AWAKE_MS_PER_DAY = 16L * 3600 * 1000

    data class Cell(
        val date: LocalDate,
        val isInMonth: Boolean,      // false = 月初/月末补位（上下月日期）
        val eventCount: Int,
        val durationMs: Long,
        val level: Int,              // 0..4
        val isToday: Boolean,
        val isFuture: Boolean        // 今天之后的日期（含本月与补位）
    )

    /** 色阶映射：ratio = durationMs / 16h */
    fun levelFor(durationMs: Long): Int {
        if (durationMs <= 0) return 0
        val ratio = durationMs.toDouble() / AWAKE_MS_PER_DAY
        return when {
            ratio < 0.25 -> 1
            ratio < 0.50 -> 2
            ratio < 0.75 -> 3
            else -> 4
        }
    }

    /**
     * 构建月历网格：固定 6 行 × 7 列 = 42 格，周一开头。
     * 月初补位用上月末几天（isInMonth=false），月末补位用下月初几天。
     */
    fun buildGrid(
        yearMonth: YearMonth,
        activities: List<DailyActivity>,
        today: LocalDate
    ): List<List<Cell>> {
        val byDate = activities.associateBy { it.date }
        val firstOfMonth = yearMonth.atDay(1)
        // 周一开头：Monday=1 .. Sunday=7，恰好对应 1..7 列偏移
        val firstCol = firstOfMonth.dayOfWeek.value
        val gridStart = firstOfMonth.minusDays((firstCol - 1).toLong())
        return (0 until 6).map { row ->
            (0 until 7).map { col ->
                val date = gridStart.plusDays((row * 7 + col).toLong())
                val act = byDate[date]
                val isToday = date == today
                val isFuture = date.isAfter(today)
                Cell(
                    date = date,
                    isInMonth = YearMonth.from(date) == yearMonth,
                    eventCount = act?.eventCount ?: 0,
                    durationMs = act?.durationMs ?: 0L,
                    level = levelFor(act?.durationMs ?: 0L),
                    isToday = isToday,
                    isFuture = isFuture
                )
            }
        }
    }

    data class YearGrid(
        val months: List<MonthGrid>   // 固定 12 项（1月..12月）
    )

    data class MonthGrid(
        val yearMonth: YearMonth,
        val cells: List<List<Cell>>,  // 6 行 × 7 列，复用 buildGrid 返回类型
        val monthLabel: String        // "1月".."12月"
    )

    /**
     * 构建年视图网格：12 个月的 mini 月历拼贴。
     * 每月调用既有 [buildGrid] 构建 6×7 网格（复用！），从 activities 过滤当月活动传入。
     * today 用于标记今天（与 buildGrid 同语义）。
     *
     * **去重**：[buildGrid] 会把 today 标记到 padding 格（上下月补位），
     * 在单月视图中有意义（"今天在下个月"），但在年视图中，今天的日期会在
     * 相邻月 padding + 当前月实体各出现一次，导致重复标记。
     * 年视图语义：今天只在当前所在月的实体格标记，padding 格不标 isToday。
     *
     * **性能**：单次 `groupBy` 按 YearMonth 分桶（O(N)），再逐月查表取当月活动，
     * 避免 12 次全量 `filter`（O(12N)）。对全年 365 天活动：4380 次迭代 → 365 次。
     */
    fun buildYearGrid(
        year: Year,
        activities: List<DailyActivity>,
        today: LocalDate
    ): YearGrid {
        // 单次分桶：YearMonth → 当月活动列表。避免 12 次 filter 各扫全量。
        val byMonth: Map<YearMonth, List<DailyActivity>> = activities.groupBy { YearMonth.from(it.date) }
        val months = (1..12).map { m ->
            val yearMonth = YearMonth.of(year.value, m)
            val monthActivities = byMonth[yearMonth].orEmpty()
            val rawCells = buildGrid(yearMonth, monthActivities, today)  // 复用既有 buildGrid
            // 年视图去重：padding 格（isInMonth=false）不标 isToday
            val cells = rawCells.map { row ->
                row.map { cell ->
                    if (cell.isInMonth) cell else cell.copy(isToday = false)
                }
            }
            MonthGrid(
                yearMonth = yearMonth,
                cells = cells,
                monthLabel = "${m}月"
            )
        }
        return YearGrid(months = months)
    }
}
