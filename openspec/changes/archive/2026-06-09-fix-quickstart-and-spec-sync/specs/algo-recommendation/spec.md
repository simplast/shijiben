# algo-recommendation Specification (Delta)

## MODIFIED Requirements

### Requirement: 推荐与事件的关联显示

推荐名称 SHALL 以横向滑动的文字 Chip 标签（不包含任何图标）形式呈现。点击推荐 Chip 的行为为"直接开始计时"：系统停止当前所有活动事件，直接创建该名称的 IN_PROGRESS 事件并开始计时。

#### Scenario: 点击推荐 Chip 直接开始计时
- **WHEN** 用户点击"建议快速开始"区域中的推荐 Chip
- **THEN** 系统停止当前所有活动事件
- **AND** 若当天已有同名 PENDING 事件，系统 SHALL 将该 PENDING 事件转为 IN_PROGRESS（而非创建新事件）
- **AND** 若当天无同名 PENDING 事件，系统 SHALL 创建新的 IN_PROGRESS 事件并开始计时
- **AND** 新开始或复用的事件 SHALL 将 dayKey 设置为今天的日期
- **AND** 用户无需二次点击"开始"按钮

#### Scenario: 推荐项已存在完成事件
- **WHEN** 推荐名称在当天已有 COMPLETED 状态的事件
- **THEN** 该推荐项显示为已完成状态，显示完成时间，不包含任何图标

#### Scenario: 推荐项存在进行中事件
- **WHEN** 推荐名称在当天已有 IN_PROGRESS 状态的事件
- **THEN** 该推荐项高亮显示，实时显示已用时间，不包含任何图标

#### Scenario: 推荐项存在待办事件
- **WHEN** 推荐名称在当天已有 PENDING 状态的事件
- **THEN** 该推荐项显示待办状态，点击直接将其转为 IN_PROGRESS 并开始计时（而非仅添加到列表）

### Requirement: 推荐列表刷新机制

推荐列表 SHALL 在特定时期刷新。

#### Scenario: 切换日期时刷新推荐
- **WHEN** 用户切换到不同日期
- **THEN** 推荐列表根据新日期重新计算（基于新日期往前14天）

#### Scenario: 事件完成时刷新推荐
- **WHEN** 用户完成一个事件
- **THEN** 推荐列表的状态同步更新

#### Scenario: 推荐项当天已完成则过滤
- **WHEN** 推荐名称在当天已有 COMPLETED 状态的事件
- **THEN** 该推荐名称 SHALL 从推荐列表中移除，不再显示给用户
