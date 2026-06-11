## ADDED Requirements

### Requirement: Project SHALL include Press Start 2P font file

A Press Start 2P .ttf font file SHALL be placed at `app/src/main/res/font/pressstart2p.ttf`.

#### Scenario: Font file exists
- **WHEN** the app is built
- **THEN** the Press Start 2P font is available in resources

### Requirement: PixelDisplay style SHALL use Press Start 2P font

The `PixelDisplay` TextStyle in Type.kt SHALL use `FontFamily(Font(R.font.pressstart2p))` instead of `FontFamily.Monospace`.

#### Scenario: Overview time number uses pixel font
- **WHEN** the TodayOverviewCard displays the total minutes
- **THEN** the number renders in Press Start 2P font

#### Scenario: Active event timer uses pixel font
- **WHEN** the ActiveEventCard displays the elapsed time
- **THEN** the timer renders in Press Start 2P font

### Requirement: PixelLabel style SHALL use Press Start 2P font

The `PixelLabel` TextStyle in Type.kt SHALL use `FontFamily(Font(R.font.pressstart2p))` instead of `FontFamily.Monospace`.

#### Scenario: Section headers use pixel font
- **WHEN** PixelSectionHeader displays "COMPLETED" or "PENDING"
- **THEN** the text renders in Press Start 2P font

#### Scenario: Badge text uses pixel font
- **WHEN** PixelBadge displays text like "✓ 5 已完成"
- **THEN** the text renders in Press Start 2P font

### Requirement: Chinese text SHALL use system default font

Chinese characters in body text, event names, and labels SHALL continue to use `FontFamily.Default` for readability.

#### Scenario: Event name displays in system font
- **WHEN** an event name contains Chinese characters
- **THEN** the text renders in the system default font

#### Scenario: Placeholder text displays in system font
- **WHEN** the input field shows placeholder text
- **THEN** the Chinese placeholder renders in the system default font
