# Plan 006: Align RecordingSheet button labels with the rest of the app (取消/保存)

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt`
> If this file changed since the plan was written, compare the "Current state"
> excerpt against live code; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none (coordinate with plan 002 which also touches `RecordingSheet.kt`)
- **Category**: tech-debt (UI consistency)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

`RecordingSheet` uses `"No"` / `"Yes"` for its action buttons (RecordingSheet.kt:96,103), while the sibling sheets use consistent Chinese labels: `NoteEditorSheet` uses `"取消"` / `"保存"` / `"删除"` (NoteEditorSheet.kt:86-110) and `TagEditorSheet` uses the same (TagEditorSheet.kt:113-138). "Yes/No" is ambiguous (Yes to what?) and breaks the Chinese-language consistency of the app. This plan relabels to `"取消"` / `"保存"` to match the established pattern.

## Current state

File in scope:
- `app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` — the action button Row (lines 86-113).

Excerpt — `RecordingSheet.kt:86-113`:
```kotlin
// 操作按钮
Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
) {
    TextButton(
        onClick = onDismiss,
        modifier = Modifier.weight(1f).height(48.dp)
    ) {
        Text(
            text = "No",
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = TextSecondary
        )
    }
    PixelButton(
        text = "Yes",
        onClick = {
            scope.launch {
                val ok = viewModel.save(viewingDate)
                if (ok) onSaved()
            }
        },
        modifier = Modifier.weight(1f).height(48.dp),
        backgroundColor = Primary
    )
}
Spacer(Modifier.height(8.dp))
```

For reference, the established pattern — `NoteEditorSheet.kt:96-110`:
```kotlin
PixelOutlinedButton(
    text = "取消",
    onClick = onDismiss,
    modifier = Modifier.weight(1f).height(48.dp)
)
PixelButton(
    text = "保存",
    onClick = { ... },
    modifier = Modifier.weight(1f).height(48.dp)
)
```

### Repo conventions

- Sibling sheets use `PixelOutlinedButton` for the cancel/dismiss action (outline style = secondary) and `PixelButton` for the primary save action. The current `RecordingSheet` uses a plain `TextButton` for "No", which is also stylistically off. This plan aligns both the label AND the component to the sibling pattern.
- `PixelOutlinedButton` and `PixelButton` are from `com.shijiben.ui.theme.*` (already wildcard-imported at RecordingSheet.kt:25).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` (the action button Row only)

**Out of scope**:
- `NoteEditorSheet.kt` / `TagEditorSheet.kt` — they are the reference pattern; do not change.
- Adding a "删除" button here — design doc forbids it (`docs/2026-06-22-shijiben-design.md:149`); event delete is list-level (plan 001).
- `TextButton` import — leave it if used elsewhere in the file; check and remove only if now unused.

## Git workflow

- Branch: `advisor/006-sheet-labels`
- Commit message example: `Align RecordingSheet buttons to 取消/保存 pattern`.

## Steps

### Step 1: Replace the TextButton "No" with PixelOutlinedButton "取消"

In `RecordingSheet.kt`, replace the `TextButton { Text("No", ...) }` block (lines 91-101) with:

```kotlin
PixelOutlinedButton(
    text = "取消",
    onClick = onDismiss,
    modifier = Modifier.weight(1f).height(48.dp)
)
```

This matches `NoteEditorSheet.kt:96-100` exactly (component, text, onClick, modifier).

### Step 2: Replace the PixelButton "Yes" text with "保存"

Change the `PixelButton(text = "Yes", ...)` (line 103) to `text = "保存"`. Keep the `onClick`, `modifier`, and `backgroundColor = Primary` unchanged. Result:

```kotlin
PixelButton(
    text = "保存",
    onClick = {
        scope.launch {
            val ok = viewModel.save(viewingDate)
            if (ok) onSaved()
        }
    },
    modifier = Modifier.weight(1f).height(48.dp),
    backgroundColor = Primary
)
```

### Step 3: Clean up unused imports if applicable

After Step 1, `TextButton` may no longer be used in this file. Check:

**Verify**: `grep -n "TextButton" app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` → if no matches remain (other than the import line), remove the `import androidx.compose.material3.TextButton` line. If `TextButton` is still referenced elsewhere, leave the import.

### Step 4: Build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

## Test plan

Pure UI label/component change; no unit test. Existing tests must remain green:
- `./gradlew :app:testDebugUnitTest` → all pass (RecordingViewModelTest unaffected).

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n '"Yes"\|"No"' app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` returns no matches
- [ ] `grep -n '"取消"\|"保存"' app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` returns one match each
- [ ] `grep -n "PixelOutlinedButton" app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` returns one match (the cancel button)
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 006 updated to DONE

## STOP conditions

Stop and report back if:

- The excerpt at `RecordingSheet.kt:86-113` doesn't match live code (drift — e.g. plan 002 already touched this file).
- `PixelOutlinedButton` is not importable from `com.shijiben.ui.theme.*` (it is — `PixelComponents.kt:71`; verify the wildcard import at line 25).
- The sibling-sheet pattern (`NoteEditorSheet.kt:96-110`) has changed and no longer uses `PixelOutlinedButton`/`PixelButton` — re-align to whatever the current shared pattern is.

## Maintenance notes

- **File-overlap with plan 002**: plan 002 modifies the `TimeRangeSlider(...)` call (lines 78-84) in this same file. This plan modifies the button Row (lines 86-113). The two sections are disjoint; execute in either order but re-run the drift check. If executing both, the `RecordingSheet.kt` import block may need both `TextButton` removal (this plan) and no additions from 002 — straightforward.
- Reviewer: on device, open the new-event sheet, confirm "取消" is an outlined button and "保存" is the filled primary; visually compare side-by-side with the Note editor sheet.
- Deferred: the design doc's "Yes/No" wording (docs/2026-06-22-shijiben-design.md:148 lists "Yes / No 按钮") is now superseded by this consistency change. If the design doc is treated as authoritative, update its line 148 too — but that's a docs change, optional here.
