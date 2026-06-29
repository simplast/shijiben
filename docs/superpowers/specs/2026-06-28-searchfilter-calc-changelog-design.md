# 迭代 19 设计 spec：SearchFilter + Calculator 边界用例补齐 + CHANGELOG.md 验收交付物

> 范围：**候选 1（边界用例补齐）+ 候选 2（CHANGELOG.md 验收交付物）组合**，由 orchestrator 拍板。
> 类型：候选 1 零生产代码改动（仅追加测试用例），候选 2 纯文档新增（零代码改动）。
> 本 spec 由 Design subagent 基于实读源码落盘，Coding / Test subagent 据此执行。

---

## 一、背景与目标

迭代 18 结束，`quality-scorecard.md` 已判定 100/100 满分，200 测试覆盖，四道门全绿。剩 2 轮即跑满 20 轮停止条件，过程价值优先于加分。

本轮目标有二：

1. **纯函数边界覆盖加固**（候选 1）：对三个已落地的纯函数（`filterAndMerge` / `HeatmapCalculator.buildGrid` / `TimeVizCalculator.lifeRemaining`）补齐边界用例，覆盖空输入、纯空白查询、稳定排序契约、`take` 负数边界、今天落在网格外、寿命 `>=` 临界。这些边界目前在 200 测试中无自动化守护。
2. **验收交付物**（候选 2）：新建 `CHANGELOG.md`（项目根），Keep a Changelog 格式，作为「开发者 / AI agent / 应用商店审核材料」三类读者的统一索引文档。用户向 README.md 已存在，本轮不复述。

## 二、硬约束

1. **绝对不做任何联网功能** —— 本地化 app 唯一原则，CHANGELOG.md 内容也不得引入任何联网特性描述。
2. **候选 1 零生产代码改动** —— 仅追加测试用例，不改既有用例，不动 `SearchViewModel.kt` / `HeatmapCalculator.kt` / `TimeVizCalculator.kt` 任何一行。
3. **候选 2 纯文档新增** —— 零代码改动，不触碰 `build.gradle.kts` / `AndroidManifest.xml` / 任何 `.kt` 文件。
4. **不触及 flaky 稳定区** —— 不动 `HeatmapViewModelTest.kt` / `ExportViewModelTest.kt` / 迭代9 方案 A（路由 Room executor 到 StandardTestDispatcher）相关任何文件。
5. **四道验证门全绿**：
   - 门1 `./gradlew compileDebugKotlin`
   - 门2 `./gradlew testDebugUnitTest --rerun-tasks`
   - 门3 `./gradlew assembleDebug`
   - 门4 `./gradlew assembleRelease --rerun-tasks`
6. **spec 用符号级引用**（类名 / 函数名 / 文件名），不用行号。
7. **测试用例命名风格** `method_behavior_expectedResult`，与现有 `SearchFilterTest` / `HeatmapCalculatorTest` / `TimeVizCalculatorTest` 风格一致。
8. **不修改既有测试用例**（即使发现命名风格略偏，亦不"顺手统一"）。

## 三、Orchestrator 决策点

| 决策点 | 选项 | 采纳 | 备注 |
|---|---|---|---|
| 范围组合 | 候选1 / 候选2 / 候选1+2 | **候选1+2** | orchestrator 已拍板 |
| SearchFilter 文件 | `SearchFilter.kt` | **不存在** | 实读发现 `filterAndMerge` 实际在 `SearchViewModel.kt`（见 §四、关键设计发现 1）；spec 沿用「SearchFilterTest」类名（既有测试类），不新建文件 |
| `recentLimit` 负数 | 拆 / 不拆 | **拆为 2 用例** | 实读 Kotlin stdlib `Iterable.take(n)` 发现 `require(n >= 0)`，负数抛 `IllegalArgumentException` 而非返回空（见 §四、关键设计发现 3） |
| CHANGELOG 版本号 | v1.0.0 / v1.0 | **v1.0** | 实读 `app/build.gradle.kts` 发现 `versionName = "1.0"`（非 "1.0.0"），`AboutScreen` 已对齐 `BuildConfig.VERSION_NAME`（迭代5 闭合）。CHANGELOG 用 v1.0 与代码完全一致（见 §四、关键设计发现 6） |
| CHANGELOG 语言 | 中 / 英 | **中文** | 项目惯例（README.md / AGENT.md / ARCHITECTURE.md / specs/ 全中文） |
| buildGrid 补 1 或 2 | 1 / 2 | **1** | Discover 报告留余地；实读后确认 `buildGrid_todayOutsideYearMonth_notMarked` 一例足以覆盖"今天在 yearMonth 网格外"边界，补 2 会越界触及"补位格 isToday"设计问题（见 §七、风险评估） |

