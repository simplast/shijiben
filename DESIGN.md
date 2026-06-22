# DESIGN.md — 事记本 · 8-Bit Pixel Design System

> 项目类型：移动端个人时间管理 App（事件记录 + 时间追踪）
> 设计哲学：True 8-bit Retro Pixel — 像素虚线边框 × 纯色填充 × 帧式动画

---

## 1. Color Palette — 16 Colors

Inspired by retro handhelds (GBA/NES). Tight, constrained. No alpha variants, no legacy.

| Token | HEX | Role |
|-------|-----|------|
| `PixelBlack` | #1A1A2E | Text, main borders |
| `PixelCream` | #FFF8E1 | Page background, surfaces |
| `PixelSky` | #6BC5F5 | Overview hero card |
| `PixelPink` | #FF6B9D | Active/in-progress card |
| `PixelCoral` | #FF8A6B | Completed badge, delete actions |
| `PixelLavender` | #C4B5E0 | Pending badge, input border |
| `PixelTeal` | #4DC9B8 | Completed section, success |
| `PixelAmber` | #FFB347 | Pending section border, warnings |
| `PixelMint` | #E8F8F0 | Completed card background |
| `PixelYellow` | #FFD93D | Stars, blinking indicator, accents |
| `PixelGray` | #9A8E7E | Muted text, timestamps |
| `PixelGrayLight` | #F5EEDF | Dividers, hairline |
| `PixelRed` | #E53935 | Destructive actions |
| `PixelGreen` | #66BB6A | Completion indicators |

### Convenience Aliases

| Alias | Points to |
|-------|-----------|
| `PixelBg` | `PixelCream` |
| `PixelBorder` | `PixelBlack` |
| `PixelText` | `PixelBlack` |
| `PixelTextMuted` | `PixelGray` |
| `PixelDivider` | `PixelGrayLight` |

### Module Color Assignment

| Module | Background | Border | Accent |
|--------|-----------|--------|--------|
| Overview Card | `PixelSky` | `PixelBorder` (3dp) | `PixelCoral` + `PixelLavender` badges |
| Active Card | `PixelPink` | `PixelBorder` (3dp) | `PixelYellow` blink + `PixelTeal` stop |
| Completed Section | `PixelMint` | `PixelTeal` (2dp) | Rotating color bars |
| Pending Section | `PixelCream` | `PixelAmber` (2dp) | `PixelYellow` left edge |
| Inputs | `PixelCream` | `PixelTeal`/`PixelGrayLight` (1dp) | — |
| Buttons | Various | `PixelBorder` (1dp) | — |

---

## 2. Border System — Pixel Dashed Lines

True pixel-dashed borders with alternating colored/transparent blocks. Three levels:

| Level | Width | Dash | Gap | Use |
|-------|-------|------|-----|-----|
| Primary | 3dp | 4dp | 2dp | Hero cards (overview, active) |
| Secondary | 2dp | 3dp | 2dp | Content cards (completed, pending) |
| Tertiary | 1dp | 2dp | 2dp | Inline (buttons, badges, inputs) |

---

## 3. Typography — 5 Styles

| Token | Size | Font | Use |
|-------|------|------|-----|
| `PixelDisplay` | 32sp | Press Start 2P | Hero numbers (timers, stats) |
| `PixelLabel` | 10sp | Press Start 2P | Section headers, badges |
| `PixelBody` | 16sp | System default | Event names, primary text |
| `PixelBodySmall` | 12sp | System default | Meta, timestamps |
| `PixelCaption` | 10sp | System default | Hints, placeholders |

---

## 4. Icons — 8×8 Pixel Grid

All icons are drawn as 8×8 Boolean grids. Available icons:
- `PixelPlayIcon` — Right-pointing triangle
- `PixelStopIcon` — Solid square
- `PixelCloseIcon` — X shape
- `PixelCheckIcon` — Tick mark
- `PixelAddIcon` — Plus sign
- `PixelMoreIcon` — Three dots (horizontal)
- `PixelPauseIcon` — Two vertical bars
- `PixelRefreshIcon` — Circular arrow
- `PixelCalendarIcon` — Calendar
- `PixelStarIcon` — 4-point star
- `PixelStatsIcon` — Bar chart
- `PixelClockIcon` — Clock (new)
- `PixelTrashIcon` — Trash can (new)
- `PixelEditIcon` — Pencil (new)

---

## 5. Components

| Component | File | Description |
|-----------|------|-------------|
| `PixelCard` | `PixelComponents.kt` | Container with pixel border + fill |
| `PixelButton` | `PixelComponents.kt` | Clickable box with press animation |
| `PixelIconButton` | `PixelComponents.kt` | Icon-only button |
| `PixelBadge` | `PixelComponents.kt` | Small tag/chip |
| `PixelSectionHeader` | `PixelComponents.kt` | Colored bar + label |
| `PixelInput` | `PixelComponents.kt` | Text field with pixel border |
| `PixelBlinkIndicator` | `PixelComponents.kt` | Animated 8dp square |
| `PixelDivider` | `PixelComponents.kt` | 1dp full-width line |

---

## 6. Animations

- **Blink**: `keyframes` 0→1→0 instant cuts at 800ms
- **Press**: Scale to 0.94x, spring back
- **Fade in**: 3 discrete alpha steps (0 → 0.33 → 0.66 → 1.0)
- **Number hop**: Instant digit change, no smooth interpolation

---

## 7. Layout — 8dp Grid

All spacing snaps to multiples of 8dp: 8, 16, 24, 32.

- Page padding: 16dp horizontal
- Card padding: 16dp internal
- Section gaps: 16dp

---

## 8. Screen Architecture

| Screen | Files |
|--------|-------|
| Home | `HomeScreen.kt` + `TodayOverviewCard.kt` + `ActiveEventCard.kt` + `CompletedSection.kt` + `PendingSection.kt` + `TopBar.kt` + `QuickNameLine.kt` + `SharedComponents.kt` |
| Weekly Review | `WeeklyReviewScreen.kt` |
| Monthly Review | `MonthlyReviewScreen.kt` |
| Event Editor | `EventEditorScreen.kt` |
| Date Picker | `DayPickerDialog.kt` |

---

## 9. Theme

```kotlin
ShijibenTheme(darkTheme: Boolean = false) { ... }
```

Light mode: `PixelCream` background, `PixelBlack` text.
Dark mode: `PixelBlack` background, `PixelCream` text (defined, ready to use).

---

## 10. Do's and Don'ts

### Do's
1. Use pixel-dashed borders (`pixelBorderPrimary/Secondary/Tertiary`)
2. Use solid color fills only (no gradients, no shadows)
3. Use the 16-color palette exclusively
4. Press Start 2P for display numbers and labels
5. System font for Chinese body text
6. Frame-based animations (instant cuts)
7. 8dp grid spacing

### Don'ts
1. No rounded corners (0dp everywhere)
2. No shadows or elevation
3. No gradients
4. No solid-line borders (use pixel-dashed)
5. No smooth animations (use frame-based)
6. No pixel font for Chinese text
7. No colors outside the 16-color palette
