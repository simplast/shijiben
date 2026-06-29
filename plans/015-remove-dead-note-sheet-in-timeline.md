# Plan 015: Remove dead `showNoteSheet`/`NoteEditorSheet` block from TimelineScreen

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 2a71f17..HEAD -- app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
> If this file changed since this plan was written, compare the "Current state"
> excerpts against the live code before proceeding. Compare the **content** of
> each excerpt's 3-line window (the line before, the target line(s), the line
> after), not just the line numbers — line numbers are hints, the surrounding
> lines are the drift signature. If surrounding lines shifted but the target
> code itself is unchanged, proceed. If the target code itself has changed,
> treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: tech-debt (dead UI code)
- **Planned at**: commit `2a71f17`, 2026-06-29

## Why this matters

`TimelineScreen` declares `showNoteSheet` and renders a `NoteEditorSheet` when it's true — but nothing in the file ever sets `showNoteSheet = true`. The whole `if (showNoteSheet) { NoteEditorSheet(...) }` block is unreachable, can never fire, and confuses any reader trying to understand how notes are edited from the timeline (the real path is `onNoteClick → navigate(Routes.NOTES)`). Removing it shrinks the surface area and removes a misleading "second" note-editing path that doesn't exist.

## Current state

The relevant file:

- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — the home screen. Notes are opened via navigation to `Routes.NOTES`, never via an inline sheet.

The dead pieces, as 3-line windows (content-labeled, line numbers are hints):

1. The state declaration — inside `TimelineScreen(...)` composable, near the other `remember`-backed UI state:
   ```
   // TimelineScreen.kt, inside TimelineScreen() — around the var showSheet/editingEvent block
   var showSheet by remember { mutableStateOf(false) }
   var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
   var showNoteSheet by remember { mutableStateOf(false) }   // ← DEAD: never set to true
   ```

