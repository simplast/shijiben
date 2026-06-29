# 热力图月视图：回看我把时间花在了哪里

日期：2026-06-28
范围：新增「热力图月视图」回看能力——首页顶栏加入口，新页面以月历方块网格展示每天记录时长占比（5 档绿色色阶），支持上一月/下一月/回到本月切换，点击方块跳回首页对应日期
前置：2026-06-28 首页构图重平衡 spec（顶栏 36dp 一条带 + 8dp 彩虹条 + 2dp 黑分隔线）与 2026-06-28 时间可视化 spec（timeviz 页）均已落地；minSdk 26，可用 `java.time`；`AppDatabase` version 2

## 问题

`MY_ORIGIN_GOAL.md` 第 30-34 行明确写到「回看」的原始设想：

> 回看一周/一月时，想要类似 GitHub 活跃表的视图：每一天是一个方块，如果某天没有记录就是空白方块，如果记录了一半的时间，面积就填一半。
> 未经审视的人生不值得一过，所以记录就是审视的前提。如果我们每做两件事之间，都要犹豫、墨迹、准备很久，就会形成空白。我们的目的就是减少空白。
> 我想要告诉自己：我知道我在做什么，你看，都记录了。

这是原始目标的核心缺口之一：App 目前只回答「今天我做了什么」，不回答「过去这段时间我把时间花在了什么上」。回看是「轻量存在」哲学的关键一环——不是评判、不是打卡 streak，而是让长期记录自然显形，告诉自己「我知道我在做什么」。

## 目标

- **月视图网格**：选定月按月历布局（周一开头，固定 6 行 × 7 列 = 42 格），每格代表一天，用 5 档绿色色阶表达当天记录时长占清醒时间（16h）的比例。
- **月份切换**：上一月 / 下一月 / 回到本月；下一月不超过当前月（未来无意义）。
- **点击方块跳首页**：点击过去或今天的方块 → 返回首页并设为该日期（复用 `TimelineViewModel.setDate` 的等价机制）。
- **跨日事件归属**：归开始日，时长截断到当天 24:00；结束日不显示该事件。
- **进行中事件**：`endTime IS NULL` 的，时长用 `now - startTime`，并 clamp 到当天范围（与首页 `DayProgressBar` 一致）。
- **聚合方案**：Repository 内存聚合——DAO 只返回原始事件 Flow，Repository 用 `java.time` 在内存按本地时区分组聚合。时区绝对正确，跨日逻辑可测。
- **8-bit 美学**：2dp 黑边 + 直角 + 复用 `AppColors` 色板，与全 app 一致。
- **文案中性**：用「记录」而非「成就」，不用「连续打卡」「streak」「满勤」等词。

## 非目标

- **不做年视图**（放 backlog）。
- **不做详情弹窗**：点击方块直接跳首页，不在热力图内弹当日事件列表。
- **不做随笔聚合**：热力图只聚合事件时长，随笔不计入色阶。
- **不联网**：纯本地 Room 查询 + `java.time` 计算。
- **不引入新依赖**：`build.gradle.kts` 不动。
- **不改 DB schema**：`EventDao` 只加查询方法，不改表结构/索引/迁移，`AppDatabase` version 保持 2。
- **不改 `TimelineViewModel` / `TimeViz*` 现有代码**：`TimelineScreen` 仅加入口点击回调 + 接收回看日期的最小 `LaunchedEffect`，不改业务逻辑。
- **不做像素字体**：沿用系统字体（与 timeviz spec 一致）。

## 设计

### 第 1 节 · 数据层

#### 1.1 EventDao 新增查询（仅加方法，不改表）

`EventDao.kt` 新增：

```kotlin
@Query("SELECT * FROM events WHERE startTime >= :startOfMonth AND startTime < :endOfMonth ORDER BY startTime ASC")
fun getEventsByMonth(startOfMonth: Long, endOfMonth: Long): Flow<List<EventEntity>>
```

- 范围左闭右开 `[startOfMonth, endOfMonth)`，与现有 `getEventsByDate` 一致。
- 按 `startTime` 过滤——跨日事件 `startTime` 在上月则不在本月范围，本月查不到（符合「归开始日」原则，结束日不显示）。
- 复用现有 `index_events_start_time` 索引，无需新增索引。

#### 1.2 数据模型

新增 `data/model/HeatmapModels.kt`：

