package com.shijiben.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
    borderWidth: Dp = 1.dp,
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
    val bg = if (enabled) backgroundColor else Color(0xFFCBD5E1)
    
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
        border = androidx.compose.foundation.BorderStroke(2.dp, if (enabled) borderColor else Color(0xFFCBD5E1)),
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 16.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (enabled) textColor else Color(0xFF94A3B8),

                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
        }
    }
}
