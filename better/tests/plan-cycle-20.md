# Plan: tests cycle 20 — F020

## Finding (F020)
`TimeVizCalculator.todayProgress` 和 `yearProgress` 是**零单测覆盖**的纯函数。
它们在 `TimeVizViewModel.refreshState` 里被消费用于进度条填充（`TimeVizUiState.todayProgress` / `yearProgress`），
但现有 `TimeVizCalculatorTest` 仅覆盖 `todayRemaining` / `yearRemaining` / `lifeRemaining` 三个**格式化字符串**函数，
进度比例（Float，[0,1]）的边界、clamp、闰年分母等逻辑完全无回归保护。

### 证据（symbol-level 引用）
文件：`app/src/main/java/com/shijiben/feature/timeviz/TimeVizCalculator.kt`

- `todayProgress(nowMillis: Long): Float`（L32-37）
  - 用 `LocalDateTime.ofInstant(..., ZoneId.systemDefault())` 取本地时间
  - `elapsedMin / (24f * 60f)`，最后 `.coerceIn(0f, 1f)`
  - **未测边界**：00:00 → 0f；12:00 → 0.5f；23:59:59 → ~0.9999f；超界 clamp 行为
- `yearProgress(nowMillis: Long): Float`（L56-63）
  - `startOfYear`..`endOfYear` 总时长作为分母，`elapsed / total`，`.coerceIn(0f, 1f)`
  - **未测边界**：1-1 00:00 → 0f；12-31 23:59:59 → ~1f；**闰年分母差异**（2024 = 366 天 vs 2026 = 365 天）

### 测试现状（grep `todayProgress|yearProgress` in `app/src/test`）
**No matches found** — 零直接调用、零间接断言。
`TimeVizViewModelTest` 仅用 `matches("\\d+h \\d+m")` 校验字符串，**不断言 progress Float 值**。

### 为什么重要
1. **数据完整性**：进度条是用户最直观的「时间去向」可视化，比例算错（如 >1 或 <0）会撑爆 Compose 布局或显示空进度条
2. **闰年分母差异是真实边界**：2024 是闰年（366 天），2026 不是（365 天）；若有人改 `endOfYear` 计算方式（如改用 `Year.length()`），分母变化不会被现有测试捕获
3. **clamp 是契约**：`coerceIn(0f, 1f)` 是 UI 层的硬约束（Compose `progress = yearProgress` 期望 [0,1]），删 clamp 或改 coerce 范围不会被现有测试发现
4. **纯函数 + 时区注入**：与现有 `todayRemaining`/`yearRemaining` 同模式，确定性高、无 flaky 风险

## 修复（新增测试，不改源码）
在 `app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt` 末尾追加 7 个测试，
沿用现有 `@Before` 固定 `TimeZone.setDefault(UTC)` 模式 + `localMillis` 助手函数：

1. `todayProgress_atMidnight_returns0` — 2026-06-28 00:00:00 → 0f
2. `todayProgress_atNoon_returns0_5` — 2026-06-28 12:00:00 → 0.5f（精确等于）
3. `todayProgress_oneSecondBeforeMidnight_returnsAlmost1` — 23:59:59 → 接近 1f（< 1f 且 > 0.999f），验证 clamp 上界
4. `yearProgress_atStartOfYear_returns0` — 2026-01-01 00:00:00 → 0f
5. `yearProgress_atMidYear_returnsCloseToHalf` — 2026-07-02 12:00:00（年中附近）→ 介于 0.49 与 0.51 之间
6. `yearProgress_oneSecondBeforeYearEnd_returnsAlmost1` — 2026-12-31 23:59:59 → < 1f 且 > 0.9999f
7. `yearProgress_leapYearVsNonLeapYear_denominatorMatchesYearLength` —
   闰年 2024 与非闰年 2026 在 06-30 12:00 的 progress 比值 ≈ 365/366（验证闰年分母是 366 天而非 365）

### 边界值选取理由
- 00:00 / 12:00 / 23:59:59 三点覆盖一天的 0 / 0.5 / ~1 三大节点
- 1-1 00:00 / 7-2 12:00 / 12-31 23:59:59 覆盖一年的同三大节点
- 闰年对比测试专门盯 `Duration.between(startOfYear, endOfYear)` 在闰年是否正确为 366 天

## In-scope files（1 个文件）
- `app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt`（追加 7 个 @Test 函数）

## Out of scope
- 不修改 `TimeVizCalculator.kt` 源码
- 不修改 `TimeVizViewModel.kt` 或 `TimeVizScreen.kt`
- 不新增测试文件（追加到既有测试类）

## 风险评估
- 仅新增测试，零生产代码改动 → 零行为风险
- 沿用既有 `@Before` TZ 固定模式 → 确定性，无 flaky
- 全部用纯函数 + 显式 `now` 参数 → 无时钟依赖
- 浮点断言用区间（如 `isGreaterThan(0.999f)` `isLessThan(1f)`），避免精度抖动

## Gate
```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew assembleDebug
./gradlew :app:assembleRelease
```

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `HeatmapYearViewModelTest > yearGrid_updatesWhenRepoEmitsNewData`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
- `TimelineViewModelTest > markInProgress_validEventId_setsInProgressWithNullEndTime`

## 目录审计
- **审计**：`feature/timeviz/`（src + test）、`feature/heatmap/`（src + test，TimeAllocation）、`data/repository/`（src + test）、`data/export/`（src + test）
- **未审计**：`ui/theme/`、`ui/debug/`、`navigation/`、`di/`、`data/local/`、`data/model/`、`feature/notes/`、`feature/recording/`、`feature/search/`、`feature/settings/`、`feature/timeline/`（除 TimelineViewModelTest 外）、`MainActivity.kt`、`ShiJiBenApplication.kt`
