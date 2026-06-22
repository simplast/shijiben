package com.shijiben

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shijiben.ui.theme.AppTheme
import com.shijiben.ui.theme.PixelButton
import com.shijiben.ui.theme.PixelCard
import com.shijiben.ui.theme.PixelOutlinedButton
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
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("事记本", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(24.dp))
                        PixelCard(
                            modifier = Modifier.width(220.dp).height(80.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("像素卡片示例")
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        PixelButton(
                            text = "记一笔",
                            onClick = {},
                            modifier = Modifier.width(160.dp).height(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        PixelOutlinedButton(
                            text = "取消",
                            onClick = {},
                            modifier = Modifier.width(160.dp).height(48.dp)
                        )
                    }
                }
            }
        }
    }
}
