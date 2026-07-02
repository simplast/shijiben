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
import java.time.Year

@Composable
fun HeatmapYearTab(
    onDateClick: (Triple<Int, Int, Int>) -> Unit,
    viewModel: HeatmapYearViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxWidth()) {
        YearSwitcher(
            year = state.year,
            canGoNext = state.canGoNext,
            isCurrentYear = state.isCurrentYear,
            onPrevious = viewModel::previousYear,
            onNext = viewModel::nextYear,
            onGoCurrent = viewModel::goToCurrentYear
        )
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
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
            HeatmapLegend()
            Spacer(Modifier.height(8.dp))
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
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        PixelArrowBox(
            onClick = onNext,
            enabled = canGoNext,
            arrow = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "下一年"
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .border(2.dp, if (isCurrentYear) Disabled else Primary)
                .background(Color.Transparent)
                .clickable(enabled = !isCurrentYear, onClick = onGoCurrent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "今年",
                color = if (isCurrentYear) DisabledText else Primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MiniMonth(monthGrid: HeatmapCalculator.MonthGrid) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = monthGrid.monthLabel,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(2.dp))
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
    if (!cell.isInMonth) return
    val levelColor = when (cell.level) {
        0 -> HeatmapLevel0
        1 -> HeatmapLevel1
        2 -> HeatmapLevel2
        3 -> HeatmapLevel3
        else -> HeatmapLevel4
    }
    val borderColor = when {
        cell.isToday -> Primary
        else -> Color.Black
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(1.dp, borderColor)
            .background(levelColor)
            .let { base ->
                if (cell.isFuture) base.alpha(0.5f) else base
            }
    )
}

