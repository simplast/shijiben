package com.doer.shijiben.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material.icons.filled.FileDownload
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

import androidx.compose.material3.SwipeToDismissBox
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
            CompactTopBar(
                title = "事记本",
                onSmartMerge = { viewModel.smartMergeEvents() },
                onOpenReview = onOpenReview,
                onExportCsv = {
                    val timestamp = System.currentTimeMillis()
                    csvExportLauncher.launch("shijiben_export_$timestamp.csv")
                },
                onExportJson = {
                    val timestamp = System.currentTimeMillis()
                    jsonExportLauncher.launch("shijiben_export_$timestamp.json")
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            CompactFAB(
                onClick = {
                    editingEventId = null
                    editorSheetVisible = true
                }
            )
        },
        bottomBar = {
            CompactDayBar(
                selectedDate = selectedDate,
                dateLabel = dateLabel,
                onPrevDay = { viewModel.setSelectedDate(selectedDate.minusDays(1)) },
                onNextDay = { viewModel.setSelectedDate(selectedDate.plusDays(1)) },
                onPickDate = { datePickerVisible = true },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal=12.dp),
        ) {
            CompactQuickInput(
                value = quickInputName,
                onValueChange = { quickInputName = it },
                onSend = {
                    if (quickInputName.isNotBlank()) {
                        viewModel.quickStartEvent(quickInputName)
                        quickInputName = ""
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 0.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Active event inline
                if (activeEvent != null) {
                    val currentActiveEvent = activeEvent!!
                    item {
                        CompactActiveChip(
                            event = currentActiveEvent,
                            elapsedMinutes = elapsedMinutes,
                            onStop = { viewModel.stopActiveEvent() },
                            onClick = {
                                editingEventId = currentActiveEvent.id
                                editorSheetVisible = true
                            }
                        )
                    }
                }

                items(events, key = { it.id }) { event ->
                    CompactEventSummaryCard(
                        event = event,
                        onClick = {
                            editingEventId = event.id
                            editorSheetVisible = true
                        }
                    )
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

// ===== Compact Components =====

@Composable
private fun CompactTopBar(
    title: String,
    onSmartMerge: () -> Unit,
    onOpenReview: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Text(
                    "事记本",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.alpha(0.5f)
                )
            }
        },
        actions = {
            IconButton(onClick = onSmartMerge) {
                Icon(Icons.Default.CleaningServices, contentDescription = "整理记录", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onOpenReview) {
                Icon(Icons.Default.QueryStats, contentDescription = "数据统计")
            }
            IconButton(onClick = onExportCsv) {
                Icon(Icons.Default.FileDownload, contentDescription = "导出 CSV")
            }
            IconButton(onClick = onExportJson) {
                Icon(Icons.Default.History, contentDescription = "备份 JSON")
            }
        }
    )
}

@Composable
private fun CompactFAB(onClick: () -> Unit) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    ) {
        Icon(Icons.Default.Add, contentDescription = "新建事件")
    }
}

@Composable
private fun CompactDayBar(
    selectedDate: LocalDate,
    dateLabel: String,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPickDate: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = onPrevDay) {
                Icon(Icons.Default.ChevronLeft, contentDescription = "前一天", modifier = Modifier.size(22.dp))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = dateLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                val today = LocalDate.now()
                val relativeLabel = when {
                    selectedDate == today -> "今天"
                    selectedDate == today.minusDays(1) -> "昨天"
                    selectedDate == today.plusDays(1) -> "明天"
                    selectedDate < today.minusDays(1) -> "过去"
                    selectedDate > today.plusDays(1) -> "未来"
                    else -> "选择日期"
                }
                Text(
                    text = relativeLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onPickDate) {
                Icon(Icons.Default.Today, contentDescription = "选择日期", modifier = Modifier.size(22.dp))
            }
            IconButton(onClick = onNextDay) {
                Icon(Icons.Default.ChevronRight, contentDescription = "后一天", modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun CompactQuickInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        placeholder = {
            Text("记录今天…", style = MaterialTheme.typography.bodySmall)
        },
        singleLine = true,
        shape = MaterialTheme.shapes.small,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
        trailingIcon = {
            if (value.isNotBlank()) {
                IconButton(onClick = onSend) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "开始", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
        },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = androidx.compose.ui.text.input.ImeAction.Go
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onGo = { onSend() }
        )
    )
}

/** Compact active-event chip — replaces the big ActiveEventCard */
@Composable
private fun CompactActiveChip(
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
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Live indicator
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = event.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${elapsedMinutes}m",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onStop,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.height(26.dp),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(3.dp))
                Text("结束", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CompactEventSummaryCard(
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
        "${minutes.coerceAtLeast(0)}m"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
        tonalElevation = if (isInProgress) 1.dp else 0.dp,
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Accent line on left
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        if (isInProgress) accentColor
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                        shape = MaterialTheme.shapes.small
                    )
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = event.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = timeRange,
                style = MaterialTheme.typography.labelSmall,
                color = if (isInProgress) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = durationLabel,
                style = MaterialTheme.typography.labelMedium,
                color = if (isInProgress) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
