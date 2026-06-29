# 时间可视化：今天 / 今年 / 这一生还剩多少时间

日期：2026-06-28
范围：新增「时间可视化」能力——首页顶栏展示「今天还有多少时间」，新增 `timeviz` 页面展示「今年还有多少时间」与「这一生还有多少时间」
前置：2026-06-28 首页构图重平衡 spec（顶栏一条带 + 8dp 彩虹条 + 2dp 黑色分隔线）已落地；minSdk 26，可用 `java.time`

## 问题

`MY_ORIGIN_GOAL.md` 第 26 行明确要求「时间可视化，今天还有多少时间，今年还有多少时间，这一生还有多少时间」，这是原始目标核心缺口，至今未实现。整个 App 目前只回答「我把时间花在了什么上」，不回答「我还剩多少时间」，缺了「轻量存在」哲学的另一半——让时间静静在场，不评判、不催促。

## 目标

- **今天还剩**：首页顶栏直接可见（无需点击），随系统时间每分钟自动刷新，格式 `Xh Ym`。
- **今年还剩**：新增 `timeviz` 页面，格式「今年还有 X 天 Y 小时」。
- **这一生还剩**：同 `timeviz` 页面，基于用户本地存储的生日 + 可调平均寿命（默认 80）计算，文案中性不焦虑。
- 隐私：生日纯本地（SharedPreferences），不出设备，不进 Room DB。
- 8-bit 美学一致：复用 `AppColors` 色板与「2dp 黑边 + 白底 Surface」模式，不引入新依赖。
- 入口可发现但不打扰：首页轻量入口跳 `timeviz`，主页面交互逻辑不变。

## 非目标

- **不联网**：纯本地 `java.time` 计算，无任何网络请求、远程 API、云同步。
- **不改 DB schema**：生日走 SharedPreferences，不动 `AppDatabase` / Entity / DAO / Repository。
- **不引入新依赖**：`build.gradle.kts` 不动；仅用已有的 compose / material3 / hilt。
- **不改 `TimelineViewModel`**：今天剩余文案在 UI 层用现有 `now` produceState 计算。
- **不改现有交互**：日期徽章点击仍开 `DateSelectorDialog`；底部 `BottomEntryBar` / `EntryDrawer` 行为不变；`EventCard` / `NoteRow` 不动。
- **不做热力图**（V2 范围）；不做寿命分布像素图等高级可视化。
- **不引入像素字体**：沿用现状系统字体，spec 注明后续可在 `AppTheme` 加像素字体统一应用。

## 设计

### 第 1 节 · 今天还剩多少时间（首页顶栏）

**展示位置**：`TimelineScreen` 顶栏一条带右侧统计文案区。复用现有 `now: Long` produceState（每 60s 刷新一次），不新增协程。

**计算**：基于 `now` 与「次日 00:00:00」的差值（即「今天还剩多少」按 24:00 为终点，这样 0:00 时显示 24h 0m，符合直觉）。用 `java.time`：

```kotlin
// TimeVizCalculator.todayRemaining(nowMillis): String
val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
val endOfToday = now.toLocalDate().plusDays(1).atStartOfDay()  // 次日 00:00
val dur = Duration.between(now, endOfToday)
val hours = dur.toHours()                    // 0..24
val minutes = (dur.toMinutes() % 60).toInt() // 0..59
"${hours}h ${minutes}m"
```

边界（必须正确，按「次日 00:00 为终点」）：
- `00:00:00` → `24h 0m`（满 24 小时）
- `00:00:01` → `23h 59m`
- `12:00:00` → `12h 0m`（满 12 小时）
- `23:59:00` → `0h 1m`
- `23:59:59` → `0h 0m`（剩余 1 秒，向下取整到分钟）

**顶栏文案改写**（仅当 `isToday(viewingDate)` 时显示「今天还剩」段；过去日不显示，因为「今天还剩」对过去日无意义）：

| 状态 | 现状文案 | 新文案 |
|---|---|---|
| 今天 · 有记录 | `5 件事 · 2 条随笔` | `今天还有 13h 24m · 5 件事 · 2 条随笔` |
| 今天 · 无记录 | `还没有记录` | `今天还有 13h 24m` |
| 非今天 · 有记录 | `5 件事 · 2 条随笔` | `5 件事 · 2 条随笔`（不变） |
| 非今天 · 无记录 | `还没有记录` | `还没有记录`（不变） |

