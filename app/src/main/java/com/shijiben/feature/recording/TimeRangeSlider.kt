package com.shijiben.feature.recording

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.ui.theme.*

// ==================== 彩虹方块颜色 ====================
// 方块使用 SliderRainbowActive/Inactive（6 色彩虹循环，定义在 AppColors.kt）
private val CursorColor = Color(0xFFD946EF)   // 亮粉紫光标

// ==================== 开始时间参数 ====================
private const val ABS_MIN = 300      // 5:00
private const val ABS_MAX = 1440     // 24:00
private const val WINDOW_HALF = 180  // ±3h 半宽

// ==================== 持续时间参数 ====================
private const val DUR_MAX_DEFAULT = 180      // 3 小时（新建事件默认上限）
private const val DUR_HARD_CEILING = 480     // 8 小时硬上限（防止极端值）

// 方块大小
private val BLOCK_DP = 8.dp
private val TRACK_HEIGHT_DP = 24.dp
private val CURSOR_HEIGHT_DP = 40.dp // 比轨道高，上下穿透

/**
 * 时间选择器
 * scroll track = 方块像素网格
 * scroll thumb = 细竖线
 * 开始时间：动态窗口（当前±3h），拖到边缘偏移
 * 持续时间：0-3h
 */
@Composable
fun TimeRangeSlider(
    startMinutes: Int,
    durationMinutes: Int,
    onStartChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    durationMaxMinutes: Int = DUR_MAX_DEFAULT   // 默认 180（新建场景）
) {
    Column(modifier = modifier.fillMaxWidth()) {

        // ==================== 开始时间 ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("开始", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextSecondary)
            Box(
                modifier = Modifier
                    .background(Accent, RoundedCornerShape(0.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = formatTime(startMinutes),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        StartTimeBar(
            value = startMinutes,
            onValueChange = onStartChange
        )

        Spacer(Modifier.height(16.dp))

        // ==================== 持续时间 ====================
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("时长", fontWeight = FontWeight.Medium, fontSize = 13.sp, color = TextSecondary)
            Box(
                modifier = Modifier
                    .background(Primary, RoundedCornerShape(0.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = formatDuration(durationMinutes),
                    fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White
                )
            }
        }
        Spacer(Modifier.height(4.dp))

        DurationBar(
            value = durationMinutes,
            onValueChange = onDurationChange,
            maxMinutes = durationMaxMinutes
        )
    }
}

// ==================== 开始时间（固定光标居中，拖拽轨道滚动） ====================

@Composable
private fun StartTimeBar(value: Int, onValueChange: (Int) -> Unit) {
    // 窗口始终以当前值为中心（±3h），拖拽时 value 变化 → 窗口自然滑动
    val windowStart = (value - WINDOW_HALF).coerceAtLeast(ABS_MIN)
    val windowEnd = (value + WINDOW_HALF).coerceAtMost(ABS_MAX)

    val labels = remember(windowStart, windowEnd) {
        val hStart = windowStart / 60
        val hEnd = windowEnd / 60
        buildList {
            for (h in hStart..hEnd) add(if (h == 0) "0点" else "${h}点")
        }
    }

    Column {
        PixelTrackWithCursor(
            value = value,
            valueMin = windowStart,
            valueMax = windowEnd,
            onValueChange = { onValueChange(it.coerceIn(ABS_MIN, ABS_MAX)) },
            cursorAtCenter = true
        )
        BarLabels(labels)
    }
}

// ==================== 持续时间（0-3h） ====================

@Composable
private fun DurationBar(
    value: Int,
    onValueChange: (Int) -> Unit,
    maxMinutes: Int
) {
    val effectiveMax = maxMinutes.coerceIn(1, DUR_HARD_CEILING)
    val labels = remember(effectiveMax) { buildDurationLabels(effectiveMax) }
    Column {
        PixelTrackWithCursor(
            value = value.coerceIn(0, effectiveMax),
            valueMin = 0,
            valueMax = effectiveMax,
            onValueChange = { onValueChange(it.coerceIn(0, effectiveMax)) }
        )
        BarLabels(labels)
    }
}

/** 生成 0..max 的整点小时标签（每 60 分钟一个，含两端）。 */
private fun buildDurationLabels(maxMinutes: Int): List<String> {
    val hours = maxMinutes / 60
    return buildList {
        add("0")
        for (h in 1..hours) add("${h}h")
    }
}

// ==================== 像素方块轨道 + 竖线光标 ====================

@Composable
private fun PixelTrackWithCursor(
    value: Int,
    valueMin: Int,
    valueMax: Int,
    onValueChange: (Int) -> Unit,
    cursorAtCenter: Boolean = false
) {
    val density = LocalDensity.current
    val blockPx = with(density) { BLOCK_DP.toPx() }
    val trackHeightPx = with(density) { TRACK_HEIGHT_DP.toPx() }
    val cursorHeightPx = with(density) { CURSOR_HEIGHT_DP.toPx() }
    val cursorWidthPx = with(density) { 4.dp.toPx() }

    val range = (valueMax - valueMin).coerceAtLeast(1)
    val fraction = ((value - valueMin).toFloat() / range).coerceIn(0f, 1f)

    var trackWidth by remember { mutableStateOf(1f) }

    // 固定光标模式需要响应式引用（闭包会捕获旧值）
    val latestValue by rememberUpdatedState(value)
    val latestMin by rememberUpdatedState(valueMin)
    val latestMax by rememberUpdatedState(valueMax)
    val latestOnValueChange by rememberUpdatedState(onValueChange)

    // 拖拽使用的触摸区域高度与光标一致（便于抓取）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(CURSOR_HEIGHT_DP)
            .wrapContentHeight(Alignment.CenterVertically)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { trackWidth = it.width.toFloat() }
                .pointerInput(Unit) {
                    if (cursorAtCenter) {
                        // 固定光标模式：光标居中，拖拽轨道整体滚动
                        detectDragGestures(
                            onDragStart = { /* 不用特殊初始化 */ },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = trackWidth
                                if (w <= 0f) return@detectDragGestures
                                val r = (latestMax - latestMin).coerceAtLeast(1)
                                val deltaMinutes = -(dragAmount.x / w * r).toInt()
                                if (deltaMinutes != 0) {
                                    latestOnValueChange(
                                        (latestValue + deltaMinutes).coerceIn(latestMin, latestMax)
                                    )
                                }
                            }
                        )
                    } else {
                        // 传统模式：用绝对位置拖动光标
                        detectDragGestures(
                            onDrag = { change, _ ->
                                change.consume()
                                val f = (change.position.x / trackWidth).coerceIn(0f, 1f)
                                onValueChange(valueMin + (f * range).toInt())
                            }
                        )
                    }
                }
        ) {
            val numBlocks = (trackWidth / blockPx).toInt().coerceAtLeast(1)
            val actualBlockWidth = trackWidth / numBlocks

            // 轨道在垂直方向上居中
            val trackTop = (cursorHeightPx - trackHeightPx) / 2f

            // 绘制方块轨道
            if (cursorAtCenter) {
                // 固定光标模式：方块按 20 分钟固定间隔绘制，随 value 滚动
                val blockMinutes = 20
                val plotRange = 2f * WINDOW_HALF   // 360 分钟，固定参考
                val blockWidthPx = (blockMinutes / plotRange) * trackWidth
                val pixelsPerMinute = trackWidth / plotRange

                // 从窗口左边界向下取整到 block 边界
                val firstBlockTime = ((value - WINDOW_HALF) / blockMinutes * blockMinutes)
                    .coerceAtLeast(ABS_MIN)
                val lastBlockTime = (value + WINDOW_HALF).coerceAtMost(ABS_MAX)
                var t = firstBlockTime
                while (t <= lastBlockTime) {
                    val screenX = (t.toFloat() - value + WINDOW_HALF) * pixelsPerMinute
                    // 只绘制落在可见区域内的方块
                    if (screenX < trackWidth && screenX + blockWidthPx > 0f) {
                        val isActive = t <= value
                        val blockIdx = (t - ABS_MIN) / blockMinutes
                        val color = if (isActive) {
                            SliderRainbowActive[blockIdx % 6]
                        } else {
                            SliderRainbowInactive[blockIdx % 6]
                        }
                        drawRect(
                            color = color,
                            topLeft = Offset(screenX, trackTop + 2f),
                            size = Size(blockWidthPx - 1f, trackHeightPx - 4f)
                        )
                    }
                    t += blockMinutes
                }
            } else {
                // 传统模式：将窗口等分
                for (i in 0 until numBlocks) {
                    val blockLeft = i * actualBlockWidth
                    val isActive = (i.toFloat() / numBlocks) <= fraction
                    val color = if (isActive) {
                        SliderRainbowActive[i % 6]
                    } else {
                        SliderRainbowInactive[i % 6]
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(blockLeft, trackTop + 2f),
                        size = Size(actualBlockWidth - 1f, trackHeightPx - 4f)
                    )
                }
            }

            // 竖线光标（scroll thumb），上下穿透轨道
            val thumbX = if (cursorAtCenter) trackWidth / 2f else fraction * trackWidth
            drawRect(
                color = CursorColor,
                topLeft = Offset(thumbX - cursorWidthPx / 2f, 0f),
                size = Size(cursorWidthPx, cursorHeightPx)
            )
        }
    }
}

// ==================== 标签行 ====================

@Composable
private fun BarLabels(labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (lbl in labels) {
            Text(
                text = lbl,
                fontSize = 9.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

// ==================== 格式化 ====================

private fun formatTime(min: Int): String {
    val h = min / 60
    val m = min % 60
    return "%02d:%02d".format(h, m)
}

private fun formatDuration(min: Int): String {
    val h = min / 60
    val m = min % 60
    return when {
        h == 0 -> "${m}分钟"
        m == 0 -> "${h}小时"
        else -> "${h}小时${m}分钟"
    }
}