## 四、设计内容

### 关键设计发现（实读源码后，部分与 Discover 报告假设不符）

**发现 1：`SearchFilter.kt` 不存在**
- Discover 报告路径 `app/src/main/java/com/shijiben/feature/search/SearchFilter.kt` 实际不存在。
- `filterAndMerge` 函数定义在 `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt`，签名：
  ```
  internal fun filterAndMerge(
      events: List<EventEntity>,
      notes: List<NoteEntity>,
      query: String,
      recentLimit: Int = SearchViewModel.RECENT_LIMIT
  ): List<SearchViewModel.SearchItem>
  ```
- 既有测试类 `SearchFilterTest.kt` 直接调 `filterAndMerge`（同包 `com.shijiben.feature.search`，可见 internal）。本轮沿用此类，**不新建 SearchFilter.kt 文件**。

**发现 2：`SearchItem.sortKey` 与稳定排序契约**
- `SearchViewModel.SearchItem.EventItem.sortKey = event.startTime`
- `SearchViewModel.SearchItem.NoteItem.sortKey = note.timestamp`
- `filterAndMerge` 在空查询与非空查询两个分支均构造 `(events.map{EventItem} + notes.map{NoteItem})` 后 `.sortedByDescending { it.sortKey }`。events 在 `+` 左侧，notes 在右侧。
- Kotlin stdlib `sortedByDescending` 实现为 TimSort（稳定排序），同 `sortKey` 时保持原列表相对顺序，故同 `startTime`/`timestamp` 的 `EventItem` 必在 `NoteItem` 之前。
- `eventAndNoteSameSortKey_stableSort` 用例断言此契约。

**发现 3：`Iterable.take(n)` 在负数时抛 `IllegalArgumentException`（修正 Discover 报告）**
- Discover 报告假设 `recentLimit_zeroOrNegative_returnsEmpty`：`take(0)` / `take(-1)` 均返回空。
- 实读 Kotlin stdlib `Iterable.take(n)` 实现：`require(n >= 0) { "Requested element count $n is less than zero." }`，`n == 0` 返回 `emptyList()`，`n < 0` 抛 `IllegalArgumentException`。
- `filterAndMerge` 内 `.take(recentLimit)` 不做 `coerceAtLeast(0)` 防御，调用方 `SearchViewModel` 用默认值 `RECENT_LIMIT = 50`，正常路径不会传负数；但若调用方传负数会崩。
- **决策**：拆为 2 个用例，分别文档化两种边界：
  - `recentLimit_zero_returnsEmpty`：`take(0)` 返回 `emptyList()`
  - `recentLimit_negative_throwsIllegalArgumentException`：`take(-1)` 抛 `IllegalArgumentException`（用 `assertThrows` 验证 stdlib 契约，暴露过滤层不 clamp 的设计事实）

**发现 4：`buildGrid` 的 `isToday` 不检查 `isInMonth`**
- `HeatmapCalculator.buildGrid` 中 `val isToday = date == today`，**仅比较 date 相等**，不检查 `YearMonth.from(date) == yearMonth`。
- 对 `buildGrid_todayOutsideYearMonth_notMarked`（`yearMonth=2026-07`, `today=2026-06-28`）：2026-07-01 是周三，`firstCol=3`，`gridStart=2026-06-29`（周一），网格首格是 6-29，6-28 不在网格内，故全网格无 `isToday=true` 的 cell。与 `buildYearGrid_todayOutsideYearNotMarked`（year=2025, today=2026-01-15）平行。
- **不补**「today 落在补位格被标记 isToday=true && isInMonth=false」用例——那会暴露一个设计层面问题（是否应在 `isToday` 计算时加 `isInMonth` 守卫），超出本轮"零生产代码改动"范围。仅在 §七、风险评估备案。

**发现 5：`lifeRemaining` 的 `exceeded` 边界是 `>=`**
- `TimeVizCalculator.lifeRemaining` 中 `val exceeded = yearsLived >= lifespanYears`，恰好等于时 `exceeded=true`。
- `val yearsRemaining = (lifespanYears - yearsLived).coerceAtLeast(0)`，恰好等于时 `yearsRemaining=0`。
- 对 `bday=1946-06-28, now=2026-06-28, lifespan=80`：`ChronoUnit.YEARS.between(1946-06-28, 2026-06-28) = 80`（恰好整 80 年），`yearsLived=80`, `yearsRemaining=0`, `exceeded=true`。Discover 报告假设正确。
- 现有 `lifeRemaining_exceededLifespan_returns0RemainingAndExceededTrue`（bday=1940-01-01, yearsLived=86）测的是远超，未测临界，本轮补临界。

