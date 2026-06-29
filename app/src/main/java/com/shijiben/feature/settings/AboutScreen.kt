package com.shijiben.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shijiben.BuildConfig
import com.shijiben.ui.theme.Background
import com.shijiben.ui.theme.Surface as SurfaceColor
import com.shijiben.ui.theme.TextPrimary
import com.shijiben.ui.theme.TextSecondary
import com.shijiben.ui.theme.RainbowTrim
import com.shijiben.ui.theme.TextTertiary

/**
 * 关于页（Stateless composable）。无 ViewModel。
 * 区块 1：关于事记本（名称/简介/版本号——版本号取自 BuildConfig.VERSION_NAME）。
 * 区块 2：隐私政策（纯本地声明，无同意按钮，无 checkbox）。
 */
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    scrollToPrivacy: Boolean = false
) {
    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 顶部 8dp 彩虹条（与首页/timeviz/heatmap 一致）
            RainbowTrim()
            // 顶栏：左返回 + 标题「关于事记本」
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
                    text = "关于事记本",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
            }
            // 2dp 黑色分隔线
            Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color.Black))

            // 隐私政策是本页最后一个区块；从「隐私政策」入口进入时直接滚到底部，
            // 即可让隐私区块进入视口（比 onGloballyPositioned 取偏移更稳健）。
            val scrollState = rememberScrollState()
            LaunchedEffect(scrollToPrivacy) {
                if (scrollToPrivacy) {
                    withFrameNanos {}
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 区块 1：关于
                Section(title = "关于事记本") {
                    Text(
                        text = "事记本 ShiJiBen",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "一个纯粹的本地时间记录工具，受《奇特的一生》启发。",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "版本 ${BuildConfig.VERSION_NAME}",
                        fontSize = 12.sp,
                        color = TextTertiary
                    )
                }

                // 区块 2：隐私政策
                Section(title = "隐私政策") {
                    Text(
                        text = "事记本是一款纯本地时间记录应用。本隐私政策说明数据处理方式：",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    PrivacyBullet("不联网：App 不发起任何网络请求，无远程 API、云同步、推送、统计 SDK、广告 SDK。")
                    PrivacyBullet("不采集：不收集任何个人信息、设备信息、使用行为数据。")
                    PrivacyBullet("不分享、不分析：数据不离开本设备，无任何上传、共享、分析行为。")
                    PrivacyBullet("本机存储：所有事件、随笔、生日、寿命设置仅存于本机 Room 数据库与 SharedPreferences。")
                    PrivacyBullet("卸载即清除：卸载 App 后所有数据随之删除，无残留、无备份。")
                    PrivacyBullet("无账号：无需注册登录，无账号体系。")
                    PrivacyBullet("敏感数据说明：生日与假设寿命仅用于「时间可视化」页面的这一生剩余时间计算，存于本机 SharedPreferences，不出设备。")
                }
            }
        }
    }
}


/** 8-bit 卡片：2dp 黑边白底 + 直角 + 标题 + 内容，与 TimeVizCard 同风格。 */
@Composable
private fun Section(
    title: String,
    content: @Composable () -> Unit
) {
    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(0.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

/** 隐私政策条目：‣ + 正文。 */
@Composable
private fun PrivacyBullet(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            fontSize = 13.sp,
            color = TextTertiary,
            modifier = Modifier.padding(end = 6.dp)
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}