```kotlin
package com.shijiben.data.model

import java.time.LocalDate

/** 单日聚合结果（内存聚合产物，不落库） */
data class DailyActivity(
    val date: LocalDate,       // 本地时区日期
    val eventCount: Int,       // 当天归属的事件数（含 not_started，供未来扩展）
    val durationMs: Long       // 当天有效记录时长（仅 in_progress + completed，跨日已截断）
)
```

- `eventCount` 含 not_started 但 `durationMs` 不含——not_started 是「预写」未占时间，色阶只看 `durationMs`。当前 UI 不显示数字，字段保留供 backlog（详情弹窗）。
- 不落库，不进 Entity。

#### 1.3 EventRepository 新增方法（内存聚合）

`EventRepository.kt` 新增：

```kotlin
fun getDailyActivityForMonth(yearMonth: YearMonth): Flow<List<DailyActivity>> =
    eventDao.getEventsByMonth(monthStartEpoch(yearMonth), monthEndEpoch(yearMonth))
        .map { events -> aggregateMonth(events, yearMonth) }

private fun monthStartEpoch(ym: YearMonth): Long =
    ym.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun monthEndEpoch(ym: YearMonth): Long =
    ym.plusMonths(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
```

#### 1.4 聚合算法（核心，跨日截断 + 进行中 clamp）

```kotlin
internal fun aggregateMonth(
    events: List<EventEntity>,
    yearMonth: YearMonth,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): List<DailyActivity> {
    val nowInstant = Instant.ofEpochMilli(now)
    val byDay = mutableMapOf<LocalDate, Pair<Int, Long>>() // date -> (count, durationMs)
    for (e in events) {
        val eventStart = Instant.ofEpochMilli(e.startTime).atZone(zone)
        val startDay = eventStart.toLocalDate()
        // 仅归属开始日；不在选定月的事件（理论上 DAO 已过滤）跳过
        if (startDay.yearMonth != yearMonth) continue
        // not_started(0) 计入 count 但不计入时长
        val (cnt, dur) = byDay[startDay] ?: (0 to 0L)
        val newCnt = cnt + 1
        val newDur = if (e.status == 0) dur else dur + effectiveDurationMs(e, startDay, now, zone)
        byDay[startDay] = newCnt to newDur
    }
    return byDay.entries.map { (d, pair) ->
        DailyActivity(date = d, eventCount = pair.first, durationMs = pair.second)
    }.sortedBy { it.date }
}

/**
 * 单事件在指定 day 的有效时长（毫秒）。
 * - 跨日事件：截断到当天 24:00（dayEnd）。
 * - 进行中事件（endTime null）：用 now，并 clamp 到 dayEnd。
 * - 归属规则：仅当 eventStart.toLocalDate() == day 才返回非 0。
 */
internal fun effectiveDurationMs(
    event: EventEntity,
    day: LocalDate,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault()
): Long {
    val eventStart = Instant.ofEpochMilli(event.startTime).atZone(zone)
    if (eventStart.toLocalDate() != day) return 0L
    val rawEnd = event.endTime ?: now
    val eventEnd = Instant.ofEpochMilli(rawEnd).atZone(zone)
    val dayEnd = day.plusDays(1).atStartOfDay(zone) // 当天 24:00 = 次日 00:00
    val effectiveEnd = if (eventEnd.isBefore(dayEnd)) eventEnd else dayEnd
    val ms = Duration.between(eventStart, effectiveEnd).toMillis()
    return ms.coerceAtLeast(0L)
}
```

**关键决策**：

- **跨日事件 23:00–次日 01:00**：`eventStart.toLocalDate() = 今天`，归属今天；`effectiveEnd = min(次日01:00, 次日00:00) = 次日00:00`；时长 = 次日00:00 − 23:00 = 1 小时。结束日方块 `effectiveDurationMs` 返回 0（`eventStart.toLocalDate() != day`），且 DAO 查不到（startTime 在昨天）。✓
- **进行中事件跨多天（本月 3 号 14:00 开始未停，now = 本月 5 号 10:00）**：归属 3 号；`effectiveEnd = min(now, 4号00:00) = 4号00:00`；时长 = 4号00:00 − 3号14:00 = 10 小时。clamp 到当天范围，不会把 5 号的时长算进 3 号。✓
- **进行中事件 startTime 在选定月之前（上月 3 号开始未停）**：DAO 查不到（startTime 不在本月范围），本月不显示。符合「归开始日」原则，简单一致。✓
- **not_started 事件**：`status == 0`，计入 `eventCount` 但 `durationMs` 不增——色阶仍按 0（空白方块），符合「预写不算记录时间」。
- **时区**：全部用 `ZoneId.systemDefault()`，与现有 `Calendar.getInstance(TimeZone.getDefault())` 一致；用户跨时区旅行时随系统时区变化，可接受。

