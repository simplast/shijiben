package com.doer.shijiben.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ============================================================
// Pixel Icon Library — 8x8 Grid Pixel Art
// ============================================================
// All icons are drawn as 8x8 pixel grids using Canvas drawRect.
// Each icon is an 8x8 Boolean array where true = filled pixel.
//
// Sizes:
//   Large  — 32dp (primary actions)
//   Medium — 24dp (list actions)
//   Small  — 16dp (decorations)
// ============================================================

object PixelIconSize {
    val Large = 32.dp
    val Medium = 24.dp
    val Small = 16.dp
}

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
                        topLeft = androidx.compose.ui.geometry.Offset(col * pixelSize, row * pixelSize),
                        size = androidx.compose.ui.geometry.Size(pixelSize, pixelSize),
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// Play — right-pointing triangle (redrawn)
// ═══════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════
// Stop — solid square
// ═══════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════
// Close / Delete — X shape (bolder)
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelCloseIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(false, true,  true,  false, false, true,  true,  false),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// Check — tick mark (redrawn symmetric)
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelCheckIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, true,  false),
        booleanArrayOf(false, false, false, false, false, true,  true,  false),
        booleanArrayOf(false, false, false, false, true,  true,  false, false),
        booleanArrayOf(true,  false, false, true,  true,  false, false, false),
        booleanArrayOf(true,  true,  true,  true,  false, false, false, false),
        booleanArrayOf(true,  true,  true,  false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// Add — plus sign
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelAddIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, true,  true,  true,  true,  true,  true,  false),
        booleanArrayOf(false, true,  true,  true,  true,  true,  true,  false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// More — three horizontal dots
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelMoreIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, true,  true,  false, true,  true,  false, false),
        booleanArrayOf(false, true,  true,  false, true,  true,  false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, true,  true,  false, true,  true,  false, false),
        booleanArrayOf(false, true,  true,  false, true,  true,  false, false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// Pause — two vertical bars
// ═══════════════════════════════════════════════════════════

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

// ═══════════════════════════════════════════════════════════
// Refresh — circular arrow (redrawn)
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelRefreshIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  false, false, false, true,  false, false),
        booleanArrayOf(true,  false, false, false, false, false, true,  false),
        booleanArrayOf(true,  false, false, false, true,  true,  true,  false),
        booleanArrayOf(true,  false, false, true,  true,  false, false, false),
        booleanArrayOf(true,  false, false, false, false, false, false, false),
        booleanArrayOf(false, true,  false, false, false, false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// Calendar — simplified date icon
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelCalendarIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(true,  false, true,  false, false, true,  false, true ),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(true,  false, true,  false, false, true,  false, true ),
        booleanArrayOf(true,  false, false, false, false, false, false, true ),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// Star — simplified 4-point star (redrawn)
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelStarIcon(
    modifier: Modifier = Modifier,
    color: Color = PixelYellow,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  true,  true,  true,  true,  true,  false),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(false, true,  true,  true,  true,  true,  true,  false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// Stats — three vertical bars
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelStatsIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, false, false, false),
        booleanArrayOf(false, false, false, true,  false, false, false, false),
        booleanArrayOf(false, false, false, true,  false, true,  false, false),
        booleanArrayOf(false, true,  false, true,  false, true,  false, false),
        booleanArrayOf(false, true,  false, true,  false, true,  false, true ),
        booleanArrayOf(false, true,  true,  true,  true,  true,  false, true ),
        booleanArrayOf(false, true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// NEW: Clock — time-related UI
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelClockIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(true,  false, false, true,  false, false, false, true ),
        booleanArrayOf(true,  false, false, true,  false, false, false, true ),
        booleanArrayOf(true,  false, false, true,  false, false, false, true ),
        booleanArrayOf(true,  false, false, false, true,  false, false, true ),
        booleanArrayOf(false, true,  false, false, false, true,  true,  false),
        booleanArrayOf(false, false, true,  true,  true,  false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// NEW: Trash — dedicated delete icon
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelTrashIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  true ),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(false, true,  false, true,  true,  false, true,  false),
        booleanArrayOf(false, true,  false, true,  true,  false, true,  false),
        booleanArrayOf(false, true,  false, false, false, false, true,  false),
        booleanArrayOf(false, false, true,  true,  true,  true,  false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}

// ═══════════════════════════════════════════════════════════
// NEW: Edit — pencil icon
// ═══════════════════════════════════════════════════════════

@Composable
fun PixelEditIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = PixelIconSize.Medium,
) {
    val grid = arrayOf(
        booleanArrayOf(false, false, false, false, false, true,  true,  false),
        booleanArrayOf(false, false, false, false, true,  true,  false, false),
        booleanArrayOf(false, false, false, true,  true,  false, false, false),
        booleanArrayOf(false, false, true,  true,  false, false, false, false),
        booleanArrayOf(false, true,  true,  false, false, false, false, false),
        booleanArrayOf(true,  true,  false, false, false, false, false, false),
        booleanArrayOf(true,  true,  true,  true,  true,  true,  true,  false),
        booleanArrayOf(false, false, false, false, false, false, false, false),
    )
    PixelGrid(grid = grid, modifier = modifier, color = color, size = size)
}
