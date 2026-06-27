# Plan 014: Render note markers on the timeline (collect the unused `notes` Flow)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 9da5c72..HEAD -- app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`
> If either in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live code before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none
- **Category**: direction
- **Planned at**: commit `9da5c72`, 2026-06-27

## Why this matters

Notes are fully CRUD-capable (`NotesScreen`, `NoteEditorSheet`, the bottom-drawer entry at `TimelineScreen.kt:198-201`) but invisible from the main screen for the day they were recorded on — the user must navigate to a separate list page to see them. The design spec at `docs/2026-06-22-shijiben-design.md:181` explicitly promised "V1 仅支持时间轴右侧点的展示" (V1 supports dot display on the right side of the timeline), and `TimelineViewModel` already runs the `notes` StateFlow on every `viewingDate` change (`TimelineViewModel.kt:46-51`) — but `TimelineScreen` never collects it. The data is flowing, the UI just isn't listening. This plan collects that Flow and renders note markers interleaved with events in `EventList` (the "right side" of the timeline), so a note taken at 14:30 appears between the 14:00 and 15:00 events. It delivers a stated-but-undelivered V1 feature at low risk (purely additive rendering, no migration).

## Current state

### `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`

The `notes` StateFlow is declared but uncollected (lines 46-51):

```kotlin
val notes: StateFlow<List<NoteEntity>> = combine(_viewingDate, _refreshTrigger) { d, _ -> d }
    .flatMapLatest { (y, m, d) ->
        val (start, end) = dayRange(y, m, d)
        noteRepository.getNotesByDateRange(start, end)
    }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

`NoteEntity` import is at line 6: `import com.shijiben.data.local.NoteEntity`. The Flow already runs a Room query per day change — this plan makes that work visible, it does not add new query cost.

### `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`

State collection (line 88) — only `events` is collected; `notes` is not:

```kotlin
val events by viewModel.events.collectAsStateWithLifecycle()
val date by viewModel.viewingDate.collectAsStateWithLifecycle()
```

The `EventList` call (lines 177-188) — passes `events` and `now`, no notes:

```kotlin
EventList(
    events = events,
    onEventClick = { event -> editingEvent = event; showSheet = true },
    onStartEvent = { event -> viewModel.markInProgress(event.id) },
    onStopEvent = { event -> viewModel.markCompleted(event.id) },
    onEventLongClick = { event -> pendingDelete = event },
    now = now
)
```

The `EventList` composable (lines 283-315) — renders events in a `Column+verticalScroll+for` loop:

```kotlin
@Composable
fun EventList(
    events: List<EventEntity>,
    onEventClick: (EventEntity) -> Unit,
    onStartEvent: (EventEntity) -> Unit,
    onStopEvent: (EventEntity) -> Unit,
    onEventLongClick: (EventEntity) -> Unit,
    now: Long
) {
    Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
        if (events.isEmpty()) { /* "今天还是空白" empty state */ }
        else {
            for (event in events) {
                EventCard(event = event, onClick = { onEventClick(event) }, ..., now = now)
            }
        }
    }
}
```

### `app/src/main/java/com/shijiben/data/local/NoteEntity.kt`

```kotlin
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val timestamp: Long,     // the note's moment — this is the sort key for interleaving
    val createdAt: Long,
    val updatedAt: Long
)
```

### `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — `EventCard` shape (lines 317-439)

For reference, the existing `EventCard` uses: `Surface(color = Surface, shape = RoundedCornerShape(0.dp), shadowElevation = 2.dp)`, `Row` with `horizontal=12.dp, vertical=10.dp` padding, `Text` with `FontWeight.SemiBold`/`fontSize=15.sp`, time formatted via the file-local `private fun formatTime(ts: Long): String` (line 441, `SimpleDateFormat("HH:mm", Locale.getDefault())`). Match this style for `NoteRow`.

### Design intent

From `docs/2026-06-22-shijiben-design.md:181`:
> V1 仅支持时间轴右侧点的展示

The "时间轴右侧" (right side of the timeline) is the `EventList` area (the layout at `TimelineScreen.kt:168-189` puts `DayProgressBar` on the left at 40dp width, `EventList` on the right filling the rest). So note markers belong in `EventList`, interleaved by timestamp — a note at 14:30 appears between the 14:00 event and the 15:00 event.

### Color identity (from the redesign spec)

