package com.shijiben.ui.theme

import androidx.compose.ui.graphics.Color

// 配色方案：彩虹色 + 黑白灰
//
// 彩虹色（高饱和）：蓝、橙、绿、紫、粉、青、红、黄
// 黑白灰：纯黑、纯白、不同明度的灰色

// ==================== 彩虹色主色调 ====================
val Primary = Color(0xFFEF4444)        // 鲜艳红（替代蓝色）
val PrimaryLight = Color(0xFFFCA5A5)   // 浅红
val PrimaryDark = Color(0xFFDC2626)    // 深红

val Accent = Color(0xFFF97316)         // 亮橙
val AccentLight = Color(0xFFFDBA74)    // 浅橙（比之前更鲜艳）

val Secondary = Color(0xFF10B981)      // 翠绿
val SecondaryLight = Color(0xFF6EE7B7) // 浅绿（比之前更鲜艳）

// ==================== 彩虹辅助色 ====================
val RainbowOrange = Color(0xFFF97316)
val RainbowPurple = Color(0xFFA855F7)
val RainbowCyan = Color(0xFF06B6D4)
val RainbowPink = Color(0xFFEC4899)
val RainbowAmber = Color(0xFFF59E0B)
val RainbowLime = Color(0xFF84CC16)

// ==================== 黑白灰背景 ====================
val Background = Color(0xFFF8F9FA)     // 极浅灰
val Surface = Color(0xFFFFFFFF)        // 纯白

// ==================== 黑白灰文字 ====================
val TextPrimary = Color(0xFF000000)    // 纯黑
val TextSecondary = Color(0xFF6B7280)  // 中灰
val TextTertiary = Color(0xFF9CA3AF)   // 浅灰
val TextOnPrimary = Color(0xFFFFFFFF)  // 白
val TextOnSurface = Color(0xFF000000)  // 纯黑

// ==================== 黑白灰边框 ====================
val Border = Color(0xFFE5E7EB)        // 浅灰边框
val BorderLight = Color(0xFFF3F4F6)   // 极浅灰边框

// ==================== 彩虹状态色 ====================
val Error = Color(0xFFEF4444)          // 红
val ErrorLight = Color(0xFFFCA5A5)    // 浅红
val Success = Color(0xFF10B981)        // 绿
val Warning = Color(0xFFF59E0B)        // 黄

// ==================== 时间条 ====================
val TimeBlockPast = PrimaryLight               // 浅红（已过去未记录，统一 to 主色）
val TimeBlockRecorded = Color(0xFF6EE7B7)     // 浅绿（有记录）
val TimeBlockFuture = BorderLight             // 极浅灰（未来）
val TimeBlockNowBorder = Error                // 红色边框（当前小时）
val TimeBlockNowBackground = Color(0xFFFDE68A) // 暖黄（当前小时背景）

// ==================== 彩虹小时色板（8 色循环，24h × 3 轮） ====================
val RainbowHourColors = listOf(
    Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
    Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
    Color(0xFF6366F1), Color(0xFFA855F7),
)

// ==================== 彩虹滑块色板（6 色循环，无蓝色） ====================
val SliderRainbowActive = listOf(
    Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
    Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFFA855F7),
)
val SliderRainbowInactive = listOf(
    Color(0xFFFCA5A5), Color(0xFFFDBA74), Color(0xFFFDE68A),
    Color(0xFFBEF264), Color(0xFF86EFAC), Color(0xFFD8B4FE),
)

// ==================== 向后兼容别名 ====================
val PixelRed = Error
val PixelGreen = Secondary
val PixelPurple = Color(0xFF8B5CF6)
val PixelPink = Color(0xFFEC4899)
val PixelGold = Accent
val PixelIndigo = Primary
val PixelBackground = Background
val PixelSurface = Surface
val PixelText = TextPrimary
val PixelTextSecondary = TextSecondary
val PixelBorder = Border
val PixelShadow = Color(0xFF2A2828)
