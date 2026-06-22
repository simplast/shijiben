package com.shijiben.data.model

enum class EventStatus(val value: Int) {
    NotStarted(0),
    InProgress(1),
    Completed(2);
    companion object {
        fun fromValue(v: Int): EventStatus = entries.firstOrNull { it.value == v } ?: NotStarted
    }
}
