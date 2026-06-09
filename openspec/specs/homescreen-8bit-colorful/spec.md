# homescreen-8bit-colorful Specification

## Purpose

Defines the 8-bit pixel-flat colorful design language applied to the homepage, including the color token system, monospace typography, pixel-shape components, and feature-module color identities.

## Requirements

### Requirement: 8-bit 像素扁平设计语言

首页 SHALL 采用 8-bit 像素扁平设计语言，全部使用直角矩形、实色边框、纯色填充。

#### Scenario: 全局直角形状
- **WHEN** 首页渲染任何 Card、Button、Badge、Input、DropdownMenu 组件
- **THEN** 所有组件使用 `RoundedCornerShape(0.dp)` 直角矩形
- **AND** 不使用任何圆角值

#### Scenario: 边框替代阴影
- **WHEN** 首页渲染需要层级区分的组件
- **THEN** 使用 2-3dp 实色边框（PixelDeepNavy #1A1A2E）替代 elevation/shadow
- **AND** 不使用任何 `box-shadow` 或 `CardDefaults.cardElevation`

#### Scenario: 纯色填充无渐变
- **WHEN** 首页渲染任何背景色
- **THEN** 使用纯色填充（solid Color）
- **AND** 不使用 `Brush.linearGradient` 或 `Brush.radialGradient`

### Requirement: 多彩配色体系

首页各功能模块 SHALL 使用独立的色彩身份，形成多彩但语义清晰的配色体系。

#### Scenario: 概览卡片色彩
- **WHEN** 概览仪表盘卡片渲染
- **THEN** 背景使用 PixelSkyBlue (#6BC5F5) + 3dp PixelDeepNavy 边框
- **AND** 已完成徽章使用 PixelCoralRed (#FF8A6B) 背景 + 白色文字
- **AND** 待办徽章使用 PixelLavender (#C4B5E0) 背景 + 白色文字

#### Scenario: 进行中卡片色彩
- **WHEN** ActiveEventCard 渲染
- **THEN** 背景使用 PixelHotPink (#FF6B9D) + 3dp PixelDeepNavy 边框
- **AND** 呼吸指示方块使用 PixelStarYellow (#FFD93D)
- **AND** "结束"按钮使用 PixelTeal (#4DC9B8) 背景 + 深色文字

#### Scenario: 已完成区色彩
- **WHEN** 已完成 Section 渲染
- **THEN** 卡片背景使用 PixelMintLight (#E8F8F0) + 2dp PixelTeal 边框
- **AND** 事件色条按顺序轮换 PixelTeal / PixelSkyBlue / PixelLavender / PixelCoralRed / PixelStarYellow

#### Scenario: 待办区色彩
- **WHEN** 待办 Section 渲染
- **THEN** 卡片使用白色背景 + 2dp PixelAmberOrange (#FFB347) 边框
- **AND** 左侧有 5dp 宽金色实色边缘条
- **AND** 播放按钮按顺序轮换 PixelCoralRed / PixelLavender / PixelTeal / PixelSkyBlue / PixelHotPink 背景色

#### Scenario: 快速推荐标签色彩
- **WHEN** 快速推荐标签渲染
- **THEN** 每个标签使用不同的色彩底色（从 PixelCoralRed / PixelTeal / PixelAmberOrange / PixelSkyBlue 中选择）
- **AND** 标签文字为白色

#### Scenario: 底部输入栏色彩
- **WHEN** 底部 QuickNameLine 渲染
- **THEN** 使用 2dp PixelLavender 边框 + 白色背景
- **AND** "+"图标使用 PixelHotPink 着色
- **AND** 提交按钮使用 PixelAmberOrange 背景 + 白色播放图标

### Requirement: 像素风排版

首页标题和数字 SHALL 使用 Monospace 字体族排版，模拟 8-bit 像素感。

#### Scenario: 标题和数字使用 Monospace
- **WHEN** 首页渲染概览时间数字、进行中计时数字
- **THEN** 使用 FontFamily.Monospace + FontWeight.Black + letterSpacing 0.05em
- **AND** 字号为 32sp（PixelDisplay 样式）

#### Scenario: Section 标签使用像素风排版
- **WHEN** 首页渲染 Section 标题
- **THEN** 使用 FontFamily.Monospace + FontWeight.Bold + letterSpacing 0.12em + uppercase
- **AND** 字号为 10sp（PixelLabel 样式）

#### Scenario: 中文正文保持默认字体
- **WHEN** 首页渲染中文事件名称或描述文字
- **THEN** 使用 FontFamily.Default（系统默认字体）确保中文可读性

### Requirement: 像素风呼吸动画

进行中卡片的指示灯 SHALL 使用方块形状 + 帧式闪烁动画替代圆形平滑呼吸。

#### Scenario: 方块指示灯
- **WHEN** ActiveEventCard 渲染呼吸指示灯
- **THEN** 指示灯使用 8dp × 8dp 方块形状（PixelShape）
- **AND** 颜色为 PixelStarYellow (#FFD93D)

#### Scenario: 帧式闪烁动画
- **WHEN** 呼吸动画播放
- **THEN** 使用 keyframes 动画在 0 和 1 之间瞬切（非平滑过渡）
- **AND** 动画周期为 800ms（400ms 可见 + 400ms 不可见）

### Requirement: Section 彩色短线标题

首页 Section 标题 SHALL 使用 `PixelSectionHeader` 组件，带彩色短线锚点。

#### Scenario: 已完成标题短线
- **WHEN** 已完成 Section 标题渲染
- **THEN** 标题前显示 12dp × 3dp 的 PixelTeal 色短线

#### Scenario: 待办标题短线
- **WHEN** 待办 Section 标题渲染
- **THEN** 标题前显示 12dp × 3dp 的 PixelAmberOrange 色短线

#### Scenario: 推荐标题短线
- **WHEN** "建议快速开始" Section 标题渲染
- **THEN** 标题前显示 12dp × 3dp 的 PixelCoralRed 色短线

### Requirement: 多彩色板 Token

Color.kt SHALL 新增 8-bit 多彩色板 Token。

#### Scenario: 新增 9 个色彩 Token
- **WHEN** 首页使用多彩配色
- **THEN** Color.kt 定义以下 Token：PixelSkyBlue (#6BC5F5)、PixelHotPink (#FF6B9D)、PixelCoralRed (#FF8A6B)、PixelLavender (#C4B5E0)、PixelTeal (#4DC9B8)、PixelAmberOrange (#FFB347)、PixelMintLight (#E8F8F0)、PixelStarYellow (#FFD93D)、PixelDeepNavy (#1A1A2E)

#### Scenario: 新增边框 Token
- **WHEN** 组件使用像素风边框
- **THEN** 使用 PixelBorder (= PixelDeepNavy) 作为主边框色
- **AND** 使用 PixelBorderLight (= PixelDeepNavy 20% alpha) 作为轻量分割色
