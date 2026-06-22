import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import '../theme/app_colors.dart';
import '../theme/pixel_border.dart';

/// 8-bit 像素风格按钮。
///
/// 使用 [PixelBorder] 绘制硬边框，无圆角，附带无模糊的硬阴影。
/// 按下时阴影消失并向下偏移，模拟像素游戏的按压反馈。
class PixelButton extends StatefulWidget {
  const PixelButton({
    super.key,
    required this.label,
    this.onPressed,
    this.color = AppColors.indigo,
    this.textColor = AppColors.background,
    this.borderColor = AppColors.border,
    this.shadowColor = AppColors.border,
    this.shadowOffset = const Offset(4, 4),
    this.padding = const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
  });

  final String label;
  final VoidCallback? onPressed;
  final Color color;
  final Color textColor;
  final Color borderColor;
  final Color shadowColor;
  final Offset shadowOffset;
  final EdgeInsets padding;

  @override
  State<PixelButton> createState() => _PixelButtonState();
}

class _PixelButtonState extends State<PixelButton> {
  bool _pressed = false;

  bool get _disabled => widget.onPressed == null;

  void _setPressed(bool value) {
    if (_disabled) return;
    setState(() => _pressed = value);
  }

  @override
  Widget build(BuildContext context) {
    final pressed = _pressed && !_disabled;
    final offset = pressed ? Offset.zero : widget.shadowOffset;

    return GestureDetector(
      onTapDown: (_) => _setPressed(true),
      onTapUp: (_) => _setPressed(false),
      onTapCancel: () => _setPressed(false),
      onTap: widget.onPressed,
      child: PixelBorder(
        color: widget.borderColor,
        width: 2,
        child: AnimatedContainer(
          duration: const Duration(milliseconds: 60),
          transform: Matrix4.translationValues(offset.dx, offset.dy, 0),
          decoration: BoxDecoration(
            color: _disabled ? widget.color.withValues(alpha: 0.5) : widget.color,
            boxShadow: pressed
                ? []
                : [
                    BoxShadow(
                      color: widget.shadowColor,
                      offset: Offset(-widget.shadowOffset.dx, -widget.shadowOffset.dy),
                      blurRadius: 0,
                    ),
                  ],
          ),
          child: Padding(
            padding: widget.padding,
            child: Text(
              widget.label,
              style: GoogleFonts.pressStart2p(
                fontSize: 10,
                height: 1.4,
                color: _disabled
                    ? widget.textColor.withValues(alpha: 0.5)
                    : widget.textColor,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
