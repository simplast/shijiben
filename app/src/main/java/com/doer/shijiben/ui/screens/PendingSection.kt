package com.doer.shijiben.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.doer.shijiben.ui.DatePerspective
import com.doer.shijiben.ui.theme.PixelAmber
import com.doer.shijiben.ui.theme.PixelBadge
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelCloseIcon
import com.doer.shijiben.ui.theme.PixelCoral
import com.doer.shijiben.ui.theme.PixelCream
import com.doer.shijiben.ui.theme.PixelDivider
import com.doer.shijiben.ui.theme.PixelGray
import com.doer.shijiben.ui.theme.PixelIconButton
import com.doer.shijiben.ui.theme.PixelPlayIcon
import com.doer.shijiben.ui.theme.PixelSectionHeader
import com.doer.shijiben.ui.theme.PixelSky
import com.doer.shijiben.ui.theme.PixelTeal
import com.doer.shijiben.ui.theme.PixelYellow
import com.doer.shijiben.ui.theme.pressScaleEffect

// ============================================================
// Pending Section
// ============================================================
// Shows pending events with play/delete actions,
// plus recommendation chips for quick-start.
// ============================================================

private val playButtonColors = listOf(
    PixelCoral, PixelTeal, PixelSky, PixelYellow, PixelAmber
)

@Composable
fun PendingSection(
    events: List<EventEntity>,
    datePerspective: DatePerspective,
    recommendedNames: List<String>,
    completedNames: Set<String>,
    onStart: (EventEntity) -> Unit,
    onAdd: (String) -> Unit,
    onQuickStart: (String) -> Unit,
    onDelete: (EventEntity) -> Unit,
    onOpen: (EventEntity) -> Unit
) {
    val existingNames = events.map { it.name.trim() }.toSet()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Recommendation chips
        val validRecommendations = recommendedNames
            .map { it.trim() }
            .filter { !existingNames.contains(it) && !completedNames.contains(it) }

        if (validRecommendations.isNotEmpty() && datePerspective != DatePerspective.PAST) {
            PixelSectionHeader(
                title = "建议快速开始",
                accentColor = PixelCoral,
            )
            Spacer(Modifier.height(6.dp))

            val tagColors = listOf(PixelCoral, PixelTeal, PixelYellow, PixelSky)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(validRecommendations.size) { index ->
                    val name = validRecommendations[index]
                    val tagColor = tagColors[index % tagColors.size]
                    PixelBadge(
                        text = name,
                        backgroundColor = tagColor,
                        onClick = { onQuickStart(name) },
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        if (datePerspective == DatePerspective.PAST && events.isEmpty()) {
            EmptyState(datePerspective)
            return
        }

        PixelSectionHeader(
            title = if (datePerspective == DatePerspective.TODAY) "今日待办" else "待办",
            accentColor = PixelYellow,
        )
        Spacer(Modifier.height(8.dp))

        // Pending card with left amber edge
        PixelCard(
            modifier = Modifier.fillMaxWidth(),
            level = PixelCardLevel.Secondary,
            backgroundColor = PixelCream,
            borderColor = PixelAmber,
            contentPadding = PaddingValues(all = 0.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                // Left 5dp amber edge bar
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(PixelYellow)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .animateContentSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    if (events.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = when (datePerspective) {
                                    DatePerspective.PAST -> "那天似乎什么也没发生"
                                    DatePerspective.FUTURE -> "这一天还很空，不如规划点什么？"
                                    else -> "写下一件事，先不用开始"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = PixelGray,
                            )
                        }
                    } else {
                        events.forEachIndexed { index, event ->
                            PendingEventRow(
                                event = event,
                                datePerspective = datePerspective,
                                playButtonColor = playButtonColors[index % playButtonColors.size],
                                onStart = { onStart(event) },
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
}

@Composable
private fun PendingEventRow(
    event: EventEntity,
    datePerspective: DatePerspective,
    playButtonColor: Color,
    onStart: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .then(
                if (isPressed) Modifier.background(PixelYellow.copy(alpha = 0.15f))
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .pressScaleEffect(interactionSource),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 3dp yellow left bar
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(PixelYellow)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = event.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))

        if (datePerspective == DatePerspective.TODAY) {
            PixelIconButton(
                onClick = onStart,
                icon = { PixelPlayIcon(color = Color.White, size = 18.dp) },
                backgroundColor = playButtonColor,
                pressedBackgroundColor = playButtonColor.copy(alpha = 0.8f),
                borderColor = Color.Transparent,
                size = 36.dp,
                borderless = true,
                contentDescription = "开始",
            )
        }
        PixelIconButton(
            onClick = onDelete,
            icon = { PixelCloseIcon(color = PixelCoral, size = 18.dp) },
            backgroundColor = PixelCoral.copy(alpha = 0.12f),
            pressedBackgroundColor = PixelCoral.copy(alpha = 0.25f),
            borderColor = Color.Transparent,
            size = 36.dp,
            borderless = true,
            contentDescription = "删除",
        )
    }
}
