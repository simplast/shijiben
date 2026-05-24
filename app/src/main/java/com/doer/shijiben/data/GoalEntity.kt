package com.doer.shijiben.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** The name of the event or category this goal applies to. */
    val name: String,
    /** Target duration in minutes. */
    val targetMinutes: Long,
    /** Year-Week key (e.g., "2024-W20") to identify the week. */
    val weekKey: String
)