**入口**：整段统计文案（不论今天与否）改为 `clickable` → 触发 `onTimeVizClick` 回调跳 `timeviz` 页。文案末尾不加 ▷ 等装饰（保持顶栏极简），靠点击行为本身提供可发现性——若实测发现率低，后续可加 1 个像素小图标。

**实现要点**：
- `TimelineScreen` 现有 `now` 已在 `produceState` 中每分钟更新，直接传入文案计算函数即可，无需新增 state。
- 文案色保持 `TextTertiary`、11sp、`FontWeight.Medium`，与现状一致。
- 计算 helper 放在 `TimeVizCalculator`（见第 6 节），便于单测。

### 第 2 节 · 今年还剩多少时间（timeviz 页）

**新页面**：`feature/timeviz/TimeVizScreen.kt`，由 `AppNavHost` 注册路由 `Routes.TIMEVIZ = "timeviz"`。

**计算**：基于 `now` 与「次年 1-1 00:00:00」的差值（与「今天还剩」一致，按下一周期起点为终点，1-1 0:00 时显示满 365 天）。

```kotlin
// TimeVizCalculator.yearRemaining(nowMillis): String
val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), ZoneId.systemDefault())
val endOfYear = LocalDate.of(now.year + 1, 1, 1).atStartOfDay()  // 次年 1-1 00:00
val dur = Duration.between(now, endOfYear)
val days = dur.toDays().toInt()           // 0..365
val hours = (dur.toHours() % 24).toInt()  // 0..23
"今年还有 ${days} 天 ${hours} 小时"
```

文案中性：「**今年还有** X 天 Y 小时」，不用「只剩」。

边界（按「次年 1-1 00:00 为终点」）：
- 1-1 00:00:00（平年）→ `今年还有 365 天 0 小时`（满 365 天）
- 1-1 00:00:00（闰年）→ `今年还有 366 天 0 小时`
- 6-30 12:00:00 → 中间值，按实际算
- 12-31 23:59:00 → `今年还有 0 天 0 小时`（剩余 1 分钟，向下取整到小时为 0）
- 12-31 23:59:59 → `今年还有 0 天 0 小时`

**页面布局**（自上而下，全部沿用 8-bit 像素风：2dp 黑边 + 白底 Surface + 直角）：

```
┌─────────────────────────────┐
│ [顶部 8dp 彩虹条]            │  ← 与首页一致，品牌标识
│ ‹ 返回    时间可视化          │  ← 顶栏：左返回箭头 + 标题
│ [2dp 黑色分隔线]             │
│                             │
│ ┌─────────────────────────┐ │
│ │ 今天                     │ │  ← PixelCard 风格分块
│ │ 13h 24m                 │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ 今年                     │ │
│ │ 还有 186 天 14 小时      │ │
│ └─────────────────────────┘ │
│ ┌─────────────────────────┐ │
│ │ 这一生                   │ │
│ │ 已走过 33 年             │ │
│ │ 假设 80 岁，还有约 47 年 │ │
│ │ [修改生日]  [- 80 +]     │ │  ← 修改入口 + 寿命 stepper
│ └─────────────────────────┘ │
└─────────────────────────────┘
```

每块结构：标题（`TextSecondary` 12sp）+ 主数字（`TextPrimary` 粗体 22sp）+ 副文案（`TextTertiary` 12sp）。块间距 12dp，外边距 16dp。

**「今天」块**：复用 `TimeVizCalculator.todayRemaining(now)`，每分钟刷新（页面内同样用 `produceState` 起一个 60s 循环，与首页一致；不起额外协程负担）。

**「今年」块**：同上 `yearRemaining(now)`，每分钟刷新。

**「这一生」块**：见第 3 节。

### 第 3 节 · 这一生还剩多少时间

**生日存储**：`SharedPreferences`，文件名 `timeviz_prefs.xml`，键 `birthday_millis`（Long，时间戳 00:00:00 UTC of birthday）、`lifespan_years`（Int，默认 80）。不进 Room。

**首次进入引导**：若 `birthday_millis` 未设（值为 0L 视为未设），「这一生」块不显示数字，改为显示引导：