**发现 6：`versionName = "1.0"`，非 `"1.0.0"`（修正 Discover 报告）**
- Discover 报告假设「版本号 v1.0.0 与 build.gradle.kts versionName 一致」。
- 实读 `app/build.gradle.kts`：`versionCode = 1`, `versionName = "1.0"`。
- `quality-scorecard.md` 已关闭项 `appVersion`：迭代5 已接 `BuildConfig.VERSION_NAME`，顺带修齐 `AboutScreen` "1.0.0" 历史不一致 → 即 `AboutScreen` 现显示 "1.0"。
- **决策**：CHANGELOG.md 用 `v1.0`（与 `build.gradle.kts` versionName + `AboutScreen` 显示完全一致），不用 `v1.0.0`。

**发现 7：`SearchViewModelTest.queryWithSpaces_trimmedBeforeMatch` 已存在，测的是非纯空白**
- 既有用例 `queryWithSpaces_trimmedBeforeMatch`（`SearchViewModelTest.kt`）测的是 `"  abc  "`（非纯空白，trim 后命中），走非空查询分支。
- 本轮 `whitespaceOnlyQuery_treatedAsEmpty` 测的是 `"   "` / `"  \t  "`（纯空白，trim 后 `isEmpty()`），走空查询分支。两者不重复。

---

### §4.1 SearchFilterTest 用例（新增 5 个，原 10 个 → 15 个）

> 文件：`app/src/test/java/com/shijiben/feature/search/SearchFilterTest.kt`
> 沿用既有 `event(...)` / `note(...)` helper（同文件已定义），不新增 helper。
> 全部直调 `filterAndMerge`（internal，同包可见），无 Android/Room 依赖。

#### 4.1.1 `emptyQuery_emptyInputs_returnsEmptyList`
- **输入**：`events = emptyList()`, `notes = emptyList()`, `query = ""`, `recentLimit = 50`（默认）
- **路径**：`trimmed = "".trim() = ""`, `trimmed.isEmpty() == true` → 空查询分支 → `(emptyList + emptyList).sortedByDescending{}.take(50)` → `emptyList`
- **断言**：`assertThat(items).isEmpty()`
- **价值**：文档化"空查询 + 空输入"返回空 list，与 `emptyQuery_returnsRecentLimitedAndSorted`（有输入）形成完整边界对照。

#### 4.1.2 `whitespaceOnlyQuery_treatedAsEmpty`
- **输入**：`events = listOf(event(id=1, startTime=1000L))`, `notes = listOf(note(id=10, timestamp=2000L))`, 两次调用：
  - `query = "   "`（纯空格）
  - `query = "  \t  "`（空格+制表符）
- **路径**：`trimmed = "".trim()`（两种均 trim 为 `""`），`trimmed.isEmpty() == true` → 空查询分支
- **断言**：两次调用均 `items.hasSize(2)`，且 `items[0].sortKey == 2000L`、`items[1].sortKey == 1000L`（与 `emptyQuery_returnsRecentLimitedAndSorted` 同序）
- **价值**：补纯空白（与 `SearchViewModelTest.queryWithSpaces_trimmedBeforeMatch` 的 `"  abc  "` 非纯空白形成互补），文档化 `String.trim()` 移除 `Char.isWhitespace()` 全集（含制表符）。

#### 4.1.3 `eventAndNoteSameSortKey_stableSort`
- **输入**：`events = listOf(event(id=1, startTime=1000L))`, `notes = listOf(note(id=10, timestamp=1000L))`（同 `sortKey=1000L`），`query = ""`
- **路径**：空查询分支 → `(events.map{EventItem} + notes.map{NoteItem})` 中 `EventItem` 在前 → `.sortedByDescending{sortKey}` 稳定排序，同 key 保持原序
- **断言**：
  - `items.hasSize(2)`
  - `items[0]` `isInstanceOf(SearchViewModel.SearchItem.EventItem::class.java)`，`(items[0] as EventItem).event.id == 1L`
  - `items[1]` `isInstanceOf(SearchViewModel.SearchItem.NoteItem::class.java)`，`(items[1] as NoteItem).note.id == 10L`
- **价值**：文档化 `sortedByDescending` 稳定排序契约 + `events + notes` 拼接顺序。若未来有人将 `+` 两侧对调或改用非稳定排序，此用例会失败。
- **Coding 注意**：若实跑发现顺序相反（理论上不应），以实际为准并回头修订 spec；但基于 `sortedByDescending`（TimSort）+ `events + notes` 拼接顺序，`EventItem` 在前是确定的。

