package com.doer.shijiben.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.EventRepository
import com.doer.shijiben.data.TimeFormats
import android.content.Context
import android.net.Uri
import com.doer.shijiben.data.GoalEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

data class WeeklyStats(
    val totalMinutes: Long,
    val daySummaries: List<DaySummary>,
    val topEvents: List<EventSummary>,
    val goalProgress: List<GoalProgress> = emptyList()
)

data class MonthlyStats(
    val monthLabel: String,
    val totalMinutes: Long,
    val daySummaries: List<DaySummary>,
    val categorySummaries: List<EventSummary>,
    val topEvents: List<EventSummary>
)

data class DaySummary(val dateLabel: String, val totalMinutes: Long, val count: Int)
data class EventSummary(val name: String, val totalMinutes: Long, val count: Int)
data class GoalProgress(val name: String, val targetMinutes: Long, val currentMinutes: Long)

@OptIn(ExperimentalCoroutinesApi::class)
class EventViewModel(
    private val repository: EventRepository,
) : ViewModel() {

    private val selectedDateFormatter =
        DateTimeFormatter.ofPattern("yyyy年M月d日 EEEE", java.util.Locale.CHINA)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate

    val selectedDateLabel: StateFlow<String> =
        _selectedDate.map { it.format(selectedDateFormatter) }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            "",
        )

    val eventsForSelectedDay: StateFlow<List<EventEntity>> =
        _selectedDate
            .flatMapLatest { date: LocalDate ->
                val dayKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                repository.observeEventsForDay(dayKey)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList(),
            )

    val recentDistinctEventNames: StateFlow<List<String>> =
        repository.observeRecentDistinctEventNames().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList(),
        )

    val activeEvent: StateFlow<EventEntity?> =
        repository.observeActiveEvent().stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null,
        )

    /** A flow that emits the current system time in millis every minute. */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(10_000) // Update every 10 seconds for better responsiveness than 60s
        }
    }

    val activeEventElapsedMinutes: StateFlow<Long> =
        combine(activeEvent, ticker) { event, now ->
            if (event != null) {
                (now - event.startTimeMillis) / 60_000L
            } else {
                0L
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            0L,
        )

    val weeklyStats: StateFlow<WeeklyStats?> =
        _selectedDate.flatMapLatest { date ->
            val start = date.minusDays(6) // Looking at 7 days including current
            val startKey = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endKey = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            val weekKey = getWeekKey(date)
            
            combine(
                repository.observeEventsInRange(startKey, endKey),
                repository.observeGoalsForWeek(weekKey)
            ) { events, goals ->
                calculateWeeklyStats(events, goals)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    val monthlyStats: StateFlow<MonthlyStats?> =
        _selectedDate.flatMapLatest { date ->
            val start = date.withDayOfMonth(1)
            val end = date.withDayOfMonth(date.lengthOfMonth())
            val startKey = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val endKey = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
            
            repository.observeEventsInRange(startKey, endKey).map { events ->
                calculateMonthlyStats(date, events)
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            null
        )

    private fun getWeekKey(date: LocalDate): String {
        val year = date.get(IsoFields.WEEK_BASED_YEAR)
        val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        return "$year-W$week"
    }

    private fun calculateWeeklyStats(events: List<EventEntity>, goals: List<GoalEntity>): WeeklyStats {
        val completedEvents = events.filter { it.status == "COMPLETED" }
        val totalMinutes = completedEvents.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L }
        
        val daySummaries = completedEvents.groupBy { it.dayKey }
            .map { (dayKey, dayEvents) ->
                DaySummary(
                    dateLabel = dayKey,
                    totalMinutes = dayEvents.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L },
                    count = dayEvents.size
                )
            }.sortedBy { it.dateLabel }

        val eventGroups = completedEvents.groupBy { it.name.trim() }
        val topEvents = eventGroups
            .map { (name, eventList) ->
                EventSummary(
                    name = name,
                    totalMinutes = eventList.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L },
                    count = eventList.size
                )
            }.sortedByDescending { it.totalMinutes }

        val goalProgress = goals.map { goal ->
            val current = eventGroups[goal.name.trim()]?.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L } ?: 0L
            GoalProgress(goal.name, goal.targetMinutes, current)
        }

        return WeeklyStats(totalMinutes, daySummaries, topEvents, goalProgress)
    }

    private fun calculateMonthlyStats(date: LocalDate, events: List<EventEntity>): MonthlyStats {
        val completedEvents = events.filter { it.status == "COMPLETED" }
        val totalMinutes = completedEvents.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L }
        
        val daySummaries = completedEvents.groupBy { it.dayKey }
            .map { (dayKey, dayEvents) ->
                DaySummary(
                    dateLabel = dayKey,
                    totalMinutes = dayEvents.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L },
                    count = dayEvents.size
                )
            }.sortedBy { it.dateLabel }

        val categorySummaries = completedEvents.filter { it.category != null }
            .groupBy { it.category!!.trim() }
            .map { (cat, eventList) ->
                EventSummary(
                    name = cat,
                    totalMinutes = eventList.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L },
                    count = eventList.size
                )
            }.sortedByDescending { it.totalMinutes }

        val topEvents = completedEvents.groupBy { it.name.trim() }
            .map { (name, eventList) ->
                EventSummary(
                    name = name,
                    totalMinutes = eventList.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L },
                    count = eventList.size
                )
            }.sortedByDescending { it.totalMinutes }

        return MonthlyStats(
            monthLabel = date.format(DateTimeFormatter.ofPattern("yyyy年MM月")),
            totalMinutes = totalMinutes,
            daySummaries = daySummaries,
            categorySummaries = categorySummaries,
            topEvents = topEvents
        )
    }

    fun upsertGoal(name: String, targetHours: Int) {
        viewModelScope.launch {
            val weekKey = getWeekKey(_selectedDate.value)
            repository.upsertGoal(GoalEntity(
                name = name.trim(),
                targetMinutes = targetHours * 60L,
                weekKey = weekKey
            ))
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun smartMergeEvents() {
        viewModelScope.launch {
            val events = eventsForSelectedDay.value
                .filter { it.status == "COMPLETED" }
                .sortedBy { it.startTimeMillis }
            
            if (events.size < 2) return@launch

            val toDelete = mutableListOf<EventEntity>()
            val toUpdate = mutableListOf<EventEntity>()

            var current = events[0]
            for (i in 1 until events.size) {
                val next = events[i]
                // Criteria: Same name AND (Overlap OR Gap < 5 minutes)
                val gap = next.startTimeMillis - current.endTimeMillis
                if (current.name.trim() == next.name.trim() && gap < 5 * 60_000L) {
                    // Merge next into current
                    current = current.copy(
                        endTimeMillis = maxOf(current.endTimeMillis, next.endTimeMillis)
                    )
                    toDelete.add(next)
                } else {
                    if (current !== events[i-1]) { // If current was modified/merged
                        toUpdate.add(current)
                    }
                    current = next
                }
            }
            // Add the last one if it was part of a merge
            if (current !== events.last() || (toUpdate.isEmpty() && toDelete.isNotEmpty())) {
                // This logic is a bit simplified, but covers the common cases
                // For a robust implementation, we'd track the origin better
                toUpdate.add(current)
            }

            toDelete.forEach { repository.delete(it) }
            toUpdate.forEach { repository.upsert(it) }
        }
    }

    fun upsert(event: EventEntity, onResult: (Throwable?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (event.status == "COMPLETED") {
                    require(event.endTimeMillis >= event.startTimeMillis) {
                        "结束时间不能早于开始时间"
                    }
                }
                repository.upsert(event)
                onResult(null)
            } catch (t: Throwable) {
                onResult(t)
            }
        }
    }

    fun stopActiveEvent() {
        viewModelScope.launch {
            val currentActive = activeEvent.value ?: return@launch
            val now = System.currentTimeMillis()
            upsert(currentActive.copy(
                endTimeMillis = now,
                status = "COMPLETED"
            ))
        }
    }

    fun quickStartEvent(name: String) {
        viewModelScope.launch {
            // First stop any current active event
            stopActiveEvent()
            
            val now = System.currentTimeMillis()
            val newEvent = EventEntity(
                name = name,
                startTimeMillis = now,
                endTimeMillis = now, // Placeholder
                dayKey = "", // Will be derived in repository
                status = "IN_PROGRESS"
            )
            repository.upsert(newEvent)
        }
    }

    suspend fun getEvent(id: Long): EventEntity? = repository.getById(id)

    fun delete(event: EventEntity, onResult: (Throwable?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                repository.delete(event)
                onResult(null)
            } catch (t: Throwable) {
                onResult(t)
            }
        }
    }

    fun exportDataAsJson(context: Context, uri: Uri, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            try {
                val events = repository.getAllEvents()
                val json = eventsToJson(events)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(json.toByteArray())
                    }
                }
                onResult(null)
            } catch (t: Throwable) {
                onResult(t)
            }
        }
    }

    fun exportDataAsCsv(context: Context, uri: Uri, onResult: (Throwable?) -> Unit) {
        viewModelScope.launch {
            try {
                val events = repository.getAllEvents()
                val csv = eventsToCsv(events)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(csv.toByteArray())
                    }
                }
                onResult(null)
            } catch (t: Throwable) {
                onResult(t)
            }
        }
    }

    private fun eventsToCsv(events: List<EventEntity>): String {
        val sb = StringBuilder()
        sb.append("ID,名称,开始时间,结束时间,日期,状态,分类,备注\n")
        events.forEach { event ->
            sb.append("${event.id},")
            sb.append("\"${escapeCsv(event.name)}\",")
            sb.append("${TimeFormats.formatTimeMillis(event.startTimeMillis)},")
            sb.append("${if (event.status == "COMPLETED") TimeFormats.formatTimeMillis(event.endTimeMillis) else "进行中"},")
            sb.append("${event.dayKey},")
            sb.append("${event.status},")
            sb.append("\"${escapeCsv(event.category ?: "")}\",")
            sb.append("\"${escapeCsv(event.note ?: "")}\"\n")
        }
        return sb.toString()
    }

    private fun escapeCsv(s: String): String {
        return s.replace("\"", "\"\"")
    }

    private fun eventsToJson(events: List<EventEntity>): String {
        val sb = StringBuilder()
        sb.append("[\n")
        events.forEachIndexed { index, event ->
            sb.append("  {\n")
            sb.append("    \"id\": ${event.id},\n")
            sb.append("    \"name\": \"${escapeJson(event.name)}\",\n")
            sb.append("    \"startTimeMillis\": ${event.startTimeMillis},\n")
            sb.append("    \"endTimeMillis\": ${event.endTimeMillis},\n")
            sb.append("    \"dayKey\": \"${event.dayKey}\",\n")
            sb.append("    \"status\": \"${event.status}\",\n")
            sb.append("    \"category\": ${if (event.category != null) "\"${escapeJson(event.category)}\"" else "null"},\n")
            sb.append("    \"note\": ${if (event.note != null) "\"${escapeJson(event.note)}\"" else "null"}\n")
            sb.append("  }${if (index < events.size - 1) "," else ""}\n")
        }
        sb.append("]")
        return sb.toString()
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    companion object {
        fun factory(repository: EventRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(EventViewModel::class.java))
                    return EventViewModel(repository) as T
                }
            }
    }
}
