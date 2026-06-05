## MODIFIED Requirements

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

### Requirement: Name-only event creation

首页 SHALL 允许用户通过仅输入名称来创建事件，但在历史视角下应隐藏此功能。

#### Scenario: 历史视角下隐藏添加框
- **WHEN** 用户选择过去日期
- **THEN** 快速添加输入框（QuickNameLine）被隐藏

#### Scenario: 今日或未来视角下显示添加框
- **WHEN** 用户选择今天或未来日期
- **THEN** 显示快速添加输入框

### Requirement: Not-started events behave as todos

未开始的事件在首页显示为待办事项，但在历史或未来视角下，其交互行为应有所限制。

#### Scenario: 历史视角下禁用开始操作
- **WHEN** 用户在过去日期查看一个未开始的事件
- **THEN** 该事件行不显示“开始”按钮

#### Scenario: 未来视角下禁用开始操作
- **WHEN** 用户在未来日期查看一个未开始的事件
- **THEN** 该事件行显示“开始”按钮但处于禁用状态，或隐藏“开始”按钮

### Requirement: Repeat event with "again" action

已完成的事件允许用户创建同名待办，但在历史视角下应移除此操作。

#### Scenario: 历史视角下隐藏“再来一次”
- **WHEN** 用户在过去日期查看一个已完成的事件
- **THEN** 该事件行不显示“再来一次”按钮

## ADDED Requirements

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
- **THEN** 显示“这一天还很空，不如规划点什么？”
