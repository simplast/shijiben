# Plan — Cycle 16 (performance): F016

## Finding
**F016 — TimelineScreen `now` State read at top level triggers whole-tree recomposition every 60s**

`TimelineScreen` uses `val now by produceState(...)` with a 60-second tick. The `by`
delegate reads the State value at the **top level of `TimelineScreen`**, so the entire
composable recomposes every 60 seconds. The recomposition cascades through:

1. `EventList(now = now)` — receives `now` → recomputes
2. **ALL `EventCard(now = now)`** — every card recomposes, even though:
   - `status == 2` (completed) uses `event.endTime`, NOT `now`
   - `status == 0` (not_started) displays no time at all
   - Only `status == 1` (in-progress) uses `now` for the elapsed badge (line 507)
3. `DayProgressBar(now = now)` — legitimately needs `now` (current hour marker)
4. Top bar progress fill + "今天还有 Xh Ym" text — legitimately needs `now`

The click lambdas (`onClick = { onEventClick(item.event) }`, etc.) are **new instances every
recomposition**, so `EventCard` can never skip — even if `now` were removed, the unstable
lambdas would still force recomposition. But the root cause is the top-level `now` read:
if `TimelineScreen` doesn't recompose, the lambdas stay stable and `EventList`/`EventCard`
skip entirely.

**Impact: L** — Main screen of the app. For a day with 20 events (19 completed, 1
in-progress), 20 EventCards recompose every 60s. After fix: only 1 `ElapsedBadge`
recomposes. 95% reduction. For all-completed days: 100% reduction (zero recompositions
per minute).

## Evidence
- `TimelineScreen.kt:122` — `val now by produceState(...)` reads State at top level
- `TimelineScreen.kt:152` — `todayProgressFraction(date, now)` reads `now` in composition
- `TimelineScreen.kt:249` — `TimeVizCalculator.todayRemaining(now)` reads `now` in composition
- `TimelineScreen.kt:275` — `DayProgressBar(... now = now)` passes `now` down
- `TimelineScreen.kt:290` — `EventList(... now = now)` passes `now` to all cards
- `TimelineScreen.kt:388` — `EventList` signature: `now: Long`
- `TimelineScreen.kt:433` — `EventCard` signature: `now: Long`
- `TimelineScreen.kt:507` — `now` ONLY used for `status == 1` elapsed badge
- `TimelineScreen.kt:121` — `val nowHour = ...` dead variable (computed but never read)
- `DayProgressBar.kt:27` — `now: Long` parameter

## Fix Strategy
**Defer `now` State reads to leaf composables.** Don't read `nowState.value` at the
`TimelineScreen` level — pass the `State<Long>` object (stable, same instance) down and
read `.value` only where the tick actually matters:

1. **Top bar progress fill** — move `todayProgressFraction` inside `drawBehind` block
   (draw-phase state read → redraw only, no recomposition)
2. **"今天还有" stats text** — extract to `RowScope.StatsText` composable that reads
   `nowState.value` internally
3. **`DayProgressBar`** — change `now: Long` → `nowState: State<Long>`; read inside
4. **`EventCard` in-progress elapsed badge** — extract to `ElapsedBadge(startTime, nowState)`
   composable; remove `now` from `EventCard` signature

After fix, the 60s tick only recomposes:
- `StatsText` (small text composable)
- `DayProgressBar` (24 small Boxes)
- `ElapsedBadge` for in-progress events only (typically 0 or 1)

All completed/not-started `EventCard`s, the background `Image`, `BottomEntryBar`,
`EntryDrawer`, and the top bar `Row` structure skip recomposition entirely.

