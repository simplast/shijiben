package com.shijiben.data.repository

import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId

/**
 * 热力图聚合算法单测：跨日截断、进行中 clamp、not_started 不计时长、时区。
 * 纯 JUnit，测 EventRepository.kt 中 internal 顶层函数。
 */
class EventRepositoryHeatmapTest {

    private val zone = ZoneId.of("Asia/Shanghai")
    private val ym = YearMonth.of(2026, 6)
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

    // ==================== effectiveDurationMs ====================

    @Test
    fun effectiveDuration_crossDayEvent_onStartDay_truncatesToMidnight() {
        // 6-28 23:00 – 6-29 01:00
        val start = ms(2026, 6, 28, 23, 0)
        val end = ms(2026, 6, 29, 1, 0)
        val e = entity(start, end)
        val dStart = effectiveDurationMs(e, LocalDate.of(2026, 6, 28), end, zone)
        assertThat(dStart).isEqualTo(1 * h) // 23:00 → 24:00
    }

    @Test
    fun effectiveDuration_crossDayEvent_onEndDay_returnsZero() {
        val start = ms(2026, 6, 28, 23, 0)
        val end = ms(2026, 6, 29, 1, 0)
        val e = entity(start, end)
        val dEnd = effectiveDurationMs(e, LocalDate.of(2026, 6, 29), end, zone)
        assertThat(dEnd).isEqualTo(0L)
    }

    @Test
    fun effectiveDuration_inProgressSameDay_usesNowClampedToDayEnd() {
        // 6-15 14:00 开始未停，now = 6-15 20:00 → 6h
        val start = ms(2026, 6, 15, 14, 0)
        val now = ms(2026, 6, 15, 20, 0)
        val e = entity(start, null, EventStatus.InProgress)
        val d = effectiveDurationMs(e, LocalDate.of(2026, 6, 15), now, zone)
        assertThat(d).isEqualTo(6 * h)
    }

    @Test
    fun effectiveDuration_inProgressMultiDay_clampsToStartDayMidnight() {
        // 6-03 14:00 开始未停，now = 6-05 10:00
        val start = ms(2026, 6, 3, 14, 0)
        val now = ms(2026, 6, 5, 10, 0)
        val e = entity(start, null, EventStatus.InProgress)
        // day=6-03 → clamp 到 6-04 00:00，10h
        assertThat(effectiveDurationMs(e, LocalDate.of(2026, 6, 3), now, zone)).isEqualTo(10 * h)
        // day=6-04 → 0（eventStart.toLocalDate != 6-04）
        assertThat(effectiveDurationMs(e, LocalDate.of(2026, 6, 4), now, zone)).isEqualTo(0L)
        // day=6-05 → 0
        assertThat(effectiveDurationMs(e, LocalDate.of(2026, 6, 5), now, zone)).isEqualTo(0L)
    }

    // ==================== aggregateMonth ====================

    @Test
    fun aggregateMonth_emptyList_returnsEmpty() {
        val result = aggregateMonth(emptyList(), ym, now = ms(2026, 6, 15, 12, 0), zone)
        assertThat(result).isEmpty()
    }

