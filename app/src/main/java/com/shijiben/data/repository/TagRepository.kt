package com.shijiben.data.repository

import com.shijiben.data.local.TagDao
import com.shijiben.data.local.TagEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagDao: TagDao
) {
    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun getTagById(id: Long): TagEntity? = tagDao.getTagById(id)

    suspend fun createTag(name: String, color: Int, sortOrder: Int = 0): Long {
        val now = System.currentTimeMillis()
        val tag = TagEntity(
            name = name,
            color = color,
            sortOrder = sortOrder,
            createdAt = now,
            updatedAt = now
        )
        return tagDao.insertTag(tag)
    }

    suspend fun updateTag(tag: TagEntity) {
        tagDao.updateTag(tag.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTag(tag: TagEntity) {
        tagDao.deleteTag(tag)
    }

    suspend fun deleteTagById(id: Long) = tagDao.deleteTagById(id)
}
