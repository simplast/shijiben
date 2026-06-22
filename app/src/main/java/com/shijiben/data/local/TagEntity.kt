package com.shijiben.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: Int,
    val sortOrder: Int = 0,
    val createdAt: Long,
    val updatedAt: Long
)
