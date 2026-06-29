# Plan: architecture cycle 9 — F009

## Finding (F009)
`EventRepository.shiftToTargetDay` 内有死代码：`cal2` 被创建并赋值但**从未读取**。

### 证据（symbol-level 引用）
文件：`app/src/main/java/com/shijiben/data/repository/EventRepository.kt`

`shiftToTargetDay` 的 `newEnd` 计算分支：

```kotlin
val newEnd: Long? = if (e.endTime != null) {
    val cal2 = Calendar.getInstance(TimeZone.getDefault())  // 创建
    cal2.timeInMillis = e.endTime                           // 赋值
    val dur = e.endTime - e.startTime                       // dur 直接用 e 计算
    newStart + dur                                         // 返回值只用 newStart + dur
} else null
```

- `cal2` 创建 + 赋值后，**无任何读取**
- 返回值 `newStart + dur` 仅依赖 `e.endTime`、`e.startTime`、`newStart`
- 推测：原作者本意是用 `cal2` 提取 endTime 的 wall-clock hour:minute（保留钟点而非时长），
  但最终改为「保留时长」方案，`cal2` 残留未删

### 影响
- 死代码误导读者，暗示 `cal2` 参与计算（实际没有）
- 静态分析/lint 会标记 unused variable
- 零行为影响（删除不改变输出）

## 修复
删除两行 `cal2` 代码，保留正确的时长保留逻辑：

改后：
```kotlin
val newEnd: Long? = if (e.endTime != null) {
    val dur = e.endTime - e.startTime
    newStart + dur
} else null
```

无 import 变更（`Calendar` / `TimeZone` 仍被同函数的 `cal` 及文件其他函数使用）。

## 风险评估
- 纯删除死代码，零行为变化
- Cycle 7 新增的 5 个 `shiftToTargetDay_*` 直接单测 + 既有 `carryOverNotStarted_*` 集成测试会确认无回归
- 无新测试需要（行为不变，现有测试已覆盖）

## Gate
`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
