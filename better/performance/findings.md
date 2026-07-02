# Findings: performance

## F016 — TimelineScreen `now` State read at top level triggers whole-tree recomposition every 60s
- status: DONE (cycle 16)
- evidence: app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt:122
- impact: L

## F026 — SearchViewModel fires full filter+merge+sort on every keystroke (no debounce)
- status: DONE (cycle 26)
- evidence: app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt:46
- impact: M
- cycle: 26
- 修复：新增 `debouncedQuery` StateFlow（`_query.debounce(150L).distinctUntilChanged().stateIn(...)`），`combine` 改用此流。UI 文本框仍绑定 `query`（无防抖）保证即时显示。

## F036 — buildYearGrid 12 次全量 filter + TimeAllocationTab LazyColumn 缺 key
- status: DONE (cycle 36)
- evidence:
  - app/src/main/java/com/shijiben/feature/heatmap/HeatmapCalculator.kt (buildYearGrid)
  - app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationTab.kt (AllocationList)
- impact: M
- cycle: 36
- 修复：
  1. **buildYearGrid**：12 次 `activities.filter { YearMonth.from(it.date) == yearMonth }`（O(12N) 迭代 + 12 中间列表分配）改为单次 `groupBy { YearMonth.from(it.date) }`（O(N)）+ 逐月 `byMonth[ym].orEmpty()` 查表。全年 365 天活动：4380 次迭代 → 365 次。
  2. **TimeAllocationTab.AllocationList**：`itemsIndexed(items)` 加 `key = { i, item -> "$i:${item.title}" }`，列表内容变化（切换范围）时按 title 稳定身份复用 row 组合，避免整列重建。
- 测试：新增 3 个 groupBy 回归测试（allMonthsReceiveActivity / multipleActivitiesSameMonth / activitiesFromOtherYearIgnored），覆盖分桶完整性、同月多条、跨年过滤。
