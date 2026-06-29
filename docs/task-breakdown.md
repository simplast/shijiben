## Development Task Breakdown — 时间去向聚合（Heatmap 3-tab + TimeAllocation）

> Prerequisites: Read AGENT.md for project context. Refer to changes.md for scope.
> 设计依据：`docs/superpowers/specs/2026-06-29-time-allocation-design.md`

### T-01 新建 TimeAllocationCalculator + 单测
**Priority:** P0
**Status:** 🟢 pure frontend（纯 Kotlin，无 Android 依赖）
**Depends on:** 无

**Content:** 新建 `TimeAllocationCalculator` object 纯函数，按标题聚合 completed 事件时长，支持 4 种时间范围过滤。

**Implementation guidance:**
- 在 `app/src/main/java/com/shijiben/feature/heatmap/` 新建 `TimeAllocationCalculator.kt`
- 定义 `object TimeAllocationCalculator`
- 定义 `data class TitleDuration(val title: String, val totalMs: Long)`
- 定义 `enum class TimeRange { WEEK, MONTH, YEAR, ALL }`
- 定义 `fun aggregate(events: List<EventEntity>, range: TimeRange, now: Long, zone: TimeZone): List<TitleDuration>`
- 实现逻辑：
  - 过滤 `events` 只保留 `status == EventStatus.Completed.value`（Completed 必有 endTime）
  - 按 `range` 过滤 startTime：
    - WEEK：计算本周一 00:00 的 epoch millis（用 `zone` + `now` 推算），过滤 `startTime >= weekStart`
    - MONTH：本月 1 日 00:00 的 epoch millis，过滤 `startTime >= monthStart`
    - YEAR：本年 1 月 1 日 00:00 的 epoch millis，过滤 `startTime >= yearStart`
    - ALL：不过滤
  - 按 `title` 分组，`totalMs = sum(endTime - startTime)`
  - 按 `totalMs` 降序排列
  - 返回 `List<TitleDuration>`
- import `com.shijiben.data.local.EventEntity` + `com.shijiben.data.model.EventStatus` + `java.time.TimeZone` + `java.time.YearMonth` / `java.time.LocalDate` / `java.time.ZonedDateTime`（按需）
- 新建 `app/src/test/java/com/shijiben/feature/heatmap/TimeAllocationCalculatorTest.kt`，覆盖 9 个测试用例（见 spec §六）

**Verification:** 单测全绿；`aggregate` 纯函数无 Android 依赖，可用纯 JUnit 运行

**Files involved:** `TimeAllocationCalculator.kt`, `TimeAllocationCalculatorTest.kt`

---

### T-02 新建 TimeAllocationViewModel + 单测
**Priority:** P0
**Status:** 🟡 partial backend（需 Hilt + Clock 注入）
**Depends on:** T-01

**Content:** 新建 `TimeAllocationViewModel`，管理范围选择 + 聚合数据加载。

**Implementation guidance:**
- 在 `app/src/main/java/com/shijiben/feature/heatmap/` 新建 `TimeAllocationViewModel.kt`
- 标注 `@HiltViewModel` + `class TimeAllocationViewModel @Inject constructor(private val eventRepository: EventRepository, private val clock: Clock) : ViewModel()`
- 定义 `sealed interface TimeAllocationUiState { data object Loading; data class Success(val items: List<TitleDuration>) }`
- 定义 `private val _selectedRange = MutableStateFlow(TimeRange.MONTH)`
- 定义 `val state: StateFlow<TimeAllocationUiState> = _selectedRange.map { loadAggregation(it) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Loading)`
- 定义 `fun selectRange(range: TimeRange) { _selectedRange.value = range }`
- 定义 `private suspend fun loadAggregation(range: TimeRange): TimeAllocationUiState`：
  - `val events = eventRepository.getAllEvents().first()`
  - `val now = clock.millis()` + `val zone = clock.zone`
  - `val items = TimeAllocationCalculator.aggregate(events, range, now, zone)`
  - 返回 `Success(items)`
- import `javax.inject.Inject` + `androidx.lifecycle.ViewModel` + `androidx.lifecycle.viewModelScope` + `kotlinx.coroutines.flow.*` + `java.time.Clock` + `com.shijiben.data.repository.EventRepository`
- 参考 `feature/timeviz/TimeVizViewModel.kt` 的 Clock 注入模式
- 新建 `TimeAllocationViewModelTest.kt`：fake EventRepository + `Clock.fixed(instant, zone)`，测 `selectRange_updatesState` + `state_initial_loadsMonthRange`
- 参考 `feature/recording/RecordingViewModelTest.kt` 的 MainCoroutineRule 模式

**Verification:** 单测全绿；ViewModel 注入 Clock 可测试（不用墙钟）

**Files involved:** `TimeAllocationViewModel.kt`, `TimeAllocationViewModelTest.kt`

---

### T-03 新建 TimeAllocationTab Composable
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-02

**Content:** 新建 `TimeAllocationTab` Composable，渲染范围选择器 + 水平条形图列表。

