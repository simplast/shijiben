# Plan: ux cycle 17 — F017

## Finding (F017)
Clicking a note on the **Timeline** navigates the user **away** to the `NotesScreen` list view, instead of opening `NoteEditorSheet` inline. The user loses their timeline context (viewing date, scroll position, surrounding events) and must re-locate the note in a separate list just to edit it — 3+ taps for what should be 1 tap.

This directly contradicts the established pattern in `SearchScreen`, where clicking a note result opens `NoteEditorSheet` inline (preserving context). `TimelineScreen` is the outlier.

### Evidence (symbol-level)
File: `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`

- **Line 275**: `onNoteClick = onNotesClick` — passes the *navigation* callback (→ `NotesScreen` route) as the note-row click handler.
- **Lines 451–454**: `NoteRow(note = item.note, onClick = onNoteClick)` — the row's click triggers navigation away.
- **Line 423**: `onNoteClick: () -> Unit` — `EventList` signature takes a no-arg callback, so the specific note is discarded; `NotesScreen` has no way to know which note to scroll to / open.

Contrast — `app/src/main/java/com/shijiben/feature/search/SearchScreen.kt:216–222`:
```kotlin
is SearchViewModel.SearchItem.NoteItem -> NoteRow(
    note = item.note,
    onClick = {
        notesViewModel.startEdit(item.note)
        showNoteSheet = true
    }
)
```
Search opens the editor **inline**, preserving the search-results context. Timeline should do the same.

### Why this matters (project philosophy)
- **记录即审视**: the timeline interleaves notes with events by timestamp *so the user can review them in context*. Clicking a note to edit it should keep that context — navigating away breaks the review flow.
- **轻量存在**: 1 tap vs 3+ taps. No new screens, no new concepts — just removing friction.
- **Not anxiety-inducing**: this is pure friction reduction; no forcing, no new obligations.

### Existing infrastructure (no new code needed)
- `NotesViewModel` is **already injected** into `TimelineScreen` (line 110: `notesViewModel: NotesViewModel = hiltViewModel()`).
- `NotesViewModel.startEdit(note)`, `.editing`, `.save(content)`, `.delete(note)`, `.closeSheet()` all exist (`NotesViewModel.kt:35–63`).
- `NoteEditorSheet` is a public composable in `feature/notes/NoteEditorSheet.kt` — already imported and used by `SearchScreen`.
- `TimelineViewModel.notes` is backed by `combine(_viewingDate, _refreshTrigger)` (line 46) — needs `viewModel.refresh()` after a note mutation (same pattern as the existing note-drawer save at `TimelineScreen.kt:310–313`).

## Fix
Mirror `SearchScreen`'s inline-note-editing pattern in `TimelineScreen`.

### Change `onNoteClick` from navigation to inline-edit
1. **`EventList` signature** (line 423): change `onNoteClick: () -> Unit` → `onNoteClick: (NoteEntity) -> Unit` so the specific note is forwarded.
2. **`EventList` body** (line 451–454): `onClick = { onNoteClick(item.note) }`.
3. **`EventList` call site** (line 275): `onNoteClick = { note -> notesViewModel.startEdit(note); showNoteSheet = true }`.

### Add inline `NoteEditorSheet` state + rendering
4. Add local state alongside the existing `showSheet`/`editingEvent`/`pendingDelete` state (~line 130):
   ```kotlin
   val editingNote by notesViewModel.editing.collectAsStateWithLifecycle()
   var showNoteSheet by remember { mutableStateOf(false) }
   ```
5. Add the sheet rendering near the existing `RecordingSheet` block (~line 325), mirroring `SearchScreen.kt:238–245`:
   ```kotlin
   if (showNoteSheet) {
       NoteEditorSheet(
           editing = editingNote,
           onDismiss = { showNoteSheet = false; notesViewModel.closeSheet() },
           onSave = { content ->
               val ok = notesViewModel.save(content)
               if (ok) viewModel.refresh()
               ok
           },
           onDelete = { note ->
               notesViewModel.delete(note)
               viewModel.refresh()
           }
       )
   }
   ```
6. Add import: `import com.shijiben.feature.notes.NoteEditorSheet` (next to the existing `NotesViewModel` import at line 78).

`onNotesClick` (the navigation callback) stays wired to the `BottomEntryBar`'s ✎ icon button (line 289) — that's a "view all notes" action, a different intent than "edit this specific note". No change there.

## Scope

**In scope** (1 file):
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`

**Out of scope**:
- `NoteEditorSheet.kt` — unchanged (public API already supports this use case).
- `NotesViewModel.kt` — unchanged (all needed methods exist).
- `SearchScreen.kt` — already does the right thing; reference implementation, not modified.
- `TimelineViewModel.kt` — `refresh()` already exists.

## Risk assessment
- **Behavior change**: note rows on timeline now open an inline sheet instead of navigating away. This is the intended improvement; the ✎ icon in the bottom bar still navigates to the full notes list.
- **State consistency**: `NotesViewModel` is a separate `hiltViewModel()` instance per nav entry (same as `SearchScreen`'s usage) — no cross-screen state leakage.
- **Refresh**: `viewModel.refresh()` after save/delete keeps the timeline's `notes` list in sync (same pattern as existing note-drawer save at line 310–313).
- **`EventList` is private to `TimelineScreen.kt`** (defined at line 416, only called at line 265 — both in this file). Signature change has no external callers.
- **No new tests needed**: pure UI wiring; `NotesViewModel` save/delete are already covered by `NotesViewModelTest`.

## Steps
1. Add `import com.shijiben.feature.notes.NoteEditorSheet` to `TimelineScreen.kt`.
2. Add `editingNote` collector + `showNoteSheet` state next to existing `showSheet`/`editingEvent` state.
3. Change `EventList`'s `onNoteClick` parameter type from `() -> Unit` to `(NoteEntity) -> Unit`.
4. Update `EventList` body: `NoteRow(note = item.note, onClick = { onNoteClick(item.note) })`.
5. Update `EventList` call site: `onNoteClick = { note -> notesViewModel.startEdit(note); showNoteSheet = true }`.
6. Add `NoteEditorSheet(...)` rendering block (copy `SearchScreen.kt:238–245` pattern, add `viewModel.refresh()` calls).

## Gate
```bash
./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease
```

Known baseline flaky (treat as PASS):
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `HeatmapYearViewModelTest > yearGrid_updatesWhenRepoEmitsNewData`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
- `TimelineViewModelTest > markInProgress_validEventId_setsInProgressWithNullEndTime`
