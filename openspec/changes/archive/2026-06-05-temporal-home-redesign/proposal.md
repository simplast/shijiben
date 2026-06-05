## Why

目前主界面在不同日期（过去、现在、未来）下的显示内容和交互逻辑完全一致，缺乏时间维度的反馈。通过区分“历史视角”、“操作视角”和“规划视角”，可以更好地引导用户进行回顾、执行和规划。

## What Changes

根据 `selectedDate` 与当前日期的关系，调整主界面的 UI 和交互逻辑：

- **历史视角 (PAST, selectedDate < 今日)**：
    - 隐藏快速添加输入框。
    - 更新分区标题文案（如“那天已完成”、“已归档”）。
    - 移除“开始/结束”和“再来一次”按钮。
    - 空状态显示：“那天似乎什么也没发生”。
- **规划视角 (FUTURE, selectedDate > 今日)**：
    - 更新分区标题文案（如“已规划的任务”）。
    - 禁用“开始”操作。
    - 空状态显示：“这一天还很空，不如规划点什么？”。
- **操作视角 (TODAY, selectedDate == 今日)**：
    - 维持现有逻辑和文案。

## Capabilities

### New Capabilities
- 无

### Modified Capabilities
- `minimal-line-home`: 更新主界面需求，引入时间维度的差异化显示和交互逻辑。

## Impact

- `HomeScreen.kt`: 增加根据日期类型切换 UI 元素显示和文案的逻辑。
- `EventViewModel.kt`: 提供日期类型的判断逻辑或状态。
