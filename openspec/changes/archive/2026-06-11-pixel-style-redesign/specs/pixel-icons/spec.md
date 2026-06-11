## ADDED Requirements

### Requirement: Pixel icons SHALL use 8x8 grid pixel block rendering

All icons in PixelIcons.kt SHALL be rendered as 8x8 pixel grids using Canvas drawRect for each pixel block. Icons SHALL NOT use drawLine or drawPath for smooth curves.

#### Scenario: Play icon renders as pixel blocks
- **WHEN** PixelPlayIcon is rendered
- **THEN** the icon displays as a right-pointing triangle made of 8x8 pixel blocks

#### Scenario: Stop icon renders as pixel blocks
- **WHEN** PixelStopIcon is rendered
- **THEN** the icon displays as a solid square made of 8x8 pixel blocks

#### Scenario: Close icon renders as pixel blocks
- **WHEN** PixelCloseIcon is rendered
- **THEN** the icon displays as an X shape made of 8x8 pixel blocks

#### Scenario: Check icon renders as pixel blocks
- **WHEN** PixelCheckIcon is rendered
- **THEN** the icon displays as a checkmark made of 8x8 pixel blocks

#### Scenario: Add icon renders as pixel blocks
- **WHEN** PixelAddIcon is rendered
- **THEN** the icon displays as a plus sign made of 8x8 pixel blocks

### Requirement: Each icon SHALL be defined as an 8x8 Boolean array

Each icon function SHALL contain a hardcoded 8x8 Boolean array where true = filled pixel, false = empty pixel.

#### Scenario: Icon data is compact
- **WHEN** an icon function is defined
- **THEN** it contains exactly 64 Boolean values (8 rows × 8 columns)

### Requirement: Icons SHALL scale to any size while maintaining pixel crispness

Icons SHALL use `Modifier.size(size)` and the Canvas SHALL draw each pixel block at `size / 8` width and height.

#### Scenario: Icon scales to 16dp
- **WHEN** PixelPlayIcon is rendered with size = 16.dp
- **THEN** each pixel block is 2dp × 2dp

#### Scenario: Icon scales to 32dp
- **WHEN** PixelPlayIcon is rendered with size = 32.dp
- **THEN** each pixel block is 4dp × 4dp
