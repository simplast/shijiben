package com.doer.shijiben.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.doer.shijiben.R

// ============================================================
// DESIGN.md §3 — Typography System
// ============================================================
//
// Font strategy:
//   Press Start 2P → display/hero headings & numbers (8-bit pixel aesthetic)
//   FontFamily.Default → body/caption (Chinese text readability)
//
// ============================================================

// ── Font Family references ──
val PixelFont = FontFamily(Font(R.font.pressstart2p))  // 8-bit pixel font
private val DisplayFont = PixelFont   // pixel feel headings
private val BodyFont = FontFamily.Default  // Chinese body readable
private val MonoFont = PixelFont      // pixel numbers/timers

// ── 9-Level Type Scale ──
// Tokens: display / hero / h1 / h2 / h3 / body / body-sm / caption / nano

private val textDisplay = TextStyle(
    fontFamily = DisplayFont,
    fontSize = 40.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 46.sp,
    letterSpacing = (-0.02).em,
)

private val textHero = TextStyle(
    fontFamily = DisplayFont,
    fontSize = 32.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 38.sp,
    letterSpacing = (-0.015).em,
)

private val textH1 = TextStyle(
    fontFamily = DisplayFont,
    fontSize = 26.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 32.sp,
    letterSpacing = (-0.01).em,
)

private val textH2 = TextStyle(
    fontFamily = BodyFont,
    fontSize = 22.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 28.sp,
)

private val textH3 = TextStyle(
    fontFamily = BodyFont,
    fontSize = 18.sp,
    fontWeight = FontWeight.SemiBold,
    lineHeight = 24.sp,
)

private val textBody = TextStyle(
    fontFamily = BodyFont,
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 25.sp,
)

private val textBodySmall = TextStyle(
    fontFamily = BodyFont,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 21.sp,
)

private val textCaption = TextStyle(
    fontFamily = BodyFont,
    fontSize = 12.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 17.sp,
    letterSpacing = 0.01.em,
)

private val textNano = TextStyle(
    fontFamily = BodyFont,
    fontSize = 10.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 13.sp,
    letterSpacing = 0.02.em,
)

// ── Specialized Styles ──

/** DESIGN.md §3 core feature: uppercase + wide letter-spacing labels */
val LabelUppercase = TextStyle(
    fontFamily = BodyFont,
    fontSize = 10.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 13.sp,
    letterSpacing = 0.1.em,
)

/** 8-bit Pixel Display: large pixel numbers for overview/active cards */
val PixelDisplay = TextStyle(
    fontFamily = PixelFont,
    fontSize = 32.sp,
    fontWeight = FontWeight.Black,
    lineHeight = 38.sp,
    letterSpacing = 0.05.em,
)

/** 8-bit Pixel Label: tiny uppercase pixel font for section headers / meta */
val PixelLabel = TextStyle(
    fontFamily = PixelFont,
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 13.sp,
    letterSpacing = 0.12.em,
)

val Typography = Typography(
    displayLarge = textDisplay,
    displayMedium = textHero,
    displaySmall = textH1,
    headlineLarge = textH1,
    headlineMedium = textH2,
    headlineSmall = textH3,
    titleLarge = textH2,
    titleMedium = textH3,
    titleSmall = textBody.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = textBody,
    bodyMedium = textBodySmall,
    bodySmall = textCaption,
    labelLarge = textBodySmall.copy(fontWeight = FontWeight.Medium),
    labelMedium = textCaption,
    labelSmall = LabelUppercase,
)
