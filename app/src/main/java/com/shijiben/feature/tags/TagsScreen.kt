package com.shijiben.feature.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.data.local.TagEntity
import com.shijiben.ui.theme.PixelBorder
import com.shijiben.ui.theme.PixelCard
import com.shijiben.ui.theme.PixelText
import com.shijiben.ui.theme.PixelTextSecondary

@Composable
fun TagsScreen(
    onBack: () -> Unit,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val tags by viewModel.tags.collectAsStateWithLifecycle()
    val sheetOpen by viewModel.sheetOpen.collectAsStateWithLifecycle()
    val editing by viewModel.editing.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            // 顶部栏
            PixelCard(modifier = Modifier.fillMaxWidth(), shadow = false) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "返回")
                    }
                    Text(
                        text = "标签",
                        style = MaterialTheme.typography.titleLarge,
                        color = PixelText
                    )
                    IconButton(onClick = { viewModel.startCreate() }) {
                        Icon(Icons.Default.Add, contentDescription = "新增")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (tags.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "还没有标签，点右上角 + 创建",
                        color = PixelTextSecondary,

                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tags, key = { it.id }) { tag ->
                        TagRow(tag = tag, onClick = { viewModel.startEdit(tag) })
                    }
                }
            }
        }
        if (sheetOpen) {
            TagEditorSheet(
                editing = editing,
                onDismiss = { viewModel.closeSheet() },
                onSave = { name, color -> viewModel.save(name, color) },
                onDelete = { tag -> viewModel.delete(tag) }
            )
        }
    }
}

@Composable
private fun TagRow(tag: TagEntity, onClick: () -> Unit) {
    PixelCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color(tag.color))
                    .border(2.dp, PixelBorder)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = tag.name,
                color = PixelText,

                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
