# minimal-line-home Specification

## Purpose

Minimal line-frame homepage for daily event logging with completed/pending sections.

## Requirements

### Requirement: Minimal line-frame homepage

The homepage SHALL present primary daily logging content in a structured card-grouped style with visual feedback for interactive elements.

#### Scenario: Homepage opens with card-grouped layout
- **WHEN** the user opens the homepage
- **THEN** the screen shows completed and pending lists grouped within rounded, soft-background card containers

#### Scenario: No app name in top-left
- **WHEN** the homepage top bar is displayed
- **THEN** the top-left area shows a calendar icon/date control instead of the app name

#### Scenario: Date picker from calendar icon
- **WHEN** the user taps the top-left calendar icon
- **THEN** the DayPickerDialog opens for date selection

### Requirement: 首页已完成区块

首页 SHALL 根据选择的日期显示已完成事件区块，其标题文案应反映时间视角。

#### Scenario: 今日已完成区块显示
- **WHEN** 用户选择今天且存在已完成事件
- **THEN** 标题显示为“今天已完成”

#### Scenario: 历史已完成区块显示
- **WHEN** 用户选择过去日期且存在已完成事件
- **THEN** 标题显示为“那天已完成”

#### Scenario: 未来已完成区块显示
- **WHEN** 用户选择未来日期且存在已完成事件
- **THEN** 标题显示为“已完成（计划外）”

#### Scenario: 已完成区块为空
- **WHEN** 当天没有任何已完成事件
- **THEN** 已完成区块显示为空状态区域

### Requirement: 首页待办区块

首页 SHALL 根据选择的日期显示待办或计划区块，其标题文案应反映时间视角。

#### Scenario: 今日待办区块显示
- **WHEN** 用户选择今天
- **THEN** 标题显示为“今日待办”

#### Scenario: 历史遗留区块显示
- **WHEN** 用户选择过去日期且存在未完成事件
- **THEN** 标题显示为“那天未完成”

#### Scenario: 未来计划区块显示
- **WHEN** 用户选择未来日期
- **THEN** 标题显示为“已规划的任务”

#### Scenario: 待办区块显示推荐信息
- **WHEN** 待办区块有推荐事件
- **THEN** 显示"今日待办 (X/5)"格式的标题，X为已完成推荐数

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

### Requirement: Active event self-timing

进行中的事件 SHALL 以 8-bit 像素风 Hero Card 展示。

#### Scenario: Active event 显示为像素风 Hero Card
- **WHEN** 事件正在进行中 (status is IN_PROGRESS)
- **THEN** 显示为 PixelHotPink 纯色背景 + 3dp PixelDeepNavy 边框的直角矩形卡片
- **AND** 左侧有 PixelStarYellow 方块闪烁指示灯
- **AND** 卡片内文字为白色

#### Scenario: Active event elapsed time updates
- **WHEN** an event is in progress
- **THEN** the Hero Card shows elapsed time updating in real time

#### Scenario: Stop active event
- **WHEN** the user taps stop on the Hero Card
- **THEN** the event status becomes completed and the end time is saved

#### Scenario: 结束按钮样式
- **WHEN** Hero Card 显示结束按钮
- **THEN** 按钮使用 PixelTeal 背景 + 2dp PixelDeepNavy 边框 + 深色文字
- **AND** 按钮为直角矩形（0dp 圆角）

### Requirement: 首页区块间距节奏

首页各区块之间 SHALL 使用差异化间距制造视觉段落感。

#### Scenario: 概览到进行中卡片间距
- **WHEN** 首页同时显示概览卡片和 ActiveEventCard
- **THEN** 两者之间的间距为 16dp

#### Scenario: 进行中到已完成区块间距
- **WHEN** 首页同时显示 ActiveEventCard 和已完成区块
- **THEN** 两者之间的间距为 12dp

#### Scenario: 已完成到待办区块间距
- **WHEN** 首页同时显示已完成区块和待办区块
- **THEN** 两者之间的间距为 20dp

### Requirement: Repeat event with "again" action

已完成的事件允许用户创建同名待办，"再来一次"按钮精简为图标形式。

#### Scenario: Tap "again" on completed event
- **WHEN** user taps the Refresh icon button (no text label) on a completed event
- **THEN** a new pending event with the same name is created for today

#### Scenario: Completed event shows icon-only "again" button
- **WHEN** an event is completed
- **THEN** its row shows a Refresh icon button (without text "再来一次")

#### Scenario: 历史视角下隐藏"再来一次"
- **WHEN** 用户在过去日期查看一个已完成的事件
- **THEN** 该事件行不显示"再来一次"按钮

