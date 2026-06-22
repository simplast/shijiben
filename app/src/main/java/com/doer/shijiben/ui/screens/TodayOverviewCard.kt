package com.doer.shijiben.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.doer.shijiben.ui.theme.PixelBadge
import com.doer.shijiben.ui.theme.PixelCard
import com.doer.shijiben.ui.theme.PixelCardLevel
import com.doer.shijiben.ui.theme.PixelCoral
import com.doer.shijiben.ui.theme.PixelDisplay
import com.doer.shijiben.ui.theme.PixelHopNumber
import com.doer.shijiben.ui.theme.PixelLabel
import com.doer.shijiben.ui.theme.PixelLavender
import com.doer.shijiben.ui.theme.PixelSky

// ============================================================
// Today Overview Hero Card
// ============================================================
// Shows total focused time + completed/pending counts.
// Only shown when viewing today's date.
// ============================================================

@Composable
fun TodayOverviewCard(
    totalMinutes: Long,
    completedCount: Int,
    pendingCount: Int,
) {
    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        level = PixelCardLevel.Primary,
        backgroundColor = PixelSky,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left: large time number
            Column {
                PixelHopNumber(
                    targetValue = totalMinutes,
                    format = { "${it}m" },
                    textStyle = PixelDisplay,
                    contentColor = Color.White,
                )
                Text(
                    text = "已专注",
                    style = PixelLabel,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            // Right: stacked badges
            Column(horizontalAlignment = Alignment.End) {
                PixelBadge(
                    text = "✓ $completedCount 已完成",
                    backgroundColor = PixelCoral,
                )
                Spacer(Modifier.height(6.dp))
                PixelBadge(
                    text = "◇ $pendingCount 待办",
                    backgroundColor = PixelLavender,
                )
            }
        }
    }
}
