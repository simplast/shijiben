# Plan 010: Remove dead `EventList.kt` file

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/timeline/EventList.kt app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
> If either file changed since this plan was written, compare the "Current
> state" excerpts against live code; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (but land AFTER plans 001 and 005, which modify the *inline* `EventList` in `TimelineScreen.kt` — deleting this dead file is independent, but verifying "no callers" is cleaner after those land)
- **Category**: tech-debt
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

`app/src/main/java/com/shijiben/feature/timeline/EventList.kt` defines a top-level `EventList` composable (lines 22-52) that is **never called** — `TimelineScreen.kt` defines its own inline `EventList` (line 222) with a different signature (4 callbacks vs. 1), and that's the one used by the app. Both live in the same package `com.shijiben.feature.timeline`, so they coexist as overloads (Kotlin permits top-level overloading by signature). The dead file is a maintenance trap: a future reader may edit it thinking it's the live `EventList`, or wonder why there are two. It also imports `PixelText`/`PixelTextSecondary` and a `formatTime` helper that nothing else uses. Removing it shrinks the surface and removes the confusion.

## Current state

File in scope:
- `app/src/main/java/com/shijiben/feature/timeline/EventList.kt` — the entire file (52 lines).

Excerpt — `EventList.kt:22-52` (the dead composable):
```kotlin
@Composable
fun EventList(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        if (events.isEmpty()) {
            Text(
                text = "今天还是空白",
                color = PixelTextSecondary,
                fontSize = 15.sp,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            for (event in events) {
                Text(
                    text = "${formatTime(event.startTime)} - ${event.title}",
                    color = PixelText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

private fun formatTime(ts: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(ts))
```

The live `EventList` — `TimelineScreen.kt:222-249` — has signature `(events, onEventClick, onStartEvent, onStopEvent)` (and `onEventLongClick`/`now` after plans 001/005 land). It is the one called at `TimelineScreen.kt:152`. The dead file's 3-param version is never called.

### Confirmation that nothing calls the dead overload

Before deleting, verify no call site passes the 3-param signature `(events, onEventClick, modifier)`. The only `EventList(...)` call is at `TimelineScreen.kt:152` and passes 4+ callbacks — that resolves to the inline overload, not the dead one.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/timeline/EventList.kt` (delete the file)

**Out of scope**:
- `TimelineScreen.kt` — do not modify (its inline `EventList` is the live one).
- Any other file.

## Git workflow

- Branch: `advisor/010-remove-dead-eventlist`
- Commit message example: `Remove dead EventList.kt (shadowed by inline EventList in TimelineScreen)`.

## Steps

### Step 1: Confirm no caller references the dead overload

Run these checks; ALL must hold before deleting:

**Verify**:
- `grep -rn "import com.shijiben.feature.timeline.EventList" app/src/` → no matches (it's same-package, no import needed; but if anything explicitly imports it, STOP).
- `grep -rn "EventList(" app/src/main/java/com/shijiben/feature/timeline/` → matches only at `TimelineScreen.kt:152` (call, 4+ args) and `TimelineScreen.kt:222` (inline definition). The dead definition at `EventList.kt:23` is its own file — that's fine, it's what we're deleting.
- Visually confirm the call at `TimelineScreen.kt:152` passes `onStartEvent`/`onStopEvent` (so it cannot resolve to the 3-param dead overload). If the call ever passed only `(events, onEventClick, modifier)`, STOP — the dead overload is actually in use.

### Step 2: Delete the file

Delete `app/src/main/java/com/shijiben/feature/timeline/EventList.kt`.

(Use the IDE/file deletion — the file is removed from disk; git stages the deletion on the next `git add`.)

### Step 3: Build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0 (proves nothing depended on the deleted file)
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

If the compile fails with an unresolved `EventList` reference, STOP — the dead overload was actually used somewhere Step 1 missed; restore the file and report.

## Test plan

No tests reference `EventList.kt` (it's a UI composable with no test). Existing tests must remain green:
- `./gradlew :app:testDebugUnitTest` → all pass.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `ls app/src/main/java/com/shijiben/feature/timeline/EventList.kt` → file does not exist
- [ ] `grep -rn "fun EventList" app/src/main/java/com/shijiben/feature/timeline/` → exactly one match (the inline definition in `TimelineScreen.kt`)
- [ ] `git status` shows the file deleted (and no other changes)
- [ ] `plans/README.md` status row for 010 updated to DONE

## STOP conditions

Stop and report back if:

- Step 1's grep finds an explicit `import` of `EventList` from this package, or a call site that passes only `(events, onEventClick, modifier)` — the dead overload may be in use; do not delete.
- Compile fails after deletion with an unresolved reference — restore the file immediately and report which symbol was unresolved.
- The file has diverged from the excerpt above (e.g. someone added real logic to it since the plan was written) — re-evaluate whether it's still dead before deleting.

## Maintenance notes

- After deletion, there is a single source of truth for `EventList` (the inline composable in `TimelineScreen.kt`). If a future refactor extracts it back into its own file, do so deliberately — don't recreate the dual-definition trap.
- This plan is intentionally trivial; its value is removing confusion, not adding function.
- Reviewer: the build passing is the whole verification — if it compiles and tests pass, the deletion is safe.