### 第 2 节 · HeatmapCalculator（纯函数，易测）

新增 `feature/heatmap/HeatmapCalculator.kt`，无 Android 依赖，普通 JUnit 可测：

```kotlin
package com.shijiben.feature.heatmap

import com.shijiben.data.model.DailyActivity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object HeatmapCalculator {

    /** 16 小时清醒时间（毫秒），色阶分母 */
    const val AWAKE_MS_PER_DAY = 16L * 3600 * 1000

    enum class WeekStart { MONDAY } // 本 spec 固定周一开头

    data class Cell(
        val date: LocalDate,
        val isInMonth: Boolean,      // false = 月初/月末补位（上下月日期）
        val eventCount: Int,
        val durationMs: Long,
        val level: Int,              // 0..4
        val isToday: Boolean,
        val isFuture: Boolean        // 今天之后的日期（含本月与补位）
    )

    /** 色阶映射：ratio = durationMs / 16h */
    fun levelFor(durationMs: Long): Int {
        if (durationMs <= 0) return 0
        val ratio = durationMs.toDouble() / AWAKE_MS_PER_DAY
        return when {
            ratio < 0.25 -> 1
            ratio < 0.50 -> 2
            ratio < 0.75 -> 3
            else -> 4
        }
    }

    /**
     * 构建月历网格：固定 6 行 × 7 列 = 42 格，周一开头。
     * 月初补位用上月末几天（isInMonth=false），月末补位用下月初几天。
     */
    fun buildGrid(
        yearMonth: YearMonth,
        activities: List<DailyActivity>,
        today: LocalDate
    ): List<List<Cell>> {
        val byDate = activities.associateBy { it.date }
        val firstOfMonth = yearMonth.atDay(1)
        // 周一开头：把 Sunday(7) 映射到第 7 列，Monday(1) 到第 1 列
        val firstCol = firstOfMonth.dayOfWeek.value // Monday=1 .. Sunday=7
        val gridStart = firstOfMonth.minusDays((firstCol - 1).toLong())
        return (0 until 6).map { row ->
            (0 until 7).map { col ->
                val date = gridStart.plusDays((row * 7 + col).toLong())
                val act = byDate[date]
                val isToday = date == today
                val isFuture = date.isAfter(today)
                Cell(
                    date = date,
                    isInMonth = date.yearMonth == yearMonth,
                    eventCount = act?.eventCount ?: 0,
                    durationMs = act?.durationMs ?: 0L,
                    level = levelFor(act?.durationMs ?: 0L),
                    isToday = isToday,
                    isFuture = isFuture
                )
            }
        }
    }
}
```

**色阶阈值**（确认 Discover 方案，16h 为分母）：

| level | 条件（ratio = durationMs / 16h） | 含义 |
|---|---|---|
| 0 | `durationMs <= 0` | 无记录（空白） |
| 1 | `0 < ratio < 0.25` | 少量（< 4h） |
| 2 | `0.25 ≤ ratio < 0.50` | 部分（4–8h） |
| 3 | `0.50 ≤ ratio < 0.75` | 大部分（8–12h） |
| 4 | `ratio ≥ 0.75` | 充实（≥ 12h） |

### 第 3 节 · HeatmapViewModel

