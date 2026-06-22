package com.doer.shijiben.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// Pixel Design System — 16-Color Palette
// ============================================================
// Inspired by retro handhelds (GBA / NES).
// Tight, constrained, every color has a purpose.
// No alpha variants, no deprecated aliases, no legacy.
// ============================================================

// ── Core (Black & White) ──

val PixelBlack = Color(0xFF1A1A2E)    // Text, main borders
val PixelCream = Color(0xFFFFF8E1)    // Page background, surfaces

// ── Module Colors ──

val PixelSky = Color(0xFF6BC5F5)      // Overview hero card
val PixelPink = Color(0xFFFF6B9D)     // Active/in-progress card
val PixelCoral = Color(0xFFFF8A6B)    // Completed badge, delete actions
val PixelLavender = Color(0xFFC4B5E0) // Pending badge, input border
val PixelTeal = Color(0xFF4DC9B8)     // Completed section, success
val PixelAmber = Color(0xFFFFB347)    // Pending section border, warnings
val PixelMint = Color(0xFFE8F8F0)     // Completed card background
val PixelYellow = Color(0xFFFFD93D)   // Stars, blinking indicator, accents

// ── Neutrals ──

val PixelGray = Color(0xFF9A8E7E)     // Muted text, timestamps
val PixelGrayLight = Color(0xFFF5EEDF) // Dividers, hairline

// ── Semantic (used sparingly) ──

val PixelRed = Color(0xFFE53935)      // Destructive actions
val PixelGreen = Color(0xFF66BB6A)    // Completion indicators

// ============================================================
// Convenience aliases (self-documenting)
// ============================================================

val PixelBg = PixelCream
val PixelBorder = PixelBlack
val PixelText = PixelBlack
val PixelTextMuted = PixelGray
val PixelDivider = PixelGrayLight
