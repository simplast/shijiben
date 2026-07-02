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

## F024 — initEdit 把 Completed 事件开始时间钳到 5:00 AM，编辑保存后凌晨事件被静默篡改
- status: DONE (cycle 24)
- evidence: app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt:88
- impact: M

## F034 — dayRange 用 +24h 算次日 0 点，DST 切换日漏查/多查 1 小时数据
- status: DONE (cycle 34)
- 文件：`app/src/main/java/com/shijiben/data/repository/EventRepository.kt` + `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`
- 符号：`EventRepository.dayRange`（私有，两处副本）→ 提取为顶层 `internal fun dayRangeMs(year, month, day, zone)`
- 问题：`end = start + 24L * 3600 * 1000` 在 DST 切换日错误——春进日（23h）end 偏到次日 01:00（多查 1h），秋退日（25h）end 偏到当日 23:00（漏查 1h）。影响 `getEventsByDate` / `getNotesByDateRange` 的 `WHERE startTime < end` 查询边界。用户在 Asia/Shanghai（无 DST）不受影响，但对 DST 时区是潜在数据错误。
- 修复：提取 `dayRangeMs` 顶层函数，用 `Calendar.add(Calendar.DAY_OF_MONTH, 1)` 替代 `+24h`——Calendar.add 按日历日推进，DST-aware。两处 `dayRange` 副本统一调用该函数（顺带消除 DRY 违规）。
- 回归测试：`DayRangeMsDstTest` 6 个用例——正常日 24h（上海/纽约）+ 春进日 23h + 秋退日 25h + 春进/秋退日 end 落在次日 00:00（非 01:00/23:00）
- evidence: EventRepositoryHeatmapTest.kt DayRangeMsDstTest；EventRepository.kt dayRangeMs
- impact: M（DST 时区数据正确性；用户当前时区无 DST 故无即时影响，但是潜在 bug 根除）
