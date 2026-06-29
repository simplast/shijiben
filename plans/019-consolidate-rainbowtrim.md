# Plan 019: Consolidate `RainbowTrim` into `PixelComponents.kt`

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 2a71f17..HEAD -- app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt app/src/main/java/com/shijiben/feature/search/SearchScreen.kt`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding. Compare the **content** of each excerpt's window, not just line numbers. If surrounding lines shifted but the target code itself is unchanged, proceed. If the target code itself has changed, treat it as a STOP condition.

## Status

- **Priority**: P3
- **Effort**: M
- **Risk**: LOW
- **Depends on**: none — but coordinate with 015/016/017 (file overlap on `TimelineScreen.kt`, `SettingsScreen.kt`, `AboutScreen.kt`). Land **after** 015/016/017 to minimize drift. See "Dependency notes".
- **Category**: tech-debt (DRY)
- **Planned at**: commit `2a71f17`, 2026-06-29

## Why this matters

The 8dp rainbow brand strip at the top of every screen is copy-pasted **7 times**: as a `private fun RainbowTrim()` in 5 files (`HeatmapScreen`, `HeatmapYearScreen`, `TimeVizScreen`, `SettingsScreen`, `AboutScreen`) and inline in 2 (`TimelineScreen`, `SearchScreen`). Each copy carries a comment literally saying "复制 8 行避免改 PixelComponents". If the brand trim ever changes (color order, height, block count), 7 files need editing in lockstep — and a future reader can't tell which copy is canonical. `PixelComponents.kt` exists exactly for this. This plan moves the strip there as a public `RainbowTrim()` and replaces all 7 copies with a call.

## Current state

The canonical body, identical in all 7 sites (content-labeled; line numbers are hints):

```
// The rainbow strip body — same in all 7 sites
val trimColors = listOf(
    Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
    Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
    Color(0xFF6366F1), Color(0xFFA855F7)
)
Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
    for (i in 0 until 80) {
        Box(
            modifier = Modifier
                .weight(1f).fillMaxHeight()
                .background(trimColors[i % 8])
        )
    }
}
```

The 7 sites:

1. **`app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt`** — does NOT yet contain `RainbowTrim`. This is where the canonical copy will live. Currently exports `PixelCard`, `PixelButton`, `PixelOutlinedButton`. Already imports `Box`, `Color`, `background`, `Modifier`, `Composable`, `dp`, `sp` — but NOT `Row`, `fillMaxWidth`, `fillMaxHeight`, `height`. Those imports must be added.

2. **`app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`** — inline, inside the top `Column`, with the comment `// 顶部 8dp 彩虹条（品牌标识，全 app 唯一保留处）`:
   ```
   // TimelineScreen.kt, inside the root Column, after the Image background
           Column(modifier = Modifier.fillMaxSize().padding(bottom = 6.dp)) {
               // 顶部 8dp 彩虹条（品牌标识，全 app 唯一保留处）
               Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                   val trimColors = listOf(
                       Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
                       Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
                       Color(0xFF6366F1), Color(0xFFA855F7)
                   )
                   for (i in 0 until 80) {
                       Box(
                           modifier = Modifier
                               .weight(1f).fillMaxHeight()
                               .background(trimColors[i % 8])
                       )
                   }
               }
   ```
   This file imports `com.shijiben.ui.theme.*` (line ~77), so `RainbowTrim()` resolves automatically once added to `PixelComponents.kt` — **no new import needed** in this file.

3. **`app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt`** — has `private fun RainbowTrim()` (lines ~114-130) with the comment `/** 顶部 8dp 彩虹条（与首页/timeviz 一致，复制 8 行避免改 PixelComponents）。 */`, called as `RainbowTrim()` at line ~60. Imports specific symbols from `com.shijiben.ui.theme` (NOT a wildcard) — **must add** `import com.shijiben.ui.theme.RainbowTrim`.

4. **`app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt`** — has `private fun RainbowTrim()` (lines ~128-144), called at line ~60. Specific theme imports — **must add** the import.

5. **`app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt`** — has `private fun RainbowTrim()` (lines ~192-208), called at line ~76. Specific theme imports — **must add** the import.

6. **`app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt`** — has `private fun RainbowTrim()` (lines ~228-244), called at line ~136. Specific theme imports — **must add** the import.

7. **`app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`** — has `private fun RainbowTrim()` (lines ~122-139), called at line ~49. Specific theme imports — **must add** the import.

8. **`app/src/main/java/com/shijiben/feature/search/SearchScreen.kt`** — inline (lines ~78-91), inside the top `Column`:
   ```
   // SearchScreen.kt, inside the root Column, after the Box background
           Column(modifier = Modifier.fillMaxSize()) {
               // 顶部 8dp 彩虹条（与 TimelineScreen/HeatmapScreen 同款）
               Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
                   val trimColors = listOf(
                       Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
                       Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
                       Color(0xFF6366F1), Color(0xFFA855F7)
                   )
                   for (i in 0 until 80) {
                       Box(
                           modifier = Modifier
                               .weight(1f).fillMaxHeight()
                               .background(trimColors[i % 8])
                       )
                   }
               }
   ```
   Specific theme imports — **must add** `import com.shijiben.ui.theme.RainbowTrim`.

