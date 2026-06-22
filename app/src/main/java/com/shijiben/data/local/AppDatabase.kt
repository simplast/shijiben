package com.shijiben.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    // 暂时不需要复杂转换，status 用 Int 存储
}

@Database(
    entities = [EventEntity::class, NoteEntity::class, TagEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
}
