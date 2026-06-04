# minimal-line-home Specification

## Purpose

TBD - created by archiving change minimal-line-home-redesign. Update Purpose after archive.

## ADDED Requirements

### Requirement: 首页已完成区块

首页 SHALL 在顶部显示"今天已完成"区块，包含当天所有 COMPLETED 状态的事件。

#### Scenario: 已完成区块显示
- **WHEN** 用户打开首页且当天存在已完成事件
- **THEN** 屏幕顶部显示"今天已完成"标题，下方列出所有已完成事件

#### Scenario: 已完成区块为空
- **WHEN** 当天没有任何已完成事件
- **THEN** 已完成区块显示为空状态区域

### Requirement: 首页待办区块

首页 SHALL 在底部显示"今日待办"区块，包含当天所有 PENDING 和 IN_PROGRESS 状态的事件。

#### Scenario: 待办区块显示
- **WHEN** 用户打开首页
- **THEN** 屏幕底部显示"今日待办"标题，下方列出所有未完成事件

#### Scenario: 待办区块显示推荐信息
- **WHEN** 待办区块有推荐事件
- **THEN** 显示"今日待办 (X/5)"格式的标题，X为已完成推荐数

## MODIFIED Requirements

### Requirement: Name-only event creation
The homepage SHALL allow a user to create an event by entering only a name.

#### Scenario: Add event by name
- **WHEN** the user enters a non-empty event name and confirms add
- **THEN** a new event row appears on the selected day without starting a timer

#### Scenario: Empty name is ignored
- **WHEN** the user confirms add with an empty or whitespace-only name
- **THEN** no event is created

### Requirement: Not-started events behave as todos
A newly added event SHALL be shown as a not-started todo until the user starts it.

#### Scenario: New event shows todo state
- **WHEN** an event has not been started
- **THEN** its row shows the event name and a start action, not a duration

#### Scenario: Start todo event
- **WHEN** the user taps start on a not-started event row
- **THEN** the event status becomes in-progress and timing begins from the current time

### Requirement: Active event self-timing
An in-progress event SHALL show elapsed time and a stop action on its homepage row.

#### Scenario: Active event elapsed time updates
- **WHEN** an event is in progress
- **THEN** its row shows elapsed minutes derived from current time minus start time

#### Scenario: Stop active event
- **WHEN** the user taps stop on an in-progress event row
- **THEN** the event status becomes completed and the end time is saved

### Requirement: Repeat event with "again" action
A completed event SHALL allow user to create a new pending event with the same name.

#### Scenario: Tap "again" on completed event
- **WHEN** user taps "再来一次" button on a completed event
- **THEN** a new pending event with the same name is created for today

#### Scenario: Completed event shows "again" button
- **WHEN** an event is completed
- **THEN** its row shows a "再来一次" button

## REMOVED Requirements

### Requirement: Frequent event shortcuts

**Reason**: Replaced by algorithm-driven recommendation list
**Migration**: Use the new "今日待办" section with algo-recommendation

### Requirement: Compact no-scroll priority

**Reason**: New layout design separates completed and pending sections, scroll behavior changed
**Migration**: New design prioritizes clarity over no-scroll constraint
