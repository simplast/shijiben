# 事记本架构文档

> 本文从 4 个视角统一描述事记本的整体架构：模块依赖图、数据流、状态管理、导航图。
> 配合 [AGENT.md](../AGENT.md)（项目上下文）与 [README.md](../README.md)（面向人类入口）阅读。
> 所有符号引用均与当前代码库核对一致（迭代 14 落地时二次实读核对）。

## 1. 概述

事记本采用 **MVVM + Repository + 单向数据流 + Hilt DI + Compose UI** 架构。受《奇特的一生》启发，纯本地无任何联网功能（无网络 SDK / 无云同步 / 无统计 / 无广告），所有数据落 Room SQLite。视觉为 8-bit 像素美学，所有视觉常量集中在 `ui/theme`。技术栈：Kotlin + Jetpack Compose + Room + Hilt + Coroutines/Flow + Navigation Compose。

单向数据流：DB 变化 → DAO Flow emit → Repository Flow 透传 → ViewModel `stateIn(WhileSubscribed(5000))` 缓存 → Compose `collectAsStateWithLifecycle` 重组渲染。命令式操作（导出/导入）走 `MutableStateFlow` + `first()` 一次性取数。

## 2. 模块依赖图

五大块单向依赖（`data` ← `feature` ← `navigation` ← `MainActivity`），`ui/theme` 与 `di` 横切：

```
┌─────────────────────────────────────────────┐
│              MainActivity                    │
│   (setContent { AppTheme { AppNavHost() } }) │
└──────────────────┬──────────────────────────┘
                   │ depends on
                   ▼
┌─────────────────────────────────────────────┐
│            navigation/AppNavHost             │
│   (Routes object + NavHost + 8 composable)   │
└──────────────────┬──────────────────────────┘
                   │ depends on
                   ▼
┌─────────────────────────────────────────────┐
│              feature/* (7 模块)              │
│   heatmap / notes / recording / search       │
│   settings / timeline / timeviz              │
│   每个 = Screen (Composable) + ViewModel      │
└──────┬──────────────────┬────────────────────┘
       │                  │ depends on
       ▼                  ▼
┌──────────────┐  ┌─────────────────────────┐
│  ui/theme/   │  │       data/*            │
│  AppColors   │  │  local/  (Entity/DAO/DB)│
│  AppTheme    │  │  model/  (EventStatus)  │
│PixelComponents│ │  repository/ (Repo)     │
└──────────────┘  │  export/ (导入导出)      │
                  │  DataModule.kt (Hilt)   │
                  └─────────────────────────┘

横切：di/ (DispatchersModule) — 提供 @IoDispatcher
横切：feature/timeviz/TimeVizModule.kt — 提供 SharedPreferences / TimeVizPrefs / Clock
横切：ShiJiBenApplication — @HiltAndroidApp 入口
```

**说明**：`data` 层不依赖任何上层；`feature` 层依赖 `data` + `ui/theme`；`navigation` 依赖 `feature`；`MainActivity` 依赖 `navigation` + `ui/theme`。单向依赖，无环。`di/DispatchersModule` 与 `feature/timeviz/TimeVizModule` 横切提供 Hilt 绑定（共 3 个 Hilt Module，详见 §6）。

实际包路径（`app/src/main/java/com/shijiben/`）：

- `data/local/`：`AppDatabase`（`@Database version=2`）、`EventDao` / `NoteDao`（`@Dao`）、`EventEntity` / `NoteEntity`、`MIGRATION_1_2`。
- `data/model/`：`EventStatus`（enum）、`DailyActivity`（data class）。
- `data/repository/`：`EventRepository` / `NoteRepository`（`@Singleton`）。`EventRepository.kt` 内含 internal 顶层纯函数 `aggregateMonth` / `aggregateYear` / `effectiveDurationMs`。
- `data/export/`：`DataExportManager` / `DataImportManager`（JSON + SAF 流读写）。
- `data/DataModule.kt`：Hilt Module。
- `di/DispatchersModule.kt`：Hilt Module + `@IoDispatcher` qualifier。
- `feature/heatmap/`：`HeatmapScreen` / `HeatmapViewModel` / `HeatmapYearScreen` / `HeatmapYearViewModel` / `HeatmapCalculator`（object，纯函数）。
- `feature/notes/`：`NotesScreen` / `NoteEditorSheet` / `NotesViewModel`。
- `feature/recording/`：`RecordingSheet` / `RecordingViewModel` / `TimeRangeSlider`。
- `feature/search/`：`SearchScreen` / `SearchViewModel`（内含 `SearchItem` sealed interface + internal 纯函数 `filterAndMerge`）。
- `feature/settings/`：`SettingsScreen` / `AboutScreen` / `ExportViewModel` / `ImportViewModel`。
- `feature/timeline/`：`TimelineScreen`（内含 private `TimelineItem` sealed interface）/ `TimelineViewModel` / `DayProgressBar`。
- `feature/timeviz/`：`TimeVizScreen` / `TimeVizViewModel` / `TimeVizCalculator`（object，纯函数）/ `TimeVizPrefs` / `TimeVizModule.kt`。
- `navigation/AppNavHost.kt`：`Routes` object + `AppNavHost` composable。
- `ui/theme/`：`AppColors` / `AppTheme` / `PixelComponents`。
- `ui/debug/`：`DebugOverlay`（`@Composable`，仅 `BuildConfig.DEBUG` 包裹 app 内容叠加悬浮按钮）+ `DebugLog`（`object`，内存 ring buffer 200 条 + 未捕获异常持久化到 `debug-last-crash.txt`，release 空操作）。
- `MainActivity.kt`（`@AndroidEntryPoint`）/ `ShiJiBenApplication.kt`（`@HiltAndroidApp`）。

