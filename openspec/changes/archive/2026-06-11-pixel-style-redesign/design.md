## Context

当前 PixelIcons.kt 用 Canvas drawLine/drawPath 画平滑线条图标，不是像素块。Type.kt 用 FontFamily.Monospace 而非 Press Start 2P。PixelIconButton 默认使用 tertiaryDoubleBorder。底部 QuickNameLine 三个元素样式不一致。

## Goals / Non-Goals

**Goals:**
- 所有图标改为 8x8 网格像素块绘制
- 添加 Press Start 2P 字体，英文数字用像素字体
- 小按钮去掉双线边框
- 底部输入栏元素统一

**Non-Goals:**
- 不修改大卡片（概览、进行中）的双线边框
- 不修改颜色系统
- 不添加新的交互功能

## Decisions

### D1: 像素图标绘制方式

**选择**: 用 8x8 二维数组定义图标，Canvas 逐像素绘制方块

**理由**: 
- 最 authentic 的 8-bit 风格
- 每个图标数据量小（8x8 = 64 像素）
- 易于维护和扩展

**替代方案**:
- SVG 像素图标: 需要外部资源，增加复杂度
- Unicode 像素字符: 跨平台显示不一致

### D2: 字体文件获取

**选择**: 从 Google Fonts 下载 Press Start 2P .ttf 文件放入 res/font/

**理由**:
- DESIGN.md 原始方案
- Google Fonts 免费可商用
- Android 原生支持 .ttf

### D3: 小按钮边框处理

**选择**: PixelIconButton 增加 `borderless: Boolean` 参数，默认 false

**理由**:
- 保持向后兼容
- 调用方可以精确控制
- 不影响大卡片的双线边框

### D4: 底部栏统一高度

**选择**: 所有元素统一 40dp 高度，"+"变成方形按钮

**理由**:
- 视觉一致性
- 触摸区域足够大
- 与输入框高度匹配

## Risks / Trade-offs

- **Press Start 2P 不支持中文**: 中文仍用系统字体，可能有视觉跳跃 → 可接受，DESIGN.md 已预见
- **像素图标可读性**: 8x8 分辨率下部分图标可能难以辨认 → 保持图标简单，避免复杂图形
- **向后兼容**: PixelIconButton 参数变更可能影响现有调用 → 默认值保持原有行为
