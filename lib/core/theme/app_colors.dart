import 'package:flutter/material.dart';

/// 8-bit 活泼复古色板。
///
/// 高饱和度 NES 风格，鲜艳明快，适合像素风格界面。
/// 标签色取自 [tagColors]，可按索引循环选用。
class AppColors {
  AppColors._();

  // --- 标签色板（活泼鲜艳） ---
  static const Color darkRed = Color(0xFFE83838); // 鲜红
  static const Color indigo = Color(0xFF3858E8); // 亮蓝
  static const Color forestGreen = Color(0xFF38B858); // 翠绿
  static const Color amber = Color(0xFFF0A820); // 暖橙
  static const Color violet = Color(0xFF9838E8); // 亮紫
  static const Color slateBlue = Color(0xFF20B8E8); // 青蓝
  static const Color rose = Color(0xFFFF4878); // 桃红
  static const Color olive = Color(0xFF98D038); // 黄绿
  static const Color deepBlue = Color(0xFF3030C8); // 深蓝
  static const Color brickRed = Color(0xFFF06020); // 橘红
  static const Color mint = Color(0xFF38E0B0); // 薄荷
  static const Color sandGold = Color(0xFFF0D020); // 金黄

  // --- 背景色 ---
  static const Color background = Color(0xFFF5F0E8); // 米白

  // --- 文字色 ---
  static const Color textPrimary = Color(0xFF2A2420); // 深棕

  // --- 边框色 ---
  static const Color border = Color(0xFF3A3838); // 深灰

  // --- 标签色列表，方便按索引选择 ---
  static const List<Color> tagColors = <Color>[
    darkRed,
    indigo,
    forestGreen,
    amber,
    violet,
    slateBlue,
    rose,
    olive,
    deepBlue,
    brickRed,
    mint,
    sandGold,
  ];
}
