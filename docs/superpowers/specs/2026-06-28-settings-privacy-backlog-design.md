# 事记本 迭代 3 设计 spec：文档对齐 + 隐私政策 + 修 backlog + 设置页骨架

> 日期：2026-06-28
> 迭代：3（自迭代 loop）
> 范围：4 子项组合（A 设置页骨架 / B 隐私政策 / C 修 6 项 backlog / D 文档对齐）
> 前置：迭代 1（时间可视化）、迭代 2（热力图月视图）已完成；本 spec 不改业务代码以外的 DB schema、不引入新依赖、不联网。
> 验证门：`./gradlew :app:compileDebugKotlin` + `./gradlew :app:testDebugUnitTest` + `./gradlew assembleDebug` 三道全绿。

---

## 1. 问题

### 1.1 backlog（迭代 1/2 遗留 6 项）

| ID | 文件 | 问题 | 严重度 |
|----|------|------|--------|
| M1 | `TimeVizViewModel.kt:55-58` | `setBirthday` 存 UTC 00:00 millis，`TimeVizCalculator.lifeRemaining:49-52` 用 `systemDefault()` 读 → 负偏移时区（美东 -5）生日错位一天。中国 UTC+8 不受影响，属 spec 设计盲点。 | minor（时区相关） |
| M2 | `TimeVizViewModelTest.kt:84-97`（#18） | `setBirthday` 内部用真实 `LocalDate.now()` 判今天，断言「2030 是未来」依赖墙钟 < 2030，时间炸弹。污染测试可信度。 | minor（时间炸弹） |
| B1 | `HeatmapScreen.kt:256-288`（DayCell） | 方块只渲染色阶无日期数字，点击精准度受损。 | minor（可用性） |
| B2 | `HeatmapViewModel.kt:33` | `private val currentMonth = YearMonth.now(...)` 构造时捕获，跨自然月停留不更新，`canGoNext`/`isCurrentMonth` 失准。 | minor（跨月边界） |
| N1 | `TimeVizScreen.kt:45` | 未使用 import `Accent`。 | nit |
| N2 | `TimeVizScreen.kt:264` | StepperBox disabled 态硬编码 `Color(0xFFCBD5E1)`，未走主题色。 | nit |

### 1.2 无隐私政策

App 收集生日/寿命等敏感信息（虽纯本地），上架与合规需明确「不联网/不采集」声明。当前无任何隐私政策入口与文案。

### 1.3 无设置入口

数据导出（迭代 4+ 候选）天然需要设置页承载；当前无设置页，也无「关于」入口。需本轮铺骨架。

### 1.4 文档不一致（已实际伤害前两轮 Discover）

- `AGENT.md:92` V1 仍写「标签」（标签 2026-06-27 已移除）；`:93` V2 描述「热力图 + 时间可视化 + 随笔完整化」过时（timeviz/heatmap 已落地）；项目结构缺 `feature/timeviz`、`feature/heatmap`、`feature/settings` 目录；无 spec 索引。
- `plans/README.md:6` 写「all TODO」与表格 011-014「DONE」自相矛盾（迭代 1 Discover 据此误判方案 C 未实现）。
- `docs/2026-06-22-shijiben-design.md:49-50` 仍写「heatmap/time_viz V2 未建」（已建）；`:115` 「右下角悬浮加号」（已被底部双 block 替代，见 `2026-06-28-homepage-composition-rebalance-design.md`）。

---

## 2. 目标

1. **A 设置页骨架**：新增 `SettingsScreen` + `AboutScreen`，AppNavHost 加 `SETTINGS`/`ABOUT` 路由，确定入口位置。
2. **B 隐私政策**：AboutScreen 内含纯本地隐私声明（不联网/不采集/不分享/不分析/卸载即清/无账号无云同步/生日寿命仅本机计算）。
3. **C 修 backlog**：6 项全部修复，M1 时区统一基准、M2 clock 注入、B1 方块补数字、B2 currentMonth 改计算属性、N1 删 import、N2 disabled 走主题色。
4. **D 文档对齐**：AGENT.md / plans/README.md / 2026-06-22-shijiben-design.md 三份文档同步现状。

---

## 3. 非目标

