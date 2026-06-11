package com.doer.shijiben.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================
// Pixel UI Design System — Color Palette
// ============================================================
// Philosophy: Summer seaside — sea blue + mint green base,
// sun yellow + coral orange accents, cream white background.
// Deep teal replaces pure black for a softer, warmer feel.
// ============================================================

// ── Primary (Sea Blue Series) ──
// Main brand colors — use for primary actions, headers, key elements

val SeaBlue = Color(0xFF4DB6AC)          // 海蓝 — 主色、大面积使用
val SeaBlueLight = Color(0xFF80CBC4)     // 浅海蓝 — 悬停态、次强调
val SeaBlueDark = Color(0xFF3BA99C)      // 深海蓝 — 按压态、深背景

val LightSeaBlue = SeaBlueLight     // 浅海蓝 — 卡片背景 (alias for SeaBlueLight)
val MintBlue = Color(0xFFB2DFDB)         // 薄荷蓝 — 浅色背景
val MistBlue = Color(0xFFE0F2F1)         // 雾蓝 — 最浅背景/分割线

// ── Accent (Sun & Beach) ──
// Used sparingly for highlights, warnings, and decorative elements

val SunYellow = Color(0xFFFFD54F)        // 日光黄 — 强调、装饰
val SunYellowLight = Color(0xFFFFEE58)   // 浅日光黄 — 悬停态
val SunYellowDark = Color(0xFFFFCA28)    // 深日光黄 — 按压态

val CoralOrange = Color(0xFFFF8A65)      // 珊瑚橙 — 重要操作、警告
val CoralOrangeLight = Color(0xFFFFAB91) // 浅珊瑚橙 — 悬停态
val CoralOrangeDark = Color(0xFFFF7043)  // 深珊瑚橙 — 按压态

// ── Special Status Color ──
// Use sparingly — only for "in progress" state

val LightPink = Color(0xFFF48FB1)        // 淡粉 — 仅用于"进行中"状态
val LightPinkLight = Color(0xFFFFB2D0)   // 浅粉
val LightPinkDark = Color(0xFFF06292)    // 深粉

// ── Base Colors ──

val CreamWhite = Color(0xFFFFF8E1)       // 奶油白 — 页面背景
val DeepTeal = Color(0xFF263238)         // 深青蓝 — 文字/边框（替代纯黑）

// ── Text Colors ──

val TextPrimary = DeepTeal               // 主要文字
val TextSecondary = Color(0xFF546E7A)    // 次要文字
val TextMuted = Color(0xFF90A4AE)        // 辅助文字
val TextOnPrimary = Color.White          // 主色背景上的文字

// ── Warm Gray Scale (refreshed for seaside palette) ──
// Keeps warm undertones but shifted to match cream/teal base

val WarmGray50 = CreamWhite              // 页面背景（奶油白）
val WarmGray100 = Color(0xFFFFF3D6)      // 卡片软背景
val WarmGray200 = Color(0xFFF5E6C8)      // 分割线、浅边框
val WarmGray300 = Color(0xFFE8D5B0)      // 禁用态、占位符
val WarmGray400 = Color(0xFFC4A77D)      // 次要文字
val WarmGray500 = Color(0xFF8D6E63)      // 辅助文字
val WarmGray600 = Color(0xFF6D4C41)      // 正文
val WarmGray700 = Color(0xFF5D4037)      // 标题文字
val WarmGray800 = DeepTeal               // 主标题

// ── Surface & Border ──

val SurfaceWhite = Color(0xFFFFFFFF)
val SurfaceCream = CreamWhite
val BorderColor = DeepTeal
val BorderColorLight = Color(0x1A263238) // 10% alpha deep teal
val BorderColorMuted = MistBlue

// ============================================================
// Semantic Color Tokens
// ============================================================
// Unified semantic names — use these in component code
// instead of raw color values for better maintainability.
// ============================================================

object SemanticColors {
    // Status / Feedback
    val Success = SeaBlue                 // 成功、完成
    val SuccessLight = SeaBlueLight
    val SuccessDark = SeaBlueDark

    val Warning = SunYellow               // 警告、注意
    val WarningLight = SunYellowLight
    val WarningDark = SunYellowDark

    val Error = CoralOrange               // 错误、危险
    val ErrorLight = CoralOrangeLight
    val ErrorDark = CoralOrangeDark

    val Info = SeaBlue                    // 信息、提示
    val InfoLight = SeaBlueLight
    val InfoDark = SeaBlueDark

    // Task / Event Status
    val Active = LightPink                // 进行中
    val ActiveLight = LightPinkLight
    val ActiveDark = LightPinkDark

    val Pending = SunYellow               // 待办、待开始
    val PendingLight = SunYellowLight
    val PendingDark = SunYellowDark

    val Completed = SeaBlue               // 已完成
    val CompletedLight = SeaBlueLight
    val CompletedDark = SeaBlueDark
}

// Convenience aliases (flat namespace for backward compatibility style)
val SuccessColor = SemanticColors.Success
val WarningColor = SemanticColors.Warning
val ErrorColor = SemanticColors.Error
val InfoColor = SemanticColors.Info
val ActiveColor = SemanticColors.Active
val PendingColor = SemanticColors.Pending

// ============================================================
// Active Gradient (Hero Card breathing background)
// ============================================================

val ActiveGradientStart = SeaBlue
val ActiveGradientMiddle = LightSeaBlue
val ActiveGradientEnd = MintBlue

// ============================================================
// Status Accent Bars (EventRowWithDelete left bar)
// ============================================================

val StatusPending = SunYellow
val StatusActive = LightPink
val StatusCompleted = SeaBlue

// ============================================================
// Dark Mode (placeholder — future work)
// ============================================================

