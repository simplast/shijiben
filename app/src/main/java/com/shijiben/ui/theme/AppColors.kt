package com.shijiben.ui.theme

import androidx.compose.ui.graphics.Color

// 8-bit NES 风格高饱和色板
val PixelRed = Color(0xFFE84A3C)      // 鲜红
val PixelGreen = Color(0xFF4BC66D)    // 翠绿
val PixelPurple = Color(0xFF7B5BF5)   // 亮紫
val PixelPink = Color(0xFFF25CA2)     // 桃红
val PixelGold = Color(0xFFF2C94C)     // 金黄
val PixelIndigo = Color(0xFF3B82F6)   // 靛蓝

// 标签可选 12 色（基础 6 色 + 扩展 6 色）
val TagColorPalette = listOf(
    PixelRed, PixelGreen, PixelPurple, PixelPink, PixelGold, PixelIndigo,
    Color(0xFF2A9D8F),  // 青绿
    Color(0xFFE76F51),  // 橙红
    Color(0xFF8338EC),  // 深紫
    Color(0xFF3A86FF),  // 亮蓝
    Color(0xFFFB5607),  // 亮橙
    Color(0xFF06A77D),  // 森绿
)

// 背景与文字
val PixelBackground = Color(0xFFF5F0E8)  // 米白
val PixelSurface = Color(0xFFFFFBF2)     // 略浅表面
val PixelText = Color(0xFF2A2828)        // 深灰文字
val PixelTextSecondary = Color(0xFF6B6663)
val PixelBorder = Color(0xFF2A2828)      // 深灰边框
val PixelShadow = Color(0xFF2A2828)      // 硬阴影

// 时间条专用
val TimeBlockPast = PixelIndigo          // 已过去未记录 = 蓝色
val TimeBlockRecorded = PixelGreen       // 有记录 = 绿色
val TimeBlockFuture = PixelBackground    // 还未到 = 米白
val TimeBlockNowBorder = PixelRed        // 当前小时红色边框
