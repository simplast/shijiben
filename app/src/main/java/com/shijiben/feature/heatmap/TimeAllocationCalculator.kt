package com.shijiben.feature.heatmap

import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters

/**
 * 时间去向聚合纯函数。
 *
 * 按事件标题聚合 completed 事件时长，支持 4 种时间范围过滤：
 * - WEEK：本周（周一 00:00 ~ 现在）
 * - MONTH：本月（1 日 00:00 ~ 现在）
 * - YEAR：本年（1 月 1 日 00:00 ~ 现在）
 * - ALL：不过滤
 *
 * 规则：
 * - 只聚合 status == Completed 的事件（InProgress/NotStarted 无 endTime，时长未确定）
 * - 时长 = endTime - startTime（Completed 必有 endTime）
 * - 按 title 分组求和，降序排列
 * - 范围过滤基于 startTime
 */
object TimeAllocationCalculator {

    data class TitleDuration(
        val title: String,
        val totalMs: Long
    )

    enum class TimeRange { WEEK, MONTH, YEAR, ALL }

    fun aggregate(
        events: List<EventEntity>,
        range: TimeRange,
        now: Long,
        zone: ZoneId
    ): List<TitleDuration> {
        if (events.isEmpty()) return emptyList()
        val rangeStart = rangeStartMillis(range, now, zone)
        return events.asSequence()
            .filter { it.status == EventStatus.Completed.value }
            .filter { it.endTime != null && it.startTime >= rangeStart }
            .groupBy { it.title }
            .map { (title, group) ->
                TitleDuration(
                    title = title,
                    totalMs = group.sumOf { (it.endTime ?: 0L) - it.startTime }
                )
            }
            .sortedByDescending { it.totalMs }
            .toList()
    }

    /** 返回范围起始 epoch millis；ALL 返回 Long.MIN_VALUE（不过滤）。 */
    private fun rangeStartMillis(range: TimeRange, now: Long, zone: ZoneId): Long {
        val nowZdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone)
        val startZdt = when (range) {
            TimeRange.WEEK -> nowZdt.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
                .toLocalDate().atStartOfDay(zone)
            TimeRange.MONTH -> nowZdt.toLocalDate().withDayOfMonth(1).atStartOfDay(zone)
            TimeRange.YEAR -> nowZdt.toLocalDate().withDayOfYear(1).atStartOfDay(zone)
            TimeRange.ALL -> return Long.MIN_VALUE
        }
        return startZdt.toInstant().toEpochMilli()
    }
}
