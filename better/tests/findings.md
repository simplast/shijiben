# Findings: tests

## F007 — DONE
- 文件：`app/src/main/java/com/shijiben/data/repository/EventRepository.kt`
- 符号：`EventRepository.shiftToTargetDay`（internal）
- 问题：零直接单测，仅通过 5 个 DB 集成测试（`carryOverNotStarted_*`）间接覆盖；跨月/跨年/时长保留等日历边界无精确定位测试
- 修复：新增 5 个直接单测到 `EventRepositoryTest`（preserve hour:minute / cross-month / null endTime / already-on-target / preserve duration），全部通过
- 计划：`better/tests/plan-cycle-7.md`
