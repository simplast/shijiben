package com.doer.shijiben.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.TimeFormats
import com.doer.shijiben.ui.EventViewModel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material.icons.filled.FileDownload
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EventViewModel,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
    onOpenReview: () -> Unit,
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dateLabel by viewModel.selectedDateLabel.collectAsState()
    val events by viewModel.eventsForSelectedDay.collectAsState()
    val activeEvent by viewModel.activeEvent.collectAsState()
    val elapsedMinutes by viewModel.activeEventElapsedMinutes.collectAsState()
    val weeklyStats by viewModel.weeklyStats.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var quickInputName by remember { mutableStateOf("") }

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

    var datePickerVisible by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<EventEntity?>(null) }

    if (eventToDelete != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("确认删除") },
            text = { Text("确定要删除「${eventToDelete?.name}」这段时光吗？") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        val event = eventToDelete
                        if (event != null) {
                            viewModel.delete(event) { err ->
                                if (err != null) {
                                    scope.launch { snackbarHostState.showSnackbar("删除失败") }
                                }
                            }
                        }
                        eventToDelete = null
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { eventToDelete = null }) { Text("取消") }
            }
        )
    }
    
    // Bottom Sheet state
    var editorSheetVisible by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

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
            dragHandle = { Box(Modifier.padding(top = 12.dp).width(32.dp).height(4.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape)) }
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
        topBar = {
            TopAppBar(
                title = { Text("事记本", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) },
                actions = {
                    IconButton(onClick = { viewModel.smartMergeEvents() }) {
                        Icon(Icons.Default.CleaningServices, contentDescription = "整理记录", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onOpenReview) {
                        Icon(Icons.Default.QueryStats, contentDescription = "数据统计")
                    }
                    IconButton(onClick = {
                        val timestamp = System.currentTimeMillis()
                        csvExportLauncher.launch("shijiben_export_$timestamp.csv")
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导出 CSV")
                    }
                    IconButton(onClick = {
                        val timestamp = System.currentTimeMillis()
                        jsonExportLauncher.launch("shijiben_export_$timestamp.json")
                    }) {
                        Icon(Icons.Default.History, contentDescription = "备份 JSON")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEventId = null
                    editorSheetVisible = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建事件")
            }
        },
        bottomBar = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                ),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                DaySelectorRow(
                    selectedDate = selectedDate,
                    dateLabel = dateLabel,
                    onPrevDay = { viewModel.setSelectedDate(selectedDate.minusDays(1)) },
                    onNextDay = { viewModel.setSelectedDate(selectedDate.plusDays(1)) },
                    onPickDate = { datePickerVisible = true },
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            StatsDashboard(
                events = events,
                topGoal = weeklyStats?.goalProgress?.filter { it.targetMinutes > 0 }?.maxByOrNull { it.currentMinutes.toFloat() / it.targetMinutes.toFloat() }
            )
            
            Spacer(Modifier.height(8.dp))

            // Quick Start Input
            androidx.compose.material3.OutlinedTextField(
                value = quickInputName,
                onValueChange = { quickInputName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("今天想记录点什么？", style = MaterialTheme.typography.bodyMedium) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                trailingIcon = {
                    if (quickInputName.isNotBlank()) {
                        IconButton(onClick = {
                            viewModel.quickStartEvent(quickInputName)
                            quickInputName = ""
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "开始", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    imeAction = androidx.compose.ui.text.input.ImeAction.Go
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onGo = {
                        if (quickInputName.isNotBlank()) {
                            viewModel.quickStartEvent(quickInputName)
                            quickInputName = ""
                        }
                    }
                )
            )

            Spacer(Modifier.height(16.dp))

            activeEvent?.let { active ->
                ActiveEventCard(
                    event = active,
                    elapsedMinutes = elapsedMinutes,
                    onStop = { viewModel.stopActiveEvent() },
                    onClick = {
                        editingEventId = active.id
                        editorSheetVisible = true
                    }
                )
                Spacer(Modifier.height(16.dp))
            }
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            when (value) {
                                SwipeToDismissBoxValue.StartToEnd -> {
                                    viewModel.quickStartEvent(event.name)
                                    false
                                }
                                SwipeToDismissBoxValue.EndToStart -> {
                                    eventToDelete = event
                                    false
                                }
                                else -> false
                            }
                        }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primaryContainer
                                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.surface
                            }
                            val icon = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Icons.Default.PlayArrow
                                SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                else -> null
                            }
                            val alignment = when (dismissState.dismissDirection) {
                                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                else -> Alignment.Center
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(color, MaterialTheme.shapes.medium)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = alignment
                            ) {
                                icon?.let { Icon(it, contentDescription = null) }
                            }
                        },
                        enableDismissFromStartToEnd = true,
                        enableDismissFromEndToStart = true
                    ) {
                        EventSummaryCard(
                            event = event, 
                            onClick = { 
                                editingEventId = event.id
                                editorSheetVisible = true
                            }
                        )
                    }
                }
                item {
                    if (events.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 64.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                text = "静候佳音，虚位以待",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveEventCard(
    event: EventEntity,
    elapsedMinutes: Long,
    onStop: () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "正在进行",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "已持续 ${elapsedMinutes} 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("结束")
            }
        }
    }
}

@Composable
private fun StatsDashboard(events: List<EventEntity>, topGoal: com.doer.shijiben.ui.GoalProgress?) {
    val totalMinutes = events.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L }
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.QueryStats, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("今日统计", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " / 总时长",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${events.size}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Text(
                        text = "项事件",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            if (topGoal != null) {
                Spacer(Modifier.height(16.dp))
                androidx.compose.material3.HorizontalDivider(
                    thickness = 0.5.dp, 
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                )
                Spacer(Modifier.height(12.dp))
                
                val currentH = topGoal.currentMinutes / 60
                val targetH = topGoal.targetMinutes / 60
                val progress = (topGoal.currentMinutes.toFloat() / topGoal.targetMinutes.toFloat()).coerceAtMost(1f)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "目标: ${topGoal.name}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${currentH}h / ${targetH}h",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(4.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun DaySelectorRow(
    selectedDate: LocalDate,
    dateLabel: String,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
) {
    val today = LocalDate.now()
    val relativeLabel = when {
        selectedDate == today -> "今天"
        selectedDate == today.minusDays(1) -> "昨天"
        selectedDate == today.plusDays(1) -> "明天"
        selectedDate < today.minusDays(1) -> "过去"
        selectedDate > today.plusDays(1) -> "未来"
        else -> "选择日期"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevDay) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "前一天")
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPickDate),
        ) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = relativeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
            )
        }
        IconButton(onClick = onNextDay) {
            Icon(Icons.Default.ChevronRight, contentDescription = "后一天")
        }
    }
}

@Composable
private fun EventSummaryCard(
    event: EventEntity,
    onClick: () -> Unit,
) {
    val isInProgress = event.status == "IN_PROGRESS"
    val accentColor = MaterialTheme.colorScheme.secondary
    val startTime = TimeFormats.formatTimeMillis(event.startTimeMillis)
    val timeRange = if (isInProgress) {
        "$startTime — 进行中"
    } else {
        "$startTime — ${TimeFormats.formatTimeMillis(event.endTimeMillis)}"
    }
    val durationLabel = if (isInProgress) {
        "进行中"
    } else {
        val minutes = ((event.endTimeMillis - event.startTimeMillis) / 60_000L).toInt()
        "${minutes.coerceAtLeast(0)}分"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isInProgress) accentColor.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = event.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = timeRange,
                style = MaterialTheme.typography.bodySmall,
                color = if (isInProgress) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
            Text(
                text = durationLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (isInProgress) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.padding(start = 8.dp),
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp),
            )
        }
    }
}
