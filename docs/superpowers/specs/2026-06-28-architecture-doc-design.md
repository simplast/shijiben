# 2026-06-28 架构文档 Design（迭代 14）

> 本轮目标：新增 `docs/ARCHITECTURE.md`，统一描述 4 个视角：模块依赖图 + 数据流 + 状态管理 + 导航图。
> 这是文档类 spec，不涉及代码改动（仅新增 1 文档 + 改 1 文档加链接）。
> Design 阶段已实读代码，下文所有符号引用均与当前代码库核对一致。

## 一、背景与目标

- 当前文档可维护性 9/10，剩 1 分空间（累计质量 99/100）。
- 项目缺一份统一架构文档：`AGENT.md` 是给 AI agent 的项目上下文，`README.md` 是面向人类的入口，二者都没有完整的架构视图。
- 新增 `docs/ARCHITECTURE.md` 独立成文，4 视角统一描述，便于新人 onboarding / AI agent 理解整体架构。
- `AGENT.md` 加一行链接指向 `ARCHITECTURE.md`，保证可发现性。

## 二、硬约束

1. 绝对不做任何联网功能（本地化 app 唯一原则）。spec 中涉及的所有依赖均纯本地（Room / Hilt / Coroutines / Navigation Compose / SAF 导入导出，无网络 SDK）。
2. 本轮仅文档改动，不改任何 `.kt` / `.xml` / `build.gradle.kts` / `AndroidManifest.xml`。
3. 四道验证门仍须全绿（纯文档改动预期零回归）。
4. 范围严格：不借机清理其他 backlog / 不做防御性改进 / 不组合其他目标。
5. spec 用符号级引用（类名 / 函数名 / 文件名），不用行号（项目级工程约定，避免后续代码改动行号漂移）。

## 三、Orchestrator 决策点（4 项全部选项 A）

1. 架构文档位置：选项 A = `docs/ARCHITECTURE.md`（独立成文，不并入 AGENT.md）。
2. AGENT.md 加链接：选项 A = 加一行链接（保证可发现性）。
3. 图示方式：选项 A = ASCII art（零依赖纯文本，不用 Mermaid，不用无图纯文字）。
4. 范围：选项 A = 单独做架构文档（不组合边界测试加固等，避免范围蔓延）。

## 四、设计内容

### §4.1 新增文件 `docs/ARCHITECTURE.md`

#### §4.1.1 文档结构（7 节）

1. **概述**：一段话介绍事记本架构（MVVM + Repository + 单向数据流 + Hilt DI + Compose UI，纯本地无联网）。
2. **模块依赖图**：ASCII art 画五大块单向依赖（`data` ← `feature` ← `navigation` ← `MainActivity`，`di` 横切，`ui/theme` 横切被 feature 用）。
3. **数据流**：ASCII art 画 Room → DAO（`@Query` 返回 Flow）→ Repository（Flow）→ ViewModel（`stateIn(WhileSubscribed(5000))`）→ Compose（`collectAsStateWithLifecycle`），配文字说明。
4. **状态管理**：描述 ViewModel + StateFlow/Flow 模式 + `WhileSubscribed(5000)` 缓存 + sealed state 状态机 + `flatMapLatest` + 纯函数抽离。
5. **导航图**：ASCII art 画 8 路由 + 入口跳转关系。
6. **依赖注入**：Hilt `@HiltAndroidApp` + 3 个 Module（`DataModule` / `DispatchersModule` / `TimeVizModule`）+ `@IoDispatcher` qualifier。
7. **关键设计模式**：Repository 模式 / 纯函数抽离 / sealed interface 状态机 / 8-bit 美学集中 / flaky test 根治方案 A。

#### §4.1.2 4 视角具体内容

以下内容为 Design 阶段实读代码后确定的准确事实，Coding subagent 落地时须以此为准并二次核对。

##### 视角 1：模块依赖图

实际包路径（已核对 `app/src/main/java/com/shijiben/`）：

