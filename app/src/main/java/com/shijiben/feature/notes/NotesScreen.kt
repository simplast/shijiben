package com.shijiben.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.data.local.NoteEntity
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.PixelCard
import com.shijiben.ui.theme.RainbowTrim
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary

@Composable
fun NotesScreen(
    onBack: () -> Unit,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val notes by viewModel.allNotes.collectAsStateWithLifecycle()
    val sheetOpen by viewModel.sheetOpen.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            RainbowTrim()
            // 顶栏：左返回 + 标题 + 右新增（对齐其他 5 屏标准结构：Row + SurfaceColor 背景 + 2dp 黑色分隔线）
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
                Text("随笔", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { viewModel.startCreate() }) {
                    Icon(Icons.Default.Add, contentDescription = "新增")
                }
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))
            // 内容区
            Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                Spacer(Modifier.height(8.dp))
                if (notes.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "还没有随笔，点右上角 + 记下此刻的想法",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteRow(note = note, onClick = { viewModel.startEdit(note) })
                        }
                    }
                }
            }
        }
        if (sheetOpen) {
            NoteEditorSheet(
                editing = editing,
                onDismiss = { viewModel.closeSheet() },
                onSave = { content -> viewModel.save(content) },
                onDelete = { note -> viewModel.delete(note) }
            )
        }
    }
}

@Composable
private fun NoteRow(note: NoteEntity, onClick: () -> Unit) {
    PixelCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp)
        ) {
            Text(
                note.content,
                color = TextPrimary,
                fontSize = 16.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                formatTimestamp(note.timestamp),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}
