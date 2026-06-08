# algo-recommendation Spec Delta

## MODIFIED Requirements

### Requirement: 推荐与事件的关联显示

推荐名称 SHALL 以横向滑动的文字 Chip 标签（不包含任何图标）形式呈现。点击推荐 Chip 的行为从"添加待办"改为"直接开始计时"。

#### Scenario: 点击推荐 Chip 直接开始计时
- **WHEN** 用户点击"建议快速开始"区域中的推荐 Chip
- **THEN** 系统停止当前所有活动事件，直接创建该名称的 IN_PROGRESS 事件并开始计时
- **AND** 用户无需二次点击"开始"按钮

#### Scenario: 推荐项存在待办事件
- **WHEN** 推荐名称在当天已有 PENDING 状态的事件
- **THEN** 该推荐项显示待办状态，点击直接将其转为 IN_PROGRESS 并开始计时（而非仅添加到列表）

## ADDED Requirements

### Requirement: 同名待办去重

系统 SHALL 阻止同一日期下创建同名 PENDING 事件。

#### Scenario: 添加同名待办被忽略
- **WHEN** 用户输入一个与当日已有 PENDING 事件同名（trim + 忽略大小写）的名称
- **THEN** 系统静默跳过，不创建重复事件

#### Scenario: 不同日期可存在同名待办
- **WHEN** 用户在不同日期添加同名待办
- **THEN** 系统允许创建（去重仅限同一天）

#### Scenario: 不同状态的同名事件不冲突
- **WHEN** 当日已存在名为"阅读"的 COMPLETED 事件，用户再添加"阅读"待办
- **THEN** 系统允许创建（仅检查 PENDING 状态的同名事件）
