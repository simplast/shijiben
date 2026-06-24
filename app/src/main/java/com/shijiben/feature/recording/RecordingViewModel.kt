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

    private val _startMinutes = MutableStateFlow(540) // 9:00 默认
    val startMinutes: StateFlow<Int> = _startMinutes.asStateFlow()

    private val _durationMinutes = MutableStateFlow(10) // 10 分钟默认
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    private val _selectedTagId = MutableStateFlow<Long?>(null)
    val selectedTagId: StateFlow<Long?> = _selectedTagId.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private var editingId: Long? = null

    fun onTitleChange(v: String) { _title.value = v }
    fun onNoteChange(v: String) { _note.value = v }
    fun onStartChange(v: Int) { _startMinutes.value = v }
    fun onDurationChange(v: Int) { _durationMinutes.value = v }
    fun onTagSelected(id: Long?) { _selectedTagId.value = id }

    /** 进入"新建"模式：基于当前时间初始化（取整到最近一刻钟） */
    fun initNew() {
        editingId = null
        _title.value = ""
        _note.value = ""
        _selectedTagId.value = null
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val snapped = ((nowMin / 15) * 15).coerceIn(300, 1440)
        _startMinutes.value = snapped
        _durationMinutes.value = 10
    }

    /** 进入"编辑"模式：加载已有事件 */
    fun initEdit(event: EventEntity) {
        editingId = event.id
        _title.value = event.title
        _note.value = event.note ?: ""
        _selectedTagId.value = event.tagId
        val cal = Calendar.getInstance(TimeZone.getDefault())

        // 如果没有设置时间（NotStarted 且无 endTime），默认使用当前时间
        if (event.endTime == null && event.status == EventStatus.NotStarted.value) {
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            _startMinutes.value = ((nowMin / 15) * 15).coerceIn(0, 1440)
            _durationMinutes.value = 0
            return
        }

        cal.timeInMillis = event.startTime
        val start = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(300, 1440)
        _startMinutes.value = start
        _durationMinutes.value = if (event.endTime != null) {
            val cal2 = Calendar.getInstance(TimeZone.getDefault())
            cal2.timeInMillis = event.endTime
            val end = cal2.get(Calendar.HOUR_OF_DAY) * 60 + cal2.get(Calendar.MINUTE)
            (end - start).coerceIn(0, 480)
        } else {
            60
        }
    }

    /**
     * 保存（新建或更新）。返回 true 表示成功。
     * 状态自动推算：duration=0→未开始, 结束时间在未来→进行中, 结束时间在过去→已完成
     */
    suspend fun save(viewingDate: Triple<Int, Int, Int>): Boolean {
        val title = _title.value.trim()
        if (title.isEmpty()) return false
        val (y, m, d) = viewingDate
        val start = minutesToTimestamp(y, m, d, _startMinutes.value)
        val duration = _durationMinutes.value
        val now = System.currentTimeMillis()

        val actualEnd: Long? = if (duration > 0) {
            minutesToTimestamp(y, m, d, _startMinutes.value + duration)
        } else {
            null
        }

        val status = when {
            duration == 0 -> EventStatus.NotStarted.value
            actualEnd != null && now > actualEnd -> EventStatus.Completed.value
            else -> EventStatus.InProgress.value
        }

        val eid = editingId
        if (eid != null) {
            val existing = eventRepository.getEventById(eid) ?: return false
            eventRepository.updateEvent(
                existing.copy(
                    title = title,
                    startTime = start,
                    endTime = actualEnd,
                    status = status,
                    tagId = _selectedTagId.value,
                    note = _note.value.ifBlank { null }
                )
            )
        } else {
            eventRepository.createEvent(
                title = title,
                startTime = start,
                endTime = actualEnd,
                status = status,
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

    private fun minutesToTimestamp(y: Int, m: Int, d: Int, minutes: Int): Long {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.set(y, m - 1, d, 0, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        cal.add(Calendar.MINUTE, minutes)
        return cal.timeInMillis
    }
}