```
┌─────────────────────────┐
│ 这一生                   │
│ 设置生日，看看时间还有多远 │
│ [设置生日]               │
└─────────────────────────┘
```

点击「设置生日」弹 `DatePickerDialog`（复用 Material3，与 `DateSelectorDialog` 同模式，但仅取年月日，存为 UTC 00:00 millis）。设好后块内即时刷新。

**修改生日**：已设状态下，「这一生」块底部显示「修改生日」文字按钮（`PixelOutlinedButton` 风），点击同样弹 `DatePickerDialog`，确认后覆盖存储。

**寿命调整**：块底部显示 `[- 80 +]` stepper（两个 26dp 像素方块按钮 + 中间数字），范围 60–120，步长 1，默认 80。点击 ± 即时刷新「这一生」计算。寿命也存 SharedPreferences。

**计算**：

```kotlin
// TimeVizCalculator.lifeRemaining(birthdayMillis, lifespanYears, nowMillis): LifeResult
data class LifeResult(
    val yearsLived: Int,        // 已完整走过的年数
    val yearsRemaining: Int,    // 剩余整年数（>= 0，clamp 到 0）
    val exceeded: Boolean       // 是否已超过 lifespanYears
)
```

逻辑：
- `birthDate = LocalDate.ofInstant(Instant.ofEpochMilli(birthdayMillis), ZoneId.systemDefault())`，取 `LocalDate`（截到天，避免时区导致跨日）。
- `nowDate = LocalDate.now(ZoneId.systemDefault())`。
- `yearsLived = ChronoUnit.YEARS.between(birthDate, nowDate).toInt()`（已完整走过的年数，向下取整）。
- `yearsRemaining = (lifespanYears - yearsLived).coerceAtLeast(0)`。
- `exceeded = yearsLived >= lifespanYears`。

**文案**（中性、不焦虑）：
- 正常：`已走过 33 年` + `假设 80 岁，还有约 47 年`
- 已超过寿命：`已走过 85 年` + `已超过假设的 80 岁，每一天都是赠礼`
- 未设生日：`设置生日，看看时间还有多远` + [设置生日] 按钮

「**约**」「**假设**」字眼不可省略——这是中性、不贩卖焦虑的承诺。不用「只剩」「剩余寿命」「倒计时」等词。

**隐私**：`timeviz_prefs.xml` 不加备份（默认 Android backup 可能上传到 Google Drive，但本 spec 不改 manifest 备份策略；后续如需更严格可加 `android:fullBackupContent` 排除规则，本 spec 不做）。

### 第 4 节 · 入口设计

**决策**：点首页顶栏统计文案 → 跳 `timeviz` 页。

权衡记录：
- **点统计文案跳转**（选定）：零新增 UI 元素，不破坏已重平衡的顶栏比例；统计文案本身就是「时间相关」语义，点击进入更详细的时间可视化在认知上连贯。可发现性中等，但「今天还有 Xh Ym」文案本身就是诱饵，看到的人会想点进去看更多。
- 顶栏加小图标：更明显，但顶栏已紧凑（36dp），再加图标破坏 2026-06-28 重平衡 spec 的成果。
- `BottomEntryBar` 加入口：底栏职责是「记事/随笔输入触发器」，混入导航入口职责不清。
- 顶部徽章长按：隐藏手势，可发现性最差。

**导航改动**：`AppNavHost` 加 `Routes.TIMEVIZ = "timeviz"` 与对应 `composable(Routes.TIMEVIZ) { TimeVizScreen(onBack = { navController.popBackStack() }) }`。`TimelineScreen` 新增 `onTimeVizClick: () -> Unit = {}` 参数，由 `AppNavHost` 传入 `{ navController.navigate(Routes.TIMEVIZ) }`。

### 第 5 节 · 8-bit 美学一致性

