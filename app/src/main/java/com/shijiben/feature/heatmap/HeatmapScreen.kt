package com.shijiben.feature.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.Border
import com.shijiben.ui.theme.HeatmapLevel0
import com.shijiben.ui.theme.HeatmapLevel1
import com.shijiben.ui.theme.HeatmapLevel2
import com.shijiben.ui.theme.HeatmapLevel3
import com.shijiben.ui.theme.HeatmapLevel4
import com.shijiben.ui.theme.Primary
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextTertiary

@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    onDateClick: (Triple<Int, Int, Int>) -> Unit,
    onYearClick: () -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxWidth().background(Background)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 顶部 8dp 彩虹条
            RainbowTrim()
            // 顶栏：左返回 + 标题「回看」
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
                    text = "回看",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 月份切换栏：‹ + 年月 + › + 本月 + 年
            MonthSwitcher(
                yearMonth = state.yearMonth,
                canGoNext = state.canGoNext,
                isCurrentMonth = state.isCurrentMonth,
                onPrevious = viewModel::previousMonth,
                onNext = viewModel::nextMonth,
                onGoCurrent = viewModel::goToCurrentMonth,
                onYearClick = onYearClick
            )
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 主体（可滚动，防小屏挤压）
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            ) {
                WeekHeader()
                Spacer(Modifier.height(4.dp))
                HeatmapGrid(cells = state.cells, onDateClick = onDateClick)
                Spacer(Modifier.height(12.dp))
                Legend()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/** 顶部 8dp 彩虹条（与首页/timeviz 一致，复制 8 行避免改 PixelComponents）。 */
@Composable
private fun RainbowTrim() {
    val trimColors = listOf(
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
        Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
        Color(0xFF6366F1), Color(0xFFA855F7)
    )
    Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
        for (i in 0 until 80) {
            Box(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(trimColors[i % 8])
            )
        }
    }
}

@Composable
private fun MonthSwitcher(
    yearMonth: java.time.YearMonth,
    canGoNext: Boolean,
    isCurrentMonth: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoCurrent: () -> Unit,
    onYearClick: () -> Unit
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
            contentDescription = "上一月"
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${yearMonth.year}年${yearMonth.monthValue}月",
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
            contentDescription = "下一月"
        )
        Spacer(Modifier.width(8.dp))
        // 本月按钮：PixelOutlinedButton 风但小号
        Box(
            modifier = Modifier
                .border(
                    2.dp,
                    if (isCurrentMonth) Color(0xFFCBD5E1) else Primary
                )
                .background(Color.Transparent)
                .clickable(enabled = !isCurrentMonth, onClick = onGoCurrent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "本月",
                color = if (isCurrentMonth) Color(0xFF94A3B8) else Primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        // 年跳转按钮：与"本月"同范式，始终可点（年视图总是可达）
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .border(2.dp, Primary)
                .background(Color.Transparent)
                .clickable(onClick = onYearClick)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "年",
                color = Primary,
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
    arrow: androidx.compose.ui.graphics.vector.ImageVector,
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
private fun WeekHeader() {
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
    Row(modifier = Modifier.fillMaxWidth()) {
        for (label in labels) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextTertiary
                )
            }
        }
    }
}

@Composable
private fun HeatmapGrid(
    cells: List<List<HeatmapCalculator.Cell>>,
    onDateClick: (Triple<Int, Int, Int>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in cells) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (cell in row) {
                    Box(modifier = Modifier.weight(1f)) {
                        DayCell(cell = cell, onDateClick = onDateClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    cell: HeatmapCalculator.Cell,
    onDateClick: (Triple<Int, Int, Int>) -> Unit
) {
    // 补位方块：透明背景，不可点
    if (!cell.isInMonth) {
        Box(modifier = Modifier.size(40.dp))
        return
    }
    val levelColor = when (cell.level) {
        0 -> HeatmapLevel0
        1 -> HeatmapLevel1
        2 -> HeatmapLevel2
        3 -> HeatmapLevel3
        else -> HeatmapLevel4
    }
    val isFuture = cell.isFuture
    val borderColor = when {
        cell.isToday -> Primary
        isFuture -> Border
        else -> Color.Black
    }
    val cellModifier = Modifier
        .size(40.dp)
        .border(2.dp, borderColor)
        .background(levelColor)
        .let { base ->
            if (isFuture) base.alpha(0.5f)
            else base.clickable { onDateClick(Triple(cell.date.year, cell.date.monthValue, cell.date.dayOfMonth)) }
        }
    Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
        // 日期数字：浅背景用浅灰字（TextTertiary），深背景 level 2-4 用白字反色
        Text(
            text = cell.date.dayOfMonth.toString(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = when (cell.level) {
                0, 1 -> TextTertiary
                else -> Color.White
            }
        )
    }
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
