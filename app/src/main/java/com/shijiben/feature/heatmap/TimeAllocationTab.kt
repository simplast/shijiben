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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.ui.theme.Accent
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.Disabled
import com.shijiben.ui.theme.DisabledText
import com.shijiben.ui.theme.Primary
import com.shijiben.ui.theme.RainbowCyan
import com.shijiben.ui.theme.RainbowLime
import com.shijiben.ui.theme.RainbowPink
import com.shijiben.ui.theme.RainbowPurple
import com.shijiben.ui.theme.Secondary
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary
import com.shijiben.ui.theme.Warning

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
        itemsIndexed(items) { index, item ->
            AllocationRow(rank = index, item = item, maxMs = maxMs)
        }
    }
}

/**
 * 排名色板：
 * - 0=Primary 红（最大时间去向，最高警示色）
 * - 1=Accent 橙
 * - 2=Warning 黄
 * - 3=Secondary 绿
 * - 4+=冷色循环（青/紫/粉/柠檬绿），与"前 4 名暖色"形成视觉分层
 * 与 8-bit 像素风一致：高饱和、强对比、有"游戏排行榜"感。
 *
 * internal 供单测直接调（与 filterAndMerge 同模式）。
 */
internal fun rankColor(rank: Int): Color = when (rank) {
    0 -> Primary
    1 -> Accent
    2 -> Warning
    3 -> Secondary
    else -> listOf(RainbowCyan, RainbowPurple, RainbowPink, RainbowLime)[(rank - 4) % 4]
}

/** 格式化时长为 "Xh Ym"，hours=0 时只显示分。internal 供单测直接调。 */
internal fun formatAllocationDuration(totalMs: Long): String {
    val totalMin = totalMs / 60_000
    val hours = totalMin / 60
    val mins = totalMin % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}

@Composable
private fun AllocationRow(
    rank: Int,
    item: TimeAllocationCalculator.TitleDuration,
    maxMs: Long
) {
    val fraction = (item.totalMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)
    val barColor = rankColor(rank)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 排名徽章（仅前 3 名显示，避免长列表视觉拥挤）
        if (rank < 3) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, Color.Black)
                    .background(barColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (rank + 1).toString(),
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.width(6.dp))
        } else {
            // 4 名以后留空，与 title 对齐（保持 20+6=26dp 占位）
            Spacer(Modifier.width(26.dp))
        }
        // 标题：固定宽度（约 6 个 16sp 字符），单行省略，保证柱状图起点对齐
        Text(
            text = item.title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(96.dp)
        )
        Spacer(Modifier.width(8.dp))
        // 像素方块条：weight(1f) 填充剩余空间，所有行总长度一致
        // 颜色按排名循环：前 4 名暖色（红/橙/黄/绿），5+ 冷色循环（青/紫/粉/柠檬绿）
        Box(
            modifier = Modifier
                .weight(1f)
                .height(16.dp)
                .border(2.dp, Color.Black)
                .background(SurfaceColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(16.dp)
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        // 时长：固定宽度右对齐，避免长度不一挤压柱状图
        Text(
            text = formatAllocationDuration(item.totalMs),
            color = TextSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(64.dp),
            textAlign = TextAlign.End
        )
    }
}
