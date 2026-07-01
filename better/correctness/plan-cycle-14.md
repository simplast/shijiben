# Plan: correctness cycle 14 — F014

## Finding (F014)
`RecordingViewModel.save()` 在事件开始时间处于**未来**（`start > now`）且 `duration > 0` 时，
把 status 置为 `InProgress`，违反状态机不变式：未来事件应为 `NotStarted`。

### 证据（symbol-level 引用）
文件：`app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`

- `save()` 内 `start` 计算（line 110）：`val start = minutesToTimestamp(y, m, d, _startMinutes.value)`
- `save()` 内 `now`（line 112）：`val now = System.currentTimeMillis()`
- `save()` 内 `status` 推算（lines 120-130）：
  ```
  duration == 0 -> ...                                // 不检查 start > now
  actualEnd != null && now > actualEnd -> Completed   // 不检查 start > now
  else -> InProgress                                  // ← BUG：吞掉 start > now 的未来事件
  ```
  当 `duration > 0` 且 `start > now` 且 `now <= actualEnd` 时，落入 `else` → `InProgress`。

### 不变式（来自 EventRepository.determineStatus）
```kotlin
if (endTime == null && startTime <= now) return InProgress
if (startTime > now) return NotStarted   // ← 未来事件必为 NotStarted，与 endTime 无关
return Completed
```
即：`startTime > now` → `NotStarted`，无论 endTime 是否为 null。`RecordingViewModel.save()`
绕过 `determineStatus` 自行推算 status，遗漏了 `start > now` 这一支，产生非法的
`InProgress + startTime>now` 状态。

### 触发场景
1. 用户在 10:00 打开记录弹窗，把开始时间拖到 20:00（未来），时长 60 分钟，保存
   → 事件被存为 `InProgress`（应为 `NotStarted`）
2. 用户切换到「明天」视图，新建一个 9:00-10:00 的事件并保存
   → 事件被存为 `InProgress`（应为 `NotStarted`，因为整件事都在未来）

### 影响
1. **热力图虚增时长**：`aggregateMonth`/`aggregateYear`（EventRepository.kt:222, 255）对
   `status != NotStarted` 的事件计入时长。未来事件被误标 InProgress 后，其完整时长
   （start→end，均在未来）会被加进当天的 `durationMs`，导致今天/本月热力图显示
   **尚未花费的幻影时间**（`effectiveDurationMs` 用 `event.endTime` 而非 `now`，
   不 clamp 到 now，故未来时长被全额计入）。
2. **时间轴误显示**：未来事件在时间轴上显示为「进行中」，误导用户。
3. **状态机不变式破坏**：`InProgress + startTime>now` 是 `determineStatus` 永不产生的状态。

注意：`getOngoingEvent()` 查询为 `endTime IS NULL AND status = 1`，未来事件有 endTime
（duration>0），不会误触发「当前进行中」单例，故此项不受影响——但上述 1/2/3 仍成立。

## 修复
在 `save()` 的 `status` when 表达式最前加一支：`start > now -> NotStarted`，
与 `determineStatus` 的 `startTime > now → NotStarted` 对齐，覆盖 duration==0 与
duration>0 两条路径。

### 改动点（symbol-level）
`RecordingViewModel.save()` 内 `status` when 表达式，新增第一分支：

改前（lines 120-130）：
```kotlin
val status = when {
    duration == 0 -> if (originalStatus == EventStatus.Completed.value) {
        EventStatus.InProgress.value
    } else {
        originalStatus ?: EventStatus.NotStarted.value
    }
    actualEnd != null && now > actualEnd -> EventStatus.Completed.value
    else -> EventStatus.InProgress.value
}
```

改后：
```kotlin
val status = when {
    // 开始时间在未来 → 未开始（与 determineStatus 不变式对齐：startTime > now → NotStarted，
    // 与 endTime/duration 无关）。防止未来事件被误标 InProgress 而在热力图中虚增时长。
    start > now -> EventStatus.NotStarted.value
    duration == 0 -> if (originalStatus == EventStatus.Completed.value) {
        EventStatus.InProgress.value
    } else {
        originalStatus ?: EventStatus.NotStarted.value
    }
    actualEnd != null && now > actualEnd -> EventStatus.Completed.value
    else -> EventStatus.InProgress.value
}
```

### 为什么放最前
- `start > now` 时，无论 duration 是否为 0、原状态是什么，事件都尚未开始，必为 NotStarted。
  Completed/InProgress 的原状态在「开始时间移到未来」后不再有意义。
- 不影响 `start <= now` 的所有既有路径（duration==0 降级、Completed 补录、InProgress 进行中）。

## 回归测试
在 `RecordingViewModelTest` 新增一个测试，覆盖未来开始时间场景：
`save_futureStartEventWithDuration_markedNotStartedNotInProgress`

- viewingDate 取明天（任意开始时间都在未来，避免边界分钟翻转）
- `initNew()` → `onStartChange(540)`（9:00）→ `onDurationChange(60)` → `onTitleChange("明天事件")` → `save(明天)`
- 断言：`saved.status == NotStarted`，`saved.endTime` 非空（duration>0 仍产生 endTime），`saved.endTime != null`

## 现有测试不受影响（已逐一核对）
- `save_editingInProgressEvent_preservesNullEndTimeAndStatus`：startTime = now-60_000（过去），`start > now` 为 false → 走原 duration==0 分支，保持 InProgress。✓
- `save_editingCompletedEvent_keepsEndTime`：startTime = now-7200_000（过去），`start > now` 为 false → 走原路径。✓
- `save_editingCompletedEventWithZeroDuration_downgradesToInProgress`：startTime 过去，`start > now` 为 false → 走 duration==0 降级分支。✓
- `save_newEvent_hasNonNullEndTime`：`initNew()` 把 start 向下取整到 15 分钟（必 <= now），`start > now` 为 false → 走原路径，endTime 非空。该测试只断言 endTime 非空与 title，不断言 status。✓
- 其余测试（blankTitle / deletedEvent / delete / initEdit 系列 / initNew 系列）：不涉及未来开始时间。✓

## In-scope files
1. `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`（修复）
2. `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt`（回归测试）

## Steps
1. 在 `RecordingViewModel.save()` 的 `status` when 表达式最前新增 `start > now -> EventStatus.NotStarted.value` 分支
2. 在 `RecordingViewModelTest` 新增 `save_futureStartEventWithDuration_markedNotStartedNotInProgress` 回归测试
3. 跑 gate：`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

## 风险评估
- 影响面：仅 `RecordingViewModel.save()` 的 status 推算分支，新增一支，不改既有分支
- 不改 DB schema、不改 EventRepository、不改 DAO
- `initNew()` 的 start 向下取整保证 <= now，新建「当下」事件不受影响（仍走 InProgress/Completed 原路径）
- 仅在用户显式把开始时间设到未来、或在未来日期视图新建事件时改变行为（从错误的 InProgress 改为正确的 NotStarted）

## Gate
`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
