package com.doer.shijiben.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = SecondaryIndigo,
    onPrimary = Color.White,
    secondary = AccentPurple,
    onSecondary = Color.White,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurface.copy(alpha = 0.7f),
)

private val LightColors = lightColorScheme(
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    secondary = SecondaryIndigo,
    onSecondary = Color.White,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurface.copy(alpha = 0.6f),
    outlineVariant = LightMuted,
    primaryContainer = LightMuted,
    onPrimaryContainer = PrimaryIndigo,
)

@Composable
fun ShijibenTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = Typography,
        content = content,
    )
}
