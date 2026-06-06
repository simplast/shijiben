# Proposal: Task Management Refinement

## Goal
Optimize task management logic across different time perspectives (Past, Today, Future) and implement automatic task carry-over from past days.

## Scope
- **Past Perspective:** Hide pending tasks to focus on completed history.
- **Task Carry-over:** Automatically move unfinished tasks from past days to "Today" on app startup.
- **Recommendations:** Restrict the top 5 recommended tasks to the "Today" perspective only.
- **Future Perspective:** Keep planned tasks specific to their dates, avoiding premature movement.

## Core Changes
- **Database:** Add capability to bulk update task dates for pending items.
- **ViewModel:**
  - Logic to filter tasks based on date perspective.
  - Restricted recommendation calculation.
  - Startup routine to sync past pending tasks.
- **UI:** Ensure consistent display behavior across perspectives.
