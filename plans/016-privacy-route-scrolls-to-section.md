# Plan 016: Make "隐私政策" open the About page scrolled to the privacy section

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 2a71f17..HEAD -- app/src/main/java/com/shijiben/navigation/AppNavHost.kt app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`
> If any in-scope file changed since this plan was written, compare the "Current state" excerpts against the live code before proceeding. Compare the **content** of each excerpt's 3-line window, not just line numbers. If surrounding lines shifted but the target code itself is unchanged, proceed. If the target code itself has changed, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: bug (UX — misleading navigation)
- **Planned at**: commit `2a71f17`, 2026-06-29

## Why this matters

In `SettingsScreen`, the "关于事记本" and "隐私政策" rows are wired to the **same** `onAboutClick` callback, so both open the identical About page positioned at the top. A user tapping "隐私政策" expects to land on the privacy policy, not the about blurb — the duplicate destination is misleading. This plan adds a dedicated `privacy` route that opens `AboutScreen` already scrolled to its "隐私政策" section, and rewires only the "隐私政策" row to use it. The "关于事记本" row keeps its current behavior.

## Current state

Three files, each with one line on its role:

- `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` — settings list; both rows currently call `onAboutClick`.
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt` — the About page (stateless). Contains two `Section` blocks: "关于事记本" then "隐私政策".
- `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` — defines `Routes` and the NavHost graph.

Excerpts (3-line windows, content-labeled, line numbers are hints):

1. `SettingsScreen.kt`, the two-row block inside the settings `Column`:
   ```
   // SettingsScreen.kt, inside the settings list Column
               SettingsRow(title = "关于事记本", onClick = onAboutClick)
               SettingsRow(title = "隐私政策", onClick = onAboutClick)   // ← same destination
               SettingsRow(
   ```
   And the `SettingsScreen` signature, just above:
   ```
   // SettingsScreen.kt, fun SettingsScreen(...) signature
   fun SettingsScreen(
       onBack: () -> Unit,
       onAboutClick: () -> Unit,
       viewModel: ExportViewModel = hiltViewModel(),
   ```

2. `AboutScreen.kt`, signature + the scrollable Column + the two sections:
   ```
   // AboutScreen.kt, fun AboutScreen(...) signature
   @Composable
   fun AboutScreen(
       onBack: () -> Unit
   ) {
   ```
   ```
   // AboutScreen.kt, the scrollable content Column
           Column(
               modifier = Modifier
                   .fillMaxSize()
                   .verticalScroll(rememberScrollState())   // ← inline state, must hoist
                   .padding(16.dp),
               verticalArrangement = Arrangement.spacedBy(12.dp)
           ) {
               // 区块 1：关于
               Section(title = "关于事记本") {
   ```
   ```
   // AboutScreen.kt, the privacy Section (block 2)
               // 区块 2：隐私政策
               Section(title = "隐私政策") {
                   Text(
                       text = "事记本是一款纯本地时间记录应用。本隐私政策说明数据处理方式：",
   ```
   `AboutScreen` is `Stateless` (no ViewModel) — per its top-of-file KDoc.

3. `AppNavHost.kt`, the `Routes` object and the two relevant composable entries:
   ```
   // AppNavHost.kt, object Routes
   object Routes {
       const val TIMELINE = "timeline"
       const val NOTES = "notes"
   ```
   ```
   // AppNavHost.kt, inside object Routes (end)
       const val SETTINGS = "settings"
       const val ABOUT = "about"
   }
   ```
   ```
   // AppNavHost.kt, SettingsScreen wiring
       composable(Routes.SETTINGS) {
           SettingsScreen(
               onBack = { navController.popBackStack() },
               onAboutClick = { navController.navigate(Routes.ABOUT) }
           )
       }
       composable(Routes.ABOUT) {
           AboutScreen(onBack = { navController.popBackStack() })
       }
   ```

## Repo conventions to match

- 8-bit aesthetic, Kotlin + Jetpack Compose, MVVM. See `AGENT.md` "开发约定".
- `AboutScreen` is intentionally stateless with no ViewModel — keep it that way. Scrolling is pure UI state, fine to hold in `rememberScrollState()` inside the composable.
- All UI strings are Chinese (zh-Hans), inline literals — no `strings.xml` migration in this plan.

