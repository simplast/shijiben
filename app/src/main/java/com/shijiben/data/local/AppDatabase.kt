package com.shijiben.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class Converters {
    // 暂时不需要复杂转换，status 用 Int 存储
}

@Database(
    entities = [EventEntity::class, NoteEntity::class],
    version = 2,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun noteDao(): NoteDao
}

/**
 * v1 → v2：移除标签功能。
 * - events 表删除 tagId 列（SQLite < 3.35 不支持 DROP COLUMN，用建新表-拷数据-删旧-改名）。
 * - tags 表整体删除。
 * - 重建 events 上的索引。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `events_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`startTime` INTEGER NOT NULL, " +
                "`endTime` INTEGER, " +
                "`status` INTEGER NOT NULL, " +
                "`note` TEXT, " +
                "`createdAt` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL)"
        )
        db.execSQL(
            "INSERT INTO `events_new` (`id`,`title`,`startTime`,`endTime`,`status`,`note`,`createdAt`,`updatedAt`) " +
                "SELECT `id`,`title`,`startTime`,`endTime`,`status`,`note`,`createdAt`,`updatedAt` FROM `events`"
        )
        db.execSQL("DROP TABLE IF EXISTS `events`")
        db.execSQL("DROP TABLE IF EXISTS `tags`")
        db.execSQL("ALTER TABLE `events_new` RENAME TO `events`")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_start_time` ON `events`(`startTime`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_events_status_start` ON `events`(`status`, `startTime`)")
    }
}
