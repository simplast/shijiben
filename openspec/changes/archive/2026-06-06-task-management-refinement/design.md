# Design: Task Management Refinement

## Architecture Overview

The system will use a centralized "Startup Sync" logic in the `EventViewModel` to handle task movement, while the view-specific filtering will be handled in the `StateFlow` transformations within the ViewModel.

## Component Design

### 1. Data Layer (Room)
- **New Query:** `movePendingTasksToDate(todayKey: String)`
  - SQL: `UPDATE events SET dayKey = :todayKey WHERE status = 'PENDING' AND dayKey < :todayKey`
- **Rationale:** Bulk update is more efficient than individual updates. We only move `PENDING` tasks from the past. `IN_PROGRESS` tasks are left as-is (though they will be hidden in the past view per filtering logic).

### 2. ViewModel Logic (EventViewModel)
- **Task Filtering:**
  - `pendingEventsForSelectedDay`: Update transformation to filter out `PENDING` items if `datePerspective == PAST`.
- **Recommendation Logic:**
  - `recommendedEventNames`: Update transformation to emit `emptyList()` if `selectedDate != LocalDate.now()`.
- **Startup Sync:**
  - `init` block calls `moveUnfinishedTasksToToday()`.
  - `moveUnfinishedTasksToToday()` runs a background coroutine to call `repository.movePendingTasksToDate(todayKey)`.

### 3. UI Layer (HomeScreen)
- **Simplification:** Remove explicit `datePerspective != PAST` checks for recommendations, as the ViewModel will now handle this.
- **Visual Consistency:** The UI will naturally reflect the filtered data from the ViewModel flows.

## Logic Flow

```
User Opens App
      │
      ▼
EventViewModel Init
      │
      ├─▶ movePendingTasksToToday()
      │         │
      │         └─▶ Repo.movePendingTasksToDate(today)
      │                   │
      │                   └─▶ SQL Update: PENDING & Past -> Today
      │
      └─▶ StateFlows Initialized
                │
                ├─▶ pendingEventsForSelectedDay
                │         (Filtered by Perspective)
                │
                └─▶ recommendedEventNames
                          (Filtered by Today)
```

## Considerations
- **Orphaned IN_PROGRESS:** If a task was left `IN_PROGRESS` yesterday, it stays on yesterday but is hidden. We could choose to move it too, but the user specifically asked for "unfinished tasks" (待办), which usually maps to `PENDING`.
- **Future Tasks:** Adding a task in the future uses the selected date's `dayKey`. The sync logic `dayKey < todayKey` ensures future tasks are never moved.
