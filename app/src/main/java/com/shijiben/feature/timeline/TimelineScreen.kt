package com.shijiben.feature.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.R
import com.shijiben.data.local.EventEntity
import com.shijiben.feature.notes.NoteEditorSheet
import com.shijiben.feature.notes.NotesViewModel
import com.shijiben.feature.recording.RecordingSheet
import com.shijiben.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun TimelineScreen(
    onTagsClick: () -> Unit = {},
    onNotesClick: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val date by viewModel.viewingDate.collectAsStateWithLifecycle()
    val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)

    var showSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    val inputFocusRequester = remember { FocusRequester() }
    val editingNote by notesViewModel.editing.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        // 像素山水背景（弱化）
        Image(
            painter = painterResource(com.shijiben.R.drawable.rainbow_bg),
            contentDescription = "像素山水背景",
            modifier = Modifier.fillMaxSize().alpha(0.2f),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Column(modifier = Modifier.fillMaxSize().padding(bottom = 6.dp)) {
            PixelCard(modifier = Modifier.fillMaxWidth(), shadow = false, backgroundColor = Surface) {
                // 彩虹像素装饰条
                Row(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                    val trimColors = listOf(
                        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
                        Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
                        Color(0xFF6366F1), Color(0xFFA855F7)
                    )
                    for (i in 0 until 80) {
                        Box(
                            modifier = Modifier
                                .weight(1f).fillMaxHeight()
                                .background(trimColors[i % 8])
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTagsClick) {
                            Icon(Icons.Default.Star, contentDescription = "标签", tint = TextSecondary)
                        }
                        IconButton(onClick = { viewModel.goToPreviousDay() }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前一天", tint = TextPrimary)
                        }
                    }
                    val onToday = isToday(date)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.then(
                            if (onToday) Modifier
                            else Modifier.clickable { viewModel.goToToday() }
                        )
                    ) {
                        Text(
                            text = formatDateCompact(date),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (onToday) TextPrimary else Accent
                        )
                        if (!onToday) {
                            Text(
                                text = "回今天",
                                fontSize = 10.sp,
                                color = TextTertiary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNotesClick) {
                            Icon(Icons.Default.Edit, contentDescription = "随笔", tint = TextSecondary)
                        }
                        IconButton(onClick = { viewModel.goToNextDay() }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "后一天", tint = TextPrimary)
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.weight(1f).fillMaxWidth().fillMaxHeight()) {
                Box(modifier = Modifier.width(40.dp).fillMaxHeight().padding(top = 12.dp, start = 12.dp)) {
                    DayProgressBar(
                        events = events,
                        viewingDate = date,
                        nowHour = nowHour
                    )
                }
                Box(modifier = Modifier.fillMaxHeight().weight(1f).padding(start = 8.dp, top = 12.dp, end = 12.dp)) {
                    EventList(
                        events = events,
                        onEventClick = { event ->
                            editingEvent = event
                            showSheet = true
                        },
                        onStartEvent = { event -> viewModel.markInProgress(event.id) },
                        onStopEvent = { event -> viewModel.markCompleted(event.id) }
                    )
                }
            }
        }

        // 底部像素风格输入框
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text("记一件事（无时间）...", color = TextTertiary, fontWeight = FontWeight.Medium)
                },
                trailingIcon = {
                    IconButton(onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.quickAddEvent(inputText.trim())
                            inputText = ""
                        }
                    }) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "添加",
                            tint = Primary
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                textStyle = TextStyle(
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (inputText.isNotBlank()) {
                            viewModel.quickAddEvent(inputText.trim())
                            inputText = ""
                        }
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(inputFocusRequester)
            )
        }

        if (showSheet) {
            RecordingSheet(
                viewingDate = date,
                editingEvent = editingEvent,
                onDismiss = { showSheet = false },
                onSaved = { viewModel.refresh(); showSheet = false }
            )
        }

        if (showNoteSheet) {
            NoteEditorSheet(
                editing = editingNote,
                onDismiss = { showNoteSheet = false; notesViewModel.closeSheet() },
                onSave = { content -> val ok = notesViewModel.save(content); if (ok) viewModel.refresh(); ok },
                onDelete = { note -> notesViewModel.delete(note); viewModel.refresh() }
            )
        }
    }
}

@Composable
fun EventList(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    onStartEvent: (EventEntity) -> Unit,
    onStopEvent: (EventEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
        if (events.isEmpty()) {
            Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
                Text(
                    text = "今天还是空白",
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp,
                    color = TextSecondary
                )
            }
        } else {
            for (event in events) {
                EventCard(
                    event = event,
                    onClick = { onEventClick(event) },
                    onStart = { onStartEvent(event) },
                    onStop = { onStopEvent(event) }
                )
            }
        }
    }
}

@Composable
fun EventCard(
    event: EventEntity,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 未完成事项的开始/停止按钮
            if (event.status != 2) {
                IconButton(
                    onClick = {
                        if (event.status == 0) onStart() else onStop()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    if (event.status == 0) {
                        // 未开始 → 三角形播放按钮
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = "开始",
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        // 进行中 → 方形停止按钮
                        Canvas(
                            modifier = Modifier.size(16.dp)
                        ) {
                            drawRect(
                                color = Primary,
                                size = size
                            )
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
            }

            // 标题
            Text(
                text = event.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = when (event.status) {
                    2 -> Success
                    0 -> TextTertiary
                    else -> TextPrimary
                },
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            Spacer(Modifier.width(8.dp))

            // 时间范围 + 耗时 badge
            if (event.endTime != null) {
                Text(
                    text = "${formatTime(event.startTime)}-${formatTime(event.endTime)}",
                    fontSize = 12.sp,
                    color = when (event.status) {
                        2 -> Secondary
                        0 -> TextTertiary
                        else -> TextSecondary
                    }
                )
                Spacer(Modifier.width(6.dp))
                val durationText = formatDurationShort(event.startTime, event.endTime)
                Box(
                    modifier = Modifier
                        .background(Accent, RoundedCornerShape(0.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = durationText,
                        fontSize = 10.sp,
                        color = TextOnPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatTime(ts: Long): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(java.util.Date(ts))

private fun formatDurationShort(start: Long, end: Long): String {
    val minutes = (end - start) / 1000 / 60
    return if (minutes < 60) {
        "${minutes}min"
    } else {
        "%.1fhours".format(minutes / 60.0)
    }
}

private fun formatDate(date: Triple<Int, Int, Int>): String {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.set(date.first, date.second - 1, date.third, 0, 0, 0)
    return SimpleDateFormat("MM月dd日", Locale.getDefault()).format(cal.time)
}
private fun formatDateCompact(date: Triple<Int, Int, Int>): String {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.set(date.first, date.second - 1, date.third, 0, 0, 0)
    val weekdayNames = arrayOf("日", "一", "二", "三", "四", "五", "六")
    val weekday = weekdayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
    return "${date.second}/${date.third}/$weekday"
}
private fun hourOfDay(timestamp: Long): Int {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.timeInMillis = timestamp
    return cal.get(Calendar.HOUR_OF_DAY)
}
private fun isToday(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    return date == Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
}
private fun isPastDay(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val today = Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH))
    return date.first < today.first || (date.first == today.first && (date.second < today.second || (date.second == today.second && date.third < today.third)))
}
