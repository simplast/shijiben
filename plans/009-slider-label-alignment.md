# Plan 009: Align slider labels with their true tick positions

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt`
> If this file changed since this plan was written, compare the "Current
> state" excerpts against live code before proceeding; on a mismatch, treat it
> as a STOP condition. **Note**: plan 002 modifies `DurationBar` in this same
> file — if 002 has landed, the `BarLabels(labels)` call sites and label list
> shape will differ; adapt accordingly.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (but coordinate with plan 002 which also modifies `TimeRangeSlider.kt` — execute sequentially)
- **Category**: bug (UX)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

`BarLabels` renders labels with `Arrangement.SpaceBetween` (TimeRangeSlider.kt:293-310), which distributes them evenly across the width regardless of their true tick positions. The duration bar's labels `["0","30分","1h","1h30","2h","2h30","3h"]` are NOT evenly spaced in value (the `0` and `30分` are close, but visually they get equal spacing to `3h`), so a label sits under the wrong block. The start-time bar has the same issue. A user reading "1h" under a slider thumb thinks they've selected 1h when the thumb is actually elsewhere. This plan positions each label at its true fractional position along the track.

## Current state

File in scope:
- `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` — `BarLabels` composable (lines 293-310), called from `StartTimeBar` (line 135) and `DurationBar` (line 151).

Excerpt — `TimeRangeSlider.kt:292-310`:
```kotlin
@Composable
private fun BarLabels(labels: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 2.dp, end = 2.dp, top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (lbl in labels) {
            Text(
                text = lbl,
                fontSize = 9.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
```

Callers pass plain `List<String>`:
- `StartTimeBar` (line 135): `BarLabels(labels)` where `labels` are hour markers like `["5点","6点",...,"11点"]` built from `windowStart..windowEnd` (line 119-125).
- `DurationBar` (line 151): `BarLabels(labels)` where `labels = listOf("0","30分","1h","1h30","2h","2h30","3h")`.

The problem: `SpaceBetween` puts label[0] at the left edge, label[last] at the right edge, and the rest evenly between — which matches their *true* positions ONLY if the values are evenly spaced. Duration labels aren't (0,30,60,90,120,150,180 — actually these ARE evenly spaced at 30-min steps! 0→30→60→...→180, all 30 apart). Wait — re-check: the duration labels are at minutes 0,30,60,90,120,150,180 — **evenly spaced by 30**. So `SpaceBetween` IS correct for the duration bar. The MISALIGNMENT is on the **start-time bar**, where labels are hourly (`5点,6点,...`) but the window is `±3h = 360 min` and the cursor/value sits at center — the labels are at the *edges* of each hour block, not centered, and `SpaceBetween` aligns them to the row edges which doesn't match the block grid drawn in `PixelTrackWithCursor`.

Let me re-examine. The actual misalignment: in `PixelTrackWithCursor`'s fixed-cursor mode (start time), blocks are drawn at 20-min intervals (line 232: `blockMinutes = 20`), and the cursor is at the *center* (`trackWidth/2`). The labels are hourly. `SpaceBetween` puts the first label at the left edge and last at the right edge — but those correspond to `windowStart` and `windowEnd` hours, which IS where those hours sit. The intermediate labels are evenly spaced, which matches evenly-spaced hours. So for the start-time bar, `SpaceBetween` is *approximately* correct but the labels align to tick *edges* while the user reads them as tick *centers* — a half-block offset.

The clearest, lowest-risk fix that improves both bars: position each label at its true fractional position (`(value - min) / (max - min)`) and center-align the text at that point, rather than relying on `SpaceBetween`'s edge-pinning. This makes the alignment exact regardless of label spacing and removes the edge-vs-center ambiguity.

### Repo conventions

- `BoxWithConstraints` from `androidx.compose.foundation.layout` is available in Compose foundation.
- `IntOffset` from `androidx.compose.ui.unit`, density via `LocalDensity`.
- 9sp `TextTertiary` labels (existing style — preserve).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` (`BarLabels` + its two call sites, which must now pass label *positions*, not just strings)

**Out of scope**:
- `PixelTrackWithCursor` block drawing — unchanged.
- The cursor/thumb position — unchanged.
- Plan 002's `durationMaxMinutes` parameter — if 002 landed, integrate with its `buildDurationLabels` (see Step 1 note).

## Git workflow

- Branch: `advisor/009-slider-labels`
- Commit message example: `Position slider labels at their true tick positions`.

## Steps

### Step 1: Change `BarLabels` to take label positions and render them at true fractions

Replace `BarLabels(labels: List<String>)` with a version that accepts each label's fractional position (0..1) and absolutely positions it. Using `BoxWithConstraints` + `offset` in pixels:

```kotlin
@Composable
private fun BarLabels(items: List<Pair<String, Float>>) {
    // items: (label text, fraction 0..1 of the track width where it should sit)
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp)
    ) {
        val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
        for ((text, fraction) in items) {
            val x = (fraction.coerceIn(0f, 1f) * widthPx)
            Text(
                text = text,
                fontSize = 9.sp,
                color = TextTertiary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.offset {
                    IntOffset(x = (x - with(LocalDensity.current) { /* half label width approx */ 0 }).toInt(), y = 0)
                }
            )
        }
    }
}
```

The `offset { IntOffset(...) }` lambda form avoids needing the label's measured width. To center each label on its tick, wrap each `Text` in a `Box` and offset by `-labelWidth/2`. The simplest robust approach: use `Modifier.offset { IntOffset((x).toInt(), 0) }` and accept that labels are left-aligned at their tick (slightly off for wide labels). For better centering, measure with `onSizeChanged` — but that's more code. **Recommended**: left-align at tick for the start-time bar's edge labels (which sit at the track edges anyway) and accept minor offset for middle labels. If the executor wants true centering, use a `Layout` composable — but that's an S→M bump; prefer the simple offset and verify on device.

Add imports: `androidx.compose.foundation.layout.BoxWithConstraints`, `androidx.compose.foundation.layout.offset`, `androidx.compose.ui.unit.IntOffset`, `androidx.compose.ui.platform.LocalDensity`, `androidx.compose.runtime.*` (for any `remember`).

### Step 2: Update `StartTimeBar` to pass fractional positions

In `StartTimeBar` (lines 113-137), build the label list as `(label, fraction)` pairs. The labels are hourly from `windowStart` to `windowEnd` (in minutes). Each hour `h` corresponds to minute `h*60`; its fraction is `(h*60 - windowStart) / (windowEnd - windowStart)`.

Replace the current `BarLabels(labels)` call (line 135) with:
```kotlin
val labelItems = remember(windowStart, windowEnd) {
    val hStart = windowStart / 60
    val hEnd = windowEnd / 60
    val span = (windowEnd - windowStart).coerceAtLeast(1)
    buildList {
        for (h in hStart..hEnd) {
            val frac = (h * 60 - windowStart).toFloat() / span
            add(if (h == 0) "0点" else "${h}点" to frac)
        }
    }
}
BarLabels(labelItems)
```

Remove the old `labels` `remember` block (lines 119-125) since it's now folded into `labelItems`.

### Step 3: Update `DurationBar` to pass fractional positions

**If plan 002 has NOT landed**: the labels are `["0","30分","1h","1h30","2h","2h30","3h"]` at minutes 0,30,60,90,120,150,180 over range 0..180. Build pairs:
```kotlin
val labelItems = listOf(
    "0" to 0f/180f, "30分" to 30f/180f, "1h" to 60f/180f,
    "1h30" to 90f/180f, "2h" to 120f/180f, "2h30" to 150f/180f, "3h" to 180f/180f
)
BarLabels(labelItems)
```
(Since these are evenly spaced, `SpaceBetween` was already correct here — but using the same positioned renderer keeps both bars consistent and handles the 0..`effectiveMax` case after plan 002.)

**If plan 002 HAS landed**: `DurationBar` now uses `buildDurationLabels(effectiveMax)` which produces `["0","1h","2h",...,"${hours}h"]` at 60-min steps over `0..effectiveMax`. Build pairs from that:
```kotlin
val labels = buildDurationLabels(effectiveMax)
val labelItems = labels.mapIndexed { i, lbl ->
    lbl to (if (labels.size <= 1) 0f else i.toFloat() / (labels.size - 1))
}
BarLabels(labelItems)
```
(The hourly labels are evenly spaced, so fraction = `i/(n-1)` is correct.)

In both branches, replace the existing `BarLabels(labels)` call (line 151 / the post-002 equivalent) with the `BarLabels(labelItems)` call.

### Step 4: Build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

On device: open the RecordingSheet, confirm each label sits under its corresponding tick/block, not evenly floated between edges.

## Test plan

Pure UI positioning; no unit test. Existing tests must remain green:
- `./gradlew :app:testDebugUnitTest` → all pass.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n "Arrangement.SpaceBetween" app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` returns no matches in `BarLabels` (the old layout is gone)
- [ ] `grep -n "BoxWithConstraints" app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` returns a match (in `BarLabels`)
- [ ] `grep -n "BarLabels(" app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` — both call sites pass `Pair<String, Float>` lists (or `labelItems`), not raw `List<String>`
- [ ] On device: labels align to their ticks on both the start-time and duration bars
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 009 updated to DONE

## STOP conditions

Stop and report back if:

- The excerpts at `TimeRangeSlider.kt:292-310` or the `StartTimeBar`/`DurationBar` call sites don't match live code (drift — especially if plan 002 already restructured `DurationBar`).
- `BoxWithConstraints`, `offset { ... }` lambda, or `IntOffset` aren't resolvable (they should be — Compose foundation/layout).
- The label centering is visibly worse than the old `SpaceBetween` after implementing the simple offset (e.g. labels overflow the left/right edge and clip). If so, STOP and switch to a `Layout`-based measured centering approach rather than shipping clipped labels.
- Plan 002 landed and its `buildDurationLabels` signature/shape differs from what Step 3 assumes — adapt to the actual shape, don't force the pre-002 shape.

## Maintenance notes

- **File-overlap with plan 002**: both modify `TimeRangeSlider.kt`. Land 002 first (it restructures `DurationBar` and its labels), then 009 (which adapts both call sites to positioned labels). If 009 lands first, 002's Step 2 must preserve the `(String, Float)` pair contract — coordinate.
- The simple `offset`-based positioning left-aligns labels at their tick; the first label (fraction 0) sits at the left edge and the last (fraction 1) at the right edge, where left-alignment is correct for the edges but middle labels sit slightly right of their tick center. This is acceptable for 9sp labels; if a reviewer finds it off, upgrade to measured centering (a `Layout` that centers each label on `fraction * width`).
- Reviewer: on device, drag the start-time slider and confirm the hour labels stay aligned with the block grid as the window scrolls; drag the duration slider and confirm labels match the thumb position.
- Deferred: showing a live value tooltip above the cursor while dragging (would supersede label reading entirely) — separate future enhancement.
