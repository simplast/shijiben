# Plan 018: Memoize `coveredHours` in `DayProgressBar`

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 2a71f17..HEAD -- app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt`
> If this file changed since this plan was written, compare the "Current state" excerpt against the live code before proceeding. Compare the **content** of the 3-line window, not just line numbers. If surrounding lines shifted but the target code is unchanged, proceed. If the target code itself has changed, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: perf
- **Planned at**: commit `2a71f17`, 2026-06-29

## Why this matters

`DayProgressBar` recomputes `coveredHours` — a `Set<Int>` built by flat-mapping every event's hour range — on **every recomposition**. The home screen passes a `now` value that ticks every 60 seconds (`produceState { delay(60_000) }` in `TimelineScreen`), so the progress bar recomposes once a minute and rebuilds this set each time, even though `events` hasn't changed. For a day with many events this is a small but pointless per-minute allocation churn. Wrapping the computation in `remember(events)` makes it recompute only when the event list actually changes.

## Current state

The relevant file:

- `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` — the 24-hour pixel rail on the home screen.

The target code, as a 3-line window (content-labeled, line numbers are hints):

```
// DayProgressBar.kt, inside fun DayProgressBar(...) — after the nowCal/nowHour vals
    val nowCal = remember(now) { Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = now } }
    val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
    val coveredHours = events.flatMap { e ->
        val startHour = hourOfDay(e.startTime)
        val endHour = e.endTime?.let { hourOfDay(it) } ?: startHour
        (startHour..endHour).toList()
    }.toSet()
```

Note: `nowCal` is already `remember(now)`-gated — only the `coveredHours` block is un-memoized. `events` is a `List<EventEntity>` flowing from `TimelineViewModel`; its list identity changes only when the DB emits a new list.

## Repo conventions to match

- Compose state memoization: `remember(key) { ... }` is the standard pattern in this codebase. See `TimelineScreen.EventList` which uses `remember(events, notes) { ... }` to memoize the sorted item list — model the new code on that.
- `hourOfDay(...)` is a private helper in the same file that reads `Calendar.getInstance()`; it's deterministic given the timestamp, so memoizing on `events` is safe.

## Commands you will need

| Purpose   | Command                                          | Expected on success |
|-----------|--------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`              | exit 0, no errors   |
| Tests     | `./gradlew :app:testDebugUnitTest --rerun-tasks` | all pass            |
| Build     | `./gradlew assembleDebug`                        | exit 0, APK produced |

## Scope

**In scope** (the only file you should modify):
- `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` — only the `coveredHours` assignment.

**Out of scope** (do NOT touch):
- `TimelineScreen.kt` — the `now` produceState stays as-is.
- `hourOfDay`, `isToday`, `isPastDay` private helpers — unchanged.
- The render loop (`for (hour in 0..23)`) — unchanged.

## Git workflow

- Work directly on the current branch.
- Commit message style: `<NNN>: <short desc>`. Example: `018: memoize DayProgressBar.coveredHours on events`.

## Steps

### Step 1: Wrap `coveredHours` in `remember(events)`

In `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt`, replace the un-memoized assignment:

```kotlin
val coveredHours = events.flatMap { e ->
    val startHour = hourOfDay(e.startTime)
    val endHour = e.endTime?.let { hourOfDay(it) } ?: startHour
    (startHour..endHour).toList()
}.toSet()
```

with:

```kotlin
val coveredHours = remember(events) {
    events.flatMap { e ->
        val startHour = hourOfDay(e.startTime)
        val endHour = e.endTime?.let { hourOfDay(it) } ?: startHour
        (startHour..endHour).toList()
    }.toSet()
}
```

The `remember` import is already present (the file uses `remember(now)` on the line above), so no new imports are needed. Do not change the keys — `events` alone is correct because the computation depends only on the event list (time-zone is read fresh inside `hourOfDay`, but the set of covered hours doesn't drift within a stable `events` value across a minute).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0, no errors.

### Step 2: Run the full gate

**Verify**: `./gradlew :app:testDebugUnitTest --rerun-tasks` → all pass. Then `./gradlew assembleDebug` → exit 0.

## Test plan

- No new automated tests. `DayProgressBar` is a composable with no existing UI test. The change is a pure memoization wrapper around an already-correct computation — same output, fewer recomputations.
- Existing tests (`TimelineViewModelTest`, `EventRepositoryHeatmapTest`) cover the data layer feeding `events`; they don't break.
- Optional device check (reviewer): the home-screen 24h rail should look identical before/after (same covered hours colored, same past/future/now styling). The only observable difference is reduced CPU work per minute tick.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest --rerun-tasks` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] Grep for `remember(events)` in `DayProgressBar.kt` → one match, wrapping the `coveredHours` flatMap
- [ ] Grep for `val coveredHours = events.flatMap` (the un-memoized form) in `DayProgressBar.kt` → no matches
- [ ] No files outside `DayProgressBar.kt` are modified (`git status --short`)
- [ ] `plans/README.md` status row updated *(orchestrator-owned; developer subagents skip this)*

## STOP conditions

Stop and report back (do not improvise) if:

- The `coveredHours` block doesn't match the excerpt (drift).
- `events` is no longer a `List<EventEntity>` (e.g. it became a `Flow` or `StateFlow` — would mean upstream changed and `remember(events)` keys on the wrong thing).
- The `remember` import is missing and adding it would pull in a non-`androidx.compose.runtime.remember` symbol (STOP and report rather than guessing the import).

## Maintenance notes

- If a future change makes `hourOfDay` depend on a time-zone `State` that can change at runtime, the `remember(events)` key becomes insufficient — add the tz to the keys. Today tz changes trigger a process restart on Android, so this is not a live concern.
- Reviewer: confirm the rail still updates when an event is added/edited/deleted — that path emits a new `events` list from the ViewModel, which invalidates `remember(events)`. Good.
- This is the only un-memoized allocation in `DayProgressBar`'s body; the `nowCal`/`nowHour` pair is already gated on `now`, which is correct (it must update each minute).
