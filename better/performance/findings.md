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
