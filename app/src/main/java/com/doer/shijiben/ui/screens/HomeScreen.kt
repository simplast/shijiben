@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.doer.shijiben.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.TimeFormats
import com.doer.shijiben.ui.DatePerspective
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.theme.ActiveGradientStart
import com.doer.shijiben.ui.theme.ActiveGradientMiddle
import com.doer.shijiben.ui.theme.ActiveGradientEnd
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: EventViewModel,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenReview: () -> Unit,
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dateLabel by viewModel.selectedDateLabel.collectAsState()
    val datePerspective by viewModel.datePerspective.collectAsState()
    val activeEvent by viewModel.activeEvent.collectAsState()
    val elapsedMinutes by viewModel.activeEventElapsedMinutes.collectAsState()
    val recommendedNames by viewModel.recommendedEventNames.collectAsState()
    val completedEvents by viewModel.completedEventsForSelectedDay.collectAsState()
    val pendingEvents by viewModel.pendingEventsForSelectedDay.collectAsState()
    val filteredPendingEvents = pendingEvents.filter { it.status != "IN_PROGRESS" }

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var quickInputName by remember { mutableStateOf("") }
    var datePickerVisible by remember { mutableStateOf(false) }
    var editorSheetVisible by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val jsonExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportDataAsJson(context, it) { err ->
                scope.launch {
                    if (err != null) snackbarHostState.showSnackbar("导出失败: ${err.message}")
                    else snackbarHostState.showSnackbar("导出成功")
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportDataAsCsv(context, it) { err ->
                scope.launch {
                    if (err != null) snackbarHostState.showSnackbar("导出失败: ${err.message}")
                    else snackbarHostState.showSnackbar("导出成功")
                }
            }
        }
    }

    if (datePickerVisible) {
        DayPickerDialog(
            initialDate = selectedDate,
            onDismiss = { datePickerVisible = false },
            onConfirm = {
                viewModel.setSelectedDate(it)
                datePickerVisible = false
            },
        )
    }

    if (editorSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = {
                editorSheetVisible = false
                editingEventId = null
            },
            sheetState = sheetState,
            dragHandle = { LineDragHandle() }
        ) {
            EventEditorContent(
                eventId = editingEventId,
                viewModel = viewModel,
                onComplete = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        editorSheetVisible = false
                        editingEventId = null
                    }
                },
                onDelete = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        editorSheetVisible = false
                        editingEventId = null
                    }
                }
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                LineTopBar(
                    dateLabel = dateLabel,
                    onPickDate = { datePickerVisible = true },
                    onOpenReview = onOpenReview,
                    onExportCsv = {
                        csvExportLauncher.launch("shijiben_export_${System.currentTimeMillis()}.csv")
                    },
                    onExportJson = {
                        jsonExportLauncher.launch("shijiben_export_${System.currentTimeMillis()}.json")
                    },
                )

                Hairline()

                Spacer(Modifier.height(8.dp))

                activeEvent?.let { event ->
                    ActiveEventCard(
                        event = event,
                        elapsedMinutes = elapsedMinutes,
                        onStop = { viewModel.stopActiveEvent() },
                        onClick = {
                            editingEventId = event.id
                            editorSheetVisible = true
                        }
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        CompletedSection(
                            events = completedEvents,
                            datePerspective = datePerspective,
                            onRestart = { viewModel.restartEvent(it) },
                            onDelete = { viewModel.deleteEvent(it) },
                            onOpen = {
                                editingEventId = it.id
                                editorSheetVisible = true
                            }
                        )
                    }

                    item {
                        PendingSection(
                            events = filteredPendingEvents,
                            datePerspective = datePerspective,
                            recommendedNames = recommendedNames,
                            activeElapsedMinutes = elapsedMinutes,
                            onStart = { viewModel.startEvent(it) },
                            onStop = { viewModel.stopActiveEvent() },
                            onAdd = { viewModel.quickAddEvent(it) },
                            onDelete = { viewModel.deleteEvent(it) },
                            onOpen = {
                                editingEventId = it.id
                                editorSheetVisible = true
                            }
                        )
                    }
                }
            }

            if (datePerspective != DatePerspective.PAST) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 14.dp, vertical = 16.dp)
                ) {
                    QuickNameLine(
                        value = quickInputName,
                        onValueChange = { quickInputName = it },
                        onAdd = {
                            val name = quickInputName.trim()
                            if (name.isNotEmpty()) {
                                viewModel.quickAddEvent(name)
                                quickInputName = ""
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletedSection(
    events: List<EventEntity>,
    datePerspective: DatePerspective,
    onRestart: (EventEntity) -> Unit,
    onDelete: (EventEntity) -> Unit,
    onOpen: (EventEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(12.dp))
        Text(
            text = if (datePerspective == DatePerspective.TODAY) "今天已完成" else "已完成",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(0.72f),
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.5f),
                        )
                    }
                } else {
                    events.forEachIndexed { index, event ->
                        EventRowWithDelete(
                            event = event,
                            datePerspective = datePerspective,
                            activeElapsedMinutes = 0L,
                            onRestart = { onRestart(event) },
                            onDelete = { onDelete(event) },
                            onClick = { onOpen(event) },
                        )
                        if (index < events.lastIndex) {
                            Hairline(alpha = 0.3f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingSection(
    events: List<EventEntity>,
    datePerspective: DatePerspective,
    recommendedNames: List<String>,
    activeElapsedMinutes: Long,
    onStart: (EventEntity) -> Unit,
    onStop: () -> Unit,
    onAdd: (String) -> Unit,
    onDelete: (EventEntity) -> Unit,
    onOpen: (EventEntity) -> Unit
) {
    val existingNames = events.map { it.name.trim() }.toSet()

    Column(modifier = Modifier.fillMaxWidth()) {
        val validRecommendations = recommendedNames
            .map { it.trim() }
            .filter { !existingNames.contains(it) }

        if (validRecommendations.isNotEmpty() && datePerspective != DatePerspective.PAST) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "建议快速开始",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.72f),
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(validRecommendations) { name ->
                    val interactionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onAdd(name) }
                            )
                            .pressScaleEffect(interactionSource)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (datePerspective == DatePerspective.PAST && events.isEmpty()) {
            EmptyLine(datePerspective)
            return
        }

        Spacer(Modifier.height(12.dp))
        Text(
            text = if (datePerspective == DatePerspective.TODAY) "今日待办" else "待办",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(0.72f),
        )
        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ),
            border = BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = when (datePerspective) {
                                DatePerspective.PAST -> "那天似乎什么也没发生"
                                DatePerspective.FUTURE -> "这一天还很空，不如规划点什么？"
                                else -> "写下一件事，先不用开始"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.alpha(0.5f),
                        )
                    }
                } else {
                    events.forEachIndexed { index, event ->
                        EventRowWithDelete(
                            event = event,
                            datePerspective = datePerspective,
                            activeElapsedMinutes = activeElapsedMinutes,
                            onStart = { onStart(event) },
                            onStop = onStop,
                            onDelete = { onDelete(event) },
                            onClick = { onOpen(event) },
                        )
                        if (index < events.lastIndex) {
                            Hairline(alpha = 0.3f)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRowWithDelete(
    event: EventEntity,
    datePerspective: DatePerspective,
    activeElapsedMinutes: Long,
    onStart: (() -> Unit)? = null,
    onStop: (() -> Unit)? = null,
    onRestart: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val isPending = event.status == "PENDING"
    val isActive = event.status == "IN_PROGRESS"
    val isCompleted = event.status == "COMPLETED"
    val accent = when {
        isActive -> MaterialTheme.colorScheme.primary
        isPending -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val meta = when {
        isPending -> "待开始"
        isActive -> "${activeElapsedMinutes}m"
        isCompleted -> {
            val minutes = ((event.endTimeMillis - event.startTimeMillis) / 60_000L).coerceAtLeast(0L)
            "${TimeFormats.formatTimeMillis(event.startTimeMillis)}—${TimeFormats.formatTimeMillis(event.endTimeMillis)} · ${minutes}m"
        }
        else -> event.status
    }

    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressScaleEffect(interactionSource),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(accent, RoundedCornerShape(1.5.dp))
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = event.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = meta,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            maxLines = 1,
        )
        Spacer(Modifier.width(4.dp))
        if (isPending && datePerspective == DatePerspective.TODAY) {
            TextButton(onClick = onStart!!, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("开始", style = MaterialTheme.typography.labelSmall)
            }
        } else if (isActive) {
            OutlinedButton(
                onClick = onStop!!,
                contentPadding = PaddingValues(horizontal = 6.dp),
                modifier = Modifier.height(28.dp),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("结束", style = MaterialTheme.typography.labelSmall)
            }
        } else if (isCompleted && datePerspective != DatePerspective.PAST) {
            TextButton(onClick = onRestart!!, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("再来一次", style = MaterialTheme.typography.labelSmall)
            }
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "删除",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun LineTopBar(
    dateLabel: String,
    onPickDate: () -> Unit,
    onOpenReview: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPickDate) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "选择日期", modifier = Modifier.size(21.dp))
        }
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPickDate),
        )
        IconButton(onClick = onOpenReview) {
            Icon(Icons.Default.QueryStats, contentDescription = "数据统计", modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onExportCsv) {
            Icon(Icons.Default.FileDownload, contentDescription = "导出 CSV", modifier = Modifier.size(18.dp))
        }
        IconButton(onClick = onExportJson) {
            Icon(Icons.Default.History, contentDescription = "备份 JSON", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun QuickNameLine(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(start = 18.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        "今天想做点什么...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.alpha(0.55f),
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onAdd() }),
                )
            }
            IconButton(onClick = onAdd) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "添加",
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun EmptyLine(datePerspective: DatePerspective) {
    val text = when (datePerspective) {
        DatePerspective.PAST -> "那天似乎什么也没发生"
        DatePerspective.FUTURE -> "这一天还很空，不如规划点什么？"
        else -> "写下一件事，先不用开始"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun Hairline(alpha: Float = 0.7f) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha))
    )
}

@Composable
private fun LineDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .width(34.dp)
            .height(2.dp)
            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
    )
}

@Composable
private fun ActiveEventCard(
    event: EventEntity,
    elapsedMinutes: Long,
    onStop: () -> Unit,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pressScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(ActiveGradientStart, ActiveGradientMiddle, ActiveGradientEnd)
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .graphicsLayer {
                                    scaleX = scale
                                    scaleY = scale
                                    this.alpha = alpha
                                }
                                .background(Color.White, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "正在记录时间...",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${elapsedMinutes}m",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = ActiveGradientStart
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            Icons.Default.Stop,
                            contentDescription = "结束",
                            modifier = Modifier.size(16.dp),
                            tint = ActiveGradientStart
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("结束", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.pressScaleEffect(interactionSource: MutableInteractionSource): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

