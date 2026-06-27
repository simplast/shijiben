package com.shijiben.feature.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import com.shijiben.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

private const val NEW_EVENT_DURATION_MAX = 180   // 与 TimeRangeSlider 默认一致
private const val DURATION_HARD_CEILING = 480

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _startMinutes = MutableStateFlow(540) // 9:00 默认
    val startMinutes: StateFlow<Int> = _startMinutes.asStateFlow()

    private val _durationMinutes = MutableStateFlow(10) // 10 分钟默认
    val durationMinutes: StateFlow<Int> = _durationMinutes.asStateFlow()

    private val _durationMax = MutableStateFlow(NEW_EVENT_DURATION_MAX)
    val durationMax: StateFlow<Int> = _durationMax.asStateFlow()

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    private var editingId: Long? = null
    private var originalStatus: Int? = null   // preserved across initEdit so save() can keep in-progress events null-ended

    fun onTitleChange(v: String) { _title.value = v }
    fun onNoteChange(v: String) { _note.value = v }
    fun onStartChange(v: Int) { _startMinutes.value = v }
    fun onDurationChange(v: Int) { _durationMinutes.value = v }

    /** 进入"新建"模式：基于当前时间初始化（取整到最近一刻钟） */
    fun initNew() {
        editingId = null
        originalStatus = null
        _title.value = ""
        _note.value = ""
        val cal = Calendar.getInstance(TimeZone.getDefault())
        val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val snapped = ((nowMin / 15) * 15).coerceIn(300, 1440)
        _startMinutes.value = snapped
        _durationMinutes.value = 10
        _durationMax.value = NEW_EVENT_DURATION_MAX
    }

    /** 进入"编辑"模式：加载已有事件 */
    fun initEdit(event: EventEntity) {
        editingId = event.id
        originalStatus = event.status
        _title.value = event.title
        _note.value = event.note ?: ""
        val cal = Calendar.getInstance(TimeZone.getDefault())

        // 没有设置时间（NotStarted 且无 endTime）：默认使用当前时间
        if (event.endTime == null && event.status == EventStatus.NotStarted.value) {
            val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
            _startMinutes.value = ((nowMin / 15) * 15).coerceIn(0, 1440)
            _durationMinutes.value = 0
            _durationMax.value = NEW_EVENT_DURATION_MAX
            return
        }
        // 进行中（InProgress 且 endTime=null）：保持无结束时间，保留原始 startTime
        if (event.endTime == null && event.status == EventStatus.InProgress.value) {
            cal.timeInMillis = event.startTime
            val startMin = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(0, 1440)
            _startMinutes.value = startMin
            _durationMinutes.value = 0
            _durationMax.value = NEW_EVENT_DURATION_MAX
            return
        }

        cal.timeInMillis = event.startTime
        val start = (cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)).coerceIn(300, 1440)
        _startMinutes.value = start
        val rawDuration = if (event.endTime != null) {
            // 用绝对时间差计算，避免跨午夜时 end < start 的分钟差为负
            ((event.endTime - event.startTime) / 60_000L).toInt().coerceAtLeast(0)
        } else {
            60   // defensive; unreachable after Step 2 (both null-endTime branches return early)
        }
        _durationMinutes.value = rawDuration.coerceIn(0, DURATION_HARD_CEILING)
        // 编辑时上限 = max(默认 3h, 实际时长向上取整到整点)，保证滑块能表示当前值
        _durationMax.value = maxOf(NEW_EVENT_DURATION_MAX, ((rawDuration + 59) / 60) * 60)
            .coerceAtMost(DURATION_HARD_CEILING)
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
            duration == 0 -> originalStatus ?: EventStatus.NotStarted.value
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
                    note = _note.value.ifBlank { null }
                )
            )
        } else {
            eventRepository.createEvent(
                title = title,
                startTime = start,
                endTime = actualEnd,
                status = status,
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
