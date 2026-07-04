package com.shijiben.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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

@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Surface,
    borderColor: Color = Border,
    borderWidth: Dp = 2.dp,
    shadow: Boolean = true,
    content: @Composable () -> Unit
) {
    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(borderWidth, borderColor),
        shadowElevation = if (shadow) 2.dp else 0.dp,
        modifier = modifier
    ) {
        content()
    }
}

@Composable
fun PixelButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    backgroundColor: Color = Primary,
    textColor: Color = TextOnPrimary,
) {
    val bg = if (enabled) backgroundColor else Disabled
    
    Surface(
        color = bg,
        shape = RoundedCornerShape(0.dp),
        shadowElevation = 4.dp,
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = textColor,

                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun PixelOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    textColor: Color = Primary,
    borderColor: Color = Primary,
) {
    Surface(
        color = Color.Transparent,
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, if (enabled) borderColor else Disabled),
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (enabled) textColor else DisabledText,

                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}

/** 顶部 8dp 彩虹条（全 app 唯一品牌标识条，各屏幕顶部统一调用）。 */
@Composable
fun RainbowTrim() {
    val trimColors = listOf(
        Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
        Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
        Color(0xFF6366F1), Color(0xFFA855F7)
    )
    Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
        for (i in 0 until 80) {
            Box(
                modifier = Modifier
                    .weight(1f).fillMaxHeight()
                    .background(trimColors[i % 8])
            )
        }
    }
}