val DarkBgWarm = Color(0xFF1A1A2E)
val DarkSurfaceWarm = Color(0xFF262340)
val DarkTextWarm = Color(0xFFF1EFEB)
val DarkMutedWarm = Color(0xFF3A3850)

// ============================================================
// Previous Warm Gold Palette (kept for backward compatibility)
// ============================================================

// ── Primary (Warm Gold) ──
val PrimaryGold = Color(0xFFF5C469)
val PrimaryGoldSoft = Color(0xFFFEF3D9)
val PrimaryGoldDark = Color(0xFFE5B050)
val PrimaryGoldDeep = Color(0xFFC9942E)

// ── Text (legacy) ──
val TextDark = Color(0xFF1A1A2E)
val TextDarkSoft = Color(0xFF4A4A5E)
val TextOnPrimaryLegacy = Color(0xFF1A1A2E)

// ── Accent (legacy) ──
val AccentMint = Color(0xFF7DD3A8)
val AccentAmber = Color(0xFFFFB347)
val AccentCoral = Color(0xFFFF8A6B)
val AccentLavender = Color(0xFFC4B5E0)

// ── Semantic (legacy) ──
val SuccessGreen = AccentMint
val WarningAmber = AccentAmber
val ErrorCoral = AccentCoral
val InfoMint = AccentMint
val WarningCoral = AccentCoral

// ── Status (legacy) ──
val StatusPendingLegacy = AccentAmber
val StatusActiveLegacy = PrimaryGoldDark
val StatusCompletedLegacy = AccentMint

// ============================================================
// Deprecated tokens (keep backward compatibility via aliasing)
// ============================================================
@Deprecated("Use SeaBlue or PrimaryGold instead", ReplaceWith("PrimaryGold"))
val PrimaryCoral = PrimaryGold

@Deprecated("Use MintBlue or PrimaryGoldSoft instead", ReplaceWith("PrimaryGoldSoft"))
val PrimaryCoralSoft = PrimaryGoldSoft

@Deprecated("Use SeaBlueDark or PrimaryGoldDark instead", ReplaceWith("PrimaryGoldDark"))
val PrimaryCoralDark = PrimaryGoldDark

@Deprecated("Use LightSeaBlue or AccentLavender instead", ReplaceWith("AccentLavender"))
val SecondaryIndigo = AccentLavender

@Deprecated("Use SeaBlueDark or PrimaryGoldDark instead", ReplaceWith("PrimaryGoldDark"))
val AccentPurple = PrimaryGoldDark

@Deprecated("Use WarmGray50 instead", ReplaceWith("WarmGray50"))
val LightBackground = WarmGray50

@Deprecated("Use SurfaceWhite instead", ReplaceWith("SurfaceWhite"))
val LightSurface = SurfaceWhite

@Deprecated("Use TextPrimary or TextDark instead", ReplaceWith("TextDark"))
val LightOnSurface = TextDark

@Deprecated("Use WarmGray200 instead", ReplaceWith("WarmGray200"))
val LightMuted = WarmGray200

@Deprecated("Use SurfaceWhite instead", ReplaceWith("SurfaceWhite"))
val LightCardBg = SurfaceWhite

@Deprecated("Use BorderColorLight instead", ReplaceWith("BorderColorLight"))
val LightCardBorder = Color(0x0F1A1A2E)

@Deprecated("Use DarkBgWarm instead", ReplaceWith("DarkBgWarm"))
val DarkBackground = DarkBgWarm

@Deprecated("Use DarkSurfaceWarm instead", ReplaceWith("DarkSurfaceWarm"))
val DarkSurface = DarkSurfaceWarm

@Deprecated("Use DarkTextWarm instead", ReplaceWith("DarkTextWarm"))
val DarkOnSurface = DarkTextWarm

@Deprecated("Use DarkMutedWarm instead", ReplaceWith("DarkMutedWarm"))
val DarkMuted = DarkMutedWarm

@Deprecated("Use DarkSurfaceWarm instead", ReplaceWith("DarkSurfaceWarm"))
val DarkCardBg = DarkSurfaceWarm

@Deprecated("Use DarkMutedWarm instead", ReplaceWith("DarkMutedWarm"))
val DarkCardBorder = DarkMutedWarm

// Old accent aliases
@Deprecated("Use AccentMint or SeaBlue instead", ReplaceWith("AccentMint"))
val AccentTeal = AccentMint

// ============================================================
// 8-bit Colorful Palette (kept — do not delete)
// ============================================================
// These are still used across the app. They will be gradually
// replaced with the new semantic tokens.

val PixelSkyBlue = Color(0xFF6BC5F5)     // Overview card bg
val PixelHotPink = Color(0xFFFF6B9D)     // Active event card bg
val PixelCoralRed = Color(0xFFFF8A6B)    // Completed badge / tags
val PixelLavender = Color(0xFFC4B5E0)    // Pending badge / input border
val PixelTeal = Color(0xFF4DC9B8)        // Completed color bar / tags
val PixelAmberOrange = Color(0xFFFFB347) // Pending border / submit btn
val PixelMintLight = Color(0xFFE8F8F0)   // Completed section bg
val PixelStarYellow = Color(0xFFFFD93D)  // Pixel star / decorations
val PixelDeepNavy = Color(0xFF1A1A2E)    // Main border color

// ── Pixel Border Tokens (legacy) ──
val PixelBorder = PixelDeepNavy              // dark navy — main border
val PixelBorderLight = Color(0x331A1A2E)     // 20% alpha — light divider

// ── Pixel Shape (global 0dp corner = sharp rectangle) ──
val PixelShape = RoundedCornerShape(0.dp)
