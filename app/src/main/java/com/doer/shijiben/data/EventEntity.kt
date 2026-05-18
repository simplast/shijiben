package com.doer.shijiben.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    /** Local-date key (`yyyy-MM-dd`) derived from start time for daily queries. */
    val dayKey: String,
    /** Status of the event: "IN_PROGRESS" or "COMPLETED" */
    val status: String = "COMPLETED"
)