#### 4.1.4 `recentLimit_zero_returnsEmpty`
- **输入**：`events = listOf(event(id=1, startTime=1000L), event(id=2, startTime=2000L))`, `notes = listOf(note(id=10, timestamp=3000L))`, `query = ""`, `recentLimit = 0`
- **路径**：空查询分支 → `.sortedByDescending{}.take(0)` → `emptyList()`
- **断言**：`assertThat(items).isEmpty()`
- **价值**：文档化 `take(0)` 返回空（Kotlin stdlib 契约）。

#### 4.1.5 `recentLimit_negative_throwsIllegalArgumentException`
- **输入**：`events = listOf(event(id=1, startTime=1000L))`, `notes = emptyList()`, `query = ""`, `recentLimit = -1`
- **路径**：空查询分支 → `.sortedByDescending{}.take(-1)` → `require(n >= 0)` 失败 → 抛 `IllegalArgumentException("Requested element count -1 is less than zero.")`
- **断言**：`assertThrows(IllegalArgumentException::class.java) { filterAndMerge(events, notes, query = "", recentLimit = -1) }`（Truth 风格 `assertThat { ... }.isInstanceOf(IllegalArgumentException::class.java)` 亦可，与项目依赖 `com.google.truth:truth:1.4.4` 一致）
- **价值**：文档化 `filterAndMerge` 不 clamp `recentLimit` 负数的设计事实（调用方 `SearchViewModel` 用默认值 `RECENT_LIMIT=50`，正常路径不传负数）；暴露边界，防未来"以为会返回空"的误判。
- **Coding 注意**：Truth 断言异常用 `assertThat { block }.isInstanceOf(...)` 风格（truth 1.4.4 支持 Kotlin `assertThat { }` lambda）；或退回 JUnit `@Test(expected = IllegalArgumentException::class)`。Coding subagent 选其一即可，保持与项目现有风格一致（项目其他测试多用 Truth `assertThat`，但未见异常断言先例，Coding 可选 Truth Kotlin lambda 或 JUnit expected）。

---

### §4.2 HeatmapCalculatorTest 用例（新增 1 个，原 22 个 → 23 个）

> 文件：`app/src/test/java/com/shijiben/feature/heatmap/HeatmapCalculatorTest.kt`
> 沿用既有 helper（`h`, `day16h` 不需要，本例无活动数据）。
> 直调 `HeatmapCalculator.buildGrid`，纯 JUnit。

#### 4.2.1 `buildGrid_todayOutsideYearMonth_notMarked`
- **输入**：`yearMonth = YearMonth.of(2026, 7)`, `activities = emptyList()`, `today = LocalDate.of(2026, 6, 28)`
- **路径**：
  - `firstOfMonth = 2026-07-01`（周三，`dayOfWeek.value = 3`）
  - `firstCol = 3`
  - `gridStart = 2026-07-01.minusDays(2) = 2026-06-29`（周一）
  - 网格首格 = 2026-06-29，末格 = 2026-06-29 + 41 天 = 2026-08-09
  - `today = 2026-06-28` 不在 [2026-06-29, 2026-08-09] 区间 → 全 42 格无 `date == today`
- **断言**：
  - `grid.hasSize(6)`，每行 `hasSize(7)`，`grid.flatten().hasSize(42)`（与既有 `buildGrid_2026_06_firstCellIsJune1` 同基础断言，确认网格结构正确）
  - `grid.flatten().filter { it.isToday }.isEmpty()`（核心断言：全网格无 isToday cell）
  - 额外断言 `grid[0][0].date == LocalDate.of(2026, 6, 29)`（确认 gridStart 计算正确，6-29 才是首格，6-28 不在网格内）
- **价值**：与 `buildYearGrid_todayOutsideYearNotMarked`（year=2025, today=2026-01-15 → 全年无 isToday）平行，补 `buildGrid` 单月版"今天在网格外"边界。
- **Coding 注意**：日期计算（2026-07-01 是周三）已实读 java.time `DayOfWeek` 语义核对：`dayOfWeek.value` 周一=1..周日=7，周三=3。Coding 无需重新推导，按 spec 输入断言即可。

---

### §4.3 TimeVizCalculatorTest 用例（新增 2 个，原 14 个 → 16 个）

> 文件：`app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt`
> 沿用既有 helper：`@Before` 固定时区为 UTC，`utcMidnightMillis(y,m,d)` 产出 bday（与 `lifeRemaining_onBirthday_returns33Years` 同模式），`localMillis(y,m,d,h,mi,s)` 产出 now（因时区固定为 UTC，二者数值等价，但语义分工：bday 用"当地 00:00"，now 带 12 小时避免边界）。

