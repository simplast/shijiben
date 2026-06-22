package com.shijiben.feature.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.local.TagEntity
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TagsViewModel @Inject constructor(
    private val tagRepository: TagRepository,
    private val eventRepository: EventRepository
) : ViewModel() {

    val tags: StateFlow<List<TagEntity>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editing = MutableStateFlow<TagEntity?>(null)
    val editing: StateFlow<TagEntity?> = _editing.asStateFlow()

    // null 表示未编辑/新建；与 _sheetOpen 配合区分"新建模式"与"关闭弹窗"
    private val _sheetOpen = MutableStateFlow(false)
    val sheetOpen: StateFlow<Boolean> = _sheetOpen.asStateFlow()

    fun startCreate() {
        _editing.value = null
        _sheetOpen.value = true
    }

    fun startEdit(tag: TagEntity) {
        _editing.value = tag
        _sheetOpen.value = true
    }

    fun closeSheet() {
        _sheetOpen.value = false
        _editing.value = null
    }

    suspend fun save(name: String, color: Int): Boolean {
        val n = name.trim()
        if (n.isEmpty()) return false
        val existing = _editing.value
        if (existing != null) {
            tagRepository.updateTag(existing.copy(name = n, color = color))
        } else {
            tagRepository.createTag(name = n, color = color, sortOrder = tags.value.size)
        }
        _editing.value = null
        _sheetOpen.value = false
        return true
    }

    suspend fun delete(tag: TagEntity) {
        // 先清除事件引用，再删除标签
        eventRepository.clearTagReference(tag.id)
        tagRepository.deleteTag(tag)
        _editing.value = null
        _sheetOpen.value = false
    }
}
