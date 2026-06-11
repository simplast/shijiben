@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.doer.shijiben.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import com.doer.shijiben.ui.theme.PixelInput
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.TimeFormats
import com.doer.shijiben.ui.DatePerspective
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.theme.SeaBlue
import com.doer.shijiben.ui.theme.SeaBlueDark
import com.doer.shijiben.ui.theme.SeaBlueLight
import com.doer.shijiben.ui.theme.LightSeaBlue
import com.doer.shijiben.ui.theme.MintBlue
import com.doer.shijiben.ui.theme.MistBlue
import com.doer.shijiben.ui.theme.SunYellow
import com.doer.shijiben.ui.theme.CoralOrange
import com.doer.shijiben.ui.theme.CoralOrangeDark
import com.doer.shijiben.ui.theme.LightPink
import com.doer.shijiben.ui.theme.CreamWhite
import com.doer.shijiben.ui.theme.DeepTeal
import com.doer.shijiben.ui.theme.WarmGray50
import com.doer.shijiben.ui.theme.WarmGray200
import com.doer.shijiben.ui.theme.WarmGray300
import com.doer.shijiben.ui.theme.WarmGray500
import com.doer.shijiben.ui.theme.PixelDisplay
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelButton
import com.doer.shijiben.ui.theme.PixelIconButton
import com.doer.shijiben.ui.theme.PixelBadge
import com.doer.shijiben.ui.theme.PixelSectionHeader
import com.doer.shijiben.ui.theme.PixelShape
import com.doer.shijiben.ui.theme.primaryDoubleBorder
import com.doer.shijiben.ui.theme.secondaryDoubleBorder
import com.doer.shijiben.ui.theme.tertiaryDoubleBorder
import com.doer.shijiben.ui.theme.doublePixelBorder
import com.doer.shijiben.ui.theme.PixelPlayIcon
import com.doer.shijiben.ui.theme.PixelStopIcon
import com.doer.shijiben.ui.theme.PixelCloseIcon
import com.doer.shijiben.ui.theme.PixelRefreshIcon
import com.doer.shijiben.ui.theme.PixelMoreIcon
import com.doer.shijiben.ui.theme.PixelCalendarIcon
import com.doer.shijiben.ui.theme.PixelAddIcon
import com.doer.shijiben.ui.theme.PixelHopNumber
import com.doer.shijiben.ui.theme.pixelStateTransition
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
    val completedNames = completedEvents.map { it.name.trim() }.toSet()
    val pendingEvents by viewModel.pendingEventsForSelectedDay.collectAsState()
    val filteredPendingEvents = pendingEvents.filter { it.status != "IN_PROGRESS" }

    val todayTotalMinutes = if (datePerspective == DatePerspective.TODAY) {
        completedEvents.sumOf { (it.endTimeMillis - it.startTimeMillis) / 60_000L }
    } else 0L

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
            shape = PixelShape,
            tonalElevation = 0.dp,
            scrimColor = Color.Black.copy(alpha = 0.32f),
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
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

                // ── Today Overview Hero (only shown for TODAY) ──
                if (datePerspective == DatePerspective.TODAY) {
                    TodayOverviewCard(
                        totalMinutes = todayTotalMinutes,
                        completedCount = completedEvents.size,
                        pendingCount = filteredPendingEvents.size,
                    )
                    // Task 12.1: 16dp spacing from overview to active event
                    Spacer(Modifier.height(16.dp))
                }

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
                    contentPadding = PaddingValues(bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    item {
                        // Task 12.2: 12dp spacing from active to completed
                        Spacer(Modifier.height(12.dp))
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
                        // Task 12.3: 20dp spacing from completed to pending
                        Spacer(Modifier.height(20.dp))
                        PendingSection(
                            events = filteredPendingEvents,
                            datePerspective = datePerspective,
                            recommendedNames = recommendedNames,
                            completedNames = completedNames,
                            activeElapsedMinutes = elapsedMinutes,
                            onStart = { viewModel.startEvent(it) },
                            onStop = { viewModel.stopActiveEvent() },
                            onAdd = { viewModel.quickAddEvent(it) },
                            onQuickStart = { viewModel.quickStartEvent(it) },
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
                        .padding(horizontal = 20.dp, vertical = 16.dp)
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

// ═══════════════════════════════════════════════════════════
// Today Overview Hero Card (Tasks 4.1-4.3)
// ═══════════════════════════════════════════════════════════

@Composable
private fun TodayOverviewCard(
    totalMinutes: Long,
    completedCount: Int,
    pendingCount: Int,
) {
    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Primary,
        backgroundColor = SeaBlue,
        borderOuterColor = DeepTeal,
        borderInnerColor = Color.White,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: large time number
            Column(horizontalAlignment = Alignment.Start) {
                PixelHopNumber(
                    targetValue = totalMinutes,
                    format = { "${it}m" },
                    textStyle = PixelDisplay,
                    contentColor = Color.White,
                )
                Text(
                    text = "已专注",
                    style = PixelLabel,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Right: stacked badges
            Column(horizontalAlignment = Alignment.End) {
                // Completed badge — CoralOrange
                PixelBadge(
                    text = "✓ $completedCount 已完成",
                    backgroundColor = CoralOrange,
                    contentColor = Color.White,
                )
                Spacer(Modifier.height(6.dp))
                // Pending badge — LightSeaBlue
                PixelBadge(
                    text = "◇ $pendingCount 待办",
                    backgroundColor = LightSeaBlue,
                    contentColor = Color.White,
                )
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
// Completed Section (Tasks 6.1-6.5, 8.2)
// ═══════════════════════════════════════════════════════════

private val completedBarColors = listOf(
    SeaBlue,
    LightSeaBlue,
    SunYellow,
    CoralOrange,
    MintBlue,
)

@Composable
private fun CompletedSection(
    events: List<EventEntity>,
    datePerspective: DatePerspective,
    onRestart: (EventEntity) -> Unit,
    onDelete: (EventEntity) -> Unit,
    onOpen: (EventEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // PixelSectionHeader with SeaBlue accent + Chinese title
        PixelSectionHeader(
            title = "COMPLETED",
            chineseTitle = if (datePerspective == DatePerspective.TODAY) "今天已完成" else "已完成",
            accentColor = SeaBlue,
        )
        Spacer(Modifier.height(8.dp))

        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            level = PixelCardLevel.Secondary,
            backgroundColor = MintBlue,
            borderOuterColor = SeaBlue,
            borderInnerColor = Color.White,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                            color = WarmGray500,
                            modifier = Modifier.alpha(0.5f),
                        )
                    }
                } else {
                    events.forEachIndexed { index, event ->
                        CompletedEventRow(
                            event = event,
                            datePerspective = datePerspective,
                            barColor = completedBarColors[index % completedBarColors.size],
                            onRestart = { onRestart(event) },
                            onDelete = { onDelete(event) },
                            onClick = { onOpen(event) },
                        )
                        if (index < events.lastIndex) {
                            Hairline(alpha = 0.2f)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Completed Event Row (Tasks 6.2-6.5)
// ═══════════════════════════════════════════════════════════

@Composable
private fun CompletedEventRow(
    event: EventEntity,
    datePerspective: DatePerspective,
    barColor: Color,
    onRestart: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val minutes = ((event.endTimeMillis - event.startTimeMillis) / 60_000L).coerceAtLeast(0L)
    val meta = "${TimeFormats.formatTimeMillis(event.startTimeMillis)}—${TimeFormats.formatTimeMillis(event.endTimeMillis)} · ${minutes}m"
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)  // Task 6.2: compact row
            .pixelStateTransition(stateKey = event.id)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressScaleEffect(interactionSource),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Task 6.3: 3dp rotating color bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(barColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = event.name,
            style = MaterialTheme.typography.bodySmall,  // Task 6.2: 12sp
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(4.dp))
        // Task 6.4: WarmGray500 + PixelLabel style meta
        Text(
            text = meta,
            style = PixelLabel,
            color = WarmGray500,
            maxLines = 1,
        )
        // Restart button — pixel style
        if (datePerspective != DatePerspective.PAST) {
            PixelIconButton(
                onClick = onRestart,
                icon = { PixelRefreshIcon(color = SeaBlue, size = 18.dp) },
                backgroundColor = MistBlue,
                pressedBackgroundColor = SeaBlueLight,
                borderColor = SeaBlue,
                size = 28.dp,
            )
        }
        // Delete button — pixel style (larger for better hit area)
        PixelIconButton(
            onClick = onDelete,
            icon = { PixelCloseIcon(color = CoralOrange, size = 18.dp) },
            backgroundColor = CoralOrange.copy(alpha = 0.12f),
            pressedBackgroundColor = CoralOrange.copy(alpha = 0.25f),
            borderColor = CoralOrange,
            size = 28.dp,
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Pending Section (Tasks 7.1-7.4, 8.3, 9.1-9.2)
// ═══════════════════════════════════════════════════════════

private val playButtonColors = listOf(
    CoralOrange,
    LightSeaBlue,
    SeaBlue,
    SunYellow,
    MintBlue,
)

@Composable
private fun PendingSection(
    events: List<EventEntity>,
    datePerspective: DatePerspective,
    recommendedNames: List<String>,
    completedNames: Set<String>,
    activeElapsedMinutes: Long,
    onStart: (EventEntity) -> Unit,
    onStop: () -> Unit,
    onAdd: (String) -> Unit,
    onQuickStart: (String) -> Unit,
    onDelete: (EventEntity) -> Unit,
    onOpen: (EventEntity) -> Unit
) {
    val existingNames = events.map { it.name.trim() }.toSet()

    Column(modifier = Modifier.fillMaxWidth()) {
        val validRecommendations = recommendedNames
            .map { it.trim() }
            .filter { !existingNames.contains(it) && !completedNames.contains(it) }

        if (validRecommendations.isNotEmpty() && datePerspective != DatePerspective.PAST) {
            Spacer(Modifier.height(12.dp))
            // PixelCoralRed section header for recommendations
            PixelSectionHeader(
                title = "QUICK START",
                chineseTitle = "建议快速开始",
                accentColor = CoralOrange,
            )
            Spacer(Modifier.height(6.dp))
            // Tasks 9.1-9.2: Colorful pixel tags
            val tagColors = listOf(CoralOrange, SeaBlue, SunYellow, LightSeaBlue)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(validRecommendations.size) { index ->
                    val name = validRecommendations[index]
                    val tagColor = tagColors[index % tagColors.size]
                    val interactionSource = remember { MutableInteractionSource() }
                    PixelBadge(
                        text = name,
                        backgroundColor = tagColor,
                        contentColor = Color.White,
                        modifier = Modifier
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onQuickStart(name) }
                            )
                            .pressScaleEffect(interactionSource),
                    )
                }
            }
        }

        if (datePerspective == DatePerspective.PAST && events.isEmpty()) {
            EmptyLine(datePerspective)
            return
        }

        Spacer(Modifier.height(12.dp))
        // SunYellow section header for pending
        PixelSectionHeader(
            title = "PENDING",
            chineseTitle = if (datePerspective == DatePerspective.TODAY) "今日待办" else "待办",
            accentColor = SunYellow,
        )
        Spacer(Modifier.height(8.dp))

        // Task 7.1: White bg + 2dp amber border + 5dp gold left edge
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            level = PixelCardLevel.Secondary,
            backgroundColor = CreamWhite,
            borderOuterColor = SeaBlue,
            borderInnerColor = Color.White,
            contentPadding = PaddingValues(all = 0.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left 5dp gold edge bar
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(SunYellow)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                                color = WarmGray500,
                                modifier = Modifier.alpha(0.5f),
                            )
                        }
                    } else {
                        events.forEachIndexed { index, event ->
                            PendingEventRow(
                                event = event,
                                datePerspective = datePerspective,
                                activeElapsedMinutes = activeElapsedMinutes,
                                playButtonColor = playButtonColors[index % playButtonColors.size],
                                onStart = { onStart(event) },
                                onStop = onStop,
                                onDelete = { onDelete(event) },
                                onClick = { onOpen(event) },
                            )
                            if (index < events.lastIndex) {
                                Hairline(alpha = 0.2f)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Pending Event Row (Tasks 7.2-7.4)
// ═══════════════════════════════════════════════════════════

@Composable
private fun PendingEventRow(
    event: EventEntity,
    datePerspective: DatePerspective,
    activeElapsedMinutes: Long,
    playButtonColor: Color,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .pixelStateTransition(stateKey = event.id)
            .then(
                if (isPressed) Modifier.background(SunYellow.copy(alpha = 0.15f))
                else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .pressScaleEffect(interactionSource),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 3dp sun yellow left bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(SunYellow)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = event.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(4.dp))

        if (datePerspective == DatePerspective.TODAY) {
            PixelIconButton(
                onClick = onStart,
                icon = { PixelPlayIcon(color = Color.White, size = 18.dp) },
                backgroundColor = playButtonColor,
                pressedBackgroundColor = playButtonColor.copy(alpha = 0.8f),
                borderColor = DeepTeal,
                size = 32.dp,
            )
        }
        PixelIconButton(
            onClick = onDelete,
            icon = { PixelCloseIcon(color = CoralOrange, size = 18.dp) },
            backgroundColor = CoralOrange.copy(alpha = 0.12f),
            pressedBackgroundColor = CoralOrange.copy(alpha = 0.25f),
            borderColor = CoralOrange,
            size = 32.dp,
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Top Bar (Tasks 10.1-10.3)
// ═══════════════════════════════════════════════════════════

@Composable
private fun LineTopBar(
    dateLabel: String,
    onPickDate: () -> Unit,
    onOpenReview: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Pixel calendar icon
        PixelIconButton(
            onClick = onPickDate,
            icon = { PixelCalendarIcon(color = SeaBlue, size = 20.dp) },
            backgroundColor = Color.Transparent,
            pressedBackgroundColor = SeaBlue.copy(alpha = 0.15f),
            borderColor = Color.Transparent,
            size = 36.dp,
        )
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
        // Task 10.1: MoreVert icon (PixelDeepNavy tint)
        Box {
            PixelIconButton(
                onClick = { menuExpanded = true },
                icon = { PixelMoreIcon(color = DeepTeal, size = 20.dp) },
                backgroundColor = Color.Transparent,
                pressedBackgroundColor = DeepTeal.copy(alpha = 0.1f),
                borderColor = Color.Transparent,
                size = 36.dp,
            )
            // Task 10.2-10.3: DropdownMenu with pixel style
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                shape = PixelShape,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .background(CreamWhite)
                    .secondaryDoubleBorder(
                        outerColor = SeaBlue,
                        innerColor = Color.White,
                    ),
            ) {
                DropdownMenuItem(
                    text = { Text("数据统计", style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        menuExpanded = false
                        onOpenReview()
                    },
                    leadingIcon = {
                        PixelMoreIcon(color = DeepTeal, size = 18.dp)
                    },
                )
                DropdownMenuItem(
                    text = { Text("导出 CSV", style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        menuExpanded = false
                        onExportCsv()
                    },
                    leadingIcon = {
                        PixelAddIcon(color = DeepTeal, size = 18.dp)
                    },
                )
                DropdownMenuItem(
                    text = { Text("导出 JSON", style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        menuExpanded = false
                        onExportJson()
                    },
                    leadingIcon = {
                        PixelCalendarIcon(color = DeepTeal, size = 18.dp)
                    },
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Quick Name Line (Tasks 11.1-11.3)
// ═══════════════════════════════════════════════════════════

@Composable
private fun QuickNameLine(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(start = 12.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Coral "+" icon
        PixelAddIcon(
            color = CoralOrange,
            size = 18.dp,
        )
        Spacer(Modifier.width(10.dp))
        PixelInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = "今天想做点什么...",
            modifier = Modifier.weight(1f),
        )
        // Submit button — coral orange + play icon
        PixelIconButton(
            onClick = onAdd,
            icon = { PixelPlayIcon(color = Color.White, size = 20.dp) },
            backgroundColor = CoralOrange,
            pressedBackgroundColor = CoralOrangeDark,
            borderColor = DeepTeal,
            size = 36.dp,
        )
    }
}

// ═══════════════════════════════════════════════════════════
// Shared Utilities
// ═══════════════════════════════════════════════════════════

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
            color = WarmGray500,
        )
    }
}

@Composable
private fun Hairline(alpha: Float = 0.7f) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(WarmGray200.copy(alpha = alpha))
    )
}

@Composable
private fun LineDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .width(34.dp)
            .height(2.dp)
            .background(WarmGray300, PixelShape)
    )
}

// ═══════════════════════════════════════════════════════════
// Active Event Hero Card (Tasks 5.1-5.5)
// ═══════════════════════════════════════════════════════════

@Composable
private fun ActiveEventCard(
    event: EventEntity,
    elapsedMinutes: Long,
    onStop: () -> Unit,
    onClick: () -> Unit,
) {
    // Pixel blink animation — keyframes instant-switch (800ms cycle)
    val infiniteTransition = rememberInfiniteTransition(label = "pixelBlink")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 800
                0f at 0
                1f at 400
                0f at 401
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "pixelBlink"
    )

    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "pressScale"
    )

    PixelCard(
        modifier = Modifier
            .fillMaxWidth()
            .pixelStateTransition(stateKey = event.id)
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
        level = PixelCardLevel.Primary,
        backgroundColor = LightPink,
        borderOuterColor = DeepTeal,
        borderInnerColor = Color.White,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Blinking indicator square
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .graphicsLayer { alpha = blinkAlpha }
                            .background(SunYellow, PixelShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "正在记录时间...",
                        style = PixelLabel,
                        color = Color.White.copy(alpha = 0.85f),
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
                PixelHopNumber(
                    targetValue = elapsedMinutes,
                    format = { "${it}m" },
                    textStyle = PixelDisplay,
                    contentColor = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                // Stop button
                PixelButton(
                    onClick = onStop,
                    backgroundColor = SeaBlue,
                    pressedBackgroundColor = SeaBlueDark,
                    contentColor = Color.White,
                    borderColor = DeepTeal,
                    borderInnerColor = Color.White,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    PixelStopIcon(color = Color.White, size = 16.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "结束",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}


// ═══════════════════════════════════════════════════════════
// Pixel Border Modifier (8-bit dashed style)
// ═══════════════════════════════════════════════════════════

fun Modifier.pixelBorder(
    color: Color,
    width: Dp = 2.dp,
    pixelSize: Dp = 4.dp,
    gap: Dp = 2.dp,
): Modifier = this.drawBehind {
    val strokeWidth = width.toPx()
    val px = pixelSize.toPx()
    val gp = gap.toPx()
    val step = px + gp

    // Top edge
    var x = 0f
    while (x < size.width) {
        drawRect(color, Offset(x, 0f), Size(px.coerceAtMost(size.width - x), strokeWidth))
        x += step
    }
    // Bottom edge
    x = 0f
    while (x < size.width) {
        drawRect(color, Offset(x, size.height - strokeWidth), Size(px.coerceAtMost(size.width - x), strokeWidth))
        x += step
    }
    // Left edge
    var y = 0f
    while (y < size.height) {
        drawRect(color, Offset(0f, y), Size(strokeWidth, px.coerceAtMost(size.height - y)))
        y += step
    }
    // Right edge
    y = 0f
    while (y < size.height) {
        drawRect(color, Offset(size.width - strokeWidth, y), Size(strokeWidth, px.coerceAtMost(size.height - y)))
        y += step
    }
}

// ═══════════════════════════════════════════════════════════
// Modifier Extension
// ═══════════════════════════════════════════════════════════

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
