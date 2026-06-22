import 'package:flutter/material.dart';

import 'app_colors.dart';

/// 像素风格硬边框组件。
///
/// 使用 [CustomPaint] 的前景景画板在子组件之上绘制无圆角的硬边框，
/// 关闭抗锯齿以呈现 8-bit 像素质感。拐角为直角，边线由填充矩形拼合而成。
class PixelBorder extends StatelessWidget {
  const PixelBorder({
    super.key,
    required this.child,
    this.color = AppColors.border,
    this.width = 2.0,
  });

  /// 边框颜色。
  final Color color;

  /// 边框线宽（像素）。
  final double width;

  /// 包裹的子组件。
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      foregroundPainter: _PixelBorderPainter(
        color: color,
        width: width,
      ),
      child: child,
    );
  }
}

class _PixelBorderPainter extends CustomPainter {
  const _PixelBorderPainter({
    required this.color,
    required this.width,
  });

  final Color color;
  final double width;

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color
      ..style = PaintingStyle.fill
      ..isAntiAlias = false;

    final w = width;

    // 上边
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, w), paint);
    // 下边
    canvas.drawRect(
      Rect.fromLTWH(0, size.height - w, size.width, w),
      paint,
    );
    // 左边
    canvas.drawRect(
      Rect.fromLTWH(0, w, w, size.height - 2 * w),
      paint,
    );
    // 右边
    canvas.drawRect(
      Rect.fromLTWH(size.width - w, w, w, size.height - 2 * w),
      paint,
    );
  }

  @override
  bool shouldRepaint(covariant _PixelBorderPainter oldDelegate) {
    return oldDelegate.color != color || oldDelegate.width != width;
  }
}