- `data/local/`：`AppDatabase`（`@Database version=2`）、`EventDao` / `NoteDao`（`@Dao`）、`EventEntity` / `NoteEntity`、`MIGRATION_1_2`。
- `data/model/`：`EventStatus`（enum，`NotStarted(0)`/`InProgress(1)`/`Completed(2)`）、`DailyActivity`（data class，热力图聚合产物）。
- `data/repository/`：`EventRepository` / `NoteRepository`（`@Singleton`，`@Inject constructor`）。`EventRepository.kt` 内含 internal 纯函数 `aggregateMonth` / `aggregateYear` / `effectiveDurationMs`。
- `data/export/`：`DataExportManager` / `DataImportManager`（JSON 序列化/解析 + SAF 流读写）。
- `data/DataModule.kt`：Hilt Module。
- `di/DispatchersModule.kt`：Hilt Module + `@IoDispatcher` qualifier。
- `feature/heatmap/`：`HeatmapScreen` / `HeatmapViewModel` / `HeatmapYearScreen` / `HeatmapYearViewModel` / `HeatmapCalculator`（object，纯函数）。
- `feature/notes/`：`NotesScreen` / `NoteEditorSheet` / `NotesViewModel`。
- `feature/recording/`：`RecordingSheet` / `RecordingViewModel` / `TimeRangeSlider`。
- `feature/search/`：`SearchScreen` / `SearchViewModel`（内含 `SearchItem` sealed interface + internal 纯函数 `filterAndMerge`）。
- `feature/settings/`：`SettingsScreen` / `AboutScreen` / `ExportViewModel` / `ImportViewModel`。
- `feature/timeline/`：`TimelineScreen` / `TimelineViewModel` / `DayProgressBar`。`TimelineScreen.kt` 内含 private `TimelineItem` sealed interface。
- `feature/timeviz/`：`TimeVizScreen` / `TimeVizViewModel` / `TimeVizCalculator`（object，纯函数）/ `TimeVizPrefs` / `TimeVizModule.kt`（Hilt Module）。
- `navigation/AppNavHost.kt`：`Routes` object + `AppNavHost` composable。
- `ui/theme/`：`AppColors`（8-bit 色板）、`AppTheme`（composable，强制浅色 + 像素字体 + 0.dp 直角）、`PixelComponents`（`PixelCard` 等组件）。
- `MainActivity.kt`：`@AndroidEntryPoint`，`setContent { AppTheme { Surface { AppNavHost() } } }`，`installSplashScreen()`。
- `ShiJiBenApplication.kt`：`@HiltAndroidApp`。

ASCII art（Coding 落地时核对实际代码后照此绘制）：

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

文字说明：`data` 层不依赖任何上层；`feature` 层依赖 `data` + `ui/theme`；`navigation` 依赖 `feature`；`MainActivity` 依赖 `navigation` + `ui/theme`。单向依赖，无环。`di` 与 `TimeVizModule` 横切提供 Hilt 绑定。

##### 视角 2：数据流

ASCII art：

