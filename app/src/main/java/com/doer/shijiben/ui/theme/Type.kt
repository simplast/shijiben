package com.doer.shijiben.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// ============================================================
// DESIGN.md §3 — Typography System
// ============================================================
//
// Font strategy:
//   Outfit   → display/hero headings (geometric warmth)
//   Inter    → body/caption (reading comfort)
//   JetBrains Mono → numbers/timers (scanability)
//
// Currently using FontFamily.Default (Roboto) as a fallback.
// To enable the full typeface experience, download the font files
// from Google Fonts (OFL licensed) and place them in res/font/,
// then replace the FontFamily references below.
//
//   Outfit: https://fonts.google.com/specimen/Outfit
//   Inter:  https://fonts.google.com/specimen/Inter
//   JetBrains Mono: https://fonts.google.com/specimen/JetBrains+Mono
// ============================================================

// ── Font Family references (swappable) ──
private val DisplayFont = FontFamily.Default   // → Outfit
private val BodyFont = FontFamily.Default      // → Inter
private val MonoFont = FontFamily.Default      // → JetBrains Mono

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