#### 4.3.1 `lifeRemaining_exactlyLifespanYears_exceededTrue`
- **输入**：`bday = utcMidnightMillis(1946, 6, 28)`, `lifespanYears = 80`, `now = localMillis(2026, 6, 28, 12, 0, 0)`
- **路径**：
  - `birthDate = 1946-06-28`, `nowDate = 2026-06-28`
  - `rawYearsLived = ChronoUnit.YEARS.between(1946-06-28, 2026-06-28) = 80`（恰好整 80 年，无余数）
  - `yearsLived = 80.coerceAtLeast(0) = 80`
  - `yearsRemaining = (80 - 80).coerceAtLeast(0) = 0`
  - `exceeded = 80 >= 80 = true`
- **断言**：
  - `r.yearsLived == 80`
  - `r.yearsRemaining == 0`
  - `r.exceeded == true`（核心：验证 `>=` 边界，恰好等于时 exceeded=true）
- **价值**：与既有 `lifeRemaining_exceededLifespan_returns0RemainingAndExceededTrue`（bday=1940-01-01, yearsLived=86，远超）形成"远超"与"恰好等于"对照，文档化 `>=` 临界。

#### 4.3.2 `lifeRemaining_oneYearBeforeLifespan_exceededFalse`
- **输入**：`bday = utcMidnightMillis(1947, 6, 28)`, `lifespanYears = 80`, `now = localMillis(2026, 6, 28, 12, 0, 0)`
- **路径**：
  - `rawYearsLived = ChronoUnit.YEARS.between(1947-06-28, 2026-06-28) = 79`
  - `yearsLived = 79`, `yearsRemaining = (80 - 79) = 1`, `exceeded = 79 >= 80 = false`
- **断言**：
  - `r.yearsLived == 79`
  - `r.yearsRemaining == 1`
  - `r.exceeded == false`
- **价值**：与 4.3.1 形成"恰好等于 (exceeded=true)"与"差一年 (exceeded=false)"对照，锁定 `>=` 边界语义（差一天/差一年均 false，恰好等于才 true）。

---

### §4.4 CHANGELOG.md 设计

