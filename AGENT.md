# AGENT.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**事记本 (Shijiben)** is a Kotlin-based Android application for personal time management and event tracking. It features an 8-bit pixel art design with a flat, colorful aesthetic inspired by retro gaming UIs.

## Build & Development Commands

### Building the Project
```bash
./gradlew build
```

### Running Tests
```bash
./gradlew test                    # Run all tests
./gradlew testDebugUnitTest       # Run unit tests for debug build
./gradlew connectedAndroidTest    # Run instrumented tests (requires device/emulator)
```

### Installing on Device
```bash
./gradlew installDebug            # Install debug APK
./gradlew installRelease          # Install release APK
```

### Cleaning
```bash
./gradlew clean                   # Clean build artifacts
```

## Architecture

### Tech Stack
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material3
- **Persistence**: Room 2.6.1 (SQLite)
- **Async**: Kotlin Coroutines + Flow
- **Navigation**: Compose Navigation 2.8.4
- **Build System**: Gradle 8.7.2 with Kotlin DSL

### Application Structure

**MVVM Pattern with Repository Layer**

```
MainActivity (Compose Host)
    ↓
ShijibenNavHost (Navigation)
    ↓
Screens (HomeScreen, WeeklyReviewScreen, MonthlyReviewScreen, EventEditorScreen)
    ↓
EventViewModel (State Management)
    ↓
EventRepository (Business Logic)
    ↓
EventDao / GoalDao (Data Access)
    ↓
AppDatabase (Room Database)
```

### Core Entities

**EventEntity** (`data/EventEntity.kt`)
- Represents a time-tracked event
- Status lifecycle: `PENDING` → `IN_PROGRESS` → `COMPLETED`
- Key fields: `name`, `startTimeMillis`, `endTimeMillis`, `dayKey` (yyyy-MM-dd), `status`, `category`, `note`

**GoalEntity** (`data/GoalEntity.kt`)
- Weekly time goals for specific event categories
- Scoped by `weekKey` (format: "2024-W20")
- Fields: `name`, `targetMinutes`, `weekKey`

### Key Architectural Patterns

**State Management**
- `EventViewModel` exposes `StateFlow` for all reactive state
- Selected date drives data queries via `flatMapLatest`
- Ticker flow updates active event duration every 10 seconds

**Event Lifecycle**
1. Quick add creates `PENDING` events (no start time)
2. Starting an event sets status to `IN_PROGRESS` and records `startTimeMillis`
3. Stopping sets status to `COMPLETED` and records `endTimeMillis`
4. Only one event can be `IN_PROGRESS` at a time (auto-stops previous)

**Smart Merge**
- When stopping an event, the system automatically merges consecutive same-name events with gaps < 5 minutes
- Prevents fragmentation from accidental stops/restarts

**Date Handling**
- `dayKey` (yyyy-MM-dd) is derived from `startTimeMillis` for efficient daily queries
- `PENDING` events keep their assigned `dayKey` until started
- Unfinished tasks auto-migrate to today on app startup

**Time Utilities**
- `TimeFormats.kt` centralizes all date/time formatting
- Handles Material DatePicker's UTC millis convention vs local time
- Uses `java.time` APIs (not legacy Date/Calendar)

### Database Migrations

Database is at version 6. Migrations are defined in `AppDatabase.kt`:
- v1→v2: Added `status` column
- v2→v3: Added `category` and `note` columns
- v3→v4: Added `goals` table
- v4→v6: No schema changes

## Design System

**8-bit Pixel Art Aesthetic** (see `DESIGN.md` for complete specification)

**Core Principles**
- Zero border radius on all components (`PixelShape = RoundedCornerShape(0.dp)`)
- Pixel-dashed borders via custom `pixelBorder()` modifier (no solid `BorderStroke`)
- Solid color fills only (no gradients, no shadows)
- Press Start 2P font for display text, system font for Chinese body text
- Frame-based animations (instant cuts, not smooth transitions)

**Color Palette**

Two palette systems coexist in `ui/theme/Color.kt`:

1. **Warm Gold Palette** (Material3 theme colors)
   - Primary: `PrimaryGold` (#F5C469) with soft/dark variants
   - Accents: Mint, Amber, Coral, Lavender
   - Neutrals: `WarmGray50`-`WarmGray800` (warm tones only)
   - Used by: Material3 components, backgrounds, text

2. **8-bit Pixel Palette** (custom UI components)
   - 9 vibrant colors: SkyBlue, HotPink, CoralRed, Lavender, Teal, AmberOrange, MintLight, StarYellow, DeepNavy
   - Border tokens: `PixelBorder` (DeepNavy), `PixelBorderLight` (20% alpha)
   - Used by: Custom pixel-style components (cards, badges, section headers)

**Theme Configuration** (`ui/theme/Theme.kt`)
- Light mode: `LightWarmColors` with gold primary on cream background
- Dark mode: `DarkWarmColors` with gold primary on navy background
- Currently hardcoded to light mode (dark mode defined but not active)

**Typography** (`ui/theme/Type.kt`)
- Display fonts: `FontFamily.Monospace` for pixel aesthetic (not Press Start 2P in code)
- Body fonts: `FontFamily.Default` for Chinese readability
- Special styles: `PixelDisplay` (32sp black), `PixelLabel` (10sp bold uppercase)
- `LabelUppercase`: 10sp with 0.1em letter-spacing for section headers

**Component Guidelines**
- Use `pixelBorder()` with varying widths: 3dp (prominent), 2dp (content), 1dp (inline)
- All interactive elements need press feedback (scale or background change)
- Section headers use 12dp × 3dp colored bars + uppercase PixelLabel
- Badge/tag components use直角方块 with 1dp pixel borders

## Key Files

- `app/src/main/java/com/doer/shijiben/MainActivity.kt` - Compose host
- `app/src/main/java/com/doer/shijiben/ui/EventViewModel.kt` - Business logic
- `app/src/main/java/com/doer/shijiben/data/EventRepository.kt` - Data layer
- `app/src/main/java/com/doer/shijiben/data/AppDatabase.kt` - Room database + migrations
- `app/src/main/java/com/doer/shijiben/ui/theme/` - Design system tokens
- `DESIGN.md` - Complete 8-bit design specification

## Common Development Tasks

### Adding a New Screen
1. Create screen composable in `ui/screens/`
2. Add route to `ShijibenNavHost.kt`
3. Inject `EventViewModel` via `viewModel(factory = EventViewModel.factory(repository))`

### Adding a New Event Field
1. Add field to `EventEntity.kt`
2. Create migration in `AppDatabase.kt` (increment version)
3. Update CSV/JSON export in `EventViewModel.kt` if needed
4. Update `EventEditorScreen.kt` for editing UI

### Creating Pixel-Style Components
- Always use `PixelShape` (0dp corners)
- Apply `pixelBorder(color, width)` modifier
- Use colors from `ui/theme/Color.kt`
- Follow spacing system: 2/4/8/12/16/20/24/32/40/48 dp

### Working with Time Data
- Use `TimeFormats.kt` utilities for all conversions
- `dayKey` format: yyyy-MM-dd (ISO local date)
- `weekKey` format: yyyy-Www (ISO week-based year)
- Store times as epoch milliseconds (Long)

## Important Notes

- The app is in Chinese - UI strings and date formats use Chinese locale
- Room database name: `shijiben.db`
- Package name: `com.doer.shijiben`
- Min SDK: 26 (Android 8.0), Target SDK: 36
- Uses KSP (Kotlin Symbol Processing) for Room, not KAPT
