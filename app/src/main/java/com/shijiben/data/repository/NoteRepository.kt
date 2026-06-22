package com.shijiben.data.repository

import com.shijiben.data.local.NoteDao
import com.shijiben.data.local.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getNotesByDateRange(startOfDay: Long, endOfDay: Long): Flow<List<NoteEntity>> =
        noteDao.getNotesByDate(startOfDay, endOfDay)

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    suspend fun getNoteById(id: Long): NoteEntity? = noteDao.getNoteById(id)

    suspend fun createNote(content: String, timestamp: Long): Long {
        val now = System.currentTimeMillis()
        val note = NoteEntity(
            content = content,
            timestamp = timestamp,
            createdAt = now,
            updatedAt = now
        )
        return noteDao.insertNote(note)
    }

    suspend fun updateNote(note: NoteEntity) {
        noteDao.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteNoteById(id: Long) = noteDao.deleteNoteById(id)
}