新增 `feature/heatmap/HeatmapViewModel.kt`：

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HeatmapViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    data class HeatmapUiState(
        val yearMonth: YearMonth = YearMonth.now(),
        val cells: List<List<HeatmapCalculator.Cell>> = emptyList(),
        val isCurrentMonth: Boolean = true,
        val canGoNext: Boolean = false   // 是否还能往未来翻（不超过当前月）
    )

    private val currentMonth = YearMonth.now(ZoneId.systemDefault())

    private val _selectedMonth = MutableStateFlow(currentMonth)
    val selectedMonth: StateFlow<YearMonth> = _selectedMonth.asStateFlow()

    val state: StateFlow<HeatmapUiState> = _selectedMonth
        .flatMapLatest { ym ->
            eventRepository.getDailyActivityForMonth(ym).map { activities ->
                val today = LocalDate.now(ZoneId.systemDefault())
                HeatmapUiState(
                    yearMonth = ym,
                    cells = HeatmapCalculator.buildGrid(ym, activities, today),
                    isCurrentMonth = ym == currentMonth,
                    canGoNext = ym < currentMonth
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeatmapUiState())

    fun previousMonth() { _selectedMonth.value = _selectedMonth.value.minusMonths(1) }
    fun nextMonth() {
        // 下一月不超过当前月（未来无意义）
        if (_selectedMonth.value < currentMonth) {
            _selectedMonth.value = _selectedMonth.value.plusMonths(1)
        }
    }
    fun goToCurrentMonth() { _selectedMonth.value = currentMonth }
}
```

- 进行中事件 `now` 实时性：`eventRepository.getDailyActivityForMonth` 内部用 `System.currentTimeMillis()`，Flow 每次 emit（DB 变化时）重新聚合，`now` 取最新。本 spec 不为「进行中事件每分钟刷新色阶」起定时器（成本高、收益低）；用户切换月份或 DB 变化时自然刷新。backlog 可加 60s 定时 re-collect。

### 第 4 节 · HeatmapScreen

新增 `feature/heatmap/HeatmapScreen.kt`，布局自上而下：

```
┌───────────────────────────────┐
│ [顶部 8dp 彩虹条]              │  ← 与首页统一品牌标识（复用 trimColors 循环）
│ ‹ 回看                         │  ← 顶栏：左返回箭头 + 标题"回看"
│ [2dp 黑色分隔线]               │
│                               │
│ ‹  2026年6月  ›   [本月]       │  ← 月份切换栏：‹ 按钮 + 月份标题 + › 按钮 + 回到本月
│ [2dp 黑色分隔线]               │
│                               │
│  一  二  三  四  五  六  日     │  ← 星期表头（周一开头）
│ ┌─┐┌─┐┌─┐┌─┐┌─┐┌─┐┌─┐        │
│ │ ││ ││ ││ ││ ││ ││ │ 6/1    │  ← 7 列 × 6 行网格，方块 40dp + gap 4dp
│ └─┘└─┘└─┘└─┘└─┘└─┘└─┘        │
│ ... (共 6 行)                  │
│                               │
│ 少 ▢▢▢▢▢ 多                   │  ← 图例：5 个色阶方块由浅到深
└───────────────────────────────┘
```

**关键交互**：
- `‹` / `›` 按钮：26dp 像素方块（2dp 黑边白底 + 黑色三角），`‹` 总可点，`›` 仅 `canGoNext=true` 可点（置灰不可点）。
- 「本月」按钮：`PixelOutlinedButton` 小号（文字 11sp，padding 紧凑），仅 `isCurrentMonth=false` 时可点。
- 方块点击：`isFuture=false` 的方块 `clickable` → 触发 `onDateClick(date)`；`isFuture=true` 不可点（置灰）。
- 补位方块（`isInMonth=false`）：不显示色阶，背景透明，不可点。

**方块样式**（见第 6 节美学）：
- 今天方块：2dp `Primary`（红）边框 + 色阶填充。
- 其他方块：2dp `Color.Black` 边框 + 色阶填充。
- 未来日方块：色阶用 `level 0`（必为空白）+ 边框 `Border`（浅灰）+ `alpha = 0.5f` 置灰，不可点。

**函数签名**：

```kotlin
@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    onDateClick: (Triple<Int, Int, Int>) -> Unit,  // (year, month, day)
    viewModel: HeatmapViewModel = hiltViewModel()
)
```

`onDateClick` 内部把 `LocalDate` 转 `Triple(year, monthValue, dayOfMonth)`。

### 第 5 节 · 入口与导航

#### 5.1 首页顶栏入口（不破坏 36dp 重平衡成果）

**决策**：在顶栏一条带内，日期徽章右侧、统计文案左侧，加一个 26dp 像素热力图小图标按钮。

形态：26dp `Box`，2dp `Color.Black` 边框，白底，内嵌一个 2×2 的小绿方块矩阵（每个小方块 8dp，gap 2dp，用 `HeatmapLevel1`/`HeatmapLevel3` 拼出「迷你热力图」语义）。点击触发 `onHeatmapClick`。

理由：
- 26dp 与 `BottomEntryBar` 图标盒一致，不破坏顶栏 36dp 高度。
- 图标本身即「热力图」语义，可发现性优于纯文字。
- 与日期徽章（左）同属「日期相关」簇，统计文案（右）保持 `clickable → onTimeVizClick` 不变。

`TimelineScreen` 顶栏 `Row` 改动（最小化）：

```kotlin
Row(
    modifier = Modifier.fillMaxWidth().background(Surface),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // 原日期徽章（Box + clickable { showDatePicker = true }）保持不变
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) { /* 徽章 */ }
        // 新增：热力图回看入口
        Box(
            modifier = Modifier
                .size(26.dp)
                .border(2.dp, Color.Black)
                .background(Surface)
                .clickable { onHeatmapClick() },
            contentAlignment = Alignment.Center
        ) { /* 2×2 小绿方块矩阵 */ }
    }
    // 原统计文案 Text(...).clickable { onTimeVizClick() } 保持不变
    Text(...)
}
```

- `TimelineScreen` 新增参数：`onHeatmapClick: () -> Unit = {}`。
- 不动 `TimelineViewModel`。

#### 5.2 AppNavHost 路由

```kotlin
object Routes {
    const val TIMELINE = "timeline"
    const val NOTES = "notes"
    const val TIMEVIZ = "timeviz"
    const val HEATMAP = "heatmap"
}
```

#### 5.3 点击方块跳首页对应日期（方案）

**决策**：用 `savedStateHandle` 在 heatmap 与 timeline backstack entry 间传值；`TimelineScreen` 加最小 `LaunchedEffect` 接收并调 `viewModel.setDate`。不改 `TimelineViewModel`。

`AppNavHost`：

```kotlin
@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.TIMELINE) {
        composable(Routes.TIMELINE) { entry ->
            var pendingDate by remember { mutableStateOf<Triple<Int, Int, Int>?>(null) }
            LaunchedEffect(entry) {
                entry.savedStateHandle
                    .getStateFlow<Triple<Int, Int, Int>?("heatmap_target_date", null)
                    .collect { d ->
                        if (d != null) {
                            pendingDate = d
                            entry.savedStateHandle.remove("heatmap_target_date")
                        }
                    }
            }
            TimelineScreen(
                onNotesClick = { navController.navigate(Routes.NOTES) },
                onTimeVizClick = { navController.navigate(Routes.TIMEVIZ) },
                onHeatmapClick = { navController.navigate(Routes.HEATMAP) },
                targetDate = pendingDate,
                onDateApplied = { pendingDate = null }
            )
        }
        composable(Routes.NOTES) { NotesScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.TIMEVIZ) { TimeVizScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.HEATMAP) {
            HeatmapScreen(
                onBack = { navController.popBackStack() },
                onDateClick = { (y, m, d) ->
                    navController.getBackStackEntry(Routes.TIMELINE)
                        .savedStateHandle["heatmap_target_date"] = Triple(y, m, d)
                    navController.popBackStack()
                }
            )
        }
    }
}
```

`TimelineScreen` 接收侧（最小改动）：

```kotlin
@Composable
fun TimelineScreen(
    onNotesClick: () -> Unit = {},
    onTimeVizClick: () -> Unit = {},
    onHeatmapClick: () -> Unit = {},                                  // 新增
    targetDate: Triple<Int, Int, Int>? = null,                        // 新增
    onDateApplied: () -> Unit = {},                                   // 新增
    viewModel: TimelineViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel()
) {
    // ... 原有逻辑不动 ...
    LaunchedEffect(targetDate) {
        targetDate?.let { (y, m, d) ->
            viewModel.setDate(y, m, d)
            onDateApplied()
        }
    }
    // ... 原有 UI 不动 ...
}
```

- `viewModel.setDate` 已存在，复用，不改 `TimelineViewModel`。
- `LaunchedEffect(targetDate)`：`targetDate` 变化即触发；`onDateApplied` 清空 `pendingDate`，避免重复 `setDate`。
- 从 heatmap `popBackStack` 回到 timeline，timeline backstack entry 重新激活，`LaunchedEffect(entry)` 的 collect 仍在运行（entry 没销毁），收到新值即更新 `pendingDate` → 触发 `LaunchedEffect(targetDate)` → `setDate`。

### 第 6 节 · 8-bit 美学

#### 6.1 色阶色板（在 AppColors 新增热力图专用组）

`ui/theme/AppColors.kt` 新增（集中管理，便于后续调色）：

```kotlin
// ==================== 热力图色阶（5 档绿色，由浅到深） ====================
val HeatmapLevel0 = Surface            // 0xFFFFFFFF 纯白（空白，复用 Surface）
val HeatmapLevel1 = Color(0xFFA7F3D0)  // emerald 200（浅绿）
val HeatmapLevel2 = Color(0xFF34D399)  // emerald 400（中绿）
val HeatmapLevel3 = Secondary          // 0xFF10B981 emerald 500（翠绿，复用身份色）
val HeatmapLevel4 = Color(0xFF047857)  // emerald 700（深绿）
```

- 5 档纯净绿色梯度，8-bit 高饱和；`Level3` 复用 `Secondary` 保持与全 app 「记录=绿」身份一致（与 `TimeBlockRecorded`/`Success` 同色族）。
- `Level0` 复用 `Surface`（白），不新增。

#### 6.2 方块尺寸与边距

- 方块：40dp × 40dp，gap 4dp，网格外边距 8dp。
- 边框：2dp（今天用 `Primary` 红，其他用 `Color.Black`）。
- 直角：`RoundedCornerShape(0.dp)`，全 app 一致。
- 色阶填充：`when (level) { 0 -> HeatmapLevel0; 1 -> HeatmapLevel1; ... }`。

#### 6.3 月份切换按钮样式

- `‹` / `›`：26dp `Box` + 2dp `Color.Black` 边框 + 白底 + 黑色三角 `Icon`（用 `Icons.Default.KeyboardArrowLeft/Right`，`Modifier.size(14.dp)`，`tint = Color.Black`）。与 `BottomEntryBar` 图标盒风格一致。
- 不可点态（`›` 在当前月）：背景 `Color(0xFFCBD5E1)`（灰），`tint = Color(0xFF94A3B8)`，不响应点击。
- 「本月」按钮：`PixelOutlinedButton` 风但小号——2dp `Primary` 边框 + 透明底 + `Primary` 文字 11sp，padding `horizontal=8.dp, vertical=4.dp`。当前月时置灰不可点。

#### 6.4 彩虹条与图例

- 顶部 8dp 彩虹条：复用首页 `trimColors` 循环（与 timeviz spec 一致，可复制 8 行代码，不强求抽 `RainbowTrim` composable）。
- 图例：底部一行，文案「少」+ 5 个 16dp 方块（`HeatmapLevel0..4`，2dp 黑边）+ 文案「多」，水平居中，`TextTertiary` 11sp。

## 涉及文件

| 文件 | 改动 |
|---|---|
| `app/src/main/java/com/shijiben/data/local/EventDao.kt` | **改**：新增 `getEventsByMonth(startOfMonth, endOfMonth): Flow<List<EventEntity>>` 查询方法（仅加方法，不改表/索引/迁移） |
| `app/src/main/java/com/shijiben/data/model/HeatmapModels.kt` | **新增**：`DailyActivity` data class |
| `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` | **改**：新增 `getDailyActivityForMonth(yearMonth)` + `aggregateMonth` + `effectiveDurationMs`（internal，供单测）+ `monthStartEpoch`/`monthEndEpoch` 私有 helper |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapCalculator.kt` | **新增**：纯函数 `Cell` data class + `levelFor` + `buildGrid`，无 Android 依赖 |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapViewModel.kt` | **新增**：`@HiltViewModel` + `HeatmapUiState` + `previousMonth`/`nextMonth`/`goToCurrentMonth` |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` | **新增**：UI（彩虹条 + 顶栏 + 月份切换栏 + 星期表头 + 6×7 网格 + 图例） |
| `app/src/main/java/com/shijiben/ui/theme/AppColors.kt` | **改**：新增 `HeatmapLevel1..4` 色阶常量（`Level0` 复用 `Surface`，`Level3` 复用 `Secondary`，新增 3 个） |
| `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` | **改**：新增 `onHeatmapClick`/`targetDate`/`onDateApplied` 参数 + 顶栏日期徽章右侧 26dp 热力图图标按钮 + 接收 `targetDate` 的 `LaunchedEffect` 调 `viewModel.setDate` |
| `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | **改**：`Routes.HEATMAP` + `composable(Routes.HEATMAP)` + timeline composable 块加 `savedStateHandle` 监听 + 传 `onHeatmapClick`/`targetDate`/`onDateApplied` |
| `app/src/test/java/com/shijiben/feature/heatmap/HeatmapCalculatorTest.kt` | **新增**：纯函数单测（色阶映射/网格构建/补位） |
| `app/src/test/java/com/shijiben/data/repository/EventRepositoryHeatmapTest.kt` | **新增**：`aggregateMonth`/`effectiveDurationMs` 单测（跨日截断/进行中 clamp/not_started/空月/月末） |
| `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt` | **新增**：VM 单测（月份切换/nextMonth 限制/Flow 转换，用 fake `EventRepository` 或直接调 `aggregateMonth`） |

无需改：
- `app/build.gradle.kts`（不引入新依赖）
- `app/src/main/java/com/shijiben/data/local/AppDatabase.kt`（version 保持 2，不加迁移）
- `app/src/main/java/com/shijiben/data/local/EventEntity.kt`（不改表结构）
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`（不改）
- `app/src/main/java/com/shijiben/feature/timeviz/*`（不改）
- 现有任何测试文件

