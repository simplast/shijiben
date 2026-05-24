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
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Event
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlyReviewScreen(
    viewModel: EventViewModel,
    onBack: () -> Unit
) {
    val stats by viewModel.monthlyStats.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("月度回顾") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (stats == null || stats?.totalMinutes == 0L) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("本月尚无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text(
                            "分类统计",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(stats!!.categorySummaries) { cat ->
                        CategorySummaryItem(cat)
                    }
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
                    EventSummaryItemForMonth(event)
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
                val currentSummariesTotal = summaries.sumOf { it.totalMinutes }
                if (currentSummariesTotal < totalMinutes) {
                    val sweepAngle = ((totalMinutes - currentSummariesTotal).toFloat() / totalMinutes.toFloat()) * 360f
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
                val currentSummariesTotal = summaries.sumOf { it.totalMinutes }
                if (currentSummariesTotal < totalMinutes) {
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
private fun MonthlySummaryCard(monthLabel: String, totalMinutes: Long, dayCount: Int) {
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.QueryStats, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(monthLabel, style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = " / 月度总计",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = "本月累计记录 ${dayCount} 天",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun CategorySummaryItem(cat: EventSummary) {
    val hours = cat.totalMinutes / 60
    val mins = cat.totalMinutes % 60
    
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
                Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(12.dp))
                Text(cat.name, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = if (hours > 0) "${hours}h ${mins}m" else "${mins}m",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

@Composable
private fun EventSummaryItemForMonth(event: EventSummary) {
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
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
    }
}
