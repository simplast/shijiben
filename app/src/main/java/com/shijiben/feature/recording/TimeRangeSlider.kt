package com.shijiben.feature.recording

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.ui.theme.PixelBackground
import com.shijiben.ui.theme.PixelBorder
import com.shijiben.ui.theme.PixelGreen
import com.shijiben.ui.theme.PixelIndigo
import com.shijiben.ui.theme.PixelRed
import com.shijiben.ui.theme.PixelTextSecondary
import com.shijiben.ui.theme.pixelBorder

/**
 * 双行时间滑块：
 * - 第一行 0-12 小时（0-720 分钟）
 * - 第二行 12-24 小时（720-1440 分钟）
 * - 开始/结束两个手柄，可在两行间移动
 * - 内部状态用 0-1440 分钟（一天分钟数）
 */
@Composable
fun TimeRangeSlider(
    startMinutes: Int,
    endMinutes: Int,
    onMinutesChange: (start: Int, end: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 8-bit 风格滑块配色：靛蓝手柄/激活轨道，深灰淡化非激活轨道；刻度过密置透明
        val sliderColors = SliderDefaults.colors(
            thumbColor = PixelIndigo,
            activeTrackColor = PixelIndigo,
            inactiveTrackColor = PixelBorder.copy(alpha = 0.3f),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent
        )
        // 第一行 0-12h 视觉
        TimeRowVisual(
            startMin = startMinutes,
            endMin = endMinutes,
            rowStart = 0,
            rowEnd = 720,
            label = "上午 00:00 - 12:00"
        )
        Spacer(Modifier.height(6.dp))
        // 第二行 12-24h 视觉
        TimeRowVisual(
            startMin = startMinutes,
            endMin = endMinutes,
            rowStart = 720,
            rowEnd = 1440,
            label = "下午 12:00 - 24:00"
        )
        Spacer(Modifier.height(12.dp))
        // 开始时间滑块
        Text(
            text = "开始: ${formatMin(startMinutes)}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = PixelTextSecondary
        )
        Slider(
            value = startMinutes.toFloat(),
            onValueChange = { newStart ->
                val ns = newStart.toInt().coerceIn(0, endMinutes - 1)
                onMinutesChange(ns, endMinutes)
            },
            valueRange = 0f..1440f,
            steps = 1439
        )
        // 结束时间滑块
        Text(
            text = "结束: ${formatMin(endMinutes)}",
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = PixelTextSecondary
        )
        Slider(
            value = endMinutes.toFloat(),
            onValueChange = { newEnd ->
                val ne = newEnd.toInt().coerceIn(startMinutes + 1, 1440)
                onMinutesChange(startMinutes, ne)
            },
            valueRange = 0f..1440f,
            steps = 1439,
            colors = sliderColors
        )
    }
}

@Composable
private fun TimeRowVisual(
    startMin: Int,
    endMin: Int,
    rowStart: Int,
    rowEnd: Int,
    label: String
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = PixelTextSecondary
        )
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(PixelBackground)
                .pixelBorder()
        ) {
            val w = size.width
            val h = size.height
            val span = (rowEnd - rowStart).toFloat()
            // 选中区间
            val selStart = startMin.coerceIn(rowStart, rowEnd) - rowStart
            val selEnd = endMin.coerceIn(rowStart, rowEnd) - rowStart
            if (selEnd > selStart) {
                drawRect(
                    color = PixelIndigo,
                    topLeft = Offset(x = w * selStart / span, y = 0f),
                    size = Size(width = w * (selEnd - selStart) / span, height = h)
                )
            }
            // 开始手柄
            if (startMin in rowStart..rowEnd) {
                val x = w * (startMin - rowStart) / span
                drawRect(
                    color = PixelRed,
                    topLeft = Offset(x - 2f, 0f),
                    size = Size(4f, h)
                )
            }
            // 结束手柄
            if (endMin in rowStart..rowEnd) {
                val x = w * (endMin - rowStart) / span
                drawRect(
                    color = PixelGreen,
                    topLeft = Offset(x - 2f, 0f),
                    size = Size(4f, h)
                )
            }
        }
    }
}

private fun formatMin(min: Int): String {
    val h = min / 60
    val m = min % 60
    return "%02d:%02d".format(h, m)
}
