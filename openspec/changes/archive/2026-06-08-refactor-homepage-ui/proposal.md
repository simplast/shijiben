## Why

The current home screen uses a flat, linear, line-frame layout with thin dividers. While functional, it lacks visual hierarchy, dynamic interaction, and modern aesthetics. Active in-progress events are easily lost in the pending list, and recommendation items clutter the screen vertically. Refactoring the UI to be card-grouped, adding a focal active event card, and introducing smooth Compose animations will make the app feel alive ("灵动") and significantly more polished.

## What Changes

- **Active Event Hero Card**: When an event is in progress, display a prominent, beautifully styled card at the top with a gradient background, breathing animation, large timer, and direct stop/pause actions.
- **Card-Grouped Layout**: Group pending and completed lists inside rounded, soft-background cards instead of separating them with flat grey divider lines.
- **Horizontal Recommendation Chips**: Render recommendation items as horizontal scrollable text-only chips (no icons) below/near lists rather than vertical rows that push content down.
- **Floating Capsule Input**: Redesign the quick-add line into a bottom-floating capsule input bar with soft shadows and interactive focus states.
- **Lively Transitions & Touch Effects**: Implement slide/fade layout animations for adding, completing, and deleting tasks, and add press-down scale-feedback effects for list items.

## Capabilities

### New Capabilities

*(None)*

### Modified Capabilities

- `minimal-line-home`: Replaces the linear line-frame homepage with a card-grouped, animated layout, adding the Hero Card for active timers and the floating input capsule.
- `algo-recommendation`: Changes how recommendation items are rendered from vertical rows to a horizontal chip bar, and specifies that they should not include icons.

## Impact

- `ui/screens/HomeScreen.kt`: Deep refactor of the screen layout, section composables, list item components, and interaction animations.
- `ui/theme/Color.kt` and `ui/theme/Theme.kt`: Introduction of visual tokens for gradient backgrounds, card containers, and soft shadows.
- `ui/theme/Type.kt`: Check and refine font styles if necessary.
