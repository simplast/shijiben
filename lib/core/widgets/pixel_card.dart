import 'package:flutter/material.dart';

import '../theme/app_colors.dart';
import '../theme/pixel_border.dart';

/// 8-bit 像素风格卡片容器。
///
/// 使用 [PixelBorder] 绘制硬边框，无圆角，内部带内边距。
class PixelCard extends StatelessWidget {
  const PixelCard({
    super.key,
    required this.child,
    this.color = AppColors.background,
    this.borderColor = AppColors.border,
    this.borderWidth = 2.0,
    this.padding = const EdgeInsets.all(12.0),
  });

  final Widget child;
  final Color color;
  final Color borderColor;
  final double borderWidth;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    return PixelBorder(
      color: borderColor,
      width: borderWidth,
      child: ColoredBox(
        color: color,
        child: Padding(
          padding: padding,
          child: child,
        ),
      ),
    );
  }
}
