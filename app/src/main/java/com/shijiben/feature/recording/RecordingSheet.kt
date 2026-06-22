package com.shijiben.feature.recording

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.TagEntity
import com.shijiben.ui.theme.PixelBorder
import com.shijiben.ui.theme.PixelButton
import com.shijiben.ui.theme.PixelOutlinedButton
import com.shijiben.ui.theme.PixelSurface
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary
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
    LaunchedEffect(editingEvent?.id) {
        if (editingEvent != null) viewModel.initEdit(editingEvent)
        else viewModel.initNew()
    }
    val title by viewModel.title.collectAsStateWithLifecycle()
    val startMin by viewModel.startMinutes.collectAsStateWithLifecycle()
    val endMin by viewModel.endMinutes.collectAsStateWithLifecycle()
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val selectedTagId by viewModel.selectedTagId.collectAsStateWithLifecycle()
    val note by viewModel.note.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = PixelSurface,
        shape = RoundedCornerShape(0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editingEvent != null) "编辑事件" else "记一笔",
                style = MaterialTheme.typography.titleLarge,
                color = PixelText
            )
            // 标题输入
            OutlinedTextField(
                value = title,
                onValueChange = viewModel::onTitleChange,
                label = { Text("做了什么 / 打算做什么") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            // 时间滑块
            TimeRangeSlider(
                startMinutes = startMin,
                endMinutes = endMin,
                onMinutesChange = viewModel::onTimeChange,
                modifier = Modifier.fillMaxWidth()
            )
            // 标签 chips
            Text(
                text = "标签",
                fontSize = 13.sp,
                color = PixelTextSecondary
            )
            if (tags.isEmpty()) {
                Text(
                    text = "还没有标签，去标签页创建",
                    fontSize = 12.sp,
                    color = PixelTextSecondary
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tags, key = { it.id }) { tag ->
                        TagChip(
                            tag = tag,
                            selected = tag.id == selectedTagId,
                            onClick = {
                                viewModel.onTagSelected(
                                    if (tag.id == selectedTagId) null else tag.id
                                )
                            }
                        )
                    }
                }
            }
            // 备注
            OutlinedTextField(
                value = note,
                onValueChange = viewModel::onNoteChange,
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            // 状态切换（仅编辑模式）
            if (editingEvent != null) {
                Text(
                    text = "状态",
                    fontSize = 13.sp,
                    color = PixelTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelOutlinedButton(
                        text = "未开始",
                        onClick = {
                            scope.launch {
                                viewModel.markNotStarted()
                                onSaved()
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp)
                    )
                    PixelOutlinedButton(
                        text = "进行中",
                        onClick = {
                            scope.launch {
                                viewModel.markInProgress()
                                onSaved()
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp)
                    )
                    PixelOutlinedButton(
                        text = "完成",
                        onClick = {
                            scope.launch {
                                viewModel.markCompleted()
                                onSaved()
                            }
                        },
                        modifier = Modifier.weight(1f).height(40.dp)
                    )
                }
            }
            // 按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editingEvent != null) {
                    PixelOutlinedButton(
                        text = "删除",
                        onClick = {
                            scope.launch {
                                viewModel.delete()
                                onSaved()
                            }
                        },
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
                PixelOutlinedButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                )
                PixelButton(
                    text = "确定",
                    onClick = {
                        scope.launch {
                            val ok = viewModel.save(viewingDate)
                            if (ok) onSaved()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TagChip(tag: TagEntity, selected: Boolean, onClick: () -> Unit) {
    val bg = Color(tag.color)
    Row(
        modifier = Modifier
            .background(bg)
            .border(2.dp, if (selected) PixelBorder else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = tag.name,
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}
