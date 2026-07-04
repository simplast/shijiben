package com.shijiben.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.RainbowHourColors
import com.shijiben.ui.theme.RainbowTrim
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary

/**
 * 本地生物钟 Screen（Circadian Fingerprint）。
 *
 * 布局：
 * - 顶部 8dp 彩虹条 + 标题栏（左返回 + "我的生物钟"）+ 2dp 黑色分隔线
 * - 一行说明"按事件开始时间统计，事实呈现"（唯一允许的说明性文字，不含任何评判）
 * - 24 行水平条形图列表（0-23 小时，每行：小时标签 + 条 + "N 事件 · Xh Ym"）
 *
 * 哲学：纯事实镜。绝无"建议早睡"之类评语，无对比基准。
 * 条形图样式复用 TimeAllocationTab 的画法（Box + 2dp 黑边 + 内部 fillMaxWidth(fraction)），
 * 但不直接 import heatmap 包的 private 组件，复制相同的 Modifier。
 */
@Composable
fun CircadianScreen(
    onBack: () -> Unit,
    viewModel: CircadianViewModel = hiltViewModel()
) {
    val buckets by viewModel.buckets.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部 8dp 彩虹条（与首页/timeviz/heatmap/settings 一致）
            RainbowTrim()
            // 顶栏：左返回 + 标题「我的生物钟」（与 NotesScreen/AboutScreen 标准结构一致）
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "我的生物钟",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = TextPrimary
                )
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 唯一允许的说明性文字，纯事实，无评判
            Text(
                text = "按事件开始时间统计，事实呈现",
                fontSize = 14.sp,
                color = TextSecondary,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // 内容区
            if (buckets.isEmpty()) {
                // StateFlow 初始值 emptyList → loading 期
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "加载中…", color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                CircadianBarList(buckets)
            }
        }
    }
}

@Composable
private fun CircadianBarList(buckets: List<CircadianCalculator.HourBucket>) {
    // 归一化基准：24 桶里最大的 durationMs（coerceAtLeast 1L 避免除零）
    val maxMs = buckets.maxOf { it.durationMs }.coerceAtLeast(1L)
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(buckets, key = { it.hour }) { bucket ->
            CircadianBarRow(bucket = bucket, maxMs = maxMs)
        }
    }
}

@Composable
private fun CircadianBarRow(
    bucket: CircadianCalculator.HourBucket,
    maxMs: Long
) {
    val fraction = (bucket.durationMs.toFloat() / maxMs.toFloat()).coerceIn(0f, 1f)
    // 8 色循环：与 RainbowHourColors 一致，给每行 8-bit 像素感
    val barColor = RainbowHourColors[bucket.hour % RainbowHourColors.size]
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 小时标签：固定宽度 48dp，2 位 24h 制，如 "14:00"
        Text(
            text = String.format("%02d:00", bucket.hour),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(48.dp)
        )
        Spacer(Modifier.width(8.dp))
        // 像素方块条：weight(1f) 填充剩余空间，所有行总长度一致
        // 画法复用 TimeAllocationTab：外 Box 2dp 黑边白底，内 Box fillMaxWidth(fraction) 着色
        Box(
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .background(SurfaceColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .height(14.dp)
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        // 时长 + 事件数：固定宽度右对齐，避免长度不一挤压柱状图
        Text(
            text = "${bucket.eventCount} 事件 · ${formatDuration(bucket.durationMs)}",
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(124.dp),
            textAlign = TextAlign.End
        )
    }
}

/** 格式化时长为 "Xh Ym"，hours=0 时只显示分（与 TimeAllocationTab.formatAllocationDuration 同款）。 */
private fun formatDuration(totalMs: Long): String {
    val totalMin = totalMs / 60_000
    val hours = totalMin / 60
    val mins = totalMin % 60
    return if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
}