```
Room (AppDatabase, version=2, events + notes 两表)
  │
  ▼  @Query 返回 Flow<List<Entity>>
DAO (EventDao / NoteDao)
  │  - EventDao.getEventsByDate / getEventsByMonth / getAllEvents / getOngoingEvent → Flow
  │  - NoteDao.getNotesByDate / getAllNotes → Flow
  │  - insert/update/delete 为 suspend（OnConflictStrategy.REPLACE 支持导入幂等 upsert）
  ▼  Flow 透传（Repository 不缓存，纯转发 + 业务逻辑）
Repository (EventRepository / NoteRepository, @Singleton)
  │  - EventRepository.getEventsByDate / getAllEvents / getOngoingEvent → Flow
  │  - EventRepository.getDailyActivityForMonth(YearMonth) → Flow<List<DailyActivity>>
  │  - EventRepository.getDailyActivityForYear(Year) → Flow<List<DailyActivity>>
  │  - createEvent / updateEvent / markCompleted / carryOverNotStarted 为 suspend
  ▼
ViewModel (combine / flatMapLatest + stateIn(WhileSubscribed(5000)))
  │  - TimelineViewModel: combine(_viewingDate, _refreshTrigger).flatMapLatest → stateIn
  │  - HeatmapViewModel: _selectedMonth.flatMapLatest → stateIn
  │  - HeatmapYearViewModel: _selectedYear.flatMapLatest → stateIn
  │  - SearchViewModel: combine(eventsFlow, notesFlow, _query) → stateIn
  │  - NotesViewModel: noteRepository.getAllNotes().stateIn
  │  - ExportViewModel/ImportViewModel: MutableStateFlow<ExportState/ImportState>（一次性动作状态机）
  │  - TimeVizViewModel/RecordingViewModel: MutableStateFlow（非 DB 驱动，同步状态）
  ▼
Compose UI (collectAsStateWithLifecycle)
  │  - 生命周期感知订阅
  │  - config change 不重订阅
  ▼
渲染
```

文字说明：单向数据流，DB 变化自动驱动 UI 刷新（Room invalidation tracker → Flow emit → ViewModel `stateIn` → Compose 重组）。`WhileSubscribed(5000)` 提供 5s grace period，配置变更（如旋转）不重启上游。`ExportViewModel` / `ImportViewModel` 走 `MutableStateFlow` + `getAllEvents().first()` 一次性取数，不接持续 Flow（导出/导入是一次性动作）。

##### 视角 3：状态管理

描述：

- **ViewModel + StateFlow/Flow**：所有 ViewModel 暴露 `state: StateFlow<XxxUiState>` 或具名 `StateFlow`（如 `events` / `notes` / `allNotes`），UI 用 `collectAsStateWithLifecycle` 订阅。
- **WhileSubscribed(5000) 缓存**：DB 驱动的上游 Flow 经 `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), initialValue)` 缓存，5s grace period 避免短时间取消重订阅。涉及：`TimelineViewModel.events` / `TimelineViewModel.notes`、`HeatmapViewModel.state`、`HeatmapYearViewModel.state`、`SearchViewModel.state`（+ 内部 `eventsFlow`/`notesFlow`）、`NotesViewModel.allNotes`。
- **sealed state 状态机**：
  - `TimelineItem` sealed interface（`EventItem` / `NoteItem`，private 于 `TimelineScreen.kt`，按 `sortKey` 合并排序）。
  - `SearchItem` sealed interface（`EventItem` / `NoteItem`，定义于 `SearchViewModel`）。
  - `ExportState` sealed interface（`Idle` / `Exporting` / `Success(fileName)` / `Error(message)`，定义于 `ExportViewModel`）。
  - `ImportState` sealed interface（`Idle` / `Importing` / `Success(events,notes,prefsUpdated)` / `Error(message)`，定义于 `ImportViewModel`）。
  - `HeatmapCalculator.Cell` data class（`level: Int` 0..4 色阶，非 sealed，但承担色阶状态语义）。
- **flatMapLatest**：`TimelineViewModel` / `HeatmapViewModel` / `HeatmapYearViewModel` 用 `flatMapLatest` 切换上游（`_viewingDate`+`_refreshTrigger` / `_selectedMonth` / `_selectedYear` 变化时取消旧订阅启新订阅）。
- **纯函数抽离**（internal，无 Android 依赖，纯 JUnit 可测）：
  - `aggregateMonth` / `aggregateYear` / `effectiveDurationMs`（`EventRepository.kt` 顶层 internal 函数，热力图月/年聚合 + 跨日时长截断）。
  - `filterAndMerge`（`SearchViewModel.kt` 顶层 internal 函数，搜索过滤+合并+排序）。
  - `HeatmapCalculator`（object：`buildGrid` / `buildYearGrid` / `levelFor`，月历 6×7 网格 + 色阶映射）。
  - `TimeVizCalculator`（object：`todayRemaining` / `yearRemaining` / `lifeRemaining`，时间可视化计算）。

