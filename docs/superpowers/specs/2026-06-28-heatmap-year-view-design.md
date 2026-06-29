# 事记本 迭代 10 设计 spec：热力图年视图（方案 C 12 月迷你月历拼贴）

> 日期：2026-06-28
> 迭代：10（自迭代 loop）
> 范围：1 目标（热力图年视图，年度回看）
> 前置：迭代 1-9 已落地，当前质量评分 94/100，118 个单测全绿（flaky 已根治）；热力图月视图（`HeatmapScreen` / `HeatmapViewModel` / `HeatmapCalculator` / `EventRepository.getDailyActivityForMonth` / `aggregateMonth` / `effectiveDurationMs`）均已就位。
> 验证门（四道，按顺序）：① `./gradlew :app:compileDebugKotlin` → ② `./gradlew :app:testDebugUnitTest --rerun-tasks` → ③ `./gradlew assembleDebug` → ④ `./gradlew :app:assembleRelease`，四道全绿。
> 当前质量评分：94/100；本轮目标 +1~2，核心功能完整度 38→39~40。

---

## 一、问题

迭代 2 已实现热力图**月视图**（`HeatmapScreen` + `HeatmapViewModel` + `HeatmapCalculator.buildGrid` + `EventRepository.getDailyActivityForMonth` + `aggregateMonth`/`effectiveDurationMs`），用户可逐月回看每日记录活跃度（5 档绿色色阶 + 8-bit 像素美学 + 月份切换 + 点击方块跳首页）。

但**年视图缺失**：用户只能逐月翻看，无法一眼总览全年活跃态势。年度回看是"回看"的自然延伸——月视图答"某月每天怎样"，年视图答"某年每月怎样"。当前月视图切换 12 次才能扫完一年，缺年度总览能力。

本轮补上**方案 C：12 月迷你月历拼贴**（3 列 × 4 行），复用月视图 `Cell` / `buildGrid` / 色阶 / `RainbowTrim` / `Legend` / ViewModel 状态机范式，视觉与月视图一致，竖屏自然滚动。

---

## 二、目标

1. **数据层 `aggregateYear` + `getDailyActivityForYear`**：新增独立 `aggregateYear(events, year, now, zone)` 顶层函数（复用 `effectiveDurationMs`，与 `aggregateMonth` 平行，**不改 `aggregateMonth`**）；`EventRepository` 加 `getDailyActivityForYear(year)`，复用既有 `EventDao.getEventsByMonth`（左闭右开，传年范围）。
2. **纯函数层 `buildYearGrid`**：`HeatmapCalculator` 加 `buildYearGrid(year, activities, today): YearGrid`，内部对 12 个月各调既有 `buildGrid`（复用！），返回 `YearGrid { months: List<MonthGrid> }`，`MonthGrid { yearMonth, cells, monthLabel }`。
3. **ViewModel `HeatmapYearViewModel`**：平行 `HeatmapViewModel`，`_selectedYear: MutableStateFlow<Year>` + `state: StateFlow<HeatmapYearUiState>`（`flatMapLatest` + `WhileSubscribed(5000)`），`previousYear`/`nextYear`/`goToCurrentYear`，`currentYear` 计算属性（B2 修复模式）。
4. **UI `HeatmapYearScreen`**：`RainbowTrim` + 顶栏（返回 + "年度回看"）+ `YearSwitcher`（‹ 2026年 › 今年）+ 12 月 mini 月历拼贴（3 列 × 4 行，weight 自适应屏宽）+ `Legend`（复用）。mini 格子纯展示不可点击（年视图核心是总览）。
5. **入口**：`HeatmapScreen` 顶栏 `MonthSwitcher` 旁加"年"跳转按钮 → `Routes.HEATMAP_YEAR` → `HeatmapYearScreen`。`AppNavHost` 加 `composable(Routes.HEATMAP_YEAR)`。
6. **测试**：`HeatmapCalculatorTest` 加 `buildYearGrid` 用例；`EventRepositoryHeatmapTest` 加 `aggregateYear` 用例；新建 `HeatmapYearViewModelTest`（复用迭代 9 方案 A 范式）。

> 三个 orchestrator 决策已拍板（年视图方案 C / 入口选项 3 / 不组合文档打磨 选项 A），详见 §八。

---

## 三、非目标

- **不改 `aggregateMonth` / `getDailyActivityForMonth` / 月视图任何代码**：`aggregateYear` 独立实现，不抽 helper 不泛化 `aggregateMonth`，保持月视图测试零回归。
- **不改 `EventDao`**：复用既有 `getEventsByMonth(start, end)`（已是左闭右开范围查询），仅传年范围 epoch，Dao 零改动。
- **不改 `effectiveDurationMs` / `levelFor` / `buildGrid` / `Cell` / `DailyActivity` / `HeatmapLevel0..4`**：纯函数层只**新增** `buildYearGrid` + `YearGrid`/`MonthGrid` data class，既有符号零改动。
- **不改 `HeatmapScreen` / `HeatmapViewModel`**：仅 `HeatmapScreen` 顶栏加"年"跳转按钮（最小改动：加一个 `onYearClick` 参数 + 一个按钮 composable）；`HeatmapViewModel` 零改动。
- **不做 mini 格子/月份标题点击跳月视图**：年视图核心是总览，弱化点击；跳月视图需改 `HeatmapScreen`/`HeatmapViewModel` 支持初始月份参数，破坏"不改"原则，本轮不做（§六.4 缓解）。
- **不做文档打磨**：M+ 已够一轮（orchestrator 决策 3 选项 A）。
- **不做搜索 / 数据导出迭代 / release 配置 / UI 测试**：与本轮无关。
- **不引入新依赖**：仅用 `java.time.Year` / `java.time.YearMonth` / `java.time.LocalDate`（已在用）+ 既有 Compose/material3 组件。
- **绝对不联网**：纯本地 Room 聚合 + Compose 渲染。
- **不改 `MainCoroutineRule` / `DispatchersModule` / `build.gradle.kts` / `AndroidManifest.xml`**：测试范式沿用迭代 9 方案 A，无新依赖。

---

## 四、设计

### 4.1 数据层

#### 4.1.1 `EventRepository.getDailyActivityForYear`

`EventRepository` 加方法（紧随 `getDailyActivityForMonth` 之后）：

```kotlin
fun getDailyActivityForYear(year: Year): Flow<List<DailyActivity>> =
    eventDao.getEventsByMonth(yearStartEpoch(year), yearEndEpoch(year))
        .map { events -> aggregateYear(events, year) }
```

辅助 epoch 函数（紧随 `monthStartEpoch`/`monthEndEpoch` 之后，同模式）：

