package com.shijiben.feature.timeline

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun TimelineScreen(
    onNotesClick: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val date by viewModel.viewingDate.collectAsStateWithLifecycle()
    val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)
    val now by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(60_000L)
            value = System.currentTimeMillis()
        }
    }

    var showSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showNoteSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<EventEntity?>(null) }
    var activeDrawer by remember { mutableStateOf<DrawerType?>(null) }
    var eventDraft by remember { mutableStateOf("") }
    var noteDraft by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
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
                // 彩虹像素装饰条（加厚至 8dp，8 色循环铺满整宽）
                Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
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
                // 居中日期徽章：2dp 黑边白底 + 3dp 硬阴影（8-bit 风）
                Box(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val onToday = isToday(date)
                    Box(modifier = Modifier.clickable { showDatePicker = true }) {
                        // 硬阴影：与徽章同尺寸、向右下偏移 3dp 的纯黑底盒（无圆角）
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .offset(x = 3.dp, y = 3.dp)
                                .background(Color.Black)
                        )
                        // 徽章本体：白底 2dp 黑边
                        Box(
                            modifier = Modifier
                                .border(2.dp, Color.Black)
                                .background(Surface)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatDateCompact(date),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (onToday) TextPrimary else Accent
                            )
                        }
                    }
                }
            }
            
            Row(modifier = Modifier.weight(1f).fillMaxWidth().fillMaxHeight()) {
                Box(modifier = Modifier.width(40.dp).fillMaxHeight().padding(top = 12.dp, start = 12.dp)) {
                    DayProgressBar(
                        events = events,
                        viewingDate = date,
                        now = now
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
                        onStopEvent = { event -> viewModel.markCompleted(event.id) },
                        onEventLongClick = { event -> pendingDelete = event },
                        now = now
                    )
                }
            }
        }

        // Part B：底部双 block 统一入口（常态）或抽屉（展开态）
        if (activeDrawer == null) {
            Box(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                BottomEntryBar(
                    onCalendarClick = { showDatePicker = true },
                    onEventTriggerClick = { activeDrawer = DrawerType.EVENT },
                    onNoteTriggerClick = { activeDrawer = DrawerType.NOTE },
                    onNotesClick = onNotesClick
                )
            }
        } else {
            EntryDrawer(
                drawerType = activeDrawer!!,
                eventDraft = eventDraft,
                noteDraft = noteDraft,
                onEventDraftChange = { eventDraft = it },
                onNoteDraftChange = { noteDraft = it },
                onSubmit = { text ->
                    when (activeDrawer) {
                        DrawerType.EVENT -> {
                            if (text.isNotBlank()) {
                                viewModel.quickAddEvent(text.trim())
                                eventDraft = ""
                            }
                        }
                        DrawerType.NOTE -> {
                            if (text.isNotBlank()) {
                                scope.launch {
                                    if (notesViewModel.save(text.trim())) {
                                        noteDraft = ""
                                        viewModel.refresh()
                                    }
                                }
                            }
                        }
                        null -> {}
                    }
                    activeDrawer = null
                },
                onDismiss = { activeDrawer = null }
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

        if (showDatePicker) {
            DateSelectorDialog(
                initialDate = date,
                onConfirm = { (y, m, d) ->
                    viewModel.setDate(y, m, d)
                    showDatePicker = false
                },
                onDismiss = { showDatePicker = false }
            )
        }

        pendingDelete?.let { target ->
            AlertDialog(
                onDismissRequest = { pendingDelete = null },
                title = { Text("删除这条记录？", color = TextPrimary) },
                text = { Text("「${target.title}」将被永久删除，无法恢复。", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.deleteEvent(target.id)
                        pendingDelete = null
                    }) { Text("删除", color = Error) }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDelete = null }) { Text("取消", color = TextSecondary) }
                }
            )
        }
    }
}