##### 视角 4：导航图

`Routes` object 8 常量（已核对 `navigation/AppNavHost.kt`）：`TIMELINE` / `NOTES` / `TIMEVIZ` / `HEATMAP` / `HEATMAP_YEAR` / `SEARCH` / `SETTINGS` / `ABOUT`。`startDestination = Routes.TIMELINE`。

跳转关系（已核对 `AppNavHost` 内 `navController.navigate(...)` 与 `popBackStack()`）：

- `TIMELINE` → `NOTES` / `TIMEVIZ` / `HEATMAP` / `SEARCH` / `SETTINGS`（5 个出口，分别由 `onNotesClick` / `onTimeVizClick` / `onHeatmapClick` / `onSearchClick` / `onSettingsClick` 触发）。
- `HEATMAP` → `HEATMAP_YEAR`（`onYearClick`）；`HEATMAP` 点击日期 → 写 `TIMELINE` 的 `savedStateHandle["heatmap_target_date"]` 后 `popBackStack()` 回 `TIMELINE`（`onDateClick`）。
- `SETTINGS` → `ABOUT`（`onAboutClick`）。
- `NOTES` / `TIMEVIZ` / `SEARCH` / `HEATMAP_YEAR` / `ABOUT` → `popBackStack()` 返回上级。

ASCII art：

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

文字说明：8 路由，`TIMELINE` 是启动页。`HEATMAP` → `HEATMAP_YEAR` 是层级跳转；`SETTINGS` → `ABOUT` 是层级跳转；`SEARCH` 从 `TIMELINE` 顶栏进入。其余叶子路由返回上级用 `popBackStack()`。

#### §4.1.3 依赖注入节具体内容

- **入口**：`ShiJiBenApplication` 标 `@HiltAndroidApp`；`MainActivity` 标 `@AndroidEntryPoint`；所有 ViewModel 标 `@HiltViewModel` + `@Inject constructor`。
- **3 个 Hilt Module**（均 `@InstallIn(SingletonComponent::class)`）：
  - `data/DataModule.kt`：`provideAppDatabase`（`@Singleton`，`Room.databaseBuilder` + `addMigrations(MIGRATION_1_2)`，db 名 `"shijiben.db"`）、`provideEventDao` / `provideNoteDao`。
  - `di/DispatchersModule.kt`：`@IoDispatcher` qualifier（`@Retention(BINARY)`）+ `provideIoDispatcher` 返回 `Dispatchers.IO`。**注：项目仅此一个 dispatcher qualifier，无 `@DefaultDispatcher`。**
  - `feature/timeviz/TimeVizModule.kt`：`provideTimeVizSharedPreferences`（`@Singleton`，`timeviz_prefs`）、`provideTimeVizPrefs`（`@Singleton`）、`provideClock`（`@Singleton`，`Clock.systemDefaultZone()`，注入 `TimeVizViewModel` 替代墙钟便于测试）。
- **消费方**：`ExportViewModel` / `ImportViewModel` 注入 `@IoDispatcher`（导出/导入 IO 在该 dispatcher 执行）；`TimeVizViewModel` 注入 `Clock` + `TimeVizPrefs`。

#### §4.1.4 关键设计模式节具体内容