```kotlin
private fun yearStartEpoch(year: Year): Long =
    year.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun yearEndEpoch(year: Year): Long =
    year.plusYears(1).atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
```

要点：
- **复用 `EventDao.getEventsByMonth`**：该方法查询条件为 `startTime >= :start AND startTime < :end`（左闭右开），传 `yearStartEpoch`/`yearEndEpoch` 即得全年事件，Dao 零改动。
- **复用 `aggregateYear`**：与 `getDailyActivityForMonth` 调 `aggregateMonth` 完全平行。
- `now` / `zone` 走 `aggregateYear` 默认参数（生产代码用 `System.currentTimeMillis()` / `ZoneId.systemDefault()`），测试注入。

#### 4.1.2 `aggregateYear` 顶层函数（独立实现，不泛化 `aggregateMonth`）

新增 `internal` 顶层函数（紧随 `aggregateMonth` 之后，同文件 `EventRepository.kt`）：

```kotlin
/**
 * 年度聚合：按事件开始日归属，跨日事件时长截断到当天 24:00，进行中事件 clamp 到当天范围。
 * 与 [aggregateMonth] 逻辑平行，仅范围不同（年 vs 月）。复用 [effectiveDurationMs]。
 * internal 供单测访问；now 与 zone 提供测试注入点。
 *
 * 设计决策：独立实现而非抽 helper 泛化 aggregateMonth，保持 aggregateMonth 零改动零回归。
 * 返回值只包含有事件的天（与 aggregateMonth 一致），无事件的天由 buildGrid 通过
 * activities.associateBy + ?: 0 处理（复用既有逻辑）。
 */
internal fun aggregateYear(
    events: List<EventEntity>,
    year: Year,
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault()
): List<DailyActivity> {
    val byDay = mutableMapOf<LocalDate, Pair<Int, Long>>() // date -> (count, durationMs)
    for (e in events) {
        val eventStart = Instant.ofEpochMilli(e.startTime).atZone(zone)
        val startDay = eventStart.toLocalDate()
        // 仅归属开始日；不在选定年的事件（理论上 DAO 已过滤）跳过
        if (Year.from(startDay) != year) continue
        // not_started(0) 计入 count 但不计入时长
        val (cnt, dur) = byDay[startDay] ?: (0 to 0L)
        val newCnt = cnt + 1
        val newDur = if (e.status == EventStatus.NotStarted.value) dur
        else dur + effectiveDurationMs(e, startDay, now, zone)
        byDay[startDay] = newCnt to newDur
    }
    return byDay.entries.map { (d, pair) ->
        DailyActivity(date = d, eventCount = pair.first, durationMs = pair.second)
    }.sortedBy { it.date }
}
```

要点：
- **与 `aggregateMonth` 唯一区别**：归属谓词 `Year.from(startDay) != year`（`aggregateMonth` 用 `YearMonth.from(startDay) != yearMonth`）。其余循环体逐字相同。
- **复用 `effectiveDurationMs`**：跨日截断 / 进行中 clamp / not_started 不计时长逻辑完全复用，零改动。
- **独立实现不抽 helper**：严格遵守"不泛化 `aggregateMonth`"原则。抽 helper 需把归属谓词参数化（`inRange: (LocalDate) -> Boolean`），会改动 `aggregateMonth` 实现委托，虽签名不变但有回归风险（迭代 2 月视图测试是核心资产）。复制几行循环体成本远低于回归风险。
- **返回值只含有事件的天**：与 `aggregateMonth` 一致，不返回全年 365/366 个 `DailyActivity`（避免大量 0 值对象 + 与 `aggregateMonth` 行为一致易测）。无事件的天由 `buildGrid` 的 `activities.associateBy { it.date }` + `act?.eventCount ?: 0` / `act?.durationMs ?: 0L` 处理（既有逻辑，复用）。
- `import java.time.Year` 需加到 `EventRepository.kt` 顶部 import 区。

#### 4.1.3 数据流

```
EventDao.getEventsByMonth(yearStart, yearEnd)   // 既有，左闭右开，复用
   → Flow<List<EventEntity>>
   → .map { aggregateYear(it, year) }            // 新增，复用 effectiveDurationMs
   → Flow<List<DailyActivity>>                   // 有事件的天
   → HeatmapYearViewModel.flatMapLatest
   → .map { HeatmapCalculator.buildYearGrid(year, it, today) }  // 新增，复用 buildGrid
   → HeatmapYearUiState(months = List<MonthGrid>)
```

### 4.2 纯函数层

#### 4.2.1 `HeatmapCalculator.buildYearGrid` + `YearGrid` / `MonthGrid`

`HeatmapCalculator` 加 data class 与函数（紧随 `buildGrid` 之后）：

```kotlin
data class YearGrid(
    val months: List<MonthGrid>   // 固定 12 项（1月..12月）
)

data class MonthGrid(
    val yearMonth: YearMonth,
    val cells: List<List<Cell>>,  // 6 行 × 7 列，复用 buildGrid 返回类型
    val monthLabel: String        // "1月".."12月"
)

/**
 * 构建年视图网格：12 个月的 mini 月历拼贴。
 * 每月调用既有 [buildGrid] 构建 6×7 网格（复用！），从 activities 过滤当月活动传入。
 * today 用于标记今天（与 buildGrid 同语义）。
 */
fun buildYearGrid(
    year: Year,
    activities: List<DailyActivity>,
    today: LocalDate
): YearGrid {
    val months = (1..12).map { m ->
        val yearMonth = YearMonth.of(year.value, m)
        val monthActivities = activities.filter { YearMonth.from(it.date) == yearMonth }
        val cells = buildGrid(yearMonth, monthActivities, today)  // 复用既有 buildGrid
        MonthGrid(
            yearMonth = yearMonth,
            cells = cells,
            monthLabel = "${m}月"
        )
    }
    return YearGrid(months = months)
}
```

要点：
- **完全复用 `buildGrid`**：每月 6×7 网格、周一开头、月初月末补位（`isInMonth=false`）、今天/未来标记、活动映射，全部由 `buildGrid` 既有逻辑处理，零重复。
- **`MonthGrid.cells` 用 `List<List<Cell>>`**：与 `buildGrid` 返回类型一致，UI 层直接 `for (row in monthGrid.cells) { Row { for (cell in row) { ... } } }` 渲染，无需扁平化。
- **`monthLabel`**：`"${m}月"`（1月..12月），用于 mini 月历标题。
- **`levelFor` / `Cell` / `DailyActivity` 零改动**：色阶映射、格子数据结构完全复用。
- `import java.time.Year` 需加到 `HeatmapCalculator.kt` 顶部 import 区（`YearMonth`/`LocalDate` 已在用）。