## Commands you will need

| Purpose   | Command                                          | Expected on success |
|-----------|--------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`              | exit 0, no errors   |
| Tests     | `./gradlew :app:testDebugUnitTest --rerun-tasks` | all pass            |
| Build     | `./gradlew assembleDebug`                        | exit 0, APK produced |

## Scope

**In scope** (the only files you should modify):
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`
- `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/shijiben/navigation/AppNavHost.kt`

**Out of scope** (do NOT touch):
- `ExportViewModel.kt`, `ImportViewModel.kt` — unrelated to navigation.
- The content of the privacy policy text itself.
- `strings.xml` — no i18n migration.

## Git workflow

- Work directly on the current branch.
- Commit message style (from `git log`): `<NNN>: <short desc>`. Example: `016: route 隐私政策 to a privacy-scrolled About page`.

## Steps

### Step 1: Add `Routes.PRIVACY` and wire it in `AppNavHost.kt`

In `app/src/main/java/com/shijiben/navigation/AppNavHost.kt`:

1. Inside `object Routes { ... }`, add a new constant after `ABOUT`:
   ```kotlin
   const val ABOUT = "about"
   const val PRIVACY = "privacy"
   ```
2. In the `NavHost` block, add a new `composable(Routes.PRIVACY) { ... }` entry right after the `composable(Routes.ABOUT)` entry:
   ```kotlin
   composable(Routes.PRIVACY) {
       AboutScreen(
           onBack = { navController.popBackStack() },
           scrollToPrivacy = true
       )
   }
   ```
3. Update the `composable(Routes.SETTINGS)` block to pass a second callback to `SettingsScreen`:
   ```kotlin
   composable(Routes.SETTINGS) {
       SettingsScreen(
           onBack = { navController.popBackStack() },
           onAboutClick = { navController.navigate(Routes.ABOUT) },
           onPrivacyClick = { navController.navigate(Routes.PRIVACY) }
       )
   }
   ```
   Leave the existing `composable(Routes.ABOUT) { AboutScreen(onBack = ...) }` entry as-is (it stays the top-of-page entry for the "关于事记本" row).

**Verify**: do not compile yet — `SettingsScreen` and `AboutScreen` signatures change in steps 2–3, so the build will only pass after those land.

### Step 2: Add `scrollToPrivacy` param + scroll behavior to `AboutScreen.kt`

In `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`:

1. Change the signature to accept the new param with a default of `false`:
   ```kotlin
   @Composable
   fun AboutScreen(
       onBack: () -> Unit,
       scrollToPrivacy: Boolean = false
   ) {
   ```
2. Hoist the scroll state into a `val` so it can be driven by a `LaunchedEffect`. Replace the inline `.verticalScroll(rememberScrollState())` on the content `Column` with a hoisted state:
   ```kotlin
   val scrollState = rememberScrollState()
   // …
   Column(
       modifier = Modifier
           .fillMaxSize()
           .verticalScroll(scrollState)
           .padding(16.dp),
       verticalArrangement = Arrangement.spacedBy(12.dp)
   ) {
   ```
3. Capture the y-offset of the privacy `Section` so we can scroll to it. Wrap the **second** `Section(title = "隐私政策") { ... }` call site in a `Box` that records its position, and add a `LaunchedEffect` that scrolls when `scrollToPrivacy` is true. Concretely, just before the `// 区块 2：隐私政策` comment, introduce a state holder; the pattern is:
   ```kotlin
   var privacyOffsetPx by remember { mutableStateOf(0) }
   LaunchedEffect(scrollToPrivacy) {
       if (scrollToPrivacy && privacyOffsetPx > 0) {
           scrollState.animateScrollTo(privacyOffsetPx)
       }
   }
   ```
   Then wrap the privacy `Section(...)` in:
   ```kotlin
   Box(
       modifier = Modifier.onGloballyPositioned { coordinates ->
           // position in the scrollable content's parent coordinate space
           privacyOffsetPx = coordinates.positionInParent().y.toInt()
       }
   ) {
       Section(title = "隐私政策") { /* unchanged content */ }
   }
   ```
   Imports needed (add to the existing import block):
   - `androidx.compose.foundation.layout.onGloballyPositioned` is **not** an import — use `androidx.compose.ui.layout.onGloballyPositioned`
   - `androidx.compose.ui.layout.positionInParent`
   - `androidx.compose.runtime.mutableStateOf`, `remember`, `getValue`, `setValue` (some may already be imported — check before adding duplicates)
   - `androidx.compose.foundation.ScrollState` already comes in via `rememberScrollState`
   - `androidx.compose.runtime.LaunchedEffect`
   - `androidx.compose.foundation.layout.Box` (likely already imported)

   Do **not** change the privacy section's text content.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0. (After step 3 it will fully link.)