1. **Repository 模式**：UI 不直接访问 DAO，经 `EventRepository` / `NoteRepository`。Repository `@Singleton`，`@Inject constructor(dao)`，暴露 Flow（查询）+ suspend（写操作）。
2. **纯函数抽离**：`aggregateMonth` / `aggregateYear` / `effectiveDurationMs` / `filterAndMerge` / `HeatmapCalculator` / `TimeVizCalculator` 均无 Android 依赖，纯 JUnit 可测（对应 `EventRepositoryHeatmapTest` / `SearchFilterTest` / `HeatmapCalculatorTest` / `TimeVizCalculatorTest`）。
3. **sealed interface 状态机**：`TimelineItem` / `SearchItem`（列表项多态合并）、`ExportState` / `ImportState`（一次性动作状态机，`Idle → Exporting/Importing → Success/Error → resetState() → Idle`）。
4. **8-bit 美学集中**：所有视觉元素集中在 `ui/theme`——`AppColors`（高饱和彩虹色板 + `HeatmapLevel0..4` 绿色色阶 + `RainbowHourColors` 8 色循环）、`AppTheme`（强制浅色 + Fusion Pixel 字体 + 全 0.dp 直角 `PixelShapes`）、`PixelComponents`（`PixelCard` 等复用组件）。feature 层只消费色板/组件，不自定义视觉常量。
5. **flaky test 根治方案 A**（迭代 9 落地，仅测试侧改动，生产代码零改动）：根因为 `WhileSubscribed(5000)` grace period 内 Room invalidation tracker 跑在真实 executor 线程 + `Dispatchers.resetMain()` 后命中 `NoopDispatcher`。方案 A 在 `HeatmapViewModelTest.setup()` 用 `setQueryExecutor` / `setTransactionExecutor` 把 Room executor 桥接到 `StandardTestDispatcher`，teardown 后队列静止不再 emit，竞态从根上消除。详见 `docs/superpowers/specs/2026-06-28-flaky-rootfix-and-docs-design.md`。

#### §4.1.5 Room schema 节具体内容（可并入数据流或单列）

- `AppDatabase`：`@Database(entities=[EventEntity, NoteEntity], version=2, exportSchema=true)`，schema 落 `app/schemas/`（`1.json` / `2.json`）。
- 两表：`events`（id/title/startTime/endTime/status/note/createdAt/updatedAt）+ `notes`（id/content/timestamp/createdAt/updatedAt）。
- `EventStatus` enum：`NotStarted(0)` / `InProgress(1)` / `Completed(2)`，`status` 列存 Int。
- `MIGRATION_1_2`：v1→v2 移除标签功能（建新表-拷数据-删旧-改名重建索引，删 `tags` 表）。
- 索引：`index_events_start_time` / `index_events_status_start`。

#### §4.1.6 文档长度预估

`ARCHITECTURE.md` 约 150-200 行（4 视角各 30-40 行 + 概述 + DI + 设计模式 + Room schema）。

### §4.2 改动文件 `AGENT.md`

在 `AGENT.md` 的「项目概述」节末尾（即「事记本是一个纯本地的时间记录应用……」段落之后、「## 技术栈」之前）加一行链接：

```markdown
> 架构详情见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)（模块依赖图 + 数据流 + 状态管理 + 导航图 4 视角）。
```

仅这一行，不动其他内容。位置选择理由：「项目概述」节末尾是最高可发现性位置（读者第一屏即可见），且不打断「技术栈」/「项目结构」原有结构。

### §4.3 不改动文件清单（复核）

- 所有 `.kt` 文件：零改动。
- `build.gradle.kts` / `settings.gradle.kts`：零改动。
- `AndroidManifest.xml`：零改动。
- 其他 spec / `README.md` / `MY_ORIGIN_GOAL.md` / `iteration-log.md` / `quality-scorecard.md`：零改动（Persist 阶段 orchestrator 自维护）。

## 五、涉及文件清单

| 类型 | 文件 | 改动 |
|------|------|------|
| 新增 | `docs/ARCHITECTURE.md` | 4 视角架构文档（约 150-200 行，中文，ASCII art） |
| 改动 | `AGENT.md` | 加 1 行链接指向 `docs/ARCHITECTURE.md`（「项目概述」节末尾） |

## 六、验证标准

1. 四道门全绿（预期零回归，纯文档改动）：
   - `./gradlew :app:compileDebugKotlin`
   - `./gradlew :app:testDebugUnitTest --rerun-tasks`
   - `./gradlew assembleDebug`
   - `./gradlew :app:assembleRelease --rerun-tasks`
