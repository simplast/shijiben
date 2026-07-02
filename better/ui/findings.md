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

## F018 — NoteEditorSheet 标题残留 MaterialTheme.typography.titleLarge 间接引用 + PixelText 旧别名（全 app 唯一 typography.* 调用点，与 6 屏显式 20sp Bold + TextPrimary 标准漂移）
- status: DONE (cycle 18)
- evidence: app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt:64
- impact: M

## F028 — TimeAllocationTab 条形图全红无排名视觉分层，"时间去向"缺少一眼可读的排行榜感
- status: DONE (cycle 28)
- evidence: app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationTab.kt:131
- impact: M
- cycle: 28
- 问题：所有 bar 都是 Primary 红色，10 个标题看上去都一样"显眼"，无法一眼看出哪几个是主要时间去向。8-bit 像素风的强项是"游戏排行榜"视觉，缺一笔就少了一份辨识度。
- 修复：(1) 引入 `rankColor(rank)` — 前 4 名暖色梯度（红/橙/黄/绿，热→冷），5+ 名冷色循环（青/紫/粉/柠檬绿），与像素调色板一致；(2) 前 3 名加 20dp 排名徽章（数字白字 + 黑边 + 排名色背景），4+ 名留等宽空白保持对齐；(3) `itemsIndexed` 替代 `items` 传 rank。

## F038 — NotesScreen/NoteEditorSheet 残留 Pixel* 旧别名 + PixelCard 默认 1dp 灰边偏离 8-bit 标准
- status: DONE (cycle 38)
- evidence:
  - app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt (PixelCard 默认 border)
  - app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt (PixelText/PixelTextSecondary + PixelCard)
  - app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt (PixelSurface/PixelTextSecondary)
- impact: M
- cycle: 38
- 问题：(1) PixelCard 默认 `borderWidth=1.dp, borderColor=Border`（浅灰 1dp），与全 app 8-bit 标准的 2dp 纯黑边框（SettingsRow/HeatmapDayCell/PixelArrowBox/BottomEntryBar 等）漂移，NotesScreen 随笔卡边框视觉偏弱。(2) NotesScreen 与 NoteEditorSheet 是全 app 仅剩的两个 feature 层文件仍用 Pixel*/PixelTextSecondary 旧别名（F018/F002 已清其余 6 屏），令牌系统未完全收口。
- 修复：
  1. PixelCard 默认 `borderColor=Color.Black, borderWidth=2.dp`——共享组件默认即 8-bit 标准，后续复用零漂移
  2. NotesScreen：PixelText→TextPrimary、PixelTextSecondary→TextSecondary、移除 FontFamily 未用 import + 清 3 处 Text 内残留空行
  3. NoteEditorSheet：PixelSurface→SurfaceColor、PixelTextSecondary→TextSecondary
- 测试：纯 UI 令牌替换，编译+单测全绿。
