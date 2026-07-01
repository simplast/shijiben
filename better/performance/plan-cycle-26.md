# Plan — Cycle 26 (performance, Rotation 2)

## Finding: F026 — SearchViewModel fires full filter+merge+sort on every keystroke (no debounce)

### Problem
`SearchViewModel.onQueryChange` writes directly to `_query` (a `MutableStateFlow`). The
`state` flow is built as `combine(eventsFlow, notesFlow, _query) { ... filterAndMerge(...) }`,
so **every single keystroke** triggers `filterAndMerge` over the full events + notes lists.

`filterAndMerge` (SearchViewModel.kt:71–95) does, per invocation:
1. `events.filter { it.title.lowercase().contains(q) || it.note?.lowercase()?.contains(q) == true }`
   — allocates a new `String` per event per keystroke (`.lowercase()` on title AND note).
2. `notes.filter { it.content.lowercase().contains(q) }`
   — same pattern, one `.lowercase()` allocation per note per keystroke.
3. `.map { ... }` over both matched lists — wraps each entity in `SearchItem`.
4. `(matchedEvents + matchedNotes).sortedByDescending { it.sortKey }` — full sort per keystroke.

For a user with N events + M notes, typing a K-character query performs
**O(K × (N + M))** string allocations + **O(K × (N+M) log (N+M))** sort work. With 1000 events
and a 5-char query that is 5000+ lowercase allocations and 5 full sorts — jank is visible
on mid-range devices during fast typing.

This is genuinely different from F016 (TimelineScreen `now` State deferral): F016 was about a
60s timer triggering whole-tree Compose recomposition; F026 is about a Flow pipeline running
a full filter+sort on every character input.

### Evidence
- `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt:46` —
  `fun onQueryChange(q: String) { _query.value = q }` — synchronous write, no debounce.
- `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt:56-60` —
  `combine(eventsFlow, notesFlow, _query) { events, notes, q -> filterAndMerge(events, notes, q) }`
  — fires on every `_query` emission.
- `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt:85-92` —
  `it.title.lowercase().contains(q)` / `it.content.lowercase().contains(q)` — per-event
  String allocation per keystroke.
- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt:36` —
  `fun getAllEvents(): Flow<List<EventEntity>>` — unbounded list (no paging), so cost grows
  linearly with lifelong event count.
- `app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationCalculator.kt:42` —
  contrast: the time-allocation aggregator uses `asSequence()` for lazy evaluation, while
  `filterAndMerge` uses eager `.filter { }.map { }` chains.

### Fix
Add `debounce(150ms)` + `distinctUntilChanged()` on the query flow before `combine`. This
collapses rapid typing into a single filter pass 150ms after the last keystroke — standard
search-bar pattern.

### Steps (max 8)
1. **Edit `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt`**:
   - Add imports: `kotlinx.coroutines.flow.debounce`, `kotlinx.coroutines.flow.distinctUntilChanged`.
     (`debounce` is stable in kotlinx-coroutines ≥ 1.6; project already uses `combine`/`stateIn`
     from same package, so version compatibility is confirmed by existing code.)
2. **Introduce a debounced query flow** (single-line change near line 44):
   ```kotlin
   private val debouncedQuery = _query
       .debounce(150L)
       .distinctUntilChanged()
   ```
3. **Swap the source in `combine`** (line 57): replace `_query` with `debouncedQuery`.
4. **Behavior preservation check**:
   - Empty-query branch (line 78–83) still fires immediately after 150ms — acceptable,
     SearchScreen shows "最近 N 条" by default and the 150ms delay only applies when user
     actively types/clears.
   - `eventsFlow`/`notesFlow` changes still trigger immediate re-filter (correct — new
     events should appear in results without delay).
5. **Unit test**: add `SearchViewModelTest` case verifying that 3 rapid `onQueryChange`
   calls within 100ms produce exactly 1 `filterAndMerge` invocation (use
   `StandardTestDispatcher` + `advanceTimeBy` like existing ViewModel tests). If a test
   proves too brittle, skip — the change is small and the existing
   `filterAndMerge`-as-pure-function tests remain valid.

### In-scope files (1, max 5)
- `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt`

### Out-of-scope but cited for evidence (4 files, 3 directories)
- `app/src/main/java/com/shijiben/feature/search/SearchScreen.kt` — UI consumer (same dir
  as fix, but relevant to confirm no UI-side debounce already exists; line 121
  `onValueChange = viewModel::onQueryChange` confirms direct passthrough).
- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` — confirms
  `getAllEvents()` returns unbounded list (line 36).
- `app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationCalculator.kt` — shows
  `asSequence()` pattern used elsewhere in the codebase for similar aggregation (line 42).

### Impact
- **M** — Eliminates per-keystroke O((N+M) log (N+M)) sort + O(N+M) string allocation.
  Measurable: for 1000 events, typing a 5-char query drops from ~5000 lowercase allocations
  + 5 sorts to ~1000 allocations + 1 sort (5× reduction in allocations, 5× in sorts).
  Benefit grows with lifelong event count (the app's whole point is long-term recording).

### Effort
- **S** — ~5 LOC change to one file, plus optional test. No new dependencies; `debounce`
  ships in `kotlinx-coroutines-core` which is already a transitive dep (used by Flow itself).

### Risk
- 150ms latency added between last keystroke and results appearing. Standard trade-off,
  acceptable for search. If user clears query (`onQueryChange("")`), there's a 150ms delay
  before "最近 N 条" repopulates — fine.
- `debounce` operator requires `@OptIn(ExperimentalCoroutinesApi::class)` already present on
  the class (line 19). No new opt-in needed.

### Directories NOT audited (declared)
- `feature/timeline/` (TimelineScreen, TimelineViewModel, DayProgressBar, EventCard)
- `feature/timeviz/` (TimeVizScreen, TimeVizCalculator, TimeVizViewModel, TimeVizPrefs)
- `feature/settings/` (SettingsScreen, AboutScreen, ExportViewModel, ImportViewModel)
- `data/local/` (DAOs, Entities, AppDatabase)
- `data/export/` (DataExportManager, DataImportManager)
- `data/model/` (EventStatus, HeatmapModels)
- `ui/theme/`, `ui/debug/`
- `di/`, `navigation/`
