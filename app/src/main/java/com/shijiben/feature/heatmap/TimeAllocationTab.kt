package com.shijiben.feature.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.Disabled
import com.shijiben.ui.theme.DisabledText
import com.shijiben.ui.theme.Primary
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary

@Composable
fun TimeAllocationTab(
    viewModel: TimeAllocationViewModel = hiltViewModel()
) {
    val range by viewModel.selectedRange.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth().background(Background)) {
        // 范围选择器
        RangeSelector(
            selectedRange = range,
            onSelect = viewModel::selectRange
        )
        // 2dp 黑色分隔线
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        // 内容区
        when (val s = state) {
            is TimeAllocationViewModel.UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "加载中…", color = TextSecondary, fontSize = 16.sp)
                }
            }
            is TimeAllocationViewModel.UiState.Success -> {
                if (s.items.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "暂无记录", color = TextSecondary, fontSize = 16.sp)
                    }
                } else {
                    AllocationList(items = s.items)
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(
    selectedRange: TimeAllocationCalculator.TimeRange,
    onSelect: (TimeAllocationCalculator.TimeRange) -> Unit
) {
    val ranges = listOf(
        TimeAllocationCalculator.TimeRange.WEEK to "本周",
        TimeAllocationCalculator.TimeRange.MONTH to "本月",
        TimeAllocationCalculator.TimeRange.YEAR to "本年",
        TimeAllocationCalculator.TimeRange.ALL to "全部"
    )
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for ((r, label) in ranges) {
            val isSelected = r == selectedRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, if (isSelected) Primary else Disabled)
                    .background(if (isSelected) Primary else Color.Transparent)
                    .clickable { onSelect(r) }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else DisabledText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun AllocationList(items: List<TimeAllocationCalculator.TitleDuration>) {
    val maxMs = items.maxOf { it.totalMs }.coerceAtLeast(1L)
    LazyColumn(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items) { item ->
            AllocationRow(item = item, maxMs = maxMs)
        }
    }
}

@Composable
private fun AllocationRow(
    item: TimeAllocationCalculator.TitleDuration,
    maxMs: Long
) {
    val fraction = (item.totalMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标题
        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        // 像素方块条
        Box(
            modifier = Modifier
                .weight(2f)
                .height(16.dp)
                .border(2.dp, Color.Black)
                .background(SurfaceColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(16.dp)
                    .background(Primary)
            )
        }
        Spacer(Modifier.width(8.dp))
        // 时长
        Text(
            text = formatDuration(item.totalMs),
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** 格式化时长为 "Xh Ym"，hours=0 时只显示分。 */
private fun formatDuration(totalMs: Long): String {
    val totalMin = totalMs / 60_000
    val hours = totalMin / 60
    val mins = totalMin % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
