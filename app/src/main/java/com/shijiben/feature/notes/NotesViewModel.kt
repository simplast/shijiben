package com.shijiben.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotesViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val allNotes: StateFlow<List<NoteEntity>> = noteRepository.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _editing = MutableStateFlow<NoteEntity?>(null)
    val editing: StateFlow<NoteEntity?> = _editing.asStateFlow()

    private val _sheetOpen = MutableStateFlow(false)
    val sheetOpen: StateFlow<Boolean> = _sheetOpen.asStateFlow()

    fun startCreate() {
        _editing.value = null
        _sheetOpen.value = true
    }

    fun startEdit(note: NoteEntity) {
        _editing.value = note
        _sheetOpen.value = true
    }

    fun closeSheet() {
        _sheetOpen.value = false
        _editing.value = null
    }

    suspend fun save(content: String): Boolean {
        val c = content.trim()
        if (c.isEmpty()) return false
        val existing = _editing.value
        if (existing != null) {
            noteRepository.updateNote(existing.copy(content = c))
        } else {
            noteRepository.createNote(content = c, timestamp = System.currentTimeMillis())
        }
        _sheetOpen.value = false
        _editing.value = null
        return true
    }

    suspend fun delete(note: NoteEntity) {
        noteRepository.deleteNoteById(note.id)
        _sheetOpen.value = false
        _editing.value = null
    }
}
