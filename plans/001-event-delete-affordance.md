# Plan 001: Add event delete affordance via long-press on EventCard

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug (UX gap)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

There is currently **no way to delete an event from the UI**. `RecordingViewModel.delete()` is implemented (`app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt:144`) but no composable ever calls it, and the RecordingSheet deliberately has no delete button (per design doc `docs/2026-06-22-shijiben-design.md:149`: "编辑时无'删除'按钮"). The result: a typo'd or mistakenly-created event is permanent. The design doc forbids a delete button *inside the editor*; it does not forbid a list-level delete affordance. This plan adds long-press on an event card → confirmation dialog → delete, honoring the design constraint.

## Current state

Files in scope and their roles:

- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — defines the timeline UI, the inline `EventList` composable (line 222) and `EventCard` composable (line 251). This is the file to modify.
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt` — timeline state holder; needs a new `deleteEvent(id)` method.

Key excerpts (confirm these match live code before proceeding):

`TimelineScreen.kt:251-266` — `EventCard` Surface currently only handles click:
```kotlin
@Composable
fun EventCard(
    event: EventEntity,
    onClick: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Surface(
        color = Surface,
        shape = RoundedCornerShape(0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 6.dp),
        shadowElevation = 2.dp
    ) {
```

`TimelineScreen.kt:152-160` — `EventList` is invoked from `TimelineScreen` with three callbacks:
```kotlin
EventList(
    events = events,
    onEventClick = { event ->
        editingEvent = event
        showSheet = true
    },
    onStartEvent = { event -> viewModel.markInProgress(event.id) },
    onStopEvent = { event -> viewModel.markCompleted(event.id) }
)
```

`TimelineScreen.kt:239-247` — the inline `EventList` loops events into `EventCard`:
```kotlin
for (event in events) {
    EventCard(
        event = event,
        onClick = { onEventClick(event) },
        onStart = { onStartEvent(event) },
        onStop = { onStopEvent(event) }
    )
}
```

`TimelineViewModel.kt:95-107` — existing mutation pattern (`markInProgress`) to mirror for delete:
```kotlin
fun markInProgress(eventId: Long) {
    viewModelScope.launch {
        val event = eventRepository.getEventById(eventId) ?: return@launch
        val now = System.currentTimeMillis()
        eventRepository.updateEvent(event.copy(...))
        refresh()
    }
}
```

`EventRepository.kt:60` — delete already exists at the data layer:
```kotlin
suspend fun deleteEventById(id: Long) = eventDao.deleteEventById(id)
```

### Repo conventions to honor

- State mutation lives in the ViewModel, dispatched via `viewModelScope.launch { ... refresh() }` — match `markInProgress`/`markCompleted` exactly. See `TimelineViewModel.kt:95-121`.
- 8-bit aesthetic: use `PixelButton`/`PixelOutlinedButton` (from `ui/theme/PixelComponents.kt`) and `RoundedCornerShape(0.dp)` for any new UI. AlertDialog is acceptable for confirmations (Material3 default styling is fine; the rest of the app uses plain Material3 dialogs where needed).
- Chinese strings are inlined in composables (no `strings.xml` usage beyond `app_name`) — see `TimelineScreen.kt:232` `"今天还是空白"`. Match this: inline Chinese literals.
- `clickable` is the existing click pattern; for long-press use `Modifier.combinedClickable` (from `androidx.compose.foundation`), which is part of Compose foundation already in the dependency graph.

### Design constraint (must honor)

`docs/2026-06-22-shijiben-design.md:149`: "编辑时无'删除'按钮、无'状态'按钮". **Do NOT add a delete button to `RecordingSheet`.** Delete is list-level only (long-press on the card).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile (typecheck) | `./gradlew :app:compileDebugKotlin` | exit 0, no errors |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0, all pass |
| Build APK | `./gradlew assembleDebug` | exit 0, `app/build/outputs/apk/debug/app-debug.apk` exists |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`

**Out of scope** (do NOT touch):
- `app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` — design doc forbids delete button here.
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` — its `delete()` stays unused; leave it (a later plan may wire it). Do not remove.
- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` — `deleteEventById` already exists.
- `app/src/main/java/com/shijiben/feature/timeline/EventList.kt` — dead file (see plan 010); do not touch here.

## Git workflow

- Branch: `advisor/001-event-delete`
- Commit per logical unit. Repo has no conventional-commit history (recent commits are plain messages); use a clear imperative message, e.g. `Add long-press delete for timeline events`.
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Add `deleteEvent` to TimelineViewModel

In `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`, add a new method immediately after `markCompleted` (after line 121), mirroring the existing mutation pattern:

```kotlin
/** 删除事件（列表长按触发） */
fun deleteEvent(eventId: Long) {
    viewModelScope.launch {
        eventRepository.deleteEventById(eventId)
        refresh()
    }
}
```

`eventRepository` is already injected (see `TimelineViewModel.kt:26`). `deleteEventById` exists (`EventRepository.kt:60`).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 2: Thread a long-press callback through the inline `EventList`

In `TimelineScreen.kt`, the inline `EventList` composable (defined at line 222) currently has signature:
```kotlin
fun EventList(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    onStartEvent: (EventEntity) -> Unit,
    onStopEvent: (EventEntity) -> Unit
)
```
Add an `onEventLongClick: (EventEntity) -> Unit` parameter (last position). In its body (line 239-247 `for (event in events)` loop), pass it to `EventCard`:
```kotlin
EventCard(
    event = event,
    onClick = { onEventClick(event) },
    onStart = { onStartEvent(event) },
    onStop = { onStopEvent(event) },
    onLongClick = { onEventLongClick(event) }
)
```

### Step 3: Add `onLongClick` parameter to `EventCard` and switch to `combinedClickable`

In `EventCard` (line 251), add `onLongClick: () -> Unit` to its parameter list. Replace `.clickable(onClick = onClick)` on the `Surface` modifier (line 263) with:
```kotlin
.combinedClickable(
    onClick = onClick,
    onLongClick = onLongClick
)
```
Add the import `androidx.compose.foundation.combinedClickable` at the top of the file (the existing `import androidx.compose.foundation.clickable` at line 7 can stay or be removed if no longer used elsewhere in the file — check with grep first; if `clickable` is still referenced, keep it).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 4: Wire long-press to a delete confirmation dialog in `TimelineScreen`

In the `TimelineScreen` composable body (around line 78-83 where `showSheet`/`editingEvent` state is declared), add state for the pending-delete event and the dialog:

```kotlin
var pendingDelete by remember { mutableStateOf<EventEntity?>(null) }
```

Update the `EventList(...)` call site (line 152-160) to pass the new callback:
```kotlin
EventList(
    events = events,
    onEventClick = { event -> editingEvent = event; showSheet = true },
    onStartEvent = { event -> viewModel.markInProgress(event.id) },
    onStopEvent = { event -> viewModel.markCompleted(event.id) },
    onEventLongClick = { event -> pendingDelete = event }
)
```

Add an `AlertDialog` near the other sheet conditionals (after the `showNoteSheet` block, around line 217). Use Material3 `androidx.compose.material3.AlertDialog`:

```kotlin
pendingDelete?.let { target ->
    AlertDialog(
        onDismissRequest = { pendingDelete = null },
        title = { Text("删除这条记录？", color = TextPrimary) },
        text = { Text("「${target.title}」将被永久删除，无法恢复。", color = TextSecondary) },
        confirmButton = {
            TextButton(onClick = {
                viewModel.deleteEvent(target.id)
                pendingDelete = null
            }) { Text("删除", color = Error) }
        },
        dismissButton = {
            TextButton(onClick = { pendingDelete = null }) { Text("取消", color = TextSecondary) }
        }
    )
}
```

`TextPrimary`, `TextSecondary`, `Error` are already imported via `com.shijiben.ui.theme.*` (line 61). `TextButton` is already imported (line 8 area — confirm `androidx.compose.material3.TextButton` is imported; if not, add it).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 5: Confirm no other `EventList` reference breaks

The dead file `app/src/main/java/com/shijiben/feature/timeline/EventList.kt` defines a *different* `EventList` signature (no callbacks beyond `onEventClick`). It is never imported/called, so adding a parameter to the inline `EventList` in `TimelineScreen.kt` does not affect it. Verify nothing else calls the inline `EventList`:

**Verify**: `grep -rn "EventList(" app/src/main/java/` → only the definition at `TimelineScreen.kt:222` and the call at `TimelineScreen.kt:152`. (The dead `EventList.kt:23` definition is a different file and won't match `EventList(` with this signature; if it appears, it is its own definition, not a caller — that's fine.)

## Test plan

The repo has no Compose UI tests; verification is build + existing unit tests. Existing tests must still pass:

- `./gradlew :app:testDebugUnitTest` → all pass (TimelineViewModelTest, RecordingViewModelTest, repository tests).

No new unit test is required for this UI-wiring change (the data-layer delete is already covered indirectly by repository tests). If you want to add one, model after `app/src/test/java/com/shijiben/feature/timeline/TimelineViewModelTest.kt` and assert that calling `deleteEvent(id)` triggers `deleteEventById` on a fake repository — but this is optional given the change is thin UI wiring.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0 (all existing tests pass)
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n "fun deleteEvent" app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt` returns one match
- [ ] `grep -n "combinedClickable" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns at least one match
- [ ] `grep -n "pendingDelete" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns matches (state + dialog usage)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 001 updated to DONE

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `TimelineScreen.kt:251-266` or `TimelineViewModel.kt:95-121` doesn't match the excerpts above (codebase has drifted).
- `combinedClickable` is not resolvable from `androidx.compose.foundation` (it should be — Compose foundation 2024.10.01 BOM has it; if not, STOP and report the unresolved reference).
- `EventRepository.deleteEventById` does not exist at `EventRepository.kt:60` (it should — if missing, the data layer changed).
- Adding the `onLongClick` parameter causes a compile error in a caller you didn't expect (i.e., a caller of the inline `EventList` other than the one at `TimelineScreen.kt:152`).

## Maintenance notes

- Future: if a delete button is ever added to `RecordingSheet`, prefer reusing `TimelineViewModel.deleteEvent` rather than `RecordingViewModel.delete()` to keep a single delete path. The unused `RecordingViewModel.delete()` is intentionally left in place.
- Reviewer should scrutinize: (1) that long-press does not also fire the card's `onClick` (Compose `combinedClickable` handles this correctly — verify on device); (2) that the dialog dismisses on confirm.
- Deferred: swipe-to-delete was considered and rejected for v1 — long-press is more discoverable for the 8-bit aesthetic and simpler to implement reliably.