- 不做数据导出（设置页仅预留位置，UI 隐藏或置灰，注释标记下轮）。
- 不做 release 构建、应用图标/splash、UI 测试（Compose UI test）。
- 不引入新依赖（build.gradle.kts 不动；`java.time.Clock` 为 JDK 自带，minSdk 26 支持，不算新依赖）。
- 不改 DB schema。
- 不彻底解决 HeatmapViewModel 跨月停留自动刷新（需 clock 注入 + 定时 emit，超本轮范围；本轮仅改计算属性解决「操作时取最新」）。
- 不新增 Activity 承载隐私政策（AboutScreen 即可）。

---

## 4. 设计

### 4.1 子项 A：设置页骨架

#### 4.1.1 入口位置决策（⚠️ 需 orchestrator 拍板）

**现状顶栏**（`TimelineScreen.kt:159-225`）：
```
Row(SpaceBetween) {
  左 Row { 日期徽章(可点跳日期选择) + 26dp 热力图方块 }
  右 Text { 统计文案(可点跳 timeviz) }
}
```

三方案评估：

| 方案 | 位置 | 评估 | 结论 |
|------|------|------|------|
| 1 | 顶栏左侧热力图方块右边加 26dp 齿轮方块 | 语义聚合（左侧=导航入口区：日期/回看/设置；右侧=信息区：统计）。左侧 3 元素 ≈ 140dp，右侧统计 ≈ 150dp，360dp 屏够，320dp 紧（统计文案加 ellipsis 兜底）。视觉左工具栏 + 右信息，平衡。 | **推荐** |
| 2 | 顶栏最右（统计文案右边）加齿轮 | 右侧塞两元素，统计文案被挤压；语义上设置归导航非信息。 | 次选 |
| 3 | 底部 BottomEntryBar 加设置入口 | 破坏底部对称双 block 构图（记事/随笔），且底部是输入触发区非导航区。 | 否决 |
| 4 | timeviz 页加设置入口 | 藏二级页太深，设置需全局可达。 | 否决 |

**推荐方案 1**：左侧热力图方块右边加 26dp 齿轮方块（`Icons.Default.Settings`，2dp 黑边白底，与热力图方块同风格），点击 `onSettingsClick` 跳 SETTINGS 路由。统计文案加 `maxLines = 1` + `TextOverflow.Ellipsis` 兜底窄屏。

**理由**：语义清晰（左导航/右信息）、视觉平衡（左工具栏三方块 + 右文案）、复用现有 26dp 方块范式、窄屏可控。

> ⚠️ **orchestrator 拍板点**：是否采纳方案 1（左侧加齿轮）？若担心左侧三元素过密，可改方案 2（右侧）。

#### 4.1.2 SettingsScreen（Stateless composable）

无状态，不建 ViewModel。结构：

```
Column {
  RainbowTrim()  // 8dp 彩虹条，与 timeviz/heatmap 一致（复制 8 行，不改 PixelComponents）
  顶栏 Row { IconButton(返回) + Text("设置") }
  2dp 黑色分隔线
  Column(verticalScroll) {
    SettingsRow(title = "关于事记本", onClick = onAboutClick)   // → ABOUT 路由
    SettingsRow(title = "隐私政策", onClick = onAboutClick)      // → ABOUT 路由（滚动到政策区，或同页）
    // 预留：数据导出（下轮实现）
    // SettingsRow(title = "数据导出", enabled = false)  // 注释标记，UI 不渲染或置灰
  }
}
```

- `SettingsRow`：8-bit 风格行项（2dp 黑边白底卡片 + 左标题 + 右 `›` 箭头），padding 12dp，直角，复用 `Surface`/`Border`/`TextPrimary`。
- 「关于」与「隐私政策」都跳 AboutScreen（同页两区，隐私政策在关于下方）。简化路由，避免再加 PRIVACY 路由。

#### 4.1.3 AboutScreen（Stateless composable）

无状态。结构：

```
Column {
  RainbowTrim()
  顶栏 Row { IconButton(返回) + Text("关于事记本") }
  2dp 黑色分隔线
  Column(verticalScroll, padding 16dp) {
    // 区块 1：关于
    Section(title = "关于事记本") {
      Text("事记本 ShiJiBen")
      Text("一个纯粹的本地时间记录工具，受《奇特的一生》启发。")
      Text("版本 1.0.0")  // 硬编码，下轮接 BuildConfig
    }
    // 区块 2：隐私政策（见 4.2）
    Section(title = "隐私政策") { ... }
  }
}
```

