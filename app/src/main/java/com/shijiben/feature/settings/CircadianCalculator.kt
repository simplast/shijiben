package com.shijiben.feature.settings

import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import java.time.Instant
import java.time.ZoneId

/**
 * 本地生物钟聚合纯函数（Circadian Fingerprint）。
 *
 * 按 completed 事件 **startTime 的小时数**（0-23）归属。跨小时事件（如 14:00-16:00）
 * 整体归到开始小时 14，不拆分到 14/15/16——这是 S-effort 的简化，按开始小时归属。
 *
 * 规则：
 * - 只统计 status == Completed 的事件（InProgress/NotStarted 的 endTime 未确定，不计）
 * - 每个 completed 事件：hour = hourOfDay(startTime, zone)，
 *   bucket[hour].eventCount++，bucket[hour].durationMs += (endTime - startTime)
 * - 输出永远是 24 个桶（0..23），即使某小时无事件也返回 count=0/duration=0
 *
 * zone 参数与 aggregateMonth/aggregateYear/TimeAllocationCalculator 一致，便于测试注入
 * 固定时区避免漂移；生产默认用 ZoneId.systemDefault()。
 */
object CircadianCalculator {

    /** 单个小时桶：0..23。 */
    data class HourBucket(
        val hour: Int,
        val eventCount: Int,
        val durationMs: Long
    )

    fun circadianDistribution(
        events: List<EventEntity>,
        zone: ZoneId = ZoneId.systemDefault()
    ): List<HourBucket> {
        val counts = IntArray(24)
        val durations = LongArray(24)
        for (e in events) {
            if (e.status != EventStatus.Completed.value) continue
            val hour = Instant.ofEpochMilli(e.startTime).atZone(zone).hour
            counts[hour]++
            // Completed 理论上 endTime 必非 null（状态机保证）；保守判空防御脏数据
            val end = e.endTime ?: continue
            durations[hour] += (end - e.startTime)
        }
        return (0 until 24).map { HourBucket(it, counts[it], durations[it]) }
    }
}