### 4.3 ViewModel

#### 4.3.1 `HeatmapYearViewModel`

新增 `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearViewModel.kt`（与 `HeatmapViewModel.kt` 同包、同模式）：

```kotlin
package com.shijiben.feature.heatmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.Year
import java.time.ZoneId
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HeatmapYearViewModel @Inject constructor(
    private val eventRepository: EventRepository
) : ViewModel() {

    data class HeatmapYearUiState(
        val year: Year = Year.now(ZoneId.systemDefault()),
        val months: List<HeatmapCalculator.MonthGrid> = emptyList(),
        val isCurrentYear: Boolean = true,
        val canGoNext: Boolean = false   // 是否还能往未来翻（不超过当前年）
    )

    private val currentYear: Year
        get() = Year.now(ZoneId.systemDefault())   // B2 修复模式：计算属性，避免跨年不刷新

    private val _selectedYear = MutableStateFlow(currentYear)
    val selectedYear: StateFlow<Year> = _selectedYear.asStateFlow()

    val state: StateFlow<HeatmapYearUiState> = _selectedYear
        .flatMapLatest { year ->
            eventRepository.getDailyActivityForYear(year).map { activities ->
                val today = LocalDate.now(ZoneId.systemDefault())
                HeatmapYearUiState(
                    year = year,
                    months = HeatmapCalculator.buildYearGrid(year, activities, today).months,
                    isCurrentYear = year == currentYear,
                    canGoNext = year < currentYear
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeatmapYearUiState())

    fun previousYear() {
        _selectedYear.value = _selectedYear.value.minusYears(1)
    }

    fun nextYear() {
        // 下一年不超过当前年（未来无意义）
        if (_selectedYear.value < currentYear) {
            _selectedYear.value = _selectedYear.value.plusYears(1)
        }
    }

    fun goToCurrentYear() {
        _selectedYear.value = currentYear
    }
}
```

要点：
- **平行 `HeatmapViewModel`**：同 `@HiltViewModel` + 注入 `EventRepository`（**不注入 dispatcher**，与 `HeatmapViewModel` 一致；`getDailyActivityForYear` 返回 Flow，`.map` 在 Flow 内执行，无需 `withContext`）。`ExportViewModel` 注入 `@IoDispatcher` 是因为有 `withContext(ioDispatcher)` 包裹的同步 IO 操作，本 ViewModel 无此需求。
- **`currentYear` 计算属性**：与 `HeatmapViewModel.currentMonth` 同模式（B2 修复：避免构造时捕获 `Year.now()` 导致跨年停留不刷新）。
- **`state` 链路**：`_selectedYear.flatMapLatest { year -> getDailyActivityForYear(year).map { buildYearGrid } }.stateIn(WhileSubscribed(5000), HeatmapYearUiState())`，与 `HeatmapViewModel.state` 逐字同构（`YearMonth` → `Year`，`buildGrid` → `buildYearGrid`）。
- **`canGoNext = year < currentYear`**：不能超过当前年（与 `ym < currentMonth` 同语义）。
- **默认 `HeatmapYearUiState()`**：`year = Year.now()` / `months = emptyList()` / `isCurrentYear = true` / `canGoNext = false`，与 `HeatmapUiState()` 默认值同模式（占位 state，等 Room Flow 初始查询落定后填充）。

### 4.4 UI

#### 4.4.1 `HeatmapYearScreen` 整体结构

新增 `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt`（与 `HeatmapScreen.kt` 同包、同美学范式）：

```
HeatmapYearScreen(onBack: () -> Unit, viewModel: HeatmapYearViewModel = hiltViewModel())
  ├─ RainbowTrim()                          // 复制 HeatmapScreen 的 RainbowTrim（8dp 彩虹条）
  ├─ 顶栏 Row { IconButton(onBack) + "年度回看" }
  ├─ 2dp 黑色分隔线
  ├─ YearSwitcher(‹ + "2026年" + › + 今年)   // 平行 MonthSwitcher
  ├─ 2dp 黑色分隔线
  ├─ 主体 Column(verticalScroll) {
  │     Column { repeat(4) { row ->          // 4 行
  │         Row { repeat(3) { col ->          // 3 列
  │             MiniMonth(monthGrid)          // mini 月历
  │         } }
  │     } }
  │     Legend()                              // 复制 HeatmapScreen 的 Legend
  │ }
```

#### 4.4.2 ASCII 线框

```
┌───────────────────────────────────┐
│ ████████████████████████████████  │  RainbowTrim（8dp 彩虹条）
├───────────────────────────────────┤
│ ‹  年度回看                        │  顶栏（返回 + 标题）
├───────────────────────────────────┤  2dp 黑线
│ ‹  2026年  ›      [今年]          │  YearSwitcher
├───────────────────────────────────┤  2dp 黑线
│  1月        2月        3月         │  月份标题行（3 列 weight 自适应）
│ □□□□□□□   □□□□□□□   □□□□□□□       │  mini 月历第 1 行（7 格 × 3 月）
│ □□□□□□□   □□□□□□□   □□□□□□□       │
│ □□□□□□□   □□□□□□□   □□□□□□□       │
│ □□□□□□□   □□□□□□□   □□□□□□□       │
│ □□□□□□□   □□□□□□□   □□□□□□□       │
│ □□□□□□□   □□□□□□□   □□□□□□□       │  （6 行 × 7 列 = 42 格/月）
│                                   │  行间间距
│  4月        5月        6月         │  mini 月历第 2 行
│ □□□□□□□   □□□□□□□   □□□□□□□       │
│ ...                               │
│  7月        8月        9月         │  mini 月历第 3 行
│ ...                               │
│  10月       11月       12月        │  mini 月历第 4 行
│ ...                               │
├───────────────────────────────────┤
│  少 □□□□□ 多                       │  Legend（复用）
└───────────────────────────────────┘
```

#### 4.4.3 mini 月历格子尺寸（weight 自适应屏宽）

- **3 列布局**：`Row { repeat(3) { Box(Modifier.weight(1f)) { MiniMonth(...) } } }`，每列占 1/3 屏宽减间距。
- **每月内 6×7 网格**：`Column { repeat(6) { Row { repeat(7) { Box(Modifier.weight(1f).aspectRatio(1f)) { MiniCell(...) } } } }`，格子用 `weight(1f).aspectRatio(1f)` 保持正方形且自适应。
- **典型尺寸估算**（360dp 屏宽）：列间距 8dp × 2 = 16dp，每列宽 ≈ (360 - 16) / 3 ≈ 114dp；格子间距 1dp × 6 = 6dp，格子边长 ≈ (114 - 6) / 7 ≈ 15dp。窄屏（320dp）下格子约 13dp，仍可辨识色阶。
- **不用固定 dp**：严格用 `weight(1f).aspectRatio(1f)`，避免窄屏溢出（迭代 2 月视图 `DayCell` 用固定 40dp 在 <320dp 屏有挤压风险，年视图格子更多更小，必须 weight 自适应）。

