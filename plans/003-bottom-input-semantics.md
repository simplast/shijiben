# Plan 003: Fix bottom quick-add input placeholder semantics

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md`.
>
> **Drift check (run first)**: `git diff --stat d74ab99..HEAD -- app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
> If this file changed since the plan was written, compare the "Current state"
> excerpt against live code; on a mismatch, treat it as a STOP condition.

## Status

- **Priority**: P1
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug (UX/mental model)
- **Planned at**: commit `d74ab99`, 2026-06-26

## Why this matters

The bottom input field's placeholder reads `"记一笔..."` (TimelineScreen.kt:177), which in this product's vocabulary means *随笔/Note* — see `NoteEditorSheet.kt:59` `"记一笔随笔"` and `MY_ORIGIN_GOAL.md`'s framing of "记一笔" as a thought/insight. But the field actually calls `viewModel.quickAddEvent(inputText.trim())` (TimelineScreen.kt:190), creating an untimed `NotStarted` **Event** on the timeline. The user types expecting to jot a thought; instead a no-time event appears in the timeline. The mental-model mismatch is the highest-friction onboarding bug on the main screen. This plan aligns the placeholder (and adds a tiny affordance) with what the field actually does: quick-add an untimed event.

## Current state

File in scope:
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — the bottom input Box (lines 166-199).

Excerpt — `TimelineScreen.kt:166-199`:
```kotlin
// 底部像素风格输入框
Box(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .background(Surface)
        .padding(horizontal = 12.dp, vertical = 8.dp)
) {
    OutlinedTextField(
        value = inputText,
        onValueChange = { inputText = it },
        placeholder = {
            Text("记一笔...", color = TextTertiary, fontWeight = FontWeight.Medium)
        },
        singleLine = true,
        shape = RoundedCornerShape(0.dp),
        textStyle = TextStyle(
            fontSize = 15.sp,
            color = TextPrimary,
            fontWeight = FontWeight.SemiBold
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(
            onDone = {
                if (inputText.isNotBlank()) {
                    viewModel.quickAddEvent(inputText.trim())
                    inputText = ""
                }
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(inputFocusRequester)
    )
}
```

`TimelineViewModel.kt:78-92` — `quickAddEvent` creates a `NotStarted` event (untimed):
```kotlin
fun quickAddEvent(title: String) {
    if (title.isBlank()) return
    viewModelScope.launch {
        val now = System.currentTimeMillis()
        eventRepository.createEvent(
            title = title.trim(),
            startTime = now,
            endTime = null,
            ...
            status = com.shijiben.data.model.EventStatus.NotStarted.value
        )
        refresh()
    }
}
```

### Vocabulary (from intent docs — use these terms)

- "事件" = Event (timed or untimed record of doing/intending something). See `docs/2026-06-22-shijiben-design.md:67`.
- "随笔" = Note (a thought/insight). See `NoteEditorSheet.kt:59` `"记一笔随笔"` and `docs/2026-06-22-shijiben-design.md:81`.
- "记一笔" colloquially leans toward 随笔 in this app's voice. The bottom field creates an **untimed event**, so its copy must say so.

### Repo conventions

- Chinese inline strings (no `strings.xml`). Match.
- 8-bit aesthetic: `RoundedCornerShape(0.dp)`, pixel colors from `ui/theme/AppColors.kt`.
- Icons used elsewhere: `Icons.Default.*` from `androidx.compose.material.icons.Icons` (already imported in this file).

## Commands you will need

| Purpose | Command | Expected on success |
|---------|---------|---------------------|
| Kotlin compile | `./gradlew :app:compileDebugKotlin` | exit 0 |
| Unit tests | `./gradlew :app:testDebugUnitTest` | exit 0 |
| Build APK | `./gradlew assembleDebug` | exit 0 |

## Scope

**In scope**:
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` (bottom input Box only)

**Out of scope**:
- `TimelineViewModel.quickAddEvent` — behavior is correct; only the UI copy is wrong.
- `NoteEditorSheet` / `NotesScreen` — the 随笔 entry is the top-bar Edit icon; do not change.
- Adding a send Icon button is optional; if added, keep it minimal (see Step 2).

## Git workflow

- Branch: `advisor/003-input-copy`
- Commit message example: `Clarify bottom quick-add input creates an untimed event`.

## Steps

### Step 1: Replace the misleading placeholder

In `TimelineScreen.kt`, change the placeholder text at line 177 from `"记一笔..."` to a copy that accurately describes creating an untimed event. Use:

```kotlin
placeholder = {
    Text("记一件事（无时间）...", color = TextTertiary, fontWeight = FontWeight.Medium)
}
```

Rationale: "记一件事" matches the Event vocabulary ("事件" = a thing done/intended); "（无时间）" sets the expectation that no time range is attached (distinguishing it from the `+` button which opens the full RecordingSheet with time sliders). This is truthful and on-philosophy (`MY_ORIGIN_GOAL.md`: "记录生活").

### Step 2 (optional but recommended): Add a trailing submit icon button

The current field relies solely on the IME `Done` action, which varies by keyboard and isn't obviously a "submit". Add a trailing pixel-style submit icon inside the `OutlinedTextField` so the affordance is unambiguous. Use `Icons.Default.Add` (already imported via the file's existing `Icons` usage — confirm `androidx.compose.material.icons.filled.Add` is imported; if not, add it).

```kotlin
trailingIcon = {
    IconButton(onClick = {
        if (inputText.isNotBlank()) {
            viewModel.quickAddEvent(inputText.trim())
            inputText = ""
        }
    }) {
        Icon(
            Icons.Default.Add,
            contentDescription = "添加",
            tint = Primary
        )
    }
}
```

Keep the existing `keyboardActions` `onDone` block (so both IME-done and the icon submit). `IconButton` and `Icon` are already imported in this file (used in the top bar).

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 3: Build & test

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew :app:testDebugUnitTest` → exit 0, all pass
- `./gradlew assembleDebug` → exit 0

## Test plan

No unit test applies (pure UI copy + trailing icon). Existing tests must remain green:
- `./gradlew :app:testDebugUnitTest` → all pass.

## Done criteria

ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] `grep -n '"记一笔...' app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns no matches (the misleading placeholder is gone)
- [ ] `grep -n '记一件事' app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` returns one match
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 003 updated to DONE

## STOP conditions

Stop and report back if:

- The excerpt at `TimelineScreen.kt:166-199` doesn't match live code (drift).
- `Icons.Default.Add` or `IconButton`/`Icon` aren't resolvable (they should be — both are used elsewhere in the same file at lines 117-138).
- The maintainer prefers a different copy — pause and confirm rather than guessing.

## Maintenance notes

- If a future change makes the bottom field optionally create a 随笔 instead (toggle), revisit the copy and the trailing icon — they assume event-creation only.
- Reviewer: on device, confirm the placeholder reads correctly and the trailing + submits + clears the field.
- Deferred: a true "记一笔随笔" shortcut from the timeline (without navigating to NotesScreen) is a V2 feature, out of scope here.
