package com.shijiben.data.repository

import com.shijiben.data.local.EventDao
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.DailyActivity
import com.shijiben.data.model.EventStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.ZoneId
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventRepository @Inject constructor(
    private val eventDao: EventDao
) {
    fun getEventsByDate(year: Int, month: Int, day: Int): Flow<List<EventEntity>> {
        val (start, end) = dayRange(year, month, day)
        return eventDao.getEventsByDate(start, end)
    }

    fun getEventsByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>> =
        eventDao.getEventsByDate(startOfDay, endOfDay)

    fun getOngoingEvent(): Flow<EventEntity?> = eventDao.getOngoingEvent()

    suspend fun getEventById(id: Long): EventEntity? = eventDao.getEventById(id)

    fun getAllEvents(): Flow<List<EventEntity>> = eventDao.getAllEvents()

    suspend fun createEvent(
        title: String,
        startTime: Long,
        endTime: Long?,
        note: String?,
        status: Int? = null
    ): Long {
        val now = System.currentTimeMillis()
        val finalStatus = status ?: determineStatus(startTime, endTime, now).value
        val event = EventEntity(
            title = title,
            startTime = startTime,
            endTime = endTime,
            status = finalStatus,
            note = note,
            createdAt = now,
            updatedAt = now
        )
        return eventDao.insertEvent(event)
    }

    /** 导入用：按原 ID 批量 upsert（Dao OnConflictStrategy.REPLACE 覆盖同 ID）。保留原 id，幂等可重复导入。 */
    suspend fun upsertAll(events: List<EventEntity>) {
        for (e in events) eventDao.insertEvent(e)
    }

    suspend fun updateEvent(event: EventEntity) {
        eventDao.updateEvent(event.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteEvent(event: EventEntity) = eventDao.deleteEvent(event)

    suspend fun deleteEventById(id: Long) = eventDao.deleteEventById(id)

    /** 手动把事件标记为已完成（停止计时） */
    suspend fun markCompleted(eventId: Long) {
        val event = eventDao.getEventById(eventId) ?: return
        val now = System.currentTimeMillis()
        val endTime = if (event.endTime == null) now else event.endTime
        eventDao.updateEvent(event.copy(endTime = endTime, status = EventStatus.Completed.value, updatedAt = now))
    }

    /** 手动把事件标记为进行中（开始计时），end_time 置 null */
    suspend fun markInProgress(eventId: Long) {
        val event = eventDao.getEventById(eventId) ?: return
        val now = System.currentTimeMillis()
        eventDao.updateEvent(event.copy(endTime = null, status = EventStatus.InProgress.value, updatedAt = now))
    }

    /** 手动把事件标记为未开始（预写） */
    suspend fun markNotStarted(eventId: Long) {
        val event = eventDao.getEventById(eventId) ?: return
        val now = System.currentTimeMillis()
        eventDao.updateEvent(event.copy(status = EventStatus.NotStarted.value, updatedAt = now))
    }

    /**
     * 自动顺延：把指定日期之前仍未开始的 notStarted 事件移到目标日期的相同时刻。
     * 仅顺延 notStarted，completed/inProgress 不动。
     * 返回顺延的事件数量。
     */
    suspend fun carryOverNotStarted(targetYear: Int, targetMonth: Int, targetDay: Int): Int {
        val (targetStart, targetEnd) = dayRange(targetYear, targetMonth, targetDay)
        val now = System.currentTimeMillis()
        // 取今天 0 点之前的所有 notStarted
        val pending = eventDao.getEventsByStatusBeforeDate(EventStatus.NotStarted.value, targetStart)
        var count = 0
        for (e in pending) {
            // 把 startTime/endTime 平移到 targetDay 的相同时刻
            val shifted = shiftToTargetDay(e, targetYear, targetMonth, targetDay)
            if (shifted != null) {
                eventDao.updateEventTime(
                    id = e.id,
                    newStart = shifted.first,
                    newEnd = shifted.second,
                    now = now
                )
                count++
            }
        }
        return count
    }

    internal fun shiftToTargetDay(
        e: EventEntity,
        year: Int, month: Int, day: Int
    ): Pair<Long, Long?>? {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = e.startTime
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        cal.set(year, month - 1, day, hour, minute, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val newStart = cal.timeInMillis
        if (newStart == e.startTime) return null // 已在目标日，无需顺延
        val newEnd: Long? = if (e.endTime != null) {
            val cal2 = Calendar.getInstance(TimeZone.getDefault())
            cal2.timeInMillis = e.endTime
            val dur = e.endTime - e.startTime
            newStart + dur
        } else null
        return Pair(newStart, newEnd)
    }

    internal fun determineStatus(startTime: Long, endTime: Long?, now: Long): EventStatus {
        // 进行中
        if (endTime == null && startTime <= now) return EventStatus.InProgress
        // 预写
        if (startTime > now) return EventStatus.NotStarted
        // 补录完成
        return EventStatus.Completed
    }

    private fun dayRange(year: Int, month: Int, day: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(year, month - 1, day, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        val end = start + 24L * 60 * 60 * 1000
        return Pair(start, end)
    }

    

    /** 工具：返回当天 0 点与次日 0 点的时间戳 */
    fun todayRange(): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        return dayRange(y, m, d)
    }

    /** 工具：返回指定时间戳所在天的 0 点与次日 0 点 */
    fun dayRangeOf(timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = timestamp
        return dayRange(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    // ==================== 热力图月视图聚合 ====================

    fun getDailyActivityForMonth(yearMonth: YearMonth): Flow<List<DailyActivity>> =
        eventDao.getEventsByMonth(monthStartEpoch(yearMonth), monthEndEpoch(yearMonth))
            .map { events -> aggregateMonth(events, yearMonth) }

    private fun monthStartEpoch(ym: YearMonth): Long =
        ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun monthEndEpoch(ym: YearMonth): Long =
        ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // ==================== 热力图年视图聚合 ====================

    fun getDailyActivityForYear(year: Year): Flow<List<DailyActivity>> =
        eventDao.getEventsByMonth(yearStartEpoch(year), yearEndEpoch(year))
            .map { events -> aggregateYear(events, year) }

    private fun yearStartEpoch(year: Year): Long =
        year.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun yearEndEpoch(year: Year): Long =
        year.plusYears(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * 月度聚合：按事件开始日归属，跨日事件时长截断到当天 24:00，进行中事件 clamp 到当天范围。
 * internal 供单测访问；now 与 zone 提供测试注入点。
 */
internal fun aggregateMonth(
    events: List<EventEntity>,
    yearMonth: YearMonth,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): List<DailyActivity> {
    val byDay = mutableMapOf<LocalDate, Pair<Int, Long>>() // date -> (count, durationMs)
    for (e in events) {
        val eventStart = Instant.ofEpochMilli(e.startTime).atZone(zone)
        val startDay = eventStart.toLocalDate()
        // 仅归属开始日；不在选定月的事件（理论上 DAO 已过滤）跳过
        if (YearMonth.from(startDay) != yearMonth) continue
        // not_started(0) 计入 count 但不计入时长
        val (cnt, dur) = byDay[startDay] ?: (0 to 0L)
        val newCnt = cnt + 1
        val newDur = if (e.status == EventStatus.NotStarted.value) dur
        else dur + effectiveDurationMs(e, startDay, now, zone)
        byDay[startDay] = newCnt to newDur
    }
    return byDay.entries.map { (d, pair) ->
        DailyActivity(date = d, eventCount = pair.first, durationMs = pair.second)
    }.sortedBy { it.date }
}

/**
 * 年度聚合：按事件开始日归属，跨日事件时长截断到当天 24:00，进行中事件 clamp 到当天范围。
 * 与 [aggregateMonth] 逻辑平行，仅范围不同（年 vs 月）。复用 [effectiveDurationMs]。
 * internal 供单测访问；now 与 zone 提供测试注入点。
 *
 * 设计决策：独立实现而非抽 helper 泛化 aggregateMonth，保持 aggregateMonth 零改动零回归。
 * 返回值只包含有事件的天（与 aggregateMonth 一致），无事件的天由 buildGrid 通过
 * activities.associateBy + ?: 0 处理（复用既有逻辑）。
 */
internal fun aggregateYear(
    events: List<EventEntity>,
    year: Year,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): List<DailyActivity> {
    val byDay = mutableMapOf<LocalDate, Pair<Int, Long>>() // date -> (count, durationMs)
    for (e in events) {
        val eventStart = Instant.ofEpochMilli(e.startTime).atZone(zone)
        val startDay = eventStart.toLocalDate()
        // 仅归属开始日；不在选定年的事件（理论上 DAO 已过滤）跳过
        if (Year.from(startDay) != year) continue
        // not_started(0) 计入 count 但不计入时长
        val (cnt, dur) = byDay[startDay] ?: (0 to 0L)
        val newCnt = cnt + 1
        val newDur = if (e.status == EventStatus.NotStarted.value) dur
        else dur + effectiveDurationMs(e, startDay, now, zone)
        byDay[startDay] = newCnt to newDur
    }
    return byDay.entries.map { (d, pair) ->
        DailyActivity(date = d, eventCount = pair.first, durationMs = pair.second)
    }.sortedBy { it.date }
}

/**
 * 单事件在指定 day 的有效时长（毫秒）。
 * - 跨日事件：截断到当天 24:00（dayEnd）。
 * - 进行中事件（endTime null）：用 now，并 clamp 到 dayEnd。
 * - 归属规则：仅当 eventStart.toLocalDate() == day 才返回非 0。
 */
internal fun effectiveDurationMs(
    event: EventEntity,
    day: LocalDate,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val eventStart = Instant.ofEpochMilli(event.startTime).atZone(zone)
    if (eventStart.toLocalDate() != day) return 0L
    val rawEnd = event.endTime ?: now
    val eventEnd = Instant.ofEpochMilli(rawEnd).atZone(zone)
    val dayEnd = day.plusDays(1).atStartOfDay(zone) // 当天 24:00 = 次日 00:00
    val effectiveEnd = if (eventEnd.isBefore(dayEnd)) eventEnd else dayEnd
    val ms = Duration.between(eventStart, effectiveEnd).toMillis()
    return ms.coerceAtLeast(0L)
}

fun dayRangeMs(year: Int, month: Int, day: Int, tz: java.util.TimeZone): Pair<Long, Long> {
    val cal = java.util.Calendar.getInstance(tz)
    cal.set(year, month - 1, day, 0, 0, 0)
    cal.set(java.util.Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.add(java.util.Calendar.DAY_OF_MONTH, 1)
    val end = cal.timeInMillis
    return Pair(start, end)
}
