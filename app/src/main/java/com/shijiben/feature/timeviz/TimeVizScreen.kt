package com.shijiben.feature.timeviz

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.Disabled
import com.shijiben.ui.theme.Primary
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary
import com.shijiben.ui.theme.PixelOutlinedButton
import com.shijiben.ui.theme.RainbowTrim
import com.shijiben.ui.theme.TextTertiary
import java.util.Calendar
import java.util.TimeZone
import kotlinx.coroutines.delay

@Composable
fun TimeVizScreen(
    onBack: () -> Unit,
    viewModel: TimeVizViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 每分钟刷新一次，与首页 now produceState 模式一致
    produceState(initialValue = Unit) {
        while (true) {
            viewModel.refresh()
            delay(60_000L)
        }
    }

    var showDatePicker by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部 8dp 彩虹条（与首页一致）
            RainbowTrim()
            // 顶栏：左返回箭头 + 标题
            Row(
                modifier = Modifier.fillMaxWidth().background(SurfaceColor),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "返回",
                        tint = TextPrimary
                    )
                }
                Text(
                    text = "时间可视化",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 三块卡片
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TimeVizCard(title = "今天") {
                    Text(
                        text = state.todayRemaining,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextPrimary
                    )
                }
                TimeVizCard(title = "今年") {
                    Text(
                        text = state.yearRemaining,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = TextPrimary
                    )
                }
                TimeVizCard(title = "这一生") {
                    val life = state.lifeResult
                    if (life == null) {
                        // 未设生日：引导 + 设置按钮
                        Text(
                            text = "设置生日，看看时间还有多远",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(8.dp))
                        PixelOutlinedButton(
                            text = "设置生日",
                            onClick = { showDatePicker = true }
                        )
                    } else {
                        // 已设：显示数字
                        Text(
                            text = "已走过 ${life.yearsLived} 年",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = TextPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        val subText = if (life.exceeded) {
                            "已超过假设的 ${state.lifespanYears} 岁，每一天都是赠礼"
                        } else {
                            "假设 ${state.lifespanYears} 岁，还有约 ${life.yearsRemaining} 年"
                        }
                        Text(
                            text = subText,
                            fontSize = 12.sp,
                            color = TextTertiary
                        )
                        Spacer(Modifier.height(12.dp))
                        // 修改生日 + 寿命 stepper
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PixelOutlinedButton(
                                text = "修改生日",
                                onClick = { showDatePicker = true }
                            )
                            LifespanStepper(
                                value = state.lifespanYears,
                                onDecrement = { viewModel.setLifespan(state.lifespanYears - 1) },
                                onIncrement = { viewModel.setLifespan(state.lifespanYears + 1) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        BirthdayPickerDialog(
            initialMillis = state.birthdayMillis.takeIf { it > 0 },
            onConfirm = { (y, m, d) ->
                viewModel.setBirthday(y, m, d)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}


/** 8-bit 卡片：2dp 黑边 + 白底 + 直角 + 2dp 阴影。 */
@Composable
private fun TimeVizCard(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** 寿命 stepper：[- N +]，两个 26dp 黑边方块按钮 + 中间数字。 */
@Composable
private fun LifespanStepper(
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StepperBox(text = "-", onClick = onDecrement, enabled = value > 60)
        Text(
            text = value.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = TextPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        StepperBox(text = "+", onClick = onIncrement, enabled = value < 120)
    }
}

@Composable
private fun StepperBox(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean
) {
    val bg = if (enabled) Primary else Disabled
    Box(
        modifier = Modifier
            .size(26.dp)
            .border(2.dp, Color.Black)
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthdayPickerDialog(
    initialMillis: Long?,
    onConfirm: (Triple<Int, Int, Int>) -> Unit,
    onDismiss: () -> Unit
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                    cal.timeInMillis = millis
                    onConfirm(
                        Triple(
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH) + 1,
                            cal.get(Calendar.DAY_OF_MONTH)
                        )
                    )
                }
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    ) {
        DatePicker(state = state)
    }
}