- **色板**：复用 `AppColors`——卡片底 `Surface`（白）、外框 `Color.Black`（2dp）、标题 `TextSecondary`、主数字 `TextPrimary`、副文案 `TextTertiary`、按钮主色 `Primary`（红）/ `Accent`（橙）。
- **卡片**：用 `Surface` + `border(2.dp, Color.Black)` + `RoundedCornerShape(0.dp)` + `shadowElevation = 2.dp`，与 `EventCard` / `NoteRow` 一致。可直接用 `PixelCard`。
- **按钮**：修改生日用 `PixelOutlinedButton`；stepper ± 用 26dp 黑边方块 `Box` + `clickable`，与 `BottomEntryBar` 图标盒风格一致。
- **彩虹条**：`timeviz` 页顶部保留 8dp 彩虹条（与首页统一品牌标识），代码直接复用首页的 `trimColors` 循环（可抽成 `RainbowTrim` composable 放 `PixelComponents.kt`，但本 spec 不强求抽离，复制 8 行代码也可）。
- **字体**：沿用现状系统字体（粗体）。spec 注明：后续若 `AppTheme` 引入像素字体（如 Fusion Pixel），全 app 自动统一，本 spec 不处理。
- **直角**：所有圆角 0dp，与全 app 一致。

### 第 6 节 · 架构与数据流

```
TimeVizScreen (Composable)
   │  hiltViewModel<TimeVizViewModel>()
   ▼
TimeVizViewModel @HiltViewModel
   │  inject: TimeVizPrefs (interface)
   │  state: StateFlow<TimeVizUiState>
   ▼
TimeVizPrefs (interface)  ←─── TimeVizPrefsImpl (SharedPreferences-backed, Hilt @Provides)
                                   │
                                   └─ timeviz_prefs.xml
                                       ├── birthday_millis: Long (0 = 未设)
                                       └── lifespan_years: Int (default 80)
```

**`TimeVizCalculator`**（object，纯函数，无 Android 依赖，易测试）：
- `fun todayRemaining(nowMillis: Long): String`
- `fun yearRemaining(nowMillis: Long): String`
- `fun lifeRemaining(birthdayMillis: Long, lifespanYears: Int, nowMillis: Long): LifeResult`

放在 `feature/timeviz/TimeVizCalculator.kt`。所有函数纯函数，参数全部显式传入（不依赖 `System.currentTimeMillis()`），便于单测覆盖边界。

**`TimeVizPrefs`**（interface，在 `feature/timeviz/TimeVizPrefs.kt`）：
```kotlin
interface TimeVizPrefs {
    fun getBirthdayMillis(): Long        // 0 表示未设
    fun setBirthdayMillis(millis: Long)  // 0 清除
    fun getLifespanYears(): Int          // 默认 80
    fun setLifespanYears(years: Int)
}
```

**`TimeVizPrefsImpl`**（同文件，private class）：构造接收 `SharedPreferences`，实现读写。`getLifespanYears` 在 key 不存在时返回 80。`getBirthdayMillis` 在 key 不存在时返回 0L。

**Hilt 模块**（`feature/timeviz/TimeVizModule.kt`，`@Module @InstallIn(SingletonComponent::class)`）：
```kotlin
@Provides @Singleton
fun provideTimeVizSharedPreferences(@ApplicationContext ctx: Context): SharedPreferences =
    ctx.getSharedPreferences("timeviz_prefs", Context.MODE_PRIVATE)

@Provides @Singleton
fun provideTimeVizPrefs(prefs: SharedPreferences): TimeVizPrefs = TimeVizPrefsImpl(prefs)
```

不动 `DataModule.kt`，feature 自包含。

**`TimeVizViewModel`**：
```kotlin
@HiltViewModel
class TimeVizViewModel @Inject constructor(
    private val prefs: TimeVizPrefs
) : ViewModel() {
    data class TimeVizUiState(
        val todayRemaining: String = "",
        val yearRemaining: String = "",
        val lifeResult: LifeResult? = null,   // null 表示未设生日
        val birthdayMillis: Long = 0L,
        val lifespanYears: Int = 80
    )
    private val _state = MutableStateFlow(refreshState(System.currentTimeMillis()))
    val state: StateFlow<TimeVizUiState> = _state.asStateFlow()

    fun refresh() { _state.value = refreshState(System.currentTimeMillis()) }
    fun setBirthday(year: Int, month: Int, day: Int) { /* 写 prefs + refresh */ }
    fun setLifespan(years: Int) { /* 写 prefs + refresh */ }

    private fun refreshState(now: Long): TimeVizUiState { /* 读 prefs + 调 Calculator */ }
}
```