## Repo conventions to match

- `PixelComponents.kt` is the canonical home for shared 8-bit UI atoms (`PixelCard`, `PixelButton`, `PixelOutlinedButton`). Public composables, no `private`. KDoc on each.
- Theme color tokens live in `AppColors.kt`; the rainbow strip currently hardcodes its 8 colors inline (they match `RainbowHourColors`'s 8-color set but in a fixed order). **Keep the inline `listOf(...)` in the consolidated `RainbowTrim`** — do not refactor it to index into `RainbowHourColors`, because `RainbowHourColors` is semantically "per-hour color" and the trim is a fixed brand sequence. Out of scope.
- No lint config / unused-import gate (per `plans/README.md` "Verification baseline") — leftover unused imports after deletion are tolerated, but remove the obvious ones (`fillMaxHeight` if it was only used by the deleted private fun).

## Commands you will need

| Purpose   | Command                                          | Expected on success |
|-----------|--------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`              | exit 0, no errors   |
| Tests     | `./gradlew :app:testDebugUnitTest --rerun-tasks` | all pass            |
| Build     | `./gradlew assembleDebug`                        | exit 0, APK produced |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt` — add `RainbowTrim`.
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` — replace inline strip with `RainbowTrim()`.
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` — delete private fun, add import.
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt` — delete private fun, add import.
- `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt` — delete private fun, add import.
- `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` — delete private fun, add import.
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt` — delete private fun, add import.
- `app/src/main/java/com/shijiben/feature/search/SearchScreen.kt` — replace inline strip with `RainbowTrim()`, add import.

**Out of scope** (do NOT touch):
- `AppColors.kt` — color tokens stay.
- The 8 hardcoded colors inside the strip — do not swap to `RainbowHourColors`.
- Any other `private fun` duplications (`PixelArrowBox` in heatmap screens, `Legend`, `StepperBox`, etc.) — separate findings, not this plan.

## Git workflow

- Work directly on the current branch.
- Commit message style: `<NNN>: <short desc>`. Example: `019: consolidate RainbowTrim into PixelComponents`.

## Steps

### Step 1: Add `RainbowTrim` to `PixelComponents.kt`

In `app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt`:

1. Add the missing layout imports to the existing import block:
   ```kotlin
   import androidx.compose.foundation.layout.Row
   import androidx.compose.foundation.layout.fillMaxHeight
   import androidx.compose.foundation.layout.fillMaxWidth
   import androidx.compose.foundation.layout.height
   ```
   (`Box`, `background`, `Modifier`, `Color`, `Composable`, `dp` are already imported — verify before adding duplicates.)

2. Append the new composable at the end of the file (after `PixelOutlinedButton`):
   ```kotlin
   /** 顶部 8dp 彩虹条（全 app 唯一品牌标识条，各屏幕顶部统一调用）。 */
   @Composable
   fun RainbowTrim() {
       val trimColors = listOf(
           Color(0xFFEF4444), Color(0xFFF97316), Color(0xFFF59E0B),
           Color(0xFF84CC16), Color(0xFF22C55E), Color(0xFF06B6D4),
           Color(0xFF6366F1), Color(0xFFA855F7)
       )
       Row(modifier = Modifier.fillMaxWidth().height(8.dp)) {
           for (i in 0 until 80) {
               Box(
                   modifier = Modifier
                       .weight(1f).fillMaxHeight()
                       .background(trimColors[i % 8])
               )
           }
       }
   }
   ```

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0. (`RainbowTrim` is now defined but not yet referenced from the 7 sites — that's fine, they still have their own copies until steps 2–4.)

### Step 2: Replace the 5 `private fun RainbowTrim()` copies

For each of these 5 files, in any order:
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt`
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt`
- `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt`
- `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`

do exactly two edits:

(a) **Delete** the entire `private fun RainbowTrim() { ... }` declaration including its KDoc comment line. The signature line is `private fun RainbowTrim() {` and the body is the canonical 13-line body. Leave the **call site** `RainbowTrim()` (inside the screen's top `Column`) untouched — it will now resolve to `PixelComponents.RainbowTrim` via the new import.

(b) **Add** the import `import com.shijiben.ui.theme.RainbowTrim` to the import block (place it alphabetically among the existing `com.shijiben.ui.theme.*` imports in that file).

After deleting the private fun, check whether `androidx.compose.foundation.layout.fillMaxHeight` is still referenced elsewhere in that file (Grep for `fillMaxHeight` within the file). If the deleted `RainbowTrim` was the **only** caller, remove the now-unused import too. If other code still uses it, leave the import. (Unused imports don't break the build, but removing the obvious ones keeps the diff clean.)

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0. If the compiler reports "RainbowTrim is defined in package com.shijiben.ui.theme, do you want to import it?" for any of these 5 files, the import was missed — add it.

### Step 3: Replace the 2 inline copies

For each of these 2 files:
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
- `app/src/main/java/com/shijiben/feature/search/SearchScreen.kt`

replace the inline rainbow strip (the `// 顶部 8dp 彩虹条...` comment + the entire `Row(modifier = Modifier.fillMaxWidth().height(8.dp)) { val trimColors = ...; for (i in 0 until 80) { Box(...) } }` block) with a single call:

```kotlin
// 顶部 8dp 彩虹条（品牌标识）
RainbowTrim()
```

- `TimelineScreen.kt` imports `com.shijiben.ui.theme.*`, so `RainbowTrim()` resolves with **no new import**.
- `SearchScreen.kt` uses specific theme imports — **add** `import com.shijiben.ui.theme.RainbowTrim`.

In both files, keep the surrounding context (the `Column { ... }` the strip lives in, the lines before/after) unchanged.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0, no errors.

### Step 4: Confirm no `private fun RainbowTrim` remains and run the full gate

Run a Grep (not shell grep) for `private fun RainbowTrim` across `app/src/main/java/com/shijiben` → expect **zero** matches. Grep for `fun RainbowTrim` → expect exactly one match (in `PixelComponents.kt`).

**Verify**: `./gradlew :app:testDebugUnitTest --rerun-tasks` → all pass. Then `./gradlew assembleDebug` → exit 0.

## Test plan

- No new automated tests. `RainbowTrim` is a pure-layout composable with no logic; the repo has no Compose UI test framework.
- Existing unit tests don't touch these composables; they continue to pass.
- Optional device check (reviewer): open each of the 7 screens (Timeline, Search, Heatmap month, Heatmap year, TimeViz, Settings, About) and confirm the top 8dp rainbow strip renders identically (8 colors, 80 blocks, full width, 8dp tall).

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest --rerun-tasks` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] Grep for `private fun RainbowTrim` in `app/src/main/java/com/shijiben` → no matches
- [ ] Grep for `fun RainbowTrim` in `app/src/main/java/com/shijiben` → exactly one match, in `PixelComponents.kt`
- [ ] Grep for `RainbowTrim()` call sites → 7 matches (one per screen: Timeline, Search, Heatmap, HeatmapYear, TimeViz, Settings, About)
- [ ] Grep for the inline `trimColors = listOf(` literal → exactly one match, inside `PixelComponents.kt` (was 7 before)
- [ ] No files outside the in-scope list are modified (`git status --short`)
- [ ] `plans/README.md` status row updated *(orchestrator-owned; developer subagents skip this)*

## STOP conditions

Stop and report back (do not improvise) if:

- Any of the 7 sites doesn't match its excerpt (drift since this plan was written).
- A screen calls `RainbowTrim(...)` with arguments (e.g. a custom color list) — would mean a copy was already parameterized differently; the canonical `RainbowTrim()` doesn't accept args, so STOP rather than silently dropping the customization.
- `PixelComponents.kt` already has a `RainbowTrim` (would mean a prior plan landed this differently — STOP rather than collide).
- After step 2/3, the compiler reports `fillMaxHeight`/`height`/`Row` unresolved in `PixelComponents.kt` — would mean the import additions in step 1 didn't land; re-check, don't improvise alternative APIs.
- Two screens end up rendering the strip with visibly different heights/colors after consolidation (device check) — would mean a copy had silently drifted from the canonical body; report which one.

## Dependency notes

- **015, 017** also edit `TimelineScreen.kt`. Disjoint sections: 015 removes mid-file dead code; 017 rewrites `formatDurationShort` near the bottom; 019 replaces the top-of-`Column` inline strip. Land 015/017 first, then 019 — re-run the drift check before 019.
- **016** edits `SettingsScreen.kt` and `AboutScreen.kt`. Disjoint sections: 016 touches the signature/scroll Column/privacy Section; 019 deletes the `private fun RainbowTrim` at the bottom of each file. Land 016 first, then 019 — re-run the drift check before 019.
- Suggested execution order overall: **015 → 017 → 018 → 016 → 019** (019 last, after all other edits to its 8 in-scope files have settled, to minimize drift re-checks).

## Maintenance notes

- After this lands, `RainbowTrim()` is the **only** brand strip. Any future change to the strip (color order, height, block count, animation) is one edit in `PixelComponents.kt`.
- Reviewer: the 7 call sites must render identically to before. The body is byte-for-byte the same code, so the only risk is an import resolution issue (covered by the build gate) or a missed call site (covered by the Grep done-criteria).
- Follow-up (out of scope, separate finding): `PixelArrowBox` is duplicated between `HeatmapScreen` and `HeatmapYearScreen`; `Legend` is duplicated between the same two. If accepted later, the same consolidation pattern applies — move to `PixelComponents.kt`, delete the private copies, add imports.
- The 8 hardcoded colors in `RainbowTrim` intentionally duplicate the 8 colors in `RainbowHourColors` (`AppColors.kt`). They are semantically different (brand trim vs. per-hour palette) and were already duplicated before this plan; do not merge them.
