package com.doer.shijiben.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// Pixel Icon Library (8x8 Grid Pixel Art)
// ============================================================
// All icons are drawn as 8x8 pixel grids using Canvas drawRect.
// Each icon is defined as an 8x8 Boolean array where:
//   true = filled pixel block
//   false = empty pixel
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

// ── Pixel Grid Drawing Utility ──

@Composable
private fun PixelGrid(
    grid: Array<BooleanArray>,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    Canvas(modifier = modifier.size(size)) {
        val pixelSize = size.toPx() / 8f
        for (row in 0..7) {
            for (col in 0..7) {
                if (grid[row][col]) {
                    drawRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(
                            col * pixelSize,
                            row * pixelSize
                        ),
                        size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize)
                    )
                }
            }
        }
    }
}

// ── Play (right-pointing triangle) ──

@Composable
fun PixelPlayIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, true,  false, false, false, false, false, false),
        booleanArrayOf(false, true,  true,  false, false, false, false, false),
        booleanArrayOf(false, true,  true,  true,  false, false, false, false),
        booleanArrayOf(false, true,  true,  true,  true,  false, false, false),
        booleanArrayOf(false, true,  true,  true,  false, false, false, false),
        booleanArrayOf(false, true,  true,  false, false, false, false, false),
        booleanArrayOf(false, true,  false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Stop (solid square) ──

@Composable
fun PixelStopIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Close / Delete (X shape) ──

@Composable
fun PixelCloseIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    val grid = arrayOf(
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(false, false, true,  false, false, true,  false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, true,  false, false, true,  false, false),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Check (tick / fold line) ──

@Composable
fun PixelCheckIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, true,  false),
        booleanArrayOf(false, false, false, false, false, true,  true,  false),
        booleanArrayOf(false, false, false, false, true,  true,  false, false),
        booleanArrayOf(true,  true,  true,  true,  true,  false, false, false),
        booleanArrayOf(true,  true,  true,  true,  false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Add (plus sign) ──

@Composable
fun PixelAddIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── More (three vertical dots) ──

@Composable
fun PixelMoreIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Pause (two vertical bars) ──

@Composable
fun PixelPauseIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Refresh (circular arrow) ──

@Composable
fun PixelRefreshIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(true,  false, false, false, false, false, true,  true ),
        booleanArrayOf(true,  false, false, false, false, true,  true,  false),
        booleanArrayOf(true,  false, false, false, false, false, false, false),
        booleanArrayOf(false, true,  false, false, false, false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Calendar (square + horizontal lines) ──

@Composable
fun PixelCalendarIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    strokeWidth: Dp = 2.dp,
) {
    val grid = arrayOf(
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(true,  false, true,  false, true,  false, false, true ),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(true,  false, true,  false, true,  false, false, true ),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Star (5-point star) ──

@Composable
fun PixelStarIcon(
    modifier: Modifier = Modifier,
    color: Color = SunYellow,
    size: Dp = PixelIconSize.Medium,
    filled: Boolean = true,
    strokeWidth: Dp = 2.dp,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(false, true,  true,  true,  true,  true,  true,  false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(true,  true,  false, false, false, false, true,  true ),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ── Stats / Chart (3 vertical bars) ──

@Composable
fun PixelStatsIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
    barWidth: Dp = 3.dp,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, true,  false, false, false, false),
        booleanArrayOf(false, false, false, true,  false, false, false, false),
        booleanArrayOf(false, false, false, true,  false, true,  false, false),
        booleanArrayOf(true,  false, false, true,  false, true,  false, false),
        booleanArrayOf(true,  false, false, true,  false, true,  false, true ),
        booleanArrayOf(true,  true,  false, true,  true,  true,  false, true ),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
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
    val grid = arrayOf(
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(true,  true,  false, false, false, false, true,  true ),
        booleanArrayOf(true,  true,  false, false, false, false, true,  true ),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}