    @Test
    fun aggregateMonth_notStarted_countsButNoDuration() {
        // not_started 事件计入 eventCount 但 durationMs 不增
        val start = ms(2026, 6, 15, 10, 0)
        val end = ms(2026, 6, 15, 11, 0)
        val e = entity(start, end, EventStatus.NotStarted)
        val result = aggregateMonth(listOf(e), ym, now = end, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 15))
        assertThat(result[0].eventCount).isEqualTo(1)
        assertThat(result[0].durationMs).isEqualTo(0L)
    }

    @Test
    fun aggregateMonth_singleDayMultipleEvents_sumsDuration() {
        // 同日 2 条 completed + 1 条 not_started → count=3, dur=2.5h
        val e1 = entity(ms(2026, 6, 15, 10, 0), ms(2026, 6, 15, 11, 0)) // 1h
        val e2 = entity(ms(2026, 6, 15, 14, 0), ms(2026, 6, 15, 15, 30)) // 1.5h
        val e3 = entity(ms(2026, 6, 15, 16, 0), ms(2026, 6, 15, 17, 0), EventStatus.NotStarted)
        val result = aggregateMonth(listOf(e1, e2, e3), ym, now = ms(2026, 6, 15, 17, 0), zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 15))
        assertThat(result[0].eventCount).isEqualTo(3)
        assertThat(result[0].durationMs).isEqualTo((2.5 * h).toLong())
    }

    @Test
    fun aggregateMonth_crossDayEventDoesNotPolluteEndDay() {
        // 6-28 23:00 – 6-29 01:00：归 6-28，6-29 不出现
        val e = entity(ms(2026, 6, 28, 23, 0), ms(2026, 6, 29, 1, 0))
        val result = aggregateMonth(listOf(e), ym, now = ms(2026, 6, 29, 1, 0), zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 28))
        assertThat(result[0].durationMs).isEqualTo(1 * h)
        // 6-29 不在结果
        assertThat(result.any { it.date == LocalDate.of(2026, 6, 29) }).isFalse()
    }

    @Test
    fun aggregateMonth_monthEndBoundary_oneMinEventAt2359() {
        // 6-30 23:59 的 1min 事件 → 6-30 dur=60_000
        val start = ms(2026, 6, 30, 23, 59)
        val end = ms(2026, 7, 1, 0, 0)
        val e = entity(start, end)
        val result = aggregateMonth(listOf(e), ym, now = end, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 30))
        assertThat(result[0].durationMs).isEqualTo(60_000L)
        // levelFor(60_000) → 1
        assertThat(com.shijiben.feature.heatmap.HeatmapCalculator.levelFor(60_000L)).isEqualTo(1)
    }

    @Test
    fun aggregateMonth_timezoneShanghai_startDayIsLocal() {
        // 事件 2026-06-15T00:00:00+08:00 → startDay=2026-06-15
        val start = ms(2026, 6, 15, 0, 0)
        val end = ms(2026, 6, 15, 1, 0)
        val e = entity(start, end)
        val result = aggregateMonth(listOf(e), ym, now = end, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 15))
    }

    @Test
    fun aggregateMonth_eventOutsideMonthIsSkipped() {
        // 5-31 事件传入（理论上 DAO 已过滤），算法层也跳过
        val e = entity(ms(2026, 5, 31, 23, 0), ms(2026, 6, 1, 1, 0))
        val result = aggregateMonth(listOf(e), ym, now = ms(2026, 6, 1, 1, 0), zone = zone)
        assertThat(result).isEmpty()
    }

    // ==================== aggregateYear ====================

    private val y2026 = Year.of(2026)

    @Test
    fun aggregateYear_emptyList_returnsEmpty() {
        val result = aggregateYear(emptyList(), y2026, now = ms(2026, 6, 15, 12, 0), zone = zone)
        assertThat(result).isEmpty()
    }

    @Test
    fun aggregateYear_notStarted_countsButNoDuration() {
        // not_started 事件计入 eventCount 但 durationMs 不增
        val start = ms(2026, 6, 15, 10, 0)
        val end = ms(2026, 6, 15, 11, 0)
        val e = entity(start, end, EventStatus.NotStarted)
        val result = aggregateYear(listOf(e), y2026, now = end, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 15))
        assertThat(result[0].eventCount).isEqualTo(1)
        assertThat(result[0].durationMs).isEqualTo(0L)
    }

    @Test
    fun aggregateYear_singleDayMultipleEvents_sumsDuration() {
        // 同日 2 条 completed + 1 条 not_started → count=3, dur=2.5h
        val e1 = entity(ms(2026, 6, 15, 10, 0), ms(2026, 6, 15, 11, 0)) // 1h
        val e2 = entity(ms(2026, 6, 15, 14, 0), ms(2026, 6, 15, 15, 30)) // 1.5h
        val e3 = entity(ms(2026, 6, 15, 16, 0), ms(2026, 6, 15, 17, 0), EventStatus.NotStarted)
        val result = aggregateYear(listOf(e1, e2, e3), y2026, now = ms(2026, 6, 15, 17, 0), zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 15))
        assertThat(result[0].eventCount).isEqualTo(3)
        assertThat(result[0].durationMs).isEqualTo((2.5 * h).toLong())
    }

    @Test
    fun aggregateYear_crossDayEventDoesNotPolluteEndDay() {
        // 12-31 23:00 – 1-1 01:00（跨年）：归 12-31，1-1 不出现
        val e = entity(ms(2026, 12, 31, 23, 0), ms(2027, 1, 1, 1, 0))
        val result = aggregateYear(listOf(e), y2026, now = ms(2027, 1, 1, 1, 0), zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(result[0].durationMs).isEqualTo(1 * h)
        // 1-1 不在结果
        assertThat(result.any { it.date == LocalDate.of(2027, 1, 1) }).isFalse()
    }

    @Test
    fun aggregateYear_inProgressMultiDay_clampsToStartDayMidnight() {
        // 6-03 14:00 开始未停，now = 6-05 10:00 → 6-03 dur=10h，6-04/6-05 不出现
        val start = ms(2026, 6, 3, 14, 0)
        val now = ms(2026, 6, 5, 10, 0)
        val e = entity(start, null, EventStatus.InProgress)
        val result = aggregateYear(listOf(e), y2026, now = now, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 3))
        assertThat(result[0].durationMs).isEqualTo(10 * h)
        // 6-04/6-05 不出现
        assertThat(result.any { it.date == LocalDate.of(2026, 6, 4) }).isFalse()
        assertThat(result.any { it.date == LocalDate.of(2026, 6, 5) }).isFalse()
    }

    @Test
    fun aggregateYear_yearEndBoundary_oneMinEventAt2359() {
        // 12-31 23:59 的 1min 事件 → 12-31 dur=60_000
        val start = ms(2026, 12, 31, 23, 59)
        val end = ms(2027, 1, 1, 0, 0)
        val e = entity(start, end)
        val result = aggregateYear(listOf(e), y2026, now = end, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 12, 31))
        assertThat(result[0].durationMs).isEqualTo(60_000L)
    }

    @Test
    fun aggregateYear_timezoneShanghai_startDayIsLocal() {
        // 事件 2026-06-15T00:00:00+08:00 → startDay=2026-06-15
        val start = ms(2026, 6, 15, 0, 0)
        val end = ms(2026, 6, 15, 1, 0)
        val e = entity(start, end)
        val result = aggregateYear(listOf(e), y2026, now = end, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 15))
    }

    @Test
    fun aggregateYear_eventOutsideYearIsSkipped() {
        // 2025-12-31 事件传入（理论上 DAO 已过滤），算法层也跳过
        val e = entity(ms(2025, 12, 31, 23, 0), ms(2026, 1, 1, 1, 0))
        val result = aggregateYear(listOf(e), y2026, now = ms(2026, 1, 1, 1, 0), zone = zone)
        assertThat(result).isEmpty()
    }

    @Test
    fun aggregateYear_leapYearFeb29EventIncluded() {
        // year=2024（闰年），事件 2024-02-29 10:00-11:00 → 含 2024-02-29，dur=1h
        val y2024 = Year.of(2024)
        val start = ms(2024, 2, 29, 10, 0)
        val end = ms(2024, 2, 29, 11, 0)
        val e = entity(start, end)
        val result = aggregateYear(listOf(e), y2024, now = end, zone = zone)
        assertThat(result).hasSize(1)
        assertThat(result[0].date).isEqualTo(LocalDate.of(2024, 2, 29))
        assertThat(result[0].durationMs).isEqualTo(1 * h)
    }
}

