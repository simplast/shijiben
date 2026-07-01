package com.shijiben

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.shijiben.BuildConfig
import com.shijiben.navigation.AppNavHost
import com.shijiben.ui.debug.DebugOverlay
import com.shijiben.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // 启用 edge-to-edge：让 IME inset 正确传递给 Compose 层。
        // 配合 AndroidManifest 的 windowSoftInputMode="adjustResize" + 根 Surface 的 safeDrawingPadding，
        // 统一处理 statusBars + navigationBars + IME，让内容自动避开系统栏与输入法。
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // safeDrawingPadding = systemBars ∪ IME ∪ displayCutout 的并集 padding
                    // 由根节点统一消费，子节点不再需要单独 imePadding / statusBarsPadding
                    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
                        if (BuildConfig.DEBUG) {
                            DebugOverlay { AppNavHost() }
                        } else {
                            AppNavHost()
                        }
                    }
                }
            }
        }
    }
}
