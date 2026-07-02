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