## 边界情况

### 跨日事件
- **23:00–次日 01:00**：归开始日，时长 = 1h（截断到 24:00）；结束日方块不显示该事件（DAO 查不到，`effectiveDurationMs` 返回 0）。✓ 第 1.4 节算法保证。
- **跨多天已完成事件（如 6-28 22:00 – 6-30 03:00）**：仅归 6-28，时长 = 6-29 00:00 − 6-28 22:00 = 2h；6-29、6-30 方块不显示。符合「归开始日」原则，简单一致。

### 进行中事件
- **当天开始未停**：`endTime null`，`now` 作 end，clamp 到当天 24:00。例如 14:00 开始，now=20:00 → 6h。
- **跨多天未停（本月 3 号开始，now = 本月 5 号）**：归 3 号，时长 clamp 到 4 号 00:00 − 3 号 14:00 = 10h。5 号方块不显示该事件。✓
- **上月开始未停**：DAO 查不到（startTime 在上月），本月不显示。符合「归开始日」。
- **not_started 事件**：`status=0`，计入 `eventCount` 但不增 `durationMs`，色阶为 0（空白方块）。若该日只有 not_started 事件，方块仍为空白——符合「预写不算记录时间」。

### 空月
- 全月无事件：所有 `isInMonth` 方块 `level=0`（白），网格正常渲染。图例与切换栏正常。

