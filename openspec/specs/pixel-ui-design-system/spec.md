# pixel-ui-design-system Specification

## Purpose

Defines the complete 8-bit pixel UI design system for the Shijiben app, including the double-line border system, seaside color palette, pixel geometric icon library, and reusable pixel UI component library. This is the design system foundation used by all screens.

## Requirements

### Requirement: 像素 UI 组件库

系统 SHALL 提供一套完整的 8-bit 像素风格 UI 组件库，供所有页面复用。

#### Scenario: 组件库包含基础卡片组件
- **WHEN** 开发者使用 `PixelCard` 组件
- **THEN** 组件 SHALL 支持 Primary / Secondary / Tertiary 三级边框样式
- **AND** 组件 SHALL 支持自定义背景色和边框色
- **AND** 组件 SHALL 默认使用双线边框（外层主色 + 内层白色）

#### Scenario: 组件库包含按钮组件
- **WHEN** 开发者使用 `PixelButton` 或 `PixelIconButton` 组件
- **THEN** 按钮 SHALL 为直角矩形（0dp 圆角）
- **AND** 按钮 SHALL 具备按压态视觉反馈（颜色变化 + 轻微缩放）
- **AND** 按钮 SHALL 支持自定义背景色和文字/图标颜色

#### Scenario: 组件库包含像素图标
- **WHEN** 应用中使用核心交互图标
- **THEN** 图标 SHALL 为 Canvas 自绘的几何形状（非 Material 图标）
- **AND** 图标 SHALL 至少包含：播放、停止、删除、对勾、添加、更多、刷新、暂停
- **AND** 图标颜色 SHALL 可通过参数动态配置

#### Scenario: 组件库包含输入框组件
- **WHEN** 开发者使用 `PixelInput` 输入框
- **THEN** 输入框 SHALL 使用双线边框样式
- **AND** 输入框 SHALL 支持 placeholder 文本
- **AND** 输入框聚焦时 SHALL 有明确的视觉反馈（边框加粗或变色）

#### Scenario: 组件库包含徽章/标签组件
- **WHEN** 开发者使用 `PixelBadge` 组件
- **THEN** 徽章 SHALL 为直角矩形 + 双线边框
- **AND** 徽章文字 SHALL 为白色像素风格
- **AND** 徽章背景色 SHALL 可配置

### Requirement: 双线边框系统

系统 SHALL 使用双线边框作为像素风格的核心视觉语言，替代原有的虚线边框。

#### Scenario: 一级卡片使用粗双线边框
- **WHEN** 展示高突出卡片（概览卡、进行中卡）
- **THEN** 边框 SHALL 为 2dp 外线 + 1dp 内线
- **AND** 外线颜色 SHALL 为深青蓝或主色
- **AND** 内线颜色 SHALL 为白色或同色系浅色

#### Scenario: 二级内容区使用中等双线边框
- **WHEN** 展示列表区卡片（已完成、待办）
- **THEN** 边框 SHALL 为 1.5dp 外线 + 0.5dp 内线
- **AND** 外线颜色 SHALL 为对应模块主题色

#### Scenario: 三级内嵌元素使用细双线边框
- **WHEN** 展示按钮、标签等内嵌元素
- **THEN** 边框 SHALL 为 1dp 外线 + 0.5dp 内线（或仅外线）
- **AND** 边框风格 SHALL 与整体系统统一

### Requirement: 夏日海边色彩系统

系统 SHALL 使用海蓝 + 薄荷绿为主色调的色彩系统，营造夏日海边清新感。

#### Scenario: 主色调为蓝绿色系
- **WHEN** 用户打开应用
- **THEN** 主要视觉区域（卡片背景、按钮、强调色）SHALL 以海蓝、薄荷绿等蓝绿色系为主
- **AND** 粉紫色 SHALL 仅作为特殊状态色少量使用（如"进行中"状态）
- **AND** 整体色彩 SHALL 给人清爽、通透的夏日海边感觉

#### Scenario: 点缀色使用暖色调
- **WHEN** 展示强调元素（按钮、标签、装饰）
- **THEN** 日光黄、珊瑚橙等暖色调 SHALL 作为点缀色使用
- **AND** 点缀色占比 SHALL 不超过整体视觉的 20%

#### Scenario: 每个主色具有明暗变体
- **WHEN** 组件需要显示按压态或悬停态
- **THEN** 每个主色 SHALL 提供 Dark 变体（按压态）和 Light 变体（背景态）
- **AND** 变体色值 SHALL 在色板中统一定义

### Requirement: 语义色 Token

系统 SHALL 提供统一的语义色彩 Token，确保信息传达一致性。

#### Scenario: 成功状态使用绿色系
- **WHEN** 展示成功、完成、确认状态
- **THEN** 颜色 SHALL 使用语义色 `SemanticSuccess`
- **AND** 默认值 SHALL 为薄荷绿色系

#### Scenario: 警告状态使用橙黄色系
- **WHEN** 展示警告、注意、待处理状态
- **THEN** 颜色 SHALL 使用语义色 `SemanticWarning`
- **AND** 默认值 SHALL 为珊瑚橙或日光黄色系

#### Scenario: 错误状态使用红色系
- **WHEN** 展示错误、删除、危险操作
- **THEN** 颜色 SHALL 使用语义色 `SemanticError`
- **AND** 默认值 SHALL 为珊瑚红色系

#### Scenario: 信息状态使用蓝色系
- **WHEN** 展示提示、信息、帮助
- **THEN** 颜色 SHALL 使用语义色 `SemanticInfo`
- **AND** 默认值 SHALL 为海蓝色系
