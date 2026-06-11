## ADDED Requirements

### Requirement: All interactive elements SHALL have minimum 44dp touch target

Every tappable element (button, icon, row) SHALL have a minimum touch area of 44dp × 44dp.

#### Scenario: Small icon buttons meet touch target
- **WHEN** PixelIconButton is rendered with size < 44dp
- **THEN** the clickable area is expanded to 44dp minimum

#### Scenario: List item rows meet touch target
- **WHEN** a CompletedEventRow or PendingEventRow is rendered
- **THEN** the row height is at least 48dp

### Requirement: Adjacent touch targets SHALL have minimum 8dp spacing

Interactive elements next to each other SHALL have at least 8dp gap.

#### Scenario: Play and Delete buttons have spacing
- **WHEN** Play and Delete buttons are adjacent
- **THEN** there is at least 8dp gap between them

#### Scenario: Bottom bar buttons have spacing
- **WHEN** "+" and submit buttons are in the bottom bar
- **THEN** there is at least 8dp gap between them and the input field
