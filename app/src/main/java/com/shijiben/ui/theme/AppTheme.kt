package com.shijiben.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.R

// 8-bit 像素字体：Fusion Pixel Font（SIL OFL 1.1 开源协议，可免费商用）
private val PixelFont = FontFamily(
    Font(R.font.fusion_pixel_12px_proportional_zh_hans),
    Font(R.font.fusion_pixel_12px_proportional_latin)
)

// 等宽像素字体
private val PixelMonoFont = FontFamily(
    Font(R.font.fusion_pixel_12px_proportional_zh_hans),
    Font(R.font.fusion_pixel_12px_proportional_latin)
)

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
        fontFamily = PixelFont,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    titleLarge = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    titleMedium = TextStyle(
        fontFamily = PixelFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = PixelMonoFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    ),
    labelMedium = TextStyle(
        fontFamily = PixelMonoFont,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    )
)

private val PixelColorScheme = lightColorScheme(
    primary = Primary,
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
