package com.doer.shijiben.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// ============================================================
// Warm Gold Palette (DESIGN.md v2 aligned)
// Philosophy: Golden sunlight + warm cream, like morning sun through the window
// ============================================================

// ── Primary (Warm Gold) ──
val PrimaryGold = Color(0xFFF5C469)
val PrimaryGoldSoft = Color(0xFFFEF3D9)
val PrimaryGoldDark = Color(0xFFE5B050)
val PrimaryGoldDeep = Color(0xFFC9942E)

// ── Text ──
val TextDark = Color(0xFF1A1A2E)
val TextDarkSoft = Color(0xFF4A4A5E)
val TextOnPrimary = Color(0xFF1A1A2E)   // dark text on gold background for contrast compliance

// ── Accent ──
val AccentMint = Color(0xFF7DD3A8)
val AccentAmber = Color(0xFFFFB347)
val AccentCoral = Color(0xFFFF8A6B)
val AccentLavender = Color(0xFFC4B5E0)

// ── Warm Gray Scale ──
val WarmGray50 = Color(0xFFFFF9F0)   // page background (cream)
val WarmGray100 = Color(0xFFFDF5E8)  // card soft background
val WarmGray200 = Color(0xFFF5EEDF)  // dividers, light borders
val WarmGray300 = Color(0xFFE8DED0)  // disabled, placeholder
val WarmGray400 = Color(0xFFC4B8A8)  // secondary text
val WarmGray500 = Color(0xFF9A8E7E)  // auxiliary text
val WarmGray600 = Color(0xFF6B6258)  // body text
val WarmGray700 = Color(0xFF4A4A5E)  // title text
val WarmGray800 = Color(0xFF1A1A2E)  // main titles

// ── Surface ──
val SurfaceWhite = Color(0xFFFFFFFF)
val BorderWarm = Color(0x0F1A1A2E)          // rgba(26,26,46,0.06)
val BorderWarmLight = Color(0x0A1A1A2E)     // rgba(26,26,46,0.04)

// ── Semantic ──
val SuccessGreen = AccentMint    // #7DD3A8 — mint green for completion
val WarningAmber = AccentAmber   // #FFB347 — amber for warnings
val ErrorCoral = AccentCoral     // #FF8A6B — coral for errors
val InfoMint = AccentMint        // #7DD3A8 — info

// ── Active Gradient (Hero Card breathing background) ──
val ActiveGradientStart = Color(0xFFF5C469)  // Warm Gold
val ActiveGradientMiddle = Color(0xFFFFB347) // Amber
val ActiveGradientEnd = Color(0xFF7DD3A8)    // Mint

// ── Status Accent Bars (EventRowWithDelete left bar) ──
val StatusPending = AccentAmber       // #FFB347 — "needs action" warmth
val StatusActive = PrimaryGoldDark    // #E5B050 — active warm gold
val StatusCompleted = AccentMint      // #7DD3A8 — done mint green

// ── Warning / Error ──
val WarningCoral = AccentCoral        // #FF8A6B

// ── Dark Mode ──
val DarkBgWarm = Color(0xFF1A1A2E)
val DarkSurfaceWarm = Color(0xFF262340)
val DarkTextWarm = Color(0xFFF1EFEB)
val DarkMutedWarm = Color(0xFF3A3850)

// ============================================================
// Deprecated tokens (keep backward compatibility via aliasing)
// ============================================================
@Deprecated("Use PrimaryGold instead", ReplaceWith("PrimaryGold"))
val PrimaryCoral = PrimaryGold

@Deprecated("Use PrimaryGoldSoft instead", ReplaceWith("PrimaryGoldSoft"))
val PrimaryCoralSoft = PrimaryGoldSoft

@Deprecated("Use PrimaryGoldDark instead", ReplaceWith("PrimaryGoldDark"))
val PrimaryCoralDark = PrimaryGoldDark

@Deprecated("Use AccentLavender instead", ReplaceWith("AccentLavender"))
val SecondaryIndigo = AccentLavender

@Deprecated("Use PrimaryGoldDark instead", ReplaceWith("PrimaryGoldDark"))
val AccentPurple = PrimaryGoldDark

@Deprecated("Use WarmGray50 instead", ReplaceWith("WarmGray50"))
val LightBackground = WarmGray50

@Deprecated("Use SurfaceWhite instead", ReplaceWith("SurfaceWhite"))
val LightSurface = SurfaceWhite

@Deprecated("Use TextDark instead", ReplaceWith("TextDark"))
val LightOnSurface = TextDark

@Deprecated("Use WarmGray200 instead", ReplaceWith("WarmGray200"))
val LightMuted = WarmGray200

@Deprecated("Use WarmGray100 instead", ReplaceWith("WarmGray100"))
val LightCardBg = SurfaceWhite

@Deprecated("Use BorderWarm instead", ReplaceWith("BorderWarm"))
val LightCardBorder = BorderWarm

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
@Deprecated("Use AccentMint instead", ReplaceWith("AccentMint"))
val AccentTeal = AccentMint

// ── 8-bit Colorful Palette ──
val PixelSkyBlue = Color(0xFF6BC5F5)     // Overview card bg
val PixelHotPink = Color(0xFFFF6B9D)     // Active event card bg
val PixelCoralRed = Color(0xFFFF8A6B)    // Completed badge / tags
val PixelLavender = Color(0xFFC4B5E0)    // Pending badge / input border
val PixelTeal = Color(0xFF4DC9B8)        // Completed color bar / tags
val PixelAmberOrange = Color(0xFFFFB347) // Pending border / submit btn
val PixelMintLight = Color(0xFFE8F8F0)   // Completed section bg
val PixelStarYellow = Color(0xFFFFD93D)  // Pixel star / decorations
val PixelDeepNavy = Color(0xFF1A1A2E)    // Main border color

// ── Pixel Border Tokens ──
val PixelBorder = PixelDeepNavy              // dark navy — main border
val PixelBorderLight = Color(0x331A1A2E)     // 20% alpha — light divider

// ── Pixel Shape (global 0dp corner = sharp rectangle) ──
val PixelShape = RoundedCornerShape(0.dp)
