@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.doer.shijiben.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.doer.shijiben.ui.DatePerspective
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.theme.PixelDivider
import kotlinx.coroutines.launch

// ============================================================
// HomeScreen — Main screen of Shijiben
// ============================================================
// Composed from sub-components:
//   TopBar, TodayOverviewCard, ActiveEventCard,
//   CompletedSection, PendingSection, QuickNameLine
// ============================================================

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

    // ── Dialogs ──
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
            scrimColor = Color.Black.copy(alpha = 0.32f),
            dragHandle = { DragHandle() }
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

    // ── Main Layout ──
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                // Top bar
                TopBar(
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

                PixelDivider()
                Spacer(Modifier.height(8.dp))

                // Scrollable content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp),
                ) {
                    // Today overview (only for today)
                    if (datePerspective == DatePerspective.TODAY) {
                        item {
                            TodayOverviewCard(
                                totalMinutes = todayTotalMinutes,
                                completedCount = completedEvents.size,
                                pendingCount = filteredPendingEvents.size,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // Active event card
                    activeEvent?.let { event ->
                        item {
                            ActiveEventCard(
                                event = event,
                                elapsedMinutes = elapsedMinutes,
                                onStop = { viewModel.stopActiveEvent() },
                                onClick = {
                                    editingEventId = event.id
                                    editorSheetVisible = true
                                }
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    // Completed section
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
                        Spacer(Modifier.height(16.dp))
                    }

                    // Pending section
                    item {
                        PendingSection(
                            events = filteredPendingEvents,
                            datePerspective = datePerspective,
                            recommendedNames = recommendedNames,
                            completedNames = completedNames,
                            onStart = { viewModel.startEvent(it) },
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

            // Floating bottom input (hidden for past dates)
            if (datePerspective != DatePerspective.PAST) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp, vertical = 16.dp)
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
