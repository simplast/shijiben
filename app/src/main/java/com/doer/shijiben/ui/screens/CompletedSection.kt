package com.doer.shijiben.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doer.shijiben.data.EventEntity
import com.doer.shijiben.data.TimeFormats
import com.doer.shijiben.ui.DatePerspective
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelCloseIcon
import com.doer.shijiben.ui.theme.PixelCoral
import com.doer.shijiben.ui.theme.PixelDivider
import com.doer.shijiben.ui.theme.PixelGray
import com.doer.shijiben.ui.theme.PixelIconButton
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.PixelLavender
import com.doer.shijiben.ui.theme.PixelMint
import com.doer.shijiben.ui.theme.PixelRefreshIcon
import com.doer.shijiben.ui.theme.PixelSectionHeader
import com.doer.shijiben.ui.theme.PixelSky
import com.doer.shijiben.ui.theme.PixelTeal
import com.doer.shijiben.ui.theme.PixelYellow
import com.doer.shijiben.ui.theme.pressScaleEffect

// ============================================================
// Completed Section
// ============================================================
// Shows completed events with color-bar accents, compact rows.
// ============================================================

private val completedBarColors = listOf(
    PixelTeal, PixelCoral, PixelYellow, PixelLavender, PixelSky
)

@Composable
fun CompletedSection(
    events: List<EventEntity>,
    datePerspective: DatePerspective,
    onRestart: (EventEntity) -> Unit,
    onDelete: (EventEntity) -> Unit,
    onOpen: (EventEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        PixelSectionHeader(
            title = if (datePerspective == DatePerspective.TODAY) "今天已完成" else "已完成",
            accentColor = PixelTeal,
        )
        Spacer(Modifier.height(8.dp))

        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            level = PixelCardLevel.Secondary,
            backgroundColor = PixelMint,
            borderColor = PixelTeal,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize()
            ) {
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(
                            text = "暂无记录",
                            style = MaterialTheme.typography.bodyMedium,
                            color = PixelGray,
                        )
                    }
                } else {
                    events.forEachIndexed { index, event ->
                        CompletedEventRow(
                            event = event,
                            datePerspective = datePerspective,
                            barColor = completedBarColors[index % completedBarColors.size],
                            onRestart = { onRestart(event) },
                            onDelete = { onDelete(event) },
                            onClick = { onOpen(event) },
                        )
                        if (index < events.lastIndex) {
                            PixelDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompletedEventRow(
    event: EventEntity,
    datePerspective: DatePerspective,
    barColor: Color,
    onRestart: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val minutes = ((event.endTimeMillis - event.startTimeMillis) / 60_000L).coerceAtLeast(0L)
    val meta = "${TimeFormats.formatTimeMillis(event.startTimeMillis)}—${TimeFormats.formatTimeMillis(event.endTimeMillis)} · ${minutes}m"
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScaleEffect(interactionSource),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 3dp rotating color bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(barColor)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = event.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = meta,
            style = PixelLabel,
            color = PixelGray,
            maxLines = 1,
        )
        if (datePerspective != DatePerspective.PAST) {
            PixelIconButton(
                onClick = onRestart,
                icon = { PixelRefreshIcon(color = PixelTeal, size = 18.dp) },
                backgroundColor = PixelMint,
                pressedBackgroundColor = PixelTeal.copy(alpha = 0.2f),
                borderColor = PixelTeal,
                size = 36.dp,
                contentDescription = "重来",
            )
            Spacer(Modifier.width(6.dp))
        }
        PixelIconButton(
            onClick = onDelete,
            icon = { PixelCloseIcon(color = PixelCoral, size = 18.dp) },
            backgroundColor = PixelCoral.copy(alpha = 0.12f),
            pressedBackgroundColor = PixelCoral.copy(alpha = 0.25f),
            borderColor = PixelCoral,
            size = 36.dp,
            contentDescription = "删除",
        )
    }
}
