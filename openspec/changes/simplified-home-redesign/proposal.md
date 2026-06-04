## Why

当前首页仍显复杂，"常用"推荐区块以 chips 形式展示，用户需要主动点击才能添加待办。这种设计不够直观，用户需要一个更清晰的工作流：明确知道今天要做什么事，完成后自动显示在已完成区域。

## What Changes

- **首页区块重组**：顶部显示"今天已完成"列表，底部显示"今日待办"列表，区块分离，视觉清晰
- **推荐算法替代常用chips**：移除现有的"常用"快捷chips，改用算法推荐的"今日待办"
- **推荐算法逻辑**：统计最近14天内每个事件名称的总出现次数，取前5名，按次数降序排列
- **再来一次**：已完成的事件可点击"再来一次"，创建新的 PENDING 待办（支持同事件一天多次完成）
- **智能合并默认化**：事件结束时自动检查并合并同类事件（间隔<5分钟）
- **简化操作**：删除操作无需确认弹窗

## Capabilities

### New Capabilities

- `algo-recommendation`: 基于历史数据算法的待办推荐能力，包括推荐生成逻辑、推荐与事件的关联、推荐完成状态的展示
- `repeat-event`: 支持同一事件在同一天完成多次的能力，包括再来一次的语义和独立记录展示

### Modified Capabilities

- `minimal-line-home`: 首页的"频繁事件快捷方式"需求变更为"算法推荐待办"，不再显示10个快捷chips，而是显示算法推荐的待办列表；删除操作简化

## Impact

- `ui/screens/HomeScreen.kt`：大幅重构，移除 FrequentNameLines，新增已完成/未完成区块分离
- `data/EventDao.kt`：新增 `getTopEventNamesLast14Days()` 查询方法
- `ui/EventViewModel.kt`：新增推荐列表的状态管理，智能合并逻辑
- `data/EventEntity.kt`：无需修改，dayKey + name 组合已支持同事件多次完成
