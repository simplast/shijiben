package com.shijiben.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.data.local.TagEntity
import com.shijiben.ui.theme.Error
import com.shijiben.ui.theme.PixelBorder
import com.shijiben.ui.theme.PixelButton
import com.shijiben.ui.theme.PixelOutlinedButton
import com.shijiben.ui.theme.PixelSurface
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary
import com.shijiben.ui.theme.TagColorPalette
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagEditorSheet(
    editing: TagEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (name: String, color: Int) -> Boolean,
    onDelete: suspend (TagEntity) -> Unit
) {
    var name by remember(editing?.id) { mutableStateOf(editing?.name ?: "") }
    var selectedColor by remember(editing?.id) {
        mutableStateOf(editing?.color ?: TagColorPalette.first().toArgb())
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }
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
                text = if (editing != null) "编辑标签" else "新建标签",
                style = MaterialTheme.typography.titleLarge,
                color = PixelText
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("标签名称") },
                singleLine = true,
                shape = RectangleShape,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "选择颜色",
                fontSize = 13.sp,
                color = PixelTextSecondary
            )
            // 3x4 颜色网格（每行 4 个，共 3 行）
            val rows = TagColorPalette.chunked(4)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowColors.forEach { color ->
                            val argb = color.toArgb()
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(color)
                                    .then(
                                        if (argb == selectedColor) Modifier.border(3.dp, PixelBorder)
                                        else Modifier.border(2.dp, PixelBorder)
                                    )
                                    .clickable { selectedColor = argb }
                            )
                        }
                        // 补齐最后一行（如果不足 4 个）
                        repeat(4 - rowColors.size) {
                            Spacer(Modifier.size(48.dp))
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editing != null) {
                    PixelOutlinedButton(
                        text = "删除",
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.weight(1f).height(48.dp)
                    )
                }
                PixelOutlinedButton(
                    text = "取消",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f).height(48.dp)
                )
                PixelButton(
                    text = "保存",
                    onClick = {
                        scope.launch {
                            val ok = onSave(name, selectedColor)
                            if (ok) onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }
            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    title = { Text("删除标签「${editing!!.name}」？") },
                    text = { Text("所有事件中该标签的关联将被清除，且无法恢复。") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteConfirm = false
                            scope.launch {
                                onDelete(editing!!)
                                onDismiss()
                            }
                        }) { Text("删除", color = Error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