- `Section`：8-bit 卡片（2dp 黑边白底 + 标题 + 内容），与 timeviz 的 `TimeVizCard` 同风格。
- 版本号硬编码「1.0.0」，注释标记下轮接 `BuildConfig.VERSION_NAME`（本轮不动 build.gradle.kts）。

#### 4.1.4 AppNavHost 改动

```kotlin
object Routes {
    const val TIMELINE = "timeline"
    const val NOTES = "notes"
    const val TIMEVIZ = "timeviz"
    const val HEATMAP = "heatmap"
    const val SETTINGS = "settings"   // 新增
    const val ABOUT = "about"         // 新增
}
```

- TIMELINE composable 加 `onSettingsClick = { navController.navigate(Routes.SETTINGS) }` 传给 TimelineScreen。
- TimelineScreen 签名加 `onSettingsClick: () -> Unit = {}`（默认空，向后兼容）。
- 新增 `composable(Routes.SETTINGS) { SettingsScreen(onBack = popBackStack, onAboutClick = navigate(ABOUT)) }`。
- 新增 `composable(Routes.ABOUT) { AboutScreen(onBack = popBackStack) }`。

#### 4.1.5 新增文件

- `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt`
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`

包名 `com.shijiben.feature.settings`。

---

### 4.2 子项 B：隐私政策文案

AboutScreen 内 Section「隐私政策」，文案 8-bit 风格但严肃清晰（用 `TextSecondary` 正文 + `TextPrimary` 标题，不卖萌不焦虑）：

```
事记本是一款纯本地时间记录应用。本隐私政策说明数据处理方式：

• 不联网：App 不发起任何网络请求，无远程 API、云同步、推送、统计 SDK、广告 SDK。
• 不采集：不收集任何个人信息、设备信息、使用行为数据。
• 不分享、不分析：数据不离开本设备，无任何上传、共享、分析行为。
• 本机存储：所有事件、随笔、生日、寿命设置仅存于本机 Room 数据库与 SharedPreferences。
• 卸载即清除：卸载 App 后所有数据随之删除，无残留、无备份。
• 无账号：无需注册登录，无账号体系。
• 敏感数据说明：生日与假设寿命仅用于「时间可视化」页面的这一生剩余时间计算，存于本机 SharedPreferences，不出设备。
```

- 不需要单独 Activity，AboutScreen 承载。
- 不需要 checkbox / 同意按钮（纯展示声明）。
- 文案固定字符串，不做 i18n（项目现状全中文硬编码）。

---

### 4.3 子项 C：修 backlog

#### 4.3.1 M1 时区 bug（TimeVizViewModel + Calculator）

**根因**：`setBirthday` 用 `Calendar.getInstance(TimeZone.getTimeZone("UTC"))` 存 UTC 00:00 millis；`Calculator.lifeRemaining` 用 `LocalDate.ofInstant(..., ZoneId.systemDefault())` 读。负偏移时区（UTC-5）：UTC 00:00 → 当地前一天 19:00 → `LocalDate` 是前一天 → 生日错位一天。

**修复**：统一基准为「当地 00:00」。

`TimeVizViewModel.setBirthday`（`:55-58`）改：
```kotlin
// 旧
val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
cal.set(year, month - 1, day, 0, 0, 0)
cal.set(Calendar.MILLISECOND, 0)
prefs.setBirthdayMillis(cal.timeInMillis)

// 新
val localMillis = LocalDate.of(year, month, day)
    .atStartOfDay(ZoneId.systemDefault())
    .toInstant()
    .toEpochMilli()
