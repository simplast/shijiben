package com.shijiben.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.BuildConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * 顶层调试 overlay：包裹 app 内容，在之上叠加一个可拖动的悬浮小圆点；
 * 点击圆点展开全屏调试面板，查看 [DebugLog] 收集的异常/日志。
 * 仅 debug 构建显示悬浮层（[content] 始终渲染）。
 */
@Composable
fun DebugOverlay(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (BuildConfig.DEBUG) {
            DebugFloatingButton()
        }
    }
}

@Composable
private fun DebugFloatingButton() {
    var expanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (expanded) {
            DebugPanel(onClose = { expanded = false })
        }
        // 悬浮小圆点：24dp 半透明红，可拖动，点击展开
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 90.dp) // 从右下角内缩，避开底部入口
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .size(24.dp)
                .background(Color(0xCCFF4444), CircleShape)
                .border(1.dp, Color.Black, CircleShape)
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var totalX = 0f
                        var totalY = 0f
                        var dragged = false
                        val touchSlop = viewConfiguration.touchSlop
                        while (true) {
                            val event = awaitPointerEvent()
                            val change: PointerInputChange = event.changes.first()
                            val dx = change.positionChange().x
                            val dy = change.positionChange().y
                            totalX += dx
                            totalY += dy
                            if (!dragged && (abs(totalX) > touchSlop || abs(totalY) > touchSlop)) {
                                dragged = true
                            }
                            if (dragged) {
                                offsetX += dx
                                offsetY += dy
                                change.consume()
                            }
                            if (change.changedToUp()) {
                                if (!dragged) expanded = !expanded
                                break
                            }
                        }
                    }
                }
        )
    }
}

@Composable
private fun DebugPanel(onClose: () -> Unit) {
    var filter by remember { mutableStateOf(DebugFilter.ALL) }
    val timeFmt = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    val filtered = remember(DebugLog.entries.toList(), filter) {
        when (filter) {
            DebugFilter.ALL -> DebugLog.entries
            DebugFilter.ERROR -> DebugLog.entries.filter { it.level == DebugLevel.ERROR }
            DebugFilter.LOGS -> DebugLog.entries.filter { it.level != DebugLevel.ERROR }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF0000000))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶栏
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1A1A1A))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "调试 console",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.width(12.dp))
                DebugFilterTab("全部", DebugFilter.ALL, filter) { filter = it }
                DebugFilterTab("错误", DebugFilter.ERROR, filter) { filter = it }
                DebugFilterTab("日志", DebugFilter.LOGS, filter) { filter = it }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${filtered.size}条",
                    color = Color(0xFF999999),
                    fontSize = 11.sp
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "关闭",
                    tint = Color.White,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(20.dp)
                        .clickable { onClose() }
                )
            }
            // 列表
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("暂无记录", color = Color(0xFF666666), fontSize = 13.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(filtered, key = { it.time.toString() + it.summary }) { entry ->
                        DebugEntryRow(entry, timeFmt)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebugFilterTab(
    label: String,
    target: DebugFilter,
    current: DebugFilter,
    onSelect: (DebugFilter) -> Unit
) {
    val selected = target == current
    Text(
        text = label,
        color = if (selected) Color(0xFFFF6666) else Color(0xFF999999),
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .clickable { onSelect(target) }
    )
}

@Composable
private fun DebugEntryRow(entry: DebugEntry, timeFmt: SimpleDateFormat) {
    var expanded by remember { mutableStateOf(false) }
    val levelColor = when (entry.level) {
        DebugLevel.ERROR -> Color(0xFFFF6666)
        DebugLevel.WARN -> Color(0xFFFFAA33)
        DebugLevel.INFO -> Color(0xFF66AAFF)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111111))
            .clickable { if (entry.stacktrace != null) expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timeFmt.format(Date(entry.time)),
                color = Color(0xFF999999),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = entry.level.name,
                color = levelColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
            entry.tag?.let {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "[$it]",
                    color = Color(0xFF888888),
                    fontSize = 10.sp
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = entry.summary,
                color = Color(0xFFDDDDDD),
                fontSize = 11.sp,
                maxLines = if (expanded) 1 else 2,
                modifier = Modifier.weight(1f)
            )
            if (entry.stacktrace != null) {
                Text(
                    text = if (expanded) "▾" else "▸",
                    color = Color(0xFF999999),
                    fontSize = 10.sp
                )
            }
        }
        if (expanded && entry.stacktrace != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = entry.stacktrace,
                color = Color(0xFFAAAAAA),
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .height(400.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .height(1.dp)
                .background(Color(0xFF222222))
        )
    }
}

private enum class DebugFilter { ALL, ERROR, LOGS }
