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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.doer.shijiben.ui.EventSummary
import com.doer.shijiben.ui.EventViewModel
import com.doer.shijiben.ui.theme.PixelBg
import com.doer.shijiben.ui.theme.PixelBorder
import com.doer.shijiben.ui.theme.PixelCalendarIcon
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelCloseIcon
import com.doer.shijiben.ui.theme.PixelCoral
import com.doer.shijiben.ui.theme.PixelCream
import com.doer.shijiben.ui.theme.PixelDisplay
import com.doer.shijiben.ui.theme.PixelGray
import com.doer.shijiben.ui.theme.PixelIconButton
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.PixelMint
import com.doer.shijiben.ui.theme.PixelPlayIcon
import com.doer.shijiben.ui.theme.PixelSectionHeader
import com.doer.shijiben.ui.theme.PixelSky
import com.doer.shijiben.ui.theme.PixelTeal
import com.doer.shijiben.ui.theme.PixelText
import com.doer.shijiben.ui.theme.PixelYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReviewScreen(
    viewModel: EventViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.monthlyStats.collectAsState()

    Scaffold { innerPadding ->
        if (stats == null || stats?.totalMinutes == 0L) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("本月尚无记录", style = MaterialTheme.typography.bodyMedium, color = PixelGray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(PixelBg)
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PixelIconButton(
                            onClick = onBack,
                            icon = { PixelCloseIcon(color = PixelBorder, size = 20.dp) },
                            backgroundColor = Color.Transparent,
                            pressedBackgroundColor = PixelTeal.copy(alpha = 0.15f),
                            borderColor = Color.Transparent,
                            size = 36.dp,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "月度回顾",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = PixelText,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                item {
                    MonthlySummaryCard(
                        monthLabel = stats!!.monthLabel,
                        totalMinutes = stats!!.totalMinutes,
                        dayCount = stats!!.daySummaries.size
                    )
                }

                item {
                    SimplePieChart(
                        summaries = if (stats!!.categorySummaries.isNotEmpty()) stats!!.categorySummaries.take(5) else stats!!.topEvents.take(5),
                        totalMinutes = stats!!.totalMinutes
                    )
                }

                if (stats!!.categorySummaries.isNotEmpty()) {
                    item {
                        PixelSectionHeader(title = "分类统计", accentColor = PixelCoral)
                    }
                    items(stats!!.categorySummaries) { cat ->
                        CategorySummaryItem(cat)
                    }
                }

                item {
                    PixelSectionHeader(title = "时间去哪儿了", accentColor = PixelSky)
                }
                items(stats!!.topEvents) { event ->
                    EventSummaryItem(event)
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Sub-components
// ═══════════════════════════════════════════════════════════

@Composable
private fun SimplePieChart(summaries: List<EventSummary>, totalMinutes: Long) {
    if (totalMinutes == 0L) return

    val colors = listOf(PixelTeal, PixelCoral, PixelYellow, PixelSky, PixelMint)

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Secondary,
        backgroundColor = PixelCream,
        borderColor = PixelTeal,
        contentPadding = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Canvas(modifier = Modifier.size(100.dp)) {
                var startAngle = -90f
                summaries.forEachIndexed { index, summary ->
                    val sweepAngle = (summary.totalMinutes.toFloat() / totalMinutes.toFloat()) * 360f
                    drawArc(
                        color = colors.getOrElse(index) { PixelGray },
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = true
                    )
                    startAngle += sweepAngle
                }
                val currentSummariesTotal = summaries.sumOf { it.totalMinutes }
                if (currentSummariesTotal < totalMinutes) {
                    drawArc(
                        color = PixelMint,
                        startAngle = startAngle,
                        sweepAngle = ((totalMinutes - currentSummariesTotal).toFloat() / totalMinutes.toFloat()) * 360f,
                        useCenter = true
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                summaries.forEachIndexed { index, summary ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(colors.getOrElse(index) { PixelGray }))
                        Spacer(Modifier.width(8.dp))
                        Text(summary.name, style = MaterialTheme.typography.bodySmall, maxLines = 1, color = PixelText)
                    }
                }
                val currentSummariesTotal = summaries.sumOf { it.totalMinutes }
                if (currentSummariesTotal < totalMinutes) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(10.dp).background(PixelMint))
                        Spacer(Modifier.width(8.dp))
                        Text("其他", style = MaterialTheme.typography.bodySmall, color = PixelGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthlySummaryCard(monthLabel: String, totalMinutes: Long, dayCount: Int) {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Primary,
        backgroundColor = PixelTeal,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelCalendarIcon(color = PixelYellow, size = 20.dp)
            Spacer(Modifier.width(8.dp))
            Text(monthLabel, style = PixelLabel, color = Color.White.copy(alpha = 0.9f))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = PixelDisplay,
                color = Color.White,
            )
            Text(
                text = " / 月度总计",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "本月累计记录 ${dayCount} 天",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun CategorySummaryItem(cat: EventSummary) {
    val hours = cat.totalMinutes / 60
    val mins = cat.totalMinutes % 60

    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Tertiary,
        backgroundColor = PixelMint,
        borderColor = PixelTeal,
        contentPadding = PaddingValues(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PixelPlayIcon(color = PixelCoral, size = 16.dp)
                Spacer(Modifier.width(10.dp))
                Text(cat.name, style = MaterialTheme.typography.bodyLarge, color = PixelText)
            }
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = MaterialTheme.typography.titleMedium,
                color = PixelTeal,
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(event.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = PixelText)
            Text("${event.count} 次记录", style = MaterialTheme.typography.bodySmall, color = PixelGray)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            PixelPlayIcon(color = PixelSky, size = 14.dp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = MaterialTheme.typography.bodyLarge,
                color = PixelTeal,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
