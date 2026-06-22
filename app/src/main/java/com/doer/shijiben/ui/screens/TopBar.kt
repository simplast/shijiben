package com.doer.shijiben.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.doer.shijiben.ui.theme.PixelAddIcon
import com.doer.shijiben.ui.theme.PixelBorder
import com.doer.shijiben.ui.theme.PixelCalendarIcon
import com.doer.shijiben.ui.theme.PixelCream
import com.doer.shijiben.ui.theme.PixelDivider
import com.doer.shijiben.ui.theme.PixelIconButton
import com.doer.shijiben.ui.theme.PixelMoreIcon
import com.doer.shijiben.ui.theme.PixelTeal
import com.doer.shijiben.ui.theme.pixelBorderSecondary

// ============================================================
// Top Bar
// ============================================================
// Calendar picker + date label + overflow menu.
// ============================================================

@Composable
fun TopBar(
    dateLabel: String,
    onPickDate: () -> Unit,
    onOpenReview: () -> Unit,
    onExportCsv: () -> Unit,
    onExportJson: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().height(42.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PixelIconButton(
            onClick = onPickDate,
            icon = { PixelCalendarIcon(color = PixelTeal, size = 20.dp) },
            backgroundColor = Color.Transparent,
            pressedBackgroundColor = PixelTeal.copy(alpha = 0.15f),
            borderColor = Color.Transparent,
            size = 36.dp,
            contentDescription = "选择日期",
        )
        Text(
            text = dateLabel,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onPickDate),
        )
        Box {
            PixelIconButton(
                onClick = { menuExpanded = true },
                icon = { PixelMoreIcon(color = PixelBorder, size = 20.dp) },
                backgroundColor = Color.Transparent,
                pressedBackgroundColor = PixelBorder.copy(alpha = 0.1f),
                borderColor = Color.Transparent,
                size = 36.dp,
                contentDescription = "菜单",
            )
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(PixelCream)
                    .pixelBorderSecondary(PixelTeal),
            ) {
                DropdownMenuItem(
                    text = { Text("数据统计", style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        menuExpanded = false
                        onOpenReview()
                    },
                )
                DropdownMenuItem(
                    text = { Text("导出 CSV", style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        menuExpanded = false
                        onExportCsv()
                    },
                )
                DropdownMenuItem(
                    text = { Text("导出 JSON", style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        menuExpanded = false
                        onExportJson()
                    },
                )
            }
        }
    }
}