- `setBirthday` 将 (year, month, day) 转 UTC 00:00 millis 存入 prefs（用 `Calendar.getInstance(TimeZone.getTimeZone("UTC"))` 设 00:00:00，与 `DateSelectorDialog` 的 UTC 处理一致），然后 `refresh()`。
- `setLifespan` clamp 到 [60, 120] 后存入 prefs，然后 `refresh()`。
- 状态用 `StateFlow`，UI 用 `collectAsStateWithLifecycle()`。
- 不依赖 `EventRepository` / `NoteRepository`。

**UI 层刷新**：`TimeVizScreen` 内起一个 `produceState` 每分钟调 `viewModel.refresh()`（与首页 `now` 模式一致）：
```kotlin
val state by viewModel.state.collectAsStateWithLifecycle()
produceState(initialValue = Unit) {
    while (true) {
        viewModel.refresh()
        delay(60_000L)
    }
}
```

**`TimelineScreen` 改动**（最小化）：
- 新增参数 `onTimeVizClick: () -> Unit = {}`。
- 顶栏右侧统计 `Text` 加 `clickable { onTimeVizClick() }`。
- 统计文案计算改为：`if (isToday(date)) "今天还有 ${TimeVizCalculator.todayRemaining(now)} · $baseStats" else baseStats`，其中 `baseStats` 为现状的 `"{n} 件事 · {n} 条随笔"` 或 `"还没有记录"`。今天且无记录时仅显示 `"今天还有 Xh Ym"`。
- 不动 `TimelineViewModel`。

**`AppNavHost` 改动**：
- `Routes` 加 `const val TIMEVIZ = "timeviz"`。
- `NavHost` 加 `composable(Routes.TIMEVIZ) { TimeVizScreen(onBack = { navController.popBackStack() }) }`。
- `TimelineScreen(onNotesClick = ..., onTimeVizClick = { navController.navigate(Routes.TIMEVIZ) })`。

### 第 7 节 · 数据存储

| 数据 | 存储位置 | Key | 类型 | 默认值 | 说明 |
|---|---|---|---|---|---|
| 生日 | `timeviz_prefs.xml` | `birthday_millis` | Long | 0L（未设） | UTC 00:00 millis，0 视为未设 |
| 平均寿命 | `timeviz_prefs.xml` | `lifespan_years` | Int | 80 | 范围 [60, 120]，UI stepper 步长 1 |

- 不写 Room，不改 `AppDatabase` / Entity / DAO。
- 不做加密（生日非敏感等级数据；纯本地存储已满足隐私承诺）。
- 不做自动备份策略改动（本 spec 范围外）。

## 涉及文件

| 文件 | 改动 |
|---|---|
| `app/src/main/java/com/shijiben/feature/timeviz/TimeVizCalculator.kt` | **新增**：纯函数计算 + `LifeResult` data class |
| `app/src/main/java/com/shijiben/feature/timeviz/TimeVizPrefs.kt` | **新增**：`TimeVizPrefs` interface + `TimeVizPrefsImpl` |
| `app/src/main/java/com/shijiben/feature/timeviz/TimeVizModule.kt` | **新增**：Hilt `@Module` 提供 `SharedPreferences` 与 `TimeVizPrefs` |
| `app/src/main/java/com/shijiben/feature/timeviz/TimeVizViewModel.kt` | **新增**：`@HiltViewModel` + `TimeVizUiState` + `setBirthday` / `setLifespan` / `refresh` |
| `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt` | **新增**：UI（顶栏 + 三块卡片 + 引导/修改/stepper） |
| `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` | **改**：新增 `onTimeVizClick` 参数；统计文案加「今天还有 Xh Ym」前缀（仅今天）；统计文案 `clickable` 跳 `timeviz` |
| `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | **改**：`Routes.TIMEVIZ` + `composable(Routes.TIMEVIZ)` + 给 `TimelineScreen` 传 `onTimeVizClick` |
| `app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt` | **新增**：纯函数单测（不依赖 Android，普通 JUnit） |
| `app/src/test/java/com/shijiben/feature/timeviz/TimeVizViewModelTest.kt` | **新增**：VM 单测，用 fake `TimeVizPrefs`（Robolectric，因 `@HiltViewModel` 测试需要；或直接 new VM 传入 fake） |

无需改：
- `app/build.gradle.kts`（不引入新依赖）
- `app/src/main/java/com/shijiben/data/` 下任何文件（不动 DB）
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`
- `app/src/main/java/com/shijiben/ui/theme/` 下任何文件（复用现有色板与组件）
- 现有任何测试文件

