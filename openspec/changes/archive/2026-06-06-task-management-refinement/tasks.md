# Tasks: Task Management Refinement

## Data Layer
- [x] Add `movePendingTasksToDate` query to `EventDao`.
- [x] Add `movePendingTasksToDate` method to `EventRepository`.

## ViewModel
- [x] Implement `moveUnfinishedTasksToToday()` in `EventViewModel`.
- [x] Call `moveUnfinishedTasksToToday()` in `EventViewModel.init`.
- [x] Update `pendingEventsForSelectedDay` flow to filter out PENDING tasks in PAST perspective.
- [x] Update `recommendedEventNames` flow to only emit values in TODAY perspective.

## UI
- [x] Simplify `PendingSection` in `HomeScreen.kt` by removing redundant perspective checks (let VM handle it).
- [x] Verify `QuickNameLine` and `EmptyLine` behaviors align with new requirements.

## Verification
- [x] Manually verify past view hides pending tasks.
- [x] Manually verify future view shows planned tasks but no recommendations.
- [x] Manually verify recommendations appear only on today.
- [x] Simulate app start with past pending tasks and verify they move to today.
