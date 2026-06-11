## Why

当前 UI 的像素风格不够 authentic：图标是平滑线条而非像素块、字体用系统等宽而非 Press Start 2P、小按钮双线边框视觉杂乱、底部输入栏元素不一致。需要一次系统性重设计，让 8-bit 风格真正到位。

## What Changes

- **图标重写**: 所有 PixelIcons 从 Canvas 平滑线条改为 8x8 网格像素块绘制
- **字体升级**: 添加 Press Start 2P .ttf 字体文件，英文数字用像素字体，中文用系统字体
- **边框简化**: 小按钮（Play/Delete/+/提交）去掉双线边框，仅大卡片保留
- **底部栏统一**: "+"和提交按钮无边框纯色背景，输入框保留单线边框，统一高度 40dp

## Capabilities

### New Capabilities

- `pixel-icons`: 8x8 网格像素图标系统，替换当前 Canvas 平滑线条图标
- `pixel-font`: Press Start 2P 字体集成，英文数字像素化显示

### Modified Capabilities

（无现有 spec 需要修改）

## Impact

- `app/src/main/res/font/pressstart2p.ttf` — 新增字体文件
- `app/src/main/java/com/doer/shijiben/ui/theme/Type.kt` — 引用 Press Start 2P
- `app/src/main/java/com/doer/shijiben/ui/theme/PixelIcons.kt` — 全部重写为像素网格
- `app/src/main/java/com/doer/shijiben/ui/theme/PixelComponents.kt` — PixelIconButton 增加无边框选项
- `app/src/main/java/com/doer/shijiben/ui/screens/HomeScreen.kt` — Pending 按钮、底部栏样式调整
