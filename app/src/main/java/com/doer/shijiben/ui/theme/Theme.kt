package com.doer.shijiben.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightWarmColors = lightColorScheme(
    primary = PrimaryGold,
    onPrimary = TextOnPrimary,
    primaryContainer = PrimaryGoldSoft,
    onPrimaryContainer = PrimaryGoldDeep,
    secondary = AccentMint,
    onSecondary = Color.White,
    secondaryContainer = PrimaryGoldSoft,
    onSecondaryContainer = PrimaryGoldDark,
    tertiary = AccentLavender,
    onTertiary = Color.White,
    background = WarmGray50,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = WarmGray100,
    onSurfaceVariant = WarmGray500,
    outline = WarmGray300,
    outlineVariant = WarmGray200,
    error = AccentCoral,
    onError = Color.White,
    errorContainer = PrimaryGoldSoft,
    onErrorContainer = AccentCoral,
)

private val DarkWarmColors = darkColorScheme(
    primary = PrimaryGold,
    onPrimary = Color(0xFF1A1A2E),
    primaryContainer = PrimaryGoldDark,
    onPrimaryContainer = PrimaryGoldSoft,
    secondary = AccentMint,
    onSecondary = Color(0xFF1A1A2E),
    secondaryContainer = Color(0xFF1A3D36),
    onSecondaryContainer = AccentMint,
    tertiary = AccentLavender,
    onTertiary = Color(0xFF1A1A2E),
    background = DarkBgWarm,
    onBackground = DarkTextWarm,
    surface = DarkSurfaceWarm,
    onSurface = DarkTextWarm,
    surfaceVariant = DarkMutedWarm,
    onSurfaceVariant = DarkTextWarm.copy(alpha = 0.7f),
    outline = WarmGray600,
    outlineVariant = DarkMutedWarm,
    error = AccentCoral,
    onError = Color.White,
    errorContainer = Color(0xFF3D1A14),
    onErrorContainer = PrimaryGoldSoft,
)

@Composable
fun ShijibenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightWarmColors,
        typography = Typography,
        content = content,
    )
}
