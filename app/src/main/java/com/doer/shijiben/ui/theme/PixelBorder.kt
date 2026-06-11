package com.doer.shijiben.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// Double-line Pixel Border System
// ============================================================
// Outer border (main color) + inner border (highlight color)
// Creates a "retro game UI" feel without shadows
// ============================================================

/**
 * Draws a double-line (two-layer) pixel border.
 *
 * @param outerColor 外线颜色（主色）
 * @param innerColor 内线颜色（高光色）
 * @param outerWidth 外线宽度
 * @param innerWidth 内线宽度
 */
fun Modifier.doublePixelBorder(
    outerColor: Color,
    innerColor: Color = Color.White,
    outerWidth: Dp = 2.dp,
    innerWidth: Dp = 1.dp,
): Modifier = this.drawBehind {
    val outerPx = outerWidth.toPx()
    val innerPx = innerWidth.toPx()

    // 外层边框 —— 紧贴组件边缘
    drawRect(
        brush = SolidColor(outerColor),
        style = Stroke(width = outerPx)
    )

    // 内层边框 —— 向内偏移外线宽度
    val inset = outerPx
    drawRect(
        brush = SolidColor(innerColor),
        topLeft = Offset(inset, inset),
        size = Size(
            width = size.width - inset * 2,
            height = size.height - inset * 2
        ),
        style = Stroke(width = innerPx)
    )
}

// ============================================================
// Three-Level Border Constants
// ============================================================

/** Primary level (2dp outer + 1dp inner) — 高突出：概览卡、进行中卡、对话框 */
object PixelBorderPrimary {
    val outerWidth = 2.dp
    val innerWidth = 1.dp
    val outerColor = DeepTeal
    val innerColor = Color.White
}

/** Secondary level (1.5dp outer + 0.5dp inner) — 内容区：列表区、卡片组 */
object PixelBorderSecondary {
    val outerWidth = 1.5.dp
    val innerWidth = 0.5.dp
    val outerColor = SeaBlue
    val innerColor = MistBlue
}

/** Tertiary level (1dp outer + 0.5dp inner) — 内嵌：按钮、标签、徽章 */
object PixelBorderTertiary {
    val outerWidth = 1.dp
    val innerWidth = 0.5.dp
    val outerColor = DeepTeal
    val innerColor = Color.White
}

// ============================================================
// Convenience shortcuts
// ============================================================

fun Modifier.primaryDoubleBorder(
    outerColor: Color = PixelBorderPrimary.outerColor,
    innerColor: Color = PixelBorderPrimary.innerColor,
): Modifier = this.doublePixelBorder(
    outerColor = outerColor,
    innerColor = innerColor,
    outerWidth = PixelBorderPrimary.outerWidth,
    innerWidth = PixelBorderPrimary.innerWidth,
)

fun Modifier.secondaryDoubleBorder(
    outerColor: Color = PixelBorderSecondary.outerColor,
    innerColor: Color = PixelBorderSecondary.innerColor,
): Modifier = this.doublePixelBorder(
    outerColor = outerColor,
    innerColor = innerColor,
    outerWidth = PixelBorderSecondary.outerWidth,
    innerWidth = PixelBorderSecondary.innerWidth,
)

fun Modifier.tertiaryDoubleBorder(
    outerColor: Color = PixelBorderTertiary.outerColor,
    innerColor: Color = PixelBorderTertiary.innerColor,
): Modifier = this.doublePixelBorder(
    outerColor = outerColor,
    innerColor = innerColor,
    outerWidth = PixelBorderTertiary.outerWidth,
    innerWidth = PixelBorderTertiary.innerWidth,
)
