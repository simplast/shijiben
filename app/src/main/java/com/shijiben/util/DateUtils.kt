package com.shijiben.util

import java.util.Calendar
import java.util.TimeZone

/**
 * 日期工具函数（UI 层 Triple<Int,Int,Int> = 年/月/日 表示）。
 *
 * 此前 isToday/todayTriple 在 TimelineScreen、DayProgressBar、SearchScreen 各有一份
 * 逐字相同的私有副本，调整逻辑需同步改多处易漂移。集中到本文件单点维护。
 *
 * 注：Triple<Int,Int,Int> 是 UI 层传参约定（如 viewingDate），非 data 层模型；
 * data/repository 层用 java.time.LocalDate / epoch millis 做计算，不走本工具。
 */

/** 当前日期的 Triple 表示（年, 月, 日）。月为 1-based（与 Calendar.MONTH+1 一致）。 */
fun todayTriple(): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    return Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/** 判断给定日期是否为今天。 */
fun isToday(date: Triple<Int, Int, Int>): Boolean = date == todayTriple()

/** 判断给定日期是否在过去（严格早于今天）。 */
fun isPastDay(date: Triple<Int, Int, Int>): Boolean {
    val today = todayTriple()
    val (ty, tm, td) = today
    val (y, m, d) = date
    return y < ty || (y == ty && (m < tm || (m == tm && d < td)))
}
