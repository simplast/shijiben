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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
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
import com.shijiben.ui.theme.Border
import com.shijiben.ui.theme.Disabled
import com.shijiben.ui.theme.DisabledText
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
fun HeatmapMonthTab(
    onDateClick: (Triple<Int, Int, Int>) -> Unit,
    viewModel: HeatmapViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        MonthSwitcher(
            yearMonth = state.yearMonth,
            canGoNext = state.canGoNext,
            isCurrentMonth = state.isCurrentMonth,
            onPrevious = viewModel::previousMonth,
            onNext = viewModel::nextMonth,
            onGoCurrent = viewModel::goToCurrentMonth
        )
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

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

@Composable
private fun MonthSwitcher(
    yearMonth: java.time.YearMonth,
    canGoNext: Boolean,
    isCurrentMonth: Boolean,
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
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        PixelArrowBox(
            onClick = onNext,
            enabled = canGoNext,
            arrow = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "下一月"
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .border(2.dp, if (isCurrentMonth) Disabled else Primary)
                .background(Color.Transparent)
                .clickable(enabled = !isCurrentMonth, onClick = onGoCurrent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "本月",
                color = if (isCurrentMonth) DisabledText else Primary,
                fontSize = 12.sp,
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
            .background(if (enabled) SurfaceColor else Disabled)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            arrow,
            contentDescription = contentDescription,
            tint = if (enabled) Color.Black else DisabledText,
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
                    fontSize = 12.sp,
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
        Text(
            text = cell.date.dayOfMonth.toString(),
            fontSize = 12.sp,
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
        Text(text = "少", fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
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
        Text(text = "多", fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
    }
}
