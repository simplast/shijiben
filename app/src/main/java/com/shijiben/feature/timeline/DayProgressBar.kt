package com.shijiben.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shijiben.data.local.EventEntity
import com.shijiben.ui.theme.PixelBorder
import com.shijiben.ui.theme.TimeBlockFuture
import com.shijiben.ui.theme.TimeBlockNowBorder
import com.shijiben.ui.theme.TimeBlockPast
import com.shijiben.ui.theme.TimeBlockRecorded
import java.util.Calendar
import java.util.TimeZone

@Composable
fun DayProgressBar(
    events: List<EventEntity>,
    nowHour: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 0-12h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) < 12 },
            isPast = nowHour >= 12,
            isNow = nowHour in 0..11,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(8.dp))
        // 12-24h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) >= 12 },
            isPast = false, // 12-24h 永远不算"已过去未记录"（因为 24h 是一天结束）；用 hasRecord 判断
            isNow = nowHour >= 12,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
private fun TimeBlock(
    hasRecord: Boolean,
    isPast: Boolean,
    isNow: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        hasRecord -> TimeBlockRecorded
        isPast -> TimeBlockPast
        else -> TimeBlockFuture
    }
    Box(
        modifier = modifier
            .background(color)
            .then(if (isNow) Modifier.border(3.dp, TimeBlockNowBorder) else Modifier)
    )
}

private fun hourOfDay(timestamp: Long): Int {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.timeInMillis = timestamp
    return cal.get(Calendar.HOUR_OF_DAY)
}
