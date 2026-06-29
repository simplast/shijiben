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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.Primary
import com.shijiben.ui.theme.RainbowTrim
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary

@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    onDateClick: (Triple<Int, Int, Int>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0=月, 1=年, 2=去向

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

            // tab 切换栏：月 / 年 / 去向
            TabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // tab 内容
            when (selectedTab) {
                0 -> HeatmapMonthTab(onDateClick = onDateClick)
                1 -> HeatmapYearTab(onDateClick = onDateClick)
                2 -> TimeAllocationTab()
            }
        }
    }
}

@Composable
private fun TabBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabs = listOf("月", "年", "去向")
    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        for (i in tabs.indices) {
            val isSelected = i == selectedTab
            Box(
                modifier = Modifier
                    .weight(1f)
                    .border(2.dp, if (isSelected) Primary else Color.Black)
                    .background(if (isSelected) Primary else SurfaceColor)
                    .clickable { onTabSelected(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tabs[i],
                    color = if (isSelected) Color.White else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
