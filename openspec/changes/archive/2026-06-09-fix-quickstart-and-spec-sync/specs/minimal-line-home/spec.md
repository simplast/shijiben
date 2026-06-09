# minimal-line-home Specification (Delta)

## MODIFIED Requirements

### Requirement: Name-only event creation

首页 SHALL 允许用户通过底部的像素风输入栏（Pixel Input Bar）输入名称来创建事件，但在历史视角下应隐藏此功能。

#### Scenario: Add event by name
- **WHEN** the user enters a non-empty event name in the pixel input bar and confirms add
- **THEN** a new event row appears on the selected day without starting a timer

#### Scenario: Empty name is ignored
- **WHEN** the user confirms add with an empty or whitespace-only name
- **THEN** no event is created

#### Scenario: 历史视角下隐藏添加框
- **WHEN** 用户选择过去日期
- **THEN** 像素风输入栏被隐藏

#### Scenario: 今日或未来视角下显示添加框
- **WHEN** 用户选择今天或未来日期
- **THEN** 显示像素风输入栏

### Requirement: Not-started events behave as todos

未开始的事件在首页显示为像素风待办事项。

#### Scenario: Pending item 像素风样式
- **WHEN** 事件状态为 PENDING
- **THEN** 事件行显示：左侧 3dp PixelAmberOrange 色条、事件名称、方块形播放按钮（28dp，多彩背景 + 白色三角形）、删除按钮
- **AND** 播放按钮使用直角矩形（0dp 圆角）
- **AND** 每个播放按钮使用不同的色彩背景（从 PixelCoralRed / PixelLavender / PixelTeal / PixelSkyBlue / PixelHotPink 中轮换）

#### Scenario: Start pending event via icon
- **WHEN** the user taps the PlayArrow icon on a pending event row
- **THEN** the event status becomes IN_PROGRESS and timing begins from the current time

#### Scenario: Pending accent bar uses amber color
- **WHEN** an event status is PENDING
- **THEN** its left accent bar (3dp wide) uses the amber color (#FFB347)
- **AND** IN_PROGRESS uses warm gold dark (#E5B050)
- **AND** COMPLETED uses mint green (#7DD3A8)

#### Scenario: 历史视角下禁用开始操作
- **WHEN** 用户在过去日期查看一个未开始的事件
- **THEN** 该事件行不显示"开始"按钮

#### Scenario: 未来视角下禁用开始操作
- **WHEN** 用户在未来日期查看一个未开始的事件
- **THEN** 该事件行显示"开始"按钮但处于禁用状态，或隐藏"开始"按钮

## REMOVED Requirements

### Requirement: 排版体系
**Reason**: The Outfit + Inter + JetBrains Mono three-font system and 9-level Type Scale are replaced by the 8-bit pixel monospace system defined in the homescreen-8bit-colorful change.
**Migration**: Use the 8-bit pixel monospace font system (PixelDisplay / PixelBody / PixelLabel) as defined in the homescreen-8bit-colorful spec.

### Requirement: 标签排版规范
**Reason**: The labelUppercase (10sp / Medium / letter-spacing 0.1em) style is replaced by the PixelLabel style defined in the 8-bit pixel system.
**Migration**: Use PixelLabel style from the homescreen-8bit-colorful spec for all section labels.

### Requirement: 圆角与间距体系
**Reason**: The rounded corner system (20dp cards, 16dp buttons, 56dp min row height) conflicts with the 8-bit pixel design which uses 0dp corners throughout.
**Migration**: Use the PixelShape (0dp) system from the homescreen-8bit-colorful spec for all components.

### Requirement: 暖调阴影系统
**Reason**: The warm-tone shadow system (6 levels of rgba shadows) is replaced by the pixel border system which uses solid borders instead of shadows.
**Migration**: Use the pixel border system (2dp-3dp solid PixelDeepNavy borders) from the homescreen-8bit-colorful spec for elevation and depth.