2. `ARCHITECTURE.md` 4 视角齐全（模块依赖图 + 数据流 + 状态管理 + 导航图）。
3. ASCII art 图示清晰（用代码块包裹，不用 Mermaid）。
4. 符号级引用（用类名/函数名/文件名，不用行号）。
5. `AGENT.md` 加 1 行链接，位置合理（「项目概述」节末尾，「技术栈」前），相对路径 `docs/ARCHITECTURE.md` 正确。
6. 内容与代码一致（Coding subagent 实读代码后落地，Test subagent 独立核对）。

## 七、风险评估

- **风险 1：内容与代码不一致** —— 缓解：Design 阶段已实读代码 + Coding 阶段二次核对 + Test 阶段独立验证。
- **风险 2：ASCII art 渲染问题** —— 缓解：用代码块包裹，等宽字体对齐，避免使用易错位的全角字符。
- **风险 3：文档过长** —— 缓解：控制在 150-200 行，每视角 30-40 行。
- **风险 4：范围蔓延** —— 缓解：硬约束本轮仅 2 文件改动，不借机改其他文档/spec。

## 八、预估加分

+1（文档可维护性 9→10，总分 99→100）。来源：架构文档补齐 4 视角统一描述，新人/onboarding/AI agent 可快速理解整体架构。诚实打分，不夸大。

## 九、Coding subagent 注意事项

1. **必须实读代码**，不准照抄本 spec 中的 ASCII art（spec 是设计，Coding 落地时要核对实际代码；本 spec 的符号引用已核对，但仍须二次确认）。
2. **`ARCHITECTURE.md` 用中文撰写**（与 `README.md` / `AGENT.md` 一致）。
3. **ASCII art 用代码块包裹**，确保等宽渲染；避免全角字符破坏对齐。
4. **符号级引用**：用类名/函数名/文件名，不用行号。
5. **`AGENT.md` 链接位置**：在「项目概述」节末尾（「事记本是一个纯本地的时间记录应用……」段落之后、「## 技术栈」之前）加一行 blockquote，引用相对路径 `docs/ARCHITECTURE.md`。
6. **不改任何代码文件**，仅新增 `ARCHITECTURE.md` + 改 `AGENT.md` 加一行。
7. **DI 模块数准确**：实际有 3 个 Hilt Module（`DataModule` / `DispatchersModule` / `TimeVizModule`），不是 2 个；实际仅 `@IoDispatcher` 一个 qualifier，无 `@DefaultDispatcher`。
8. **返回类型准确**：`EventRepository.getDailyActivityForMonth` / `getDailyActivityForYear` 返回 `Flow<List<DailyActivity>>`（非 `Map`）。
9. **`HeatmapCalculator.Cell` 是 data class**（`level: Int` 0..4），非 sealed interface；状态机一节须如实描述。

## 十、Test subagent 注意事项

1. 独立重跑四道门（顺序，禁止并行，门2/门4 强制 `--rerun-tasks`）。
2. 独立核对 `ARCHITECTURE.md` 4 视角与实际代码一致（实读代码核对，不信任 Coding 自评）。重点核对：
   - 8 路由常量名与 `AppNavHost` 跳转关系。
   - 3 个 Hilt Module + `@IoDispatcher`。
   - `getDailyActivityForMonth` 返回 `Flow<List<DailyActivity>>`。
   - sealed interface 清单（`TimelineItem` / `SearchItem` / `ExportState` / `ImportState`）+ `HeatmapCalculator.Cell` data class。
   - `AppDatabase version=2` + `MIGRATION_1_2`。
3. 核对 `AGENT.md` 链接位置合理（「项目概述」末尾）+ 相对路径正确。
4. 核对零回归（`.kt` / `build.gradle.kts` / `AndroidManifest.xml` 零改动）。
5. 核对 ASCII art 在代码块内（不破坏 Markdown 渲染）。
