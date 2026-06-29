# Findings: ui

## F001 — TimeVizScreen 的 PixelOutlinedButtonLocal 与共享组件漂移
- status: DONE (cycle 1)
- evidence: TimeVizScreen.kt:267,285; PixelComponents.kt:76,98
- impact: M

## F002 — disabled 颜色硬编码绕过令牌系统
- status: DONE (cycle 2)
- evidence: AppColors.kt:43; PixelComponents.kt:53,87,95; HeatmapScreen.kt:161,169,204,211; HeatmapYearScreen.kt:174,182,201,208
- impact: M
