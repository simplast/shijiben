## Context

The current home screen (`HomeScreen.kt`) uses a line-frame layout with thin dividers and flat text rows. To make the UI more lively ("灵动") and structured, we will refactor it into a modern card-grouped design with dynamic transitions, horizontal text-only chips, a floating input bar, and a pulsing top-level Active Event Hero Card.

## Goals / Non-Goals

**Goals:**
- Add a top-level Active Event Hero Card showing timing in-progress events with a gradient background, breathing indicator, and direct control buttons.
- Group completed and pending lists inside rounded, soft-background card container blocks.
- Redesign the vertical recommendation items into a horizontal scrollable list of text Chips (with no icons).
- Relocate and restyle the quick-add text bar as a floating or bottom-anchored capsule row.
- Integrate fluid animations (`Modifier.animateItem()`) for list transitions and interactive touch feedback (press-down scaling).

**Non-Goals:**
- Modifying Room database models (`EventEntity`), database versions, or repository classes.
- Redesigning the full editing bottom sheet (`EventEditorScreen.kt`) or statistics page.
- Changing the recommendation generation algorithm in the ViewModel.

## Decisions

### Decision 1: Active Event Card Placement & Styling
- **Choice**: Display the active event (`IN_PROGRESS`) in a prominent top-level card (above the main lists) rather than in the lists. Style it with a linear gradient (`PrimaryIndigo` to `AccentPurple`) and an infinite pulsing alpha/scale indicator.
- **Rationale**: An active timer is the main interaction point for real-time tracking; drawing it out of the general lists prevents it from getting lost and visualizes active time tracking.
- **Alternative considered**: Keep the active event in the pending list but apply a colored background. Rejected because it fails to establish a strong focal point.

### Decision 2: Recommendation Chip Representation
- **Choice**: Render recommendations as a horizontal scrollable row (`LazyRow`) of text-only chips with a subtle background and border, and **no icons**.
- **Rationale**: Keeps the pending list focused on actual planned events, matches the user request to omit icons, and makes selecting recommendations a quick swipe-and-tap action.
- **Alternative considered**: Maintain vertical rows. Rejected because they take up too much vertical space.

### Decision 3: Card-Grouped Section Lists
- **Choice**: Group rows in the "Pending" and "Completed" sections inside unified card containers (`Card` or `Box` with `RoundedCornerShape(16.dp)` and a soft surface tint).
- **Rationale**: Reduces the visual clutter of multiple full-width horizontal divider lines, grouping related data logically.

### Decision 4: Bottom-Floating Quick-Add Capsule
- **Choice**: Reposition the quick-add line from the top to a bottom-anchored capsule-shaped input container (using `CircleShape`) with a soft shadow.
- **Rationale**: A bottom capsule is more reachable on mobile devices and aligns with modern app designs.
- **Alternative considered**: Keep it at the top but make it rounded. Rejected because a bottom-anchored capsule is more organic and fits the mobile thumb zone better.

### Decision 5: Press-down scaling feedback (Touch Animation)
- **Choice**: Implement a custom modifier that applies a slight scale reduction (e.g., from `1.0f` to `0.97f`) on press, animating it back to `1.0f` on release using Compose's animation APIs.
- **Rationale**: Connects touch input directly with visual animation, giving the UI a more responsive, tactile, and elastic feel.

## Risks / Trade-offs

- **[Risk]**: The bottom-floating capsule input bar might cover task list items at the bottom of the scroll view.
  - **Mitigation**: Add a generous bottom content padding (e.g., `80.dp`) to the list scroll view (`LazyColumn`) so that all list elements can scroll completely past the input bar.
- **[Risk]**: Constant breathing animation of the Active Card could cause battery drain or high CPU usage.
  - **Mitigation**: Ensure the animation uses a simple alpha sweep on a tiny dot or border, rather than constantly invalidating and redrawing complex graphics.
