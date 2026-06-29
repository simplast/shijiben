package com.shijiben.feature.timeviz

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 时间可视化纯函数计算。所有函数参数全部显式传入，不依赖系统时钟，便于单测。
 */
object TimeVizCalculator {

    /**
     * 今天还剩多少时间。以「次日 00:00:00」为终点。
     * 例：00:00:00 → "24h 0m"；23:59:59 → "0h 0m"。
     */
    fun todayRemaining(nowMillis: Long): String {
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val endOfToday = now.toLocalDate().plusDays(1).atStartOfDay()
        val dur = Duration.between(now, endOfToday)
        val hours = dur.toHours()
        val minutes = (dur.toMinutes() % 60).toInt()
        return "${hours}h ${minutes}m"
    }

    /**
     * 今年还剩多少时间。以「次年 1-1 00:00:00」为终点。
     * 例：1-1 00:00:00（平年）→ "今年还有 365 天 0 小时"。
     */
    fun yearRemaining(nowMillis: Long): String {
        val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
        val endOfYear = LocalDate.of(now.year + 1, 1, 1).atStartOfDay()
        val dur = Duration.between(now, endOfYear)
        val days = dur.toDays().toInt()
        val hours = (dur.toHours() % 24).toInt()
        return "今年还有 ${days} 天 ${hours} 小时"
    }

    /**
     * 这一生还剩多少时间。
     *
     * @param birthdayMillis 生日当地 00:00 millis，0 视为未设（调用方处理）
     * @param lifespanYears 假设寿命
     * @param nowMillis 当前时间
     */
    fun lifeRemaining(birthdayMillis: Long, lifespanYears: Int, nowMillis: Long): LifeResult {
        val birthDate = LocalDate.ofInstant(
            Instant.ofEpochMilli(birthdayMillis),
            ZoneId.systemDefault()
        )
        val nowDate = LocalDate.ofInstant(
            Instant.ofEpochMilli(nowMillis),
            ZoneId.systemDefault()
        )

        // 未来生日防御：clamp yearsLived 到 >= 0
        val rawYearsLived = ChronoUnit.YEARS.between(birthDate, nowDate).toInt()
        val yearsLived = rawYearsLived.coerceAtLeast(0)
        val yearsRemaining = (lifespanYears - yearsLived).coerceAtLeast(0)
        val exceeded = yearsLived >= lifespanYears
        return LifeResult(
            yearsLived = yearsLived,
            yearsRemaining = yearsRemaining,
            exceeded = exceeded
        )
    }
}

data class LifeResult(
    val yearsLived: Int,
    val yearsRemaining: Int,
    val exceeded: Boolean
)