### 月末边界
- 6-30 23:59 开始的 1 分钟事件：归 6-30，时长 1min，`ratio = 1/(16*3600) ≈ 0.00001 < 0.25` → level 1。
- 月份切换 `monthStartEpoch`/`monthEndEpoch` 用 `YearMonth.atDay(1).atStartOfDay(zone)` 与 `plusMonths(1).atDay(1).atStartOfDay(zone)`，左闭右开，跨月无遗漏无重复。

### 今天高亮
- 今天方块边框用 `Primary`（红）2dp，与 `DayProgressBar` `isNow` 红边一致。
- 今天方块色阶正常按 `durationMs` 计算（进行中事件实时累加，但本 spec 不起定时刷新，切换月份或 DB 变化时刷新；可接受）。

### 未来日
- 本月今天之后的日期：方块 `level=0`（必为空白，因未来无已完成/进行中事件——预写 not_started 不计入时长），边框 `Border`（浅灰），`alpha=0.5f` 置灰，不可点。
- 补位的下月初日期：`isInMonth=false`，背景透明，不可点，无边框。
- `nextMonth` 限制不超过当前月，避免翻到全未来月。

### 闰年 2 月 29
- `YearMonth` 与 `LocalDate` 自动处理闰年；2 月网格正常 28/29 天 + 补位。
- 跨年（12 月 → 1 月）：`YearMonth.minusMonths(1)`/`plusMonths(1)` 自动跨年。

