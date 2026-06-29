package com.shijiben.feature.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.HeatmapLevel0
import com.shijiben.ui.theme.HeatmapLevel1
import com.shijiben.ui.theme.HeatmapLevel2
import com.shijiben.ui.theme.HeatmapLevel3
import com.shijiben.ui.theme.HeatmapLevel4
import com.shijiben.ui.theme.Primary
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.RainbowTrim
import com.shijiben.ui.theme.TextTertiary
import java.time.Year

@Composable
fun HeatmapYearScreen(
    onBack: () -> Unit,
    viewModel: HeatmapYearViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxWidth().background(Background)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 顶部 8dp 彩虹条
            RainbowTrim()
            // 顶栏：左返回 + 标题「年度回看」
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
                    text = "年度回看",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 年份切换栏：‹ + 年 + › + 今年
            YearSwitcher(
                year = state.year,
                canGoNext = state.canGoNext,
                isCurrentYear = state.isCurrentYear,
                onPrevious = viewModel::previousYear,
                onNext = viewModel::nextYear,
                onGoCurrent = viewModel::goToCurrentYear
            )
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 主体（可滚动，防小屏挤压）：12 月 mini 月历拼贴 + Legend
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                // 4 行 × 3 列
                for (rowIdx in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (colIdx in 0 until 3) {
                            val monthIndex = rowIdx * 3 + colIdx
                            val monthGrid = state.months.getOrNull(monthIndex)
                            Box(modifier = Modifier.weight(1f)) {
                                if (monthGrid != null) {
                                    MiniMonth(monthGrid = monthGrid)
                                }
                            }
                        }
                    }
                    if (rowIdx < 3) Spacer(Modifier.height(8.dp))
                }
                Spacer(Modifier.height(12.dp))
                Legend()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}


@Composable
private fun YearSwitcher(
    year: Year,
    canGoNext: Boolean,
    isCurrentYear: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoCurrent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ‹ 总可点
        PixelArrowBox(
            onClick = onPrevious,
            enabled = true,
            arrow = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = "上一年"
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${year.value}年",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        // › 仅 canGoNext=true 可点
        PixelArrowBox(
            onClick = onNext,
            enabled = canGoNext,
            arrow = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "下一年"
        )
        Spacer(Modifier.width(8.dp))
        // 今年按钮：与 MonthSwitcher 的"本月"按钮同范式
        Box(
            modifier = Modifier
                .border(
                    2.dp,
                    if (isCurrentYear) Color(0xFFCBD5E1) else Primary
                )
                .background(Color.Transparent)
                .clickable(enabled = !isCurrentYear, onClick = onGoCurrent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "今年",
                color = if (isCurrentYear) Color(0xFF94A3B8) else Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PixelArrowBox(
    onClick: () -> Unit,
    enabled: Boolean,
    arrow: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .border(2.dp, Color.Black)
            .background(if (enabled) SurfaceColor else Color(0xFFCBD5E1))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            arrow,
            contentDescription = contentDescription,
            tint = if (enabled) Color.Black else Color(0xFF94A3B8),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun MiniMonth(monthGrid: HeatmapCalculator.MonthGrid) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 月份标题："1月".."12月"
        Text(
            text = monthGrid.monthLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(2.dp))
        // 6×7 mini 网格
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            for (row in monthGrid.cells) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (cell in row) {
                        Box(modifier = Modifier.weight(1f)) {
                            MiniCell(cell)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniCell(cell: HeatmapCalculator.Cell) {
    // 补位方块：透明，不渲染
    if (!cell.isInMonth) return
    val levelColor = when (cell.level) {
        0 -> HeatmapLevel0
        1 -> HeatmapLevel1
        2 -> HeatmapLevel2
        3 -> HeatmapLevel3
        else -> HeatmapLevel4
    }
    val borderColor = when {
        cell.isToday -> Primary       // 今天红边框（与 DayCell 一致）
        else -> Color.Black           // 其余黑边框
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)          // 正方形，宽由父 weight 决定
            .border(1.dp, borderColor) // 1dp（格子小，2dp 过粗）
            .background(levelColor)
            .let { base ->
                // 未来日半透明（与 DayCell 一致，弱化未来）
                if (cell.isFuture) base.alpha(0.5f) else base
            }
    )
    // 不显示日期数字（格子约 14dp 放不下 9sp 数字）
    // 不可点击（年视图核心是总览）
}

@Composable
private fun Legend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "少", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        val colors = listOf(HeatmapLevel0, HeatmapLevel1, HeatmapLevel2, HeatmapLevel3, HeatmapLevel4)
        for (c in colors) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(2.dp, Color.Black)
                    .background(c)
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(2.dp))
        Text(text = "多", fontSize = 11.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
    }
}
