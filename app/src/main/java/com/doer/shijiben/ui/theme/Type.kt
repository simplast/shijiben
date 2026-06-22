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
// Pixel Typography System — 5 Styles, Strict Rules
// ============================================================
//
// Font strategy:
//   Press Start 2P → display numbers & labels (8-bit pixel aesthetic)
//   System default  → body text (Chinese readability)
//
// ============================================================

val PixelFont = FontFamily(Font(R.font.pressstart2p))

/** 32sp pixel font — hero numbers (timers, stats) */
val PixelDisplay = TextStyle(
    fontFamily = PixelFont,
    fontSize = 32.sp,
    fontWeight = FontWeight.Black,
    lineHeight = 38.sp,
    letterSpacing = 0.05.em,
)

/** 10sp pixel font — section headers, badges, labels */
val PixelLabel = TextStyle(
    fontFamily = PixelFont,
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 13.sp,
    letterSpacing = 0.12.em,
)

/** 16sp system font — event names, primary body */
val PixelBody = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 16.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 24.sp,
)

/** 12sp system font — meta, timestamps, secondary text */
val PixelBodySmall = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 12.sp,
    fontWeight = FontWeight.Normal,
    lineHeight = 17.sp,
)

/** 10sp system font — hints, placeholders, empty states */
val PixelCaption = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 10.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = 13.sp,
)

// ============================================================
// Material3 Typography Mapping
// ============================================================

val PixelTypography = Typography(
    displayLarge = PixelDisplay,
    displayMedium = PixelDisplay,
    displaySmall = PixelDisplay,
    headlineLarge = PixelDisplay,
    headlineMedium = PixelBody.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    headlineSmall = PixelBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleLarge = PixelBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = PixelBody.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    titleSmall = PixelBody.copy(fontWeight = FontWeight.Medium),
    bodyLarge = PixelBody,
    bodyMedium = PixelBodySmall,
    bodySmall = PixelCaption,
    labelLarge = PixelBodySmall.copy(fontWeight = FontWeight.Medium),
    labelMedium = PixelCaption,
    labelSmall = PixelLabel,
)
