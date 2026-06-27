package com.shijiben.data

import android.content.Context
import androidx.room.Room
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.local.EventDao
import com.shijiben.data.local.MIGRATION_1_2
import com.shijiben.data.local.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "shijiben.db")
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides
    fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
}
