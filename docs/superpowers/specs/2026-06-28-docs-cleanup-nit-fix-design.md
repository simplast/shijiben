# 文档打磨 + dead code 清理 + 顶栏回调风格统一 + spec 字面偏离修正

日期：2026-06-28
范围：本轮（iter 12）为纯打磨 nit 修复，四件小事组合，全部 S 工作量。① 文档对齐：design doc 与 AGENT.md 的 V3 描述 / 项目结构 feature 列表更新到当前实现现状；② dead code 删除：TimelineScreen 三个未引用 private 函数清理；③ 顶栏回调风格统一：TimelineScreen 顶栏回调参数全无默认值；④ spec 字面偏离修正：search-design.md §4.4.1 对齐实现去掉 `= {}`
前置：search / notes / 年视图 / 数据导入均已落地；TimelineScreen 顶栏回调现状为 4 个有默认值 + `onSearchClick` 无默认值
迭代：iter 12（Discover 推荐 + orchestrator 6 决策）

## 验证门（四道闸全绿）

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --rerun-tasks`
- `./gradlew assembleDebug`
- `./gradlew assembleRelease --rerun-tasks`

## 一、问题

1. **文档过时**：`docs/2026-06-22-shijiben-design.md` 项目结构 feature 列表缺 `notes/`、`search/`，`heatmap/` 注释仅写月视图未提年视图；V3 分期未反映数据导入 / 年视图 / 搜索已落地。`AGENT.md` V3 描述同样缺失数据导入 / 搜索 / 年视图。
2. **dead code**：`TimelineScreen.kt` 三个 private 函数 `formatDate` / `hourOfDay` / `isPastDay` 全项目零调用（grep 确认仅函数定义处出现；`formatDateCompact` 仍被 `NoteRow` 调用，不在删除范围）。
3. **顶栏回调风格不一致**：`TimelineScreen` 顶栏 5 个回调中 `onSearchClick` 无默认值，`onNotesClick` / `onTimeVizClick` / `onHeatmapClick` / `onSettingsClick` 仍带 `= {}`，与既有方向（`HeatmapScreen.onYearClick` 等全无默认值）不一致。
4. **spec 字面偏离**：`docs/superpowers/specs/2026-06-28-search-design.md` §4.4.1 写 `onSearchClick: () -> Unit = {}`，实现已无默认值。

## 二、目标

- design doc 与 AGENT.md 文档对齐当前实现现状
- 删除 TimelineScreen 三个未引用 private 函数
- TimelineScreen 顶栏回调全无默认值，与 `onSearchClick` / `onYearClick` 既定方向一致
- search-design.md §4.4.1 字面修正对齐实现

## 三、非目标

- 不做 M1-legacy 迁移（iter 3 已决策不做）
- 不做 B2-legacy 改动（触及 flaky 稳定区）
- 不做窄屏调整（需真机回归）
- 不改 DB schema、不改既有 ViewModel 业务逻辑、零新依赖
- 不做联网功能（绝对约束）

## 四、设计

### §4.1 文档对齐

- `docs/2026-06-22-shijiben-design.md` 项目结构 feature 列表：补 `notes/`（随笔列表 / 编辑）、`search/`（搜索）；`heatmap/` 注释补「年视图」
- `docs/2026-06-22-shijiben-design.md` V3 分期：补「数据导入」「年视图」「搜索」三项已落地
- `AGENT.md` V3 描述：补「数据导入 / 搜索 / 年视图」已落地

### §4.2 dead code 删除

删除 `TimelineScreen.kt` 中三个 private 函数：
- `formatDate(date: Triple<Int, Int, Int>): String`
- `hourOfDay(timestamp: Long): Int`
- `isPastDay(date: Triple<Int, Int, Int>): Boolean`

保留 `formatDateCompact`（仍被 `NoteRow` 调用）。

### §4.3 顶栏回调统一（orchestrator 决策 1：选项 B）

去掉 `TimelineScreen` 顶栏 4 个回调参数的 `= {}` 默认值：
- `onNotesClick`
- `onTimeVizClick`
- `onHeatmapClick`
- `onSettingsClick`

统一后 5 个顶栏回调（含 `onSearchClick`）全无默认值。`targetDate` / `onDateApplied` / `viewModel` / `notesViewModel` 等非顶栏回调参数不在本轮调整范围。偏离 Discover 推荐 A 的理由：与既定方向一致 > 最小改动；`HeatmapScreen.onYearClick` 等已无默认值，本轮让 TimelineScreen 全无默认值与之对齐。

### §4.4 spec 字面修正（orchestrator 决策 3：选项 A）

`docs/superpowers/specs/2026-06-28-search-design.md` §4.4.1 第 1 项：`onSearchClick: () -> Unit = {}` → `onSearchClick: () -> Unit`，与 §4.3 联动（全无默认值）。

## 五、涉及文件清单

改 3 + spec 修正 1：
- `docs/2026-06-22-shijiben-design.md`（feature 列表 + V3 分期）
- `AGENT.md`（V3 描述）
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`（删 3 函数 + 4 回调去默认值）
- `docs/superpowers/specs/2026-06-28-search-design.md`（§4.4.1 字面修正）

## 六、边界情况

回调去默认值后调用处影响：`TimelineScreen` 唯一调用方为 `AppNavHost.kt`（grep 确认），且始终全传 5 个顶栏回调，去默认值对调用处零影响，编译期安全最大化。

## 七、测试清单

无新增测试。四道闸全绿验证回归，特别确认：
- TimelineScreen 删除 3 函数后编译通过（无残留引用）
- TimelineScreen 顶栏回调去默认值后 AppNavHost 编译通过
- assembleRelease 全绿（proguard 无新引用告警）
