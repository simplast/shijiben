# Plan 005: Show start time and elapsed duration for in-progress events

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
> If this file changed since the plan was written, compare the "Current state"
> excerpt against live code; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (but coordinate with plans 001/003/004 which also touch `TimelineScreen.kt` — see Maintenance notes)
- **Category**: bug (UX)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

In `EventCard`, the entire time-range + duration block is gated on `if (event.endTime != null)` (TimelineScreen.kt:319). An in-progress event has `endTime == null` (set by `markInProgress`, `TimelineViewModel.kt:101`), so the card shows **only the title and a stop button** — no start time, no elapsed duration. A user who started an event 2 hours ago cannot see when they started or how long it's been running. For an app whose purpose is time awareness, hiding the running timer is a core gap. This plan shows the start time and a live elapsed badge for in-progress events.

## Current state

File in scope:
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — `EventCard` composable (lines 251-346), specifically the time block at 319-343.

Excerpt — `TimelineScreen.kt:316-344`:
```kotlin
Spacer(Modifier.width(8.dp))

// 时间范围 + 耗时 badge
if (event.endTime != null) {
    Text(
        text = "${formatTime(event.startTime)}-${formatTime(event.endTime)}",
        fontSize = 12.sp,
        color = when (event.status) {
            2 -> Secondary
            0 -> TextTertiary
            else -> TextSecondary
        }
    )
    Spacer(Modifier.width(6.dp))
    val durationText = formatDurationShort(event.startTime, event.endTime)
    Box(
        modifier = Modifier
            .background(Accent, RoundedCornerShape(0.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = durationText,
            fontSize = 10.sp,
            color = TextOnPrimary,
            fontWeight = FontWeight.Bold
        )
    }
}
```

Status constants: `0 = not_started`, `1 = in_progress`, `2 = completed` (from `EventEntity` + `EventStatus.kt`). The `when (event.status)` at line 307 already uses `2`/`0`/`else` (=1 in_progress).

Helpers at bottom of file:
- `formatTime(ts: Long)` at line 348 → `"HH:mm"`.
- `formatDurationShort(start, end)` at line 350 → `"Xmin"` or `"X.Xhours"`.

`TimelineScreen.kt:76` — `nowHour` is computed once; there is no live "now" state in the screen. For elapsed display we need a ticking `now`. The cleanest minimal approach: a `produceState`/`LaunchedEffect` that emits `System.currentTimeMillis()` every 60s while the screen is composed. (Plan 007 also addresses time freshness for the DayProgressBar; this plan adds a separate, focused elapsed-time source for EventCard. They can coexist; see Maintenance notes.)

### Repo conventions

