package com.shijiben.feature.timeviz

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

/**
 * 纯 JUnit 单测，覆盖 spec 测试清单第 1-15 条。
 *
 * 时区处理：在 @Before 中把默认时区固定为 UTC，使测试结果与 CI/dev 机时区无关。
 * 这里的 localMillis 给「本地时间」转 millis，utcMidnightMillis 产出「UTC 00:00」millis。
 * VM setBirthday 实际存的是「当地 00:00」millis（用 ZoneId.systemDefault()）；
 * 因测试固定时区为 UTC，「当地 00:00」与「UTC 00:00」数值相同，故 utcMidnightMillis
 * 产出的值恰好等于 setBirthday 在该时区下会存的值，可用以模拟其存储格式。
 */
class TimeVizCalculatorTest {

    private var originalTz: TimeZone? = null

    @Before
    fun setup() {
        originalTz = TimeZone.getDefault()
        // 固定 systemDefault 为 UTC，让 ZoneId.systemDefault() 在被测函数中行为可预测
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun teardown() {
        originalTz?.let { TimeZone.setDefault(it) }
    }

    /** 「本地时间」转 epoch millis，用于 now 参数。 */
    private fun localMillis(y: Int, m: Int, d: Int, h: Int = 0, mi: Int = 0, s: Int = 0): Long =
        LocalDateTime.of(y, m, d, h, mi, s)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    /** 产出「UTC 00:00」millis。因测试固定时区为 UTC，此值等于 setBirthday 在该时区下存储的「当地 00:00」millis，可用以模拟其存储格式。 */
    private fun utcMidnightMillis(y: Int, m: Int, d: Int): Long =
        LocalDateTime.of(y, m, d, 0, 0, 0)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

    // ===================== todayRemaining =====================

    @Test
    fun todayRemaining_atMidnight_returns24h0m() {
        val now = localMillis(2026, 6, 28, 0, 0, 0)
        assertThat(TimeVizCalculator.todayRemaining(now)).isEqualTo("24h 0m")
    }

    @Test
    fun todayRemaining_oneSecondAfterMidnight_returns23h59m() {
        val now = localMillis(2026, 6, 28, 0, 0, 1)
        assertThat(TimeVizCalculator.todayRemaining(now)).isEqualTo("23h 59m")
    }

    @Test
    fun todayRemaining_atNoon_returns12h0m() {
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        assertThat(TimeVizCalculator.todayRemaining(now)).isEqualTo("12h 0m")
    }

    @Test
    fun todayRemaining_at2359_returns0h1m() {
        val now = localMillis(2026, 6, 28, 23, 59, 0)
        assertThat(TimeVizCalculator.todayRemaining(now)).isEqualTo("0h 1m")
    }

    @Test
    fun todayRemaining_oneSecondBeforeMidnight_returns0h0m() {
        val now = localMillis(2026, 6, 28, 23, 59, 59)
        assertThat(TimeVizCalculator.todayRemaining(now)).isEqualTo("0h 0m")
    }

    // ===================== yearRemaining =====================

    @Test
    fun yearRemaining_atStartOfNonLeapYear_returns365Days0Hours() {
        val now = localMillis(2026, 1, 1, 0, 0, 0)
        assertThat(TimeVizCalculator.yearRemaining(now)).isEqualTo("今年还有 365 天 0 小时")
    }

    @Test
    fun yearRemaining_atStartOfLeapYear_returns366Days0Hours() {
        val now = localMillis(2024, 1, 1, 0, 0, 0)
        assertThat(TimeVizCalculator.yearRemaining(now)).isEqualTo("今年还有 366 天 0 小时")
    }

    @Test
    fun yearRemaining_atMidYear_returnsCorrectDaysAndHours() {
        // 2026-06-30 12:00 → 次年 1-1 00:00 = 184 天 + 12 小时
        val now = localMillis(2026, 6, 30, 12, 0, 0)
        assertThat(TimeVizCalculator.yearRemaining(now)).isEqualTo("今年还有 184 天 12 小时")
    }

    @Test
    fun yearRemaining_oneSecondBeforeYearEnd_returns0Days0Hours() {
        val now = localMillis(2026, 12, 31, 23, 59, 59)
        assertThat(TimeVizCalculator.yearRemaining(now)).isEqualTo("今年还有 0 天 0 小时")
    }

    // ===================== lifeRemaining =====================

    @Test
    fun lifeRemaining_onBirthday_returns33Years() {
        val bday = utcMidnightMillis(1993, 6, 28)
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        val r = TimeVizCalculator.lifeRemaining(bday, 80, now)
        assertThat(r.yearsLived).isEqualTo(33)
        assertThat(r.yearsRemaining).isEqualTo(47)
        assertThat(r.exceeded).isFalse()
    }

    @Test
    fun lifeRemaining_dayBeforeBirthday_returns32Years() {
        val bday = utcMidnightMillis(1993, 6, 29)
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        val r = TimeVizCalculator.lifeRemaining(bday, 80, now)
        assertThat(r.yearsLived).isEqualTo(32)
        assertThat(r.yearsRemaining).isEqualTo(48)
    }

    @Test
    fun lifeRemaining_exceededLifespan_returns0RemainingAndExceededTrue() {
        val bday = utcMidnightMillis(1940, 1, 1)
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        val r = TimeVizCalculator.lifeRemaining(bday, 80, now)
        assertThat(r.yearsLived).isEqualTo(86)
        assertThat(r.yearsRemaining).isEqualTo(0)
        assertThat(r.exceeded).isTrue()
    }

    @Test
    fun lifeRemaining_bornToday_returns0Years() {
        val bday = utcMidnightMillis(2026, 6, 28)
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        val r = TimeVizCalculator.lifeRemaining(bday, 80, now)
        assertThat(r.yearsLived).isEqualTo(0)
        assertThat(r.yearsRemaining).isEqualTo(80)
    }

    @Test
    fun lifeRemaining_futureBirthday_defenseClampsYearsLivedToZero() {
        // 防御测试：若强行传入未来生日，yearsLived clamp 到 0，避免显示「已走过 -N 年」
        val bday = utcMidnightMillis(2030, 1, 1)
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        val r = TimeVizCalculator.lifeRemaining(bday, 80, now)
        assertThat(r.yearsLived).isEqualTo(0)
        assertThat(r.yearsRemaining).isEqualTo(80)
        assertThat(r.exceeded).isFalse()
    }

    @Test
    fun lifeRemaining_exactlyLifespanYears_exceededTrue() {
        // bday=1946-06-28, now=2026-06-28, lifespan=80 → 恰好整 80 年
        // yearsLived=80, yearsRemaining=0, exceeded=true（>= 临界：恰好等于即 true）
        val bday = utcMidnightMillis(1946, 6, 28)
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        val r = TimeVizCalculator.lifeRemaining(bday, 80, now)
        assertThat(r.yearsLived).isEqualTo(80)
        assertThat(r.yearsRemaining).isEqualTo(0)
        assertThat(r.exceeded).isTrue()
    }

    @Test
    fun lifeRemaining_oneYearBeforeLifespan_exceededFalse() {
        // bday=1947-06-28, now=2026-06-28, lifespan=80 → yearsLived=79, yearsRemaining=1, exceeded=false
        val bday = utcMidnightMillis(1947, 6, 28)
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        val r = TimeVizCalculator.lifeRemaining(bday, 80, now)
        assertThat(r.yearsLived).isEqualTo(79)
        assertThat(r.yearsRemaining).isEqualTo(1)
        assertThat(r.exceeded).isFalse()
    }

    // ===================== todayProgress =====================

    @Test
    fun todayProgress_atMidnight_returns0() {
        val now = localMillis(2026, 6, 28, 0, 0, 0)
        assertThat(TimeVizCalculator.todayProgress(now)).isEqualTo(0f)
    }

    @Test
    fun todayProgress_atNoon_returns0_5() {
        // 12:00 → elapsed = 720 min, 720/1440 = 0.5（IEEE 754 精确可表）
        val now = localMillis(2026, 6, 28, 12, 0, 0)
        assertThat(TimeVizCalculator.todayProgress(now)).isEqualTo(0.5f)
    }

    @Test
    fun todayProgress_oneSecondBeforeMidnight_returnsAlmost1() {
        // 23:59:59 → toMinutes 截断为 1439 min, 1439/1440 ≈ 0.9993（< 1f 且 > 0.999f）
        val now = localMillis(2026, 6, 28, 23, 59, 59)
        val progress = TimeVizCalculator.todayProgress(now)
        assertThat(progress).isLessThan(1f)
        assertThat(progress).isGreaterThan(0.999f)
    }

    // ===================== yearProgress =====================

    @Test
    fun yearProgress_atStartOfYear_returns0() {
        val now = localMillis(2026, 1, 1, 0, 0, 0)
        assertThat(TimeVizCalculator.yearProgress(now)).isEqualTo(0f)
    }

    @Test
    fun yearProgress_atMidYear_returnsCloseToHalf() {
        // 2026-07-02 12:00 UTC：1-1 至 7-2 12:00 共 181 天 + 1.5 天 = 182.5 天
        // 365 天的年中点恰为 182.5 天 → progress = 0.5；区间断言防浮点抖动
        val now = localMillis(2026, 7, 2, 12, 0, 0)
        val progress = TimeVizCalculator.yearProgress(now)
        assertThat(progress).isGreaterThan(0.49f)
        assertThat(progress).isLessThan(0.51f)
    }

    @Test
    fun yearProgress_oneSecondBeforeYearEnd_returnsAlmost1() {
        // 2026-12-31 23:59:59 → elapsed = 365 days - 1s, total = 365 days
        // progress = 1 - 1/31536000 ≈ 0.99999997（< 1f 且 > 0.9999f）
        val now = localMillis(2026, 12, 31, 23, 59, 59)
        val progress = TimeVizCalculator.yearProgress(now)
        assertThat(progress).isLessThan(1f)
        assertThat(progress).isGreaterThan(0.9999f)
    }

    @Test
    fun yearProgress_leapYearVsNonLeapYear_denominatorMatchesYearLength() {
        // 同一日历点 2024-01-15 12:00（闰年）vs 2026-01-15 12:00（非闰年）。
        // 选 1-15（早于 2-28）使两年 elapsed 完全相同（14 天 12 小时），
        // 仅分母不同（366 vs 365 天），从而把「闰年分母」从 elapsed 中隔离出来。
        // progress_leap / progress_nonLeap = (14.5/366) / (14.5/365) = 365/366
        val leap = localMillis(2024, 1, 15, 12, 0, 0)
        val nonLeap = localMillis(2026, 1, 15, 12, 0, 0)
        val progressLeap = TimeVizCalculator.yearProgress(leap)
        val progressNonLeap = TimeVizCalculator.yearProgress(nonLeap)
        // 闰年分母更大 → progress 更小
        assertThat(progressLeap).isLessThan(progressNonLeap)
        // 比值 = 365/366（验证闰年分母是 366 天而非 365）
        val ratio = progressLeap / progressNonLeap
        assertThat(ratio).isWithin(1e-6f).of(365f / 366f)
    }
}