### 点击方块跳首页
- **点击今天**：跳回首页，`setDate(今天)`，首页显示今天。
- **点击过去日**：跳回首页，`setDate(过去日)`，首页该日事件流自动 `flatMapLatest` 重拉。
- **点击未来日**：不可点（置灰），无响应。
- **点击补位方块**（`isInMonth=false`）：不可点，无响应。
- **连续点击多个方块**：每次 `popBackStack` 回首页，再点入口进 heatmap，循环正常。`savedStateHandle` 消费后 `remove`，不会重复 `setDate`。
- **从 heatmap 返回（不点方块，点返回箭头）**：`onBack` 调 `popBackStack`，首页 `viewingDate` 保持跳转前状态（`targetDate` 未设置，`LaunchedEffect` 不触发）。

## 测试

### 验证门
- `./gradlew :app:compileDebugKotlin`：类型检查（含 Hilt KSP）。
- `./gradlew :app:testDebugUnitTest`：全部单测通过（含现有 + 新增）。
- `./gradlew assembleDebug`：构建通过。

### 新增单测清单

#### `HeatmapCalculatorTest`（纯 JUnit，无 Robolectric）

`levelFor`：
1. `durationMs=0` → `0`
2. `durationMs=1`（1ms）→ `1`（>0 即进 level 1）
3. `durationMs=3h`（ratio=0.1875 <0.25）→ `1`
4. `durationMs=4h`（ratio=0.25）→ `2`（边界含左）
5. `durationMs=8h`（ratio=0.50）→ `3`（边界含左）
6. `durationMs=12h`（ratio=0.75）→ `4`（边界含左）
7. `durationMs=16h`（ratio=1.0）→ `4`
8. `durationMs=20h`（ratio=1.25，超 16h）→ `4`（clamp）

