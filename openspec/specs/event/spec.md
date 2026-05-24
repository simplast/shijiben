# Current: Event Management

## CURRENT Requirements

### Requirement: Event Storage
事件通过 Room 数据库持久化存储。

#### EventEntity 字段
- `id`: 主键，自增
- `name`: 事件名称（支持快捷名称）
- `startTimeMillis`: 开始时间戳
- `endTimeMillis`: 结束时间戳（IN_PROGRESS 时等于 startTimeMillis）
- `dayKey`: 日期键，由 startTimeMillis 推导（ISO_LOCAL_DATE 格式）
- `status`: `IN_PROGRESS` 或 `COMPLETED`

#### Scenario: 存储事件
- GIVEN 用户保存一个事件
- WHEN `EventRepository.upsert(event)` 被调用
- THEN 事件写入 Room 数据库（`events` 表）

### Requirement: Event Listing
按日期查询和展示事件列表。

#### Scenario: 查看某日事件
- GIVEN 用户选择了某一天
- WHEN `EventRepository.observeEventsForDay(dayKey)` 被调用
- THEN 返回该日所有事件，按创建顺序排列

### Requirement: Event Creation
用户通过首页创建新事件。

#### Scenario: 新建事件
- GIVEN 用户在首页
- WHEN 点击 FAB
- THEN 弹出 `ModalBottomSheet` 内的 `EventEditorContent`

#### Scenario: 保存事件
- GIVEN 用户在编辑器中填写了名称
- WHEN 点击保存
- THEN `EventViewModel.upsert(event)` 被调用，事件持久化

### Requirement: Event Editing
用户可修改已有事件。

#### Scenario: 编辑事件
- GIVEN 用户点击已有事件
- THEN 弹出同一编辑器，预填充事件数据
- WHEN 修改后保存
- THEN 事件被更新

### Requirement: Event Status
事件有两种状态：进行中（`IN_PROGRESS`）和已完成（`COMPLETED`）。

#### Scenario: 开始记录
- GIVEN 用户输入事件名称
- WHEN 保存
- THEN 事件状态为 `IN_PROGRESS`，startTimeMillis 和 endTimeMillis 相同

#### Scenario: 停止记录
- GIVEN 有一个正在进行的事件
- WHEN 用户调用 `stopActiveEvent()`
- THEN endTimeMillis 设为当前时间，status 变为 `COMPLETED`

### Requirement: Quick Names
系统记住最近使用的事件名称供快速选择。

#### Scenario: 获取快捷名称
- GIVEN 用户已保存过多个事件
- WHEN 打开编辑器
- THEN 显示最近 10 个不重复的事件名称（按最近使用排序）

### Requirement: Time Tracking
事件记录持续时长（分钟级精度）。

#### Scenario: 查看进行中事件耗时
- GIVEN 一个正在进行的事件
- THEN 列表项右侧显示"进行中"（非实时累计）

### Requirement: Smart Merge
系统可合并同名的连续事件。

#### Scenario: 合并事件
- GIVEN 多个同名且时间重叠或间隔 < 5 分钟的事件
- WHEN 用户触发 `smartMergeEvents()`
- THEN 被合并事件的 endTimeMillis 扩展到最晚值，多余事件被删除
