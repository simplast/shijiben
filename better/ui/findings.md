# Findings: ui

## F001 — TimeVizScreen 的 PixelOutlinedButtonLocal 与共享组件漂移
- status: DONE (cycle 1)
- evidence: TimeVizScreen.kt:267,285; PixelComponents.kt:76,98
- impact: M

## F002 — disabled 颜色硬编码绕过令牌系统
- status: DONE (cycle 2)
- evidence: AppColors.kt:43; PixelComponents.kt:53,87,95; HeatmapScreen.kt:161,169,204,211; HeatmapYearScreen.kt:174,182,201,208
- impact: M

## F003 — NotesScreen 缺失 RainbowTrim 品牌条
- status: DONE (cycle 3)
- evidence: NotesScreen.kt:49,51；对比 SearchScreen.kt:76-79
- impact: M

## F004 — NotesScreen 标题字号（22sp）与其他 5 屏（16sp）不一致
- status: DONE (cycle 4)
- evidence: NotesScreen.kt:62 (MaterialTheme.typography.titleLarge)；AppTheme.kt:50-54 (titleLarge=22sp)；对比 HeatmapScreen.kt:76-81 / SettingsScreen.kt:151-156 / AboutScreen.kt:66-71 / TimeVizScreen.kt:91-96 / SearchScreen.kt:92-97
- impact: M

## F005 — NotesScreen 标题栏结构与 5 屏标准结构漂移
- status: DONE (cycle 5)
- evidence: NotesScreen.kt:52-67 (PixelCard 包裹 + Column padding=12dp)；PixelComponents.kt:25-42 (PixelCard 默认 1dp 灰边)；对比 SearchScreen.kt:79-100 / AboutScreen.kt:53-74 / TimeVizScreen.kt:78-99 (Row+SurfaceColor+2dp 黑色分隔线)
- impact: M
