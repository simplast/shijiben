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
    viewingDate: Triple<Int, Int, Int>,
    nowHour: Int,
    modifier: Modifier = Modifier
) {
    val today = isToday(viewingDate)
    val past = isPastDay(viewingDate)
    Column(modifier = modifier) {
        // 0-12h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) < 12 },
            isPast = if (today) nowHour >= 12 else past,
            isNow = today && nowHour in 0..11,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(8.dp))
        // 12-24h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) >= 12 },
            isPast = if (today) nowHour >= 12 else past,
            isNow = today && nowHour >= 12,
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

/** Returns true if the viewed date is today. */
private fun isToday(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    return date == Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/**
 * Returns true if the viewed date is strictly before today (a past day).
 * Future days and today both return false.
 */
private fun isPastDay(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val today = Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    // Lexicographic compare on (year, month, day).
    val (ty, tm, td) = today
    val (y, m, d) = date
    return y < ty || (y == ty && (m < tm || (m == tm && d < td)))
}
