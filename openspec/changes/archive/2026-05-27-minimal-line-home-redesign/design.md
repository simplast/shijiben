## Context

The homepage currently reserves space for a top title, bottom date bar, FAB, cards, and bottom-sheet editing. That structure is too heavy for the desired product direction: a compact daily ledger where the homepage itself is the capture surface.

Events currently only support `IN_PROGRESS` and `COMPLETED`. To represent a newly added name that has not been started yet, the app needs a persistent not-started status. This is a schema-compatible status value change because `status` is stored as text; no new column is required.

## Goals / Non-Goals

**Goals:**
- Make the homepage a minimal line-frame surface: no event cards, no title branding, no bottom date bar.
- Put date selection in the top-left calendar icon.
- Let users add an event by name only; the created event appears as a not-started todo row.
- Let each not-started row start timing; once active, it self-times and can stop.
- Show the 10 most frequent event names by default as compact reuse shortcuts.
- Keep primary content visible with minimal scrolling.

**Non-Goals:**
- No project-management fields: no priority, due date, subtasks, labels, or recurring tasks.
- No changes to review/statistics screens.
- No new dependencies.
- No destructive database migration.

## Decisions

### 1. Use `PENDING` as the not-started status

A newly added event is persisted with `status = "PENDING"`, `startTimeMillis = 0`, `endTimeMillis = 0`, and the selected day key. This keeps event names visible without inventing start/end time before the user actually starts.

When the user taps start, the event is updated to `IN_PROGRESS`, `startTimeMillis = now`, `endTimeMillis = now`, and `dayKey` derived from the actual start time. Any existing active event is stopped first to preserve the current single-active-event model.

### 2. Preserve Room schema version unless validation requires a status-value migration

`status` is already a `TEXT NOT NULL` column, so introducing `PENDING` does not require a new column. Existing rows remain valid. No database version bump is needed unless compile/runtime validation reveals a schema mismatch.

### 3. Replace HomeScreen layout rather than incrementally patch cards

The homepage will be rebuilt around three compact zones:
1. top line: calendar icon/date controls + utility icons;
2. input line: name field + add action;
3. content: frequent-name chips and line rows for today’s events.

Visual design uses thin dividers, small type, and icon/text rhythm. Cards, shadows, elevated surfaces, and FAB are removed from the homepage.

### 4. Keep the editor sheet available for row tap edits

Rows still open the existing editor bottom sheet when tapped outside the primary start/stop action. This avoids removing established edit/delete behavior while keeping creation simpler.

## Risks / Trade-offs

| Risk | Mitigation |
|------|------------|
| `PENDING` rows with zero times could affect statistics/export | Stats already filter completed events for totals; export will show status. Display code must handle pending without formatting zero as a real time. |
| Existing repository derives dayKey from start time and would overwrite pending day | Add repository upsert logic to preserve supplied dayKey for `PENDING` events. |
| Compact controls may feel cramped | Keep icon buttons at normal touch targets; reduce visual chrome, not hit areas. |
| Home still may scroll with many events | Optimize row height and show frequent names in a compact flow; scrolling remains possible for overflow. |

## Migration Plan

1. Add `PENDING` handling in ViewModel/repository/display logic.
2. Rebuild HomeScreen into a line-frame layout.
3. Validate with `./gradlew assembleDebug`.
4. Install debug APK on connected device.

Rollback: revert `HomeScreen.kt`, ViewModel/repository/DAO changes, and the OpenSpec change directory.
