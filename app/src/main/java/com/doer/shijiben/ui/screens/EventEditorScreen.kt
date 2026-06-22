package com.doer.shijiben.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.TimeFormats
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.theme.PixelBadge
import com.doer.shijiben.ui.theme.PixelBorder
import com.doer.shijiben.ui.theme.PixelButton
import com.doer.shijiben.ui.theme.PixelCalendarIcon
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelCheckIcon
import com.doer.shijiben.ui.theme.PixelCoral
import com.doer.shijiben.ui.theme.PixelCream
import com.doer.shijiben.ui.theme.PixelDialog
import com.doer.shijiben.ui.theme.PixelGray
import com.doer.shijiben.ui.theme.PixelGrayLight
import com.doer.shijiben.ui.theme.PixelInput
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.PixelMint
import com.doer.shijiben.ui.theme.PixelPlayIcon
import com.doer.shijiben.ui.theme.PixelSky
import com.doer.shijiben.ui.theme.PixelStarIcon
import com.doer.shijiben.ui.theme.PixelTeal
import com.doer.shijiben.ui.theme.PixelText
import com.doer.shijiben.ui.theme.PixelYellow
import kotlinx.coroutines.delay
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
    var category by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var startMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var durationMin by remember { mutableIntStateOf(0) }
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
            category = entity.category ?: ""
            note = entity.note ?: ""
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
            status = customStatus ?: status,
            category = category.trim().ifEmpty { null },
            note = note.trim().ifEmpty { null }
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
                    PixelStarIcon(color = PixelYellow, size = 16.dp)
                    Spacer(Modifier.width(6.dp))
                    Text("重温与修正", style = PixelLabel, color = PixelText)
                }
            }

            // Name input
            PixelInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "此刻正在发生什么？",
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = if (isNew) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            // Suggestion chips
            if (isNew && suggestionNames.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(suggestionNames, key = { it }) { suggestion ->
                        PixelBadge(
                            text = suggestion,
                            backgroundColor = PixelSky,
                            modifier = Modifier.clickable {
                                name = suggestion
                                saveEvent()
                            },
                        )
                    }
                }
            }

            // Category + Note
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PixelInput(
                    value = category,
                    onValueChange = { category = it },
                    placeholder = "分类 (可选)",
                    modifier = Modifier.weight(1f),
                )
                PixelInput(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = "备注 (可选)",
                    modifier = Modifier.weight(1f),
                )
            }

            // Date + time card
            PixelCard(
                level = PixelCardLevel.Secondary,
                backgroundColor = PixelMint,
                borderColor = PixelTeal,
                contentPadding = PaddingValues(if (isNew) 10.dp else 16.dp),
            ) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PixelCalendarIcon(color = PixelTeal, size = if (isNew) 16.dp else 20.dp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = TimeFormats.formatDateMillis(startMillis),
                            style = if (isNew) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge,
                            color = PixelText,
                            modifier = Modifier.weight(1f),
                        )
                        PixelButton(
                            onClick = { startDatePickerOpen = true },
                            backgroundColor = Color.Transparent,
                            borderColor = Color.Transparent,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text("改日期", style = PixelLabel, color = PixelTeal)
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
                        PixelPlayIcon(color = PixelGrayLight, size = 14.dp)
                        TimeSelectionBlock(
                            label = if (status == "IN_PROGRESS") "进行中" else "结束",
                            time = if (status == "IN_PROGRESS") "--:--" else TimeFormats.formatTimeMillis(endMillis),
                            compact = isNew,
                            onClick = { endTimePickerOpen = true },
                        )
                    }
                }
            }

            // Duration slider
            Column(verticalArrangement = Arrangement.spacedBy(if (isNew) 0.dp else 4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("持续时长", style = PixelLabel, color = PixelText)
                    Text(
                        if (durationMin > 0) "${durationMin} 分钟" else "正在进行",
                        style = MaterialTheme.typography.bodySmall,
                        color = PixelTeal,
                        fontWeight = FontWeight.Bold,
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
                    colors = SliderDefaults.colors(
                        thumbColor = PixelTeal,
                        activeTrackColor = PixelTeal,
                        inactiveTrackColor = PixelGrayLight,
                    ),
                )
            }

            // Save button
            PixelButton(
                onClick = { saveEvent() },
                backgroundColor = PixelTeal,
                borderColor = PixelBorder,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = if (isNew) 10.dp else 14.dp),
            ) {
                Text(
                    if (isNew) "开始记录" else "更新",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (eventId != null && loadedEntity != null) {
                PixelButton(
                    onClick = {
                        viewModel.delete(loadedEntity!!) { err ->
                            if (err != null) showMessage(err.message ?: "删除失败")
                            else onDelete()
                        }
                    },
                    backgroundColor = PixelCoral.copy(alpha = 0.15f),
                    borderColor = PixelCoral,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 10.dp),
                ) {
                    Text("删除此段记忆", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }

                if (status == "IN_PROGRESS") {
                    PixelButton(
                        onClick = { saveEvent("COMPLETED") },
                        backgroundColor = PixelMint,
                        borderColor = PixelTeal,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 10.dp),
                    ) {
                        Text("标记完成", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
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
        Text(label, style = PixelLabel, color = PixelGray)
        Text(
            time,
            style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall,
            color = PixelText,
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
    val state = rememberTimePickerState(initialHour = hour, initialMinute = minute, is24Hour = true)
    PixelDialog(
        onDismissRequest = onDismiss,
        backgroundColor = PixelCream,
        borderColor = PixelBorder,
    ) {
        TimePicker(state = state)
        Spacer(modifier = Modifier.padding(top = 8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            PixelButton(
                onClick = onDismiss,
                backgroundColor = Color.Transparent,
                borderColor = Color.Transparent,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("取消", style = PixelLabel, color = PixelCoral)
            }
            Spacer(Modifier.width(8.dp))
            PixelButton(
                onClick = { onConfirm(state.hour, state.minute) },
                backgroundColor = PixelTeal,
                borderColor = PixelBorder,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text("确定", style = PixelLabel, color = Color.White)
            }
        }
    }
}
