package com.doer.shijiben.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.ui.theme.PixelBlinkIndicator
import com.doer.shijiben.ui.theme.PixelBorder
import com.doer.shijiben.ui.theme.PixelButton
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelDisplay
import com.doer.shijiben.ui.theme.PixelHopNumber
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.PixelPink
import com.doer.shijiben.ui.theme.PixelStopIcon
import com.doer.shijiben.ui.theme.PixelTeal
import com.doer.shijiben.ui.theme.PixelYellow

// ============================================================
// Active Event Hero Card
// ============================================================
// Shows the currently running event with blinking indicator,
// elapsed time counter, and stop button.
// ============================================================

@Composable
fun ActiveEventCard(
    event: EventEntity,
    elapsedMinutes: Long,
    onStop: () -> Unit,
    onClick: () -> Unit,
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "cardPress"
    )

    PixelCard(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = cardScale; scaleY = cardScale }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            },
        level = PixelCardLevel.Primary,
        backgroundColor = PixelPink,
        contentPadding = PaddingValues(20.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelBlinkIndicator(color = PixelYellow)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "正在记录时间...",
                        style = PixelLabel,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(horizontalAlignment = Alignment.End) {
                PixelHopNumber(
                    targetValue = elapsedMinutes,
                    format = { "${it}m" },
                    textStyle = PixelDisplay,
                    contentColor = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                PixelButton(
                    onClick = onStop,
                    backgroundColor = PixelTeal,
                    borderColor = PixelBorder,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    PixelStopIcon(color = Color.White, size = 16.dp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "结束",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
