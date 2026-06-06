# minimal-line-home Delta Spec

## Requirements

### Requirement: 首页待办区块 (Updated)

首页 SHALL 根据选择的日期显示待办或计划区块，但**历史视角下应隐藏待办事项**。

#### Scenario: 历史视角下隐藏待办
- **WHEN** 用户选择过去日期
- **THEN** 待办区块不显示任何 PENDING 状态的事件

#### Scenario: 自动移动过期待办
- **WHEN** 应用启动且检测到过去日期存在 PENDING 状态事件
- **THEN** 系统自动将这些事件的日期更新为今日
