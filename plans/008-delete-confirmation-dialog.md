# Plan 008: Add delete confirmation dialog for tags and notes

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt app/src/main/java/com/shijiben/feature/tags/TagEditorSheet.kt`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against live code; on a mismatch, treat it as a
> STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug (UX safety)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

In `NoteEditorSheet` and `TagEditorSheet`, the "删除" button immediately deletes with no confirmation (NoteEditorSheet.kt:85-94, TagEditorSheet.kt:113-122). Tag deletion is especially destructive: `TagsViewModel.delete` cascades — it clears the `tagId` reference on every event that used the tag (TagsViewModel.kt:62-68) before deleting the tag itself. A single accidental tap permanently removes a tag from all historical events with no way back. This directly conflicts with the product philosophy of not losing track of life (`MY_ORIGIN_GOAL.md`: "记录就是审视的前提"). This plan adds a confirmation `AlertDialog` gating both delete buttons. (Full undo via Snackbar was considered and deferred — see Maintenance notes.)

## Current state

Files in scope:
- `app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` — the "删除" `PixelOutlinedButton` (lines 85-94).
- `app/src/main/java/com/shijiben/feature/tags/TagEditorSheet.kt` — the "删除" `PixelOutlinedButton` (lines 113-122).

Excerpt — `NoteEditorSheet.kt:84-95`:
```kotlin
if (editing != null) {
    PixelOutlinedButton(
        text = "删除",
        onClick = {
            scope.launch {
                onDelete(editing)
                onDismiss()
            }
        },
        modifier = Modifier.weight(1f).height(48.dp)
    )
}
```

Excerpt — `TagEditorSheet.kt:112-123`:
```kotlin
if (editing != null) {
    PixelOutlinedButton(
        text = "删除",
        onClick = {
            scope.launch {
                onDelete(editing)
                onDismiss()
            }
        },
        modifier = Modifier.weight(1f).height(48.dp)
    )
}
```

Both files already import `androidx.compose.material3.ModalBottomSheet`, `OutlinedTextField`, `Text` from material3. Neither imports `AlertDialog` or `TextButton` yet — those will be added.

The tag delete cascade — `TagsViewModel.kt:62-68`:
```kotlin
suspend fun delete(tag: TagEntity) {
    // 先清除事件引用，再删除标签
    eventRepository.clearTagReference(tag.id)
    tagRepository.deleteTag(tag)
    ...
}
```

### Repo conventions

- Material3 components used throughout (`ModalBottomSheet`, `OutlinedTextField`, `TextButton` in RecordingSheet).
- Chinese inline strings.
- `PixelOutlinedButton`/`PixelButton` from `ui/theme`.
- The confirmation-dialog pattern is established by plan 001 (event delete) using `AlertDialog` + `TextButton`. Match that exactly for consistency.

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt`
- `app/src/main/java/com/shijiben/feature/tags/TagEditorSheet.kt`

**Out of scope**:
- `NotesViewModel` / `TagsViewModel` — delete logic is correct; only the UI gating changes.
- `NotesScreen` / `TagsScreen` — the editor sheets are the only delete entry points.
- Snackbar-based undo (deferred — see Maintenance notes).
- Plan 001's event-delete dialog (separate file/pattern; this plan mirrors it, doesn't depend on it).

## Git workflow

- Branch: `advisor/008-delete-confirm`
- Commit message example: `Confirm before deleting tags and notes`.

## Steps

### Step 1: Add a confirmation dialog to `NoteEditorSheet`

In `NoteEditorSheet.kt`, add a `var showDeleteConfirm by remember { mutableStateOf(false) }` near the existing `content` state (line 46).

Change the "删除" button's `onClick` (lines 87-92) to set `showDeleteConfirm = true` instead of deleting immediately:
```kotlin
PixelOutlinedButton(
    text = "删除",
    onClick = { showDeleteConfirm = true },
    modifier = Modifier.weight(1f).height(48.dp)
)
```

Add an `AlertDialog` inside the `Column` (after the button Row, before the trailing `Spacer` at line 112):
```kotlin
if (showDeleteConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("删除这条随笔？") },
        text = { Text("删除后无法恢复。") },
        confirmButton = {
            TextButton(onClick = {
                showDeleteConfirm = false
                scope.launch {
                    onDelete(editing!!)
                    onDismiss()
                }
            }) { Text("删除", color = androidx.compose.ui.graphics.Color(0xFFEF4444)) }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
        }
    )
}
```