#### 4.4.4 `MiniMonth` composable

```kotlin
@Composable
private fun MiniMonth(monthGrid: HeatmapCalculator.MonthGrid) {
    Column(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 月份标题："1月".."12月"
        Text(
            text = monthGrid.monthLabel,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(Modifier.height(2.dp))
        // 6×7 mini 网格
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            for (row in monthGrid.cells) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (cell in row) {
                        Box(modifier = Modifier.weight(1f)) {
                            MiniCell(cell)
                        }
                    }
                }
            }
        }
    }
}
```

#### 4.4.5 `MiniCell` composable（纯展示不可点击）

```kotlin
@Composable
private fun MiniCell(cell: HeatmapCalculator.Cell) {
    // 补位方块：透明，不渲染
    if (!cell.isInMonth) return
    val levelColor = when (cell.level) {
        0 -> HeatmapLevel0
        1 -> HeatmapLevel1
        2 -> HeatmapLevel2
        3 -> HeatmapLevel3
        else -> HeatmapLevel4
    }
    val borderColor = when {
        cell.isToday -> Primary       // 今天红边框（与 DayCell 一致）
        else -> Color.Black           // 其余黑边框
    }
    Box(
        modifier = Modifier
            .aspectRatio(1f)          // 正方形，宽由父 weight 决定
            .border(1.dp, borderColor) // 1dp（格子小，2dp 过粗）
            .background(levelColor)
            .let { base ->
                // 未来日半透明（与 DayCell 一致，弱化未来）
                if (cell.isFuture) base.alpha(0.5f) else base
            }
    )
    // 不显示日期数字（格子约 14dp 放不下 9sp 数字）
    // 不可点击（年视图核心是总览）
}
```

要点：
- **纯展示不可点击**：年视图核心是年度总览，弱化单日交互（§六.4）。点击交互在月视图已具备，年视图不重复。
- **1dp 边框**：格子约 14dp，2dp 边框占比过大（与月视图 `DayCell` 的 40dp+2dp 比例失调），用 1dp 保持视觉协调。这是合理的尺寸适配，非美学偏离。
- **今天 `Primary` 边框 + 未来日 `alpha(0.5f)`**：与 `DayCell` 视觉一致，年视图今天一眼可辨。
- **不显示日期数字**：格子约 14dp 放不下 9sp 数字（月视图 `DayCell` 40dp 才放 9sp）。年视图通过月份标题 + 格子位置定位，不需日数字。
- **补位方块直接 `return`**：与 `DayCell` 的补位处理同模式（`if (!cell.isInMonth) { Box(...); return }`），但 mini 版不渲染任何 Box（更紧凑）。

#### 4.4.6 `YearSwitcher` composable（平行 `MonthSwitcher`）

```kotlin
@Composable
private fun YearSwitcher(
    year: Year,
    canGoNext: Boolean,
    isCurrentYear: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onGoCurrent: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PixelArrowBox(onClick = onPrevious, enabled = true,
            arrow = Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一年")
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${year.value}年",
            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        PixelArrowBox(onClick = onNext, enabled = canGoNext,
            arrow = Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一年")
        Spacer(Modifier.width(8.dp))
        // 今年按钮：与 MonthSwitcher 的"本月"按钮同范式
        Box(
            modifier = Modifier
                .border(2.dp, if (isCurrentYear) Color(0xFFCBD5E1) else Primary)
                .background(Color.Transparent)
                .clickable(enabled = !isCurrentYear, onClick = onGoCurrent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "今年",
                color = if (isCurrentYear) Color(0xFF94A3B8) else Primary,
                fontSize = 11.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}
```

要点：
- **平行 `MonthSwitcher`**：`‹ + 年(weight 1f) + › + 今年`，与 `‹ + 年月(weight 1f) + › + 本月` 同构。
- **`PixelArrowBox` / `RainbowTrim` / `Legend` 复制**：与 `HeatmapScreen` 同模式（迭代 2 设计决策：复制 8 行 `RainbowTrim` 避免改 `PixelComponents`，本轮沿用）。`PixelArrowBox` / `Legend` 同理复制到 `HeatmapYearScreen.kt`，保持两个 Screen 独立无依赖。
- **`Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right`**：与 `HeatmapScreen` 迁移后的方向图标一致（迭代 7 已迁移到 AutoMirrored）。

#### 4.4.7 顶部 import 区

`HeatmapYearScreen.kt` 顶部 import 复制 `HeatmapScreen.kt` 的 import 集（`RainbowTrim`/`Legend`/`PixelArrowBox` 所需的 `background`/`border`/`clickable`/`layout.*`/`material3.*`/`theme.*`），加 `java.time.Year`。无新依赖。

### 4.5 入口

#### 4.5.1 `HeatmapScreen` 顶栏 `MonthSwitcher` 旁加"年"跳转按钮

**orchestrator 决策 2（选项 3）**：`HeatmapScreen` 顶栏 `MonthSwitcher` 旁加"年"跳转按钮 → 独立 `HeatmapYearScreen` + `Routes.HEATMAP_YEAR`，最小改动不破坏现有月视图。

**改 `HeatmapScreen` 签名**（加 `onYearClick` 参数）：

```kotlin
@Composable
fun HeatmapScreen(
    onBack: () -> Unit,
    onDateClick: (Triple<Int, Int, Int>) -> Unit,
    onYearClick: () -> Unit,                    // 新增
    viewModel: HeatmapViewModel = hiltViewModel()
)
```

**改 `MonthSwitcher` 调用处**（在 `HeatmapScreen` 内，`MonthSwitcher` 之后或之内加"年"按钮）：

在 `MonthSwitcher` Row 末尾（"本月"按钮之后）加"年"按钮，与"本月"并列，语义为"月切换 → 年切换"：

```kotlin
// 在 MonthSwitcher 的 Row 内，"本月" Box 之后加：
Spacer(Modifier.width(8.dp))
Box(
    modifier = Modifier
        .border(2.dp, Primary)
        .background(Color.Transparent)
        .clickable(onClick = onYearClick)
        .padding(horizontal = 8.dp, vertical = 4.dp)
) {
    Text(
        text = "年",
        color = Primary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
    )
}
```

