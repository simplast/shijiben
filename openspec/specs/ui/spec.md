# Current: User Interface

## CURRENT Requirements

### Requirement: Home Screen
首页显示事件列表和日统计。

#### HomeScreen 组件
- 日期选择条（底部 `DayPickerDialog`）
- 日统计卡片（今日事件数和耗时）
- 事件列表（LazyColumn）
- FAB（打开事件编辑器）

#### Scenario: 查看事件列表
- GIVEN 用户打开首页
- THEN 显示最近选择日期的事件列表
- 列表项显示：事件名称、开始—结束时间、耗时（分钟）、箭头

#### Scenario: 切换日期
- GIVEN 用户点击底部日期条
- THEN 弹出 `DayPickerDialog` 选择新日期
- 选择后列表刷新

### Requirement: Bottom Sheet Editor
事件编辑器通过底部半屏面板打开。

#### EventEditorScreen 组件
- 模态底部面板（`ModalBottomSheet`）
- `EventEditorContent` 负责实际编辑区域
- 支持新建和编辑两种模式（通过 `eventId` 区分）

#### Scenario: 新建事件
- GIVEN `eventId == null`
- THEN 显示空表单，底部有"保存"按钮
- AssistChip 快捷名称仅在**新建模式**下显示
- 保存走 `viewModel.upsert()`

#### Scenario: 编辑事件
- GIVEN 用户点击已有事件打开编辑器
- THEN 预填充事件数据
- 保存时更新现有事件

### Requirement: Focus Management
事件名称输入框的焦点管理。

#### Scenario: 自动聚焦
- GIVEN 编辑器打开且无事件 ID
- WHEN 名称输入框获得焦点
- THEN 键盘弹出，可立即输入

#### Constraint
- `FocusRequester` 必须配合 `Modifier.focusRequester()`，在事件回调中 `requestFocus()`
- 不要在 composition 中直接请求焦点

### Requirement: In-Progress Display
正在进行的事件有特殊展示方式。

#### Scenario: 进行中事件列表项
- GIVEN 一个 status=IN_PROGRESS 的事件
- THEN 列表项右侧显示"进行中"（非实时累计分钟数）

### Requirement: Recent Name Chips
首页显示最近使用的事件名称快捷标签。

#### Scenario: 快速选择名称
- GIVEN 用户最近使用过多个事件名称
- THEN 首页显示名称 chips（去重、按最近使用排序、最多 10 条）
- WHEN 用户点击 chip
- THEN 快速填入事件名称

### Requirement: Navigation
使用 Jetpack Compose Navigation。

#### Scenario: 导航结构
- 入口：`ShijibenNavHost`
- 编辑入口通过回调 `onAddEvent` / `onOpenEvent`，**不是独立导航页**
