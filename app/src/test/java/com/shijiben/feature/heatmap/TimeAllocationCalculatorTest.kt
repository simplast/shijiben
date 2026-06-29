package com.shijiben.feature.heatmap

import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class TimeAllocationCalculatorTest {

    // 固定测试时区 UTC，避免依赖系统时区
    private val zone = ZoneOffset.UTC
    // now = 2026-06-15T12:00:00Z（周一）
    private val now = Instant.parse("2026-06-15T12:00:00Z").toEpochMilli()

    private fun event(
        id: Long,
        title: String,
        startTime: Long,
        endTime: Long?,
        status: Int = EventStatus.Completed.value
    ) = EventEntity(
        id = id,
        title = title,
        startTime = startTime,
        endTime = endTime,
        status = status,
        note = null,
        createdAt = startTime,
        updatedAt = startTime
    )

    private fun millis(iso: String): Long = Instant.parse(iso).toEpochMilli()

    @Test
    fun aggregate_completedEvents_sumsByTitle() {
        val events = listOf(
            event(1, "阅读", 1000L, 2000L),  // 1h
            event(2, "运动", 3000L, 4000L),  // 1h
            event(3, "阅读", 5000L, 7000L)   // 2h
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.ALL, now, zone)
        // "阅读" 合并 = 1h + 2h = 3h；"运动" = 1h
        assertThat(result).hasSize(2)
        assertThat(result[0].title).isEqualTo("阅读")
        assertThat(result[0].totalMs).isEqualTo(3000L)
        assertThat(result[1].title).isEqualTo("运动")
        assertThat(result[1].totalMs).isEqualTo(1000L)
    }

    @Test
    fun aggregate_rangeWeek_filtersToCurrentWeek() {
        // now = 2026-06-15T12:00:00Z（周一），本周一 00:00 = 2026-06-15T00:00:00Z
        // 上周日事件（startTime < 本周一）应被过滤
        val lastSundayStart = millis("2026-06-14T00:00:00Z") // 上周日
        val thisMondayStart = millis("2026-06-15T00:00:00Z") // 本周一
        val events = listOf(
            event(1, "上周日事", lastSundayStart, lastSundayStart + 3600_000),  // 上周日，应过滤
            event(2, "本周一事", thisMondayStart, thisMondayStart + 3600_000)    // 本周一，保留
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.WEEK, now, zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("本周一事")
    }

    @Test
    fun aggregate_rangeMonth_filtersToCurrentMonth() {
        // now = 2026-06-15T12:00:00Z，本月 1 日 = 2026-06-01T00:00:00Z
        // 5 月事件应被过滤
        val mayEventStart = millis("2026-05-15T00:00:00Z")
        val juneEventStart = millis("2026-06-10T00:00:00Z")
        val events = listOf(
            event(1, "五月事", mayEventStart, mayEventStart + 3600_000),
            event(2, "六月事", juneEventStart, juneEventStart + 3600_000)
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.MONTH, now, zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("六月事")
    }

    @Test
    fun aggregate_rangeYear_filtersToCurrentYear() {
        // now = 2026-06-15，本年 1 月 1 日 = 2026-01-01T00:00:00Z
        val lastYearStart = millis("2025-06-15T00:00:00Z") // 2025 年
        val thisYearStart = millis("2026-01-15T00:00:00Z") // 2026 年
        val events = listOf(
            event(1, "去年事", lastYearStart, lastYearStart + 3600_000),
            event(2, "今年事", thisYearStart, thisYearStart + 3600_000)
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.YEAR, now, zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("今年事")
    }

    @Test
    fun aggregate_rangeAll_noFilter() {
        val events = listOf(
            event(1, "历史事", 1000L, 2000L),
            event(2, "近期事", now, now + 3600_000)
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.ALL, now, zone)
        assertThat(result).hasSize(2)
    }

    @Test
    fun aggregate_ignoresInProgressAndNotStarted() {
        val events = listOf(
            event(1, "进行中", now, null, status = EventStatus.InProgress.value),
            event(2, "未开始", now, null, status = EventStatus.NotStarted.value),
            event(3, "已完成", now, now + 3600_000)
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.ALL, now, zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].title).isEqualTo("已完成")
    }

    @Test
    fun aggregate_emptyEvents_returnsEmpty() {
        val result = TimeAllocationCalculator.aggregate(emptyList(), TimeAllocationCalculator.TimeRange.ALL, now, zone)
        assertThat(result).isEmpty()
    }

    @Test
    fun aggregate_descendingOrder() {
        val events = listOf(
            event(1, "短", now, now + 600_000),  // 10m
            event(2, "长", now, now + 3600_000),  // 1h
            event(3, "中", now, now + 1800_000)   // 30m
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.ALL, now, zone)
        assertThat(result).hasSize(3)
        assertThat(result[0].title).isEqualTo("长")
        assertThat(result[1].title).isEqualTo("中")
        assertThat(result[2].title).isEqualTo("短")
    }

    @Test
    fun aggregate_crossDayEvent_usesFullDuration() {
        // 跨天事件：startTime 在本周一，endTime 跨到周二——时长用完整 endTime-startTime
        val mondayStart = millis("2026-06-15T00:00:00Z")
        val tuesdayEnd = mondayStart + 30L * 3600_000 // 30h 后
        val events = listOf(
            event(1, "跨天事", mondayStart, tuesdayEnd)
        )
        val result = TimeAllocationCalculator.aggregate(events, TimeAllocationCalculator.TimeRange.WEEK, now, zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].totalMs).isEqualTo(30L * 3600_000)
    }
}
