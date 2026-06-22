package com.shijiben.feature.timeline

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.data.local.EventEntity
import com.shijiben.data.model.EventStatus
import com.shijiben.ui.theme.PixelCard
import com.shijiben.ui.theme.PixelOutlinedButton
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun EventList(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    if (events.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "今天还是空白，记一笔吧",
                color = PixelTextSecondary,
                fontFamily = FontFamily.SansSerif,
                fontSize = 15.sp
            )
        }
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
    ) {
        items(events, key = { it.id }) { event ->
            EventCard(event = event, onClick = { onEventClick(event) })
        }
    }
}

@Composable
private fun EventCard(event: EventEntity, onClick: () -> Unit) {
    PixelCard(
        modifier = Modifier.fillMaxWidth(),
        shadow = event.status != EventStatus.NotStarted.value // notStarted 无阴影（半透明感）
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 时间列
            Column(modifier = Modifier.width(72.dp)) {
                Text(
                    text = formatTime(event.startTime),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = PixelText
                )
                Text(
                    text = formatDuration(event.startTime, event.endTime),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = PixelTextSecondary
                )
            }
            Spacer(Modifier.width(8.dp))
            // 标题列
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (event.status == EventStatus.NotStarted.value) PixelTextSecondary else PixelText
                )
                if (event.note != null) {
                    Text(
                        text = event.note,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        color = PixelTextSecondary,
                        maxLines = 1
                    )
                }
            }
            // 状态标签
            if (event.status == EventStatus.NotStarted.value) {
                PixelOutlinedButton(
                    text = "预写",
                    onClick = onClick,
                    modifier = Modifier.height(28.dp)
                )
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))

private fun formatDuration(start: Long, end: Long?): String {
    val e = end ?: System.currentTimeMillis()
    val mins = TimeUnit.MILLISECONDS.toMinutes(e - start)
    val h = mins / 60
    val m = mins % 60
    return if (h > 0) "${h}h${m}m" else "${m}m"
}
