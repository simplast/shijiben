package com.doer.shijiben.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// Pixel Icon Library (Canvas-drawn geometric icons)
// ============================================================
// All icons are drawn as simple geometric shapes for a consistent
// 8-bit pixel aesthetic. No external icon dependencies.
//
// Size tokens:
//   Large  — 32dp (primary action buttons)
//   Medium — 24dp (list actions, secondary buttons)
//   Small  — 16dp (decorations, badges)
// ============================================================

object PixelIconSize {
    val Large = 32.dp
    val Medium = 24.dp
    val Small = 16.dp
}

// ── Play (right-pointing triangle) ──

@Composable
fun PixelPlayIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.toPx(), size.toPx() / 2f)
            lineTo(0f, size.toPx())
            close()
        }
        drawPath(path, color)
    }
}

// ── Stop (solid square) ──

@Composable
fun PixelStopIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    Canvas(modifier = modifier.size(size)) {
        val inset = size.toPx() * 0.2f
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.toPx() - inset * 2, size.toPx() - inset * 2)
        )
    }
}

// ── Close / Delete (X shape) ──

@Composable
fun PixelCloseIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val inset = size.toPx() * 0.2f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(inset, inset),
            end = androidx.compose.ui.geometry.Offset(size.toPx() - inset, size.toPx() - inset),
            strokeWidth = strokePx
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(size.toPx() - inset, inset),
            end = androidx.compose.ui.geometry.Offset(inset, size.toPx() - inset),
            strokeWidth = strokePx
        )
    }
}

// ── Check (tick / fold line) ──

@Composable
fun PixelCheckIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val s = size.toPx()
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(s * 0.2f, s * 0.5f),
            end = androidx.compose.ui.geometry.Offset(s * 0.42f, s * 0.72f),
            strokeWidth = strokePx
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(s * 0.42f, s * 0.72f),
            end = androidx.compose.ui.geometry.Offset(s * 0.8f, s * 0.28f),
            strokeWidth = strokePx
        )
    }
}

// ── Add (plus sign) ──

@Composable
fun PixelAddIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val center = size.toPx() / 2f
        val armLength = size.toPx() * 0.3f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(center - armLength, center),
            end = androidx.compose.ui.geometry.Offset(center + armLength, center),
            strokeWidth = strokePx
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(center, center - armLength),
            end = androidx.compose.ui.geometry.Offset(center, center + armLength),
            strokeWidth = strokePx
        )
    }
}

// ── More (three vertical dots) ──

@Composable
fun PixelMoreIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    Canvas(modifier = modifier.size(size)) {
        val dotSize = size.toPx() * 0.18f
        val centerX = size.toPx() / 2f
        val spacing = size.toPx() / 4f
        for (i in 0..2) {
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(
                    centerX - dotSize / 2,
                    spacing + i * spacing - dotSize / 2
                ),
                size = androidx.compose.ui.geometry.Size(dotSize, dotSize)
            )
        }
    }
}

// ── Pause (two vertical bars) ──

@Composable
fun PixelPauseIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    Canvas(modifier = modifier.size(size)) {
        val barWidth = size.toPx() * 0.2f
        val barHeight = size.toPx() * 0.6f
        val offsetY = (size.toPx() - barHeight) / 2f
        val gap = size.toPx() * 0.2f
        val centerX = size.toPx() / 2f
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(centerX - gap / 2 - barWidth, offsetY),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(centerX + gap / 2, offsetY),
            size = androidx.compose.ui.geometry.Size(barWidth, barHeight)
        )
    }
}

// ── Diamond (hollow diamond) ──

@Composable
fun PixelDiamondIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    filled: Boolean = false,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val path = Path().apply {
            val half = size.toPx() / 2f
            moveTo(half, 0f)
            lineTo(size.toPx(), half)
            lineTo(half, size.toPx())
            lineTo(0f, half)
            close()
        }
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(width = strokeWidth.toPx()))
        }
    }
}

// ── Refresh (circular arrow, simplified) ──

