package com.shijiben.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NoteRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: NoteRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = NoteRepository(db.noteDao())
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun createNote_thenGetById_returnsNote() = runTest {
        val now = System.currentTimeMillis()
        val id = repo.createNote("想法", now)
        val note = repo.getNoteById(id)!!
        assertThat(note.content).isEqualTo("想法")
        assertThat(note.timestamp).isEqualTo(now)
    }

    @Test
    fun getNotesByDateRange_filtersToRange() = runTest {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(Calendar.HOUR_OF_DAY, 10); cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
        val inRange = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val outOfRange = cal.timeInMillis

        repo.createNote("今天", inRange)
        repo.createNote("明天", outOfRange)

        val dayStart = inRange
        val dayEnd = inRange + 24L * 3600 * 1000
        val notes = repo.getNotesByDateRange(dayStart, dayEnd).first()
        assertThat(notes).hasSize(1)
        assertThat(notes.first().content).isEqualTo("今天")
    }

    @Test
    fun updateNote_changesContent() = runTest {
        val id = repo.createNote("旧", System.currentTimeMillis())
        val note = repo.getNoteById(id)!!
        repo.updateNote(note.copy(content = "新"))
        assertThat(repo.getNoteById(id)!!.content).isEqualTo("新")
    }

    @Test
    fun deleteNoteById_removesNote() = runTest {
        val id = repo.createNote("temp", System.currentTimeMillis())
        repo.deleteNoteById(id)
        assertThat(repo.getNoteById(id)).isNull()
    }
}