## 边界情况

### 今天还剩
- **00:00:00**：`24h 0m`（`Duration` 计算到次日 00:00，验证 `toHours()=24`、`toMinutes()%60=0`）。
- **00:00:01**：`23h 59m`。
- **12:00:00**：`12h 0m`（满 12 小时）。
- **23:59:00**：`0h 1m`。
- **23:59:59**：`0h 0m`（差 1 秒，向下取整到分钟为 0）。
- **跨时区**：用 `ZoneId.systemDefault()`，与现有 `Calendar.getInstance(TimeZone.getDefault())` 一致；用户跨时区旅行时随系统时区变化，可接受。
- **夏令时切换日**：`java.time` 与 `Duration` 自动处理，无需特殊代码；本 spec 不针对 DST 做 UI 提示。

### 今年还剩
- **1-1 00:00:00（平年）**：`今年还有 365 天 0 小时`（满 365 天）。
- **1-1 00:00:00（闰年）**：`今年还有 366 天 0 小时`。
- **6-30 12:00:00**（平年）：从 6-30 12:00 到次年 1-1 00:00，`toDays()` 按实际算。
- **12-31 23:59:00**：`今年还有 0 天 0 小时`（剩余 1 分钟，向下取整到小时为 0）。
- **12-31 23:59:59**：`今年还有 0 天 0 小时`。
- **闰年**：`Year.now().isLeap()` 由 `LocalDate` 自动处理，2-29 出生的人寿命计算也正确。

### 这一生还剩
- **未设生日**：`lifeResult = null`，UI 显示引导文案 + [设置生日] 按钮；不显示数字。
- **生日当天（今天 = 生日）**：`yearsLived = 0`，`yearsRemaining = 80`，文案「已走过 0 年 · 假设 80 岁，还有约 80 年」。
- **生日明天**：`yearsLived = 0`（未满 1 整年），同上。
- **已超过默认寿命**（如 85 岁 > 80）：`exceeded = true`，`yearsRemaining = 0`，文案「已走过 85 年 · 已超过假设的 80 岁，每一天都是赠礼」。
- **未来生日**（用户误选未来日期）：`yearsLived < 0`，`yearsRemaining = 80 - (-N) = 80 + N > 80`。UI 不报错，但文案会显示「已走过 -N 年」不合理。**处理**：在 `lifeRemaining` 中 clamp `yearsLived` 到 `>= 0`，若 `birthDate > nowDate` 视为未设（返回 `LifeResult(yearsLived=0, yearsRemaining=lifespanYears, exceeded=false)`），UI 也可在 `setBirthday` 时校验 `birthDate <= today`，否则不保存并 toast「生日不能晚于今天」。**本 spec 取**：`setBirthday` 时校验 `birthDate <= today`，晚于今天则忽略（不保存、不报错，静默失败；保持极简）。
- **闰年 2-29 出生**：`ChronoUnit.YEARS.between` 在非闰年的 2-28 返回 N、3-1 返回 N+1，行为符合直觉（已过完一整年才算 N+1）。
- **寿命 stepper 边界**：UI clamp [60, 120]；`setLifespan` 内部也 clamp，双保险。

### 入口与导航
- **从首页跳 timeviz 后返回**：`popBackStack` 回到首页，首页 `now` produceState 仍在跑，统计文案不丢。
- **timeviz 页面旋转 / 后台返回**：状态在 `ViewModel` 中，`ViewModel` 在配置变更中存活；后台返回时 `produceState` 重新启动并立即 `refresh()`，数据最新。
- **生日未设时点修改生日按钮**：首次引导按钮和已设状态下的「修改生日」按钮行为一致，都弹 `DatePickerDialog`。

## 测试

### 验证门
- `./gradlew :app:compileDebugKotlin`：类型检查（含 Hilt KSP）。
- `./gradlew :app:testDebugUnitTest`：全部单测通过（含现有 + 新增）。
- `./gradlew assembleDebug`：构建通过。

