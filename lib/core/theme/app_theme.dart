import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'app_colors.dart';

/// 8-bit 像素风格主题。
///
/// 标题使用 Press Start 2P（像素字体），正文使用 VT323（等宽复古字体）。
/// 浅色模式，米白背景，无圆角的硬边像素风。
class AppTheme {
  AppTheme._();

  /// 浅色主题。
  static ThemeData get light {
    final base = ThemeData.light(useMaterial3: true);

    return base.copyWith(
      scaffoldBackgroundColor: AppColors.background,
      colorScheme: base.colorScheme.copyWith(
        primary: AppColors.indigo,
        secondary: AppColors.amber,
        surface: AppColors.background,
        onSurface: AppColors.textPrimary,
        error: AppColors.darkRed,
        outline: AppColors.border,
      ),
      textTheme: _buildTextTheme(base.textTheme),
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.background,
        foregroundColor: AppColors.textPrimary,
        centerTitle: true,
        titleTextStyle: GoogleFonts.pressStart2p(
          fontSize: 14,
          color: AppColors.textPrimary,
        ),
        elevation: 0,
        scrolledUnderElevation: 0,
      ),
      cardTheme: base.cardTheme.copyWith(
        color: AppColors.background,
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.zero,
        ),
        margin: EdgeInsets.zero,
      ),
      elevatedButtonTheme: ElevatedButtonThemeData(
        style: ElevatedButton.styleFrom(
          backgroundColor: AppColors.indigo,
          foregroundColor: AppColors.background,
          shape: const RoundedRectangleBorder(
            borderRadius: BorderRadius.zero,
          ),
          elevation: 0,
        ),
      ),
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(
          borderRadius: BorderRadius.zero,
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.zero,
          borderSide: BorderSide(color: AppColors.border, width: 2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.zero,
          borderSide: BorderSide(color: AppColors.indigo, width: 2),
        ),
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.border,
        thickness: 2,
        space: 2,
      ),
    );
  }

  static TextTheme _buildTextTheme(TextTheme base) {
    return GoogleFonts.vt323TextTheme(base).copyWith(
      headlineLarge: GoogleFonts.pressStart2p(
        fontSize: 20,
        height: 1.6,
        color: AppColors.textPrimary,
      ),
      headlineMedium: GoogleFonts.pressStart2p(
        fontSize: 16,
        height: 1.6,
        color: AppColors.textPrimary,
      ),
      headlineSmall: GoogleFonts.pressStart2p(
        fontSize: 14,
        height: 1.6,
        color: AppColors.textPrimary,
      ),
      titleLarge: GoogleFonts.pressStart2p(
        fontSize: 12,
        height: 1.6,
        color: AppColors.textPrimary,
      ),
      titleMedium: GoogleFonts.pressStart2p(
        fontSize: 10,
        height: 1.6,
        color: AppColors.textPrimary,
      ),
      bodyLarge: GoogleFonts.vt323(
        fontSize: 20,
        height: 1.2,
        color: AppColors.textPrimary,
      ),
      bodyMedium: GoogleFonts.vt323(
        fontSize: 18,
        height: 1.2,
        color: AppColors.textPrimary,
      ),
      bodySmall: GoogleFonts.vt323(
        fontSize: 16,
        height: 1.2,
        color: AppColors.textPrimary,
      ),
    );
  }
}
