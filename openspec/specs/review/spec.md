# Current: Review & Goals

## CURRENT Requirements

### Requirement: Weekly Review
首页可导航到本周回顾页面。

#### WeeklyReviewScreen 功能
- 展示近 7 天的事件汇总
- 显示每周总耗时和记录天数
- 饼图展示 Top 5 事件时间分布
- 目标进度条（如有设定目标）
- 每日进展列表
- 导出数据（JSON）
- 跳转到月度回顾

#### Scenario: 查看周回顾
- GIVEN 本周有事件记录
- THEN 显示周统计卡片（总耗时 + 天数）
- 饼图展示时间分布
- 下方列出每日进展

### Requirement: Monthly Review
周回顾页面可导航到月度回顾页面。

#### MonthlyReviewScreen 功能
- 展示当月事件汇总
- 显示月度总耗时和记录天数
- 饼图展示时间分布
- 按分类统计（如果有分类数据）
- 时间分布 Top 事件

#### Scenario: 查看月回顾
- GIVEN 本月有事件记录
- THEN 显示月度统计卡片
- 分类统计按耗时排序

### Requirement: Goal Setting
用户可为每周设定目标时长。

#### GoalEntity 字段
- `id`: 主键，自增
- `name`: 目标名称（关联到事件名称）
- `targetMinutes`: 目标时长（分钟）
- `weekKey`: 年份-周数键（ISO 周，格式如 "2024-W20"）

#### Scenario: 设定目标
- GIVEN 用户在周回顾页面
- WHEN 点击"设定目标"
- THEN 弹出对话框输入名称和目标小时数
- 目标小时数自动转换为分钟

#### Scenario: 查看目标进度
- GIVEN 已设定了某个事件的目标
- THEN 周回顾页面显示线性进度条
- 进度 = 实际耗时 / 目标耗时（上限 1.0）
- 达成时显示"🎉 目标已达成！"

### Requirement: Weekly Stats Calculation
周统计数据基于事件的实际耗时。

#### 计算规则
- 只统计 `status = COMPLETED` 的事件
- 耗时 = `(endTimeMillis - startTimeMillis) / 60_000`
- 按 `dayKey` 分组计算每日汇总
- 按事件名称分组计算 Top 事件
- 目标进度关联同名事件的累计耗时

### Requirement: Monthly Stats Calculation
月统计数据基于事件的实际耗时。

#### 计算规则
- 只统计 `status = COMPLETED` 的事件
- 按 `category` 字段分组（如有）作为分类统计
- 按事件名称分组作为时间分布

### Requirement: Data Export
用户可将事件数据导出为 JSON。

#### Scenario: 导出 JSON
- GIVEN 用户在周回顾页面
- WHEN 点击导出按钮
- THEN 系统使用 `ActivityResultContracts.CreateDocument("application/json")`
- 文件名格式：`shijiben_export_<timestamp>.json`
- 导出成功后显示"导出成功"

### Requirement: Navigation to Review
从首页可导航到回顾页面。

#### Scenario: 进入周回顾
- GIVEN 用户在首页
- WHEN 点击周回顾入口
- THEN 打开 `WeeklyReviewScreen`，顶部有返回按钮
