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

## F027 — 关键动作无触觉反馈，8-bit 像素质感缺一笔触觉印记
- status: DONE (cycle 27)
- evidence: app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt:140
- impact: M
- cycle: 27
- 问题：开始/停止/删除/快速添加都是"重要动作"，但点下后只有视觉反馈（按钮变色、列表变化），没有触觉反馈。8-bit 美学强调"按下即响应"的物理感，缺触觉则像素风失去一笔关键印记。
- 修复：在 TimelineScreen 顶层取 `LocalHapticFeedback.current`，关键动作各包一层：开始/停止/删除用 `HapticFeedbackType.LongPress`（强确认），快速添加提交用 `HapticFeedbackType.TextHandleMove`（轻确认，区分"创建"与"状态切换"语义）。