**Implementation guidance:**
- 在 `app/src/main/java/com/shijiben/feature/heatmap/` 新建 `TimeAllocationTab.kt`
- 定义 `@Composable fun TimeAllocationTab(viewModel: TimeAllocationViewModel = hiltViewModel())`
- `val range by viewModel.selectedRange.collectAsStateWithLifecycle()`
- `val state by viewModel.state.collectAsStateWithLifecycle()`
- 布局：`Column` 垂直排列
  - 顶部范围选择器 `Row`：4 个 `PixelOutlinedButton`（本周/本月/本年/全部），选中态用 `backgroundColor = Primary`，未选中用 `Surface`
  - 分隔线 `Box(height=2.dp, background=Color.Black)`
  - 内容区 `when (state)`：
    - `Loading` → 居中 `Text("加载中…")`
    - `Success` → `LazyColumn` 渲染 `items.forEach { AllocationRow(it, maxMs) }`
- `AllocationRow` 私有 Composable：
  - `Row`：标题（`weight(1f)`, `TextPrimary`, `FontWeight.Bold`）+ 像素方块条（`weight(2f)`：背景 `Surface` + 前景 `Primary` 按 `totalMs/maxMs` 宽度填充）+ 时长（`TextSecondary`，格式 `Xh Ym`）
  - 时长格式化：`val hours = totalMs / 3600_000; val mins = (totalMs % 3600_000) / 60_000`，显示 `"${hours}h ${mins}m"`（若 hours=0 只显示分）
- import `com.shijiben.ui.theme.*`（Primary/Surface/TextPrimary/TextSecondary）+ `PixelOutlinedButton`
- 参考 `HeatmapScreen.kt` 的顶栏/Legend 风格

**Verification:** 编译通过；UI 渲染无崩溃（需手动验证或仪器测试）

**Files involved:** `TimeAllocationTab.kt`

---

### T-04 新建 HeatmapMonthTab（提取自 HeatmapScreen 主体）
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** 无（可与 T-01/T-02/T-03 并行）

**Content:** 将现有 `HeatmapScreen.kt` 的 MonthSwitcher + WeekHeader + HeatmapGrid + Legend 主体提取为独立 `HeatmapMonthTab` Composable。

**Implementation guidance:**
- 在 `app/src/main/java/com/shijiben/feature/heatmap/` 新建 `HeatmapMonthTab.kt`
- 定义 `@Composable fun HeatmapMonthTab(onDateClick: (Triple<Int,Int,Int>) -> Unit, viewModel: HeatmapViewModel = hiltViewModel())`
- `val state by viewModel.state.collectAsStateWithLifecycle()`
- 迁移 `HeatmapScreen.kt` 的 `MonthSwitcher` + `WeekHeader` + `HeatmapGrid` + `DayCell` + `Legend` + `PixelArrowBox` 等私有组件到本文件（或保留在 HeatmapScreen.kt 作为共享私有组件，视情况）
- 布局：`Column` 包含 MonthSwitcher + 2dp 分隔线 + 主体（WeekHeader + HeatmapGrid + Legend，可滚动）
- 保持 `onDateClick` 回调透传

**Verification:** 月视图功能与重构前完全一致（视觉 + 交互）

**Files involved:** `HeatmapMonthTab.kt`, `HeatmapScreen.kt`（移除迁移部分）

---

### T-05 新建 HeatmapYearTab（提取自 HeatmapYearScreen 主体）
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** 无（可与 T-04 并行）

**Content:** 将现有 `HeatmapYearScreen.kt` 的主体提取为 `HeatmapYearTab` Composable。

**Implementation guidance:**
- 在 `app/src/main/java/com/shijiben/feature/heatmap/` 新建 `HeatmapYearTab.kt`
- 定义 `@Composable fun HeatmapYearTab(onDateClick: (Triple<Int,Int,Int>) -> Unit, viewModel: HeatmapYearViewModel = hiltViewModel())`
- `val state by viewModel.state.collectAsStateWithLifecycle()`
- 迁移 `HeatmapYearScreen.kt` 的 YearSwitcher + 12 月迷你月历拼贴等主体到本文件
- 移除原 `onBack` 参数（tab 内不需要返回按钮，返回由 HeatmapScreen 顶栏统一处理）
- 保持 `onDateClick` 回调透传（年视图点日期方块仍走原逻辑：popBackStack 回 TIMELINE 带 heatmap_target_date）

**Verification:** 年视图功能与重构前完全一致（视觉 + 交互）

**Files involved:** `HeatmapYearTab.kt`, `HeatmapYearScreen.kt`（待删除）

---

### T-06 重构 HeatmapScreen 为 tab 容器
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-03, T-04, T-05

**Content:** 重构 `HeatmapScreen` 为 3 tab 容器（月/年/去向）。

**Implementation guidance:**
- 修改 `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt`
- 函数签名改为 `fun HeatmapScreen(onBack: () -> Unit, onDateClick: (Triple<Int,Int,Int>) -> Unit)`——移除 `onYearClick` 参数
- 主体改为：
  - `Box(Background)` 包裹 `Column`
  - `RainbowTrim()`
  - 顶栏：返回按钮 + "回看"标题（保留现有 TopBar 逻辑）
  - 2dp 黑色分隔线
  - `TabRow`：3 个 tab（"月"/"年"/"去向"），8-bit 风格（选中=Primary 背景+白字，未选中=Surface 背景+TextPrimary）
  - `when (selectedTab)`：
    - 0 → `HeatmapMonthTab(onDateClick = onDateClick)`
    - 1 → `HeatmapYearTab(onDateClick = onDateClick)`
    - 2 → `TimeAllocationTab()`
