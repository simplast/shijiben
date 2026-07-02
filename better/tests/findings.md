# Findings: tests

## F007 — DONE
- 文件：`app/src/main/java/com/shijiben/data/repository/EventRepository.kt`
- 符号：`EventRepository.shiftToTargetDay`（internal）
- 问题：零直接单测，仅通过 5 个 DB 集成测试（`carryOverNotStarted_*`）间接覆盖；跨月/跨年/时长保留等日历边界无精确定位测试
- 修复：新增 5 个直接单测到 `EventRepositoryTest`（preserve hour:minute / cross-month / null endTime / already-on-target / preserve duration），全部通过
- 计划：`better/tests/plan-cycle-7.md`

## F020 — TimeVizCalculator.todayProgress / yearProgress 零单测覆盖
- status: DONE (cycle 20)
- evidence: app/src/main/java/com/shijiben/feature/timeviz/TimeVizCalculator.kt:32
- impact: M

## F030 — TimeAllocationTab 的 rankColor + formatAllocationDuration 零单测覆盖（F028 新增纯函数）
- status: DONE (cycle 30)
- evidence: app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationTab.kt:149,158
- impact: M
- cycle: 30
- 问题：Cycle 28（F028）新增的 rankColor（排名色板分支 + 冷色循环）和 formatAllocationDuration（时长格式化）此前零覆盖。改色板顺序、改循环逻辑、改截断行为都会破坏视觉一致性而无人察觉。
- 修复：两函数从 private 提升为 internal 顶层函数（与 filterAndMerge 同模式），新增 TimeAllocationTabTest.kt 共 14 个测试：rankColor 6 个（前 4 名暖色 + 5-8 名冷色 + 循环回起点 + 相邻排名色不同）+ formatAllocationDuration 8 个（0/1min/59min/整小时/1h1m/2h/10h30m/亚分钟截断）。

## F040 — determineStatus 3 分支仅 2 覆盖，NotStarted 分支（cycle 26 bug 修复场景）零覆盖
- status: DONE (cycle 40)
- evidence: app/src/main/java/com/shijiben/data/repository/EventRepository.kt (determineStatus)
- impact: M
- cycle: 40
- 问题：determineStatus 是事件状态机的核心判定函数（ARCHITECTURE.md §8 文档化），3 个分支（InProgress/NotStarted/Completed）此前仅 2 个有直接单测（startTime==now 边界点）。NotStarted 分支（startTime > now）零覆盖——而此分支正是 cycle 26 修复的 bug 场景（RecordingViewModel.save() 此前误把 duration>0 的未来事件标 InProgress）。严格过去/未来的非边界点也无覆盖。
- 修复：新增 4 个分支测试到 EventRepositoryTest：pastStart_nullEnd_isInProgress（严格过去+无结束）、futureStart_nullEnd_isNotStarted（未来预写）、futureStart_withEnd_isNotStarted（未来+有结束仍 NotStarted，cycle 26 bug 场景）、pastStart_withEnd_isCompleted（严格过去+有结束=补录）。全分支+边界点 6 测试覆盖。
