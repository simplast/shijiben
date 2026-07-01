package com.shijiben.feature.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
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
    nowState: State<Long>,
    modifier: Modifier = Modifier
) {
    val today = isToday(viewingDate)
    val past = isPastDay(viewingDate)
    val nowCal = remember(nowState.value) { Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = nowState.value } }
    val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
    val coveredHours = remember(events) {
        events.flatMap { e ->
            val startHour = hourOfDay(e.startTime)
            val endHour = e.endTime?.let { hourOfDay(it) } ?: startHour
            (startHour..endHour).toList()
        }.toSet()
    }

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
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
            // 纯色方块（8dp）：三态 + 当前小时黑边高亮。去掉数字标签，精确时刻交给事件卡片。
            Box(
                modifier = Modifier
                    .size(8.dp)
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
