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

未开始的事件在首页显示为待办事项，但在历史或未来视角下，其交互行为应有所限制。

#### Scenario: New event shows todo state
- **WHEN** an event has not been started
- **THEN** its row shows the event name and a start action, not a duration

#### Scenario: Start todo event
- **WHEN** the user taps start on a not-started event row
- **THEN** the event status becomes in-progress and timing begins from the current time

#### Scenario: 历史视角下禁用开始操作
- **WHEN** 用户在过去日期查看一个未开始的事件
- **THEN** 该事件行不显示“开始”按钮

#### Scenario: 未来视角下禁用开始操作
- **WHEN** 用户在未来日期查看一个未开始的事件
- **THEN** 该事件行显示“开始”按钮但处于禁用状态，或隐藏“开始”按钮

### Requirement: Active event self-timing

An in-progress event SHALL be prominently displayed as a visual "Hero Card" at the top of the homepage, featuring a gradient background, breathing animation, real-time timer, and quick action controls.

#### Scenario: Active event shown as Hero Card
- **WHEN** an event is in progress (status is IN_PROGRESS)
- **THEN** it is displayed in a dedicated card at the top of the homepage with a gradient background and a breathing animation indicator

#### Scenario: Active event elapsed time updates
- **WHEN** an event is in progress
- **THEN** the Hero Card shows elapsed time updating in real time

#### Scenario: Stop active event
- **WHEN** the user taps stop on the Hero Card
- **THEN** the event status becomes completed and the end time is saved

### Requirement: Repeat event with "again" action

已完成的事件允许用户创建同名待办，但在历史视角下应移除此操作。

#### Scenario: Tap "again" on completed event
- **WHEN** user taps "再来一次" button on a completed event
- **THEN** a new pending event with the same name is created for today

#### Scenario: Completed event shows "again" button
- **WHEN** an event is completed
- **THEN** its row shows a "再来一次" button

#### Scenario: 历史视角下隐藏“再来一次”
- **WHEN** 用户在过去日期查看一个已完成的事件
- **THEN** 该事件行不显示“再来一次”按钮

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
