## 1. Visual Theme Setup

- [x] 1.1 Add design tokens in `ui/theme/Color.kt` for card background overlays and active state gradient stops
- [x] 1.2 Verify theme configurations in `ui/theme/Theme.kt` for light and dark color schemes

## 2. Active Event Hero Card

- [x] 2.1 Implement the `ActiveEventCard` component displaying in-progress events at the top of the homepage
- [x] 2.2 Add dynamic breathing/pulse animation to the `ActiveEventCard` indicating live timer tracking
- [x] 2.3 Add linear gradient background and stop action button on the card

## 3. Card-Grouped Lists

- [x] 3.1 Refactor completed list items inside `HomeScreen.kt` to be wrapped in a rounded card-like container
- [x] 3.2 Refactor pending list items inside `HomeScreen.kt` to be wrapped in a rounded card-like container
- [x] 3.3 Add touch-press scale animation modifier (`Modifier.graphicsLayer`) to interactive items and card items

## 4. Horizontal Recommendations & Floating Input

- [x] 4.1 Convert recommendation items to horizontal scrollable Chip elements with **no icons** using `LazyRow`
- [x] 4.2 Reposition and style the quick-add input bar into a bottom-floating capsule shape with proper text field behavior
- [x] 4.3 Add bottom content padding (`80.dp`) to the list scroll view so the floating input capsule doesn't block lists

## 5. Animations & Build Verification

- [x] 5.1 Add list transition animations (`Modifier.animateItem()`) for adding, completing, and deleting items
- [x] 5.2 Build the app using `./gradlew assembleDebug` to verify no compilation errors occur
