# 时间去向聚合设计

> 日期：2026-06-29
> 来源：`better/direction/report-cycle-13.md` 方向 2
> 状态：已确认设计，待实现

## 一、需求

### 核心诉求

用户原始目标（`MY_ORIGIN_GOAL.md`）："当我想回顾一周自己的生活时，我可以快速的查看自己做了什么，花了多少时间，我能感受到自己的进步，把时间花在正确的事情上。"

当前回看能力：日（时间轴）→ 月/年（热力图覆盖率）。缺少"时间去向"视角——不知道时间具体花在哪些事上。

### 功能定义

按事件标题聚合显示总时长，时间范围可选（本周/本月/本年/全部），水平条形图列表展示，严格"只展示事实"。

### 符合产品哲学

- **记录即审视**：看见时间去向是审视的前提
- **App 反映现实，不评判现实**：只展示时长事实，不评分、不设目标、不排名、不暗示"应该"
- **轻量存在**：用户主动进入才看，不打扰

### 不是什么

- 不是时间管理报表（不评分、不设目标）
- 不是效率分析（不对比、不暗示）
- 不按标签/分类归并（标签已移除，按原始标题聚合）
- 不显示百分比排名（只展示时长）

## 二、架构

### 入口与导航

热力图页（`HeatmapScreen`）重构为 3 tab 容器：

```
HeatmapScreen (tab 容器)
├─ 月 tab  → HeatmapMonthTab（提取自现有 HeatmapScreen 主体）
├─ 年 tab  → HeatmapYearTab（提取自现有 HeatmapYearScreen 主体）
└─ 去向 tab → TimeAllocationTab（新建）
```

- `Routes.HEATMAP` 路由保持，仍是热力图入口
- `Routes.HEATMAP_YEAR` 路由废弃（年视图改为 tab，不再独立路由）
- `AppNavHost` 的 `onYearClick` 回调移除（年视图改为 tab 内切换）
- `HeatmapScreen` 的 `onYearClick` 参数移除

### 新增文件

```
feature/heatmap/
  HeatmapScreen.kt          # 重构为 tab 容器
  HeatmapMonthTab.kt        # 新建：提取现有月视图主体
  HeatmapYearTab.kt         # 新建：提取现有年视图主体（HeatmapYearScreen.kt 废弃删除）
  TimeAllocationTab.kt      # 新建：去向 tab 内容
  TimeAllocationCalculator.kt  # 新建：object 纯函数，按标题聚合
  TimeAllocationViewModel.kt   # 新建：@HiltViewModel
  HeatmapViewModel.kt       # 不变（仅服务月 tab）
  HeatmapYearViewModel.kt   # 不变（仅服务年 tab）
```

### 删除文件

- `HeatmapYearScreen.kt`（内容迁移到 `HeatmapYearTab.kt`）

## 三、组件设计

### TimeAllocationCalculator（object 纯函数）

```kotlin
object TimeAllocationCalculator {
    data class TitleDuration(
        val title: String,
        val totalMs: Long
    )

    enum class TimeRange { WEEK, MONTH, YEAR, ALL }

    /** 按标题聚合 completed 事件的时长，降序返回。 */
    fun aggregate(
        events: List<EventEntity>,
        range: TimeRange,
        now: Long,
        zone: TimeZone
    ): List<TitleDuration>
}
```

- 纯函数，无 Android 依赖，纯 JUnit 可测
- 只聚合 `status == Completed` 的事件（InProgress/NotStarted 无 endTime，时长未确定）
- 时长 = `endTime - startTime`（Completed 必有 endTime）
- 按标题分组求和，降序排列
- 范围过滤：
  - WEEK：本周（周一 00:00 ~ 现在）
  - MONTH：本月（1 日 00:00 ~ 现在）
  - YEAR：本年（1 月 1 日 00:00 ~ 现在）
  - ALL：不过滤

### TimeAllocationViewModel（@HiltViewModel）

```kotlin
@HiltViewModel
class TimeAllocationViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val clock: Clock  // 复用 TimeVizModule 提供的 Clock
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(TimeAllocationCalculator.TimeRange.MONTH)
    val selectedRange: StateFlow<TimeAllocationCalculator.TimeRange> = _selectedRange

    val state: StateFlow<TimeAllocationUiState> = _selectedRange
        .map { range -> loadAggregation(range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimeAllocationUiState.Loading)

    fun selectRange(range: TimeAllocationCalculator.TimeRange) {
        _selectedRange.value = range
    }

    private suspend fun loadAggregation(range: TimeRange): TimeAllocationUiState {
        val events = eventRepository.getAllEvents().first()
        val now = clock.millis()
        val zone = clock.zone
        val items = TimeAllocationCalculator.aggregate(events, range, now, zone)
        return TimeAllocationUiState.Success(items)
    }
}
```

- 复用 `Clock`（TimeVizModule 已提供 `@Singleton Clock.systemDefaultZone()`）
- 非持续 Flow：`getAllEvents().first()` 一次性取数（导出/导入同款模式）
- `stateIn(WhileSubscribed(5000))` 与其他 ViewModel 一致

### TimeAllocationUiState

```kotlin
sealed interface TimeAllocationUiState {
    data object Loading : TimeAllocationUiState
    data class Success(val items: List<TimeAllocationCalculator.TitleDuration>) : TimeAllocationUiState
}
```

### TimeAllocationTab（Composable）

布局：

