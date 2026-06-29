# Findings: correctness

## F006 — DONE
- 文件：`app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt`
- 符号：`RecordingViewModel.save()` 的 `status` when 表达式
- 问题：编辑 Completed 事件并把 duration 调到 0 时，保存为 `Completed + endTime=null`（非法状态，违反 EventRepository 不变式：Completed 必须 endTime 非空）
- 修复：duration==0 且 originalStatus==Completed 时降级为 InProgress
- 回归测试：`RecordingViewModelTest.save_editingCompletedEventWithZeroDuration_downgradesToInProgress`
- 计划：`better/correctness/plan-cycle-6.md`
