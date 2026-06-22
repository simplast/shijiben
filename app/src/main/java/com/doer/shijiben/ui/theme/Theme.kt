package com.doer.shijiben.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ============================================================
// Pixel Theme — Light + Dark
// ============================================================

private val LightPixelColors = lightColorScheme(
    primary = PixelTeal,
    onPrimary = Color.White,
    primaryContainer = PixelMint,
    onPrimaryContainer = PixelBlack,
    secondary = PixelCoral,
    onSecondary = Color.White,
    secondaryContainer = PixelLavender,
    onSecondaryContainer = PixelBlack,
    tertiary = PixelAmber,
    onTertiary = PixelBlack,
    background = PixelCream,
    onBackground = PixelBlack,
    surface = PixelCream,
    onSurface = PixelBlack,
    surfaceVariant = PixelMint,
    onSurfaceVariant = PixelGray,
    outline = PixelGrayLight,
    outlineVariant = PixelGrayLight,
    error = PixelRed,
    onError = Color.White,
)

private val DarkPixelColors = darkColorScheme(
    primary = PixelTeal,
    onPrimary = PixelBlack,
    primaryContainer = PixelBlack,
    onPrimaryContainer = PixelTeal,
    secondary = PixelCoral,
    onSecondary = PixelBlack,
    secondaryContainer = PixelBlack,
    onSecondaryContainer = PixelCoral,
    tertiary = PixelAmber,
    onTertiary = PixelBlack,
    background = PixelBlack,
    onBackground = PixelCream,
    surface = Color(0xFF252540),
    onSurface = PixelCream,
    surfaceVariant = Color(0xFF2A2A45),
    onSurfaceVariant = PixelGray,
    outline = Color(0xFF3A3A55),
    outlineVariant = Color(0xFF2A2A45),
    error = PixelRed,
    onError = Color.White,
)

@Composable
fun ShijibenTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkPixelColors else LightPixelColors,
        typography = PixelTypography,
        content = content,
    )
}
