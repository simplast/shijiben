# Findings: ux

## F010 — DONE
- 文件：`app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt`
- 符号：`RecordingSheet` 操作按钮区的 `PixelButton(text="保存")`
- 问题：保存按钮始终可点（无 `enabled` 参数），title 为空时 `save()` 返回 false 但用户无任何反馈——点了没反应，不知道为什么
- 修复：加 `enabled = title.isNotBlank()`（与 save() 的 `title.trim().isEmpty()` 校验语义一致），复用既有 Disabled 令牌提供灰色视觉反馈
- 计划：`better/ux/plan-cycle-10.md`

## F017 — Timeline note click navigates away instead of editing inline
- status: DONE (cycle 17)
- evidence: app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt:275
- impact: M
