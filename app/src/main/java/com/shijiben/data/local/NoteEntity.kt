package com.shijiben.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["timestamp"], name = "index_notes_timestamp")
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long
)
