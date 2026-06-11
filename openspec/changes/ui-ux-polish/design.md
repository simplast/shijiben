## Context

像素风格重设计已完成，但存在 UI/UX 问题：部分按钮尺寸过小、缺少无障碍标签、动画不一致、间距不统一。

## Goals / Non-Goals

**Goals:**
- 所有可交互元素尺寸 ≥ 44dp
- 图标按钮添加 contentDescription
- 统一按压反馈动画
- 统一间距系统 (4/8dp)
- 移除剩余英文文本

**Non-Goals:**
- 不添加暗色模式（单独 change）
- 不修改颜色系统
- 不添加新功能

## Decisions

### D1: 触摸目标尺寸

**选择**: 所有 PixelIconButton 最小尺寸 44dp

**理由**: Material Design 标准，确保可访问性

### D2: 无障碍标签

**选择**: PixelIconButton 添加 `contentDescription: String? = null` 参数

**理由**: 屏幕阅读器需要语义标签

### D3: 动画统一

**选择**: 按压动画使用 `spring(dampingRatio = 0.6f, stiffness = 300f)`，持续时间约 150-300ms

**理由**: Material Design 推荐的自然弹性曲线

### D4: 间距系统

**选择**: 使用 4dp 基数，间距值为 4/8/12/16/20/24/32/40/48

**理由**: Material Design 间距系统

## Risks / Trade-offs

- **尺寸增大可能影响布局**: 部分紧凑区域可能需要调整 → 逐步调整，保持视觉平衡
- **无障碍标签维护**: 需要确保中文标签准确 → 使用简单描述性文本