2. The `editingNote` collection that only feeds the dead block:
   ```
   // TimelineScreen.kt, inside TimelineScreen() — after val scope = rememberCoroutineScope()
   val scope = rememberCoroutineScope()
   val editingNote by notesViewModel.editing.collectAsStateWithLifecycle()   // ← only used by dead block
   ```
   (`notesViewModel` itself is NOT dead — it's used in the `EntryDrawer` note submit via `notesViewModel.save(text.trim())`. Keep `notesViewModel`; remove only `editingNote`.)

3. The unreachable render block — sits after the `if (showSheet) { RecordingSheet(...) }` block, before `if (showDatePicker)`:
   ```
   // TimelineScreen.kt, inside the root Box, after RecordingSheet block
       if (showSheet) {
           RecordingSheet(
               viewingDate = date,
               editingEvent = editingEvent,
               onDismiss = { showSheet = false },
               onSaved = { viewModel.refresh(); showSheet = false }
           )
       }

       if (showNoteSheet) {                       // ← DEAD BLOCK START
           NoteEditorSheet(
               editing = editingNote,
               onDismiss = { showNoteSheet = false; notesViewModel.closeSheet() },
               onSave = { content -> val ok = notesViewModel.save(content); if (ok) viewModel.refresh(); ok },
               onDelete = { note -> notesViewModel.delete(note); viewModel.refresh() }
           )
       }                                          // ← DEAD BLOCK END
   ```

4. The import that becomes unused once the block is gone (near the top of the file):
   ```
   // TimelineScreen.kt, imports
   import com.shijiben.feature.notes.NoteEditorSheet
   import com.shijiben.feature.notes.NotesViewModel
   ```
   `NotesViewModel` stays (used by the `notesViewModel` param + `EntryDrawer`). Remove only `NoteEditorSheet`.

## Repo conventions to match

- 8-bit aesthetic, Kotlin + Jetpack Compose, MVVM. See `AGENT.md` "开发约定".
- No Compose UI tests exist; verification is build + existing unit tests. The four-gate verification is in `AGENT.md` "验证门（四道全绿才算过）".

## Commands you will need

| Purpose   | Command                                              | Expected on success |
|-----------|------------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`                  | exit 0, no errors   |
| Tests     | `./gradlew :app:testDebugUnitTest --rerun-tasks`     | all pass            |
| Build     | `./gradlew assembleDebug`                            | exit 0, APK produced |

## Scope

**In scope** (the only file you should modify):
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`

**Out of scope** (do NOT touch):
- `NoteEditorSheet.kt`, `NotesScreen.kt`, `NotesViewModel.kt` — the sheet is still used by `NotesScreen` and `SearchScreen`. Do not change them.
- `AppNavHost.kt` — routing is unchanged.
- Any other screen.

## Git workflow

- Work directly on the current branch (repo is single-developer; no worktree convention in `AGENT.md`).
- Commit message style (from `git log`): `<area>: <short desc>` or `<NNN>: <short desc>`. Example: `015: remove dead NoteEditorSheet block from TimelineScreen`.

## Steps

### Step 1: Remove the dead render block

In `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`, delete the entire `if (showNoteSheet) { NoteEditorSheet(...) }` block shown in excerpt (3) above. Leave the surrounding `if (showSheet) { RecordingSheet(...) }` block and the following `if (showDatePicker) { ... }` block intact.

### Step 2: Remove the now-unused `editingNote` line

Delete the single line:
```
val editingNote by notesViewModel.editing.collectAsStateWithLifecycle()
```
Do **not** remove `notesViewModel` or its `hiltViewModel()` param — `EntryDrawer` still calls `notesViewModel.save(text.trim())`.

### Step 3: Remove the `showNoteSheet` declaration

Delete the single line:
```
var showNoteSheet by remember { mutableStateOf(false) }
```
Leave `showSheet`, `editingEvent`, `showNoteSheet`'s siblings (`showDatePicker`, `pendingDelete`, `activeDrawer`, `eventDraft`, `noteDraft`) untouched.

### Step 4: Remove the unused import

Delete the single line:
```
import com.shijiben.feature.notes.NoteEditorSheet
```
Leave `import com.shijiben.feature.notes.NotesViewModel` in place.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0, no errors. (If the compiler reports `NoteEditorSheet` or `showNoteSheet` still referenced, you missed a site — search and remove it.)

### Step 5: Confirm no stale references remain

Run a search (with the Grep tool, not shell grep) for `showNoteSheet` and `NoteEditorSheet` in `TimelineScreen.kt` — expect zero matches. `NoteEditorSheet` should still appear in `NotesScreen.kt` and `SearchScreen.kt` (those are legitimate callers); do not touch them.

**Verify**: `./gradlew :app:testDebugUnitTest --rerun-tasks` → all pass. Then `./gradlew assembleDebug` → exit 0.

## Test plan

- No new tests. The removed block was unreachable, so no behavior changes to cover.
- Existing tests (`TimelineViewModelTest`, etc.) continue to pass — they don't touch this composable.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest --rerun-tasks` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] Grep for `showNoteSheet` in `TimelineScreen.kt` → no matches
- [ ] Grep for `NoteEditorSheet` in `TimelineScreen.kt` → no matches
- [ ] No files outside `TimelineScreen.kt` are modified (`git status --short`)
- [ ] `plans/README.md` status row updated *(orchestrator-owned; developer subagents skip this)*

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts (the codebase has drifted since this plan was written).
- `notesViewModel` turns out to be unused after removing the dead block (it shouldn't be — `EntryDrawer` uses it — but if so, STOP rather than also removing the param).
- `compileDebugKotlin` reports an error that points to a caller of `showNoteSheet`/`NoteEditorSheet` outside `TimelineScreen.kt` — would mean the audit missed a reference.

## Maintenance notes

- After this lands, the only ways to edit a note are: tap a `NoteRow` on the timeline (→ `Routes.NOTES` list → `NoteEditorSheet`), or tap a note in `SearchScreen` results. That's the intended design.
- Reviewer: confirm `notesViewModel` is still wired (param + at least one `save` call in `EntryDrawer`).
