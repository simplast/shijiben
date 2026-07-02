package com.shijiben.feature.settings

import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * 本地生物钟聚合算法单测（CircadianCalculator）。
 *
 * 纯 JUnit（无 Robolectric），测纯函数。固定时区 Asia/Shanghai 避免漂移，
 * 参考 EventRepositoryHeatmapTest 的 ms() 辅助写法。
 */
class CircadianCalculatorTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val h = 3600_000L

    private fun ms(y: Int, mo: Int, d: Int, hh: Int, mm: Int = 0): Long =
        LocalDateTime.of(y, mo, d, hh, mm).atZone(zone).toInstant().toEpochMilli()

    private fun entity(
        start: Long,
        end: Long?,
        status: EventStatus = EventStatus.Completed
    ) = EventEntity(
        id = 0,
        title = "e",
        startTime = start,
        endTime = end,
        status = status.value,
        note = null,
        createdAt = 0,
        updatedAt = 0
    )

    @Test
    fun emptyList_returns24ZeroBuckets() {
        val result = CircadianCalculator.circadianDistribution(emptyList(), zone)
        assertThat(result).hasSize(24)
        // 顺序 0..23
        assertThat(result.map { it.hour }).isEqualTo((0..23).toList())
        // 全部 0
        for (b in result) {
            assertThat(b.eventCount).isEqualTo(0)
            assertThat(b.durationMs).isEqualTo(0L)
        }
    }

    @Test
    fun singleCompletedEvent_attributesToStartHour() {
        // 14:30-15:30 → hour 14，dur 1h；hour 15 应为 0
        val e = entity(ms(2026, 7, 2, 14, 30), ms(2026, 7, 2, 15, 30))
        val result = CircadianCalculator.circadianDistribution(listOf(e), zone)
        assertThat(result).hasSize(24)
        assertThat(result[14].eventCount).isEqualTo(1)
        assertThat(result[14].durationMs).isEqualTo(1 * h)
        // 跨小时整体归开始小时，hour 15 不计
        assertThat(result[15].eventCount).isEqualTo(0)
        assertThat(result[15].durationMs).isEqualTo(0L)
    }

    @Test
    fun crossHourEvent_attributesWhollyToStartHour() {
        // 14:00-16:00 → 全归 hour 14，dur 2h；hour 15/16 为 0
        val e = entity(ms(2026, 7, 2, 14, 0), ms(2026, 7, 2, 16, 0))
        val result = CircadianCalculator.circadianDistribution(listOf(e), zone)
        assertThat(result[14].eventCount).isEqualTo(1)
        assertThat(result[14].durationMs).isEqualTo(2 * h)
        assertThat(result[15].eventCount).isEqualTo(0)
        assertThat(result[15].durationMs).isEqualTo(0L)
        assertThat(result[16].eventCount).isEqualTo(0)
        assertThat(result[16].durationMs).isEqualTo(0L)
    }

    @Test
    fun notStartedAndInProgressEventsAreExcluded() {
        // not_started + in_progress 都不计入任何桶
        val ns = entity(ms(2026, 7, 2, 10, 0), ms(2026, 7, 2, 11, 0), EventStatus.NotStarted)
        val ip = entity(ms(2026, 7, 2, 12, 0), null, EventStatus.InProgress)
        val result = CircadianCalculator.circadianDistribution(listOf(ns, ip), zone)
        assertThat(result).hasSize(24)
        for (b in result) {
            assertThat(b.eventCount).isEqualTo(0)
            assertThat(b.durationMs).isEqualTo(0L)
        }
    }

    @Test
    fun multipleEventsSameHour_sumsCountAndDuration() {
        // hour 9: 两条 completed
        // e1: 9:00-9:30 = 30min
        // e2: 9:45-10:15 = 30min（跨 10 点，整体归 9）
        // → hour 9: count=2, dur=60min
        val e1 = entity(ms(2026, 7, 2, 9, 0), ms(2026, 7, 2, 9, 30))
        val e2 = entity(ms(2026, 7, 2, 9, 45), ms(2026, 7, 2, 10, 15))
        val result = CircadianCalculator.circadianDistribution(listOf(e1, e2), zone)
        assertThat(result[9].eventCount).isEqualTo(2)
        assertThat(result[9].durationMs).isEqualTo(60 * 60_000L)
        // hour 10 不计（e2 归 9）
        assertThat(result[10].eventCount).isEqualTo(0)
        assertThat(result[10].durationMs).isEqualTo(0L)
    }

    @Test
    fun all24BucketsAlwaysPresent() {
        // 只有 hour 14 有事件，输出仍 24 个桶
        val e = entity(ms(2026, 7, 2, 14, 0), ms(2026, 7, 2, 15, 0))
        val result = CircadianCalculator.circadianDistribution(listOf(e), zone)
        assertThat(result).hasSize(24)
        assertThat(result.map { it.hour }).isEqualTo((0..23).toList())
        // 只有 hour 14 非零
        val nonZero = result.filter { it.eventCount > 0 }
        assertThat(nonZero).hasSize(1)
        assertThat(nonZero[0].hour).isEqualTo(14)
    }

    @Test
    fun midnightBoundary_eventAt0000GoesToBucket0() {
        // 00:00-01:00 → hour 0
        val e = entity(ms(2026, 7, 2, 0, 0), ms(2026, 7, 2, 1, 0))
        val result = CircadianCalculator.circadianDistribution(listOf(e), zone)
        assertThat(result[0].eventCount).isEqualTo(1)
        assertThat(result[0].durationMs).isEqualTo(1 * h)
        // hour 23 应为 0（前一日的 23:xx 不会归到 0）
        assertThat(result[23].eventCount).isEqualTo(0)
    }

    @Test
    fun multipleHours_independentBuckets() {
        // 三条事件分布在不同小时：hour 8, 14, 22
        val e1 = entity(ms(2026, 7, 2, 8, 0), ms(2026, 7, 2, 8, 30))   // 30min @ 8
        val e2 = entity(ms(2026, 7, 2, 14, 0), ms(2026, 7, 2, 15, 0))   // 1h @ 14
        val e3 = entity(ms(2026, 7, 2, 22, 0), ms(2026, 7, 2, 23, 30))  // 1.5h @ 22
        val result = CircadianCalculator.circadianDistribution(listOf(e1, e2, e3), zone)
        assertThat(result[8].eventCount).isEqualTo(1)
        assertThat(result[8].durationMs).isEqualTo(30 * 60_000L)
        assertThat(result[14].eventCount).isEqualTo(1)
        assertThat(result[14].durationMs).isEqualTo(1 * h)
        assertThat(result[22].eventCount).isEqualTo(1)
        assertThat(result[22].durationMs).isEqualTo(3 * h / 2)
        // 其他全 0
        val nonZeroHours = result.filter { it.eventCount > 0 }.map { it.hour }
        assertThat(nonZeroHours).containsExactly(8, 14, 22)
    }
}
