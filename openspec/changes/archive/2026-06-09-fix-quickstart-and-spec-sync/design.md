## Context

`quickStartEvent` was designed as a shortcut to immediately start an event from a recommendation name. However, it has two defects that cause data inconsistency:

1. **Orphan PENDING records**: When a user adds "读书" as a PENDING event and then taps the same name in recommendations, `quickStartEvent` creates a brand-new IN_PROGRESS record instead of starting the existing PENDING one. The PENDING record remains in the database forever, polluting the pending list.
2. **Missing dayKey**: The new record is inserted with `dayKey = ""`, which makes it invisible on any date view since queries filter by `dayKey = LocalDate.now().format(...)`. Both `quickAddEvent` and `restartEvent` correctly set `dayKey` from `_selectedDate`.

Additionally, the recommendation dedup filter in `HomeScreen.PendingSection` only checks against PENDING/IN_PROGRESS events (`existingNames` derived from `events` parameter), so a recommendation whose name already exists as COMPLETED still shows up. This is confusing because tapping it would create a duplicate.

Finally, `minimal-line-home/spec.md` retains 6 requirements that describe the old rounded-corner/shadow/Outfit-font design, which directly contradict the current 8-bit pixel implementation (`homescreen-8bit-colorful/spec.md`).

## Goals / Non-Goals

**Goals:**

- Eliminate orphan PENDING records created by quick-start from recommendation.
- Ensure quick-started events are visible on today's date view.
- Prevent recommendations from suggesting names that already exist as COMPLETED events today.
- Remove the 6 outdated requirements from `minimal-line-home/spec.md` so the spec reflects the actual 8-bit pixel design.

**Non-Goals:**

- Changing the recommendation algorithm itself (ranking, scoring, selection logic).
- Modifying database schema or adding new columns.
- Refactoring `startEvent` or `quickAddEvent`; they already work correctly.
- Migrating historical records that already have `dayKey = ""`.

## Decisions

### D1: Lookup-then-delegate pattern in `quickStartEvent`

**Approach**: Before inserting, query the repository for a same-day PENDING event with the trimmed name. If found, call `startEvent(existing)` which already updates status, sets times, and preserves the original record identity. If not found, insert a new IN_PROGRESS record as today.

**Why**: This reuses the existing `startEvent` path, keeping a single code path for "transition PENDING to IN_PROGRESS." It avoids duplicating the stop-active-event and timestamp logic. The repository already has `observeEventsForDay(dayKey)` returning a Flow; a one-shot `getEventByNameAndDay(name, dayKey)` suspend query is trivial to add.

**Alternative rejected**: Deleting the orphan PENDING after insert. This loses the original record's id and any metadata, and adds a second write operation. Delegating to `startEvent` is cleaner and preserves audit trail.

### D2: Set `dayKey` to selected date, matching `quickAddEvent`

**Approach**: Use `_selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)` for the new record's `dayKey`, identical to `quickAddEvent` and `restartEvent`.

**Why**: The `eventsForSelectedDay` flow filters by `dayKey`, so an empty `dayKey` makes the event invisible. Using the selected date (which for quick-start from recommendation is always today, since recommendations only appear in TODAY perspective) ensures consistency across all three event-creation methods.

### D3: Pass `completedEvents` into `PendingSection` for dedup filtering

**Approach**: Extend the `PendingSection` composable to accept a `completedNames: Set<String>` parameter (derived from `completedEventsForSelectedDay.value.map { it.name.trim() }.toSet()`). Merge it into the existing `existingNames` set used for recommendation filtering.

**Why**: `completedEventsForSelectedDay` is already a StateFlow collected in `HomeScreen` at line 127. Passing the name set down is a single `val` extraction at the call site. This keeps the filtering logic co-located with other recommendation logic in `PendingSection` rather than splitting it across the parent composable.

**Alternative rejected**: Filtering in the ViewModel before emitting recommendations. This would couple the recommendation flow to the completed-events flow unnecessarily; the UI layer already has both pieces of state and is the natural place for presentation-level dedup.

### D4: Spec cleanup by deletion, not amendment

**Approach**: Delete the 6 outdated requirements entirely from `minimal-line-home/spec.md`:
- 圆角与间距体系 (rounded corners/spacing system)
- 暖调阴影系统 (warm shadow system)
- 排版体系 (Outfit/Inter/JetBrains Mono font system)
- 标签排版规范 (label typography with WarmGray500)
- 底部输入栏像素风样式 - 毛玻璃 clause (frosted glass clause in input bar)
- Name-only event creation - 悬浮胶囊输入栏 (floating capsule input bar) references

**Why**: These requirements describe the superseded Warm Gold / rounded design. The 8-bit pixel design is already fully specified in `homescreen-8bit-colorful/spec.md`. Amending them to mention both styles would create ambiguity about which is authoritative. Deletion makes `minimal-line-home/spec.md` the spec for structural/behavioral requirements only, while visual style is owned by the pixel spec.

## Risks / Trade-offs

**Risk: Race condition between lookup and insert in `quickStartEvent`**. If the user taps two recommendations in rapid succession, the second tap could create a duplicate before the first PENDING is consumed. Mitigation: `stopActiveEvent()` is already called first, and the coroutine runs sequentially within `viewModelScope`. The window is microseconds and the UI disables recommendations while an event is active, so this is low probability.

**Risk: `completedEvents` not yet loaded when recommendations render**. On first render, `completedEventsForSelectedDay` might emit an empty list before the DB query completes, briefly showing a recommendation that should be filtered. Mitigation: The StateFlow replays its latest value; once the DB emits, the list recomposes. The brief flash is cosmetic and self-correcting.

**Risk: Deleting spec requirements may remove scenarios that other code still references**. Mitigation: Grep confirmed no code references the deleted requirements' scenario names; they are purely documentation artifacts.

**Trade-off: Querying by name is O(n) per quick-start**. The repository filters events in memory since `observeEventsForDay` returns all events for the day. For typical daily use (< 50 events), this is negligible. A database index on `(dayKey, name)` could be added later if needed but is not warranted now.
