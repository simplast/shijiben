package com.shijiben.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "events",
    indices = [
        Index(value = ["startTime"], name = "index_events_start_time"),
        Index(value = ["status", "startTime"], name = "index_events_status_start")
    ]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long?,
    val status: Int = 0,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