### Requirement: Temporal perspective empty states

首页 SHALL 根据选择的日期显示特定的空状态文案，以增强时间感。

#### Scenario: 今日空状态
- **WHEN** 用户选择今天且列表为空
- **THEN** 显示“写下一件事，先不用开始”

#### Scenario: 历史空状态
- **WHEN** 用户选择过去日期且列表为空
- **THEN** 显示“那天似乎什么也没发生”

#### Scenario: 未来空状态
- **WHEN** 用户选择未来日期且列表为空
- **THEN** 显示"这一天还很空，不如规划点什么？"

### Requirement: Interactive touch feedback

The home screen interactive list items and cards SHALL scale down slightly when pressed to provide organic visual feedback.

#### Scenario: Item pressed scaling
- **WHEN** a user presses and holds a list item or card
- **THEN** the item scales down slightly (e.g., to 97% of its size)
- **AND** returns to original scale upon release

### Requirement: Smooth transition animations

The homepage list items SHALL transition smoothly when added, deleted, or moved between sections.

#### Scenario: Item added or deleted
- **WHEN** an event is added, completed, or deleted
- **THEN** the list animatingly updates its layout with slide and fade transitions for the affected items

### Requirement: 今日概览摘要区块

首页 SHALL 在今日视角下显示一个非对称仪表盘概览卡片，使用 8-bit 像素扁平风格。

#### Scenario: 今日视角显示概览卡片
- **WHEN** 用户选择今天
- **THEN** 首页上方显示非对称概览卡片，左侧为大号白色时间数字（PixelDisplay 32sp Black），右侧为两个垂直堆叠的彩色徽章
- **AND** 卡片背景使用 PixelSkyBlue + 3dp PixelDeepNavy 边框
- **AND** 右上角徽章使用 PixelCoralRed 背景显示已完成数
- **AND** 右下角徽章使用 PixelLavender 背景显示待办数
- **AND** 卡片使用直角矩形（0dp 圆角）

#### Scenario: 非今日视角隐藏概览卡片
- **WHEN** 用户选择过去或未来日期
- **THEN** 概览卡片不显示

#### Scenario: 概览数字实时更新
- **WHEN** 用户完成或添加事件
- **THEN** 概览卡片中的数字同步更新

### Requirement: 卡片视觉层次

首页的待办卡片和已完成卡片 SHALL 使用 8-bit 边框系统和直角矩形区分层级。

#### Scenario: 待办卡片更突出
- **WHEN** 首页显示待办 Section
- **THEN** 待办 Card 使用白色背景 + 2dp PixelAmberOrange 边框 + 左侧 5dp 金色边缘条
- **AND** 卡片为直角矩形（0dp 圆角）
- **AND** 无阴影

#### Scenario: 已完成卡片更低调
- **WHEN** 首页显示已完成 Section
- **THEN** 已完成 Card 使用 PixelMintLight 背景 + 2dp PixelTeal 边框
- **AND** 列表项高度为 48dp（紧凑模式）
- **AND** 事件名字号为 bodySmall (12sp)
- **AND** 左侧色条 3dp 宽，颜色按事件顺序轮换多彩色
- **AND** 卡片为直角矩形（0dp 圆角）

### Requirement: 底部输入栏像素风样式

底部快捷输入栏 SHALL 采用 8-bit 像素扁平风格。

#### Scenario: 浮动输入栏像素风样式
- **WHEN** 底部 QuickNameLine 显示
- **THEN** 使用白色背景 + 2dp PixelLavender 边框 + 直角矩形（0dp 圆角）
- **AND** "+"图标使用 PixelHotPink 着色
- **AND** 提交按钮使用 PixelAmberOrange 背景方块 + 白色播放图标
- **AND** 无阴影，无毛玻璃效果

### Requirement: Warm Gold 暖色主题

整个应用 SHALL 使用 DESIGN.md 定义的 Warm Gold 暖色调色板。

#### Scenario: 主色为暖金色
- **WHEN** 任何界面使用 primary 颜色
- **THEN** 显示为暖金色 #F5C469（非珊瑚橙 #FF7A5A）

#### Scenario: 页面背景为暖奶油白
- **WHEN** 任何界面渲染背景色
- **THEN** 使用暖奶油白 #FFF9F0（非 #FCFAF7）

#### Scenario: 浅色模式固定
- **WHEN** 应用启动
- **THEN** 始终使用浅色暖色调色板，不跟随系统深色模式

#### Scenario: 深色模式保留
- **WHEN** 未来启用深色模式
- **THEN** Warm Gold 主色保持可辨识，暗色背景使用 #1A1A2E 暖深色调

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