- 8-bit aesthetic: `RoundedCornerShape(0.dp)`, pixel colors. The elapsed badge should reuse the `Accent`-background badge style already used for completed durations, but recolor to signal "running" — use `Primary` (red) for the running badge to draw attention, with `TextOnPrimary` text.
- Chinese inline strings.
- `produceState` is from `androidx.compose.runtime` (already imported via `androidx.compose.runtime.*` usage in the file).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` (`EventCard` time block + a `now` state hook in `TimelineScreen`)

**Out of scope**:
- `RecordingSheet`, `RecordingViewModel` — unrelated.
- Changing `EventEntity` or status semantics.
- Live-ticking for completed events (their duration is fixed; no ticking needed).

## Git workflow

- Branch: `advisor/005-in-progress-time`
- Commit message example: `Show start time and elapsed duration for in-progress events`.

## Steps

### Step 1: Add a ticking `now` state to `TimelineScreen`

Near the top of the `TimelineScreen` composable body (around line 76, where `nowHour` is computed), add a per-minute ticking "now" so elapsed durations stay fresh while the screen is open:

```kotlin
val now by produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        delay(60_000L)
        value = System.currentTimeMillis()
    }
}
```

Add imports: `androidx.compose.runtime.produceState` and `kotlinx.coroutines.delay`. (`delay` may need `import kotlinx.coroutines.delay`.)

This `now` will be passed down to `EventList` → `EventCard`. (It's also usable by plan 007's DayProgressBar, but that plan manages its own freshness; no coupling required.)

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 2: Thread `now` through `EventList` to `EventCard`

The inline `EventList` (line 222) and `EventCard` (line 251) signatures need a `now: Long` parameter. Add it as the last parameter to both:

- `EventList(..., now: Long)` — pass to each `EventCard(..., now = now)`.
- `EventCard(..., now: Long)` — used in Step 3.

Update the call site at `TimelineScreen.kt:152-160` to pass `now = now`.

### Step 3: Render start time + elapsed badge for in-progress events

In `EventCard`, replace the `if (event.endTime != null) { ... }` block (lines 319-343) with a branch that handles both in-progress and completed:

```kotlin
when (event.status) {
    1 -> {
        // 进行中：显示开始时间 + 运行中时长 badge
        Text(
            text = "自 ${formatTime(event.startTime)}",
            fontSize = 12.sp,
            color = TextSecondary
        )
        Spacer(Modifier.width(6.dp))
        val elapsed = formatDurationShort(event.startTime, now)
        Box(
            modifier = Modifier
                .background(Primary, RoundedCornerShape(0.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = elapsed,
                fontSize = 10.sp,
                color = TextOnPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
    2 -> {
        // 已完成：原时间范围 + 耗时 badge（保持不变）
        Text(
            text = "${formatTime(event.startTime)}-${formatTime(event.endTime!!)}",
            fontSize = 12.sp,
            color = Secondary
        )
        Spacer(Modifier.width(6.dp))
        Box(
            modifier = Modifier
                .background(Accent, RoundedCornerShape(0.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(
                text = formatDurationShort(event.startTime, event.endTime!!),
                fontSize = 10.sp,
                color = TextOnPrimary,
                fontWeight = FontWeight.Bold
            )
        }
    }
    // status 0 (not_started): no time block (unchanged — endTime is null, no duration)
}
```

Notes:
- `event.endTime!!` is safe inside the `2 ->` branch because completed events always have `endTime != null` (the status is computed from `endTime` per `RecordingViewModel.save` / `EventRepository.determineStatus`). If you want to be defensive, keep the outer `event.endTime != null` guard around the whole `when` — but the `when` on `status` already separates the cases correctly.
- The in-progress badge uses `Primary` (red) to signal "running, attention"; completed uses `Accent` (orange) as before.
- `formatDurationShort(start, now)` reuses the existing helper; it already handles `<60` min vs hours formatting.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 4: Full build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

## Test plan

Pure UI change; no new unit test. Existing tests must remain green:
- `./gradlew :app:testDebugUnitTest` → all pass (TimelineViewModelTest, RecordingViewModelTest, repository tests).

Optional: if `TimelineViewModelTest` has a `markInProgress` test, confirm it still passes (it tests the ViewModel, not the card — should be unaffected).

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n "produceState" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns a match
- [ ] `grep -n "自 " app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns a match (in-progress start-time label)
- [ ] An in-progress event card on device shows "自 HH:mm" + a red elapsed badge that updates each minute
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 005 updated to DONE

## STOP conditions

Stop and report back if:

- The excerpt at `TimelineScreen.kt:316-344` doesn't match live code (drift).
- `produceState` or `kotlinx.coroutines.delay` aren't resolvable (they should be — `kotlinx-coroutines-android` is a dependency per `app/build.gradle.kts:76`; `produceState` is in Compose runtime).
- A completed event is found with `endTime == null` in practice (would break the `endTime!!` in the `2 ->` branch). If discovered, STOP — the status invariant is broken at the data layer and must be fixed first.
- Plans 001/003/004 have already modified `TimelineScreen.kt` and the line numbers no longer match — re-locate the `EventCard` time block by content (`if (event.endTime != null)`) rather than line number before editing.

## Maintenance notes

- **File-overlap with plans 001, 003, 004, 007**: all touch `TimelineScreen.kt`. Execute sequentially, not in parallel, and re-run the drift check before each. The `EventCard` time block (this plan) is disjoint from the bottom input (003), the date bar (004), and the EventCard long-press wiring (001 — adds a parameter to `EventCard`'s signature; this plan also adds `now`, so merge both parameters in one pass if executing together).
- The per-minute `produceState` ticks only while `TimelineScreen` is composed; it stops when the user navigates to Tags/Notes (the composable leaves composition). This is acceptable — elapsed time resumes correct on return. If plan 007 also adds a ticking source, the two can share one `now` to avoid two coroutines; coordinate.
- Reviewer: on device, start an event, confirm the red badge appears and increments each minute; stop it and confirm it switches to the orange completed badge with fixed duration.
- Deferred: a real-time "stopwatch" seconds-precision tick is intentionally NOT added (minute precision matches the rest of the app and saves battery).