要点：
- **`MonthSwitcher` 需加 `onYearClick` 参数**：当前 `MonthSwitcher` 是 private composable，加一个 `onYearClick: () -> Unit` 参数透传。
- **"年"按钮样式**：与"本月"按钮同范式（`border(2.dp, Primary)` + `clickable` + 11sp Bold），但始终可点（无 enabled 约束，年视图总是可达）。
- **位置**：`MonthSwitcher` Row 末尾，"本月"之后。窄屏下 `‹ + 年月(weight 1f) + › + 本月 + 年` 仍可容纳（年月 Text 用 weight 1f 撑开，其余固定宽度）。
- **替代位置（不采纳）**：顶栏（返回 + "回看" + 年按钮）——不采纳因为任务明确要求"MonthSwitcher 旁"。

#### 4.5.2 `Routes` 加 `HEATMAP_YEAR`

```kotlin
object Routes {
    const val TIMELINE = "timeline"
    const val NOTES = "notes"
    const val TIMEVIZ = "timeviz"
    const val HEATMAP = "heatmap"
    const val HEATMAP_YEAR = "heatmap_year"     // 新增
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}
```

#### 4.5.3 `AppNavHost` 加 `composable(Routes.HEATMAP_YEAR)`

在 `composable(Routes.HEATMAP) { ... }` 之后加：

```kotlin
composable(Routes.HEATMAP_YEAR) {
    HeatmapYearScreen(
        onBack = { navController.popBackStack() }
    )
}
```

并改 `composable(Routes.HEATMAP)` 块内 `HeatmapScreen(...)` 调用，加 `onYearClick`：

```kotlin
composable(Routes.HEATMAP) {
    HeatmapScreen(
        onBack = { navController.popBackStack() },
        onDateClick = { (y, m, d) ->
            navController.getBackStackEntry(Routes.TIMELINE)
                .savedStateHandle["heatmap_target_date"] = Triple(y, m, d)
            navController.popBackStack()
        },
        onYearClick = { navController.navigate(Routes.HEATMAP_YEAR) }   // 新增
    )
}
```

要点：
- `HeatmapYearScreen` 仅需 `onBack`（无 `onDateClick`，mini 格子不可点击）。
- `import com.shijiben.feature.heatmap.HeatmapYearScreen` 需加到 `AppNavHost.kt` 顶部 import 区。

### 4.6 测试

#### 4.6.1 `HeatmapCalculatorTest` 加 `buildYearGrid` 用例（纯函数，普通 JUnit）

对齐既有 `HeatmapCalculatorTest` 范式（纯 JUnit，无 Android 依赖）。新增以下用例：

| 测试方法 | 内容 |
|----------|------|
| `buildYearGrid_returns12Months` | 任意 year + 空 activities → `months.size == 12`，`monthLabel` 依次为 "1月".."12月"，`yearMonth` 依次为 `YearMonth.of(year, 1..12)` |
| `buildYearGrid_eachMonthCellsAre6x7` | 任意 year → 每月 `cells.size == 6`，每行 `size == 7`（复用 `buildGrid` 形状契约） |
| `buildYearGrid_paddingCellsHaveIsInMonthFalse` | 1月首行补位（上月末）`isInMonth == false`，12月末行补位（下年初）`isInMonth == false` |
| `buildYearGrid_todayMarkedExactlyOnce` | today 在 year 内 → 全年 12 月 `cells.flatten().filter { isToday }.size == 1`，且该 cell 在 today 对应月份 |
| `buildYearGrid_todayOutsideYearNotMarked` | today 在 year 之外（如 year=2025, today=2026-01-15）→ 全年无 `isToday` cell |
| `buildYearGrid_activityMappedToCorrectMonth` | 构造 3 条 `DailyActivity`（1月/6月/12月各 1 条）→ 对应月份的 cell `durationMs > 0` 且 `level == levelFor(durationMs)`，其余月份对应日 `durationMs == 0` |
| `buildYearGrid_activityInLeapYearFeb29` | year=2024（闰年），activity date=2024-02-29 → 2月 cell 对应日 `durationMs > 0`（验证闰年 2-29 正确归属） |

#### 4.6.2 `EventRepositoryHeatmapTest` 加 `aggregateYear` 用例（纯 JUnit）

对齐既有 `EventRepositoryHeatmapTest` 范式（纯 JUnit，测 `internal` 顶层函数，`ms(y,mo,d,hh,mm)` + `entity(start, end, status)` helper 复用）。新增以下用例：

| 测试方法 | 内容 |
|----------|------|
| `aggregateYear_emptyList_returnsEmpty` | `aggregateYear(emptyList(), Year.of(2026), now, zone)` → `isEmpty()` |
| `aggregateYear_notStarted_countsButNoDuration` | not_started 事件 → `eventCount == 1` 且 `durationMs == 0` |
| `aggregateYear_singleDayMultipleEvents_sumsDuration` | 同日 2 completed + 1 not_started → `eventCount == 3`，`durationMs == 2.5h` |
| `aggregateYear_crossDayEventDoesNotPolluteEndDay` | 12-31 23:00 – 1-1 01:00（跨年）→ 归 12-31，`durationMs == 1h`；1-1 不出现 |
| `aggregateYear_inProgressMultiDay_clampsToStartDayMidnight` | 6-03 14:00 开始未停，now=6-05 10:00 → 6-03 `durationMs == 10h`，6-04/6-05 不出现 |
| `aggregateYear_yearEndBoundary_oneMinEventAt2359` | 12-31 23:59 的 1min 事件 → 12-31 `durationMs == 60_000` |
| `aggregateYear_timezoneShanghai_startDayIsLocal` | 事件 2026-06-15T00:00:00+08:00 → `startDay == 2026-06-15` |
| `aggregateYear_eventOutsideYearIsSkipped` | 2025-12-31 事件传入（理论上 DAO 已过滤）→ `isEmpty()` |
| `aggregateYear_leapYearFeb29EventIncluded` | year=2024，事件 2024-02-29 10:00-11:00 → 结果含 `date == 2024-02-29`，`durationMs == 1h` |

#### 4.6.3 `HeatmapYearViewModelTest` 新建（Robolectric + MainCoroutineRule，复用迭代 9 方案 A）

新增 `app/src/test/java/com/shijiben/feature/heatmap/HeatmapYearViewModelTest.kt`，**逐字复用 `HeatmapViewModelTest` 的方案 A 范式**：

