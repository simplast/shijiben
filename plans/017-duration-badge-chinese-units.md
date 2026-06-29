# Plan 017: Use Chinese units in `formatDurationShort` (timeline duration badges)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 2a71f17..HEAD -- app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
> If this file changed since this plan was written, compare the "Current state" excerpt against the live code before proceeding. Compare the **content** of the 3-line window, not just line numbers. If surrounding lines shifted but the target function body is unchanged, proceed. If the function itself has changed, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (disjoint from plan 015's edits in the same file — see "Dependency notes")
- **Category**: bug (i18n / polish)
- **Planned at**: commit `2a71f17`, 2026-06-29

## Why this matters

The whole app is Chinese (zh-Hans), but the duration badges on `EventCard` render English: `"30min"` and `"%.1fhours"` — and the hours form has no space, producing e.g. `"1.5hours"`. It's an inconsistent, slightly broken-looking string in an otherwise fully Chinese UI. The slider's own duration formatter (`TimeRangeSlider.formatDuration`) already uses `"分钟"`/`"小时"`. This plan aligns the badge formatter with that convention and fixes the missing space.

## Current state

The relevant file:

- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — home screen; the function is private, near the bottom of the file.

The target function, as a 3-line window (content-labeled, line numbers are hints):

```
// TimelineScreen.kt, private fun formatDurationShort(start, end) — near bottom of file
private fun formatDurationShort(start: Long, end: Long): String {
    val minutes = (end - start) / 1000 / 60
    return if (minutes < 60) {
        "${minutes}min"                  // ← English "min"
    } else {
        "%.1fhours".format(minutes / 60.0)   // ← English "hours", no space → "1.5hours"
    }
}
```

Call sites (both inside `EventCard`, do not change these — only the formatter):

```
// TimelineScreen.kt, inside EventCard — status == 1 (in-progress) branch
                    val elapsed = formatDurationShort(event.startTime, now)
```
```
// TimelineScreen.kt, inside EventCard — status == 2 (completed) branch
                            text = formatDurationShort(event.startTime, event.endTime!!),
```

## Repo conventions to match

- All UI strings are Chinese (zh-Hans) inline literals. Reference: `TimeRangeSlider.formatDuration` uses `"${m}分钟"`, `"${h}小时"`, `"${h}小时${m}分钟"` (see `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt`, `private fun formatDuration`).
- The badge is a compact 10sp `Text` inside a colored `Box` — keep the output short. A decimal form (`"1.5小时"`) stays compact while matching the Chinese-unit convention; the verbose `"1小时30分钟"` form is fine for the slider's big label but unnecessarily long for a badge.

## Commands you will need

| Purpose   | Command                                          | Expected on success |
|-----------|--------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`              | exit 0, no errors   |
| Tests     | `./gradlew :app:testDebugUnitTest --rerun-tasks` | all pass            |
| Build     | `./gradlew assembleDebug`                        | exit 0, APK produced |

## Scope

**In scope** (the only file you should modify):
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — only the body of `formatDurationShort`.

**Out of scope** (do NOT touch):
- `TimeRangeSlider.formatDuration` — already Chinese; leave it.
- `formatTime(ts)` (the `HH:mm` formatter) — unchanged.
- The call sites in `EventCard` — unchanged.
- `strings.xml` — no migration.

## Git workflow

- Work directly on the current branch.
- Commit message style: `<NNN>: <short desc>`. Example: `017: Chinese units for timeline duration badges`.

## Steps

### Step 1: Rewrite `formatDurationShort` body

In `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`, replace the body of `private fun formatDurationShort(start: Long, end: Long): String` with:

```kotlin
private fun formatDurationShort(start: Long, end: Long): String {
    val minutes = (end - start) / 1000 / 60
    return if (minutes < 60) {
        "${minutes}分钟"
    } else {
        val hours = minutes / 60.0
        if (hours % 1.0 == 0.0) "${hours.toInt()}小时" else "%.1f小时".format(hours)
    }
}
```

Rationale for the three branches:
- `< 60` min → `"30分钟"` (was `"30min"`).
- whole hours → `"2小时"` (avoids the awkward `"2.0小时"`).
- fractional hours → `"1.5小时"` (was `"1.5hours"`, fixes missing space + English unit).

Do not change the function signature, the call sites, or any other function.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0, no errors.

### Step 2: Confirm no other English-unit strings leaked into the badges

Run a Grep (not shell grep) for `min"` and `hours` and `min` as a word in `TimelineScreen.kt` — the only acceptable English tokens are in imports/comments. The badges themselves should now contain only Chinese units.

**Verify**: `./gradlew :app:testDebugUnitTest --rerun-tasks` → all pass. Then `./gradlew assembleDebug` → exit 0.

## Test plan

- No new automated tests. `formatDurationShort` is `private` and the repo has no Compose UI tests; testing it would require making it `internal` (scope creep for a string change). The change is a pure string-format swap with no branching logic beyond what's listed.
- Optional device check (reviewer): create an event with a 30-min duration → badge shows `"30分钟"`; a 2h duration → `"2小时"`; a 1h30m duration → `"1.5小时"`. In-progress events show the same formatter against `now`.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest --rerun-tasks` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] Grep for `min"` in `TimelineScreen.kt` → no matches inside `formatDurationShort` (the literal `"${minutes}min"` is gone)
- [ ] Grep for `hours` in `TimelineScreen.kt` → no matches inside `formatDurationShort`
- [ ] Grep for `分钟` and `小时` in `TimelineScreen.kt` → both present inside `formatDurationShort`
- [ ] No files outside `TimelineScreen.kt` are modified (`git status --short`)
- [ ] `plans/README.md` status row updated *(orchestrator-owned; developer subagents skip this)*

## STOP conditions

Stop and report back (do not improvise) if:

- The `formatDurationShort` function body doesn't match the excerpt (drift).
- The function is no longer `private` or has a different signature (would mean a prior change altered testability — STOP rather than also changing visibility).
- A call site passes something other than `(Long, Long)` — the function relies on millisecond timestamps; if callers now pass minutes or seconds, the formatter semantics changed and this plan's branches don't apply.

## Maintenance notes

- If a future plan makes `formatDurationShort` testable (e.g. `internal`), add unit tests for the three branches: `<60`, whole-hour, fractional-hour. The boundary at `minutes == 60` should yield `"1小时"` (whole-hour branch), not `"60分钟"`.
- Reviewer: confirm the in-progress badge (`status == 1`) still fits its colored `Box` — `"1.5小时"` is similar width to the old `"1.5hours"`, so layout should be unchanged.
- Dependency note: plan 015 also edits `TimelineScreen.kt`. The edits are disjoint (015 removes code near the top/middle; 017 rewrites a function near the bottom). Either order is fine; if executing both, re-run the drift check before the second one.
