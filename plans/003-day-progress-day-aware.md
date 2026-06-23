# Plan 003: DayProgressBar day-aware rendering

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat e850768..HEAD -- app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug (correctness)
- **Planned at**: commit `e850768`, 2026-06-23

## Why this matters

The left-side time bar is the timeline's at-a-glance "how is this day shaped"
indicator. Today it renders incorrectly for any day other than today: it uses
the *current* hour to decide past/future/now for whatever day the user is
viewing. So viewing yesterday at 10am shows yesterday's morning block as
"future/米白" with a red "now" border — when in fact the entire day is past and
should be blue (if unrecorded). Viewing a future day marks it as "past/blue".
This directly violates the design spec ("蓝色 = 已过去未记录…米白 = 还未到…当前小时有红色边框") and acceptance test TC-17, and undermines the
"App 反映现实，不评判现实" philosophy by misrepresenting past/future. The fix
makes the bar day-aware: past days are fully "past", future days fully
"future", and only today gets the red "now" border.

## Current state

- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` —
  computes `nowHour` once at composition (line 72) and passes it to
  `DayProgressBar` without telling it which day is being viewed:

```kotlin
// TimelineScreen.kt (lines 69-72)
val events by viewModel.events.collectAsStateWithLifecycle()
val notes by viewModel.notes.collectAsStateWithLifecycle()
val date by viewModel.viewingDate.collectAsStateWithLifecycle()
val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)
```

```kotlin
// TimelineScreen.kt (lines 142-146) — call site
DayProgressBar(
    events = events,
    nowHour = nowHour,
    modifier = Modifier.padding(top = 4.dp)
)
```

- `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` — uses
  `nowHour` to decide `isPast`/`isNow` with no awareness of the viewed date:

```kotlin
// DayProgressBar.kt (lines 23-46)
@Composable
fun DayProgressBar(
    events: List<EventEntity>,
    nowHour: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // 0-12h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) < 12 },
            isPast = nowHour >= 12,
            isNow = nowHour in 0..11,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(8.dp))
        // 12-24h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) >= 12 },
            isPast = nowHour >= 12,
            isNow = nowHour >= 12,
            modifier = Modifier.size(56.dp)
        )
    }
}

@Composable
private fun TimeBlock(
    hasRecord: Boolean, isPast: Boolean, isNow: Boolean,
    modifier: Modifier = Modifier
) {
    val color = when {
        hasRecord -> TimeBlockRecorded
        isPast -> TimeBlockPast
        else -> TimeBlockFuture
    }
    Box(
        modifier = modifier
            .background(color)
            .then(if (isNow) Modifier.border(3.dp, TimeBlockNowBorder) else Modifier)
    )
}

private fun hourOfDay(timestamp: Long): Int {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    cal.timeInMillis = timestamp
    return cal.get(Calendar.HOUR_OF_DAY)
}
```

### Design spec to honor (from `docs/2026-06-22-shijiben-design.md` §四)

> 蓝色 = 已过去未记录，绿色 = 有事件记录，米白 = 还未到
> 当前小时有红色边框标记

So: `hasRecord` (green) takes priority; then `isPast` (blue); else future
(米白). The red border marks the *current* hour — which only makes sense for
*today*. For a past day the whole day is "已过去"; for a future day the whole
day is "还未到".

### Repo conventions to honor

- Compose `@Composable` functions; `java.util.Calendar`/`TimeZone.getDefault()`
  for time (matches the rest of the file).
- Color tokens come from `com.shijiben.ui.theme` (`TimeBlockPast`,
  `TimeBlockRecorded`, `TimeBlockFuture`, `TimeBlockNowBorder`) — do NOT
  introduce hardcoded colors.
- The 2-block granularity (0-12h, 12-24h) is a deliberate design
  simplification (spec §四). Do NOT increase the number of blocks in this plan.
- The "today" rendering logic (which block gets the red border, the existing
  `isPast` values for today) is by-design and must NOT change — only the
  non-today behavior is fixed.

## Commands you will need

| Purpose    | Command                          | Expected on success |
|------------|----------------------------------|---------------------|
| Build      | `./gradlew assembleDebug`        | exit 0, BUILD SUCCESSFUL |
| Typecheck  | `./gradlew compileDebugKotlin`   | exit 0, no errors |
| Tests      | `./gradlew testDebugUnitTest`    | exit 0, all pass (unchanged) |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt`
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`

**Out of scope** (do NOT touch):
- `EventList.kt`, `TimelineViewModel.kt`, `RecordingSheet.kt`, etc.
- The `TimeBlock` composable's color/border logic — only the *inputs* computed
  in `DayProgressBar` change.
- Do NOT add a live-ticking clock (e.g. a `LaunchedEffect` that recomputes
  `nowHour` every minute). That is a separate enhancement; see Maintenance
  notes. `nowHour` staying fixed during a single composition is acceptable for
  V1 — the bug is the wrong-day rendering, not the stale-within-today rendering.
- Do NOT change the block count or sizes.

## Git workflow

- Branch: `advisor/003-day-progress-day-aware`
- Commit style: `fix(timeline): DayProgressBar renders past/future days correctly`
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add day-relation helpers to DayProgressBar.kt

In `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt`, add
three private helper functions at the bottom of the file (after the existing
`hourOfDay` function). These classify the viewed date relative to today using
tuple comparison (year, month, day) so there is no time-of-day ambiguity.

```kotlin
/** Returns true if the viewed date is today. */
private fun isToday(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    return date == Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
}

/**
 * Returns true if the viewed date is strictly before today (a past day).
 * Future days and today both return false.
 */