/**
 * dayRangeMs DST 回归测试：验证 Calendar.add 而非 +24h。
 * - 春进日（23h）：America/New_York 2026-03-08，2:00→3:00，当天 23h。
 * - 秋退日（25h）：America/New_York 2026-11-01，2:00→1:00，当天 25h。
 * - 非 DST 时区（Asia/Shanghai）：恒 24h。
 * 旧实现 end=start+24h 在春进日偏到次日 01:00、秋退日偏到当日 23:00。
 */
class DayRangeMsDstTest {

    private val ny = java.util.TimeZone.getTimeZone("America/New_York")
    private val sh = java.util.TimeZone.getTimeZone("Asia/Shanghai")
    private val dayMs = 24L * 3600 * 1000

    @Test
    fun normalDay_is24Hours() {
        val (s, e) = dayRangeMs(2026, 6, 15, sh)
        assertThat(e - s).isEqualTo(dayMs)
    }

    @Test
    fun normalDay_ny_is24Hours() {
        val (s, e) = dayRangeMs(2026, 6, 15, ny)
        assertThat(e - s).isEqualTo(dayMs)
    }

    @Test
    fun springForward_ny_is23Hours() {
        // 2026-03-08 America/New_York: 春进，2:00→3:00，当天 23h
        val (s, e) = dayRangeMs(2026, 3, 8, ny)
        assertThat(e - s).isEqualTo(23L * 3600 * 1000)
        // 旧 +24h 实现会得到 24h（end 落到次日 01:00 EDT），此处断言 23h 即可区分
    }

    @Test
    fun fallBack_ny_is25Hours() {
        // 2026-11-01 America/New_York: 秋退，2:00→1:00，当天 25h
        val (s, e) = dayRangeMs(2026, 11, 1, ny)
        assertThat(e - s).isEqualTo(25L * 3600 * 1000)
    }

    @Test
    fun springForward_ny_endIsNextDayMidnight() {
        // 春进日 end 必须是次日 00:00 EDT（UTC-4），不是次日 01:00
        val (s, e) = dayRangeMs(2026, 3, 8, ny)
        val startCal = java.util.Calendar.getInstance(ny)
        startCal.timeInMillis = s
        assertThat(startCal.get(java.util.Calendar.DAY_OF_MONTH)).isEqualTo(8)
        assertThat(startCal.get(java.util.Calendar.HOUR_OF_DAY)).isEqualTo(0)
        val endCal = java.util.Calendar.getInstance(ny)
        endCal.timeInMillis = e
        assertThat(endCal.get(java.util.Calendar.DAY_OF_MONTH)).isEqualTo(9)
        assertThat(endCal.get(java.util.Calendar.HOUR_OF_DAY)).isEqualTo(0)
    }

    @Test
    fun fallBack_ny_endIsNextDayMidnight() {
        // 秋退日 end 必须是次日 00:00 EST（UTC-5），不是当日 23:00
        val (s, e) = dayRangeMs(2026, 11, 1, ny)
        val endCal = java.util.Calendar.getInstance(ny)
        endCal.timeInMillis = e
        assertThat(endCal.get(java.util.Calendar.DAY_OF_MONTH)).isEqualTo(2)
        assertThat(endCal.get(java.util.Calendar.HOUR_OF_DAY)).isEqualTo(0)
    }
}
