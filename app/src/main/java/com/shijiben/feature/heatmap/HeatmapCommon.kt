package com.shijiben.feature.heatmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.ui.theme.Disabled
import com.shijiben.ui.theme.DisabledText
import com.shijiben.ui.theme.HeatmapLevel0
import com.shijiben.ui.theme.HeatmapLevel1
import com.shijiben.ui.theme.HeatmapLevel2
import com.shijiben.ui.theme.HeatmapLevel3
import com.shijiben.ui.theme.HeatmapLevel4
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextTertiary

/**
 * 热力图共享 UI 元素：Legend（5 档色阶图例）+ PixelArrowBox（26dp 黑边箭头按钮）。
 *
 * 此前 HeatmapMonthTab 与 HeatmapYearTab 各有一份逐字相同的副本，违反 DRY，
 * 修改一处（如调整色阶、改色块大小）需同步改两处，易漂移。集中到本文件后单点维护。
 */

/** 5 档绿色色阶图例：少 → 多。月视图与年视图共用。 */
@Composable
internal fun HeatmapLegend() {
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
                    .background(c)
            )
            Spacer(Modifier.width(4.dp))
        }
        Spacer(Modifier.width(2.dp))
        Text(text = "多", fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Bold)
    }
}

/**
 * 26dp 像素风箭头按钮：2dp 黑边白底（disabled 时灰底），居中 14dp 箭头图标。
 * 月/年视图的"上一页/下一页"导航共用。
 */
@Composable
internal fun PixelArrowBox(
    onClick: () -> Unit,
    enabled: Boolean,
    arrow: ImageVector,
    contentDescription: String
) {
    Box(
        modifier = Modifier
            .size(26.dp)
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
