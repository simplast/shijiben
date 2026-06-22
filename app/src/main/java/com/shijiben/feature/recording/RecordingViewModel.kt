package com.shijiben.feature.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.TagEntity
import com.shijiben.data.model.EventStatus
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.TagRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val tagRepository: TagRepository
) : ViewModel() {

    val tags: StateFlow<List<TagEntity>> = tagRepository.getAllTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _startMinutes = MutableStateFlow(0)
    val startMinutes: StateFlow<Int> = _startMinutes.asStateFlow()

    private val _endMinutes = MutableStateFlow(30)
    val endMinutes: StateFlow<Int> = _endMinutes.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private var editingId: Long? = null
    private var currentStatus: Int = EventStatus.NotStarted.value

    fun onTitleChange(v: String) { _title.value = v }
    fun onNoteChange(v: String) { _note.value = v }
    fun onTimeChange(start: Int, end: Int) {
        _startMinutes.value = start
        _endMinutes.value = end
    }
    fun onTagSelected(id: Long?) { _selectedTagId.value = id }

    /** 进入"新建"模式：基于当前时间初始化默认范围（当前时刻 + 30 分钟） */
    fun initNew() {
        editingId = null
        _title.value = ""
        _note.value = ""
        _selectedTagId.value = null
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        _startMinutes.value = nowMin.coerceIn(0, 1439)
        _endMinutes.value = (nowMin + 30).coerceIn(1, 1440)
    }

    /** 进入"编辑"模式：加载已有事件 */
    fun initEdit(event: EventEntity) {
        editingId = event.id
        currentStatus = event.status
        _title.value = event.title
        _note.value = event.note ?: ""
        _selectedTagId.value = event.tagId
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = event.startTime
        _startMinutes.value = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(0, 1439)
        _endMinutes.value = if (event.endTime != null) {
            val cal2 = Calendar.getInstance(TimeZone.getDefault())
            cal2.timeInMillis = event.endTime
            val em = cal2.get(Calendar.HOUR_OF_DAY) * 60 + cal2.get(Calendar.MINUTE)
            if (event.endTime >= event.startTime && em <= _startMinutes.value) 1440 else em
        } else {
            (_startMinutes.value + 30).coerceIn(1, 1440)
        }
    }

    /** 保存（新建或更新）。返回 true 表示成功。viewingDate 为查看日期 Triple<年, 月, 日> */
    suspend fun save(viewingDate: Triple<Int, Int, Int>): Boolean {
        val title = _title.value.trim()
        if (title.isEmpty()) return false
        val (y, m, d) = viewingDate
        val start = minutesToTimestamp(y, m, d, _startMinutes.value)
        val end = minutesToTimestamp(y, m, d, _endMinutes.value)
        // 若 end <= start，说明跨日，end 设为次日
        val actualEnd = if (end <= start) end + 24L * 3600 * 1000 else end
        val eid = editingId
        if (eid != null) {
            val existing = eventRepository.getEventById(eid) ?: return false
            eventRepository.updateEvent(
                existing.copy(
                    title = title,
                    startTime = start,
                    endTime = actualEnd,
                    tagId = _selectedTagId.value,
                    note = _note.value.ifBlank { null }
                )
            )
        } else {
            eventRepository.createEvent(
                title = title,
                startTime = start,
                endTime = actualEnd,
                tagId = _selectedTagId.value,
                note = _note.value.ifBlank { null }
            )
        }
        return true
    }

    suspend fun delete(): Boolean {
        val eid = editingId ?: return false
        eventRepository.deleteEventById(eid)
        return true
    }

    suspend fun markNotStarted() {
        val eid = editingId ?: return
        eventRepository.markNotStarted(eid)
        currentStatus = EventStatus.NotStarted.value
    }

    suspend fun markInProgress() {
        val eid = editingId ?: return
        eventRepository.markInProgress(eid)
        currentStatus = EventStatus.InProgress.value
    }

    suspend fun markCompleted() {
        val eid = editingId ?: return
        eventRepository.markCompleted(eid)
        currentStatus = EventStatus.Completed.value
    }

    private fun minutesToTimestamp(y: Int, m: Int, d: Int, minutes: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(y, m - 1, d, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MINUTE, minutes)
        return cal.timeInMillis
    }
}