## 3. 数据流

Room → DAO → Repository → ViewModel → Compose 单向流：

```
Room (AppDatabase, version=2, events + notes 两表)
  │
  ▼  @Query 返回 Flow<List<Entity>>
DAO (EventDao / NoteDao)
  │  - EventDao.getEventsByDate / getEventsByMonth / getAllEvents / getOngoingEvent → Flow
  │  - NoteDao.getNotesByDate / getAllNotes → Flow
  │  - insert/update/delete 为 suspend（OnConflictStrategy.REPLACE 支持导入幂等 upsert）
  ▼  Flow 透传 + 业务逻辑（Repository 不缓存）
Repository (EventRepository / NoteRepository, @Singleton)
  │  - EventRepository.getEventsByDate / getAllEvents / getOngoingEvent → Flow
  │  - EventRepository.getDailyActivityForMonth(YearMonth) → Flow<List<DailyActivity>>
  │  - EventRepository.getDailyActivityForYear(Year) → Flow<List<DailyActivity>>
  │  - createEvent / updateEvent / markCompleted / carryOverNotStarted / upsertAll 为 suspend
  ▼
ViewModel (combine / flatMapLatest + stateIn(WhileSubscribed(5000)))
  │  - TimelineViewModel: combine(_viewingDate, _refreshTrigger).flatMapLatest → stateIn
  │  - HeatmapViewModel: _selectedMonth.flatMapLatest → stateIn
  │  - HeatmapYearViewModel: _selectedYear.flatMapLatest → stateIn
  │  - SearchViewModel: combine(eventsFlow, notesFlow, _query) → stateIn
  │  - NotesViewModel: noteRepository.getAllNotes().stateIn
  │  - ExportViewModel/ImportViewModel: MutableStateFlow<ExportState/ImportState>
  │    （非持续 Flow，getAllEvents().first() 一次性取数，导出/导入是一次性动作）
  ▼
Compose UI (collectAsStateWithLifecycle)
  │  - 生命周期感知订阅
  │  - config change 不重订阅
  ▼
渲染
```

**说明**：单向数据流，DB 变化自动驱动 UI 刷新（Room invalidation tracker → Flow emit → ViewModel `stateIn` → Compose 重组）。`WhileSubscribed(5000)` 提供 5s grace period，配置变更（如旋转）不重启上游。`ExportViewModel` / `ImportViewModel` 走 `MutableStateFlow` + `getAllEvents().first()` 一次性取数，不接持续 Flow。

Room schema：`AppDatabase` 标 `@Database(entities=[EventEntity, NoteEntity], version=2, exportSchema=true)`，schema 落 `app/schemas/`。两表：`events`（id/title/startTime/endTime/status/note/createdAt/updatedAt）+ `notes`（id/content/timestamp/createdAt/updatedAt）。`EventStatus` enum（`NotStarted(0)` / `InProgress(1)` / `Completed(2)`，`status` 列存 Int）。`MIGRATION_1_2`：v1→v2 移除标签功能（建新表-拷数据-删旧-改名重建索引，删 `tags` 表）。索引：`index_events_start_time` / `index_events_status_start`。

## 4. 状态管理

