# repeat-event Specification

## Purpose

支持同一事件在同一天完成多次，适应用户可能重复进行同一活动的场景（如冥想、运动等）。

## Requirements

### Requirement: 同事件多次完成

系统 SHALL 支持同一事件名称在同一天存在多条独立的 COMPLETED 记录。

#### Scenario: 同一事件完成多次
- **WHEN** 用户对"冥想"事件完成两次
- **THEN** 系统创建两条独立的 EventEntity 记录，分别记录各自的开始和结束时间

#### Scenario: 多次完成的记录独立展示
- **WHEN** 用户完成"冥想"两次
- **THEN** 首页已完成区域显示两条独立的"冥想"记录，各自带有时间信息

### Requirement: 再来一次功能

用户 SHALL 能够对已完成的事件点击"再来一次"，创建新的 PENDING 待办。

#### Scenario: 点击再来一次
- **WHEN** 用户点击已完成事件上的"再来一次"按钮
- **THEN** 系统创建一条新的 PENDING 事件，名称与原事件相同，dayKey 为当天日期

#### Scenario: 再来一次创建的是待办非直接开始
- **WHEN** 用户点击"再来一次"
- **THEN** 新创建的事件状态为 PENDING，用户需要点击"开始"才会进入计时

#### Scenario: 再来一次的独立时间记录
- **WHEN** 用户完成"再来一次"创建的事件
- **THEN** 新事件拥有独立的 startTimeMillis 和 endTimeMillis

### Requirement: 进行中事件的再来一次

正在进行中的事件 SHALL 不能点击"再来一次"。

#### Scenario: 进行中事件无再来一次按钮
- **WHEN** 事件状态为 IN_PROGRESS
- **THEN** 该事件不显示"再来一次"按钮

### Requirement: 删除事件

用户 SHALL 能够直接删除事件，无需确认。

#### Scenario: 删除待办事件
- **WHEN** 用户点击待办事件的"删除"按钮
- **THEN** 事件立即从列表中移除，从数据库删除

#### Scenario: 删除进行中事件
- **WHEN** 用户点击正在进行中事件的"删除"按钮
- **THEN** 系统先停止计时，再删除事件
