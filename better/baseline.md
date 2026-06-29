# Verification baseline
established_at: d402f37, 2026-06-29

## Gate command
./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease

## Known baseline failures (treat as PASS)
- HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce — pre-existing failure at baseline establishment
- TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday — java.lang.IllegalStateException at TestMainDispatcher.kt:67, pre-existing dispatcher-related failure
