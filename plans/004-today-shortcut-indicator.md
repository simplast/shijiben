# Plan 004: Add "Today" shortcut and not-on-today indicator to the date bar

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against live code; on a mismatch, treat it as a
> STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug (UX gap)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

`TimelineViewModel.goToToday()` exists (line 61) but is **never called from any UI** — dead code. The date bar only has prev/next day arrows (TimelineScreen.kt:120,137), so navigating two weeks back requires 14 taps to return. Additionally, the date display (`formatDateCompact`, e.g. `6/15/一`) looks identical whether the user is on today or another day, giving no signal that they're viewing a past/future date. This plan makes the date text itself the "today" affordance: tapping it when not on today jumps back to today, and a visual marker (color + a small "今天" tag) indicates when the user is off today.

## Current state

Files in scope:
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — the top date bar (lines 111-141).
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt` — `goToToday()` already exists; no change needed there, but we verify it.

Excerpt — `TimelineScreen.kt:111-141` (the top bar Row):
```kotlin
Row(
    modifier = Modifier.fillMaxWidth().padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onTagsClick) {
            Icon(Icons.Default.Star, contentDescription = "标签", tint = TextSecondary)
        }
        IconButton(onClick = { viewModel.goToPreviousDay() }) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "前一天", tint = TextPrimary)
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatDateCompact(date),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = TextPrimary
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onNotesClick) {
            Icon(Icons.Default.Edit, contentDescription = "随笔", tint = TextSecondary)
        }
        IconButton(onClick = { viewModel.goToNextDay() }) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "后一天", tint = TextPrimary)
        }
    }
}
```

`TimelineViewModel.kt:61` — the unused function:
```kotlin
fun goToToday() { _viewingDate.value = today() }
```

`TimelineScreen.kt:76` — `nowHour` is computed from `Calendar.getInstance` but the date comparison helpers `isToday`/`isPastDay` already exist at `TimelineScreen.kt:376-383`:
```kotlin
private fun isToday(date: Triple<Int, Int, Int>): Boolean { ... }
private fun isPastDay(date: Triple<Int, Int, Int>): Boolean { ... }
```
These are currently unused in `TimelineScreen` (they're used in `DayProgressBar.kt` which has its own private copies) — reuse them here.

`formatDateCompact` at `TimelineScreen.kt:364-370` returns `"${month}/${day}/$weekday"`.

### Repo conventions

- Chinese inline strings. Pixel colors (`TextPrimary`, `TextSecondary`, `TextTertiary`, `Accent`, `Primary` from `ui/theme/AppColors.kt`).
- `clickable` from `androidx.compose.foundation` (already imported).
- Icons: `Icons.Default.*`.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` (top date bar only)

**Out of scope**:
- `TimelineViewModel.kt` — `goToToday()` already exists; do not modify.
- Adding a full date-picker dialog (calendar grid) — deferred to a future plan; this plan only adds the today-jump.
- `DayProgressBar.kt` — unrelated.

## Git workflow

- Branch: `advisor/004-today-shortcut`
- Commit message example: `Add tap-to-return-to-today on the date bar`.

## Steps

### Step 1: Make the date Column clickable when not on today, jumping to today

In `TimelineScreen.kt`, replace the inner `Column` holding the date Text (lines 124-131) with a version that:
1. Computes `val onToday = isToday(date)` (the helper at line 376 is file-private and accessible here).
2. When `!onToday`, makes the Column clickable to call `viewModel.goToToday()`, tints the date with `Accent` (orange) to signal "you're off today", and adds a tiny "今天" label below.

Target shape:
```kotlin
val onToday = isToday(date)
Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier.then(
        if (onToday) Modifier
        else Modifier.clickable { viewModel.goToToday() }
    )
) {
    Text(
        text = formatDateCompact(date),
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = if (onToday) TextPrimary else Accent
    )
    if (!onToday) {
        Text(
            text = "回今天",
            fontSize = 10.sp,
            color = TextTertiary,
            fontWeight = FontWeight.Medium
        )
    }
}
```

`clickable`, `Text`, `Column`, `Modifier`, `Accent`, `TextPrimary`, `TextTertiary`, `FontWeight` are all already imported/available in this file. `isToday` is file-private at line 376.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 2: Confirm `goToToday` is now referenced (no longer dead)

**Verify**: `grep -n "goToToday" app/src/main/java/com/shijiben/feature/timeline/` → matches in `TimelineViewModel.kt:61` (definition) AND `TimelineScreen.kt` (the new call site). Previously only the definition existed.

### Step 3: Full build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

## Test plan

Pure UI change; no new unit test. Existing `TimelineViewModelTest.kt` should already cover `goToToday` if it tests date navigation — confirm it does; if `goToToday` is untested, add one assertion mirroring the existing `goToPreviousDay`/`goToNextDay` test pattern in that file (call `goToToday()`, assert `viewingDate.value == today()`). This is optional but cheap.

Verification: `./gradlew :app:testDebugUnitTest --tests "*TimelineViewModelTest*"` → all pass.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -rn "goToToday" app/src/main/java/com/shijiben/feature/timeline/` returns at least 2 matches (definition + call site)
- [ ] `grep -n "回今天" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns one match
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 004 updated to DONE

## STOP conditions

Stop and report back if:

- The excerpt at `TimelineScreen.kt:111-141` doesn't match live code (drift).
- The file-private `isToday` helper at `TimelineScreen.kt:376` is gone or renamed (if so, use whatever date-equality helper exists, or inline the comparison).
- `Accent` color isn't importable from `ui.theme.*` (it is — `AppColors.kt:14`; if the wildcard import was narrowed, add `import com.shijiben.ui.theme.Accent`).

## Maintenance notes

- The "回今天" affordance is intentionally minimal (no extra button) to preserve the 8-bit sparse aesthetic. If a full date picker is added later, the date Column's click behavior should branch: tap = today-jump (current), long-press = open date picker (future).
- Reviewer: on device, navigate back a few days, confirm the date turns orange + shows "回今天"; tap it and confirm it returns to today (black, no sublabel).
- Deferred: jumping to an arbitrary date (date picker) is a separate future plan.
