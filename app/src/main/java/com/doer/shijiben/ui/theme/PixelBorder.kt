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
// Pixel-Dashed Border System
// ============================================================
// Draws alternating colored/transparent blocks around the
// component perimeter, creating the iconic "pixel outline"
// look from NES / Game Boy games.
//
// Three levels:
//   Primary   (3dp) — Hero cards (overview, active)
//   Secondary (2dp) — Content cards (completed, pending)
//   Tertiary  (1dp) — Inline elements (buttons, badges, inputs)
// ============================================================

/**
 * Draws a pixel-dashed border by rendering alternating colored
 * and transparent segments along the component's perimeter.
 *
 * @param color  Border color
 * @param width  Border line width
 * @param dashSize  Length of each colored pixel block
 * @param gapSize   Length of each transparent gap
 */
fun Modifier.pixelBorder(
    color: Color,
    width: Dp = 2.dp,
    dashSize: Dp = 4.dp,
    gapSize: Dp = 2.dp,
): Modifier = this.drawBehind {
    val w = width.toPx()
    val dash = dashSize.toPx()
    val gap = gapSize.toPx()
    val segment = dash + gap
    val brush = SolidColor(color)

    val totalW = size.width
    val totalH = size.height

    // ── Top edge (left to right) ──
    var x = 0f
    while (x < totalW) {
        val drawLen = minOf(dash, totalW - x)
        drawLine(
            brush = brush,
            start = Offset(x, 0f),
            end = Offset(x + drawLen, 0f),
            strokeWidth = w,
        )
        x += segment
    }

    // ── Bottom edge (left to right) ──
    x = 0f
    while (x < totalW) {
        val drawLen = minOf(dash, totalW - x)
        drawLine(
            brush = brush,
            start = Offset(x, totalH),
            end = Offset(x + drawLen, totalH),
            strokeWidth = w,
        )
        x += segment
    }

    // ── Left edge (top to bottom) ──
    var y = 0f
    while (y < totalH) {
        val drawLen = minOf(dash, totalH - y)
        drawLine(
            brush = brush,
            start = Offset(0f, y),
            end = Offset(0f, y + drawLen),
            strokeWidth = w,
        )
        y += segment
    }

    // ── Right edge (top to bottom) ──
    y = 0f
    while (y < totalH) {
        val drawLen = minOf(dash, totalH - y)
        drawLine(
            brush = brush,
            start = Offset(totalW, y),
            end = Offset(totalW, y + drawLen),
            strokeWidth = w,
        )
        y += segment
    }
}

// ============================================================
// Convenience Shortcuts
// ============================================================

/** 3dp border — hero cards (overview, active event) */
fun Modifier.pixelBorderPrimary(color: Color = PixelBorder): Modifier =
    this.pixelBorder(color = color, width = 3.dp, dashSize = 4.dp, gapSize = 2.dp)

/** 2dp border — content cards (completed section, pending section) */
fun Modifier.pixelBorderSecondary(color: Color = PixelBorder): Modifier =
    this.pixelBorder(color = color, width = 2.dp, dashSize = 3.dp, gapSize = 2.dp)

/** 1dp border — inline elements (buttons, badges, inputs) */
fun Modifier.pixelBorderTertiary(color: Color = PixelBorder): Modifier =
    this.pixelBorder(color = color, width = 1.dp, dashSize = 2.dp, gapSize = 2.dp)
