package com.shijiben.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE startTime >= :startOfDay AND startTime < :endOfDay ORDER BY startTime ASC")
    fun getEventsByDate(startOfDay: Long, endOfDay: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE status = :status AND startTime < :date ORDER BY startTime ASC")
    suspend fun getEventsByStatusBeforeDate(status: Int, date: Long): List<EventEntity>

    @Query("SELECT * FROM events WHERE endTime IS NULL AND status = 1 LIMIT 1")
    fun getOngoingEvent(): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Long): EventEntity?

    @Query("SELECT * FROM events ORDER BY startTime DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("UPDATE events SET tagId = NULL WHERE tagId = :tagId")
    suspend fun clearTagReference(tagId: Long)

    @Query("UPDATE events SET startTime = :newStart, endTime = :newEnd, updatedAt = :now WHERE id = :id")
    suspend fun updateEventTime(id: Long, newStart: Long, newEnd: Long?, now: Long)
}
