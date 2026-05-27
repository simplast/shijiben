## 1. Data and ViewModel behavior

- [x] 1.1 Add repository support so `PENDING` events preserve their selected `dayKey` instead of deriving it from `startTimeMillis = 0`
- [x] 1.2 Change quick creation to add a `PENDING` event by name only for the selected day
- [x] 1.3 Add ViewModel action to start a pending event and stop any current active event first
- [x] 1.4 Add ViewModel action to add an event from a frequent-name shortcut
- [x] 1.5 Change frequent-name DAO query to order by usage frequency and limit to 10

## 2. Homepage layout

- [x] 2.1 Rebuild `HomeScreen.kt` as a minimal line-frame layout with no cards, no FAB, no bottom date bar, and no top-left app name
- [x] 2.2 Add top-left calendar icon/date control that opens `DayPickerDialog`
- [x] 2.3 Add compact name-only input line that creates pending events
- [x] 2.4 Add compact frequent-name shortcuts visible by default
- [x] 2.5 Add line-style event rows with start for pending, elapsed/stop for active, and time/duration for completed
- [x] 2.6 Preserve row tap edit/delete behavior through existing bottom-sheet editor where practical

## 3. Cleanup and verification

- [x] 3.1 Remove `AGENTS.md` from the repository working tree
- [x] 3.2 Validate OpenSpec change with `openspec validate --changes --json`
- [x] 3.3 Build with `./gradlew assembleDebug` and fix compile errors
- [x] 3.4 Install debug APK on the connected Android device
- [x] 3.5 Launch the app on the connected Android device
