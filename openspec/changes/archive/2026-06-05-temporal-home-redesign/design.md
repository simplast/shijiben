## Context

当前 `EventViewModel` 通过 `selectedDate` (StateFlow<LocalDate>) 维护用户选择的日期。UI 层（`HomeScreen.kt`）直接订阅该日期并展示相关事件。我们需要在 UI 和 ViewModel 之间引入一个“时间视角”的概念，以便根据日期动态调整显示和交互。

## Goals / Non-Goals

**Goals:**
- 在 `EventViewModel` 中定义日期的三维视角（过去、现在、未来）。
- 在 `HomeScreen` 中根据视角动态调整文案、可见性和按钮交互。
- 优化空状态展示，使其具备时间感。

**Non-Goals:**
- 不涉及数据库 Schema 的修改。
- 不引入复杂的排程/日历视图逻辑。
- 暂不实现“复制到今日”功能。

## Decisions

### 1. 引入 `DatePerspective` 枚举
在 `EventViewModel` 中定义一个内部枚举或单独的类，用于表示当前视角。

```kotlin
enum class DatePerspective {
    PAST, TODAY, FUTURE
}
```

### 2. ViewModel 暴露视角状态
通过 `selectedDate` 派生出一个 `perspective` Flow，方便 UI 订阅。

```kotlin
val datePerspective: StateFlow<DatePerspective> = _selectedDate.map { date ->
    val today = LocalDate.now()
    when {
        date < today -> DatePerspective.PAST
        date > today -> DatePerspective.FUTURE
        else -> DatePerspective.TODAY
    }
}.stateIn(...)
```

### 3. HomeScreen 的条件渲染
- **快速添加 (QuickNameLine)**: 仅在 `perspective != PAST` 时显示。
- **区块标题**: 根据 `perspective` 传入不同的 `text` 给 `CompletedSection` 和 `PendingSection`。
- **行操作 (EventRowWithDelete)**: 将 `perspective` 传递给行组件，根据视角显示或隐藏“开始”、“停止”或“再来一次”按钮。

### 4. 空状态文案
在 `EmptyLine` 组件中增加文案参数，或者在 `HomeScreen` 中根据视角选择传参。

## Risks / Trade-offs

- **[Risk] 时区一致性** → **[Mitigation]** 统一使用 `java.time.LocalDate.now()` 并在比较时确保一致。
- **[Trade-off] 代码复杂度** → 在 `HomeScreen` 中增加 `when` 分支会略微增加 Composable 的复杂度，但通过解耦文案逻辑可以保持代码整洁。
