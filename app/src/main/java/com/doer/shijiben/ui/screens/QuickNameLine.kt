package com.doer.shijiben.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doer.shijiben.ui.theme.PixelAddIcon
import com.doer.shijiben.ui.theme.PixelCoral
import com.doer.shijiben.ui.theme.PixelIconButton
import com.doer.shijiben.ui.theme.PixelInput
import com.doer.shijiben.ui.theme.PixelPink
import com.doer.shijiben.ui.theme.PixelPlayIcon

// ============================================================
// Quick Name Line — Bottom input bar
// ============================================================
// Floating input for quickly adding events.
// ============================================================

@Composable
fun QuickNameLine(
    value: String,
    onValueChange: (String) -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // "+" button — pink, no border
        PixelIconButton(
            onClick = onAdd,
            icon = { PixelAddIcon(color = Color.White, size = 20.dp) },
            backgroundColor = PixelPink,
            pressedBackgroundColor = PixelPink.copy(alpha = 0.8f),
            borderColor = Color.Transparent,
            size = 40.dp,
            borderless = true,
            contentDescription = "添加",
        )
        Spacer(Modifier.width(8.dp))
        PixelInput(
            value = value,
            onValueChange = onValueChange,
            placeholder = "今天想做点什么...",
            modifier = Modifier.weight(1f).height(40.dp),
        )
        Spacer(Modifier.width(8.dp))
        // Submit button — coral
        PixelIconButton(
            onClick = onAdd,
            icon = { PixelPlayIcon(color = Color.White, size = 18.dp) },
            backgroundColor = PixelCoral,
            pressedBackgroundColor = PixelCoral.copy(alpha = 0.8f),
            borderColor = Color.Transparent,
            size = 40.dp,
            borderless = true,
            contentDescription = "提交",
        )
    }
}