Per `docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md:82` ("颜色身份区分：记事=红、随笔=橙") and the existing `BottomEntryBar` at `TimelineScreen.kt:547-650` (which uses `Accent` orange for the ✎ notes icon and `Primary` red for the 📅 event icon), notes use the `Accent` (orange) color identity. Reuse `Accent` and `Icons.Default.Edit` (already imported at `TimelineScreen.kt:32`) for note markers.

### Repo conventions to match

- **Compose patterns**: `collectAsStateWithLifecycle` (from `androidx.lifecycle.compose`, already imported at line 68), `Modifier.border(2.dp, Color.Black)` for 8-bit borders, `RoundedCornerShape(0.dp)` for hard edges, `FontWeight.Bold` for pixel-style text. See `EventCard` and `BottomEntryBar` as exemplars.
- **Color tokens** from `com.shijiben.ui.theme.*` (imported at line 74): `Surface` (white bg), `Accent` (orange), `Primary` (red), `TextPrimary`, `TextSecondary`, `TextTertiary`, `BorderLight`. Do NOT hardcode `Color(0xFF...)` — use tokens. (Exception: the rainbow trim uses hardcoded colors, but that's a different concern.)
- **Time formatting**: use the file-local `formatTime(ts: Long)` at line 441 (`SimpleDateFormat("HH:mm")`). Do not duplicate it.
- **Tap behavior**: existing `onNotesClick: () -> Unit` parameter on `TimelineScreen` (line 84) navigates to `NotesScreen` (wired in `AppNavHost.kt`). For a note-row tap, reuse this — do NOT add inline note editing in this plan (deferred; see Maintenance notes).

## Commands you will need

| Purpose   | Command                                          | Expected on success |
|-----------|--------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`              | exit 0, no errors   |
| Tests     | `./gradlew :app:testDebugUnitTest`               | exit 0              |
| Build     | `./gradlew assembleDebug`                        | exit 0              |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`

**Out of scope** (do NOT touch):
- `TimelineViewModel.kt` — the `notes` Flow is already correct; do not change its query, shape, or refresh wiring.
- `NoteEntity.kt`, `NoteDao.kt`, `NoteRepository.kt` — data layer is correct.
- `DayProgressBar.kt` — note markers go in `EventList` (right side), not the left rail. A future plan may add left-rail dots, but this plan delivers the stated "right side" design.
- `AppNavHost.kt`, `NotesScreen.kt`, `NoteEditorSheet.kt` — navigation and notes CRUD are correct.
- Inline note editing (tap → open `NoteEditorSheet` in edit mode) — deferred; this plan's tap → `onNotesClick()` (navigate to notes list). See Maintenance notes.

## Git workflow

- Branch: `advisor/014-timeline-note-markers`
- Commit per logical step or per logical unit; message style: `feat(timeline): render note markers interleaved with events (plan 014)` (matches recent `feat(timeline):` style, e.g. `feat(timeline): 底部双 block 统一入口 + 多行抽屉 (Part B)`).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Collect `viewModel.notes` in `TimelineScreen`

In `TimelineScreen.kt`, after the `events` collection (line 88), add collection of `notes`:

```kotlin
val events by viewModel.events.collectAsStateWithLifecycle()
val notes by viewModel.notes.collectAsStateWithLifecycle()
val date by viewModel.viewingDate.collectAsStateWithLifecycle()
```

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0 (the collected value is unused for now; compile must still pass).

### Step 2: Add a private sealed `TimelineItem` type for interleaving

Near the top of `TimelineScreen.kt` (after the imports, before `@Composable fun TimelineScreen` at line 82), add a private sealed interface so events and notes can be merged into a single sorted list:

```kotlin
private sealed interface TimelineItem {
    val sortKey: Long
    data class EventItem(val event: EventEntity) : TimelineItem {
        override val sortKey: Long get() = event.startTime
    }
    data class NoteItem(val note: NoteEntity) : TimelineItem {
        override val sortKey: Long get() = note.timestamp
    }
}
```

This keeps the merge logic type-safe and avoids `Any?` casting in the render loop. `EventEntity` is already imported (line 70); `NoteEntity` is NOT — add `import com.shijiben.data.local.NoteEntity` near line 70.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 3: Change `EventList` signature to accept notes + a note-tap callback

Update the `EventList` composable signature (lines 283-291) to accept notes and a note-tap callback:

```kotlin
@Composable
fun EventList(
    events: List<EventEntity>,
    notes: List<NoteEntity>,
    onEventClick: (EventEntity) -> Unit,
    onStartEvent: (EventEntity) -> Unit,
    onStopEvent: (EventEntity) -> Unit,
    onEventLongClick: (EventEntity) -> Unit,
    onNoteClick: () -> Unit,
    now: Long
) {
```

Then update the body (lines 292-314) to merge events and notes by `sortKey` and render each:

```kotlin
Column(modifier = Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
    val items = remember(events, notes) {
        (events.map { TimelineItem.EventItem(it) } + notes.map { TimelineItem.NoteItem(it) })
            .sortedBy { it.sortKey }
    }
    if (items.isEmpty()) {
        Box(modifier = Modifier.fillMaxHeight(), contentAlignment = Alignment.Center) {
            Text(
                text = "今天还是空白",
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    } else {
        items.forEach { item ->
            when (item) {
                is TimelineItem.EventItem -> EventCard(
                    event = item.event,
                    onClick = { onEventClick(item.event) },
                    onStart = { onStartEvent(item.event) },
                    onStop = { onStopEvent(item.event) },
                    onLongClick = { onEventLongClick(item.event) },
                    now = now
                )
                is TimelineItem.NoteItem -> NoteRow(
                    note = item.note,
                    onClick = onNoteClick
                )
            }
        }
    }
}
```

Note: the empty-state condition changes from `events.isEmpty()` to `items.isEmpty()` — a day with only notes (no events) is no longer "空白". This is the intended behavior (notes are records of the day).

`remember` needs the import — `androidx.compose.runtime.remember` is already imported (line 49).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0 (the new `NoteRow` composable doesn't exist yet, so this will FAIL — proceed to Step 4 to add it, then re-run).

### Step 4: Add the `NoteRow` composable

Add a new composable near `EventCard` (after line 439, before the `formatTime` helper at line 441). Match the `EventCard` 8-bit style: `Surface` with `RoundedCornerShape(0.dp)`, `shadowElevation = 2.dp`, 2dp black border accent on the icon box, `Accent` (orange) color identity:

```kotlin
@Composable
private fun NoteRow(
    note: NoteEntity,
    onClick: () -> Unit
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 橙色 ✎ 图标盒（2dp 黑边，8-bit 风），与 BottomEntryBar 的随笔色身份一致
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .border(2.dp, Color.Black)
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "随笔",
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatTime(note.timestamp),
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = note.content,
                fontSize = 13.sp,
                color = TextPrimary,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
```

All symbols used are already imported: `Surface` (line 41), `RoundedCornerShape` (line 25), `clickable` (line 8), `Box`/`Row`/`Spacer` (lines 11-14), `border`/`background` (lines 5-6), `Icon` (line 39), `Icons.Default.Edit` (line 32), `Text` (line 42), `Color` (line 57), `Modifier` (line 53), `dp`/`sp` (lines 65-66), `FontWeight` (line 62). `TextOverflow` is the one new symbol — add `import androidx.compose.ui.text.style.TextOverflow` near the other `androidx.compose.ui.text` imports (lines 61-63), OR fully-qualify it as shown above. Prefer the import.

`background` import: line 5 imports `androidx.compose.foundation.background` — already present. `fillMaxWidth` (line 17), `padding` (line 21), `size` (line 22), `width` (line 23) — all present.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 5: Wire `EventList` call site to pass `notes` and `onNoteClick`

Update the `EventList` call in `TimelineScreen` (lines 177-188) to pass the new parameters:

```kotlin
EventList(
    events = events,
    notes = notes,
    onEventClick = { event -> editingEvent = event; showSheet = true },
    onStartEvent = { event -> viewModel.markInProgress(event.id) },
    onStopEvent = { event -> viewModel.markCompleted(event.id) },
    onEventLongClick = { event -> pendingDelete = event },
    onNoteClick = onNotesClick,
    now = now
)
```

`onNotesClick` is the existing `TimelineScreen` parameter (line 84) — reusing it means tapping a note row navigates to `NotesScreen` (the notes list page). Inline note editing is deferred (see Maintenance notes).

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0
- `./gradlew assembleDebug` → exit 0

### Step 6: Manual device verification (cannot be automated)

Build and install to a connected device/emulator:

```
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verify on-device:
1. Create a note via the bottom-drawer ✎ entry (or NotesScreen). It should appear as an orange-accented row in the timeline at the note's timestamp, interleaved with events by time.
2. A day with only notes (no events) should NOT show "今天还是空白" — it should show the note rows.
3. Tapping a note row should navigate to `NotesScreen`.
4. A note taken at 14:30 should appear between a 14:00 event and a 15:00 event in the list.
5. Existing event rendering (status badges, start/stop buttons, long-press delete) must be unchanged.

## Test plan

- **No new automated tests in this plan.** The repo has no Compose UI tests (finding DX-01, deferred from this batch); adding the first UI test is out of scope for a single-feature plan. The manual device verification in Step 6 is the gate.
- **Existing unit tests must stay green**: `./gradlew :app:testDebugUnitTest` → exit 0. The `TimelineViewModelTest` does not assert on `notes` collection, so it is unaffected; `RecordingViewModelTest` is unrelated.
- **When DX-01 lands** (Compose UI tests), add a characterization test for `EventList` asserting: (a) notes appear interleaved by timestamp, (b) the empty-state shows "今天还是空白" only when both events and notes are empty, (c) tapping a `NoteRow` invokes `onNoteClick`. Model after the future UI test pattern DX-01 establishes.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n "viewModel.notes.collectAsStateWithLifecycle" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns a match (the Flow is now collected)
- [ ] `grep -n "NoteRow" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns at least 2 matches (the composable definition and its call site)
- [ ] `grep -n "TimelineItem" app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns matches (the sealed type is defined and used)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] Manual device verification (Step 6) passes
- [ ] `plans/README.md` status row for 014 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The code at `TimelineScreen.kt:82-315` or `TimelineViewModel.kt:46-51` doesn't match the excerpts in "Current state" (drifted since this plan was written).
- `viewModel.notes` does not exist on `TimelineViewModel` (it does at line 46 as of this plan — if missing, the codebase has drifted; report).
- `EventList` is changed to `LazyColumn` (finding PERF-02, not in this batch) before this plan lands — the merge-and-`for` loop in Step 3 assumes `Column+verticalScroll`. If `LazyColumn` is in place, the interleave logic moves into `items(events + notes.sortedBy { ... })` instead; report the new shape and re-plan rather than reverting.
- The `Accent` color token or `Icons.Default.Edit` import is not present in `TimelineScreen.kt` (both are, per the Part B redesign — but verify in drift check).
- `note.content` is not a `String` (it is, per `NoteEntity.kt:15` — but if the entity has drifted, report).
- A new import conflicts with an existing one (e.g. `TextOverflow` is already imported under a different alias) — resolve by fully-qualifying; if that fails, report.

## Maintenance notes

- **Inline note editing is deferred.** This plan's tap → `onNotesClick()` navigates to the notes LIST page, requiring the user to find the note there. A better future UX: tap `NoteRow` → open `NoteEditorSheet` in edit mode with `notesViewModel.openForEdit(note)`. This needs a `NotesViewModel.openForEdit` method (verify it exists before the follow-up plan) and a `showNoteSheet = true` trigger. Defer to a separate plan once the edit-path bug (plan 011) is stable.
- **`EventList` is still `Column+verticalScroll`**, not `LazyColumn` (finding PERF-02, not in this batch). Interleaving notes increases the list size, making the `LazyColumn` swap slightly higher-value. When PERF-02 lands, the merge logic in Step 3 moves into `items(events + notes, key = { ... })`; preserve the `TimelineItem` sealed type or replace with a tagged union — either is fine, but keep the sort-by-timestamp behavior.
- **`now` is not passed to `NoteRow`** because notes don't have a "running" state (they're instantaneous). If a future design adds note editing or a "time ago" label, `now` may need to flow in.
- **Note markers on `DayProgressBar`** (left rail) are NOT in this plan — the design doc says "时间轴右侧" (right side). A future plan may add a small dot on the left-rail hour box that has a note, as a secondary signal; that plan would touch `DayProgressBar.kt` and should union note hours into `coveredHours`.
- **The `notes` Flow's `refreshTrigger` coupling** (finding PERF-06) means saving an event re-queries notes too. This plan does not change that; when PERF-06 lands and the triggers split, verify the note markers still update on note-save (the `onSubmit` callback at `TimelineScreen.kt:222` calls `viewModel.refresh()` after a note save — keep that call).