```
┌─────────────────────────────────────┐
│  [本周] [本月] [本年] [全部]        │ ← 范围选择器（PixelOutlinedButton 风格）
├─────────────────────────────────────┤
│  阅读     ████████░░  2h 30m         │ ← 水平条形图行
│  运动     ███░░░░░░░  45m            │
│  工作     ████████████ 8h 00m         │
│  ...                                │
└─────────────────────────────────────┘
```

每行：
- 标题（左，`Modifier.weight(1f)`，`TextPrimary`，`FontWeight.Bold`）
- 像素方块条（中，`Modifier.weight(2f)`）：8-bit 风格，背景 `Surface`，前景 `Primary` 色按占比填充
- 时长（右，`TextSecondary`，等宽显示 `Xh Ym`）

时长格式化：复用现有 `formatDurationShort`（如果存在）或新建简单格式化函数。

## 四、HeatmapScreen 重构

### 现状

`HeatmapScreen` 当前是月视图单页，`MonthSwitcher` 内"年"按钮跳转到 `HeatmapYearScreen`（独立路由）。

### 重构后

`HeatmapScreen` 成为 tab 容器：

```kotlin
@Composable
fun HeatmapScreen(onBack: () -> Unit, onDateClick: (Triple<Int,Int,Int>) -> Unit) {
    var selectedTab by remember { mutableIntStateOf(0) }  // 0=月, 1=年, 2=去向
    Box(modifier = Modifier.fillMaxWidth().background(Background)) {
        Column {
            RainbowTrim()
            // 顶栏：返回 + "回看"标题
            TopBar(onBack = onBack)
            // tab 切换栏
            TabRow(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            // tab 内容
            when (selectedTab) {
                0 -> HeatmapMonthTab(onDateClick = onDateClick)
                1 -> HeatmapYearTab(onDateClick = onDateClick)
                2 -> TimeAllocationTab()
            }
        }
    }
}
```

- `onYearClick` 参数移除（年视图改为 tab）
- tab 切换栏：3 个 tab，8-bit 风格（选中 = `Primary` 背景 + 白字，未选中 = `Surface` 背景 + `TextPrimary`）

### HeatmapMonthTab

提取自现有 `HeatmapScreen` 主体（MonthSwitcher + WeekHeader + HeatmapGrid + Legend）。

### HeatmapYearTab

提取自现有 `HeatmapYearScreen` 主体（YearSwitcher + 12 月迷你月历拼贴）。

## 五、数据流

```
用户选范围（WEEK/MONTH/YEAR/ALL）
  │
  ▼  _selectedRange.value = range
TimeAllocationViewModel.state (map { loadAggregation(range) })
  │
  ▼  eventRepository.getAllEvents().first()  ← 一次性取数
  │  clock.millis() / clock.zone
  │
  ▼  TimeAllocationCalculator.aggregate(events, range, now, zone)
  │  → 过滤 range + 过滤 Completed + 按 title 分组求和 + 降序
  │
  ▼  List<TitleDuration>
  │
  ▼  stateIn(WhileSubscribed(5000))
  │
  ▼  collectAsStateWithLifecycle
  │
  ▼  TimeAllocationTab 渲染水平条形图列表
```

## 六、测试策略

### TimeAllocationCalculator 单测（纯 JUnit）

- `aggregate_completedEvents_sumsByTitle`
- `aggregate_rangeWeek_filtersToCurrentWeek`
- `aggregate_rangeMonth_filtersToCurrentMonth`
- `aggregate_rangeYear_filtersToCurrentYear`
- `aggregate_rangeAll_noFilter`
- `aggregate_ignoresInProgressAndNotStarted`
- `aggregate_emptyEvents_returnsEmpty`
- `aggregate_descendingOrder`
- `aggregate_crossDayEvent_usesFullDuration`

### TimeAllocationViewModel 单测

- `selectRange_updatesState`
- `state_initial_loadsMonthRange`

### 现有测试不破坏

- `HeatmapViewModelTest` 不受影响（HeatmapViewModel 不变）
- `HeatmapYearViewModelTest` 不受影响（HeatmapYearViewModel 不变，仅 Screen 改 Tab）
- gate 全绿

## 七、不做（YAGNI + 哲学约束）

- 不点击查看详情（后续迭代再加）
- 不评分、不设目标、不排名
- 不分类归并（按原始标题）
- 不显示百分比
- 不做时长时间区间选择器（只 4 预设范围）
- 不缓存聚合结果（每次切范围重新计算，数据量小可接受）

## 八、迁移计划

### 步骤

1. 新建 `TimeAllocationCalculator.kt` + 单测
2. 新建 `TimeAllocationViewModel.kt` + 单测
3. 新建 `TimeAllocationTab.kt`（Composable）
4. 新建 `HeatmapMonthTab.kt`（提取现有 HeatmapScreen 主体）
5. 新建 `HeatmapYearTab.kt`（提取现有 HeatmapYearScreen 主体）
6. 重构 `HeatmapScreen.kt` 为 tab 容器
7. 删除 `HeatmapYearScreen.kt`
8. 更新 `AppNavHost.kt`：移除 `HEATMAP_YEAR` 路由 + `onYearClick` 回调
9. 更新 `MainActivity.kt` / 导航图：移除 `onYearClick` 传参
10. gate 验证

### 影响范围

- `feature/heatmap/`：主要改动区
- `navigation/AppNavHost.kt`：移除 HEATMAP_YEAR 路由
- `feature/timeline/TimelineScreen.kt`：`onHeatmapClick` 不变（仍跳 HEATMAP）
- 其他 feature：不受影响
