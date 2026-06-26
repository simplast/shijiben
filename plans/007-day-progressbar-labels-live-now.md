# Plan 007: DayProgressBar hour labels + live "now" marker (merges findings #7 and #9)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against live code; on a mismatch, treat it as a
> STOP condition.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none (coordinate with plans 001/003/004/005 which also touch `TimelineScreen.kt`)
- **Category**: bug (UX) — merges audit findings #7 (no hour labels / now indicator) and #9 (nowHour doesn't update across hour boundary)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

`DayProgressBar` renders 24 small 16dp color blocks vertically with no hour labels and no readable "now" indicator (just a 2dp red border on the current-hour block — hard to spot on a 16dp square). Worse, `TimelineScreen.kt:76` computes `nowHour` exactly once at composition: if the app stays open across an hour boundary, the past/future coloring and the "current hour" marker all freeze and become wrong. For an app whose philosophy is *time awareness* (`MY_ORIGIN_GOAL.md`: "看见时间去向"), a frozen and illegible day bar undermines the core experience. This plan adds sparse hour labels (0/6/12/18), a clearer "now" marker, and a per-minute ticking source so the bar stays correct while the screen is open.

## Current state

Files in scope:
- `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` — the 24-block Column.
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — the `nowHour` computation (line 76) and the `DayProgressBar(...)` call site (lines 145-149).

