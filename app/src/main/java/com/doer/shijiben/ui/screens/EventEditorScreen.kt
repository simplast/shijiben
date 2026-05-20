package com.doer.shijiben.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.TimeFormats
import com.doer.shijiben.ui.EventViewModel
import kotlinx.coroutines.launch

@Composable
fun EventEditorContent(
    eventId: Long?,
    viewModel: EventViewModel,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val suggestionNames by viewModel.recentDistinctEventNames.collectAsState()
    val focusRequester = remember { FocusRequester() }

    var name by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var durationMin by remember { mutableIntStateOf(0) } // Default to 0
    var endMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var status by remember { mutableStateOf("IN_PROGRESS") }
    
    var loadedEntity by remember { mutableStateOf<EventEntity?>(null) }

    var startDatePickerOpen by remember { mutableStateOf(false) }
    var startTimePickerOpen by remember { mutableStateOf(false) }
    var endTimePickerOpen by remember { mutableStateOf(false) }

    fun updateEndFromDuration() {
        if (durationMin > 0) {
            endMillis = startMillis + durationMin * 60_000L
            status = "COMPLETED"
        } else {
            endMillis = startMillis
            status = "IN_PROGRESS"
        }
    }

    fun updateDurationFromEnd() {
        if (endMillis > startMillis) {
            durationMin = ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(1)
            status = "COMPLETED"
        } else {
            durationMin = 0
            status = "IN_PROGRESS"
        }
    }

    LaunchedEffect(eventId) {
        if (eventId != null) {
            val entity = viewModel.getEvent(eventId) ?: return@LaunchedEffect
            loadedEntity = entity
            name = entity.name
            startMillis = entity.startTimeMillis
            endMillis = entity.endTimeMillis
            status = entity.status
            updateDurationFromEnd()
        } else {
            updateEndFromDuration()
            delay(260)
            focusRequester.requestFocus()
        }
    }

    if (startDatePickerOpen) {
        DayPickerDialog(
            initialDate = TimeFormats.millisToLocalDate(startMillis),
            onDismiss = { startDatePickerOpen = false },
            onConfirm = {
                startMillis = TimeFormats.mergeLocalDateKeepingLocalTime(startMillis, it)
                updateEndFromDuration()
                startDatePickerOpen = false
            },
        )
    }

    if (startTimePickerOpen) {
        EventTimePickerDialog(
            millis = startMillis,
            onDismiss = { startTimePickerOpen = false },
            onConfirm = { hour, minute ->
                startMillis = TimeFormats.mergeLocalTimeKeepingLocalDate(startMillis, hour, minute)
                updateEndFromDuration()
                startTimePickerOpen = false
            },
        )
    }

    if (endTimePickerOpen) {
        EventTimePickerDialog(
            millis = endMillis,
            onDismiss = { endTimePickerOpen = false },
            onConfirm = { hour, minute ->
                endMillis = TimeFormats.mergeLocalTimeKeepingLocalDate(endMillis, hour, minute)
                updateDurationFromEnd()
                endTimePickerOpen = false
            },
        )
    }

    fun showMessage(message: String) {
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    fun saveEvent(customStatus: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            showMessage("请为这段时光命名")
            return
        }
        val entity = EventEntity(
            id = loadedEntity?.id ?: 0L,
            name = trimmed,
            startTimeMillis = startMillis,
            endTimeMillis = if (customStatus == "COMPLETED") System.currentTimeMillis() else endMillis,
            dayKey = "",
            status = customStatus ?: status
        )
        viewModel.upsert(entity) { err ->
            if (err != null) showMessage(err.message ?: "保存失败")
            else onComplete()
        }
    }

    val isNew = eventId == null
    val sectionGap = if (isNew) 10.dp else 20.dp
    val horizontalPad = if (isNew) 16.dp else 24.dp

    Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPad, vertical = if (isNew) 8.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(sectionGap)
        ) {
            if (!isNew) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("重温与修正", style = MaterialTheme.typography.titleMedium)
                }
            }

            OutlinedTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        "此刻正在发生什么？",
                        style = if (isNew) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                    )
                },
                singleLine = true,
                textStyle = if (isNew) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                )
            )

            if (isNew && suggestionNames.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(suggestionNames, key = { it }) { suggestion ->
                        AssistChip(
                            onClick = { name = suggestion },
                            label = {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(if (isNew) 10.dp else 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(if (isNew) 16.dp else 20.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = TimeFormats.formatDateMillis(startMillis),
                            style = if (isNew) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(
                            onClick = { startDatePickerOpen = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                        ) {
                            Text(
                                "改日期",
                                style = if (isNew) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Spacer(Modifier.height(if (isNew) 4.dp else 12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TimeSelectionBlock(
                            label = "开始",
                            time = TimeFormats.formatTimeMillis(startMillis),
                            compact = isNew,
                            onClick = { startTimePickerOpen = true },
                        )
                        Icon(
                            Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier.size(14.dp),
                        )
                        TimeSelectionBlock(
                            label = if (status == "IN_PROGRESS") "进行中" else "结束",
                            time = if (status == "IN_PROGRESS") "--:--" else TimeFormats.formatTimeMillis(endMillis),
                            compact = isNew,
                            onClick = { endTimePickerOpen = true },
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(if (isNew) 0.dp else 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "持续时长",
                        style = if (isNew) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        if (durationMin > 0) "${durationMin} 分钟" else "正在进行",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Slider(
                    value = durationMin.toFloat(),
                    onValueChange = {
                        durationMin = it.toInt()
                        updateEndFromDuration()
                    },
                    valueRange = 0f..120f,
                    steps = 24,
                )
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { saveEvent() },
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(vertical = if (isNew) 8.dp else 12.dp),
            ) {
                Text(
                    if (isNew) "添加" else "更新",
                    style = if (isNew) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleSmall,
                )
            }

            if (eventId != null && loadedEntity != null) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    onClick = {
                        viewModel.delete(loadedEntity!!) { err ->
                            if (err != null) showMessage(err.message ?: "删除失败")
                            else onDelete()
                        }
                    },
                    elevation = null
                ) {
                    Text("删除此段记忆", style = MaterialTheme.typography.bodySmall)
                }
                
                if (status == "IN_PROGRESS") {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { saveEvent("COMPLETED") }
                    ) {
                        Text("标记完成")
                    }
                }
            }
            
            Spacer(Modifier.height(if (isNew) 8.dp else 24.dp))
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun TimeSelectionBlock(
    label: String,
    time: String,
    compact: Boolean = false,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(horizontal = if (compact) 4.dp else 8.dp, vertical = if (compact) 2.dp else 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            time,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTimePickerDialog(
    millis: Long,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val (hour, minute) = TimeFormats.millisToHourMinute(millis)
    val state = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        text = { TimePicker(state = state) },
    )
}
