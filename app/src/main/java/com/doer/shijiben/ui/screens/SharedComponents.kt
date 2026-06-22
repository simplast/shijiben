package com.doer.shijiben.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.doer.shijiben.ui.DatePerspective
import com.doer.shijiben.ui.theme.PixelDivider
import com.doer.shijiben.ui.theme.PixelGray
import com.doer.shijiben.ui.theme.PixelGrayLight

// ============================================================
// Shared UI elements used across HomeScreen sub-components
// ============================================================

@Composable
fun EmptyState(datePerspective: DatePerspective) {
    val text = when (datePerspective) {
        DatePerspective.PAST -> "那天似乎什么也没发生"
        DatePerspective.FUTURE -> "这一天还很空，不如规划点什么？"
        else -> "写下一件事，先不用开始"
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = PixelGray,
        )
    }
}

@Composable
fun SectionSpacer() {
    Box(Modifier.height(16.dp))
}

@Composable
fun DragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp)
            .width(34.dp)
            .height(2.dp)
            .background(PixelGrayLight, shape = RoundedCornerShape(0.dp))
    )
}
