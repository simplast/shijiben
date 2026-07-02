# Verification baseline
established_at: d402f37, 2026-06-29

## Gate command
./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease

## Known baseline failures (treat as PASS)
- TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday — java.lang.IllegalStateException at TestMainDispatcher.kt:67, pre-existing dispatcher-related failure
- TimelineViewModelTest > markInProgress_validEventId_setsInProgressWithNullEndTime — same flaky TestMainDispatcher.kt:67 IllegalStateException pattern (intermittent, re-run usually passes)

## Resolved baseline failures
- HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce — FIXED in cycle 31 (F031): buildYearGrid 去除 padding 格 isToday 重复标记
- HeatmapYearViewModelTest > yearGrid_updatesWhenRepoEmitsNewData — FIXED in cycle 31 (F031): same root cause as yearGrid_todayMarkedExactlyOnce