Excerpt — `DayProgressBar.kt:22-58`:
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
    val coveredHours = events.flatMap { e ->
        val startHour = hourOfDay(e.startTime)
        val endHour = e.endTime?.let { hourOfDay(it) } ?: startHour
        (startHour..endHour).toList()
    }.toSet()

    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        for (hour in 0..23) {
            val hasRecord = hour in coveredHours
            val isPast = if (today) hour <= nowHour else past
            val isNow = today && hour == nowHour
            val baseColor = RainbowHourColors[hour % 8]
            val bgColor = when {
                hasRecord -> baseColor
                isPast -> baseColor.copy(alpha = 0.3f)
                else -> BorderLight
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(bgColor)
                    .then(if (isNow) Modifier.border(2.dp, TimeBlockNowBorder) else Modifier)
            )
        }
    }
}
```

Excerpt — `TimelineScreen.kt:76`:
```kotlin
val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)
```

Excerpt — `TimelineScreen.kt:143-150` (call site):
```kotlin
Row(modifier = Modifier.weight(1f).fillMaxWidth().fillMaxHeight()) {
    Box(modifier = Modifier.width(40.dp).fillMaxHeight().padding(top = 12.dp, start = 12.dp)) {
        DayProgressBar(
            events = events,
            viewingDate = date,
            nowHour = nowHour
        )
    }
    ...
```

Color tokens available (`ui/theme/AppColors.kt`): `TimeBlockNowBorder` (red, line 54), `TimeBlockNowBackground` (warm yellow, line 55), `TextTertiary` (light gray for labels), `RainbowHourColors` (8-color cycle, line 58).

`DayProgressBar.kt:60-85` has its own file-private `hourOfDay`/`isToday`/`isPastDay` helpers.

### Repo conventions

- 8-bit aesthetic: 16dp squares, no rounded corners, pixel colors. Labels in the small pixel font (already default via `AppTheme` typography).
- Chinese inline strings.
- `produceState` for ticking (also used by plan 005 for elapsed time — see Maintenance notes for sharing).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt`
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` (the `nowHour` line + the `DayProgressBar(...)` call site + the surrounding `Box` width if needed for labels)

**Out of scope**:
- `EventList`, `EventCard` — unrelated.
- The top date bar (plan 004).
- Changing `RainbowHourColors` palette.

## Git workflow

- Branch: `advisor/007-daybar-labels-live-now`
- Commit message example: `Add hour labels and live now marker to DayProgressBar`.

## Steps

### Step 1: Replace the frozen `nowHour: Int` parameter with a live `now: Long`

In `DayProgressBar.kt`, change the signature from `nowHour: Int` to `now: Long`:

```kotlin
@Composable
fun DayProgressBar(
    events: List<EventEntity>,
    viewingDate: Triple<Int, Int, Int>,
    now: Long,
    modifier: Modifier = Modifier
) {
    val nowCal = remember(now) { Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = now } }
    val nowHour = nowCal.get(Calendar.HOUR_OF_DAY)
    val nowMinute = nowCal.get(Calendar.MINUTE)
    ...
```

Derive `nowHour` from `now` inside the body (so the bar recomputes when `now` ticks). Add `import androidx.compose.runtime.remember` if not present.

Replace the `isNow = today && hour == nowHour` line with hour+minute awareness so the marker is meaningful within the hour:
```kotlin
val isNow = today && hour == nowHour
```
(Keep hour-level for the border; the "now" line in Step 3 will use minute precision.)

**Verify**: `./gradlew :app:compileDebugKotlin` → will fail at the call site (Step 2 fixes it).

### Step 2: Provide a ticking `now` in `TimelineScreen` and pass it to `DayProgressBar`

In `TimelineScreen.kt`, replace line 76:
```kotlin
val nowHour = Calendar.getInstance(TimeZone.getDefault()).get(Calendar.HOUR_OF_DAY)
```
with a per-minute ticking source:
```kotlin
val now by produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        delay(60_000L)
        value = System.currentTimeMillis()
    }
}
```

Add imports `androidx.compose.runtime.produceState` and `kotlinx.coroutines.delay`.

**Note on sharing with plan 005**: plan 005 also introduces a ticking `now` in `TimelineScreen` for elapsed-time display. If both plans land, define the `now` `produceState` **once** at the top of `TimelineScreen` and reuse it for both `DayProgressBar` and `EventCard`. Do not create two ticking coroutines. If 005 already landed, reuse its `now` and skip re-declaring it here.

Update the call site (`TimelineScreen.kt:145-149`):
```kotlin
DayProgressBar(
    events = events,
    viewingDate = date,
    now = now
)
```

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 3: Add sparse hour labels and a clearer "now" marker inside `DayProgressBar`

Replace the `Column { for (hour in 0..23) { Box(...) } }` block with a Row-per-hour layout: hour label (only at 0/6/12/18) + the color block, and a now-line. To keep the slim 40dp width, render labels only at sparse hours and keep blocks at 16dp.

Target shape:
```kotlin
Column(
    modifier = modifier.fillMaxHeight(),
    verticalArrangement = Arrangement.SpaceEvenly
) {
    for (hour in 0..23) {
        val hasRecord = hour in coveredHours
        val isPast = if (today) hour <= nowHour else past
        val isNow = today && hour == nowHour
        val baseColor = RainbowHourColors[hour % 8]
        val bgColor = when {
            hasRecord -> baseColor
            isPast -> baseColor.copy(alpha = 0.3f)
            else -> BorderLight
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 稀疏小时标签：仅 0/6/12/18 显示
            if (hour % 6 == 0) {
                Text(
                    text = "%02d".format(hour),
                    fontSize = 8.sp,
                    color = TextTertiary,
                    modifier = Modifier.width(16.dp),
                    maxLines = 1
                )
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(bgColor)
                    .then(if (isNow) Modifier.border(2.dp, TimeBlockNowBorder) else Modifier)
            )
        }
    }
}
```

Add imports: `androidx.compose.foundation.layout.Row`, `androidx.compose.foundation.layout.Spacer`, `androidx.compose.foundation.layout.width`, `androidx.compose.material3.Text`, `androidx.compose.ui.unit.sp` — check which are already imported (some are).

For the "now" marker clarity: the existing 2dp red border on the current-hour block is retained; combined with the sparse labels the user can now read which hour is "now". (A separate horizontal now-line across the column was considered but rejected — it complicates the `SpaceEvenly` layout and the border+label combination is sufficient.)

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 4: Widen the containing Box if labels overflow

The call site wraps `DayProgressBar` in `Box(modifier = Modifier.width(40.dp)...)` (TimelineScreen.kt:144). With a 16dp label + 16dp block = 32dp content + padding, 40dp should still fit. Verify visually on device. If the labels clip, widen to `48.dp`:

```kotlin
Box(modifier = Modifier.width(48.dp).fillMaxHeight().padding(top = 12.dp, start = 12.dp)) {
```

Only widen if needed; 40dp is the design-intent width.

**Verify**: `./gradlew assembleDebug` → exit 0; on device, confirm labels 00/06/12/18 are visible and not clipped.

### Step 5: Full build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

## Test plan

Pure UI change; no new unit test. Existing tests must remain green:
- `./gradlew :app:testDebugUnitTest` → all pass.

Optional: there is no `DayProgressBarTest` (it's a UI composable). If a Robolectric Compose test is desired, model after `app/src/test/java/com/shijiben/feature/timeline/TimelineViewModelTest.kt` — but that tests the ViewModel, not composables. Skip; rely on build + device verification.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n "nowHour: Int" app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` returns no matches (old signature gone)
- [ ] `grep -n "now: Long" app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` returns the new parameter
- [ ] `grep -n "produceState" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns a match (live now)
- [ ] `grep -n 'hour % 6 == 0' app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` returns a match (sparse labels)
- [ ] On device: labels 00/06/12/18 visible; current-hour block has red border; leaving the app open across an hour boundary updates the marker (verify by changing device time or waiting)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 007 updated to DONE

## STOP conditions

Stop and report back if:

- The excerpts at `DayProgressBar.kt:22-58` or `TimelineScreen.kt:76` / `143-150` don't match live code (drift — especially if plan 005 already changed the `now` story in `TimelineScreen`).
- `produceState` or `kotlinx.coroutines.delay` aren't resolvable (they should be).
- The 40dp containing Box cannot fit labels + block without clipping even at 8sp font, and widening to 48dp breaks the timeline's `EventList` weight layout — report rather than cascading layout changes.
- Plan 005 already landed a `now` `produceState` in `TimelineScreen` — reuse it instead of re-declaring (do not create two ticking sources).

## Maintenance notes

- **Sharing `now` with plan 005**: both plans need a ticking `now` in `TimelineScreen`. Land them so the `produceState` is declared once and consumed by both `DayProgressBar` and `EventCard`. If they land independently, a trivial follow-up deduplicates the two `produceState` blocks.
- **File-overlap with plans 001/003/004/005**: all touch `TimelineScreen.kt`. Execute sequentially and re-run drift checks. This plan's `now` declaration at line 76 and the `DayProgressBar` call at 145-149 are disjoint from the date bar (004), bottom input (003), EventCard (001/005) — but the `now` variable is shared with 005, so coordinate order.
- Reviewer: on device, change the system clock forward by 1 hour while the app is open and confirm the now-marker and past/future coloring move; confirm sparse labels are legible at the pixel font size.
- Deferred: making the hour blocks tappable (tap an hour → pre-fill a new event at that time) is the direction suggestion from the audit; it's a separate plan, not this one.
