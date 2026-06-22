package com.shijiben.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 像素边框 Modifier：2dp 黑色直角边框 */
fun Modifier.pixelBorder(color: Color = PixelBorder, width: Dp = 2.dp): Modifier =
    this.border(width = width, color = color)

/** 像素硬阴影 Modifier：向右下偏移 4dp 的纯色块（无模糊） */
fun Modifier.pixelShadow(
    color: Color = PixelShadow,
    offsetX: Dp = 4.dp,
    offsetY: Dp = 4.dp
): Modifier = this.then(Modifier.offset(x = offsetX, y = offsetY).background(color))

/**
 * 像素卡片：直角 + 2dp 黑色边框 + 可选硬阴影
 * 实现方式：外层 Box 包裹阴影层 + Surface（带边框）+ content
 */
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = PixelSurface,
    borderColor: Color = PixelBorder,
    borderWidth: Dp = 2.dp,
    shadow: Boolean = true,
    shadowColor: Color = PixelShadow,
    shadowOffsetX: Dp = 4.dp,
    shadowOffsetY: Dp = 4.dp,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        if (shadow) {
            Surface(
                color = shadowColor,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffsetX, y = shadowOffsetY)
            ) {}
        }
        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(borderWidth, borderColor),
            modifier = Modifier.matchParentSize()
        ) {
            content()
        }
    }
}

/**
 * 像素按钮：直角 + 2dp 黑色边框 + 硬阴影 + 按下时阴影消失
 */
@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = PixelIndigo,
    textColor: Color = PixelBackground,
    borderColor: Color = PixelBorder,
) {
    val bg = if (enabled) backgroundColor else PixelTextSecondary
    Box(modifier = modifier) {
        // 硬阴影
        Surface(
            color = if (enabled) PixelShadow else Color.Transparent,
            shape = RoundedCornerShape(0.dp),
            modifier = Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 3.dp)
        ) {}
        Surface(
            color = bg,
            shape = RoundedCornerShape(0.dp),
            border = BorderStroke(2.dp, if (enabled) borderColor else PixelTextSecondary),
            modifier = Modifier
                .matchParentSize()
                .clickable(enabled = enabled, onClick = onClick)
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = textColor,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

/**
 * 像素描边按钮（outlined）：透明背景 + 黑色边框
 */
@Composable
fun PixelOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = PixelText,
    borderColor: Color = PixelBorder,
) {
    Box(
        modifier = modifier
            .background(Color.Transparent)
            .border(2.dp, if (enabled) borderColor else PixelTextSecondary)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) textColor else PixelTextSecondary,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}