@Composable
fun PixelRefreshIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val strokePx = strokeWidth.toPx()
        val cx = s / 2f
        val cy = s / 2f
        val r = s * 0.35f

        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx - r * 0.7f, cy - r),
            end = androidx.compose.ui.geometry.Offset(cx + r, cy - r * 0.5f),
            strokeWidth = strokePx
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx + r, cy - r * 0.5f),
            end = androidx.compose.ui.geometry.Offset(cx + r * 0.8f, cy + r * 0.6f),
            strokeWidth = strokePx
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(cx + r * 0.8f, cy + r * 0.6f),
            end = androidx.compose.ui.geometry.Offset(cx - r * 0.3f, cy + r * 0.7f),
            strokeWidth = strokePx
        )
        // 箭头（左下方向）
        val arrowX = cx - r * 0.3f
        val arrowY = cy + r * 0.7f
        val arrowSize = s * 0.15f
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(arrowX, arrowY),
            end = androidx.compose.ui.geometry.Offset(arrowX - arrowSize, arrowY - arrowSize * 0.5f),
            strokeWidth = strokePx
        )
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(arrowX, arrowY),
            end = androidx.compose.ui.geometry.Offset(arrowX - arrowSize * 0.3f, arrowY + arrowSize * 0.7f),
            strokeWidth = strokePx
        )
    }
}

// ── Calendar (square + horizontal lines) ──

@Composable
fun PixelCalendarIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokePx = strokeWidth.toPx()
        val s = size.toPx()
        val inset = s * 0.15f

        // 外框
        drawRect(
            color = color,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset + s * 0.1f),
            size = androidx.compose.ui.geometry.Size(s - inset * 2, s - inset * 2 - s * 0.1f),
            style = Stroke(width = strokePx)
        )
        // 顶部横条（日历头）
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(inset, inset + s * 0.3f),
            end = androidx.compose.ui.geometry.Offset(s - inset, inset + s * 0.3f),
            strokeWidth = strokePx
        )
        // 左边小耳朵
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(inset + s * 0.15f, inset + s * 0.1f),
            end = androidx.compose.ui.geometry.Offset(inset + s * 0.15f, inset),
            strokeWidth = strokePx
        )
        // 右边小耳朵
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(s - inset - s * 0.15f, inset + s * 0.1f),
            end = androidx.compose.ui.geometry.Offset(s - inset - s * 0.15f, inset),
            strokeWidth = strokePx
        )
    }
}

// ── Star (simplified 5-point star) ──

@Composable
fun PixelStarIcon(
    modifier: Modifier = Modifier,
    color: Color = SunYellow,
    size: Dp = PixelIconSize.Medium,
    filled: Boolean = true,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val cx = s / 2f
        val cy = s / 2f
        val outerR = s * 0.45f
        val innerR = s * 0.2f

        val path = Path()
        for (i in 0 until 10) {
            val angle = -Math.PI / 2.0 + i * Math.PI / 5.0
            val r = if (i % 2 == 0) outerR else innerR
            val x = cx + r * kotlin.math.cos(angle).toFloat()
            val y = cy + r * kotlin.math.sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()

        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(width = strokeWidth.toPx()))
        }
    }
}

// ── Stats / Chart (3 vertical bars) ──

@Composable
fun PixelStatsIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    barWidth: Dp = 3.dp,
) {
    Canvas(modifier = modifier.size(size)) {
        val s = size.toPx()
        val barW = barWidth.toPx()
        val gap = (s - barW * 3) / 4f
        val bottom = s - s * 0.1f

        // Three bars of varying heights — 70% / 100% / 45%
        val heights = listOf(0.7f, 1.0f, 0.45f)
        val topOffset = s * 0.1f

        for (i in 0..2) {
            val left = gap + i * (barW + gap)
            val h = heights[i] * (bottom - topOffset)
            drawRect(
                color = color,
                topLeft = androidx.compose.ui.geometry.Offset(left, bottom - h),
                size = androidx.compose.ui.geometry.Size(barW, h),
            )
        }
    }
}
