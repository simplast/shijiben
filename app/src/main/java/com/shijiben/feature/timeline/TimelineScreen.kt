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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.data.local.EventEntity
import com.shijiben.feature.recording.RecordingSheet
import com.shijiben.ui.theme.PixelButton
import com.shijiben.ui.theme.PixelCard
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun TimelineScreen(
    onTagsClick: () -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val events by viewModel.events.collectAsStateWithLifecycle()
    val date by viewModel.viewingDate.collectAsStateWithLifecycle()
    val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)

    var showSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // 顶部日期栏
            PixelCard(
                modifier = Modifier.fillMaxWidth(),
                shadow = false
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onTagsClick) {
                            Icon(Icons.Default.Star, contentDescription = "标签")
                        }
                        IconButton(onClick = { viewModel.goToPreviousDay() }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前一天")
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatDate(date),
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = PixelText
                        )
                        Text(
                            text = formatWeekday(date),
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 12.sp,
                            color = PixelTextSecondary
                        )
                    }
                    IconButton(onClick = { viewModel.goToNextDay() }) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "后一天")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // 主体：左时间条 + 右事件列表
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DayProgressBar(
                    events = events,
                    nowHour = nowHour,
                    modifier = Modifier.padding(top = 4.dp)
                )
                EventList(
                    events = events,
                    onEventClick = { event ->
                        editingEvent = event
                        showSheet = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            // 底部记一笔按钮
            PixelButton(
                text = "记一笔",
                onClick = {
                    editingEvent = null
                    showSheet = true
                },
                modifier = Modifier.fillMaxWidth().height(52.dp)
            )
        }

        if (showSheet) {
            RecordingSheet(
                viewingDate = date,
                editingEvent = editingEvent,
                onDismiss = { showSheet = false },
                onSaved = { showSheet = false }
            )
        }
    }
}

private fun formatDate(date: Triple<Int, Int, Int>): String {
    val (y, m, d) = date
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.set(y, m - 1, d, 0, 0, 0)
    return SimpleDateFormat("MM月dd日", Locale.getDefault()).format(cal.time)
}

private fun formatWeekday(date: Triple<Int, Int, Int>): String {
    val (y, m, d) = date
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.set(y, m - 1, d, 0, 0, 0)
    val w = cal.get(Calendar.DAY_OF_WEEK)
    val names = arrayOf("周日","周一","周二","周三","周四","周五","周六")
    return names[w - 1]
}
