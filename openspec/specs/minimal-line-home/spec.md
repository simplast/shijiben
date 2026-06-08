# minimal-line-home Specification

## Purpose

Minimal line-frame homepage for daily event logging with completed/pending sections.

## Requirements

### Requirement: Minimal line-frame homepage

The homepage SHALL present primary daily logging content in a structured card-grouped style with rounded corners, subtle shadows, and visual feedback for interactive elements.

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

首页 SHALL 允许用户通过底部的悬浮胶囊输入栏（Floating Capsule Input Bar）输入名称来创建事件，但在历史视角下应隐藏此功能。

#### Scenario: Add event by name
- **WHEN** the user enters a non-empty event name in the floating input bar and confirms add
- **THEN** a new event row appears on the selected day without starting a timer

#### Scenario: Empty name is ignored
- **WHEN** the user confirms add with an empty or whitespace-only name
- **THEN** no event is created

#### Scenario: 历史视角下隐藏添加框
- **WHEN** 用户选择过去日期
- **THEN** 悬浮胶囊输入栏被隐藏

#### Scenario: 今日或未来视角下显示添加框
- **WHEN** 用户选择今天或未来日期
- **THEN** 显示悬浮胶囊输入栏

### Requirement: Not-started events behave as todos

未开始的事件在首页显示为精简待办事项，带状态色条和图标操作按钮。

