# minimal-line-home Specification

## Purpose
TBD - created by archiving change minimal-line-home-redesign. Update Purpose after archive.
## Requirements
### Requirement: Minimal line-frame homepage
The homepage SHALL present primary daily logging content in a compact line-frame style without event cards, elevated surfaces, FAB-first creation, or a bottom date component.

#### Scenario: Homepage opens with line-frame layout
- **WHEN** the user opens the homepage
- **THEN** the screen shows thin line-separated controls and event rows without card containers or shadows

#### Scenario: No app name in top-left
- **WHEN** the homepage top bar is displayed
- **THEN** the top-left area shows a calendar icon/date control instead of the app name

#### Scenario: Date picker from calendar icon
- **WHEN** the user taps the top-left calendar icon
- **THEN** the DayPickerDialog opens for date selection

### Requirement: Name-only event creation
The homepage SHALL allow a user to create an event by entering only a name.

#### Scenario: Add event by name
- **WHEN** the user enters a non-empty event name and confirms add
- **THEN** a new event row appears on the selected day without starting a timer

#### Scenario: Empty name is ignored
- **WHEN** the user confirms add with an empty or whitespace-only name
- **THEN** no event is created

### Requirement: Not-started events behave as todos
A newly added event SHALL be shown as a not-started todo until the user starts it.

#### Scenario: New event shows todo state
- **WHEN** an event has not been started
- **THEN** its row shows the event name and a start action, not a duration

#### Scenario: Start todo event
- **WHEN** the user taps start on a not-started event row
- **THEN** the event status becomes in-progress and timing begins from the current time

### Requirement: Active event self-timing
An in-progress event SHALL show elapsed time and a stop action on its homepage row.

#### Scenario: Active event elapsed time updates
- **WHEN** an event is in progress
- **THEN** its row shows elapsed minutes derived from current time minus start time

#### Scenario: Stop active event
- **WHEN** the user taps stop on an in-progress event row
- **THEN** the event status becomes completed and the end time is saved

### Requirement: Frequent event shortcuts
The homepage SHALL show the 10 most frequently used event names by default for quick reuse.

#### Scenario: Frequent names displayed
- **WHEN** the homepage is displayed and historical event names exist
- **THEN** up to 10 names are shown as compact shortcuts ordered by usage frequency

#### Scenario: Shortcut adds todo event
- **WHEN** the user taps a frequent-name shortcut
- **THEN** a not-started event with that name is added to the selected day

### Requirement: Compact no-scroll priority
The homepage SHALL prioritize fitting the input line, frequent shortcuts, and normal daily event rows without requiring scrolling in common cases.

#### Scenario: Common day fits primary content
- **WHEN** the selected day has a small number of events and up to 10 frequent shortcuts
- **THEN** the primary controls and rows are visible in a compact vertical layout

