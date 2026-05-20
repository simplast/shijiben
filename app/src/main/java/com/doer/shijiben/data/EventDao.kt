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
        ORDER BY startTimeMillis ASC
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

    /** Distinct trimmed names ordered by last use (`startTimeMillis`), for quick-pick. */
    @Query(
        """
        SELECT trim(name) AS name FROM events
        WHERE trim(name) != ''
        GROUP BY trim(name)
        ORDER BY MAX(startTimeMillis) DESC
        LIMIT 10
        """,
    )
    fun observeRecentDistinctNames(): Flow<List<String>>
}
