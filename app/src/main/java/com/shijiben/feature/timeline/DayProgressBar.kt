package com.shijiben.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shijiben.data.local.EventEntity
import com.shijiben.ui.theme.BorderLight
import com.shijiben.ui.theme.RainbowHourColors
import com.shijiben.ui.theme.TimeBlockNowBorder
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
    val coveredHours = events.flatMap { e ->
        val startHour = hourOfDay(e.startTime)
        val endHour = e.endTime?.let { hourOfDay(it) } ?: startHour
        (startHour..endHour).toList()
    }.toSet()

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (hour in 0..23) {
            val hasRecord = hour in coveredHours
            val isPast = if (today) hour <= nowHour else past
            val isNow = today && hour == nowHour
            val baseColor = RainbowHourColors[hour % 8]
            val bgColor = when {
                hasRecord -> baseColor
                isPast -> baseColor.copy(alpha = 0.3f)
                else -> BorderLight
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(bgColor)
                    .then(if (isNow) Modifier.border(2.dp, TimeBlockNowBorder) else Modifier)
            )
        }
    }
}

private fun hourOfDay(timestamp: Long): Int {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.timeInMillis = timestamp
    return cal.get(Calendar.HOUR_OF_DAY)
}

private fun isToday(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    return date == Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

private fun isPastDay(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val today = Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    val (ty, tm, td) = today
    val (y, m, d) = date
    return y < ty || (y == ty && (m < tm || (m == tm && d < td)))
}
