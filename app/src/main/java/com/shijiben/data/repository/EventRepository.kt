package com.shijiben.data.repository

import com.shijiben.data.local.EventDao
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import kotlinx.coroutines.flow.Flow
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
            val shifted = shiftToTargetDay(e, targetYear, targetMonth, targetDay, now)
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
        year: Int, month: Int, day: Int,
        now: Long
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
}