> 文件：`CHANGELOG.md`（项目根 `/Users/doer/dev/shijiben/CHANGELOG.md`，新建）
> 格式：[Keep a Changelog](https://keepachangelog.com/) 风格
> 语言：中文（项目惯例）
> 版本：**v1.0 (2026-06-28)** 单版本（与 `app/build.gradle.kts` `versionName = "1.0"` + `AboutScreen` 显示一致，**不用 v1.0.0**——见 §四、关键设计发现 6）
> 目标读者：开发者 / AI agent / 应用商店审核材料（非终端用户——用户向已在 `README.md`）
> 数据源：`docs/superpowers/loop/iteration-log.md`（18 轮记录）+ `docs/superpowers/loop/quality-scorecard.md`（里程碑列表）

#### 章节大纲

```
# 更新日志 (CHANGELOG)

> 本文件记录「史记本」(shijiben) 的版本演进，面向开发者 / AI agent / 应用商店审核材料。
> 终端用户向文档见 [README.md](./README.md)。
> 格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号对齐
> `app/build.gradle.kts` 的 `versionName`。

## [v1.0] - 2026-06-28

### 核心功能
- 事件 CRUD + 耗时 + 状态流转（进行中/已完成/未开始）+ 顺延
- 随笔（notes）独立 CRUD，与事件并列展示
- 时间轴（Timeline）按日聚合，事件 + 随笔混合渲染
- V3 搜索：events.title/note + notes.content 全文检索（LIKE 内存过滤）
- 热力图回看：月视图（6×7 网格）+ 年视图（12 月迷你月历拼贴）

### 时间可视化
- 今天还剩多少时间（"24h 0m" 格式）
- 今年还剩多少时间（"今年还有 N 天 M 小时" 格式）
- 这一生还剩多少时间（yearsLived / yearsRemaining / exceeded 三字段，含未来生日防御 clamp）

### 数据可移植性
- 数据导出：JSON + SAF（系统文件选择器），含 appVersion / exportedAt 元数据
- 数据导入：JSON 导入，REPLACE 幂等，备份/恢复闭环
- 卸载/换机不丢数据

### 上架成熟度
- release 构建配置：签名 + 混淆 + 资源缩减 + ProGuard
- keystore.properties 缺失时 fallback 产出 unsigned APK（CI/全新 clone 不破）
- splash 启动屏（米白 + 像素图标 → 首页无缝切换）
- 隐私政策页（本地化 app，无联网，无数据上报）

### 文档
- README.md（面向人类的仓库入口）
- AGENT.md（面向 AI agent 的项目上下文 + 调试技巧 + FAQ）
- ARCHITECTURE.md（4 视角：模块依赖图 + 数据流 + 状态管理 + 导航图，ASCII art）
- docs/superpowers/specs/（18+ 轮设计 spec 历史）
- docs/superpowers/loop/（iteration-log.md 迭代日志 + quality-scorecard.md 质量评分）

### 质量保障
- 208 测试覆盖（7 个 ViewModel + Repository 边界 + 纯函数单测，含本轮 +8 边界用例）
- 四道验证门全绿：compileDebugKotlin / testDebugUnitTest --rerun-tasks / assembleDebug / assembleRelease --rerun-tasks
- flaky test 根治（迭代9 方案 A：路由 Room executor 到 StandardTestDispatcher，5 次独立验证全绿）
- 已知 caveat：release APK 真机启动验证（需真机，orchestrator 无法执行）
```

#### 引用约定
- **引用 spec 索引而非复述细节**：CHANGELOG.md 不展开每个功能的实现细节，只列里程碑；细节读者按需查阅 `docs/superpowers/specs/` 对应 spec。
- **不引入任何联网特性描述**：硬约束 1。
- **版本号 v1.0 与 `build.gradle.kts` `versionName` 一致**：硬约束（修正 Discover 报告 v1.0.0 假设）。

---

### §4.5 不改动文件清单（零生产代码改动）

以下文件**本轮一律不动**（即使发现可优化点）：

**生产代码（不动）**：
- `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt`（`filterAndMerge` 所在）
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapCalculator.kt`
- `app/src/main/java/com/shijiben/feature/timeviz/TimeVizCalculator.kt`
- `app/build.gradle.kts`（含 `versionName`，不改）
- `app/src/main/AndroidManifest.xml`
- 任何其他 `app/src/main/**` 文件

**测试文件（仅追加，不改既有用例）**：
- `app/src/test/java/com/shijiben/feature/search/SearchFilterTest.kt`：仅追加 5 个用例
- `app/src/test/java/com/shijiben/feature/heatmap/HeatmapCalculatorTest.kt`：仅追加 1 个用例
- `app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt`：仅追加 2 个用例
- `app/src/test/java/com/shijiben/feature/search/SearchViewModelTest.kt`：**不动**（已有 `queryWithSpaces_trimmedBeforeMatch`，本轮不重复）
- `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt`：**不动**（flaky 稳定区）
- `app/src/test/java/com/shijiben/feature/settings/ExportViewModelTest.kt`：**不动**（flaky 稳定区）
- 任何其他既有测试文件：**不动**

**flaky 稳定区（绝对不动）**：
- 迭代9 方案 A 相关：`MainCoroutineRule.kt` / 任何 `Room.executor` 路由配置 / `HeatmapViewModelTest.kt` / `ExportViewModelTest.kt`

**文档（仅新增 1 个）**：
- `CHANGELOG.md`（项目根，新建）
- 既有 `README.md` / `AGENT.md` / `ARCHITECTURE.md` / `docs/` 下任何文件：**不动**

## 五、涉及文件清单（4 文件，零生产代码改动）

| # | 文件 | 类型 | 操作 | 用例/章节变化 |
|---|---|---|---|---|
| 1 | `app/src/test/java/com/shijiben/feature/search/SearchFilterTest.kt` | 测试 | 追加 5 用例 | 10 → 15 |
| 2 | `app/src/test/java/com/shijiben/feature/heatmap/HeatmapCalculatorTest.kt` | 测试 | 追加 1 用例 | 22 → 23 |
| 3 | `app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt` | 测试 | 追加 2 用例 | 14 → 16 |
| 4 | `CHANGELOG.md`（项目根） | 文档 | 新建 | 0 → 1（v1.0 单版本） |

**生产代码改动：0 文件 0 行。**
**flaky 稳定区改动：0 文件 0 行。**

## 六、验证标准

### 四道验证门（全绿）

| 门 | 命令 | 预期 |
|---|---|---|
| 1 | `./gradlew compileDebugKotlin` | BUILD SUCCESSFUL |
| 2 | `./gradlew testDebugUnitTest --rerun-tasks` | BUILD SUCCESSFUL，0 失败 |
| 3 | `./gradlew assembleDebug` | BUILD SUCCESSFUL，产出 debug APK |
| 4 | `./gradlew assembleRelease --rerun-tasks` | BUILD SUCCESSFUL，产出 release APK（可能 unsigned，fallback 机制） |

### 测试总数预期

- 迭代 18 结束：**200 测试**（quality-scorecard.md 迭代18 里程碑）
- 本轮新增：**8 测试**（5 + 1 + 2）
- 迭代 19 结束预期：**208 测试**

> Test subagent 若实际跑出 200 + 8 ± 0 之外，需回头核对：是否漏加 / 多加 / 既有用例被误改。允许的偏差仅来自"既有用例计数口径差异"（如 parameterized 拆分），否则视为回归。

### 各测试类用例数预期

| 测试类 | 迭代18 | 本轮新增 | 迭代19 |
|---|---|---|---|
| `SearchFilterTest` | 10 | +5 | 15 |
| `HeatmapCalculatorTest` | 22 | +1 | 23 |
| `TimeVizCalculatorTest` | 14 | +2 | 16 |
| 小计 | 46 | +8 | 54 |

### CHANGELOG.md 验收
- 文件存在于 `/Users/doer/dev/shijiben/CHANGELOG.md`
- 含 `[v1.0] - 2026-06-28` 单版本标题
- 含 6 个分类章节（核心功能 / 时间可视化 / 数据可移植性 / 上架成熟度 / 文档 / 质量保障）
- 不含任何联网特性描述
- 不复述 spec 细节（只列里程碑 + 引用 specs/ 索引）

## 七、风险评估

| # | 风险 | 概率 | 缓解 |
|---|---|---|---|
| 1 | `eventAndNoteSameSortKey_stableSort` 顺序断言反了（理论上 EventItem 在前） | 极低 | 基于 Kotlin stdlib TimSort + `events + notes` 拼接顺序，EventItem 在前是确定的；若实跑反了，Coding 以实际为准回头修订 spec（不强行改测试凑断言） |
| 2 | `recentLimit_negative_throwsIllegalArgumentException` 用例在 Truth 1.4.4 下 `assertThat { }` lambda 语法不可用 | 低 | Coding 退回 JUnit `@Test(expected = IllegalArgumentException::class)` 或 `@Rule ExpectedException`；项目已依赖 `junit:junit:4.13.2`，兼容 |
| 3 | `buildGrid_todayOutsideYearMonth_notMarked` 日期计算（2026-07-01 是周三）有误 | 极低 | 已实读 `java.time.DayOfWeek` 语义核对；Coding 若用 `YearMonth.of(2026,7).atDay(1).dayOfWeek.value` 打印应为 3 |
| 4 | CHANGELOG.md 版本号 v1.0 与 Discover 报告 v1.0.0 不一致引发质疑 | 低 | spec §四、发现 6 已说明：v1.0 与 `build.gradle.kts` `versionName = "1.0"` + `AboutScreen` 显示完全一致，Discover 报告 v1.0.0 是假设错误 |
| 5 | 补 `buildGrid` 用例时发现"补位格 isToday=true && isInMonth=false"设计问题想顺手修 | 中 | 硬约束 2：零生产代码改动。本轮**不修**，仅在 spec 备案；如需修，单独立项下轮处理 |
| 6 | Coding 顺手"统一"既有用例命名风格（如 `emptyQuery_*` vs `titleMatch`） | 中 | 硬约束 8：不修改既有测试用例。即使发现命名风格略偏，亦不"顺手统一" |
| 7 | Test subagent 跑门2 时 daemon 卡住（迭代9 backlog `daemon-stall`） | 低 | 非 flaky 复现，重跑即可；若复现可 `./gradlew --stop` 后重跑 |

### 备案（不在本轮处理，仅记录）

- **`buildGrid` 补位格 isToday 设计问题**：`val isToday = date == today` 不检查 `isInMonth`。若 `today` 落在补位格（如 `yearMonth=2026-06`, `today=2026-07-05`，7-05 是 6 月网格补位），补位格会被标记 `isToday=true && isInMonth=false`。UI 层是否过滤此情况未确认。本轮不补测试用例（会暴露设计问题），不修代码（超出零改动范围）。建议后续迭代单独立项评估：是否应在 `isToday` 计算时加 `&& YearMonth.from(date) == yearMonth` 守卫。

## 八、预估加分

**+0**。

理由：
- `quality-scorecard.md` 已判 100/100 满分，所有维度满分，无加分空间。
- 本轮过程价值在：
  1. 纯函数边界覆盖（`take` 负数 / 稳定排序契约 / `>=` 临界 / 今天在网格外）文档化，防未来回归；
  2. CHANGELOG.md 验收交付物补齐，三类读者（开发者 / AI agent / 应用商店审核）有统一索引文档。
- 与迭代 15-18 同模式：已达满分，加分 0，过程价值在防回归 + 文档完整性。

## 九、Coding subagent 注意事项

1. **零生产代码改动**：只动 3 个测试文件 + 1 个新建 CHANGELOG.md。任何 `app/src/main/**` 改动都是越界。
2. **不新建 SearchFilter.kt**：`filterAndMerge` 在 `SearchViewModel.kt`，既有 `SearchFilterTest.kt` 同包可见 internal，直接调即可。
3. **沿用既有 helper**：
   - `SearchFilterTest.kt` 的 `event(...)` / `note(...)` helper 已定义，不重复定义。
   - `TimeVizCalculatorTest.kt` 的 `utcMidnightMillis(...)` / `localMillis(...)` + `@Before` 固定时区为 UTC，沿用。
   - `HeatmapCalculatorTest.kt` 的 `h` / `day16h` 不需要（本例无活动数据）。
4. **用例插入位置**：追加到文件末尾（`}` 之前），不插在既有用例中间，避免改动既有行。
5. **`recentLimit_negative_throwsIllegalArgumentException` 异常断言风格**：
   - 首选 Truth Kotlin lambda：`assertThat { filterAndMerge(...) }.isInstanceOf(IllegalArgumentException::class.java)`
   - 若 truth 1.4.4 lambda 不可用，退回 JUnit：`@Test(expected = IllegalArgumentException::class.java)` 注解风格
   - **不要**用 try-catch 手写断言（项目无此先例）
6. **`eventAndNoteSameSortKey_stableSort` 顺序**：断言 `items[0]` 是 `EventItem`、`items[1]` 是 `NoteItem`。若实跑反了，**不强行改测试凑断言**，回头核对 spec（理论上 EventItem 在前，基于 `events + notes` 拼接 + TimSort 稳定排序）。
7. **CHANGELOG.md 版本号 v1.0**（非 v1.0.0）：与 `build.gradle.kts` `versionName = "1.0"` + `AboutScreen` 显示一致。
8. **CHANGELOG.md 不复述 spec 细节**：只列里程碑 + 引用 `docs/superpowers/specs/` 索引。每章节 3-6 条要点即可，不展开。
9. **不联网**：CHANGELOG.md 内容不得引入任何联网特性描述（如"云同步""账号登录""推送通知"等）。
10. **不提交 git**：Coding 完成后交 Test subagent 验证，由 orchestrator 决定是否 commit。

## 十、Test subagent 注意事项

1. **四道门全绿**是硬指标，任何一门红即阻断。
2. **门2 `--rerun-tasks` 必须带**：强制重跑，避免增量缓存掩盖问题（与迭代6 起既定验证流程一致）。
3. **测试总数预期 208**（200 + 8）。若偏差：
   - 207 / 209：先核对是否漏加 / 多加，再判定；
   - < 207 或 > 209：视为回归，阻断并报告。
4. **各测试类用例数预期**：
   - `SearchFilterTest`：15（原 10 + 新 5）
   - `HeatmapCalculatorTest`：23（原 22 + 新 1）
   - `TimeVizCalculatorTest`：16（原 14 + 新 2）
   - Test subagent 可用 `./gradlew testDebugUnitTest --tests "com.shijiben.feature.search.SearchFilterTest"` 等单类过滤跑，确认用例数。
5. **CHANGELOG.md 验收清单**：
   - 文件存在于项目根
   - 含 `[v1.0] - 2026-06-28` 单版本标题
   - 含 6 个分类章节（核心功能 / 时间可视化 / 数据可移植性 / 上架成熟度 / 文档 / 质量保障）
   - 不含联网特性描述
   - 不复述 spec 细节（只列里程碑）
   - 版本号 v1.0（非 v1.0.0）
6. **不验证 flaky 稳定区**：不动 `HeatmapViewModelTest.kt` / `ExportViewModelTest.kt`，但门2 全量跑会包含它们——若它们红，是 flaky 复现，重跑即可（迭代9 方案 A 后 5 次独立验证全绿，不应红）。
7. **daemon 卡住**（迭代9 backlog `daemon-stall`）：若门2 第5次 `--rerun-tasks` daemon 卡住 150s 无输出，`./gradlew --stop` 后重跑，非代码缺陷。
8. **CHANGELOG.md 不参与编译/测试**：门1-4 不会因 CHANGELOG.md 内容报错；CHANGELOG.md 的"验收"由 Test subagent 人工核对（按本节第 5 条清单）。
9. **独立验证身份**：Test subagent 应独立实读 3 个测试文件确认用例数与命名，不盲信 Coding 报告。