`editing!!` is safe here because the "删除" button only renders when `editing != null` (the `if (editing != null)` guard at line 84). Add imports: `androidx.compose.material3.AlertDialog`, `androidx.compose.material3.TextButton`. (`Text` is already imported; `scope` already declared at line 47.)

### Step 2: Add a confirmation dialog to `TagEditorSheet` (with stronger wording)

In `TagEditorSheet.kt`, add `var showDeleteConfirm by remember { mutableStateOf(false) }` near `name`/`selectedColor` state (line 50-53).

Change the "删除" button's `onClick` (lines 115-120) to `onClick = { showDeleteConfirm = true }`.

Add the `AlertDialog` (after the button Row, before the trailing `Spacer` at line 140). Use stronger wording because tag deletion cascades to events:
```kotlin
if (showDeleteConfirm) {
    AlertDialog(
        onDismissRequest = { showDeleteConfirm = false },
        title = { Text("删除标签「${editing!!.name}」？") },
        text = { Text("所有事件中该标签的关联将被清除，且无法恢复。") },
        confirmButton = {
            TextButton(onClick = {
                showDeleteConfirm = false
                scope.launch {
                    onDelete(editing!!)
                    onDismiss()
                }
            }) { Text("删除", color = androidx.compose.ui.graphics.Color(0xFFEF4444)) }
        },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
        }
    )
}
```

`editing!!` is safe — the "删除" button is inside `if (editing != null)` (line 112). Add imports: `androidx.compose.material3.AlertDialog`, `androidx.compose.material3.TextButton`.

Note: `0xFFEF4444` is the `Error`/`Primary` red from `AppColors.kt:11`. You may import `Error` from `com.shijiben.ui.theme` and use `color = Error` instead of the literal — prefer that for consistency. (The editor sheets import from `ui.theme` already — check the existing imports and add `Error` if not present.)

### Step 3: Build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

## Test plan

Pure UI gating change; no new unit test. Existing tests must remain green:
- `./gradlew :app:testDebugUnitTest` → all pass (NoteRepositoryTest, TagRepositoryTest unaffected — the ViewModel delete logic is unchanged).

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n "showDeleteConfirm" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` returns matches (state + dialog)
- [ ] `grep -n "showDeleteConfirm" app/src/main/java/com/shijiben/feature/tags/TagEditorSheet.kt` returns matches (state + dialog)
- [ ] `grep -n "AlertDialog" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` and `.../tags/TagEditorSheet.kt` each return a match
- [ ] On device: tapping "删除" in either editor shows the dialog; only "删除" in the dialog actually deletes; "取消" returns to the editor without deleting
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 008 updated to DONE

## STOP conditions

Stop and report back if:

- The excerpts at `NoteEditorSheet.kt:84-95` or `TagEditorSheet.kt:112-123` don't match live code (drift).
- `AlertDialog` or `TextButton` aren't resolvable from `androidx.compose.material3` (they should be — Compose Material3 is a dependency).
- The "删除" button is found to render even when `editing == null` in either sheet (would break the `editing!!` safety assumption) — if so, STOP and guard the dialog on `editing != null` instead.

## Maintenance notes

- **Undo deferred**: a Snackbar-based undo (delete → show "已删除 · 撤销" for 5s → restore on tap) is the more forgiving UX and aligns better with the philosophy. It's deferred because it requires: (1) a soft-delete or snapshot in the repository, (2) a Snackbar host wired into `NotesScreen`/`TagsScreen`, (3) restore logic. That's an L-effort follow-up; this confirmation dialog is the S-effort safety net that lands first. Tag undo is especially tricky because of the cascade (`clearTagReference`) — restoration would need to re-link events, which isn't currently snapshotted. If undo is pursued later, snapshot the affected event IDs before clearing.
- Reviewer: on device, open a tag used by several events, delete it, confirm the dialog warns about the cascade; confirm events still exist after (just untagged).
- The `0xFFEF4444` red literal should ideally use the `Error` theme token; if the executor imports `Error`, prefer it. Keep it consistent across both sheets.
