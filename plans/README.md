# Implementation Plans

Two batches of plans from the `improve` skill:

- **Batch 1 (2026-06-26, commit `d74ab99`)** — UI / interaction optimization, Compose UI only. Plans 001-010, all DONE.
- **Batch 2 (2026-06-27, commit `9da5c72`)** — post-redesign cleanup: a data-corruption bug fix, a security/log cleanup, doc reconciliation after the tag removal, and a stated-but-undelivered V1 feature (note markers on the timeline). Plans 011-014, all TODO.

Each executor: read the plan fully before starting, run its drift check, honor its STOP conditions, and update your row when done. Verification gates are shared across plans: `./gradlew :app:compileDebugKotlin` (typecheck) · `./gradlew :app:testDebugUnitTest` (unit tests) · `./gradlew assembleDebug` (build).

## Execution order & status

| Plan | Title | Priority | Effort | Depends on | Status |
|------|-------|----------|--------|------------|--------|
| 001  | [Add event delete affordance via long-press on EventCard](001-event-delete-affordance.md) | P1 | S | — | DONE |
| 002  | [Fix duration slider cap inconsistency (3h UI vs 8h ViewModel)](002-duration-slider-cap-consistency.md) | P1 | M | — | DONE |
| 003  | [Fix bottom quick-add input placeholder semantics](003-bottom-input-semantics.md) | P1 | S | — | DONE |
| 004  | [Add "Today" shortcut and not-on-today indicator to the date bar](004-today-shortcut-indicator.md) | P1 | S | — | DONE |
| 005  | [Show start time and elapsed duration for in-progress events](005-in-progress-event-time-display.md) | P2 | S | — (see overlap note) | DONE |
| 006  | [Align RecordingSheet button labels with the rest of the app (取消/保存)](006-recording-sheet-button-labels.md) | P2 | S | — | DONE |
| 007  | [DayProgressBar hour labels + live "now" marker](007-day-progressbar-labels-live-now.md) | P2 | M | — (see overlap note) | DONE |
| 008  | [Add delete confirmation dialog for tags and notes](008-delete-confirmation-dialog.md) | P2 | S | — | DONE |
| 009  | [Align slider labels with their true tick positions](009-slider-label-alignment.md) | P3 | S | — (land after 002) | DONE |
| 010  | [Remove dead `EventList.kt` file](010-remove-dead-eventlist-file.md) | P3 | S | — (land after 001 & 005) | DONE |
| 011  | [Fix `RecordingViewModel.initEdit` data corruption + un-skip the two red tests](011-fix-recording-initEdit-data-corruption.md) | P1 | M | — | DONE |
| 012  | [Remove debug `Log.d` calls from production code](012-remove-debug-log-calls.md) | P2 | S | — | DONE |
| 013  | [Update AGENT.md, design spec, and product brief to reflect the tag removal](013-update-docs-after-tag-removal.md) | P2 | S | — | DONE |
| 014  | [Render note markers on the timeline (collect the unused `notes` Flow)](014-render-note-markers-on-timeline.md) | P2 | M | — | TODO |

Status values: TODO | IN PROGRESS | DONE | BLOCKED (with one-line reason) | REJECTED (with one-line rationale).

## Dependency & ordering notes

There are **no hard dependencies** — every plan is independently shippable and the build stays green after each. The ordering notes below are about *file overlap* (merge conflicts) and *shared-state coordination*, not prerequisites.

- **`TimelineScreen.kt` is touched by 001, 003, 004, 005, 007.** Land these **sequentially**, not in parallel: each executor must re-run its drift check (`git diff --stat d74ab99..HEAD -- ...TimelineScreen.kt`) and re-locate edits by content (line numbers will have shifted). Recommended order: 004 (date bar) → 003 (bottom input) → 001 (EventCard long-press) → 005 (EventCard time block) → 007 (nowHour/live now). 001 and 005 both add a parameter to `EventCard`'s signature — if executed by the same agent in one pass, merge both parameters.
- **Shared ticking `now` between 005 and 007.** Both introduce a `produceState { delay(60_000) }` "now" source at the top of `TimelineScreen`. Declare it **once** and consume it from both `EventCard` (005) and `DayProgressBar` (007). Do not create two ticking coroutines. Land 005 first, then 007 reuses its `now`.
- **`RecordingSheet.kt` is touched by 002 and 006.** Disjoint sections (002: `TimeRangeSlider(...)` call at lines 78-84; 006: button Row at 86-113). Either order; re-run drift check.
- **`TimeRangeSlider.kt` is touched by 002 and 009.** Land **002 first** (it restructures `DurationBar` and its labels); then 009 adapts both `BarLabels` call sites to positioned labels and integrates with 002's `buildDurationLabels`. If 009 lands first, 002's Step 2 must preserve the `(String, Float)` pair contract.
- **`EventList.kt` deletion (010) should land after 001 & 005** have settled the inline `EventList` signature, so the "no callers" verification in 010 Step 1 is unambiguous. Not a hard dep — 010 just needs the inline `EventList` to remain the sole live definition.

Suggested overall sequence: **004 → 003 → 006 → 008 → 002 → 001 → 005 → 007 → 009 → 010**.
This front-loads the no-overlap, lowest-risk wins (003, 006, 008), handles the `TimeRangeSlider.kt` pair (002→009) and the `TimelineScreen.kt` cluster (001→005→007) in dependency order, and closes with the trivial deletion (010).

## Findings considered and rejected

- **#12 — Timeline empty-state guidance ("今天还是空白" lacks a + hint)**: not worth a plan right now — the bottom input (plan 003) and the existing `+` button are sufficient affordance once the placeholder copy is honest; a verbose empty state would clutter the sparse 8-bit aesthetic. Re-audit only if new-user onboarding telemetry shows confusion.
- **Swipe-to-delete for events**: rejected for v1 in favor of long-press (plan 001) — long-press is more discoverable and simpler to implement reliably in Compose.
- **Snackbar-based undo for note/tag deletion**: deferred from plan 008. The cascade in `TagsViewModel.delete` (`clearTagReference`) isn't snapshotted, so true undo requires a soft-delete/snapshot layer first. Plan 008 ships a confirmation dialog as the safety net; undo is a future L-effort follow-up.
- **Full date picker (calendar jump-to-date)**: deferred from plan 004. The "回今天" tap affordance covers the common "I drifted, get me back" case; arbitrary-date jump is a separate future plan.
- **DayProgressBar tappable hours (tap an hour → pre-fill new event)**: a direction suggestion, not a bug fix — tracked separately, not in this batch.
- **Notes list search / date grouping**: a V2 direction suggestion (notes grow unbounded); out of scope for this UI-polish batch.

## Verification baseline

- The repo has **no Compose UI tests** and **no lint config**; verification is build + existing unit tests (ViewModel/repository tests under `app/src/test/`).
- All plans gate on `./gradlew :app:compileDebugKotlin` + `./gradlew :app:testDebugUnitTest` + `./gradlew assembleDebug` exiting 0. (Plans 011+ un-skip the previously-excluded tests; they are now part of the gate.)
- Device verification (visual/interaction checks) is called out in each plan's Done criteria where relevant — these cannot be automated in this repo.