@Composable
fun EventList(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    onStartEvent: (EventEntity) -> Unit,
    onStopEvent: (EventEntity) -> Unit,
    onEventLongClick: (EventEntity) -> Unit,
    now: Long
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
                    onStop = { onStopEvent(event) },
                    onLongClick = { onEventLongClick(event) },
                    now = now
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EventCard(
    event: EventEntity,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onLongClick: () -> Unit,
    now: Long
) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
            when (event.status) {
                1 -> {
                    // 进行中：显示开始时间 + 运行中时长 badge
                    Text(
                        text = "自 ${formatTime(event.startTime)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.width(6.dp))
                    val elapsed = formatDurationShort(event.startTime, now)
                    Box(
                        modifier = Modifier
                            .background(Primary, RoundedCornerShape(0.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = elapsed,
                            fontSize = 10.sp,
                            color = TextOnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                2 -> {
                    // 已完成：原时间范围 + 耗时 badge（保持不变）
                    Text(
                        text = "${formatTime(event.startTime)}-${formatTime(event.endTime!!)}",
                        fontSize = 12.sp,
                        color = Secondary
                    )
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .background(Accent, RoundedCornerShape(0.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDurationShort(event.startTime, event.endTime!!),
                            fontSize = 10.sp,
                            color = TextOnPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // status 0 (not_started): no time block (unchanged — endTime is null, no duration)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectorDialog(
    initialDate: Triple<Int, Int, Int>,
    onConfirm: (Triple<Int, Int, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    // Material3 DatePicker 以 UTC 0 点毫秒表示选中日期
    val state = rememberDatePickerState(
        initialSelectedDateMillis = dateToUtcMillis(initialDate)
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = millis
                    onConfirm(
                        Triple(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    )
                }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = state)
    }
}

private fun dateToUtcMillis(date: Triple<Int, Int, Int>): Long {
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    cal.set(date.first, date.second - 1, date.third, 0, 0, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

// ===== Part B：底部双 block 入口 + 抽屉 =====

private enum class DrawerType { EVENT, NOTE }

/** 8 色循环彩虹条（与顶部彩虹条同色），height 通常 3.dp。 */
@Composable
private fun RainbowTrim(height: Dp) {
    val trimColors = listOf(
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
        Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
        Color(0xFF6366F1), Color(0xFFA855F7)
    )
    Row(modifier = Modifier.fillMaxWidth().height(height)) {
        for (i in 0 until 80) {
            Box(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(trimColors[i % 8])
            )
        }
    }
}

/** B1：底部双 block 栏（常态入口，不是打字处）。 */
@Composable
private fun BottomEntryBar(
    onCalendarClick: () -> Unit,
    onEventTriggerClick: () -> Unit,
    onNoteTriggerClick: () -> Unit,
    onNotesClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Surface)) {
        RainbowTrim(3.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 左 block：📅 + 记事输入框触发器
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(2.dp, Color.Black)
                        .background(Primary)
                        .clickable(onClick = onCalendarClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "选择日期",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .weight(1f)
                        .border(2.dp, Color.Black)
                        .background(Surface)
                        .clickable(onClick = onEventTriggerClick),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "在做什么？",
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            // 中间 2dp 黑色竖分隔线
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.Black)
            )
            // 右 block：随笔输入框触发器 + ✎
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .weight(1f)
                        .border(2.dp, Color.Black)
                        .background(Surface)
                        .clickable(onClick = onNoteTriggerClick),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "写点什么...",
                        color = TextTertiary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(2.dp, Color.Black)
                        .background(Accent)
                        .clickable(onClick = onNotesClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "随笔列表",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/** B2：抽屉 overlay（遮罩 + 底部抽屉本体）。无提交按钮、无 ✕，靠 IME Done 提交、点遮罩关闭。 */
@Composable
private fun EntryDrawer(
    drawerType: DrawerType,
    eventDraft: String,
    noteDraft: String,
    onEventDraftChange: (String) -> Unit,
    onNoteDraftChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(drawerType) {
        focusRequester.requestFocus()
    }
    val isEvent = drawerType == DrawerType.EVENT
    val currentDraft = if (isEvent) eventDraft else noteDraft
    val onDraftChange = if (isEvent) onEventDraftChange else onNoteDraftChange
    val placeholder = if (isEvent) "写一件事..." else "写点什么..."

    Box(modifier = Modifier.fillMaxSize()) {
        // 遮罩：点击关闭，不清草稿
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        )
        // 抽屉本体
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Surface)
        ) {
            // 顶部 2dp 黑色顶边 + 3dp 彩虹条
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
            RainbowTrim(3.dp)
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                // 标签徽章
                Box(
                    modifier = Modifier
                        .border(2.dp, Color.Black)
                        .background(if (isEvent) Primary else Accent)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isEvent) "记事" else "随笔",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                // 多行输入框：2dp 黑边、白底、min-height 96dp、IME Done 提交
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp)
                        .border(2.dp, Color.Black)
                        .background(Surface)
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = currentDraft,
                        onValueChange = onDraftChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(Color.Black),
                        singleLine = false,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { onSubmit(currentDraft) }
                        ),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (currentDraft.isEmpty()) {
                                    Text(
                                        text = placeholder,
                                        color = TextTertiary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
            }
        }
    }
}
