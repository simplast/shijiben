package com.shijiben.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 注意：8-bit 风格用直角，所以 Shapes 全部用 0 dp 圆角

private val PixelShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp)
)

private val PixelTypography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,  // 等宽数字感
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

private val PixelColorScheme = lightColorScheme(
    primary = PixelIndigo,
    onPrimary = PixelBackground,
    secondary = PixelGold,
    onSecondary = PixelText,
    tertiary = PixelPink,
    background = PixelBackground,
    onBackground = PixelText,
    surface = PixelSurface,
    onSurface = PixelText,
    error = PixelRed,
    onError = PixelBackground,
    outline = PixelBorder
)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,  // 8-bit 风格固定浅色
    content: @Composable () -> Unit
) {
    // 强制浅色，8-bit 复古风格不切换深色
    MaterialTheme(
        colorScheme = PixelColorScheme,
        typography = PixelTypography,
        shapes = PixelShapes,
        content = content
    )
}
