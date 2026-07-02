# Findings: architecture

## F009 — DONE
- 文件：`app/src/main/java/com/shijiben.data.repository/EventRepository.kt`
- 符号：`EventRepository.shiftToTargetDay` 的 `newEnd` 计算分支
- 问题：`cal2` 被创建并赋值 `e.endTime` 但从未读取——死代码，误导读者以为参与计算（推测是「保留钟点」改「保留时长」后的残留）
- 修复：删除两行 `cal2` 代码，保留正确的 `newStart + dur` 时长保留逻辑（零行为变化）
- 验证：Cycle 7 的 5 个 shiftToTargetDay 直接单测 + carryOverNotStarted_* 集成测试全过
- 计划：`better/architecture/plan-cycle-9.md`

## F019 — DataImportManager 依赖 feature 层 TimeVizPrefs（data→feature 反向依赖）
- status: DONE (cycle 19)
- evidence: app/src/main/java/com/shijiben/data/export/DataImportManager.kt:8
- impact: M

## F029 — HeatmapMonthTab 与 HeatmapYearTab 各有一份逐字相同的 Legend() + PixelArrowBox() 副本（DRY 漂移风险）
- status: DONE (cycle 29)
- evidence: app/src/main/java/com/shijiben/feature/heatmap/HeatmapMonthTab.kt:138,246 + HeatmapYearTab.kt:153,232
- impact: M
- cycle: 29
- 问题：两份完全相同的私有函数副本，调整色阶/箭头尺寸/边框色需同步改两处，易漂移。
- 修复：新增 `HeatmapCommon.kt` 集中 `HeatmapLegend()` + `PixelArrowBox()` 为 internal 共享函数；两 Tab 删除本地副本，调用点改用共享版本；清理两 Tab 因删除副本而失效的 imports（Icon/ImageVector 等）。
