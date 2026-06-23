package com.shijiben.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.local.TagEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class TagRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: TagRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = TagRepository(db.tagDao())
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun createTag_thenGetById_returnsTag() = runTest {
        val id = repo.createTag(name = "阅读", color = 0xFF3B82F6.toInt(), sortOrder = 0)
        val tag = repo.getTagById(id)!!
        assertThat(tag.name).isEqualTo("阅读")
        assertThat(tag.color).isEqualTo(0xFF3B82F6.toInt())
    }

    @Test
    fun getAllTags_ordersBySortOrderThenId() = runTest {
        repo.createTag("c", 1, sortOrder = 2)
        repo.createTag("a", 2, sortOrder = 0)
        repo.createTag("b", 3, sortOrder = 1)
        val tags = repo.getAllTags().first()
        assertThat(tags.map { it.name }).containsExactly("a", "b", "c").inOrder()
    }

    @Test
    fun updateTag_changesNameAndColor() = runTest {
        val id = repo.createTag("旧", 1, 0)
        val tag = repo.getTagById(id)!!
        repo.updateTag(tag.copy(name = "新", color = 2))
        val updated = repo.getTagById(id)!!
        assertThat(updated.name).isEqualTo("新")
        assertThat(updated.color).isEqualTo(2)
    }

    @Test
    fun deleteTagById_removesTag() = runTest {
        val id = repo.createTag("temp", 1, 0)
        repo.deleteTagById(id)
        assertThat(repo.getTagById(id)).isNull()
    }
}
