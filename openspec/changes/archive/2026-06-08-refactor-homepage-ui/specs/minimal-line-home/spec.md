## ADDED Requirements

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

## MODIFIED Requirements

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
