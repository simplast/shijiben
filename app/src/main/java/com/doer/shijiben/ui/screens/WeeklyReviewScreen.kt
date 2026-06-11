package com.doer.shijiben.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.doer.shijiben.ui.GoalProgress
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.theme.CoralOrange
import com.doer.shijiben.ui.theme.CoralOrangeDark
import com.doer.shijiben.ui.theme.CreamWhite
import com.doer.shijiben.ui.theme.DeepTeal
import com.doer.shijiben.ui.theme.LightSeaBlue
import com.doer.shijiben.ui.theme.MintBlue
import com.doer.shijiben.ui.theme.MistBlue
import com.doer.shijiben.ui.theme.PixelBadge
import com.doer.shijiben.ui.theme.PixelButton
import com.doer.shijiben.ui.theme.PixelCalendarIcon
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelCheckIcon
import com.doer.shijiben.ui.theme.PixelCloseIcon
import com.doer.shijiben.ui.theme.PixelDialog
import com.doer.shijiben.ui.theme.PixelDisplay
import com.doer.shijiben.ui.theme.PixelIconButton
import com.doer.shijiben.ui.theme.PixelInput
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.PixelPlayIcon
import com.doer.shijiben.ui.theme.PixelRefreshIcon
import com.doer.shijiben.ui.theme.PixelSectionHeader
import com.doer.shijiben.ui.theme.PixelStarIcon
import com.doer.shijiben.ui.theme.SeaBlue
import com.doer.shijiben.ui.theme.SeaBlueDark
import com.doer.shijiben.ui.theme.SeaBlueLight
import com.doer.shijiben.ui.theme.SunYellow
import com.doer.shijiben.ui.theme.WarmGray300
import com.doer.shijiben.ui.theme.WarmGray50
import com.doer.shijiben.ui.theme.WarmGray500
import com.doer.shijiben.ui.theme.primaryDoubleBorder
import com.doer.shijiben.ui.theme.secondaryDoubleBorder
import com.doer.shijiben.ui.theme.tertiaryDoubleBorder
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
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
                Text(
                    "本周尚无记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = WarmGray500,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(WarmGray50)
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top bar row (pixel-style)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PixelIconButton(
                            onClick = onBack,
                            icon = { PixelCloseIcon(color = DeepTeal, size = 20.dp) },
                            backgroundColor = Color.Transparent,
                            pressedBackgroundColor = SeaBlue.copy(alpha = 0.15f),
                            borderColor = Color.Transparent,
                            size = 36.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "本周回顾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepTeal,
                            modifier = Modifier.weight(1f),
                        )
                        PixelIconButton(
                            onClick = onOpenMonthlyReview,
                            icon = { PixelCalendarIcon(color = SeaBlue, size = 20.dp) },
                            backgroundColor = Color.Transparent,
                            pressedBackgroundColor = SeaBlue.copy(alpha = 0.15f),
                            borderColor = Color.Transparent,
                            size = 36.dp,
                        )
                        PixelIconButton(
                            onClick = {
                                val timestamp = System.currentTimeMillis()
                                exportLauncher.launch("shijiben_export_$timestamp.json")
                            },
                            icon = { PixelRefreshIcon(color = SeaBlue, size = 20.dp) },
                            backgroundColor = Color.Transparent,
                            pressedBackgroundColor = SeaBlue.copy(alpha = 0.15f),
                            borderColor = Color.Transparent,
                            size = 36.dp,
                        )
                    }
                }

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
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelSectionHeader(
                            title = "本周目标",
                            accentColor = CoralOrange,
                        )
                        PixelButton(
                            onClick = { goalDialogOpen = true },
                            backgroundColor = CoralOrange,
                            pressedBackgroundColor = CoralOrangeDark,
                            contentColor = Color.White,
                            borderColor = DeepTeal,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            PixelStarIcon(color = SunYellow, size = 14.dp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                "设定目标",
                                style = PixelLabel,
                                color = Color.White,
                            )
                        }
                    }
                }

                if (stats!!.goalProgress.isEmpty()) {
                    item {
                        Text(
                            "尚未设定目标，点上方「设定目标」开始",
                            style = MaterialTheme.typography.bodySmall,
                            color = WarmGray500,
                        )
                    }
                } else {
                    items(stats!!.goalProgress) { progress ->
                    GoalProgressItem(progress)
                }
            }

                item {
                    PixelSectionHeader(
                        title = "每日进展",
                        accentColor = SeaBlue,
                    )
                }

                items(stats!!.daySummaries) { day ->
                    DaySummaryItem(day)
                }

                item {
                    PixelSectionHeader(
                        title = "时间去哪儿了",
                        accentColor = LightSeaBlue,
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
        SeaBlue,
        CoralOrange,
        SunYellow,
        LightSeaBlue,
        MintBlue,
    )

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Secondary,
        backgroundColor = CreamWhite,
        borderOuterColor = SeaBlue,
        borderInnerColor = Color.White,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Canvas(modifier = Modifier.size(100.dp)) {
                var startAngle = -90f
                summaries.forEachIndexed { index, summary ->
                    val sweepAngle = (summary.totalMinutes.toFloat() / totalMinutes.toFloat()) * 360f
                    drawArc(
                        color = colors.getOrElse(index) { WarmGray300 },
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
                val currentTotal = summaries.sumOf { it.totalMinutes }
                if (currentTotal < totalMinutes) {
                    val sweepAngle = ((totalMinutes - currentTotal).toFloat() / totalMinutes.toFloat()) * 360f
                    drawArc(
                        color = MistBlue,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                summaries.forEachIndexed { index, summary ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(colors.getOrElse(index) { WarmGray300 })
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = summary.name,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            color = DeepTeal,
                        )
                    }
                }
                val currentTotal = summaries.sumOf { it.totalMinutes }
                if (currentTotal < totalMinutes) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(MistBlue)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("其他", style = MaterialTheme.typography.bodySmall, color = WarmGray500)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressItem(progress: GoalProgress) {
    val currentHours = progress.currentMinutes / 60
    val targetHours = progress.targetMinutes / 60
    val percent = if (progress.targetMinutes > 0) (progress.currentMinutes.toFloat() / progress.targetMinutes.toFloat()).coerceAtMost(1f) else 0f

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Tertiary,
        backgroundColor = MintBlue,
        borderOuterColor = SeaBlue,
        borderInnerColor = Color.White,
        contentPadding = PaddingValues(14.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    progress.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = DeepTeal,
                )
                Text(
                    "${currentHours}h / ${targetHours}h",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DeepTeal,
                )
            }
            Spacer(Modifier.height(10.dp))
            // Pixel-style progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(MistBlue)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percent)
                        .height(10.dp)
                        .background(if (percent >= 1f) CoralOrange else SeaBlue)
                )
            }
            if (percent >= 1f) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 6.dp),
                ) {
                    PixelStarIcon(color = SunYellow, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "目标已达成！",
                        style = PixelLabel,
                        color = CoralOrange,
                    )
                }
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

    PixelDialog(
        onDismissRequest = onDismiss,
        backgroundColor = CreamWhite,
        borderOuterColor = DeepTeal,
        borderInnerColor = Color.White,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            PixelSectionHeader(
                title = "设定本周目标",
                accentColor = CoralOrange,
            )
            Spacer(Modifier.height(4.dp))

            Text("项目/事件名称",
                style = PixelLabel,
                color = DeepTeal,
            )
            PixelInput(
                value = name,
                onValueChange = { name = it },
                placeholder = "输入名称",
            )

            Spacer(Modifier.height(8.dp))
            Text("目标时长 (小时)",
                style = PixelLabel,
                color = DeepTeal,
            )
            PixelInput(
                value = hours,
                onValueChange = { if (it.all { char -> char.isDigit() }) hours = it },
                placeholder = "5",
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                ),
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                PixelButton(
                    onClick = onDismiss,
                    backgroundColor = MistBlue,
                    pressedBackgroundColor = LightSeaBlue,
                    contentColor = DeepTeal,
                    borderColor = SeaBlue,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text("取消", style = PixelLabel, color = DeepTeal)
                }
                Spacer(Modifier.width(10.dp))
                PixelButton(
                    onClick = {
                        if (name.isNotBlank() && hours.isNotBlank()) {
                            onConfirm(name, hours.toInt())
                        }
                    },
                    backgroundColor = SeaBlue,
                    pressedBackgroundColor = SeaBlueDark,
                    contentColor = Color.White,
                    borderColor = DeepTeal,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    PixelCheckIcon(color = Color.White, size = 14.dp)
                    Spacer(Modifier.width(4.dp))
                    Text("确定", style = PixelLabel, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun WeeklySummaryCard(totalMinutes: Long, dayCount: Int) {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Primary,
        backgroundColor = SeaBlue,
        borderOuterColor = DeepTeal,
        borderInnerColor = Color.White,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelStarIcon(color = SunYellow, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            Text("周期汇总 (近7日)", style = PixelLabel, color = Color.White.copy(alpha = 0.9f))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = PixelDisplay,
                color = Color.White,
            )
            Text(
                text = " / 已记录时长",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "共计 ${dayCount} 天有记录，保持住！",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun DaySummaryItem(day: com.doer.shijiben.ui.DaySummary) {
    val hours = day.totalMinutes / 60
    val mins = day.totalMinutes % 60

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Tertiary,
        backgroundColor = CreamWhite,
        borderOuterColor = SeaBlue,
        borderInnerColor = Color.White,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelCalendarIcon(color = SeaBlue, size = 18.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    day.dateLabel,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DeepTeal,
                )
            }
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = MaterialTheme.typography.titleMedium,
                color = SeaBlue,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun EventSummaryItem(event: EventSummary) {
    val hours = event.totalMinutes / 60
    val mins = event.totalMinutes % 60

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = DeepTeal,
            )
            Text(
                "${event.count} 次记录",
                style = MaterialTheme.typography.bodySmall,
                color = WarmGray500,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelPlayIcon(color = LightSeaBlue, size = 14.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = MaterialTheme.typography.bodyLarge,
                color = SeaBlue,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