- `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [33])`
- `@get:Rule val mainRule = MainCoroutineRule(StandardTestDispatcher())`
- `setup()`：`roomExecutor` 适配器（`Executor { cmd -> mainRule.dispatcher.dispatch(EmptyCoroutineContext, cmd) }`）+ `Room.inMemoryDatabaseBuilder(...).setQueryExecutor(roomExecutor).setTransactionExecutor(roomExecutor).allowMainThreadQueries().build()` + `EventRepository(db.eventDao())` + `HeatmapYearViewModel(eventRepo)`
- `teardown()`：`db.close()`
- 每个测试：`runTest(mainRule.dispatcher)` + `backgroundScope.launch { vm.state.collect {} }` + `vm.state.first { it.months.isNotEmpty() }`（等 Room Flow 初始查询落定）
- `zone = ZoneId.systemDefault()`

| 测试方法 | 内容 |
|----------|------|
| `initialState_isCurrentYear` | `state.year == Year.now(zone)`，`isCurrentYear == true`，`canGoNext == false` |
| `previousYear_decrementsAndEnablesNext` | `previousYear()` → `state.year == Year.now(zone).minusYears(1)`，`isCurrentYear == false`，`canGoNext == true` |
| `nextYear_fromPrevious_returnsToCurrent` | `previousYear()` → `nextYear()` → `state.year == Year.now(zone)`，`canGoNext == false` |
| `nextYear_atCurrent_doesNotAdvance` | 当前年 `nextYear()` → `state.year` 不变（`before == after`），`canGoNext == false` |
| `goToCurrentYear_fromPrevious_returnsToCurrent` | `previousYear()` → `goToCurrentYear()` → `state.year == Year.now(zone)`，`canGoNext == false` |
| `stateMonths_shapeIs12AndEachMonthIs6x7` | `state.months.size == 12`，每月 `cells.size == 6`，每行 `size == 7` |
| `stateMonths_todayMarkedExactlyOnce` | 全年 `months.flatMap { it.cells.flatten() }.filter { isToday }.size == 1` |
| `stateMonths_updatesWhenRepoEmitsNewData` | 插入今天 00:00-01:00 事件 → 等待 Room invalidation → 今天所在月的对应 cell `durationMs == 3600_000` 且 `level == 1` |

要点：
- **方案 A 直接复用**：`HeatmapYearViewModel` 与 `HeatmapViewModel` 同用 `flatMapLatest` + `WhileSubscribed(5000)` + Room Flow，teardown 竞态根因相同；方案 A（路由 Room executor 到 `mainRule.dispatcher`）同样适用，无 flaky 风险。
- **`first { it.months.isNotEmpty() }`**：与 `HeatmapViewModelTest` 的 `first { it.cells.isNotEmpty() }` 同模式（等占位 state 过去）。
- **8 个 @Test**：与 `HeatmapViewModelTest` 的 7 个 @Test 平行，多一个 `stateMonths_todayMarkedExactlyOnce`（年视图今天标记跨 12 月验证）。

---

## 五、涉及文件清单

### 改

| 文件 | 改动 |
|------|------|
| `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` | 加 `getDailyActivityForYear(year)` 方法 + `yearStartEpoch`/`yearEndEpoch` private 辅助 + `aggregateYear` internal 顶层函数 + `import java.time.Year` |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapCalculator.kt` | 加 `YearGrid` / `MonthGrid` data class + `buildYearGrid` 函数 + `import java.time.Year` |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` | `HeatmapScreen` 签名加 `onYearClick: () -> Unit` 参数；`MonthSwitcher` 签名加 `onYearClick` 参数 + Row 末尾加"年"按钮 |
| `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | `Routes` 加 `HEATMAP_YEAR` 常量；`composable(Routes.HEATMAP)` 调用 `HeatmapScreen` 加 `onYearClick`；新增 `composable(Routes.HEATMAP_YEAR) { HeatmapYearScreen(onBack = popBackStack) }`；`import HeatmapYearScreen` |
| `app/src/test/java/com/shijiben/feature/heatmap/HeatmapCalculatorTest.kt` | 加 `buildYearGrid` 7 个用例 |
| `app/src/test/java/com/shijiben/data/repository/EventRepositoryHeatmapTest.kt` | 加 `aggregateYear` 9 个用例 |

### 新增

| 文件 | 内容 |
|------|------|
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearViewModel.kt` | `@HiltViewModel` + `HeatmapYearUiState` + `_selectedYear`/`state` + `previousYear`/`nextYear`/`goToCurrentYear` + `currentYear` 计算属性 |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt` | `HeatmapYearScreen` + `RainbowTrim`（复制）+ `YearSwitcher` + `PixelArrowBox`（复制）+ `MiniMonth` + `MiniCell` + `Legend`（复制） |
| `app/src/test/java/com/shijiben/feature/heatmap/HeatmapYearViewModelTest.kt` | 8 个 @Test，复用方案 A 范式 |

### 复核无改动

| 文件 | 原因 |
|------|------|
| `app/src/main/java/com/shijiben/data/local/EventDao.kt` | `getEventsByMonth(start, end)` 已是左闭右开范围查询，传年范围 epoch 即可，零改动 |
| `app/src/main/java/com/shijiben/data/model/HeatmapModels.kt` | `DailyActivity` 字段齐全，`aggregateYear` 复用 |
| `app/src/main/java/com/shijiben/ui/theme/AppColors.kt` | `HeatmapLevel0..4` 已定义，`MiniCell` 复用 |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapViewModel.kt` | 月视图 ViewModel 零改动（年视图独立 ViewModel） |
| `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` 的 `aggregateMonth` / `effectiveDurationMs` | `aggregateYear` 独立实现复用 `effectiveDurationMs`，`aggregateMonth` 零改动 |
| `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt` | 方案 A 范式作为新测试的模板，自身零改动 |
| `app/src/test/java/com/shijiben/feature/recording/MainCoroutineRule.kt` | 不动（方案 A 仅改测试 setup，不改 rule） |
| `app/src/main/java/com/shijiben/di/DispatchersModule.kt` | `HeatmapYearViewModel` 不注入 dispatcher（平行 `HeatmapViewModel`），`@IoDispatcher` 无关 |
| `app/build.gradle.kts` / `AndroidManifest.xml` | 无新依赖/权限（`java.time.Year` 是 JDK 内置，minSdk 26 充分支持） |

---

## 六、边界情况（风险与缓解）

