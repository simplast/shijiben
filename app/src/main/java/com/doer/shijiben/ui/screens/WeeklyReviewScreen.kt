package com.doer.shijiben.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doer.shijiben.ui.EventSummary
import com.doer.shijiben.ui.EventViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyReviewScreen(
    viewModel: EventViewModel,
    onBack: () -> Unit,
    onOpenMonthlyReview: () -> Unit
) {
    val stats by viewModel.weeklyStats.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var goalDialogOpen by remember { mutableStateOf(false) }

    if (goalDialogOpen) {
        GoalDialog(
            onDismiss = { goalDialogOpen = false },
            onConfirm = { name, hours ->
                viewModel.upsertGoal(name, hours)
                goalDialogOpen = false
            }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            viewModel.exportDataAsJson(context, it) { err ->
                scope.launch {
                    if (err != null) {
                        snackbarHostState.showSnackbar("导出失败: ${err.message}")
                    } else {
                        snackbarHostState.showSnackbar("导出成功")
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("本周回顾") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMonthlyReview) {
                        Icon(Icons.Default.Event, contentDescription = "月度回顾")
                    }
                    IconButton(onClick = {
                        val timestamp = System.currentTimeMillis()
                        exportLauncher.launch("shijiben_export_$timestamp.json")
                    }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "导出数据")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (stats == null || stats?.totalMinutes == 0L) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("本周尚无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WeeklySummaryCard(
                        totalMinutes = stats!!.totalMinutes,
                        dayCount = stats!!.daySummaries.size
                    )
                }

                item {
                    SimplePieChart(
                        summaries = stats!!.topEvents.take(5),
                        totalMinutes = stats!!.totalMinutes
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "本周目标",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        androidx.compose.material3.TextButton(onClick = { goalDialogOpen = true }) {
                            Text("设定目标")
                        }
                    }
                }

                if (stats!!.goalProgress.isEmpty()) {
                    item {
                        Text(
                            "尚未设定目标，点上方「设定目标」开始",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(stats!!.goalProgress) { progress ->
                        GoalProgressItem(progress)
                    }
                }

                item {
                    Text(
                        "每日进展",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(stats!!.daySummaries) { day ->
                    DaySummaryItem(day)
                }

                item {
                    Text(
                        "时间去哪儿了",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(stats!!.topEvents) { event ->
                    EventSummaryItem(event)
                }
                
                item {
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun SimplePieChart(summaries: List<EventSummary>, totalMinutes: Long) {
    if (totalMinutes == 0L) return
    
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primaryContainer,
        MaterialTheme.colorScheme.secondaryContainer
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                var startAngle = -90f
                summaries.forEachIndexed { index, summary ->
                    val sweepAngle = (summary.totalMinutes.toFloat() / totalMinutes.toFloat()) * 360f
                    drawArc(
                        color = colors.getOrElse(index) { Color.Gray },
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
                // Draw remaining as gray
                val currentTotal = summaries.sumOf { it.totalMinutes }
                if (currentTotal < totalMinutes) {
                    val sweepAngle = ((totalMinutes - currentTotal).toFloat() / totalMinutes.toFloat()) * 360f
                    drawArc(
                        color = Color.LightGray,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                }
            }
            
            Spacer(Modifier.width(24.dp))
            
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                summaries.forEachIndexed { index, summary ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(colors.getOrElse(index) { Color.Gray }, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = summary.name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                if (summaries.sumOf { it.totalMinutes } < totalMinutes) {
                     Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(Color.LightGray, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text("其他", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressItem(progress: com.doer.shijiben.ui.GoalProgress) {
    val currentHours = progress.currentMinutes / 60
    val targetHours = progress.targetMinutes / 60
    val percent = if (progress.targetMinutes > 0) (progress.currentMinutes.toFloat() / progress.targetMinutes.toFloat()).coerceAtMost(1f) else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(progress.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${currentHours}h / ${targetHours}h", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(8.dp))
            androidx.compose.material3.LinearProgressIndicator(
                progress = { percent },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = if (percent >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surface
            )
            if (percent >= 1f) {
                Text(
                    "🎉 目标已达成！",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun GoalDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, hours: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("5") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设定本周目标") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("项目/事件名称") },
                    singleLine = true
                )
                androidx.compose.material3.OutlinedTextField(
                    value = hours,
                    onValueChange = { if (it.all { char -> char.isDigit() }) hours = it },
                    label = { Text("目标时长 (小时)") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = {
                    if (name.isNotBlank() && hours.isNotBlank()) {
                        onConfirm(name, hours.toInt())
                    }
                }
            ) { Text("确定") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun WeeklySummaryCard(totalMinutes: Long, dayCount: Int) {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("周期汇总 (近7日)", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " / 已记录时长",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "共计 ${dayCount} 天有记录，保持住！",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun DaySummaryItem(day: com.doer.shijiben.ui.DaySummary) {
    val hours = day.totalMinutes / 60
    val mins = day.totalMinutes % 60
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(day.dateLabel, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EventSummaryItem(event: EventSummary) {
    val hours = event.totalMinutes / 60
    val mins = event.totalMinutes % 60
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(event.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("${event.count} 次记录", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Timer, 
                    contentDescription = null, 
                    modifier = Modifier.size(14.dp), 
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