#### Scenario: Pending item shows minimal controls
- **WHEN** an event has not been started (status is PENDING)
- **THEN** its row shows: left accent bar (4dp, amber #FFB347), event name, a PlayArrow icon button (no text label), and a delete icon button
- **AND** no status text like "待开始" is displayed

#### Scenario: Start pending event via icon
- **WHEN** the user taps the PlayArrow icon on a pending event row
- **THEN** the event status becomes IN_PROGRESS and timing begins from the current time

#### Scenario: Pending accent bar uses amber color
- **WHEN** an event status is PENDING
- **THEN** its left accent bar (4dp wide) uses the amber color (#FFB347)
- **AND** IN_PROGRESS uses warm gold dark (#E5B050)
- **AND** COMPLETED uses mint green (#7DD3A8)

#### Scenario: 历史视角下禁用开始操作
- **WHEN** 用户在过去日期查看一个未开始的事件
- **THEN** 该事件行不显示"开始"按钮

#### Scenario: 未来视角下禁用开始操作
- **WHEN** 用户在未来日期查看一个未开始的事件
- **THEN** 该事件行显示"开始"按钮但处于禁用状态，或隐藏"开始"按钮

### Requirement: Active event self-timing

An in-progress event SHALL be prominently displayed as a visual "Hero Card" at the top of the homepage, featuring a warm gold gradient background (#F5C469 → #FFB347 → #7DD3A8), breathing animation, real-time timer, and quick action controls.

#### Scenario: Active event shown as Hero Card with warm gradient
- **WHEN** an event is in progress (status is IN_PROGRESS)
- **THEN** it is displayed in a dedicated card at the top of the homepage with a warm gradient background and a breathing animation indicator

#### Scenario: Active event elapsed time updates
- **WHEN** an event is in progress
- **THEN** the Hero Card shows elapsed time updating in real time

#### Scenario: Stop active event
- **WHEN** the user taps stop on the Hero Card
- **THEN** the event status becomes completed and the end time is saved

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

首页 SHALL 在今日视角下显示一个摘要卡片，展示当日专注时长、完成数和待办数，使用暖金主色和 labelUppercase 排版。

#### Scenario: 今日视角显示概览卡片
- **WHEN** 用户选择今天
- **THEN** 首页上方（ActiveEventCard 之前）显示一个三列概览卡片，分别展示"已专注 X 分钟"、"完成 Y 件"、"待办 Z 件"
- **AND** 数字使用 PrimaryGold (#F5C469) 主色
- **AND** 标签使用 labelUppercase 排版（10sp / Medium / letter-spacing 0.1em）
- **AND** 卡片圆角 20dp

#### Scenario: 非今日视角隐藏概览卡片
- **WHEN** 用户选择过去或未来日期
- **THEN** 概览卡片不显示

#### Scenario: 概览数字实时更新
- **WHEN** 用户完成或添加事件
- **THEN** 概览卡片中的数字同步更新

### Requirement: 卡片视觉层次

首页的待办卡片和已完成卡片 SHALL 有明确的视觉层次区分，使用暖调阴影系统。

#### Scenario: 待办卡片更突出
- **WHEN** 首页显示待办 Section
- **THEN** 待办 Card 使用白色 surface 背景 + shadow-sm 暖调阴影
- **AND** 卡片圆角 20dp

#### Scenario: 已完成卡片更低调
- **WHEN** 首页显示已完成 Section
- **THEN** 已完成 Card 使用暖灰淡色背景 (#FDF5E8)，无阴影
- **AND** 卡片圆角 20dp

### Requirement: 底部输入栏毛玻璃质感

底部快捷输入栏 SHALL 具有半透明毛玻璃浮动效果，使用暖金色焦点色。

#### Scenario: 浮动输入栏显示毛玻璃效果
- **WHEN** 底部 QuickNameLine 显示
- **THEN** 其背景为半透明 surface 色 + 轻微阴影，产生浮动于内容之上的视觉感受
- **AND** 输入框聚焦时边框色为 PrimaryGold (#F5C469)，焦点光晕为 PrimaryGoldSoft (#FEF3D9)
- **AND** 输入框圆角 16dp

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

### Requirement: 排版体系

首页 SHALL 使用 Outfit（展示标题）+ Inter（正文）+ JetBrains Mono（数字/时间）三字体系统，并定义 9 级 Type Scale。

#### Scenario: 展示级标题使用 Outfit 字体
- **WHEN** 首页渲染 display/hero/h1 级标题
- **THEN** 使用 Outfit 字体族（Bold/Semibold）

#### Scenario: 正文使用 Inter 字体
- **WHEN** 首页渲染正文内容
- **THEN** 使用 Inter 字体族（Regular/Medium）

#### Scenario: 数字和时间使用 JetBrains Mono
- **WHEN** 首页渲染计时数字、统计数据
- **THEN** 使用 JetBrains Mono 字体族

### Requirement: 标签排版规范

首页所有区块标签 SHALL 使用全大写 + 加宽字距的 labelUppercase 排版风格。

#### Scenario: 区块标题使用全大写标签
- **WHEN** 首页显示 Section 标题（如"今日待办"、"已完成"、"建议快速开始"）
- **THEN** 标签使用 10sp / Medium / letter-spacing 0.1em 排版
- **AND** 颜色为 WarmGray500

### Requirement: 圆角与间距体系

首页 SHALL 使用 DESIGN.md 定义的圆角和间距体系。

#### Scenario: 卡片圆角
- **WHEN** 首页渲染任何卡片组件
- **THEN** 卡片圆角为 20dp

#### Scenario: 按钮圆角
- **WHEN** 首页渲染任何按钮组件
- **THEN** 按钮圆角为 16dp

#### Scenario: 安全边距
- **WHEN** 首页渲染内容区域
- **THEN** 左右安全边距为 20dp

#### Scenario: 列表项最小高度
- **WHEN** 首页渲染事件列表项
- **THEN** 列表项最小高度为 56dp

### Requirement: 暖调阴影系统

首页 SHALL 使用暖调阴影系统替代默认冷灰阴影。

#### Scenario: 卡片使用暖调阴影
- **WHEN** 首页渲染带阴影的卡片
- **THEN** 阴影使用暖调 rgba(26,26,46,...) 而非默认冷灰色

#### Scenario: 阴影层级
- **WHEN** 首页组件需要不同高度的阴影
- **THEN** 使用 6 级暖调阴影（xs/sm/md/lg/xl/xxl）
