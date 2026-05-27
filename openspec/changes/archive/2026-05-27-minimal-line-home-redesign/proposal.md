## Why

The current homepage still behaves like a small task manager wrapped in cards: too much chrome, too much scrolling, and too much friction before an event can exist. The homepage should feel like an extremely compact line-frame daily ledger: event names first, timing only when the user explicitly starts something.

## What Changes

- Redesign the homepage into a minimal line-frame interface: no event cards, no heavy surfaces, hierarchy expressed through thin lines, spacing, and type.
- Replace the top-left app name with a calendar icon that opens date selection; the bottom date component is removed.
- Change event creation: adding an event only requires a name. Newly added events appear on the homepage as not-started items.
- Treat not-started events as todos. Event rows provide a start action; once started, the row self-times and can be stopped.
- Show the 10 most frequently used event names by default on the homepage for quick reuse, while preserving the selected day’s event list.
- Optimize the homepage to show all primary content without scrolling in common cases.
- Remove repository `AGENTS.md`; OpenSpec project configuration lives under `openspec/config.yaml`.

## Capabilities

### New Capabilities
- `minimal-line-home`: Homepage layout, event creation, todo/start timing behavior, and frequent-event shortcuts for the minimal line-frame design.

### Modified Capabilities

## Impact

- **Affected files**: `HomeScreen.kt`, `EventViewModel.kt`, `EventDao.kt`, `EventRepository.kt`, `AGENTS.md`.
- **Data model**: likely adds a not-started/todo status or equivalent representation. If persisted schema changes are needed, increment Room database version and add a real migration.
- **No new dependencies**.
- **User-visible strings remain Chinese**.
