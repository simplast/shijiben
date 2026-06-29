# Plan: correctness cycle 6 — F006

## Finding (F006)
`RecordingViewModel.save()` 在编辑已完成事件并把 duration 调到 0 时，会产生**非法状态**：
`status = Completed` 但 `endTime = null`。

### 证据（symbol-level 引用）
文件：`app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`

- `save()` 中的 `actualEnd` 计算：
  `actualEnd = if (duration > 0) minutesToTimestamp(...) else null`
  → 当 `duration == 0` 时 `actualEnd == null`
- `save()` 中的 `status` 推算：
  `duration == 0 -> originalStatus ?: EventStatus.NotStarted.value`
  → 当 `originalStatus == Completed` 时，`status = Completed`，但 `endTime = null`

### 不变式（来自 EventRepository）
- `markCompleted`：同时设置 `endTime` 与 `Completed`（Completed 必须 endTime 非空）
- `markInProgress`：设置 `endTime = null` 与 `InProgress`
- `determineStatus`：`endTime == null && startTime <= now` → `InProgress`

→ 结论：`Completed + endTime=null` 违反状态机不变式，是非法状态。

### 触发场景
1. 用户打开一个已完成事件（`initEdit` 走 Completed 分支，`durationMinutes` = 实际时长）
2. 用户把 duration 滑块拖到 0
3. 用户点保存
4. 结果：事件被存为 `Completed + endTime=null`（非法）

## 修复
在 `save()` 的 `status` 推算中，当 `duration == 0` 且 `originalStatus == Completed` 时，
**降级为 InProgress**（重新打开事件，与 `markInProgress` 语义一致：无 endTime 即未完成）。
NotStarted / InProgress / null 保持原行为（这三种状态本身就允许 endTime=null）。

### 改动点（symbol-level）
`RecordingViewModel.save()` 内的 `status` when 表达式第一分支：

改前：
```kotlin
duration == 0 -> originalStatus ?: EventStatus.NotStarted.value
```

改后：
```kotlin
duration == 0 -> if (originalStatus == EventStatus.Completed.value) {
    EventStatus.InProgress.value
} else {
    originalStatus ?: EventStatus.NotStarted.value
}
```

## 回归测试
在 `RecordingViewModelTest` 新增一个测试，覆盖该场景：
`save_editingCompletedEventWithZeroDuration_downgradesToInProgress`

- 构造一个 Completed 事件（有 endTime）
- `initEdit` → `onDurationChange(0)` → `save`
- 断言：`saved.status == InProgress`，`saved.endTime == null`

## 风险评估
- 影响面：仅 `RecordingViewModel.save()` 的 status 推算分支
- 不改 `initEdit`、不改 DB schema、不改 EventRepository
- 现有测试 `save_editingInProgressEvent_preservesNullEndTimeAndStatus` 仍通过（InProgress 不进 Completed 分支）
- 现有测试 `save_editingCompletedEvent_keepsEndTime` 仍通过（不调 `onDurationChange(0)`，duration 保持原值 > 0）

## Gate
`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
