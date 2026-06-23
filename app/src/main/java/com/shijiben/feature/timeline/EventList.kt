package com.shijiben.feature.timeline

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.data.local.EventEntity
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventList(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        if (events.isEmpty()) {
            Text(
                text = "今天还是空白",
                color = PixelTextSecondary,

                fontSize = 15.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            for (event in events) {
                Text(
                    text = "${formatTime(event.startTime)} - ${event.title}",
                    color = PixelText,

                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
