package com.shijiben.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Clear
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.data.local.EventEntity
import com.shijiben.feature.notes.NoteEditorSheet
import com.shijiben.feature.notes.NotesViewModel
import com.shijiben.feature.recording.RecordingSheet
import com.shijiben.feature.timeline.EventCard
import com.shijiben.feature.timeline.NoteRow
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.RainbowTrim
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary
import com.shijiben.ui.theme.TextTertiary
import java.util.Calendar
import java.util.TimeZone

@Composable
fun SearchScreen(
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val editingNote by notesViewModel.editing.collectAsStateWithLifecycle()

    var showEventSheet by remember { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
    var showNoteSheet by remember { mutableStateOf(false) }

    // now 快照：EventCard 仅对 in-progress 事件用 now 显示运行时长，搜索非主计时场景，不起定时器
    val now = remember { System.currentTimeMillis() }
    val focusRequester = remember { FocusRequester() }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部 8dp 彩虹条（与 TimelineScreen/HeatmapScreen 同款）
            RainbowTrim()
            // 顶栏：‹ 返回 + "搜索" 标题
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
                    text = "搜索",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 搜索框：2dp 黑边白底直角 + 清除×按钮
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceColor)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .border(2.dp, Color.Black)
                        .background(SurfaceColor)
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = state.query,
                        onValueChange = viewModel::onQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        ),
                        cursorBrush = SolidColor(Color.Black),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {}),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (state.query.isEmpty()) {
                                    Text(
                                        text = "搜索事/随笔...",
                                        color = TextTertiary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }
                if (state.query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            viewModel.onQueryChange("")
                            focusRequester.requestFocus()
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "清除",
                            tint = TextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 结果区
            if (state.isEmpty) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "没有相关记录",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = "试试其他关键词",
                            fontSize = 13.sp,
                            color = TextTertiary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    items(
                        items = state.items,
                        key = { it.sortKey.toString() + it.javaClass.simpleName }
                    ) { item ->
                        when (item) {
                            is SearchViewModel.SearchItem.EventItem -> EventCard(
                                event = item.event,
                                onClick = {
                                    editingEvent = item.event
                                    showEventSheet = true
                                },
                                onStart = {},
                                onStop = {},
                                onLongClick = {},
                                now = now
                            )
                            is SearchViewModel.SearchItem.NoteItem -> NoteRow(
                                note = item.note,
                                onClick = {
                                    notesViewModel.startEdit(item.note)
                                    showNoteSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // §八 R3 守卫：仅当 editingEvent != null 时渲染 RecordingSheet，杜绝 initNew() 误创建
        if (showEventSheet && editingEvent != null) {
            RecordingSheet(
                viewingDate = todayTriple(),
                editingEvent = editingEvent,
                onDismiss = { showEventSheet = false; editingEvent = null },
                onSaved = { showEventSheet = false; editingEvent = null }
            )
        }
        if (showNoteSheet) {
            NoteEditorSheet(
                editing = editingNote,
                onDismiss = { showNoteSheet = false; notesViewModel.closeSheet() },
                onSave = { content -> notesViewModel.save(content) },
                onDelete = { note -> notesViewModel.delete(note) }
            )
        }
    }
}

private fun todayTriple(): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    return Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}