### 新增单测清单

#### `TimeVizCalculatorTest`（纯 JUnit，无 Robolectric）

`todayRemaining`：
1. `now = 2026-06-28T00:00:00` → `"24h 0m"`
2. `now = 2026-06-28T00:00:01` → `"23h 59m"`
3. `now = 2026-06-28T12:00:00` → `"12h 0m"`
4. `now = 2026-06-28T23:59:00` → `"0h 1m"`
5. `now = 2026-06-28T23:59:59` → `"0h 0m"`

`yearRemaining`：
6. `now = 2026-01-01T00:00:00`（平年）→ `"今年还有 365 天 0 小时"`
7. `now = 2024-01-01T00:00:00`（闰年）→ `"今年还有 366 天 0 小时"`
8. `now = 2026-06-30T12:00:00` → 按实际天数断言（先用计算器算出期望值写死）
9. `now = 2026-12-31T23:59:59` → `"今年还有 0 天 0 小时"`

`lifeRemaining`：
10. 生日 `1993-06-28`，`lifespan=80`，`now=2026-06-28`（生日当天）→ `yearsLived=33, yearsRemaining=47, exceeded=false`
11. 生日 `1993-06-29`，`lifespan=80`，`now=2026-06-28`（生日前一天）→ `yearsLived=32, yearsRemaining=48`
12. 生日 `1940-01-01`，`lifespan=80`，`now=2026-06-28`（已 86 岁，超 80）→ `yearsLived=86, yearsRemaining=0, exceeded=true`
13. 生日 `2026-06-28`（今天出生），`lifespan=80`，`now=2026-06-28` → `yearsLived=0, yearsRemaining=80`
14. 未设生日（`birthdayMillis=0`）→ VM 层不调用 `lifeRemaining`，UI 显示引导；`Calculator` 不测此分支（VM 层处理）。
15. 未来生日（`birthdayMillis` 对应 2030-01-01，`now=2026-06-28`）→ 由 VM `setBirthday` 拦截不保存，`Calculator` 不测此分支（VM 层处理）。可加一条 `Calculator` 测试断言「若强行传入未来生日，`yearsLived` clamp 到 0」，作为防御。

#### `TimeVizViewModelTest`（Robolectric，用 fake `TimeVizPrefs`）

16. 初始状态（prefs 空）：`state.lifeResult == null`、`state.birthdayMillis == 0L`、`state.lifespanYears == 80`、`state.todayRemaining` 非空、`state.yearRemaining` 非空。
17. `setBirthday(1993, 6, 28)` 后：`state.birthdayMillis > 0`、`state.lifeResult != null`、`state.lifeResult.yearsLived` 在 `now=2026` 时为 33。fake prefs 中 `getBirthdayMillis()` 返回刚存的值。
18. `setBirthday(2030, 1, 1)`（未来日期）：状态不变（`birthdayMillis` 仍为 0 或上一设定值），静默忽略。
19. `setLifespan(100)` 后：`state.lifespanYears == 100`、`state.lifeResult.yearsRemaining` 相应增加。
20. `setLifespan(30)`（低于 60）：clamp 到 60，`state.lifespanYears == 60`。
21. `setLifespan(200)`（高于 120）：clamp 到 120。
22. `setLifespan(80)` 后 `setLifespan(80)`：幂等，无副作用。
23. `refresh()` 后 `state.todayRemaining` 反映当前时间（用可控 `now` 注入或仅断言非空 + 格式正确）。注：`refresh()` 内部用 `System.currentTimeMillis()`，单测中不注入可控时钟；本条仅断言格式 `Regex("\\d+h \\d+m")` 匹配。

> 注：若要 `TimeVizViewModel` 完全可测（可控 now），可在 VM 加 `internal fun refreshAt(now: Long)` 测试入口，或把 `now: Long` 作为 `refresh` 的可选参数。本 spec 取后者：`fun refresh(now: Long = System.currentTimeMillis())`，测试传死值，生产传默认。这样第 23 条可精确断言。

### 现有测试影响
- `TimelineViewModelTest`：未改 `TimelineViewModel`，不受影响，应全部通过。
- `RecordingViewModelTest` / `EventRepositoryTest` / `NoteRepositoryTest`：未改相关代码，不受影响。
