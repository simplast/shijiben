package com.shijiben.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import com.shijiben.feature.notes.NoteEditorSheet
import com.shijiben.feature.notes.NotesViewModel
import com.shijiben.feature.recording.RecordingSheet
import com.shijiben.ui.theme.PixelBackground
import com.shijiben.ui.theme.PixelBorder
import com.shijiben.ui.theme.PixelButton
import com.shijiben.ui.theme.PixelCard
import com.shijiben.ui.theme.PixelGold
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary
import com.shijiben.ui.theme.pixelBorder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
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
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val date by viewModel.viewingDate.collectAsStateWithLifecycle()
    val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)

    var showSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }

    var showNoteSheet by remember { mutableStateOf(false) }
    val editingNote by notesViewModel.editing.collectAsStateWithLifecycle()

    // 计算当天的起止时间戳
    val (y, m, d) = date
    val dayCal = remember(date) {
        Calendar.getInstance(TimeZone.getDefault()).apply {
            set(y, m - 1, d, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val dayStart = dayCal.timeInMillis
    val dayEnd = dayStart + 24L * 3600 * 1000

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // 顶部日期栏
            PixelCard(
                modifier = Modifier.fillMaxWidth(),
                shadow = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTagsClick) {
                            Icon(Icons.Default.Star, contentDescription = "标签")
                        }
                        IconButton(onClick = { viewModel.goToPreviousDay() }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前一天")
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDate(date),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PixelText
                        )
                        Text(
                            text = formatWeekday(date),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = PixelTextSecondary
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNotesClick) {
                            Icon(Icons.Default.Edit, contentDescription = "随笔")
                        }
                        IconButton(onClick = { viewModel.goToNextDay() }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "后一天")
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // 主体：左时间条 + 中事件列表 + 右随笔轨道
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayProgressBar(
                    events = events,
                    viewingDate = date,
                    nowHour = nowHour,
                    modifier = Modifier.padding(top = 4.dp)
                )
                EventList(
                    events = events,
                    onEventClick = { event ->
                        editingEvent = event
                        showSheet = true
                    },
                    modifier = Modifier.weight(1f)
                )
                NotesRail(
                    notes = notes,
                    dayStart = dayStart,
                    dayEnd = dayEnd,
                    onNoteClick = { note ->
                        notesViewModel.startEdit(note)
                        showNoteSheet = true
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            // 底部记一笔按钮
            PixelButton(
                text = "记一笔",
                onClick = {
                    editingEvent = null
                    showSheet = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
        }

        if (showSheet) {
            RecordingSheet(
                viewingDate = date,
                editingEvent = editingEvent,
                onDismiss = { showSheet = false },
                onSaved = { showSheet = false }
            )
        }

        if (showNoteSheet) {
            NoteEditorSheet(
                editing = editingNote,
                onDismiss = {
                    showNoteSheet = false
                    notesViewModel.closeSheet()
                },
                onSave = { content -> notesViewModel.save(content) },
                onDelete = { note -> notesViewModel.delete(note) }
            )
        }
    }
}

@Composable
private fun NotesRail(
    notes: List<NoteEntity>,
    dayStart: Long,
    dayEnd: Long,
    onNoteClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val railWidth = 20.dp
    val dotSize = 10.dp
    val dotSizePx = with(density) { dotSize.roundToPx() }
    val xOffsetPx = (with(density) { railWidth.roundToPx() } - dotSizePx) / 2

    BoxWithConstraints(
        modifier = modifier
            .width(railWidth)
            .fillMaxHeight()
            .background(PixelBackground)
            .pixelBorder()
    ) {
        val heightPx = constraints.maxHeight
        notes.forEach { note ->
            val fraction = ((note.timestamp - dayStart).toFloat() / (dayEnd - dayStart)).coerceIn(0f, 1f)
            val yPx = (fraction * heightPx).toInt() - dotSizePx / 2
            Box(
                modifier = Modifier
                    .offset { IntOffset(xOffsetPx, yPx) }
                    .size(dotSize)
                    .background(PixelGold)
                    .border(1.dp, PixelBorder)
                    .clickable { onNoteClick(note) }
            )
        }
    }
}

private fun formatDate(date: Triple<Int, Int, Int>): String {
    val (y, m, d) = date
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.set(y, m - 1, d, 0, 0, 0)
    return SimpleDateFormat("MM月dd日", Locale.getDefault()).format(cal.time)
}

private fun formatWeekday(date: Triple<Int, Int, Int>): String {
    val (y, m, d) = date
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.set(y, m - 1, d, 0, 0, 0)
    val w = cal.get(Calendar.DAY_OF_WEEK)
    val names = arrayOf("周日","周一","周二","周三","周四","周五","周六")
    return names[w - 1]
}