- `var selectedTab by remember { mutableIntStateOf(0) }`
- 移除原 MonthSwitcher/HeatmapGrid/Legend 等已迁移到 Tab 的代码
- 保留共享私有组件（如 PixelArrowBox）若多个 Tab 共用，否则迁移到对应 Tab 文件

**Verification:** 3 tab 切换正常；月/年视图功能不变；去向 tab 渲染聚合数据

**Files involved:** `HeatmapScreen.kt`

---

### T-07 更新 AppNavHost 移除 HEATMAP_YEAR 路由
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-06

**Content:** 移除 `Routes.HEATMAP_YEAR` 路由 + `onYearClick` 回调。

**Implementation guidance:**
- 修改 `app/src/main/java/com/shijiben/navigation/AppNavHost.kt`
- 移除 `const val HEATMAP_YEAR = "heatmap_year"` 常量（Routes object 内）
- 移除 `import com.shijiben.feature.heatmap.HeatmapYearScreen`
- 移除 `composable(Routes.HEATMAP_YEAR) { HeatmapYearScreen(...) }` 块
- 修改 HeatmapScreen composable：移除 `onYearClick = { navController.navigate(Routes.HEATMAP_YEAR) }` 传参
- 确认无其他地方引用 `Routes.HEATMAP_YEAR`

**Verification:** 编译通过；导航到热力图页正常；年视图通过 tab 切换可达

**Files involved:** `AppNavHost.kt`

---

### T-08 删除 HeatmapYearScreen.kt
**Priority:** P1
**Status:** 🟢 pure frontend
**Depends on:** T-05, T-07

**Content:** 删除已废弃的 `HeatmapYearScreen.kt`。

**Implementation guidance:**
- 确认 `HeatmapYearScreen.kt` 内容已完全迁移到 `HeatmapYearTab.kt`
- 确认无 import 引用 `HeatmapYearScreen`（T-07 已移除 AppNavHost 的 import）
- 删除 `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt`

**Verification:** 编译通过；无悬空引用

**Files involved:** `HeatmapYearScreen.kt`（删除）

---

### T-09 更新 AGENT.md + ARCHITECTURE.md
**Priority:** P1
**Status:** 🟢 pure frontend
**Depends on:** T-06, T-07

**Content:** 更新项目文档反映新的热力图 3-tab 结构。

**Implementation guidance:**
- 修改 `AGENT.md`「项目结构」节：在 `feature/heatmap/` 注释补充 3-tab 结构说明
- 修改 `docs/ARCHITECTURE.md` §2 包路径列表：补充新增文件（TimeAllocationCalculator/TimeAllocationViewModel/TimeAllocationTab/HeatmapMonthTab/HeatmapYearTab），移除 HeatmapYearScreen
- 修改 `docs/ARCHITECTURE.md` §5 导航图：移除 HEATMAP_YEAR 节点，HEATMAP 内含 3 tab

**Verification:** 文档与代码一致

**Files involved:** `AGENT.md`, `docs/ARCHITECTURE.md`

---

### T-10 gate 验证 + 提交
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-08, T-09

**Content:** 运行四道 gate 验证 + 提交全部改动。

**Implementation guidance:**
- 运行 gate：`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`
- 确认已知 flaky failures（HeatmapYearViewModelTest / TimelineViewModelTest 的 TestMainDispatcher IllegalStateException）作为 PASS
- 确认新增 TimeAllocationCalculatorTest + TimeAllocationViewModelTest 全绿
- git add 所有新增/修改/删除文件
- commit message: `feat: 时间去向聚合（热力图 3-tab + 标题聚合水平条形图）`

**Verification:** gate 全绿（已知 flaky 除外）；提交成功

**Files involved:** 全部

---

### Dependency Graph

```
T-01 (Calculator) ─┬─→ T-02 (ViewModel) ──→ T-03 (Tab) ──┐
                    │                                     │
T-04 (MonthTab) ────┼─────────────────────────────────┐   │
                    │                                 │   │
T-05 (YearTab) ─────┼──────────────────────────┐     │   │
                    │                          │     │   │
                    │                  T-06 (HeatmapScreen refactor) ←─┘
                    │                          │
                    │                  T-07 (AppNavHost)
                    │                          │
                    │                  T-08 (Delete YearScreen) ←─ T-05
                    │                          │
                    │                  T-09 (Docs)
                    │                          │
                    └──────────────→ T-10 (Gate + Commit)
```

执行顺序：T-01 → T-02 → T-03；T-04 / T-05 可并行；T-06 依赖 T-03/T-04/T-05；T-07 依赖 T-06；T-08 依赖 T-05/T-07；T-09 依赖 T-06/T-07；T-10 依赖 T-08/T-09。
