## MODIFIED Requirements

### Requirement: 8-bit 像素扁平设计语言

首页 SHALL 采用 8-bit 像素扁平设计语言，全部使用直角矩形、双线边框、纯色填充。

#### Scenario: 全局直角形状
- **WHEN** 首页渲染任何 Card、Button、Badge、Input、DropdownMenu 组件
- **THEN** 所有组件使用 `RoundedCornerShape(0.dp)` 直角矩形
- **AND** 不使用任何圆角值

#### Scenario: 双线边框替代阴影和虚线
- **WHEN** 首页渲染需要层级区分的组件
- **THEN** 使用双线边框（外层主色 + 内层高光）替代 elevation/shadow 和原虚线边框
- **AND** 不使用任何 `box-shadow` 或 `CardDefaults.cardElevation`
- **AND** 一级卡片使用 2dp 外线 + 1dp 内线
- **AND** 二级卡片使用 1.5dp 外线 + 0.5dp 内线
- **AND** 三级元素使用 1dp 双线

#### Scenario: 纯色填充无渐变
- **WHEN** 首页渲染任何背景色
- **THEN** 使用纯色填充（solid Color）
- **AND** 不使用 `Brush.linearGradient` 或 `Brush.radialGradient`

### Requirement: 多彩配色体系

首页各功能模块 SHALL 使用独立的色彩身份，以海蓝系为主基调，形成多彩但语义清晰的夏日海边风格配色体系。

#### Scenario: 概览卡片色彩
- **WHEN** 概览仪表盘卡片渲染
- **THEN** 背景使用 SeaBlue (#4DB6AC) + 双线边框（2dp DeepTeal + 1dp 白色）
- **AND** 已完成徽章使用 CoralOrange (#FF8A65) 背景 + 白色文字
- **AND** 待办徽章使用 LightSeaBlue (#80CBC4) 背景 + 白色文字

#### Scenario: 进行中卡片色彩
- **WHEN** ActiveEventCard 渲染
- **THEN** 背景使用 LightPink (#F48FB1) + 双线边框（2dp DeepTeal + 1dp 白色）
- **AND** 呼吸指示方块使用 SunYellow (#FFD54F)，帧式闪烁
- **AND** "结束"按钮使用 SeaBlue (#4DB6AC) 背景 + 白色文字 + 双线边框

#### Scenario: 已完成区色彩
- **WHEN** 已完成 Section 渲染
- **THEN** 卡片背景使用 MintBlue (#B2DFDB) + 双线边框（1.5dp SeaBlue + 0.5dp 白色）
- **AND** 事件色条按顺序轮换 SeaBlue / LightSeaBlue / SunYellow / CoralOrange / MintBlue

#### Scenario: 待办区色彩
- **WHEN** 待办 Section 渲染
- **THEN** 卡片使用 CreamWhite (#FFF8E1) 背景 + 双线边框（1.5dp SeaBlue + 0.5dp 白色）
- **AND** 左侧有 5dp 宽 SunYellow 实色边缘条
- **AND** 播放按钮按顺序轮换 CoralOrange / LightSeaBlue / SeaBlue / SunYellow / MintBlue 背景色

#### Scenario: 快速推荐标签色彩
- **WHEN** 快速推荐标签渲染
- **THEN** 每个标签使用不同的色彩底色（从 CoralOrange / SeaBlue / SunYellow / LightSeaBlue 中选择）
- **AND** 标签文字为白色
- **AND** 标签为直角矩形 + 双线边框

#### Scenario: 底部输入栏色彩
- **WHEN** 底部 QuickNameLine 渲染
- **THEN** 背景使用 CreamWhite + 双线边框（LightSeaBlue 系）
- **AND** "+"号使用 CoralOrange 色
- **AND** 提交按钮使用 CoralOrange 背景 + 白色播放图标 + 直角矩形

### Requirement: 像素图标系统

首页所有核心交互图标 SHALL 使用 Canvas 自绘的像素几何图标，替代 Material Icons。

#### Scenario: 播放/开始图标
- **WHEN** 显示开始/播放按钮
- **THEN** 图标 SHALL 为右向三角形（像素几何风格）
- **AND** 图标颜色可配置

#### Scenario: 停止/结束图标
- **WHEN** 显示停止/结束按钮
- **THEN** 图标 SHALL 为实心方块
- **AND** 图标颜色可配置

#### Scenario: 删除/关闭图标
- **WHEN** 显示删除/关闭按钮
- **THEN** 图标 SHALL 为 X 形（四臂交叉）
- **AND** 图标尺寸与操作按钮匹配

#### Scenario: 完成/对勾图标
- **WHEN** 显示已完成状态或确认按钮
- **THEN** 图标 SHALL 为折线对勾形

### Requirement: 像素组件复用体系

首页 SHALL 基于统一的像素 UI 组件库构建，确保风格一致且可维护。

#### Scenario: PixelCard 组件
- **WHEN** 首页使用卡片组件
- **THEN**  SHALL 使用统一的 `PixelCard` 组件
- **AND** 组件支持三级边框样式
- **AND** 组件支持自定义背景色和边框色

#### Scenario: PixelButton 组件
- **WHEN** 首页使用按钮
- **THEN**  SHALL 使用统一的 `PixelButton` / `PixelIconButton` 组件
- **AND** 按钮具备按压态视觉反馈