1. **UI 布局不确定性（mini 月历尺寸适配）** —— 缓解：mini 格子严格用 `weight(1f).aspectRatio(1f)` 自适应屏宽，不固定 dp；3 列每列约 110dp，格子约 14dp（360dp 屏）/ 13dp（320dp 屏）。Coding 阶段先骨架跑通（12 月拼贴 + 占位色），再打磨间距/字号。若窄屏仍挤压，可减列间距（8dp→4dp）或格子间距（1dp→0.5dp），但不用固定 dp。
2. **`aggregateYear` 不破坏月视图** —— 缓解：`aggregateYear` 独立实现，不抽 helper 不泛化 `aggregateMonth`，`aggregateMonth` / `effectiveDurationMs` 零改动。迭代 2 月视图测试（`EventRepositoryHeatmapTest` 的 `aggregateMonth_*` 用例）是核心资产，零回归。`aggregateYear` 与 `aggregateMonth` 唯一区别是归属谓词 `Year.from` vs `YearMonth.from`，循环体逐字相同，行为可预期。
3. **年视图数据量性能** —— 缓解：本地个人时间记录量级（百~千条事件/年），`EventDao.getEventsByMonth` 范围查询一次返回全年事件，`aggregateYear` 内存遍历毫秒级，`buildYearGrid` 调 `buildGrid` 12 次也是毫秒级。无性能瓶颈。若极端万+量级，可优化 `buildYearGrid` 内 `activities.filter` 为一次 groupBy，但本轮不做（避免过早优化）。
4. **mini 格点击体验差** —— 缓解：年视图核心是总览，弱化点击。mini 格子约 14dp 不可点击（`MiniCell` 无 `clickable`），避免误触。点击交互在月视图已具备（`DayCell` 点击跳首页），年视图不重复。若用户需查看某月详情，可用 `YearSwitcher` 切到该年后回月视图（本轮不做年→月跳转，见下条）。
5. **新 ViewModel flaky 风险** —— 缓解：`HeatmapYearViewModelTest` 逐字复用迭代 9 方案 A（路由 Room executor 到 `mainRule.dispatcher`），与 `HeatmapViewModelTest` 同范式同根因同修复，无 flaky 风险。迭代 9 已 5 次独立验证全绿。
6. **mini 月历标题点击跳月视图（本轮不做）** —— 评估：跳月视图需 `HeatmapScreen`/`HeatmapViewModel` 支持初始 `YearMonth` 参数（当前默认 `YearMonth.now()`），破坏"不改 `HeatmapViewModel`"原则；且需 `AppNavHost` 路由传参（`heatmap_year/{ym}` 或 savedStateHandle），增加导航复杂度。本轮不做，仅总览。若后续需做，方案：`HeatmapScreen` 加 `initialYearMonth: YearMonth?` 参数 + `HeatmapViewModel` 加 `@Assisted` 注入或 `SavedStateHandle` 读取，独立轮次处理。
7. **跨年事件归属** —— `aggregateYear` 按 `Year.from(startDay)` 归属，12-31 23:00 – 1-1 01:00 跨年事件归 12-31（开始日），1-1 不出现（与 `aggregateMonth` 跨月归属同语义）。单测 `aggregateYear_crossDayEventDoesNotPolluteEndDay` 覆盖。
8. **闰年 2-29** —— `Year.of(2024)` 是闰年，2-29 事件正常归属；`Year.of(2026)` 非闰年，无 2-29 事件。单测 `aggregateYear_leapYearFeb29EventIncluded` + `buildYearGrid_activityInLeapYearFeb29` 覆盖。
9. **`canGoNext` 边界** —— 当前年 `canGoNext == false`（不能进未来年）；上一年 `canGoNext == true`。`nextYear()` 在当前年 no-op（与 `HeatmapViewModel.nextMonth` 同模式）。单测 `nextYear_atCurrent_doesNotAdvance` 覆盖。
10. **`MonthSwitcher` 加"年"按钮后窄屏挤压** —— `MonthSwitcher` Row 当前 `‹ + 年月(weight 1f) + › + 本月`，加"年"后 `‹ + 年月(weight 1f) + › + 本月 + 年`。`年月` Text 用 `weight(1f)` 撑开吸收剩余空间，其余固定宽度元素（`‹` 26dp / `›` 26dp / `本月` ~50dp / `年` ~30dp + 间距 8dp×4=32dp）总固定宽 ~164dp，320dp 屏剩余 156dp 给年月 Text，可容纳"2026年6月"。无挤压风险。
11. **`RainbowTrim`/`Legend`/`PixelArrowBox` 复制而非复用** —— 沿用迭代 2 设计决策（复制 8 行 `RainbowTrim` 避免改 `PixelComponents`）。三个 composable 都是 private，复制到 `HeatmapYearScreen.kt` 保持两个 Screen 独立无依赖。代码重复 ~30 行，可接受（与既有 `HeatmapScreen` 模式一致）。

---

## 七、测试清单（验证标准）

### 7.1 验证门（四道，按顺序）