- **ViewModel + StateFlow/Flow**：所有 ViewModel 暴露 `state: StateFlow<XxxUiState>` 或具名 `StateFlow`（如 `events` / `notes` / `allNotes`），UI 用 `collectAsStateWithLifecycle` 订阅。
- **WhileSubscribed(5000) 缓存**：DB 驱动的上游 Flow 经 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)` 缓存，5s grace period 避免短时间取消重订阅。涉及：`TimelineViewModel.events` / `TimelineViewModel.notes`、`HeatmapViewModel.state`、`HeatmapYearViewModel.state`、`SearchViewModel.state`（+ 内部 `eventsFlow` / `notesFlow`）、`NotesViewModel.allNotes`。
- **flatMapLatest**：`TimelineViewModel` / `HeatmapViewModel` / `HeatmapYearViewModel` 用 `flatMapLatest` 切换上游（`_viewingDate`+`_refreshTrigger` / `_selectedMonth` / `_selectedYear` 变化时取消旧订阅启新订阅）。
- **combine 多流合并**：`SearchViewModel` 用 `combine(eventsFlow, notesFlow, _query)` 合并三流。
- **sealed state 状态机**：
  - `TimelineItem` sealed interface（`EventItem` / `NoteItem`，private 于 `TimelineScreen.kt`，按 `sortKey` 合并排序）。
  - `SearchItem` sealed interface（`EventItem` / `NoteItem`，定义于 `SearchViewModel`）。
  - `ExportState` sealed interface（`Idle` / `Exporting` / `Success(fileName)` / `Error(message)`，定义于 `ExportViewModel`）。
  - `ImportState` sealed interface（`Idle` / `Importing` / `Success(events,notes,prefsUpdated)` / `Error(message)`，定义于 `ImportViewModel`）。
  - 注：`HeatmapCalculator.Cell` 是 **data class**（`level: Int` 0..4 色阶），非 sealed，但承担色阶状态语义。
- **纯函数抽离**（internal，无 Android 依赖，纯 JUnit 可测）：
  - `aggregateMonth` / `aggregateYear` / `effectiveDurationMs`（`EventRepository.kt` 顶层 internal 函数，热力图月/年聚合 + 跨日时长截断）。
  - `filterAndMerge`（`SearchViewModel.kt` 顶层 internal 函数，搜索过滤+合并+排序）。
  - `HeatmapCalculator`（object：`buildGrid` / `buildYearGrid` / `levelFor`，月历 6×7 网格 + 色阶映射）。
  - `TimeVizCalculator`（object：`todayRemaining` / `yearRemaining` / `lifeRemaining`，时间可视化计算）。

## 5. 导航图

`Routes` object 8 常量，`startDestination = Routes.TIMELINE`：

```
                    ┌─────────────┐
                    │  TIMELINE   │ ← 启动页 (startDestination)
                    │  (首页)     │
                    └─┬──┬──┬──┬─┬┘
                      │  │  │  │ │
        ┌─────────────┘  │  │  │ └─────────────┐
        ▼                ▼  ▼  ▼               ▼
   ┌─────────┐  ┌─────────┐ ┌──────────┐ ┌──────────┐
   │  NOTES  │  │ TIMEVIZ │ │ HEATMAP  │ │ SETTINGS │
   │ (随笔)  │  │ (时间)  │ │ (热力图) │ │ (设置)   │
   └─────────┘  └─────────┘ └────┬─────┘ └────┬─────┘
                                │            │
                                ▼            ▼
                          ┌─────────────┐ ┌─────────┐
                          │ HEATMAP_YEAR│ │  ABOUT  │
                          │  (年视图)   │ │ (关于)  │
                          └─────────────┘ └─────────┘

   ┌─────────┐
   │ SEARCH  │ ← 从 TIMELINE 顶栏放大镜进入 (onSearchClick)
   │ (搜索)  │
   └─────────┘

   注：HEATMAP 点日期 → popBackStack 回 TIMELINE 并带 heatmap_target_date
