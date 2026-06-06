# algo-recommendation Delta Spec

## Requirements

### Requirement: 推荐列表显示范围 (New)

推荐列表 SHALL **仅在“今日”视角下显示**。

#### Scenario: 今日视角下显示推荐
- **WHEN** 用户处于今日视图
- **THEN** 系统显示计算出的 5 个推荐项

#### Scenario: 非今日视角下隐藏推荐
- **WHEN** 用户处于历史或未来视图
- **THEN** 推荐列表为空，不显示任何推荐项
