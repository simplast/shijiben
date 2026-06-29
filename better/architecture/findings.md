# Findings: architecture

## F009 — DONE
- 文件：`app/src/main/java/com/shijiben/data/repository/EventRepository.kt`
- 符号：`EventRepository.shiftToTargetDay` 的 `newEnd` 计算分支
- 问题：`cal2` 被创建并赋值 `e.endTime` 但从未读取——死代码，误导读者以为参与计算（推测是「保留钟点」改「保留时长」后的残留）
- 修复：删除两行 `cal2` 代码，保留正确的 `newStart + dur` 时长保留逻辑（零行为变化）
- 验证：Cycle 7 的 5 个 shiftToTargetDay 直接单测 + carryOverNotStarted_* 集成测试全过
- 计划：`better/architecture/plan-cycle-9.md`