```

**说明**：8 路由（`TIMELINE` / `NOTES` / `TIMEVIZ` / `HEATMAP` / `HEATMAP_YEAR` / `SEARCH` / `SETTINGS` / `ABOUT`）。`TIMELINE` 是启动页，5 个出口（`onNotesClick` / `onTimeVizClick` / `onHeatmapClick` / `onSearchClick` / `onSettingsClick`）。`HEATMAP` → `HEATMAP_YEAR` 是层级跳转（`onYearClick`）；`SETTINGS` → `ABOUT` 是层级跳转（`onAboutClick`）；`SEARCH` 从 `TIMELINE` 顶栏放大镜进入。

特殊链路：`HEATMAP` 点日期方块 → 写 `TIMELINE` 的 `savedStateHandle["heatmap_target_date"]` = `Triple<Int,Int,Int>(y,m,d)` 后 `popBackStack()` 回 `TIMELINE`（`onDateClick`）；`TIMELINE` 用 `LaunchedEffect` collect `savedStateHandle` 的 `getStateFlow` → `setDate` → `onDateApplied` 清空。`Triple<Int,Int,Int>` 实现 `Serializable` 可入 `Bundle`，是跨屏传选中日期的载体。其余叶子路由（`NOTES` / `TIMEVIZ` / `SEARCH` / `HEATMAP_YEAR` / `ABOUT`）用 `popBackStack()` 返回上级。

## 6. 依赖注入（Hilt）

- **入口**：`ShiJiBenApplication` 标 `@HiltAndroidApp`，是 Hilt 容器入口；`MainActivity` 标 `@AndroidEntryPoint`；所有 ViewModel 标 `@HiltViewModel` + `@Inject constructor`。
- **3 个 Hilt Module**（均 `@InstallIn(SingletonComponent::class)`）：
  - `data/DataModule.kt`：`provideAppDatabase`（`@Singleton`，`Room.databaseBuilder` + `addMigrations(MIGRATION_1_2)`，db 名 `"shijiben.db"`）、`provideEventDao` / `provideNoteDao`。`EventRepository` / `NoteRepository` 标 `@Singleton` + `@Inject constructor`，Hilt 自动注入。
  - `di/DispatchersModule.kt`：`@IoDispatcher` qualifier（`@Retention(BINARY)`）+ `provideIoDispatcher` 返回 `Dispatchers.IO`。**注：项目仅此一个 dispatcher qualifier，无 `@DefaultDispatcher`。**
  - `feature/timeviz/TimeVizModule.kt`：`provideTimeVizSharedPreferences`（`@Singleton`，`"timeviz_prefs"`）、`provideTimeVizPrefs`（`@Singleton`）、`provideClock`（`@Singleton`，`Clock.systemDefaultZone()`，注入 `TimeVizViewModel` 替代墙钟便于测试）。
- **Qualifier**：仅 `@IoDispatcher` 一个。
- **消费方**：`ExportViewModel` / `ImportViewModel` 注入 `@IoDispatcher`（导出/导入 IO 在该 dispatcher 执行）；`TimeVizViewModel` 注入 `Clock` + `TimeVizPrefs`。

## 7. 关键设计模式

1. **Repository 模式**：UI 不直接访问 DAO，经 `EventRepository` / `NoteRepository`。Repository `@Singleton`，`@Inject constructor(dao)`，暴露 Flow（查询）+ suspend（写操作）。
2. **纯函数抽离**：`aggregateMonth` / `aggregateYear` / `effectiveDurationMs` / `filterAndMerge` / `HeatmapCalculator` / `TimeVizCalculator` 均无 Android 依赖，纯 JUnit 可测（对应 `EventRepositoryHeatmapTest` / `SearchFilterTest` / `HeatmapCalculatorTest` / `TimeVizCalculatorTest`）。
3. **sealed interface 状态机**：`TimelineItem` / `SearchItem`（列表项多态合并）、`ExportState` / `ImportState`（一次性动作状态机，`Idle → Exporting/Importing → Success/Error → resetState() → Idle`），编译期穷尽性检查。
4. **8-bit 美学集中**：所有视觉元素集中在 `ui/theme`——`AppColors`（高饱和彩虹色板 + `HeatmapLevel0..4` 绿色色阶 + `RainbowHourColors` 8 色循环）、`AppTheme`（强制浅色 + Fusion Pixel 字体 + 全 0.dp 直角 `PixelShapes`）、`PixelComponents`（`PixelCard` 等复用组件）。feature 层只消费色板/组件，不自定义视觉常量。
5. **flaky test 根治方案 A**（迭代 9 落地，仅测试侧改动，生产代码零改动）：根因 `WhileSubscribed(5000)` grace period 内 Room invalidation tracker 跑在真实 executor 线程 + `Dispatchers.resetMain()` 后命中 `NoopDispatcher`。方案 A 在 `HeatmapViewModelTest.setup()` 用 `setQueryExecutor` / `setTransactionExecutor` 把 Room executor 桥接到 `StandardTestDispatcher`，teardown 后队列静止不再 emit，竞态从根上消除。详见 `docs/superpowers/specs/2026-06-28-flaky-rootfix-and-docs-design.md`，5 次独立验证全绿。
6. **DB 迁移历史**：`AppDatabase` version=2，v1→v2 迁移是标签移除（建新表-拷数据-删旧-改名重建索引，删 `tags` 表），详见 spec `2026-06-27-top-bottom-redesign-design.md` Part C。