`buildGrid`：
9. 2026-06（6-1 是周一）：网格第 1 行第 1 列 = 2026-06-01，`isInMonth=true`；共 6 行 × 7 列 = 42 格。
10. 2026-02（2-1 是周日）：网格第 1 行前 6 列为 1 月末补位（`isInMonth=false`），第 1 行第 7 列 = 2-1。
11. 空月（`activities=[]`）：所有 `isInMonth` 格 `level=0`、`eventCount=0`、`durationMs=0`。
12. 今天高亮：`today=2026-06-28`，对应格 `isToday=true`，其余 `isToday=false`。
13. 未来日：`today=2026-06-28`，6-29 格 `isFuture=true`，6-28 `isFuture=false`，6-27 `isFuture=false`。
14. 补位下月初：2026-06 网格末尾含 7 月初几天，`isInMonth=false`。

#### `EventRepositoryHeatmapTest`（纯 JUnit，测 `aggregateMonth`/`effectiveDurationMs` internal 函数）

`effectiveDurationMs`：
15. 跨日事件 23:00–次日 01:00，day=开始日 → 1h（3600_000ms）；day=结束日 → 0L。
16. 进行中事件 14:00 开始，now=当天 20:00，day=开始日 → 6h。
17. 进行中事件跨多天（3 号 14:00 开始，now=5 号 10:00），day=3 号 → 10h（clamp 到 4 号 00:00）；day=4 号 → 0L；day=5 号 → 0L。
18. not_started 事件（status=0）：`aggregateMonth` 中 `durationMs` 不增，但 `eventCount` +1。

`aggregateMonth`：
19. 空列表 → 空 `List<DailyActivity>`。
20. 单日多事件：同日 2 条 completed + 1 条 not_started → `eventCount=3`，`durationMs`=两条 completed 时长之和。
21. 跨日事件不污染结束日：6-28 23:00–6-29 01:00，`aggregateMonth(2026-06)` 中 6-29 无该事件（DAO 已过滤，但算法层也保证 `startDay != 6-29` 跳过）。
22. 月末边界：6-30 23:59 的 1min 事件 → 6-30 `durationMs=60_000`，`levelFor → 1`。
23. 时区：用固定 `ZoneId.of("Asia/Shanghai")`，事件 2026-06-15T00:00:00+08:00（即 epoch 对应北京 0 点）→ `startDay=2026-06-15`。

#### `HeatmapViewModelTest`（Robolectric，用 fake `EventRepository`）

24. 初始状态：`state.yearMonth == YearMonth.now()`，`isCurrentMonth=true`，`canGoNext=false`。
25. `previousMonth()`：`yearMonth` 减一月，`isCurrentMonth=false`，`canGoNext=true`。
26. 从上月 `nextMonth()`：`yearMonth` 加一月回当前月，`canGoNext=false`。
27. 当前月 `nextMonth()`：静默无变化（`canGoNext=false`），`yearMonth` 不变。
28. `goToCurrentMonth()`（从上月调）：`yearMonth` 回当前月，`isCurrentMonth=true`。
29. `state.cells` 形状：6 行 × 7 列，today 格 `isToday=true`。
30. fake repository emit 新数据后 `state.cells` 对应格 `level` 更新（Flow 响应式）。

### 现有测试影响
- `TimelineViewModelTest`：未改 `TimelineViewModel`，不受影响。
- `EventRepositoryTest`：`EventRepository` 新增方法不破坏现有方法，现有测试应通过；新增 `aggregateMonth` 等为 internal 可独立单测。
- `RecordingViewModelTest` / `NoteRepositoryTest`：未改相关代码，不受影响。
