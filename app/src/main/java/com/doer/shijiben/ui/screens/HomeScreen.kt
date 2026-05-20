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
import androidx.compose.material3.CenterAlignedTopAppBar
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
import java.time.LocalDate

import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: EventViewModel,
    onAddEvent: () -> Unit,
    onOpenEvent: (Long) -> Unit,
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val dateLabel by viewModel.selectedDateLabel.collectAsState()
    val events by viewModel.eventsForSelectedDay.collectAsState()

    var datePickerVisible by remember { mutableStateOf(false) }
    
    // Bottom Sheet state
    var editorSheetVisible by remember { mutableStateOf(false) }
    var editingEventId by remember { mutableStateOf<Long?>(null) }
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )
    val scope = rememberCoroutineScope()

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
            CenterAlignedTopAppBar(
                title = { Text("事记本", style = MaterialTheme.typography.titleLarge) },
            )
        },
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
            StatsDashboard(events = events)
            
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(events, key = { it.id }) { event ->
                    EventSummaryCard(
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

@Composable
private fun StatsDashboard(events: List<EventEntity>) {
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
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
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
