package com.doer.shijiben.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query(
        """
        SELECT * FROM events 
        WHERE dayKey = :dayKey 
        ORDER BY CASE WHEN status = 'PENDING' THEN id ELSE startTimeMillis END ASC
        """,
    )
    fun observeEventsForDay(dayKey: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): EventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity): Long

    @Update
    suspend fun update(event: EventEntity)

    @Delete
    suspend fun delete(event: EventEntity)

    /** Distinct trimmed names ordered by usage frequency, for quick-pick. */
    @Query(
        """
        SELECT trim(name) AS name FROM events
        WHERE trim(name) != ''
        GROUP BY trim(name)
        ORDER BY COUNT(*) DESC, MAX(startTimeMillis) DESC
        LIMIT 10
        """,
    )
    fun observeRecentDistinctNames(): Flow<List<String>>

    @Query("SELECT * FROM events WHERE status = 'IN_PROGRESS' LIMIT 1")
    fun observeActiveEvent(): Flow<EventEntity?>

    @Query(
        """
        SELECT * FROM events 
        WHERE dayKey >= :startDay AND dayKey <= :endDay
        ORDER BY startTimeMillis ASC
        """
    )
    fun observeEventsInRange(startDay: String, endDay: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY startTimeMillis DESC")
    suspend fun getAllEvents(): List<EventEntity>

    /**
     * Get top event names by total occurrence count in the given day range.
     * Usually startDay is 14 days before today (in yyyy-MM-dd format).
     */
    @Query(
        """
        SELECT trim(name) AS name FROM events
        WHERE dayKey >= :startDay AND trim(name) != ''
        GROUP BY trim(name)
        ORDER BY COUNT(*) DESC, MAX(startTimeMillis) DESC
        LIMIT :limit
        """,
    )
    fun getTopEventNamesInRange(startDay: String, limit: Int = 5): Flow<List<String>>
}
