## Why

像素风格重设计完成后，需要进行 UI/UX 打磨：修复触摸目标尺寸不达标、添加无障碍标签、统一动画风格、优化间距。这些问题影响用户体验和可访问性。

## What Changes

- **触摸目标**: 所有可交互元素尺寸 ≥ 44dp
- **无障碍**: 图标按钮添加 contentDescription
- **动画**: 统一按压反馈动画 (150-300ms)
- **间距**: 统一使用 4/8dp 间距系统
- **文本**: 移除剩余英文，全部使用中文

## Capabilities

### New Capabilities

- `accessibility`: 添加 contentDescription 和语义标签
- `touch-targets`: 确保所有触摸目标 ≥ 44dp

### Modified Capabilities

（无现有 spec 需要修改）

## Impact

- `PixelComponents.kt` — PixelIconButton 添加 contentDescription 参数
- `HomeScreen.kt` — 按钮尺寸和间距调整
- `EventEditorScreen.kt` — 按钮尺寸调整
- `DayPickerDialog.kt` — 按钮尺寸调整
- `MonthlyReviewScreen.kt` — 按钮尺寸调整
- `WeeklyReviewScreen.kt` — 按钮尺寸调整