## In-scope files (2)
1. `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
2. `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt`

## Steps (8)

### Step 1 — Remove dead `nowHour` variable
`TimelineScreen.kt:121`: Remove `val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)`.
It's computed on every recomposition but never read anywhere in the file. Also removes the
unused `Calendar`/`TimeZone` import if no longer referenced (check: `Calendar` is still used
by `formatDateCompact`, `isToday`, `dateToUtcMillis` — keep import).

### Step 2 — Change `now` to `nowState` (don't read at top level)
`TimelineScreen.kt:122-127`:
```kotlin
// Before:
val now by produceState(initialValue = System.currentTimeMillis()) {
    while (true) { delay(60_000L); value = System.currentTimeMillis() }
}
// After:
val nowState = produceState(initialValue = System.currentTimeMillis()) {
    while (true) { delay(60_000L); value = System.currentTimeMillis() }
}
```
Add import: `import androidx.compose.runtime.State`.

### Step 3 — Move progress fraction into `drawBehind` (draw-phase deferred read)
`TimelineScreen.kt:152-164`: Remove `val todayFraction = todayProgressFraction(date, now)`
from composition body. Move it inside the `drawBehind` lambda:
```kotlin
.drawBehind {
    val now = nowState.value
    val fraction = todayProgressFraction(date, now)
    if (fraction > 0f) {
        drawRect(color = PrimaryLight, size = Size(size.width * fraction, size.height))
    }
}
```
This reads `nowState.value` in the **draw phase** — when `now` changes, only the draw
re-executes (cheap), no recomposition of the Row or its children.

### Step 4 — Extract `RowScope.StatsText` composable
`TimelineScreen.kt:243-265`: Replace the inline `val hasRecords / baseStats / statsText`
block + `Text(...)` with a call to a new composable:
```kotlin
@Composable
private fun RowScope.StatsText(
    date: Triple<Int, Int, Int>,
    events: List<EventEntity>,
    notes: List<NoteEntity>,
    nowState: State<Long>,
    onClick: () -> Unit
) {
    val now = nowState.value  // read here → only StatsText recomposes on tick
    val hasRecords = events.isNotEmpty() || notes.isNotEmpty()
    val baseStats = if (hasRecords) "${events.size} 件事 · ${notes.size} 条随笔" else "还没有记录"
    val statsText = if (isToday(date)) {
        if (hasRecords) "今天还有 ${TimeVizCalculator.todayRemaining(now)} · $baseStats"
        else "今天还有 ${TimeVizCalculator.todayRemaining(now)}"
    } else baseStats
    Text(
        text = statsText,
        fontSize = 12.sp, color = TextTertiary, fontWeight = FontWeight.Medium,
        maxLines = 1, overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f).clickable { onClick() }.padding(end = 12.dp, start = 8.dp)
    )
}
```
Call site: `StatsText(date = date, events = events, notes = notes, nowState = nowState, onClick = onTimeVizClick)`.
Add import: `import androidx.compose.foundation.layout.RowScope`.

### Step 5 — Update `DayProgressBar` call site
`TimelineScreen.kt:272-276`: Change `now = now` → `nowState = nowState`.

### Step 6 — Remove `now` from `EventList`/`EventCard`; add `ElapsedBadge`
- `EventList` signature: remove `now: Long`, add `nowState: State<Long>`
- `EventCard` signature: remove `now: Long`, add `nowState: State<Long>`
- In `EventCard` status `1` branch (line 507-519): replace inline elapsed badge with:
  ```kotlin
  ElapsedBadge(startTime = event.startTime, nowState = nowState)
  ```
- New composable:
  ```kotlin
  @Composable
  private fun ElapsedBadge(startTime: Long, nowState: State<Long>) {
      val now = nowState.value  // read here → only ElapsedBadge recomposes on tick
      Box(
          modifier = Modifier.background(Primary, RoundedCornerShape(0.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
      ) {
          Text(
              text = formatDurationShort(startTime, now),
              fontSize = 12.sp, color = TextOnPrimary, fontWeight = FontWeight.Bold
          )
      }
  }
  ```
- `EventList` call to `EventCard`: remove `now = now`, add `nowState = nowState`.

### Step 7 — Update `DayProgressBar.kt`
`DayProgressBar.kt:24-32`:
- Change parameter `now: Long` → `nowState: State<Long>`
- Change `val nowCal = remember(now) { Calendar.getInstance(...).apply { timeInMillis = now } }`
  → `val nowCal = remember(nowState.value) { Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = nowState.value } }`
- Add import: `import androidx.compose.runtime.State`
- `DayProgressBar` legitimately needs to recompose on tick (current-hour marker), so reading
  `nowState.value` in the composable body is correct — but its PARENT (`TimelineScreen`)
  won't recompose, so only `DayProgressBar` itself recomposes.

### Step 8 — Build verification
```bash
./gradlew :app:compileDebugKotlin
```

## Secondary benefits
- **`formatTime` mitigation**: `formatTime` creates a new `SimpleDateFormat` on every call
  (expensive). After this fix, `EventCard` only recomposes when events actually change, so
  `formatTime` is called ~0 times/min (was ~40/min for a 20-event day). A future cycle could
  cache the `SimpleDateFormat` in a thread-local or `remember`.
- **`DayProgressBar.hourOfDay` mitigation**: `hourOfDay()` allocates a `Calendar` per event.
  After this fix, `DayProgressBar` only recomposes on tick (was: on every TimelineScreen
  recomposition).

## Risk
- **Low**: Pure refactor of where State is read — no logic changes. `EventCard`/`EventList`
  signatures change but all callers are in the same file. `DayProgressBar` signature changes
  but it's only called from `TimelineScreen.kt`.
- No DB/query changes, no data flow changes.
- Compose `State<T>` is treated as stable by the Compose compiler, so passing it as a
  parameter does not trigger recomposition.
