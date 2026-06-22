package com.shijiben

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.shijiben.feature.notes.NotesScreen
import com.shijiben.feature.tags.TagsScreen
import com.shijiben.feature.timeline.TimelineScreen
import com.shijiben.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 临时验证入口：T-10 会替换为正式 Navigation
                    var screen by remember { mutableStateOf("timeline") }
                    when (screen) {
                        "timeline" -> TimelineScreen(
                            onTagsClick = { screen = "tags" },
                            onNotesClick = { screen = "notes" }
                        )
                        "tags" -> TagsScreen(onBack = { screen = "timeline" })
                        "notes" -> NotesScreen(onBack = { screen = "timeline" })
                    }
                }
            }
        }
    }
}