prefs.setBirthdayMillis(localMillis)
```

`TimeVizCalculator.lifeRemaining` 读法**不变**（`LocalDate.ofInstant(Instant.ofEpochMilli(birthdayMillis), ZoneId.systemDefault())` —— 当地 00:00 在 systemDefault 下 `LocalDate` 正确为生日当天）。

`TimeVizCalculator.lifeRemaining` 注释（`:44`）「生日 UTC 00:00 millis」→「生日当地 00:00 millis」。

**BirthdayPickerDialog**（`TimeVizScreen.kt:308-340`）**不改**：它用 UTC 解析 `selectedDateMillis` 仅取 y/m/d 三个 Int 传 `setBirthday`，不传 millis，无时区问题。

**注意**：已存旧 UTC 00:00 生日的用户，修复后读取会偏移最多 1 天（中国 UTC+8：UTC 00:00 → 当地 08:00，`LocalDate` 仍同一天，无影响；负偏移时区：旧数据 UTC 00:00 → 当地前一天，修复后 `lifeRemaining` 会把生日算前一天，`yearsLived` 可能少 1）。因中国不受影响且负偏移时区用户极少，不做数据迁移。spec 注明此局限。

#### 4.3.2 M2 测试时间炸弹（TimeVizViewModel + Module）

**根因**：`setBirthday:49` 用 `LocalDate.now(ZoneId.systemDefault())` 判今天，测试 #18 断言 2030 是未来，依赖墙钟 < 2030。

**修复**：注入 `java.time.Clock`。

`TimeVizModule.kt` 加：
```kotlin
@Provides @Singleton
fun provideClock(): Clock = Clock.systemDefaultZone()
```

`TimeVizViewModel` 构造改：
```kotlin
@HiltViewModel
class TimeVizViewModel @Inject constructor(
    private val prefs: TimeVizPrefs,
    private val clock: Clock              // 新增
) : ViewModel() {
```

`setBirthday:49` 改：
```kotlin
val today = LocalDate.now(clock)   // 旧：LocalDate.now(ZoneId.systemDefault())
```

`refresh`/`refreshState` 不变（已有 `now: Long` 参数，生产用 `System.currentTimeMillis()`，测试用 fixedNow）。

**测试改动**：`TimeVizViewModelTest` 构造 VM 时传 `Clock.fixed(Instant.parse("2026-06-28T12:00:00Z"), ZoneOffset.UTC)`，#18 断言「2030-01-01 是未来」基于固定 clock，不依赖墙钟。

```kotlin
private val fixedClock: Clock = Clock.fixed(
    Instant.parse("2026-06-28T12:00:00Z"), ZoneOffset.UTC
)
// setup: vm = TimeVizViewModel(fakePrefs, fixedClock)
```

> 注：`fixedNow`（Long）与 `fixedClock`（Clock）指向同一时刻，保持一致。测试时区仍固定 UTC（`@Before` setDefault），clock 也用 UTC，基准统一。

#### 4.3.3 B1 方块补数字（HeatmapScreen.DayCell）

`DayCell`（`:256-288`）补日期数字：

```kotlin
Box(modifier = cellModifier, contentAlignment = Alignment.Center) {
    Text(
        text = cell.date.dayOfMonth.toString(),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = when (cell.level) {
            0, 1 -> TextTertiary    // 浅背景用浅灰字
            else -> Color.White     // level 2-4 深背景用白字反色
        }
    )
}
```

- 字号 9sp（8-10sp 区间，9sp 平衡可读与不挤）。
- 补位格（`!cell.isInMonth`）已 `return`，不显示数字。
- 未来日（`isFuture`）数字显示，整体 `alpha(0.5f)` 已应用于 cellModifier，数字随之半透明。
- 今天红边框方块数字仍显示（level 0-1 用 TextTertiary，2-4 用白）。

#### 4.3.4 B2 currentMonth 跨月（HeatmapViewModel）

`HeatmapViewModel.kt:33` 改计算属性：
```kotlin
// 旧
private val currentMonth: YearMonth = YearMonth.now(ZoneId.systemDefault())

// 新
private val currentMonth: YearMonth
    get() = YearMonth.now(ZoneId.systemDefault())
```

影响点（均改为每次取最新）：
- `state` 的 `flatMapLatest` 里 `isCurrentMonth = ym == currentMonth`、`canGoNext = ym < currentMonth`（`:45-46`）
- `nextMonth` 的 `if (_selectedMonth.value < currentMonth)`（`:58`）
- `goToCurrentMonth` 的 `_selectedMonth.value = currentMonth`（`:64`）

**局限**：`state` 是 `WhileSubscribed(5000)` 缓存，跨月停留且不操作时不自动 emit。需用户触发 `previousMonth`/`nextMonth`/`goToCurrentMonth` 或重新订阅才刷新。彻底解决需 clock 注入 + 定时 emit，超本轮范围（非目标）。

**测试**：验证 `nextMonth` 在当前月边界正确（当前月 `nextMonth` 不前进）。跨月停留自动刷新不测（非本轮修复范围）。

#### 4.3.5 N1 删未用 import

`TimeVizScreen.kt:45` 删除 `import com.shijiben.ui.theme.Accent`。

#### 4.3.6 N2 disabled 走主题色

`AppColors.kt` 新增（黑白灰边框区，与 `Border`/`BorderLight` 同区）：
```kotlin
val Disabled = Color(0xFFCBD5E1)        // slate-300，disabled 统一色（与历史硬编码一致）
```

`TimeVizScreen.kt:264` 改：
```kotlin
val bg = if (enabled) Primary else Disabled   // 旧：Color(0xFFCBD5E1)
```

`import com.shijiben.ui.theme.Disabled` 加入。

**可选清理**（不强制，Coding 顺手可做）：`PixelComponents.kt:49`（PixelButton）、`:83`（PixelOutlinedButton）的 `Color(0xFFCBD5E1)` 也替换为 `Disabled`，统一全 app disabled 色。若改需加 import。`HeatmapScreen.kt:175,183,202,209` 的 `Color(0xFFCBD5E1)` / `Color(0xFF94A3B8)` 亦可统一（但涉及 disabled 文字色 `0xFF94A3B8` 需另加 `DisabledText`，scope 蔓延，本轮仅修 N2 指定的 TimeVizScreen:264）。

---

### 4.4 子项 D：文档对齐

#### 4.4.1 AGENT.md

- `:92` V1 移除「标签」：`时间轴 + 记录 + 8-bit 主题 + 随笔基础`（删「+ 标签」）。
- `:93` V2 修正：`时间可视化（今天/今年/一生）+ 热力图月视图回看 + 随笔完整化（已落地）`。V3 改：`设置页 + 数据导出/备份 + 其他打磨`。
- 项目结构（`:33-37` feature 区）补：
  ```
  feature/
    heatmap/   # 热力图月视图回看（HeatmapScreen, HeatmapViewModel, HeatmapCalculator）
    notes/     # 随笔列表与编辑
    recording/ # 记录弹窗
    settings/  # 设置 + 关于/隐私政策（SettingsScreen, AboutScreen）
    timeline/  # 时间轴主视图
    timeviz/   # 时间可视化（TimeVizScreen, TimeVizViewModel, TimeVizCalculator, TimeVizPrefs）
  ```
- 末尾加「## Spec 索引」：
  ```
  - [2026-06-27-top-bottom-redesign-design.md](docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md) — 顶底重设计 + 标签移除
  - [2026-06-28-homepage-composition-rebalance-design.md](docs/superpowers/specs/2026-06-28-homepage-composition-rebalance-design.md) — 首页构图重平衡（底部双 block 替代悬浮加号）
  - [2026-06-28-time-visualization-design.md](docs/superpowers/specs/2026-06-28-time-visualization-design.md) — 时间可视化
  - [2026-06-28-heatmap-design.md](docs/superpowers/specs/2026-06-28-heatmap-design.md) — 热力图月视图
  - [2026-06-28-settings-privacy-backlog-design.md](docs/superpowers/specs/2026-06-28-settings-privacy-backlog-design.md) — 设置页 + 隐私政策 + backlog 修复（本 spec）
  ```

#### 4.4.2 plans/README.md

`:6` 「Plans 011-014, all TODO.」→「Plans 011-014, all DONE.」（与表格 `:24-27` DONE 状态一致）。

#### 4.4.3 docs/2026-06-22-shijiben-design.md

- `:49-50` 项目结构 `features/` 下 `heatmap/   # 热力图（V2 未建）` → `heatmap/   # 热力图月视图（已实现）`；`time_viz/  # 时间可视化（V2 未建）` → `timeviz/  # 时间可视化（已实现）`；补 `settings/  # 设置 + 关于/隐私政策（已实现）`。
- `:115` 「右下角悬浮加号按钮：统一记录入口」→「底部双 block 入口（记事 + 随笔）：统一记录入口，详见 [2026-06-28-homepage-composition-rebalance-design.md]」。
- `:146-159` 回看与可视化（V2）区：标注「✅ 已实现（2026-06-28）」。
- `:166-188` 分期计划：V2 项打 `[x]`（热力图/时间可视化/随笔完整化均已完成）；V3 改「设置页 + 数据导出/备份 + 其他打磨」。

---

## 5. 涉及文件

| 子项 | 文件 | 改动类型 |
|------|------|----------|
| A | `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` | 新增 |
| A | `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt` | 新增 |
| A | `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | 改（加 SETTINGS/ABOUT 路由 + onSettingsClick） |
| A | `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` | 改（顶栏加齿轮入口 + onSettingsClick 参数 + 统计文案 ellipsis） |
| B | （含于 A 的 AboutScreen） | — |
| C-M1 | `app/src/main/java/com/shijiben/feature/timeviz/TimeVizViewModel.kt` | 改（setBirthday 存当地 00:00） |
| C-M1 | `app/src/main/java/com/shijiben/feature/timeviz/TimeVizCalculator.kt` | 改（注释更新） |
| C-M2 | `app/src/main/java/com/shijiben/feature/timeviz/TimeVizViewModel.kt` | 改（加 clock 注入） |
| C-M2 | `app/src/main/java/com/shijiben/feature/timeviz/TimeVizModule.kt` | 改（provideClock） |
| C-M2 | `app/src/test/java/com/shijiben/feature/timeviz/TimeVizViewModelTest.kt` | 改（fixedClock 替代墙钟） |
| C-B1 | `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` | 改（DayCell 补数字） |
| C-B2 | `app/src/main/java/com/shijiben/feature/heatmap/HeatmapViewModel.kt` | 改（currentMonth 计算属性） |
| C-N1 | `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt` | 改（删 import Accent） |
| C-N2 | `app/src/main/java/com/shijiben/ui/theme/AppColors.kt` | 改（加 Disabled） |
| C-N2 | `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt` | 改（用 Disabled） |
| D | `AGENT.md` | 改（V1/V2/V3 + 结构 + spec 索引） |
| D | `plans/README.md` | 改（all DONE） |
| D | `docs/2026-06-22-shijiben-design.md` | 改（同步现状） |
| 测试 | `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt` | 改/补（nextMonth 边界） |
| 测试 | `app/src/test/java/com/shijiben/feature/timeviz/TimeVizViewModelTest.kt` | 补（M1 时区测试） |

---

## 6. 边界情况

1. **M1 时区边界**：负偏移时区（UTC-5/-8）旧数据 UTC 00:00 → 当地前一天，修复后 `lifeRemaining` 读取会偏 1 天。中国 UTC+8 无影响。不做数据迁移（用户极少，影响微小）。新数据统一当地 00:00，正确。
2. **M1 生日=今天**：`candidate.isAfter(today)` false（今天不 after 今天），正常保存。修复后存今天当地 00:00，`yearsLived` 计算正确。
3. **M2 clock 注入**：生产 `Clock.systemDefaultZone()` 与原 `LocalDate.now(ZoneId.systemDefault())` 等价，零行为变化。测试 `Clock.fixed` 指向 2026-06-28，#18 断言 2030 未来基于固定时刻，无墙钟依赖。
4. **B1 数字可读性**：level 2（`0xFF34D399` 中绿）白字对比度 borderline（WCAG 约 2.3:1），但 8-bit 美学优先 + 数字仅辅助定位（点击仍靠整块），可接受。level 0/1 浅背景用 TextTertiary，level 3/4 深背景白字对比度高。
5. **B1 未来日数字**：未来日 `alpha(0.5f)`，数字半透明，不可点但仍可见（符合「未来日置灰」语义）。
6. **B2 currentMonth 计算属性**：每次访问调 `YearMonth.now()`，开销可忽略（毫微秒级）。跨月停留不自动刷新属已知局限（需用户触发操作）。
7. **设置页入口容量**：顶栏左侧三元素（徽章+热力图+齿轮）在 320dp 屏可能挤压统计文案。统计文案加 `maxLines=1` + `TextOverflow.Ellipsis`，窄屏显示「今天还有 12h 3…」可接受。
8. **AboutScreen 版本号**：硬编码「1.0.0」，下轮接 `BuildConfig.VERSION_NAME`。本轮不动 build.gradle.kts。
9. **隐私政策无同意按钮**：纯声明展示，无 checkbox/同意流程。符合「不联网不采集」无需用户授权的定位。

---

## 7. 测试

### 7.1 验证门（三道全绿）

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew assembleDebug`

### 7.2 新增/修改单测清单

| ID | 测试文件 | 测试内容 |
|----|----------|----------|
| M1-1 | `TimeVizViewModelTest.kt` | **新增**：时区 UTC-5（`TimeZone.getTimeZone("America/New_York")`）下 `setBirthday(1993,6,28)`，断言 `lifeResult.yearsLived` 与 UTC 时区一致（不错位成 6-27 导致少 1 年）。fixedNow 设 2026-06-28，yearsLived=33。 |
| M1-2 | `TimeVizViewModelTest.kt` | **新增**：时区 UTC+8 下同上断言（回归保护，确保修复不破坏中国时区）。 |
| M2-1 | `TimeVizViewModelTest.kt` | **改**：#18 `setBirthday_future_silentlyIgnored` 用 `fixedClock`（2026-06-28）构造 VM，断言 2030-01-01 被静默忽略，不依赖墙钟。 |
| M2-2 | `TimeVizViewModelTest.kt` | **改**：setup 用 `TimeVizViewModel(fakePrefs, fixedClock)`，其余测试 #16/17/19-23 保持原断言（fixedClock 与 fixedNow 同时刻，yearsLived 不变）。 |
| B2-1 | `HeatmapViewModelTest.kt` | **改/补**：`nextMonth_atCurrentMonth_doesNotAdvance`——当前月 `nextMonth()` 不前进（`canGoNext` false）。验证计算属性在操作时取最新。 |
| B2-2 | （非目标，不测） | 跨月停留自动刷新需 clock 注入，超本轮。 |
| B1 | （无单测） | 方块数字渲染为 UI 层，项目无 Compose UI test（非目标），仅 device 人工校验。 |
| N1/N2 | （无单测） | nit 级，编译通过即验证。 |
| A | （无单测） | 设置页 Stateless composable，无 VM 逻辑。入口可达由 device 校验：点齿轮 → SettingsScreen → 关于 → AboutScreen。 |

### 7.3 硬约束回归检查

- 不联网：无新增网络代码（SettingsScreen/AboutScreen 纯静态）。
- 不引入新依赖：`java.time.Clock` 为 JDK 自带（minSdk 26），build.gradle.kts 不动。
- 不改 DB schema：本轮零 DB 改动。
- 8-bit 美学一致：SettingsScreen/AboutScreen 复用 RainbowTrim + 2dp 黑边卡片 + 直角；disabled 色统一。
- 文案中性：隐私政策用「声明」非「承诺」，不卖萌不焦虑；设置项标题中性。

---

## 8. 风险与回退

- **M1 旧数据偏移**：负偏移时区用户生日可能少算 1 天。回退方案：若反馈，下轮加一次性迁移（检测 UTC 00:00 模式 + 时区偏移修正）。本轮不做。
- **M2 Hilt 注入 Clock**：若 `provideClock` 与现有 Module 冲突，回退方案用 `nowProvider: () -> LocalDate` 通过 `@Provides` 注入 lambda（Hilt 支持提供函数类型）。优先 Clock（标准方案）。
- **设置页入口拥挤**：若 device 验证顶栏左侧三元素过密，回退方案 2（右侧加齿轮）。

---

## 附录：orchestrator 拍板点

1. **设置页入口位置**（§4.1.1）：推荐方案 1（顶栏左侧热力图右边加 26dp 齿轮方块）。需确认是否接受左侧三元素密度，或改方案 2（右侧）。
2. **M1 旧数据迁移**（§4.3.1）：本轮不做迁移，负偏移时区用户生日可能少 1 天。需确认接受此局限，或要求加迁移。
3. **B2 跨月自动刷新**（§4.3.4）：本轮仅改计算属性，不解决停留时自动刷新。需确认接受，或要求加 clock 注入 + 定时 emit（超本轮范围）。
4. **N2 可选清理**（§4.3.6）：PixelComponents/HeatmapScreen 的 `0xFFCBD5E1` 硬编码是否一并清理，还是仅修 N2 指定的 TimeVizScreen:264。默认仅修指定点。