private fun isPastDay(date: Triple<Int, Int, Int>): Boolean {
    val cal = Calendar.getInstance(TimeZone.getDefault())
    val today = Triple(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH)
    )
    // Lexicographic compare on (year, month, day).
    val (ty, tm, td) = today
    val (y, m, d) = date
    return y < ty || (y == ty && (m < tm || (m == tm && d < td)))
}
```

**Verify**: `./gradlew compileDebugKotlin` → exit 0 (helpers compile; not yet
called, so no behavior change).

### Step 2: Make DayProgressBar accept the viewed date and compute day-aware flags

Change the `DayProgressBar` signature to accept `viewingDate:
Triple<Int, Int, Int>`, and recompute `isPast`/`isNow` per block so that:

- **Today**: unchanged from current behavior (block 1 isPast = `nowHour >= 12`,
  isNow = `nowHour in 0..11`; block 2 isPast = `nowHour >= 12`, isNow =
  `nowHour >= 12`).
- **Past day**: both blocks `isPast = true`, `isNow = false` (no red border).
- **Future day**: both blocks `isPast = false`, `isNow = false` (米白, no
  border).

Replace the existing `DayProgressBar` function body with:

```kotlin
@Composable
fun DayProgressBar(
    events: List<EventEntity>,
    viewingDate: Triple<Int, Int, Int>,
    nowHour: Int,
    modifier: Modifier = Modifier
) {
    val today = isToday(viewingDate)
    val past = isPastDay(viewingDate)
    Column(modifier = modifier) {
        // 0-12h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) < 12 },
            isPast = if (today) nowHour >= 12 else past,
            isNow = today && nowHour in 0..11,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(8.dp))
        // 12-24h 色块
        TimeBlock(
            hasRecord = events.any { hourOfDay(it.startTime) >= 12 },
            isPast = if (today) nowHour >= 12 else past,
            isNow = today && nowHour >= 12,
            modifier = Modifier.size(56.dp)
        )
    }
}
```

Leave `TimeBlock` and `hourOfDay` unchanged.

**Verify**: `./gradlew compileDebugKotlin` → exit 0. (The call site in
TimelineScreen does not yet pass `viewingDate`, so expect a COMPILE ERROR here
— that is expected and fixed in Step 3. If the error is anything other than
"parameter 'viewingDate' missing" / "no value passed for viewingDate", STOP.)

### Step 3: Update the call site in TimelineScreen.kt

In `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`, update
the `DayProgressBar(...)` call (around lines 142-146) to pass the viewed date:

```kotlin
DayProgressBar(
    events = events,
    viewingDate = date,
    nowHour = nowHour,
    modifier = Modifier.padding(top = 4.dp)
)
```

`date` is already collected at line 71 (`val date by viewModel.viewingDate.collectAsStateWithLifecycle()`).
Do not change how `nowHour` is computed.

**Verify**: `./gradlew compileDebugKotlin` → exit 0, no errors.
Then `./gradlew assembleDebug` → exit 0, BUILD SUCCESSFUL.

### Step 4: Confirm tests still pass

No tests exist for `DayProgressBar` (it is UI), but the repository tests must
remain green to confirm nothing else broke.

**Verify**: `./gradlew testDebugUnitTest` → exit 0, all existing tests pass.

## Test plan

`DayProgressBar` is a pure-UI composable with no existing test coverage and no
test infrastructure for Compose UI in this repo (no `createComposeRule`
dependency). Adding Compose UI test infrastructure is out of scope for this
plan. Verification is by build + manual acceptance per TC-17/TC-20:

- View **today**: morning block shows red border if before noon; afternoon
  block shows red border if noon or later. (Unchanged from before.)
- View **yesterday** (use the ‹ arrow): both blocks are blue if unrecorded /
  green if recorded; NO red border on either.
- View **tomorrow** (use the › arrow): both blocks are 米白 if unrecorded;
  NO red border.

If the executor has a running emulator/device, optionally confirm visually;
otherwise the build + typecheck gates are the machine-checkable criteria.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `grep -n "viewingDate: Triple<Int, Int, Int>" app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` returns one match
- [ ] `grep -n "viewingDate = date" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns one match
- [ ] `grep -n "fun isToday\|fun isPastDay" app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` returns two matches
- [ ] `./gradlew compileDebugKotlin` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `./gradlew testDebugUnitTest` exits 0 (all existing tests pass)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 003 updated to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `DayProgressBar.kt:23-65` or `TimelineScreen.kt:69-72,142-146`
  doesn't match the excerpts above (the codebase has drifted).
- After Step 2, the compile error is NOT the expected "missing argument for
  `viewingDate`" — a different error means the signature change went wrong.
- The `TimeBlock` composable or `hourOfDay` helper was accidentally modified
  (they must stay byte-for-byte identical).
- A step's verification fails twice after a reasonable fix attempt.

## Maintenance notes

- **Live clock (deferred)**: `nowHour` is captured once per composition and
  does not tick while the app stays open across an hour boundary. A future
  enhancement could expose the current time as an observed `State<Long>` (e.g.
  via `produceState` + a delayed loop) and recompute `nowHour` from it. Not in
  scope here; the wrong-day bug was the high-impact issue.
- **2-block granularity (by design)**: when viewing today's afternoon block at
  e.g. 15:00, the whole 12-24h block is treated as "past" (blue if unrecorded)
  even though 15-24h hasn't arrived. This is the documented 2-block
  simplification (spec §四), not a bug — do not "fix" it without a design
  decision to add finer granularity.
- **Reviewer focus**: confirm the `today` branch reproduces the exact previous
  `isPast`/`isNow` values (so today's rendering is unchanged), and that past
  days get `isNow = false` on both blocks.