1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:testDebugUnitTest --rerun-tasks`（含新增 24 测试 + 现有 118 测试全绿无回归，共 142 测试）
3. `./gradlew assembleDebug`
4. `./gradlew :app:assembleRelease`

### 7.2 新增测试统计

| 测试类 | 用例数 | 类型 |
|--------|--------|------|
| `HeatmapCalculatorTest`（加） | 7 | 纯 JUnit（`buildYearGrid`） |
| `EventRepositoryHeatmapTest`（加） | 9 | 纯 JUnit（`aggregateYear`） |
| `HeatmapYearViewModelTest`（新） | 8 | Robolectric + MainCoroutineRule（方案 A） |
| **合计新增** | **24** | |

### 7.3 现有测试无回归

现有 118 测试（含 `HeatmapViewModelTest` 7 + `EventRepositoryHeatmapTest` 9 既有 + `HeatmapCalculatorTest` 既有 + 其余）在四道门下全绿。本轮：
- `aggregateMonth` / `effectiveDurationMs` 零改动 → `EventRepositoryHeatmapTest` 既有 9 用例零回归
- `buildGrid` / `levelFor` / `Cell` 零改动 → `HeatmapCalculatorTest` 既有用例零回归
- `HeatmapViewModel` 零改动 / `HeatmapScreen` 仅加 `onYearClick` 参数（无默认值，但 `HeatmapViewModelTest` 测 VM 不调 `HeatmapScreen`，不受影响） → `HeatmapViewModelTest` 7 用例零回归
- 方案 A 范式（`HeatmapViewModelTest` setup）零改动 → flaky 根治持续有效

### 7.4 手动验证（device，可选）

1. 首页 → 热力图入口 → `HeatmapScreen` 顶栏 `MonthSwitcher` 旁可见"年"按钮。
2. 点"年" → 跳转 `HeatmapYearScreen`，顶栏"年度回看" + `YearSwitcher` 显示当前年。
3. 12 月 mini 月历拼贴可见（3 列 × 4 行），每月 6×7 格子，有事件的天着色（5 档绿），今天 `Primary` 红边框。
4. 点"‹" → 上一年，`canGoNext` 变 true；点"›" → 回当前年，`canGoNext` 变 false；当前年点"›" 无反应。
5. 点"今年" → 回当前年（从上一年返回）。
6. `Legend` 显示"少 □□□□□ 多"。
7. 竖屏滚动顺畅（4 行 mini 月历 + Legend 可能超屏）。
8. 返回 → 回 `HeatmapScreen` 月视图，状态保持。

---

## 八、orchestrator 决策点

本轮三个决策点已由 orchestrator 拍板：

1. **年视图布局方案 = 选项 C**（12 月迷你月历拼贴，3 列 × 4 行）—— 已拍板。spec 落地：`HeatmapYearScreen` 用 `Column { repeat(4) { Row { repeat(3) { MiniMonth } } } }`，每月复用 `buildGrid` 6×7，竖屏 `verticalScroll`。
2. **入口方案 = 选项 3**（`HeatmapScreen` 顶栏 `MonthSwitcher` 旁加"年"跳转按钮 → 独立 `HeatmapYearScreen` + `Routes.HEATMAP_YEAR`）—— 已拍板。spec 落地：`HeatmapScreen` 加 `onYearClick` 参数 + `MonthSwitcher` Row 末尾加"年"按钮；`AppNavHost` 加 `Routes.HEATMAP_YEAR` + `composable(Routes.HEATMAP_YEAR)`。
3. **是否组合文档打磨 = 选项 A**（单独做年视图，M+ 已够一轮）—— 已拍板。spec 落地：本轮仅做年视图，不做文档打磨。

**本轮无新增决策点。** 以下设计选择由 spec 自主决定（无需 orchestrator 拍板，因属实现细节且与既有模式一致）：

- `aggregateYear` 独立实现不抽 helper（遵守"不泛化 `aggregateMonth`"硬约束）
- `aggregateYear` 只返回有事件的天（与 `aggregateMonth` 一致，复用 `buildGrid` 处理无事件天）
- `MonthGrid.cells` 用 `List<List<Cell>>`（与 `buildGrid` 返回类型一致）
- `HeatmapYearViewModel` 不注入 dispatcher（平行 `HeatmapViewModel`，无 `withContext` 需求）
- mini 格子纯展示不可点击（年视图核心是总览，§六.4）
- mini 格子 1dp 边框（尺寸适配，非美学偏离）
- mini 月历标题点击跳月视图本轮不做（破坏"不改 `HeatmapViewModel`"原则，§六.6）
- `RainbowTrim`/`Legend`/`PixelArrowBox` 复制（沿用迭代 2 既有模式）

---

## 九、硬约束逐项核对清单

| 硬约束 | 核对 |
|--------|------|
| 1. 绝对不联网 | ✅ 仅本地 Room 聚合 + Compose 渲染；`aggregateYear`/`buildYearGrid`/`HeatmapYearViewModel`/`HeatmapYearScreen` 无任何网络 API；`EventDao.getEventsByMonth` 是本地查询。无网络权限/调用。 |
| 2. 验证门四道全绿 | ✅ 顺序：① compileDebugKotlin → ② testDebugUnitTest --rerun-tasks（含新增 24 测试 + 现有 118 全绿） → ③ assembleDebug → ④ assembleRelease。无新依赖/权限，③④ 构建不受影响。 |
| 3. Coding ≠ Test subagent | ✅ 本 spec 不指定 subagent 身份；Coding 与 Test 由 orchestrator 分派不同 subagent 执行。spec 本身仅为设计文档。 |
| 4. 全自动到失败为止 | ✅ 设计无手动步骤介入构建/测试；四道门均自动化。 |
| 不改 DB schema | ✅ 无 Migration、无 Entity 字段变更、无 Dao 新方法；`EventDao.getEventsByMonth` 复用。 |
| 不改 `aggregateMonth` / 月视图 | ✅ `aggregateYear` 独立实现；`HeatmapViewModel`/`HeatmapScreen` 逻辑零改动（仅加 `onYearClick` 参数）。 |
| 不引入新依赖 | ✅ 仅用 `java.time.Year`（JDK 内置，minSdk 26 支持）+ 既有 Compose/material3 组件。`build.gradle.kts` 零改动。 |
| 用符号引用非行号 | ✅ spec 全程用函数名/类名/属性名引用（`aggregateMonth` / `effectiveDurationMs` / `buildGrid` / `Cell` / `HeatmapLevel0..4` / `MonthSwitcher` / `PixelArrowBox` 等），无静态行号。 |

---

## 十、风险与回退

1. **`HeatmapScreen` 加 `onYearClick` 参数破坏既有调用**：`AppNavHost` 中 `composable(Routes.HEATMAP)` 调用 `HeatmapScreen` 需同步加 `onYearClick`。若遗漏，编译期 fail（Kotlin 无默认值参数强制传参）。缓解：spec §4.5.3 明确 `AppNavHost` 调用处改动；`HeatmapScreen` 的 `onYearClick` 不给默认值（强制调用方传参，编译期捕获遗漏）。**注意**：`HeatmapViewModelTest` 不直接调 `HeatmapScreen`（测 VM 不测 UI），不受影响。
2. **`MonthSwitcher` 加"年"按钮后视觉拥挤**：若窄屏挤压，回退方案：把"年"按钮移到顶栏（返回 + "回看" + 年按钮），`MonthSwitcher` 保持原样。spec §4.5.1 已评估此替代方案不采纳但可作为回退。
3. **`buildYearGrid` 内 `activities.filter` 性能**：每月一次 `filter { YearMonth.from(it.date) == yearMonth }`，12 次遍历全年 activities。本地量级安全；若极端万+量级，回退为一次 `groupBy { YearMonth.from(it.date) }` 预分组。本轮不做（避免过早优化）。
4. **`HeatmapYearViewModelTest` flaky**：方案 A 已 5 次独立验证全绿（迭代 9），`HeatmapYearViewModel` 与 `HeatmapViewModel` 同范式，flaky 风险极低。若复现，回退：检查 `first { it.months.isNotEmpty() }` 是否需加超时（`withTimeout(5.seconds)`），或改 `advanceUntilIdle` + `state.value`（但方案 A 已证 `first{}` 更稳）。
5. **mini 格子 1dp 边框在低密度屏不可见**：1dp 在 mdpi（160dpi）屏约 1.6px，可能模糊。回退：改 0.5dp 或用 `Modifier.drawBehind` 画 1px 线。本轮先用 1dp（与 `DayCell` 2dp 同 `border` API，简单一致），手动验证若不可见再调整。
