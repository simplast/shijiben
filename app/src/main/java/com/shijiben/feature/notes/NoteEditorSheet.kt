package com.shijiben.feature.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.data.local.NoteEntity
import com.shijiben.ui.theme.PixelButton
import com.shijiben.ui.theme.PixelOutlinedButton
import com.shijiben.ui.theme.PixelSurface
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorSheet(
    editing: NoteEntity?,
    onDismiss: () -> Unit,
    onSave: suspend (String) -> Boolean,
    onDelete: suspend (NoteEntity) -> Unit
) {
    var content by remember(editing?.id) { mutableStateOf(editing?.content ?: "") }
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
                text = if (editing != null) "编辑随笔" else "记一笔随笔",
                style = MaterialTheme.typography.titleLarge,
                color = PixelText
            )
            if (editing != null) {
                Text(
                    text = "时间: ${formatTimestamp(editing.timestamp)}",
                    fontSize = 12.sp,
                    color = PixelTextSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("想了什么") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (editing != null) {
                    PixelOutlinedButton(
                        text = "删除",
                        onClick = {
                            scope.launch {
                                onDelete(editing)
                                onDismiss()
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
                    text = "保存",
                    onClick = {
                        scope.launch {
                            val ok = onSave(content)
                            if (ok) onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

internal fun formatTimestamp(ts: Long): String {
    val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(ts))
}
