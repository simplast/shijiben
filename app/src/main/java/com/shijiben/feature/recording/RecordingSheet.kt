package com.shijiben.feature.recording

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.data.local.EventEntity
import com.shijiben.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingSheet(
    viewingDate: Triple<Int, Int, Int>,
    editingEvent: EventEntity? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: RecordingViewModel = hiltViewModel()
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(editingEvent?.id) {
        if (editingEvent != null) viewModel.initEdit(editingEvent)
        else {
            viewModel.initNew()
            delay(400)
            focusRequester.requestFocus()
        }
    }
    val title by viewModel.title.collectAsStateWithLifecycle()
    val startMin by viewModel.startMinutes.collectAsStateWithLifecycle()
    val durationMin by viewModel.durationMinutes.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题输入框（自动聚焦）
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("做了什么", color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )

            // 时间选择器
            TimeRangeSlider(
                startMinutes = startMin,
                durationMinutes = durationMin,
                onStartChange = viewModel::onStartChange,
                onDurationChange = viewModel::onDurationChange,
                modifier = Modifier.fillMaxWidth()
            )

            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PixelOutlinedButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                )
                PixelButton(
                    text = "保存",
                    onClick = {
                        scope.launch {
                            val ok = viewModel.save(viewingDate)
                            if (ok) onSaved()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    backgroundColor = Primary
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
