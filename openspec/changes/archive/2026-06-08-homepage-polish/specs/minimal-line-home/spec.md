# minimal-line-home Spec Delta

## MODIFIED Requirements

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
- **AND** IN_PROGRESS uses Coral (#FF7A5A)
- **AND** COMPLETED uses Green (#4CAF78)

### Requirement: Active event self-timing

An in-progress event SHALL be prominently displayed as a visual "Hero Card" at the top of the homepage, featuring a coral-to-amber-to-teal gradient background, breathing animation, real-time timer, and quick action controls.

#### Scenario: Active event shown as Hero Card with warm gradient
- **WHEN** an event is in progress (status is IN_PROGRESS)
- **THEN** it is displayed in a dedicated card at the top of the homepage with a warm gradient background (Coral #FF7A5A → Amber #FFB347 → Teal #4DC9B8) and a breathing animation indicator

### Requirement: Repeat event with "again" action

已完成的事件允许用户创建同名待办，"再来一次"按钮精简为图标形式。

#### Scenario: Tap "again" on completed event
- **WHEN** user taps the Refresh icon button (no text label) on a completed event
- **THEN** a new pending event with the same name is created for today

#### Scenario: Completed event shows icon-only "again" button
- **WHEN** an event is completed
- **THEN** its row shows a Refresh icon button (without text "再来一次")

## ADDED Requirements

### Requirement: 今日概览摘要区块

首页 SHALL 在今日视角下显示一个摘要卡片，展示当日专注时长、完成数和待办数。

#### Scenario: 今日视角显示概览卡片
- **WHEN** 用户选择今天
- **THEN** 首页上方（ActiveEventCard 之前）显示一个三列概览卡片，分别展示"已专注 X 分钟"、"完成 Y 件"、"待办 Z 件"

#### Scenario: 非今日视角隐藏概览卡片
- **WHEN** 用户选择过去或未来日期
- **THEN** 概览卡片不显示

#### Scenario: 概览数字实时更新
- **WHEN** 用户完成或添加事件
- **THEN** 概览卡片中的数字同步更新

### Requirement: 卡片视觉层次

首页的待办卡片和已完成卡片 SHALL 有明确的视觉层次区分。

#### Scenario: 待办卡片更突出
- **WHEN** 首页显示待办 Section
- **THEN** 待办 Card 使用白色 surface 背景 + shadow-sm 阴影

#### Scenario: 已完成卡片更低调
- **WHEN** 首页显示已完成 Section
- **THEN** 已完成 Card 使用暖灰淡色背景 (#F8F5F0)，无阴影

### Requirement: 底部输入栏毛玻璃质感

底部快捷输入栏 SHALL 具有半透明毛玻璃浮动效果。

#### Scenario: 浮动输入栏显示毛玻璃效果
- **WHEN** 底部 QuickNameLine 显示
- **THEN** 其背景为半透明 surface 色 + 轻微阴影，产生浮动于内容之上的视觉感受

### Requirement: Coral 暖色主题

整个应用 SHALL 使用 DESIGN.md 定义的 Coral 暖色调色板。

#### Scenario: 主色为珊瑚橙
- **WHEN** 任何界面使用 primary 颜色
- **THEN** 显示为珊瑚橙 #FF7A5A（非靛蓝 #4F46E5）

#### Scenario: 页面背景为暖米白
- **WHEN** 任何界面渲染背景色
- **THEN** 使用暖米白 #FCFAF7（非冷调 #F9FAFF）

#### Scenario: 深色模式适配
- **WHEN** 系统处于深色模式
- **THEN** Coral 主色保持可辨识，暗色背景使用暖深色调
