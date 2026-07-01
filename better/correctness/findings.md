# Findings: correctness

## F006 — DONE
- 文件：`app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`
- 符号：`RecordingViewModel.save()` 的 `status` when 表达式
- 问题：编辑 Completed 事件并把 duration 调到 0 时，保存为 `Completed + endTime=null`（非法状态，违反 EventRepository 不变式：Completed 必须 endTime 非空）
- 修复：duration==0 且 originalStatus==Completed 时降级为 InProgress
- 回归测试：`RecordingViewModelTest.save_editingCompletedEventWithZeroDuration_downgradesToInProgress`
- 计划：`better/correctness/plan-cycle-6.md`

## F014 — 未来开始时间的事件被误标 InProgress（应为 NotStarted），热力图虚增幻影时长
- status: DONE (cycle 14)
- evidence: app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt:132
- impact: M
- 问题：`save()` 的 `status` when 表达式无 `start > now` 分支。当用户把开始时间设到未来并带 duration>0 时（或在未来日期视图新建事件），事件落入 `else -> InProgress`，违反 `EventRepository.determineStatus` 不变式（`startTime > now → NotStarted`）。
- 影响：(1) 热力图聚合 `aggregateMonth`/`aggregateYear` 对 `status != NotStarted` 计入时长，`effectiveDurationMs` 用 `event.endTime` 不 clamp 到 now，未来事件的完整时长被全额计入当天，虚增「尚未花费的幻影时间」；(2) 时间轴把未来事件显示为「进行中」；(3) 状态机不变式破坏。
- 修复：在 `save()` 的 `status` when 中 `duration==0` 分支之后加 `start > now -> EventStatus.NotStarted.value`（放在 duration==0 之后以保证 Completed 降级不变式优先）。
- 回归测试：`RecordingViewModelTest.save_futureStartEventWithDuration_markedNotStartedNotInProgress`
- 计划：`better/correctness/plan-cycle-14.md`