### Step 3: Rewire `SettingsScreen.kt` to take `onPrivacyClick`

In `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt`:

1. Add the new callback param to the signature, right after `onAboutClick`:
   ```kotlin
   fun SettingsScreen(
       onBack: () -> Unit,
       onAboutClick: () -> Unit,
       onPrivacyClick: () -> Unit,
       viewModel: ExportViewModel = hiltViewModel(),
       importViewModel: ImportViewModel = hiltViewModel()
   ) {
   ```
2. Change **only** the "隐私政策" row's `onClick` from `onAboutClick` to `onPrivacyClick`:
   ```kotlin
   SettingsRow(title = "关于事记本", onClick = onAboutClick)
   SettingsRow(title = "隐私政策", onClick = onPrivacyClick)
   ```
   Leave the "关于事记本" row unchanged.

**Verify**: `./gradlew :app:compileDebugKotlin` → exit 0, no errors. Then `./gradlew :app:testDebugUnitTest --rerun-tasks` → all pass. Then `./gradlew assembleDebug` → exit 0.

## Test plan

- No new automated tests (no Compose UI test framework in this repo). The existing unit tests don't cover these composables.
- Manual device check (optional, called out for the reviewer): from Settings → tap "隐私政策" → About page opens with the privacy section in view; from Settings → tap "关于事记本" → About page opens at top. Back button returns to Settings in both cases.

## Done criteria

Machine-checkable. ALL must hold:

- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew :app:testDebugUnitTest --rerun-tasks` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] Grep for `onPrivacyClick` in `SettingsScreen.kt` and `AppNavHost.kt` → present in both
- [ ] Grep for `scrollToPrivacy` in `AboutScreen.kt` and `AppNavHost.kt` → present in both
- [ ] Grep for `Routes.PRIVACY` in `AppNavHost.kt` → present (declaration + composable entry)
- [ ] No files outside the in-scope list are modified (`git status --short`)
- [ ] `plans/README.md` status row updated *(orchestrator-owned; developer subagents skip this)*

## STOP conditions

Stop and report back (do not improvise) if:

- The code at the locations in "Current state" doesn't match the excerpts (drift).
- `AboutScreen` already has a `scrollToPrivacy`/`initialSection` param (would mean this was already done differently — STOP rather than collide).
- `positionInParent()` / `onGloballyPositioned` doesn't give a stable offset relative to `scrollState` (e.g. the wrapped `Box` reports coordinates in the wrong space). If `animateScrollTo` lands in the wrong spot, fall back to a simpler signal — e.g. give the privacy `Section` a fixed `Modifier.heightIn`/`Spacer` marker and scroll to a known offset — but only after reporting, not silently.
- The `Routes.PRIVACY` constant name collides with an existing route.

## Maintenance notes

- If a future change splits the About page into separate About/Privacy screens, delete the `scrollToPrivacy` param and the `Routes.PRIVACY` route — the per-screen navigation will supersede it.
- Reviewer: confirm the privacy `Section`'s `onGloballyPositioned` offset is in the same coordinate space as `scrollState` (both are in the content `Column`'s parent). If the scroll overshoots/undershoots on a small screen, the fallback in the STOP conditions applies.
- The `LaunchedEffect(scrollToPrivacy)` fires once per entry into the `PRIVACY` route; re-entering the route (back → settings → privacy again) re-triggers it because the composable leaves composition on navigate-away. Good.
