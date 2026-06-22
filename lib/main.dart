import 'package:flutter/material.dart';
import 'package:google_fonts/google_fonts.dart';

import 'core/theme/app_colors.dart';
import 'core/theme/app_theme.dart';
import 'core/widgets/pixel_button.dart';
import 'core/widgets/pixel_card.dart';

void main() {
  runApp(const ShijibenApp());
}

class ShijibenApp extends StatelessWidget {
  const ShijibenApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: '事记本',
      theme: AppTheme.light,
      home: const PreviewPage(),
    );
  }
}

class PreviewPage extends StatelessWidget {
  const PreviewPage({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('事记本'),
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // 标题展示
            Text('8-bit 主题预览', style: Theme.of(context).textTheme.headlineMedium),
            const SizedBox(height: 8),
            Text(
              '柔和复古色板，像素边框，Press Start 2P 标题字体 + VT323 正文字体。',
              style: Theme.of(context).textTheme.bodyMedium,
            ),
            const SizedBox(height: 24),

            // 色板展示
            Text('色板', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: AppColors.tagColors.asMap().entries.map((entry) {
                return PixelCard(
                  color: entry.value,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  child: SizedBox(
                    width: 60,
                    child: Text(
                      '${entry.key + 1}',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: AppColors.background,
                        fontSize: 14,
                        fontFamily: GoogleFonts.pressStart2p().fontFamily,
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
            const SizedBox(height: 24),

            // 按钮展示
            Text('按钮', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            Wrap(
              spacing: 12,
              runSpacing: 12,
              children: [
                PixelButton(
                  label: '开始记录',
                  color: AppColors.indigo,
                  onPressed: () {},
                ),
                PixelButton(
                  label: '完成',
                  color: AppColors.forestGreen,
                  onPressed: () {},
                ),
                PixelButton(
                  label: '删除',
                  color: AppColors.darkRed,
                  onPressed: () {},
                ),
                PixelButton(
                  label: '禁用',
                  color: AppColors.border,
                  onPressed: null,
                ),
              ],
            ),
            const SizedBox(height: 24),

            // 卡片展示
            Text('卡片', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            PixelCard(
              borderColor: AppColors.indigo,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('读《奇特的一生》', style: Theme.of(context).textTheme.bodyLarge),
                  const SizedBox(height: 4),
                  Text(
                    '14:00 - 15:30  ·  1h30min',
                    style: TextStyle(
                      fontSize: 16,
                      color: AppColors.border,
                      fontFamily: GoogleFonts.vt323().fontFamily,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 8),
            PixelCard(
              borderColor: AppColors.forestGreen,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text('下午散步', style: Theme.of(context).textTheme.bodyLarge),
                  const SizedBox(height: 4),
                  Text(
                    '17:00 - 17:45  ·  45min',
                    style: TextStyle(
                      fontSize: 16,
                      color: AppColors.border,
                      fontFamily: GoogleFonts.vt323().fontFamily,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // 状态展示
            Text('事件状态', style: Theme.of(context).textTheme.titleLarge),
            const SizedBox(height: 12),
            Row(
              children: [
                PixelCard(
                  color: AppColors.amber.withValues(alpha: 0.3),
                  borderColor: AppColors.amber,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  child: Text('未开始', style: Theme.of(context).textTheme.bodyMedium),
                ),
                const SizedBox(width: 8),
                PixelCard(
                  color: AppColors.indigo.withValues(alpha: 0.3),
                  borderColor: AppColors.indigo,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  child: Text('进行中', style: Theme.of(context).textTheme.bodyMedium),
                ),
                const SizedBox(width: 8),
                PixelCard(
                  color: AppColors.forestGreen.withValues(alpha: 0.3),
                  borderColor: AppColors.forestGreen,
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                  child: Text('已完成', style: Theme.of(context).textTheme.bodyMedium),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
