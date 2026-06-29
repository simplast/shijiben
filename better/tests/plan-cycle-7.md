# Plan: tests cycle 7 — F007

## Finding (F007)
`EventRepository.shiftToTargetDay` 是 carry-over（自动顺延）的核心日历逻辑，
但**零直接单测**——仅通过 5 个 DB 集成测试（`carryOverNotStarted_*`）间接覆盖。

### 证据（symbol-level 引用）
文件：`app/src/main/java/com/shijiben/data/repository/EventRepository.kt`

- `shiftToTargetDay(e, year, month, day): Pair<Long, Long?>?`（internal，可直接测）
  - 用 `Calendar` 把 `e.startTime` 的 hour:minute 平移到目标日
  - `newStart == e.startTime` → 返回 null（已在目标日，无需顺延）
  - `e.endTime != null` → `newEnd = newStart + (e.endTime - e.startTime)`（保持时长）
  - `e.endTime == null` → `newEnd = null`
- 测试现状（grep `shiftToTargetDay` in `app/src/test`）：仅 1 处注释引用，**无直接调用**

### 为什么重要
- 复杂日历算术（跨月/跨年边界、hour:minute 保留、时长保留）
- 是 `carryOverNotStarted` 的核心依赖——顺延错会污染时间轴数据
- 间接测试需要 DB（Robolectric，慢），直接测试可精确定位回归

## 修复（新增测试，不改源码）
在 `EventRepositoryTest` 新增 5 个直接单测（沿用 `determineStatus_*` 的直接调用模式）：

1. `shiftToTargetDay_preservesHourAndMinute` — 昨天 14:30 → 今天 14:30
2. `shiftToTargetDay_crossMonthBoundary` — 1月31日 → 2月1日（跨月边界）
3. `shiftToTargetDay_nullEndTime_returnsNullEnd` — 无 endTime → shifted 的 newEnd=null
4. `shiftToTargetDay_alreadyOnTargetDay_returnsNull` — 已在目标日 → 返回 null
5. `shiftToTargetDay_preservesDurationWithEndTime` — 10:00→11:00（1h）shifted 后时长仍 1h

## 风险评估
- 仅新增测试，不改源码 → 零生产代码风险
- 沿用现有测试类 setup（Robolectric + inMemory DB + `repo` 实例）
- 测试用真实 `Calendar`，与现有 `carryOverNotStarted_*` 测试一致

## Gate
`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
