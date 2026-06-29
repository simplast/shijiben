package com.shijiben.feature.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.Error
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary
import com.shijiben.ui.theme.TextTertiary
import com.shijiben.ui.theme.Disabled
import com.shijiben.ui.theme.RainbowTrim

/**
 * 设置页。Stateless composable + ExportViewModel + ImportViewModel（hiltViewModel 在 NavHost 内可用）。
 * 入口列表：「关于事记本」「隐私政策」「数据导出」「数据导入」。
 * 数据导出：SAF CreateDocument("application/json") → ExportViewModel.export(stream, name) → Snackbar。
 * 数据导入：确认对话框 → SAF OpenDocument → ImportViewModel.import(stream) → Snackbar。
 *
 * 已知 aesthetic 偏差：Snackbar 用 Material3 默认样式（圆角），与全 app 直角 8-bit 风不完全一致；
 * 瞬态反馈（2-3s），引入自定义方形 Snackbar 成本高于收益，spec 接受默认。
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
    importViewModel: ImportViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val importState by importViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // SAF launcher + pendingName：onClick 时写、回调里读，保证 launcher.launch 建议名
    // 与 export(stream, name) 传入名一致（避免两次 generateFileName() 产生不同时间戳）。
    var pendingName by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val name = pendingName ?: ExportViewModel.generateFileName()
        if (uri != null) {
            val stream = context.contentResolver.openOutputStream(uri)
            if (stream != null) viewModel.export(stream, name)
            else viewModel.markError()
        }
        pendingName = null
    }

    // 导入：OpenDocument 选文件 → openInputStream → importVm.import(stream)
    var showImportConfirm by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) importViewModel.import(stream)
            else importViewModel.markError()
        }
    }

    // 消费一次性状态：Success/Error → Snackbar + resetState
    LaunchedEffect(state) {
        when (val s = state) {
            is ExportViewModel.ExportState.Success -> {
                snackbarHostState.showSnackbar("已导出到 ${s.fileName}")
                viewModel.resetState()
            }
            is ExportViewModel.ExportState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportViewModel.ImportState.Success -> {
                val msg = buildString {
                    append("已导入 ${s.events} 条事件、${s.notes} 条笔记")
                    if (s.prefsUpdated) append("（含偏好）")
                }
                snackbarHostState.showSnackbar(msg)
                importViewModel.resetState()
            }
            is ImportViewModel.ImportState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                importViewModel.resetState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部 8dp 彩虹条（与首页/timeviz/heatmap 一致）
            RainbowTrim()
            // 顶栏：左返回 + 标题「设置」
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
                    text = "设置",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 设置项列表
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsRow(title = "关于事记本", onClick = onAboutClick)
                SettingsRow(title = "隐私政策", onClick = onPrivacyClick)
                SettingsRow(
                    title = "数据导出",
                    enabled = state !is ExportViewModel.ExportState.Exporting &&
                        importState !is ImportViewModel.ImportState.Importing,
                    onClick = {
                        val name = ExportViewModel.generateFileName()
                        pendingName = name
                        launcher.launch(name)
                    },
                    trailing = {
                        if (state is ExportViewModel.ExportState.Exporting) {
                            Text("导出中...", fontSize = 12.sp, color = TextTertiary)
                        } else {
                            Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
                        }
                    }
                )
                SettingsRow(
                    title = "数据导入",
                    enabled = state !is ExportViewModel.ExportState.Exporting &&
                        importState !is ImportViewModel.ImportState.Importing,
                    onClick = { showImportConfirm = true },
                    trailing = {
                        if (importState is ImportViewModel.ImportState.Importing) {
                            Text("导入中...", fontSize = 12.sp, color = TextTertiary)
                        } else {
                            Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
                        }
                    }
                )
            }
        }
        // Snackbar 在最外层 Box 底部（瞬态反馈，不阻塞列表）
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        if (showImportConfirm) {
            AlertDialog(
                onDismissRequest = { showImportConfirm = false },
                title = { Text("导入数据？", color = TextPrimary) },
                text = { Text("导入将覆盖同 ID 数据，确认？", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/json"))
                    }) { Text("导入", color = Error) }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirm = false }) {
                        Text("取消", color = TextSecondary)
                    }
                }
            )
        }
    }
}


/** 8-bit 风格行项：2dp 黑边白底卡片 + 左标题 + 右尾标（默认 ›），padding 12dp，直角。 */
@Composable
private fun SettingsRow(
    title: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: @Composable (() -> Unit)? = null
) {
    Surface(
        color = if (enabled) SurfaceColor else Disabled,   // 导出中置灰
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) TextPrimary else TextTertiary
            )
            if (trailing != null) trailing()
            else Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
        }
    }
}
