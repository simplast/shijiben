# Findings: dx

## F011 — DONE (cycle 11)
- 文件：`app/build.gradle.kts` 的 `dependencies` block 中 `testImplementation` 块
- 符号：5 处硬编码版本的 testImplementation（junit / robolectric / androidx.test:core / kotlinx-coroutines-test / truth）
- 问题：生产依赖统一走 `rootProject.extra`，但 5 处测试依赖硬编码版本，其中 coroutines-test 与生产 coroutines 重复定义版本号——版本升级时容易漏改，coroutines 主/测试版本不同步风险
- 修复：提取 4 个新 extra key（junit / robolectric / androidxTestCore / truth），coroutines-test 复用既有 `coroutines` key，5 处 testImplementation 全改用 extra 引用
- evidence：`app/build.gradle.kts` dependencies.testImplementation 块（junit:4.13.2 / robolectric:4.13 / androidx.test:core:1.6.1 / kotlinx-coroutines-test:1.8.1 / truth:1.4.4）
- impact：S

## F021 — 依赖版本管理迁移到 Gradle version catalog（libs.versions.toml）
- status: DONE (cycle 21)
- evidence: build.gradle.kts:9
- impact: M

## F031 — 修复 buildYearGrid today 重复标记，消除 2 个 baseline 失败
- status: DONE (cycle 31)
- 文件：`app/src/main/java/com/shijiben/feature/heatmap/HeatmapCalculator.kt`
- 符号：`HeatmapCalculator.buildYearGrid`
- 问题：`buildYearGrid` 复用 `buildGrid` 构建 12 个月网格，但 `buildGrid` 会把 today 标记到 padding 格（上下月补位）。年视图中今天的日期会在相邻月 padding（如 6 月末补位 7/2）+ 当前月实体格（7 月 7/2）各出现一次，两处都标 `isToday=true`，导致 `yearGrid_todayMarkedExactlyOnce` 断言 `hasSize(1)` 失败（实际 hasSize(2)）。同样影响 `yearGrid_updatesWhenRepoEmitsNewData`，其 `.first { it.isToday }` 可能找到 padding 格（durationMs=0）而非真实格。
- 修复：在 `buildYearGrid` 内对每月 `buildGrid` 返回的 raw cells 做后处理——padding 格（`isInMonth=false`）一律 `copy(isToday = false)`，仅当前月实体格保留 isToday 标记。月视图语义不变（`buildGrid` 本身未改）。
- evidence：`HeatmapCalculator.kt` buildYearGrid 函数；测试报告 `HeatmapYearViewModelTest.html` 8 tests / 0 failures / 100%
- impact：M（修复真实 bug + 消除 2 个长期 baseline 失败，gate 从此更干净）
