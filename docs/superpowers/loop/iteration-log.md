# 自迭代 Loop 迭代日志

> 持久化 loop 状态。每轮迭代追加一节。跨 compaction / 跨会话可恢复。
> 主 orchestrator 读此文件即可恢复 loop 上下文。

## 硬约束（每轮必须注入 subagent prompt）

1. **绝对不做任何联网功能** —— 这是本地化 app 的唯一原则。不得引入任何网络请求、远程 API、云同步、推送、统计 SDK、广告 SDK。新增依赖前必须确认纯本地。
2. 验证门：`./gradlew :app:compileDebugKotlin` + `./gradlew :app:testDebugUnitTest` + `./gradlew assembleDebug` 三道闸必须全绿。
3. Coding subagent 与 Test subagent 必须是不同 subagent（防 self-preferential bias）。

## 停止条件

- 至少跑满 20 轮迭代
- 且主 orchestrator 判断达到 $100 付费上架质量线（见 quality-scorecard.md）

## 当前 loop 状态

- 进行中迭代：1
- 已完成迭代：0
- 下一动作：Discover subagent 扫描全项目，找最高价值改进点

## 基线 (Iteration 0, 2026-06-28)

**项目**：事记本 ShiJiBen，纯本地时间记录 app，受《奇特的一生》启发。8-bit 像素美学。

**技术栈**：Kotlin + Jetpack Compose + Room 2.6.1 + Hilt 2.52 + Coroutines/Flow。minSdk 26, targetSdk 34。

**已完成**：plans 001-014 全部 DONE（UI 打磨批 + 重设计清理批）。

**待办 spec**：`docs/superpowers/specs/2026-06-28-homepage-composition-rebalance-design.md`（首页构图与留白重平衡 方案 C）—— 视觉打磨，未实现。

**原始目标缺口（vs $100 上架线）**：
- 时间可视化（今天/今年/一生还有多少时间）—— MY_ORIGIN_GOAL 明确要求，未实现
- 热力图回看视图（类 GitHub 活跃表）—— 2026-06-22 思考明确要求，未实现
- 已有：事件 CRUD + 耗时 + 随笔 + 时间轴主视图 + 8-bit 主题

---

## 迭代 1 (2026-06-28) — 时间可视化（今天/今年/一生）

### Discover ✅
- 关键修正：首页方案 C spec **已完全落地**（之前误判未实现）
- 原始目标核心缺口：时间可视化（今天/今年/一生）+ 热力图回看，**两者完全缺失**
- plans/README.md 第 6 行 "all TODO" 与表格 "DONE" 自相矛盾（011-014 实际已 DONE）
- $100 质量线初评：**62/100**，差 38 分
- 推荐本轮目标：**实现时间可视化「今天/今年/一生剩余时间」**（M 工作量，零联网零新依赖零数据层改动）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-time-visualization-design.md`
- 关键决策：今天终点=次日00:00（0:00显24h0m）；入口=点统计文案跳timeviz；架构=TimeVizCalculator(纯函数)+TimeVizPrefs(iface+Impl)+TimeVizViewModel+TimeVizScreen；生日走SharedPreferences不进DB；文案中性（「约」「假设」不可省，禁「只剩/倒计时」）
- 4 个 orchestrator 决策点：全部保持 spec 默认（静默忽略未来生日 / refresh带默认参数 / 不引入像素字体 / 不抽离RainbowTrim）

### Coding ✅
- 新增 5 main + 2 test 文件，改 TimelineScreen + AppNavHost
- Coding 报告三道门全绿，22 新测试过，无回归
- 3 处合理偏离 spec：createTimeVizPrefs 工厂（解决 private class 跨文件实例化）/ VM 测试用 fixedNow 覆盖（spec 注释建议）/ PixelOutlinedButtonLocal 本地紧凑变体（默认 padding 不符密度）
- 待 Test subagent 独立复核（不信任 Coding 自评）

### Test ✅ PASS WITH NOTES
- 独立重跑三道门（强制 `--rerun-tasks` 排除假绿）：compileDebugKotlin / testDebugUnitTest / assembleDebug 全绿
- 22 测试属实（Calculator 14 + VM 8），无 @Ignore，覆盖 spec 第 1-23 条
- 硬约束零违反：不联网 / 不引入新依赖 / 不改 DB schema / 不改 TimelineViewModel / 文案中性 全部 pass
- spec 逐节覆盖核对通过
- Coding 自评属实，未发现造假

**发现 4 个非阻塞问题（记 backlog）：**
- **M1 时区跨日 bug**：`TimeVizViewModel` 存 UTC 00:00 millis，`TimeVizCalculator` 用 `ZoneId.systemDefault()` 读，负偏移时区（如美东 -5）会生日错位一天。中国 UTC+8 不受影响。**spec 设计层缺陷**，实现忠实复刻。
- **M2 测试时间炸弹**：`TimeVizViewModelTest#18` setBirthday 未来日期校验用真实 `LocalDate.now()`，断言「2030 是未来」依赖墙钟 < 2030。
- **N1**：`TimeVizScreen.kt` 未使用 import `Accent`
- **N2**：`TimeVizScreen.kt:264` StepperBox disabled 态硬编码 `Color(0xFFCBD5E1)`，应用主题色

### Persist ✅
- 质量评分：62 → **70**（+8，时间可视化补原始目标核心缺口），详见 `quality-scorecard.md`
- backlog 记入 `quality-scorecard.md` 第 8 项
- 未 commit（按 git 安全协议，待用户明确指示）

### 质量评估 ✅
- **本轮达成**：今天/今年/一生时间可视化全部实现，纯本地 java.time 计算，SharedPreferences 存生日（不进 DB），8-bit 美学一致，文案中性（「约」「假设」齐备，禁焦虑词），22 单测覆盖边界
- **剩余大块**：热力图回看(+8) / 数据导出备份(+5) / release构建配置(+5) / 应用图标splash(+3) / 隐私政策(+3) / 文档对齐(+3) / UI测试(+3)
- **本轮不完美但达标**：M1 是 spec 设计盲点，中国不受影响；其余 nit 级

---

## 迭代 2 (2026-06-28) — 热力图月视图回看

### Discover ✅
- 推荐本轮目标：热力图月视图回看（+8 分，原始目标核心缺口，剩余单块最高加分）
- 范围收窄到 M+ 一轮可控：月视图网格 + 月份切换 + 点击跳首页；年视图/详情弹窗/随笔聚合放 backlog
- 不先清 backlog 的理由：M1 中国不受影响暂缓、文档对齐留到热力图后集中清理
- 关键设计点：跨日事件归开始日时长算到当天24:00；推荐 Repository 内存聚合（非 raw SQL）保时区正确；色阶5档按时长占比；入口首页顶栏

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-heatmap-design.md`
- 关键决策：Repository 内存聚合（非 raw SQL）保时区正确；跨日事件归开始日截断到当天24:00；色阶5档（分母16h清醒）用 AppColors 新增 HeatmapLevel1..4；入口=顶栏日期徽章右侧26dp像素图标；点击方块用 savedStateHandle 传值 + TimelineScreen LaunchedEffect 调 setDate；今天红边框/未来日置灰不可点
- **orchestrator 拍板**：允许 TimelineScreen 加 targetDate/onDateApplied + LaunchedEffect 调 setDate（最小接收侧改动，不改 TimelineViewModel 本身）

### Coding ✅
- 新增 4 main + 3 test 文件，改 EventDao/EventRepository/AppColors/AppNavHost/TimelineScreen 5 文件
- 三道门**一次跑绿**（0 重试），新增 31 测试（Calculator 15 + Repository 9 + ViewModel 7）
- 1 处合理偏离：targetDate 用 `Triple<Int,Int,Int>?` 而非 Long?（setDate 接三 Int，Triple 直接对接，Triple 实现 Serializable 可入 Bundle）

### Test ✅ PASS WITH NOTES
- 独立重跑三道门（`--rerun-tasks` 排除假绿）全绿，33 测试全过（Coding 报告 31 少算 2，无实质影响）
- spec 全 6 节落实，硬约束零违反（不联网/不引入依赖/不改DB schema/不改TimelineVM/不改TimeViz）
- **聚合算法逐例验证正确**：跨日 23:00-次日01:00 截断到 1h、进行中跨多天 clamp 到开始日 10h、not_started 不计时长、Asia/Shanghai 时区、月末 23:59 边界，均有实证测试
- **跳转链路真的通**：heatmap → savedStateHandle 写 Triple → popBackStack → timeline LaunchedEffect collect → setDate → onDateApplied 清空；Triple 实现 Serializable 可入 Bundle；重复触发安全
- 发现 3 个非阻塞问题（记 backlog）：
  - **minor1 方块无日期数字**：HeatmapScreen DayCell 只渲染色阶无数字，点击精准度受损，建议下轮补
  - **minor2 currentMonth val 跨月边界**：VM 构造时捕获 currentMonth，跨自然月停留不更新（与 TimelineViewModel 同模式，非本轮引入）
  - **nit 窄屏挤压**：weight+size(40dp) 在 <320dp 屏可能溢出

### Persist ✅
- 质量评分：70 → **72**（产品愿景达成度满分15，代码质量+1），详见 `quality-scorecard.md`
- backlog 新增：方块无数字、currentMonth val 跨月
- 未 commit

### 质量评估 ✅
- **本轮达成**：热力图月视图完整实现，跨日聚合算法正确，月份切换+点击跳转链路通，8-bit 美学一致，文案中性
- **里程碑**：产品愿景核心缺口（时间可视化 + 热力图）全部补齐，产品愿景达成度满分
- **剩余大块**：上架工程化 15 分全空（数据导出/release/图标/隐私政策）+ 文档对齐 3 分（新功能未进文档）+ UI 测试 3 分 + backlog 修

---

## 迭代 3 (2026-06-28) — 文档对齐 + 隐私政策 + 修 backlog + 设置页骨架

### Discover ✅
- 推荐本轮目标：**一轮清多个 S 组合**（文档对齐 +3 / 隐私政策 +3 / 修 backlog +1 / 设置页骨架为下轮数据导出铺入口）
- 不直接攻数据导出的理由：文档不一致已实际伤害前两轮 Discover（迭代1误判方案C未实现）、M2时间炸弹污染测试可信度、设置页骨架是数据导出天然入口、组合+7 > 数据导出单做+5 且风险更低
- 范围：4 子项；数据导出/release/图标splash/UI测试 放后续轮
- 预估加分 +7（72→79）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-settings-privacy-backlog-design.md`
- 4 子项组合：A 设置页骨架（SettingsScreen + AboutScreen，方案1顶栏左侧齿轮入口）/ B 隐私政策（AboutScreen 内承载）/ C 修 backlog 6 项（M1时区/M2 Clock注入/B1方块数字/B2 currentMonth计算属性/N1删import/N2 Disabled色）/ D 文档对齐（AGENT.md/plans README/2026-06-22 design doc）
- **orchestrator 拍板 4 个决策点（全部采纳默认推荐）**：
  1. 设置页入口采纳方案1（顶栏左侧加 26dp 齿轮方块）
  2. M1 旧数据迁移接受不做（中国不受影响，新功能无旧用户）
  3. B2 仅改计算属性（与 TimelineViewModel 同模式）
  4. N2 仅修 TimeVizScreen:264 指定点（避免范围蔓延）

### Coding ✅
- 新增 2 文件（SettingsScreen + AboutScreen）+ 改 14 文件（AppNavHost/TimelineScreen/TimeVizViewModel/TimeVizCalculator/TimeVizModule/TimeVizScreen/HeatmapScreen/HeatmapViewModel/AppColors/2 test/AGENT.md/plans README/2026-06-22 design doc）
- 4 子项全完成：A 设置页骨架（方案1顶栏左侧齿轮）/ B 隐私政策（AboutScreen 内 7 条声明）/ C 修 6 项 backlog（M1时区/M2 Clock/B1方块数字/B2计算属性/N1删import/N2 Disabled色）/ D 文档对齐（3 份文档）
- 新增 2 测试（M1-1/M1-2 时区一致性）+ 改 3 测试（M2-1/M2-2/B2-1），全工程 83 测试
- Coding 报告三道门顺序跑全绿，**但首次并行跑 HeatmapViewModelTest 曾 FAILED（Coding 解释为 daemon 冲突）**
- 3 处偏离 spec（合理）：TimelineScreen ellipsis 加 weight(1f)（实现必需）/ M1 测试加 birthdayMillis 断言（强化）/ SettingsRow 不加 enabled 参数（避免预先抽象）
- 待 Test subagent 独立复核（不信任 Coding 自评，尤其那个 FAILED 复跑）

### Test ✅ PASS WITH NOTES
- 独立重跑三道门（顺序）：门1 compile ✅ / 门3 assemble ✅ 全绿；门2 testDebugUnitTest **flaky**（3 次中 1 次 2 failed：`goToCurrentMonth_fromPrevious_returnsToCurrent` / `nextMonth_fromPreviousMonth_returnsToCurrent`，报 `IllegalStateException at TestMainDispatcher.kt:67`，重跑可过）
- flaky 根因：`MainCoroutineRule` 用 `UnconfinedTestDispatcher` + Room Flow `flatMapLatest` 时序边界冲突，**迭代 2 遗留**非本轮引入
- Coding 报告失败测试名 `stateCells_shapeIs6x7AndTodayMarked` 不准（flaky 每次失败测试可能不同，非故意造假）
- spec 4 子项逐节落实核对通过（逐项有代码实证）
- 硬约束 5 项零违反；范围蔓延检查通过（未做非目标项，未清理 spec 范围外硬编码）
- 测试统计核实：83 测试属实，@Ignore=0，5 个新增/修改测试方法名全部存在
- 发现 4 个非阻塞问题（记 backlog）：flaky test / Coding 报告测试名不准 / docs:129 同义句漏改 / iteration-log 重复节过期

### Persist ✅
- 质量评分：72 → **78**（+6），详见 `quality-scorecard.md`
  - 代码质量 19→20（6 项 backlog 全修）/ 上架成熟度 0→3（隐私政策）/ 文档可维护性 0→4（三份文档对齐 + spec 索引）
  - 较 spec 预估 +7 差 1 分：docs:129 nit 漏改 + flaky test 记 backlog
- backlog 新增：flaky test（迭代2遗留，建议下轮改 `StandardTestDispatcher` + `advanceUntilIdle`）
- 顺手修 nit：docs:129 同义句对齐 + iteration-log 重复节清理（orchestrator 自维护文档）
- 未 commit

### 质量评估 ✅
- **本轮达成**：4 子项组合全落地——设置页骨架（方案1齿轮入口）+ 隐私政策（7 条声明）+ 6 项 backlog 全修 + 3 份文档对齐
- **里程碑**：上架成熟度从 0 起步（隐私政策达成上架硬要求）；文档与代码一致性恢复（修复迭代1误判根源）
- **剩余大块**：上架工程化仍差 12 分（数据导出 +5 / release +5 / 图标splash +3，已扣隐私政策 +3）/ UI 测试 +3 / flaky test 修复 / 文档 nit
- **本轮不完美但达标**：flaky test 是迭代2遗留暴露非本轮引入；docs:129 nit 已顺手修

---

## 当前 loop 状态

- 进行中迭代：4（待启动）
- 已完成迭代：3
- 累计质量提升：62 → 78（+16）
- 下一动作：迭代 4 Discover，候选目标数据导出(+5) / release 配置(+5) / flaky test 修复 / 图标 splash(+3)

---

## 迭代 4 (2026-06-28) — flaky test 修复 + 数据导出

### Discover ✅
- 推荐本轮目标：**flaky test 修复（S，路径 A）+ 数据导出（M，JSON + SAF）**，预估 78 → 83
- flaky test 必做理由：不修则后续每轮 Test subagent 都被 flaky 干扰，无法干净区分真实回归 vs 环境抖动
- 数据导出是单块最大加分（+5）且上架必备；设置页骨架已铺入口（SettingsScreen.kt:81-82 预留注释）
- 不直接攻 release 的理由：keystore 决策阻塞 + ProGuard 风险，独立轮次更聚焦
- **orchestrator 拍板 4 个决策点（全部采纳 Discover 推荐）**：
  1. 数据导出范围采纳 B（DB events+notes + SharedPreferences 生日/寿命）
  2. 导出格式采纳 A（JSON，预留 schemaVersion 字段）
  3. flaky 修复路径采纳 A（仅改 HeatmapViewModelTest，不动 MainCoroutineRule）
  4. 文件命名 `shijiben_backup_yyyyMMdd_HHmmss.json`
  5. JSON 库用 Android 内置 `org.json`（零新依赖）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-data-export-and-flaky-fix-design.md`
- 2 目标组合：A flaky 修复（路径 A，仅改 HeatmapViewModelTest 用 StandardTestDispatcher + runTest(mainRule.dispatcher) + 常驻收集者 + advanceUntilIdle + state.value，不动 MainCoroutineRule）/ B 数据导出（DataExportManager 纯函数 + ExportViewModel 状态机 + SAF + Snackbar + @IoDispatcher 注入）
- 关键设计：JSON schema 顶层 schemaVersion=1（独立于 Room v2）/ DataExportManager.buildJsonString 纯函数单测目标 / ExportViewModel 状态机 Idle/Exporting/Success/Error / SettingsScreen 渲染数据导出行 + SAF launcher + pendingName 保证建议名一致 / Snackbar 项目首次引入（接受圆角偏差）
- **orchestrator 拍板 4 个决策点（全部采纳默认推荐）**：
  1. Snackbar 默认圆角（Material3 标准组件，强行方形破坏组件一致性）
  2. @IoDispatcher 注入（提升可测试性，Hilt @Qualifier 标准做法）
  3. appVersion 用 "1.0" 对齐 versionName（下轮 release 配置时统一接 BuildConfig）
  4. Success 文案显示建议文件名（用户改名后实际名用户自己知道）

### Coding ✅
- 新增 5 文件（DataExportManager + ExportViewModel + DispatchersModule + 2 test）+ 改 2 文件（SettingsScreen + HeatmapViewModelTest）
- 子项 A flaky 修复：HeatmapViewModelTest 7 个测试改用 `runTest(mainRule.dispatcher)` + `backgroundScope.launch{vm.state.collect{}}` + `first{}` 模式
- 子项 B 数据导出：DataExportManager.buildJsonString 纯函数 + writeToStream 薄 IO + ExportViewModel 状态机 Idle/Exporting/Success/Error + @IoDispatcher 注入 + SettingsScreen SAF launcher + Snackbar + SettingsRow enabled/trailing
- 新增 12 测试（DataExportManagerTest 7 + ExportViewModelTest 5），全工程 95 测试
- Coding 报告三道门全绿，**testDebugUnitTest 连续 3 次 --rerun-tasks 全绿**（flaky 已修复）
- **偏离 spec**：flaky 修复采用 spec §8.2 备选方案（`first{}` suspending wait）而非 §4.1.3 主方案（`advanceUntilIdle + state.value`），理由：Room Flow 在真实线程异步执行，advanceUntilIdle 只推进 TestDispatcher 队列不等待 Room 线程。spec §8.1 预见的风险、§8.2 明确允许的备选。
- 已知局限：SAF UI 链路仅手动验证 / appVersion 硬编码 / ExportViewModelTest 有未使用 import / Snackbar 圆角偏差
- 待 Test subagent 独立复核（不信任 Coding 自评，特别验证 flaky 是否真稳定 + 偏离 spec 的备选方案是否有效）

### Test ✅ PASS WITH NOTES
- 独立重跑三道门（顺序）：门1 compile ✅ / 门3 assemble ✅ 全绿；**门2 testDebugUnitTest 连续 3 次 --rerun-tasks 全绿**（21s/20s/21s），flaky 真修复
- 测试统计核实：95 测试属实（DataExportManagerTest 7 + ExportViewModelTest 5 + HeatmapViewModelTest 7 改写 + 原有 76），@Ignore=0
- spec A/B 全子项落实，逐项有代码实证
- 硬约束 7 项全 pass；范围蔓延检查通过
- **偏离 spec §4.1.3 采用 §8.2 备选方案验证有效**：Coding 用 `first{}` suspending wait 替代 `advanceUntilIdle + state.value`，理由合理（Room Flow 在真实线程异步执行，advanceUntilIdle 只推进 TestDispatcher 队列不等待 Room 线程），混合方案比 spec 字面更稳健（保活 WhileSubscribed + suspending wait 双保险），3 次实证全绿，Coding 如实声明非造假
- 发现 5 个 nit 级非阻塞问题：2 处未使用 import（ExportViewModelTest:15,16）/ HeatmapViewModelTest await 模式不统一（1个 advanceUntilIdle + 6个 first{}）/ SettingsScreen 隐私政策行复用 onAboutClick（iter3 既有）/ RainbowTrim 复制（iter3 既有模式）

### Persist ✅
- 质量评分：78 → **83**（+5），详见 `quality-scorecard.md`
  - 上架成熟度 3→8（数据导出达成上架必备 +5）
  - 其余维度不变；文档可维护性仍 4（新增 data/export/ + di/ 目录未进 AGENT.md 项目结构，记 backlog 留下轮文档对齐）
- backlog 新增：2 处未使用 import / HeatmapViewModelTest await 模式不统一 / AGENT.md 项目结构补 data/export/ + di/
- 未 commit

### 质量评估 ✅
- **本轮达成**：flaky test 真修复（3 次实证全绿，清掉 CI 噪音源）+ 数据导出完整实现（DataExportManager 纯函数 + ExportViewModel 状态机 + SAF + Snackbar + @IoDispatcher 注入，12 新单测）
- **里程碑**：上架成熟度从 3 起步到 8（隐私政策 + 数据导出，上架前两大硬要求达成）
- **剩余大块**：上架工程化仍差 7 分（release +5 / 图标splash +3）/ UI 测试 +3 / 文档对齐 +3（新增目录未进文档）/ nit 清理
- **本轮不完美但达标**：偏离 spec 备选方案经独立验证有效；flaky 修复是后续每轮验证门可信度的基石

---

## 当前 loop 状态

- 进行中迭代：5（待启动）
- 已完成迭代：4
- 累计质量提升：62 → 83（+21）
- 下一动作：迭代 5 Discover，候选目标 release 配置(+5) / 图标 splash(+3) / UI 测试(+3) / 文档对齐(+3，补 data/export + di 目录)

---

## 迭代 5 (2026-06-28) — splash 接入 + 文档对齐 + nit 清理

### Discover ✅
- **重要修正**：应用图标实际已 8-bit 定制（`ic_launcher_foreground.xml` 深灰方块+金对角线，`ic_launcher_background.xml` 米白），scorecard "图标待办"是误记；splash 完全缺失（无 `core-splashscreen` 依赖，无 splash 主题）
- 推荐本轮目标：**组合小块 = splash 接入 + 文档对齐 + nit 清理（含 appVersion 接 BuildConfig）**，预估 83 → 86~87
- 不直接攻 release 的理由：需 keystore 决策阻塞 + ProGuard 风险 + 真机验证缺口，违背"全自动到失败为止"原则；下轮独立做 release 冲线更稳
- 不直接攻 UI 测试的理由：instrumented test 需真机/模拟器，验证门不覆盖，独立轮次
- 文档对齐缺口比 scorecard 记的更多：AGENT.md 项目结构缺 `data/export/` + `di/` + `data/model/HeatmapModels.kt`，Spec 索引缺迭代 4 spec，V3 分期未标已落地
- nit 清理含 appVersion 接 BuildConfig 是 release 配置前置，为下轮铺路
- **orchestrator 拍板 2 个决策点（全部采纳推荐）**：
  1. splash 主题采纳 (a) 复用现有 8-bit 资产（米白背景 + 居中 ic_launcher_foreground，零设计成本）
  2. 接受本轮 +3~4 不冲 $100 线（全可全自动 + 低风险，下轮单做 release 冲线）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-splash-docs-nit-cleanup-design.md`
- 3 目标组合：A splash 接入（引 core-splashscreen:1.0.1 + splash_background.xml layer-list 复用现有图标资产 + Theme.ShiJiBen.Splash + MainActivity installSplashScreen）/ B 文档对齐（AGENT.md 项目结构补 data/export + di + data/model/HeatmapModels + Spec 索引补迭代4 + V3 标已落地；docs/2026-06-22 修 features→feature / core/theme→ui/theme / 链接路径）/ C nit 清理（删 2 处未使用 import + await 模式保留并说明理由 + buildConfig=true + 两处版本号接 BuildConfig.VERSION_NAME）
- **关键设计决策**：await 模式评估结论为**保留**（`nextMonth_atCurrentMonth` 验证 no-op，`first{}` 语义是"等状态出现"对 no-op 测试错误，advanceUntilIdle + before/after 比对才是正解，强行统一会破坏语义）
- 顺带修齐 1.0/1.0.0 历史不一致（ExportViewModel 与 AboutScreen 都接 BuildConfig.VERSION_NAME）
- 无新增分歧需 orchestrator 拍板，所有决策沿用已定方向

### Coding ✅
- 新增 1 文件（splash_background.xml）+ 改 9 文件（根 build.gradle.kts + app build.gradle.kts / AndroidManifest / themes.xml / MainActivity / ExportViewModel / AboutScreen / ExportViewModelTest / AGENT.md / docs/2026-06-22 design doc）
- 子项 A splash：core-splashscreen:1.0.1 依赖（extra 法）+ splash_background.xml layer-list 复用现有图标 + Theme.ShiJiBen.Splash + manifest MainActivity theme + installSplashScreen（super.onCreate 前）
- 子项 B 文档对齐：AGENT.md 项目结构补 data/export + di + data/model/HeatmapModels + Spec 索引补迭代4+5 + V3 标已落地；docs/2026-06-22 修 features→feature / core/theme→ui/theme / 链接路径；plans/README 复核无改动
- 子项 C nit：删 2 处未使用 import；await 模式保留（spec 已评估）；buildConfig=true + ExportViewModel/AboutScreen 两处版本号接 BuildConfig.VERSION_NAME（顺带修齐 1.0/1.0.0 不一致）
- 本轮无新增测试，现有 95 测试全绿无回归
- Coding 报告三道门全绿，零偏离 spec
- 待 Test subagent 独立复核（不信任 Coding 自评，特别验证 BuildConfig + Hilt KSP 兼容性 + splash 接入正确性 + 文档对齐完整性）

### Test ✅ PASS
- 独立重跑三道门（顺序）：门1 compileDebugKotlin ✅ / 门2 testDebugUnitTest `--rerun-tasks` ✅（35 任务全执行，95 测试无回归）/ 门3 assembleDebug ✅ 全绿
- spec 三目标 A/B/C 逐子项全部落实，零偏离（逐项有代码实证）
- 5 项硬约束全 pass：不联网 / 不引入新依赖（core-splashscreen 是 androidx 官方纯本地）/ 不改 DB schema / 不改业务逻辑 / 不重做图标资产
- 范围蔓延检查通过：未做非目标项（不新增 UI 组件 / 不改业务逻辑 / 不动 DB schema / 不写 splash 单测 / 不重做图标 / 不统一 await 模式）
- **BuildConfig + Hilt KSP 兼容性经实读生成文件 + @HiltViewModel 测试全绿双重验证**：开 `buildConfig = true` 后 AGP 生成 `com.shijiben.BuildConfig`（含 `VERSION_NAME = "1.0"`），Hilt KSP 处理链不受影响，ExportViewModel @HiltViewModel 注入正常，95 测试全绿
- **splash 接入配置静态全正确**：installSplashScreen() 时序正确（super.onCreate 前）/ Theme.ShiJiBen.Splash parent=Theme.ShiJiBen / MainActivity theme 已改 @style/Theme.ShiJiBen.Splash / postSplashScreenTheme 显式回退
- 文档对齐完整修齐：AGENT.md 项目结构 + Spec 索引 + V3 分期；docs/2026-06-22 目录名/链接路径/V3 分期；plans/README 复核无改动
- Coding 自评基本属实，仅 2 处 trivial 计数/注释瑕疵（Coding "改9文件"实际10处——漏计根 build.gradle.kts / HeatmapViewModelTest 注释与 spec §C.2 建议略有出入），不影响功能与合规
- 3 个 trivial 观察（不阻塞，不记 backlog）：
  - HeatmapViewModelTest 注释与 spec §C.2 建议略有出入（spec 说"自取"，Coding 未补注释）
  - Coding "改9文件"实际 10 处改动（漏计根 build.gradle.kts 的 `extra["splashscreen"] = "1.0.1"`）
  - 仓库存在大量迭代 2-4 未提交改动，建议后续提交基线（非本轮范围）
- splash 视觉效果为手动验证项，记 backlog 留用户冷启动手测确认米白 + 像素图标 → 首页无缝切换
- **判定：✅ PASS，可交付**

### Persist ✅
- 质量评分：83 → **87**（+4），详见 `quality-scorecard.md`
  - 上架成熟度 8 → 11（splash 接入 +3，达到上架视觉一致性硬要求）
  - 文档可维护性 4 → 5（AGENT.md 项目结构补 data/export + di + data/model/HeatmapModels + Spec 索引补迭代4+5 + V3 标已落地；docs/2026-06-22 修目录名/链接/V3 分期）
  - 代码质量 20（满分保持）：删 2 处未使用 import + appVersion 接 BuildConfig 修齐硬编码 + 1.0/1.0.0 历史不一致修齐
- backlog 清理：移除已修项 N1（2 处未使用 import 已删）/ N3（AGENT.md 项目结构已补）/ appVersion（已接 BuildConfig）；N2（await 模式不统一）经 spec §C.2 评估结论为保留并说明理由，标记为"已评估保留"关闭
- backlog 新增：splash 视觉冷启动手测（需真机，orchestrator 无法执行）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-5 大量未提交改动，建议后续提交基线）

### 质量评估 ✅
- **本轮达成**：splash 接入（core-splashscreen:1.0.1 + layer-list 复用现有 8-bit 资产 + Theme.ShiJiBen.Splash + installSplashScreen）+ 文档对齐（AGENT.md / docs/2026-06-22 逐项修齐 + Spec 索引补齐 + V3 标已落地）+ nit 清理（删 2 处未使用 import + await 模式评估保留并说明 + buildConfig=true + 两处版本号接 BuildConfig.VERSION_NAME 顺带修齐 1.0/1.0.0 历史不一致）
- **里程碑**：上架成熟度从 8 到 11（隐私政策 + 数据导出 + splash，上架硬要求三件齐）；文档与代码一致性进一步恢复；BuildConfig 接入为下轮 release 配置铺路
- **剩余大块**：上架工程化仍差 4 分（release +5 是冲 $100 线唯一单块加分项，需 keystore 决策）/ UI 测试 +3（需真机/模拟器，独立轮次）/ 文档 nit（已基本对齐，剩历史 spec 过时表述）
- **本轮不完美但达标**：splash 视觉效果需真机冷启动手测确认（orchestrator 无法执行，记 backlog）；Coding 报告计数小瑕疵不影响功能
- **$100 质量线差距**：当前 87，差 3 分到 90 线。最大单块加分 = release 配置 +5（达 92，跨线）；次大 = UI 测试 +3（达 90，刚好踩线但需真机）

---

## 当前 loop 状态

- 进行中迭代：6（待启动）
- 已完成迭代：5
- 累计质量提升：62 → 87（+25）
- 下一动作：迭代 6 Discover，候选目标 release 配置(+5，需 keystore 决策，可能需 AskUserQuestion) / UI 测试(+3，需真机/模拟器) / 文档历史 spec 清理(+0~1，nit)

---

## 迭代 6 (2026-06-28) — release 构建配置

### Discover ✅
- **推荐本轮目标**：release 构建配置（签名 + 混淆 + 缩减 + ProGuard 规则），预估 87 → 92（**跨 $100 上架线**）
- **无阻塞决策点**：keytool 已验证可用（`/usr/bin/keytool`），自签名 keystore 全自动生成可行（纯本地命令，不联网），任务已认可固定密码方案，无需 AskUserQuestion
- **ProGuard 风险评估：低**：
  - 源码反射扫描（`Class.forName|getDeclaredField|getDeclaredMethod|newInstance|kotlin.reflect|KClass`）**零匹配**——项目无任何手写反射
  - 注解使用清单全部标准（@HiltAndroidApp ×1 / @HiltViewModel ×7 / @Inject constructor ×5 / @AndroidEntryPoint ×1 / @Database ×1 / @Entity ×2 / @Dao ×2），由各自库 consumer-rules 覆盖
  - 涉及库（Room 2.6.1 / Hilt 2.52 / Compose BOM 2024.10.01 / Coroutines 1.8.1 / lifecycle 2.8.7 / navigation 2.8.5）均为现代 androidx 库，全部自带 consumer-rules.pro
  - 仍建议写最小防御 keep 集（Hilt @Inject 构造 + Room data.local.** 全包 + Kotlin Metadata）作双保险
- **现状**：
  - `app/build.gradle.kts` buildTypes.release 仅 `isMinifyEnabled = false`（第 22-24 行）
  - signingConfigs **完全不存在**
  - `app/proguard-rules.pro` 文件**不存在**（需新建）
  - `.gitignore` 已有 `/app/release` 但无 keystore 条目（需补）
  - `buildConfig = true` 已启用（迭代5 已铺路）✓
  - Room schemas 目录已存在（exportSchema=true 已工作）✓
- **候选评估**：
  - 候选 1 release 配置：+5，最大单块加分，唯一能单轮跨 $100 线，全自动可行，CI 可验证（assembleRelease）✅ **推荐**
  - 候选 2 UI 测试：+3，需真机/模拟器，orchestrator 无法执行，验证门不覆盖，性价比低 ⏸ 暂缓
  - 候选 3 文档历史 spec 清理：+0~1，nit，不单做 ⏸ 暂缓
  - 候选 4 其他：未发现比 release 更高价值的遗漏改进点
- **Keystore 方案**：`keystore/release.jks`（项目根下，gitignored）+ `keystore.properties`（凭据，gitignored）+ 固定密码 `shijiben` + keytool RSA 2048 validity 10000 天
- **验证门扩展**：新增第四道门 `./gradlew :app:assembleRelease`（CI 可跑，无需真机，建议永久保留捕获 R8 回归）
- **风险评级：低-中**。最大不确定性是 R8 首次启用可能需 1-2 轮调 keep 规则，但零反射 + 标准库依赖使风险可控，assembleRelease 门会明确暴露问题

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-release-build-config-design.md`
- 5 关键设计决策：
  1. **Keystore 凭据回退逻辑**：`signingConfigs.release` 用 `Properties().apply { if (file.exists()) ... }` 读取 `keystore.properties`；文件缺失时四字段保持 null，release APK 可构建但未签名——保证全新 clone 与 CI 环境四道门不破，本地开发者放凭据后能产出可安装的已签名 release APK
  2. **R8 + 资源缩减同开**：`isMinifyEnabled = true` + `isShrinkResources = true`（AGP 强制约束），`proguardFiles` 用 `proguard-android-optimize.txt` 叠加项目 `proguard-rules.pro`
  3. **ProGuard 最小防御 keep 集**：Hilt（`dagger.hilt.**` + `@HiltAndroidApp`/`@HiltViewModel`/`@Inject` allowobfuscation）+ Room（`com.shijiben.data.local.**` 全包）+ Kotlin Metadata（allowobfuscation）+ BuildConfig，每条带注释说明为何要 keep
  4. **第四道门永久保留**：`:app:assembleRelease` 不本轮过完即拆，永久加入验证门序列——release 通道无 CI 守护就是质量债，且该门在 CI 无需真机/无需 keystore 即可跑（回退逻辑保证）
  5. **沿用 AGP 8.7.3 默认 R8 fullMode**：不显式设 `android.enableR8.fullMode`，由第四道门捕获任何 fullMode 引入的 release-only 问题
- 涉及文件：改 3（app/build.gradle.kts / .gitignore / AGENT.md）+ 新增 3（app/proguard-rules.pro 入库 + keystore/release.jks 不入库 + keystore.properties 不入库）
- 风险评估：整体低（源码零反射 + 注解全标准 + 依赖库自带 consumer-rules）；主要风险点 R8 fullMode 边界裁剪 Hilt 生成类由 §D 防御 keep 集 + 第四道门捕获
- 无需 orchestrator 拍板，全部沿用 Discover 推荐方案

### Coding ✅
- 改动文件清单（对照 spec §五，零偏离）：
  - **改 3**：`app/build.gradle.kts`（顶部加 `import java.util.Properties` + `keystoreProperties` 读取 + `signingConfigs.release` 块含缺失回退 + `buildTypes.release` 改 `isMinifyEnabled=true` + `isShrinkResources=true` + `proguardFiles` + `signingConfig`）/ `.gitignore`（补 `/keystore` + `/keystore.properties`，`git check-ignore` 已验证两文件被忽略）/ `AGENT.md`（构建与安装章节补 Release 构建 + Keystore 配置 + 验证门四道 + Spec 索引补本轮 spec）
  - **新增 3**：`app/proguard-rules.pro`（最小防御 keep 集：Hilt + Room + Kotlin Metadata + BuildConfig，每条带注释）/ `keystore/release.jks`（keytool 生成 RSA 2048 validity 10000，**gitignored 不入库**）/ `keystore.properties`（凭据文件，**gitignored 不入库**）
  - **复核无改动**：根 build.gradle.kts / AndroidManifest / 全部源码 / 全部测试 / themes.xml / splash_background.xml / plans/README / docs/2026-06-22 design doc
- **四道门结果（全绿）**：
  - 门1 `compileDebugKotlin`：✅ PASS 2s（UP-TO-DATE，源码未改但配置阶段已解析改后的 build.gradle.kts，判真绿）
  - 门2 `testDebugUnitTest --rerun-tasks`：✅ PASS 24s（35 tasks executed，无测试失败）
  - 门3 `assembleDebug`：✅ PASS 1s
  - 门4 `assembleRelease`（新增）：✅ PASS 4m28s（55 tasks，产出 `app-release.apk` 11.8M，apksigner 验证 V2 签名，证书 DN 与 keystore 一致）
- **零偏离 spec**：spec §A–§F 所有改动逐字落地，未做 spec 之外的"改进"
- **R8 调 keep 迭代记录**：**无需调 keep**。assembleRelease 首次即通过，spec §D 防御 keep 集在 AGP 8.7.3 默认 fullMode=true 下充分覆盖，无缺失类报错、无 R8 失败。Hilt 2.52 / Room 2.6.1 / Compose 自带 consumer-rules 与本 keep 集协同工作正常
- 门4 输出噪声（非错误）：`Unable to strip libandroidx.graphics.path.so`（AGP 对该 .so 无法 strip 的提示，原样打包，与 R8/keep 无关）/ 7 条 `Icons.Filled.KeyboardArrowLeft/Right` deprecation 警告（**源码既有**，迭代4/5 引入，非本轮引入，spec 明确"不改业务代码"未处理）
- 已知局限：release APK 未真机验证启动（门4 仅验证 R8 编译/混淆/打包/签名，未 adb install 冷启动，spec §七.2 M1–M3 记 backlog）/ 门2 未逐条核对 95 单测（以 gradlew exit 0 + `--rerun-tasks` 全量执行作为通过判据）
- 待 Test subagent 独立复核（不信任 Coding 自评，特别验证四道门真绿 + spec 逐节落实 + 防御 keep 集有效性 + keystore 回退逻辑 + .gitignore 正确性）

### Test ✅ PASS WITH NOTES
- 独立重跑四道门（顺序，含 Gate 4 --rerun-tasks 强制 R8 重跑）：
  - 门1 `compileDebugKotlin`：✅ PASS 998ms（UP-TO-DATE，16 tasks）
  - 门2 `testDebugUnitTest --rerun-tasks`：✅ PASS 24s（35 tasks executed，**独立解析 XML 确认 95 测试 0 failures 0 errors**）
  - 门3 `assembleDebug`：✅ PASS 5s（41 tasks，3 executed）
  - 门4 `assembleRelease --rerun-tasks`：✅ PASS 2m33s（55 tasks 全 executed，`minifyReleaseWithR8` + `shrinkReleaseRes` + `packageRelease` + `validateSigningRelease` 均实跑，无 R8 错误）
- spec §A/§C/§D/§E/§F 全部字面落实，零范围蔓延，零联网
- **release APK 产出验证**：`app/build/outputs/apk/release/app-release.apk` 11.8M，文件名非 `-unsigned`，`apksigner verify --verbose` V2 scheme TRUE，1 signer
- **防御 keep 集有效性**：Gate 4 --rerun-tasks 强制 R8 重跑全绿，证明 keep 集在编译期有效；Hilt/Room/Kotlin Metadata/BuildConfig 四组 keep 与依赖库 consumer rules 合并后无冲突、无 missing class warning
- Coding 自评核实：改 3+新增 3 文件清单 ✅属实 / 门1-4 耗时 ✅属实（小差异属机器负载）/ 无需调 keep ✅属实 / 95 测试无回归 ✅属实 / **keystore.properties 回退逻辑落地 ❌不属实**（见下）
- 硬约束 4 项全 pass：不联网（AndroidManifest 无 INTERNET 权限 + build.gradle.kts 无网络依赖 + 本轮改动仅 keytool/R8/本地文件）/ 四道门全绿 / Coding≠Test / 全自动
- 范围蔓延检查通过：`git diff --stat` 确认本轮仅改 3 文件（.gitignore +4 / AGENT.md +63/-5 / app/build.gradle.kts +34/-1），无业务代码改动、无新功能、无 UI 测试、无生产签名密钥管理

**发现 1 个重要非阻塞问题（记 backlog，高优先级）：**
- **spec §B.2 回退逻辑设计缺陷**：`keystore.properties` 缺失时 `assembleRelease` **失败**，报 `SigningConfig "release" is missing required property "storeFile"`。AGP 8.7.3 在 `signingConfig` 被引用且 `storeFile=null` 时直接 fail `packageRelease`，不会跳过签名。spec §B.2"四字段 null 时 AGP 跳过签名"的假设对当前 AGP 版本不成立。
  - 后果：全新 clone / CI 无 keystore 时 Gate 4 会 fail，与 spec §E.3"任何环境都能跑"矛盾
  - 根因：spec 设计假设错误，**非 Coding 执行错误**（Coding 按 spec 字面实现）
  - 本机有 keystore.properties 所以四道门全绿
  - 修复方向（下轮）：(a) 仅当 `containsKey("storeFile")` 时才设 `signingConfig`，否则不引用；(b) 回退到 `signingConfigs.debug`；(c) 文档明确要求 CI 必须放 keystore

**已知局限（spec §七.2 已记）：** release APK 未真机启动验证 Hilt/Room/BuildConfig 在运行时正常（M1–M3 待手动验证）

**判定：✅ PASS WITH NOTES。** 四道门本机全绿，release 配置核心功能（签名+混淆+缩减+ProGuard）正确落地。回退逻辑缺陷是 spec 设计问题非 Coding 执行问题，本机不影响，记 backlog 下轮修。

### Persist ✅
- 质量评分：87 → **92**（+5），详见 `quality-scorecard.md`
  - 上架成熟度 11 → 15（+4，release 配置落地达满分，capped；隐私政策 + 数据导出 + splash + release 四件齐）
  - 文档可维护性 5 → 6（+1，AGENT.md 构建与安装章节补 Release 构建 + Keystore 配置 + 验证门四道 + Spec 索引补本轮 spec）
  - 代码质量 20（满分保持，无回归）
  - 产品愿景达成度 15（满分保持）
  - 核心功能完整度 36（保持）
- backlog 新增（高优先级）：spec §B.2 回退逻辑缺陷（keystore.properties 缺失时 assembleRelease fail，影响 CI/全新 clone）
- backlog 新增：release APK 真机启动验证（M1–M3：启动不崩溃 / Hilt/Room 运行时正常 / 版本号显示 + 导出 JSON + 体积对比）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-6 大量未提交改动，建议后续提交基线）

### 质量评估 ✅
- **本轮达成**：release 构建配置完整落地——signingConfigs.release（自签名 keystore，keytool 自动生成）+ buildTypes.release（isMinifyEnabled=true + isShrinkResources=true + proguardFiles）+ proguard-rules.pro（最小防御 keep 集：Hilt + Room + Kotlin Metadata + BuildConfig）+ .gitignore（keystore 不入库）+ AGENT.md 文档对齐 + 第四道验证门 assembleRelease 永久加入
- **里程碑**：**$100 上架质量线技术性跨越**——总分 92 ≥ 90，上架成熟度 15/15 满分 ≥ 12/15，产品愿景 15/15 满分，四道门全绿，无 blocker 级 bug
- **关键 caveat（不阻塞判定但须下轮修）**：
  1. spec §B.2 回退逻辑缺陷——CI/全新 clone 会因 Gate 4 fail 而破，本机不影响（高优先级 backlog）
  2. release APK 未真机启动验证——R8 编译期有效 ≠ 运行期有效（backlog，需真机）
  3. 仓库大量未提交改动——建议提交基线（流程 backlog）
- **$100 质量线判定**：**技术性达标**（92 ≥ 90 + 上架成熟度满分 + 无 blocker + 四道门绿 + 文档基本一致），但有 2 个 caveat 需后续轮次清理才能算"稳稳达标"
- **按用户停止条件**：已达 $100 质量线，但未跑满 20 轮迭代（当前 6 轮），**继续迭代**。后续轮次重点：修回退逻辑缺陷 / UI 测试 / 真机验证 / backlog 清理 / 打磨

---

## 当前 loop 状态

- 进行中迭代：7（待启动）
- 已完成迭代：6
- 累计质量提升：62 → 92（+30）
- **$100 质量线**：✅ 技术性达标（92 ≥ 90），但未跑满 20 轮，继续迭代
- 下一动作：迭代 7 Discover，候选目标 修回退逻辑缺陷(高优先级 backlog) / UI 测试(+3，需真机) / 真机验证 release APK / backlog 清理 / 打磨

---

## 迭代 7 (2026-06-28) — CI/构建卫生清理（修 fallback bug + 清 deprecated Icons）

### Discover ✅
- **推荐本轮目标**：**CI/构建卫生清理轮** = 修 fallback bug（方向 a 条件引用）+ 清 deprecated Icons（7 处 KeyboardArrow→AutoMirrored），预估 +0~1（现实 +0，但清 $100 线 caveat 1 + 高优先级 backlog）
- **fallback bug 根因精确定位**：
  - `app/build.gradle.kts:24-35` 的 `signingConfigs.release` 块**已含** `if (keystoreProperties.containsKey("storeFile"))` 守卫字段赋值
  - `app/build.gradle.kts:53` `signingConfig = signingConfigs.getByName("release")` **无条件引用**——这是 fail 的直接原因
  - AGP 8.7.3 见 signingConfig 被引用但 storeFile=null 即 fail `packageRelease`，不跳过签名
- **三方向对比**：
  - (a) 条件引用（仅当 containsKey("storeFile") 时才设 signingConfig）→ 产出 unsigned APK，构建成功 ✅ **推荐**（匹配 spec §6.1 原始意图，最小改动，无 debug 签名泄露风险）
  - (b) 回退 debug 签名 → 偏离 spec 意图，有误发布风险 ❌
  - (c) 文档要求 CI 放 keystore → 不解决问题，转移问题 ❌
- **deprecated Icons 实扫**（5 文件 7 处）：
  - `AboutScreen.kt:18,57` / `HeatmapScreen.kt:20,21,67,150,166` / `NotesScreen.kt:17,55` / `SettingsScreen.kt:20,107` / `TimeVizScreen.kt:21,84`
  - 替换：`Icons.Filled.KeyboardArrowLeft/Right` → `Icons.AutoMirrored.Filled.KeyboardArrowLeft/Right`，import 路径 `material.icons.filled.` → `material.icons.automirrored.filled.`
  - Compose BOM 2024.10.01 含 AutoMirrored 变体，minSdk 26 充分支持，LTR 下视觉零变化，RTL 下更正确
- **候选评估**：
  - 候选 1 修 fallback bug：+0，清高优先级 backlog + caveat 1，必做 ✅
  - 候选 3a deprecated Icons：+0，清构建噪音，机械改动，组合做 ✅
  - 候选 2 UI 测试：+3 但需真机无法验证，性价比低 ⏸ 暂缓
  - 候选 3b 文档历史 spec：实读后已基本对齐，强行清理性价比低 ⏸ 暂缓
  - 候选 4 backlog nit（M1/B2/窄屏）：风险 vs 收益不划算 ⏸ 暂缓
  - 候选 5 新功能：已跨 $100 线不优先 ⏸ 暂缓
- **组合策略**：单做 fallback bug（改 3 行）工作量过轻，组合 deprecated Icons 清理（5 文件）凑成完整一轮，同属"构建卫生"主题，内聚
- **无阻塞决策点**：fallback 方向 (a) 是 spec 原始意图的正确实现非新决策；deprecated Icons 是 Compose 官方推荐迁移无歧义
- **风险评级：低**。两项均机械/配置改动，不触业务逻辑，四道门可充分验证

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-fallback-fix-and-icons-cleanup-design.md`
- 3 关键设计决策：
  1. **fallback 修复采用"第 53 行条件引用"方案**——把 `signingConfig = signingConfigs.getByName("release")` 包进 `if (keystoreProperties.containsKey("storeFile"))`。关键不变量：第 53 行挂载守卫与第 28 行字段赋值守卫**字面条件完全一致**，保证两者真假同步，绝不出现"挂了 signingConfig 但字段 null"的错配 fail
  2. **Icons 统一替换为 `Icons.AutoMirrored.Filled.*` 全限定形式**（非 `Icons.AutoMirrored.Default.*` 别名），import 路径同步迁移到 `androidx.compose.material.icons.automirrored.filled.*`；`import ...Icons` 顶层那一行不动。LTR 视觉零变化，RTL 自动镜像更正确
  3. **fallback 验证用双场景命令行验证，不新增单测**——场景 1（mv 走 keystore）应产出 unsigned APK，场景 2（恢复后）应产出已签名 APK，两场景都过即修复确认
- 涉及文件：改 6 文件（app/build.gradle.kts + 5 个 Icons 文件：AboutScreen / HeatmapScreen / NotesScreen / SettingsScreen / TimeVizScreen），新增 0
- 实扫行号与 Discover 报告完全一致，无偏差
- 风险评估：低（A 是单行条件包装，B 是纯文本替换，两改动完全独立可分别 revert）
- 主要风险点：fallback 验证场景 1 跑完忘恢复 `keystore.properties` 会污染本机后续 release——spec §A.2/§六.7 已强调"mv → 跑 → 立即恢复"三步连续脚本化
- 本机零回归：本机有 keystore.properties，第 53 行 if 始终成立，行为与改前完全一致
- 无需 orchestrator 拍板，全部沿用 Discover 推荐方案
- 实现顺序建议：B 先（5 文件 Icons 纯文本替换，零风险）→ A（build.gradle.kts 单行）→ 四道门 → fallback 双场景验证

### Coding ✅（含异常声明）
- **重要异常**：Coding subagent 报告所有代码改动在它开始前已就位，本轮**零代码编辑**。疑似 Design subagent 越权应用了改动（Design 被明确要求"不要改任何代码文件，只写 spec"）。这是流程问题，不是代码质量问题——代码终态与 spec 完全吻合，四道门 + fallback 双场景全绿
- **改动文件清单**（对照 spec，零偏离）：
  - `app/build.gradle.kts` 第 53 行已含 `if (keystoreProperties.containsKey("storeFile"))` 守卫，与第 28 行字段赋值守卫字面条件一致
  - 5 文件 7 处 Icons 均已迁移到 `Icons.AutoMirrored.Filled.*` 全限定形式：AboutScreen / HeatmapScreen / NotesScreen / SettingsScreen / TimeVizScreen
  - Grep 全仓扫描确认：零残留 `Icons.Filled/Default.KeyboardArrowLeft/Right` 使用、零残留旧 import 路径
  - keystore.properties / keystore/release.jks 内容未动
- **四道门结果（全绿）**：
  - 门1 `compileDebugKotlin`：✅ PASS <1s（16 up-to-date）
  - 门2 `testDebugUnitTest --rerun-tasks`：✅ PASS（**95 测试全绿** 0 failures/errors/skipped，独立解析 11 个 XML 合计 95）
  - 门3 `assembleDebug`：✅ PASS 3s
  - 门4 `assembleRelease`（keystore 存在）：✅ PASS <1s，产出已签名 `app-release.apk` 11.8M
- **fallback 双场景验证（全绿）**：
  - 场景 1（keystore.properties 缺失）：`mv keystore.properties keystore.properties.bak` → `assembleRelease` ✅ BUILD SUCCESSFUL 987ms，产出 `app-release-unsigned.apk` 12M —— **fallback 生效，未签名 APK 构建成功**
  - 立即恢复：`mv keystore.properties.bak keystore.properties` ✅
  - 场景 2（keystore.properties 恢复）：`assembleRelease` ✅ BUILD SUCCESSFUL 767ms，产出已签名 `app-release.apk` 11.8M，`apksigner verify --verbose` V2 scheme TRUE
  - 机器清洁度：keystore.properties 已恢复 / 无 .bak 残留 / release.jks 完好
- **零偏离 spec**：代码终态与 spec §A/§B 完全吻合
- 已知局限：改动来源不可考（未查 git 历史，遵守 git 安全协议）；场景 1 命中 UP-TO-DATE 但 BUILD SUCCESSFUL 明确打印 + unsigned APK 实际存在
- 待 Test subagent 独立复核（不信任 Coding 自评，特别验证：代码状态是否真与 spec 一致 + 四道门真绿 + fallback 双场景真绿 + Design 是否越权改代码）

### Test ✅ PASS WITH NOTES
- 独立重跑四道门（顺序）：
  - 门1 `compileDebugKotlin`：✅ PASS 2s（UP-TO-DATE，16 tasks）
  - 门2 `testDebugUnitTest --rerun-tasks`：✅ PASS 27s（**独立 XML 计数确认 95 tests / 0 failures / 0 errors / 0 skipped**，11 个测试类）
  - 门3 `assembleDebug`：✅ PASS 2s（41 tasks）
  - 门4 `assembleRelease`：✅ PASS 2s（产出 `app-release.apk` 11.8M 已签名）
- **fallback 双场景独立验证（全绿）**：
  - 场景 1（keystore.properties 缺失）：`mv keystore.properties keystore.properties.bak` → `assembleRelease` ✅ BUILD SUCCESSFUL 1s，产出 `app-release-unsigned.apk` 12M —— **fallback 生效，未签名 APK 构建成功**
  - 立即恢复：`mv keystore.properties.bak keystore.properties` ✅（242 bytes，无 .bak 残留）
  - 场景 2（keystore.properties 恢复）：`assembleRelease` ✅ BUILD SUCCESSFUL 1s，产出 `app-release.apk` 12M（已签名，命名非 unsigned）
- spec §A/§B 逐节落实，代码实证完全吻合：
  - §A fallback 修复：`app/build.gradle.kts` 第 55-57 行含 `if (keystoreProperties.containsKey("storeFile"))` 守卫，与第 28 行字段赋值守卫字面条件一致（spec §六.6 关键不变量保持）
  - §B Icons 清理：5 文件 7 处 AutoMirrored 使用 + 6 条 import，Grep 确认零残留 `Icons.Filled/Default.KeyboardArrowLeft/Right` + 零残留旧 import 路径；NotesScreen 第 16 行 `import ...filled.Add` 保留正确（Add 非方向性图标）
- 硬约束 4 项全 pass：不联网 / 四道门全绿 / Coding≠Test / 全自动
- 范围蔓延检查通过：`git diff --stat` 显示迭代 5/6/7 累计未提交改动（非本轮单轮），迭代 7 范围内仅 6 文件改动，无 spec 外 deprecated 清理蔓延（全仓搜其他方向性 deprecated Icons 零发现）
- Coding 自评核实：四道门全绿 ✅属实 / 95 测试全绿 ✅属实 / fallback 双场景全绿 ✅属实 / 5 文件 7 处 Icons 已迁移 ✅属实 / keystore.properties 已恢复无残留 ✅属实 / 零偏离 spec ✅属实 / "所有改动在它开始前已就位，本轮零代码编辑" ⚠️无法证实也无法证伪（git 未提交状态使归因不可能）
- **Design 越权调查结果**：**无法从 git 证据定论**。最近提交 `d5c61ea feat: rebalance homepage composition (plan C)`，迭代 5/6/7 全部改动均未提交堆叠工作区。NotesScreen.kt git diff 显示迁移确实作为未提交编辑发生，但 git 无法区分是 Design 还是 Coding 应用的。其余 4 个 Icons 文件为未跟踪（迭代 6 新建）无 git 历史。**关键事实**：无论谁改的，当前磁盘态与 spec 完全吻合（已独立逐行核对）。**进程建议**：Design subagent 职责应限于写 spec，不应直接改代码文件；建议每迭代结束后提交一次便于 Test 归因与回滚
- 发现 3 个非阻塞问题（记 backlog）：
  1. **进程**：迭代间不提交导致归因困难（Test 无法隔离单轮改动归因，Design 越权调查无定论），建议每迭代 commit 一次
  2. **工具链**：apksigner 不在 PATH（spec 标注可选，当前以 APK 命名约定为签名状态判据，足够可靠）
  3. **build.gradle.kts 第 26-27 行注释**：迭代 5 写下时实际为假（无条件引用会 fail），迭代 7 修复后才为真，注释现已准确无需改动
- **判定：✅ PASS WITH NOTES。** 四道门 + fallback 双场景全绿，spec 逐节落实，零范围蔓延。Design 越权指控无法定论（进程观察非代码缺陷），代码终态正确性不受影响

### Persist ✅
- 质量评分：92 → **92**（+0，但清 $100 线 caveat 1），详见 `quality-scorecard.md`
  - 上架成熟度 15（满分保持）：fallback 修复让 release 配置在 CI/全新 clone 也工作，清掉 caveat 1
  - 代码质量 20（满分保持）：deprecated Icons 清理是 nit 级，代码质量已满分
  - 其余维度不变
- backlog 清理：移除已修项 fallback-bug（已修，方向 a 条件引用）+ deprecated（7 处 Icons 已迁移到 AutoMirrored）
- backlog 新增（进程）：迭代间不提交导致归因困难（建议每迭代 commit 一次）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-7 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：fallback bug 修复（方向 a 条件引用，keystore.properties 缺失时产出 unsigned APK 构建成功）+ deprecated Icons 清理（5 文件 7 处迁移到 AutoMirrored，零残留）+ fallback 双场景验证全绿
- **里程碑**：**$100 线 caveat 1 清除**——release 配置在 CI/全新 clone 也工作，"技术性达标"更稳固。剩余 caveat 仅 release 真机验证（需真机，orchestrator 无法执行）
- **$100 质量线判定**：**✅ 稳固达标**（92 ≥ 90 + 上架成熟度满分 + 无 blocker + 四道门绿含 CI 场景 + 文档基本一致）。剩余 caveat 仅 release 真机验证（R8 运行期有效性，需真机）
- **按用户停止条件**：已达 $100 质量线且更稳固，但未跑满 20 轮迭代（当前 7 轮），**继续迭代**。后续轮次重点：backlog nit 清理 / 新功能（超 $100 线）/ 真机验证（需用户配合）/ 文档打磨

---

## 当前 loop 状态

- 进行中迭代：8（待启动）
- 已完成迭代：7
- 累计质量提升：62 → 92（+30）
- **$100 质量线**：✅ 稳固达标（92 ≥ 90，caveat 1 已清，剩 release 真机验证 caveat），但未跑满 20 轮，继续迭代
- 下一动作：迭代 8 Discover，候选目标 backlog nit 清理(M1-legacy/B2-legacy/窄屏) / 新功能(V2年视图/搜索/导入，超$100线) / 文档打磨 / 真机验证(需用户配合)

---

## 迭代 8 (2026-06-28) — 数据导入（JSON 导入，备份/恢复闭环）

### Discover ✅
- **推荐本轮目标**：**数据导入（JSON 导入）**，预估 +1~2（核心功能完整度 36→37~38），完成备份/恢复闭环
- **可行性高**：schema 已定（迭代4 `DataExportManager.SCHEMA_VERSION = 1`）/ 零新依赖（`org.json` 反向解析）/ 复用模式多（DataExportManager 纯函数 → DataImportManager.parseJsonString / ExportViewModel 状态机 → ImportViewModel / SettingsScreen SAF launcher + SettingsRow）/ DAO 已用 `OnConflictStrategy.REPLACE` 天然幂等
- **候选评估**：
  - 候选 1 数据导入：+1~2，工作量 M，风险低-中，复用模式多，**性价比最高** ✅ 推荐
  - 候选 2 热力图年视图：+1~2，工作量 M+-L，53 列布局有 UI 不确定性 ⏸ 暂缓（迭代9）
  - 候选 3 搜索：+1~2，工作量 M，独立性强复用模式少 ⏸ 暂缓（迭代10）
  - 候选 4 backlog nit：+0~1，代码质量已满分加分不确定，风险高于收益 ❌ 不推荐
  - 候选 5 文档打磨：+1~2，工作量 S，但不提升产品力 ⏸ 暂缓（迭代11）
- **13 轮规划建议**：先攻新功能（8 导入 / 9 年视图 / 10 搜索）→ 文档打磨（11）→ backlog 清理（12）→ 灵活轮次（13-19）→ 最终验收（20）
- **orchestrator 拍板 4 个决策点（全部采纳方案 A）**：
  1. 导入数据覆盖策略：方案 A 合并（保留原 ID，冲突时 REPLACE 覆盖）—— 与导出对称，DAO 已用 REPLACE，幂等可重复导入
  2. timeVizPrefs 是否导入：方案 A 导入（覆盖现有生日/寿命）—— 与导出对称，完成完整恢复；JSON 中是可选字段，缺失时跳过不改现有
  3. 导入入口位置：方案 A 设置页「数据导入」行（与「数据导出」并列）—— 最自然，最小改动
  4. 导入前确认对话框：方案 A 有确认对话框（"导入将覆盖同 ID 数据，确认？"）—— 数据操作应有确认，防误操作
- **范围**：
  - 做：DataImportManager.parseJsonString 纯函数 + ImportViewModel 状态机 + TimeVizPrefs setter + SettingsScreen 导入行 + SAF OpenDocument + 确认对话框 + Snackbar + 测试
  - 不做：不改 DB schema / 不改导出格式 / 不做导入历史/增量/进度条 / 不做 schemaVersion 迁移（首版只支持 v1）/ 不动其他 feature
- **风险评级：低-中**。主要风险点 ID 冲突覆盖（缓解：确认对话框 + REPLACE 幂等）/ 损坏 JSON（catch + Error 状态）/ timeVizPrefs setter key 一致性（现有 key 已固定）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-data-import-design.md`（本轮 Design 严格遵守约束，**只写 spec 文件，未碰任何代码文件**）
- 5 关键设计决策：
  1. **DataImportManager 放 `com.shijiben.data.export` 包**（与 DataExportManager 同包）：`import` 是 Kotlin 关键字不能作包名；同包共享 `DataExportManager.SCHEMA_VERSION` 常量（单一来源），构成备份/恢复对
  2. **parseJsonString 严格解析 + 容错分层**：损坏/空 JSON → `JSONException`；schemaVersion≠1 或缺失 → `IllegalArgumentException`；events/notes/timeVizPrefs 数组缺失 → 容错（空列表 / null 跳过）；必填字段缺失 → `JSONException`。所有异常由 VM `catch(Exception)` 统一兜为 Error 状态，与 ExportViewModel 对称
  3. **Repository 加 `upsertAll` 薄包装保留原 ID**：既有 `createEvent`/`createNote` 走 autoGenerate 不保留导入 ID；新增 `upsertAll` 直接调既有 `insertEvent`/`insertNote`（已 `OnConflictStrategy.REPLACE`）传入含原 ID 的完整实体，幂等可重复导入，不改 Dao/schema
  4. **ImportViewModel 与 ExportViewModel 完全对称**：同四构造参（eventRepo/noteRepo/timeVizPrefs/@IoDispatcher）、同 sealed state 模式（Idle/Importing/Success/Error），Success 携带计数供 Snackbar 文案
  5. **确认对话框复用 TimelineScreen 的 AlertDialog 范式**：`AlertDialog`/`TextButton`/`Error`/`TextSecondary` 均已在项目内使用（非首用、非新依赖）；点导入行 → 弹确认 → SAF `OpenDocument` → 选文件 → `openInputStream` → `vm.import(stream)`
- **重要读码发现**：`TimeVizPrefs` 的 `setBirthdayMillis`/`setLifespanYears` **在迭代 4 已一并落地**（接口 `:13,19` + Impl `:38-47` 均已存在）。故任务原计划的「子项 C 加 setter」**退化为「复核无改动」**，减少本轮工作量
- 涉及文件：
  - 新增 4：`DataImportManager.kt` + `ImportViewModel.kt` + `DataImportManagerTest.kt` + `ImportViewModelTest.kt`
  - 改 3：`EventRepository.kt`（+upsertAll）+ `NoteRepository.kt`（+upsertAll）+ `SettingsScreen.kt`（导入行 + SAF + 确认对话框 + Snackbar + ImportViewModel 参数）
  - 复核无改动 9：TimeVizPrefs（setter 已存在）/ 两个 Dao / 两个 Entity / DataExportManager / DispatchersModule / ExportViewModel / MainCoroutineRule / AppNavHost / build.gradle.kts / AndroidManifest
- 风险评估：
  - OpenDocument UI 链路不可单测（SAF launcher 难模拟）→ 缓解：可测逻辑全剥离到 parseJsonString（纯函数）+ applyImport（注入 Repository）+ ImportViewModel（注入 InputStream+dispatcher），UI 仅极薄接线
  - upsertAll 循环单条插入：本地量级安全；极端万+量级回退为 Dao 批量 @Insert(List)（非 schema 变更）
  - REPLACE 覆盖不可逆：由确认对话框 + 文案明示，幂等可重复导入
  - 测试范式对齐既有稳定测试：ImportViewModelTest 复用 ExportViewModelTest 的 StandardTestDispatcher + runTest(mainRule.dispatcher) + state.first{terminal} suspending wait 模式，降低 flaky 风险
- 无新增决策点，4 个 orchestrator 已拍板决策全部落地

### Coding ✅
- 改动文件清单（对照 spec §五）：
  - **新增 4**：`DataImportManager.kt`（纯函数 parseJsonString + 薄 IO applyImport/readFromStream + ImportResult/ImportCounts/ImportedTimeVizPrefs data class，放 data.export 包避开 Kotlin 关键字 import）/ `ImportViewModel.kt`（@HiltViewModel + Idle/Importing/Success/Error 状态机 + import/resetState/markError，与 ExportViewModel 对称）/ `DataImportManagerTest.kt`（15 测试）/ `ImportViewModelTest.kt`（8 测试）
  - **改 3**：`EventRepository.kt`（+upsertAll 薄包装调既有 insertEvent REPLACE）/ `NoteRepository.kt`（+upsertAll 同上）/ `SettingsScreen.kt`（顶部 import 加 AlertDialog/TextButton/Error/TextSecondary + 签名加 importViewModel 默认参数 + 收集 importState + OpenDocument launcher + showImportConfirm 状态 + LaunchedEffect 消费 Success/Error + 数据导入 SettingsRow + AlertDialog 确认对话框）
  - **复核无改动 9**：TimeVizPrefs（setter 已存在 ✅）/ 两个 Dao（insertEvent/insertNote 已 REPLACE ✅）/ 两个 Entity / DataExportManager / DispatchersModule / ExportViewModel / MainCoroutineRule / AppNavHost / build.gradle.kts / AndroidManifest
- **四道门结果（全绿）**：
  - 门1 `compileDebugKotlin`：✅ PASS 4s
  - 门2 `testDebugUnitTest --rerun-tasks`：✅ PASS ~30s（13 套件 / **118 tests** / 0 failures / 0 errors，现有 95 + 新增 23）
  - 门3 `assembleDebug`：✅ PASS 2s
  - 门4 `assembleRelease --rerun-tasks`：✅ PASS 1m7s（产出 app-release.apk 11.8M）
- **新增 23 测试**：
  - DataImportManagerTest（15）：validFullJson / nullEndTimeAndNote / eventsMissing / notesMissing / timeVizPrefsMissing / corruptJson / emptyString / schemaVersionMismatch / schemaVersionMissing / partialFieldMissing / specialCharacters / roundTripWithExport / applyImport_idConflict_replacesExisting（含幂等二次导入）/ applyImport_prefsNull_doesNotChangePrefs / applyImport_prefsNonNull_overwritesPrefs
  - ImportViewModelTest（8）：validData_transitionsToSuccessAndPersists / corruptJson_transitionsToError / emptyStream_transitionsToError / schemaMismatch_transitionsToError / timeVizPrefsMissing_doesNotChangePrefs / idConflict_replacesExisting / setsImportingBeforeFinalState / resetState_returnsToIdle
- **两处轻微偏离 spec（合理）**：
  1. `ImportViewModelTest` fake 类命名 `ImportFakeTimeVizPrefs`（spec 暗示复用 FakeTimeVizPrefs）—— 理由：与同包 ExportViewModelTest 的 private class FakeTimeVizPrefs 重名，Kotlin Redeclaration + private in file 不可访问；改名是最小改动不影响语义
  2. DataImportManagerTest 多 2 个 applyImport 测试（prefsNull/prefsNonNull）—— 理由：spec §7.2 表未直接覆盖 applyImport 的 prefs 分支，加 2 个直接测试独立验证，属合理增量覆盖
- 过程中发现并修复的两处问题（最终四门前修复）：
  1. ImportViewModelTest 与 ExportViewModelTest 的 FakeTimeVizPrefs Redeclaration → 改名 ImportFakeTimeVizPrefs
  2. parseJsonString_partialFieldMissing_throwsJSONException 用空 events 数组取 getJSONObject(0) 越界 → 构造时传入一个 event 修复
- 核心实现零偏离 spec §4.3~4.7
- 已知局限：SAF UI 链路仅手动验证（OpenDocument launcher → Uri → openInputStream → import）/ 确认对话框 UI 仅手动验证 / upsertAll 循环单条插入（本地量级安全）/ 严格解析拒绝部分字段缺失
- 待 Test subagent 独立复核（不信任 Coding 自评，特别验证：23 新测试属实 + 118 总数 + spec 逐节落实 + 备份/恢复闭环有效 + 零回归）

### Test ✅ PASS WITH NOTES
- 独立重跑四道门（顺序，禁止并行）：
  - 门1 `compileDebugKotlin`：✅ PASS 1s（16 up-to-date）
  - 门2 `testDebugUnitTest --rerun-tasks`：⚠️ flaky→✅。**首次 FAILED**（`HeatmapViewModelTest > nextMonth_fromPreviousMonth_returnsToCurrent`，`IllegalStateException at TestMainDispatcher.kt:67`，118 tests 1 failed），重跑 BUILD SUCCESSFUL 17s，独立 XML 计数 **118 tests / 0 failures / 0 errors / 0 skipped**。失败测试名与迭代3 Test 报告记录的 flaky 完全一致，迭代8 未碰 HeatmapViewModelTest，**证实为迭代2/3/4 遗留 flaky 复现，非本轮回归**
  - 门3 `assembleDebug`：✅ PASS 1s（41 tasks，3 executed）
  - 门4 `assembleRelease --rerun-tasks`：✅ PASS 1m3s（55 tasks 全 executed，产出 `app-release.apk` 11.8M）
- spec §A/§B/§C/§D/§E 全节字面落实，逐项有代码实证：
  - §A DataImportManager（`data/export/DataImportManager.kt`）：parseJsonString 纯函数（:54-64 仅 org.json 无副作用）/ schemaVersion 校验严格（:56-58 optInt(-1)≠1 抛 IllegalArgumentException）/ 容错分层正确（损坏→JSONException / schemaVersion≠1→IllegalArgumentException / 数组缺失→容错 emptyList / 必填字段缺失→JSONException）/ ImportResult data class 齐全 / applyImport 薄 IO（:119-138 调 upsertAll + setter）/ readFromStream 薄包装（:141-142）
  - §B ImportViewModel（`feature/settings/ImportViewModel.kt`）：@HiltViewModel + 四构造参（eventRepo/noteRepo/timeVizPrefs/@IoDispatcher）/ ImportState sealed interface 四态齐全（Idle/Importing/Success(events,notes,prefsUpdated)/Error(msg)）/ import(inputStream) Idle→Importing→Success/Error 单向 / resetState() 回 Idle / markError() / 与 ExportViewModel 完全对称
  - §C/§4.5 Repository upsertAll 薄包装：EventRepository.kt:58-61 + NoteRepository.kt:31-34，循环调既有 insertEvent/insertNote（已 REPLACE），保留原 ID，不改 Dao/schema
  - §4.6 TimeVizPrefs 复核无改动：接口 :13,19 + Impl :38-47 setBirthdayMillis/setLifespanYears 在迭代4 已落地
  - §D SettingsScreen（`feature/settings/SettingsScreen.kt`）：导入行 SettingsRow（:186-198 与导出行对称，enabled 互斥）/ SAF OpenDocument launcher（:90-98 `application/json`）/ showImportConfirm 状态（:89）/ LaunchedEffect 消费 importState（:115-131 Success/Error → Snackbar + resetState）/ AlertDialog 确认对话框（:206-223 标题/文案/确认 Error 色/取消 TextSecondary）
  - §E 测试覆盖：DataImportManagerTest 15 + ImportViewModelTest 8 = 23 新增，方法名与 Coding 报告逐项一致
- 硬约束 5 项全 pass：
  - 不联网：Grep `OkHttp\|Retrofit\|Volley\|HttpURLConnection\|Firebase\|INTERNET\|Socket(\|HttpClient` 全 app/src **No matches**；新代码仅 org.json + contentResolver.openInputStream（本地 Uri）
  - 四道门全绿：门1/3/4 全绿；门2 flaky 首次 fail 二次过（非本轮回归）
  - Coding≠Test：不同 subagent 实例 ✅
  - 全自动：四道门脚本化，`--rerun-tasks` 排除假绿 ✅
  - 不引入新依赖：build.gradle.kts 依赖清单无网络库，无迭代8 新增依赖；org.json 是 Android 内置 ✅
- 范围蔓延检查通过：迭代8 范围内改动确实限于 spec §五的 7 文件（4 新增 + 3 改动），无 spec 外蔓延。仓库累计未提交是迭代7 起的进程 backlog（非本轮单轮）
- Coding 自评核实（逐项独立验证）：
  - 23 新测试 ✅属实：逐方法核对 DataImportManagerTest.kt（15 @Test）+ ImportViewModelTest.kt（8 @Test），方法名与 Coding 报告清单完全一致
  - 118 总数 ✅属实：独立 XML 计数 13 套件合计 118 = 现有 95 + 新增 23
  - 零回归 ✅属实：现有 95 测试在门2 第二次全绿（flaky 非本轮回归）
  - 备份/恢复闭环有效 ✅属实：
    - DataExportManager.buildJsonString 输出字段与 DataImportManager.parseJsonString 解析字段**逐字段对称**
    - `parseJsonString_roundTripWithExport_symmetric` 测试（DataImportManagerTest.kt:226-253）：2 events + 1 note + prefs → buildJsonString → parseJsonString → containsExactly 验证，**真实覆盖闭环**
    - `applyImport_idConflict_replacesExisting` 测试（DataImportManagerTest.kt:256-290 + ImportViewModelTest.kt:187-221）：预插 id=1 A → 导入 id=1 B → REPLACE 覆盖 → 再导一次仍 1 行（幂等）
    - Repository.upsertAll 调 insertEvent/insertNote，EventDao.kt:31 / NoteDao.kt:22 均 `@Insert(onConflict = OnConflictStrategy.REPLACE)`，**REPLACE 保证幂等**
- 特别验证（代码实证）：
  - parseJsonString 纯函数：无副作用/不调 IO/无随机时间状态依赖，输入→输出确定 ✅
  - ImportViewModel 状态机流转：Idle→Importing→Success/Error 单向，resetState 回 Idle，无 Importing→Idle 回退 ✅
  - Repository upsertAll REPLACE 幂等：EventDao.kt:31 + NoteDao.kt:22 `@Insert(onConflict = OnConflictStrategy.REPLACE)` ✅
  - SettingsScreen 导入行 + 确认对话框 + SAF OpenDocument：:186-198 + :206-223 + :90-98 ✅
  - 编译期类型安全：无反射/无 unsafe cast，用强类型 getter + 判空 ✅
- 发现的问题：
  - **Blocker**：无
  - **Non-blocker（记 backlog）**：
    1. **门2 flaky 复现**：`HeatmapViewModelTest > nextMonth_fromPreviousMonth_returnsToCurrent` 偶发 `IllegalStateException at TestMainDispatcher.kt:67`。迭代4 声称修复（连续 3 次全绿），但迭代8 验证时首次跑即复现。**迭代2/3/4 遗留问题，非迭代8 引入**。建议下轮彻底根治（可能需改 MainCoroutineRule 用 StandardTestDispatcher 或重构 HeatmapViewModelTest 的 dispatcher 时序）
    2. **Coding 报告门2 结果不完整**：Coding 报告"门2 ✅ 0 failures"未提及 flaky 风险。其跑的那次确实通过（非造假），但应提示 flaky 仍存。属报告严谨性问题，非代码缺陷
  - **Note（合理偏离 spec）**：
    1. ImportFakeTimeVizPrefs 命名（ImportViewModelTest.kt:255）：评估**合理**。DataImportManagerTest 在 `data.export` 包用 `FakeTimeVizPrefs`（private class 不冲突）；ImportViewModelTest 在 `feature.settings` 包，与同包 ExportViewModelTest 的 private class `FakeTimeVizPrefs` 重名（Kotlin Redeclaration + private in file 不可访问）。改名是最小改动不影响语义
    2. DataImportManagerTest 多 2 个 applyImport 测试（prefsNull/prefsNonNull，:292-326）：评估**合理**。spec §7.2 表确实未直接覆盖 applyImport 的 prefs 分支，加 2 个测试直接验证 prefs null/非 null 分支，属合理增量覆盖
- **判定：✅ PASS WITH NOTES。** 四道门全绿（门2 flaky 非本轮回归）/ spec 全节字面落实 / 硬约束 5 项全 pass / 范围蔓延检查通过 / Coding 自评基本属实 / 备份恢复闭环有效 / 两处偏离 spec 均合理。**迭代8 数据导入实现正确，备份/恢复闭环有效，可合并**

### Persist ✅
- 质量评分：92 → **94**（+2，核心功能完整度 36→38），详见 `quality-scorecard.md`
  - 核心功能完整度 36→38：备份/恢复闭环完成（导出+导入对称）+ 数据可移植性达成 + 23 新测试覆盖纯函数边界+VM 状态机+落库幂等 + round-trip 测试真实覆盖闭环
  - 代码质量 20（满分保持）：23 新测试提升覆盖，但 flaky test 复现记入 backlog（迭代4 修复方案未彻底根治）
  - 上架成熟度 15（满分保持）：数据导出已在迭代4 达成，本轮导入是核心功能完整度维度加分
  - 文档可维护性 6（保持）：spec 已落盘，文档与代码一致；剩历史 spec 过时表述 nit
- backlog 更新：
  - **flaky test 状态回退**：迭代4 声称修复（连续 3 次全绿），但迭代8 Test 首次跑即复现。状态从"已修复"改为"迭代4 修复方案未彻底根治，需重新评估"
  - 新增：Coding 报告严谨性（门2 flaky 未提示，属报告严谨性非代码缺陷）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-8 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：数据导入完整实现（DataImportManager 纯函数 + ImportViewModel 状态机 + Repository upsertAll + SettingsScreen 导入行 + SAF OpenDocument + 确认对话框 + Snackbar + 23 新测试），备份/恢复闭环有效（导出 JSON → 导入 JSON → 数据一致 + REPLACE 幂等）
- **里程碑**：**备份/恢复闭环完成**——数据可移植性达成，卸载/换机不丢数据。$100 质量线从 92→94 更稳固
- **$100 质量线判定**：**✅ 稳固达标**（94 ≥ 90 + 上架成熟度满分 + 无 blocker + 四道门绿含 CI 场景 + 文档基本一致）。剩余 caveat：release 真机验证（需真机）+ flaky test 复现需下轮根治
- **按用户停止条件**：已达 $100 质量线且更稳固，但未跑满 20 轮迭代（当前 8 轮），**继续迭代**。后续轮次重点：迭代9 热力图年视图（+1~2）/ 迭代10 搜索 / 迭代11 文档打磨 / 迭代12 backlog（含 flaky 根治）/ 迭代13-19 灵活 / 迭代20 最终验收

---

## 当前 loop 状态

- 进行中迭代：9（待启动）
- 已完成迭代：8
- 累计质量提升：62 → 94（+32）
- **$100 质量线**：✅ 稳固达标（94 ≥ 90，caveat 1 已清，剩 release 真机验证 caveat + flaky test 复现需根治），但未跑满 20 轮，继续迭代
- 下一动作：迭代 9 Discover，候选目标热力图年视图(+1~2) / 搜索(+1~2) / 文档打磨(+1~2) / flaky 根治 / backlog nit 清理

---

## 迭代 9 (2026-06-28) — flaky test 根治 + 文档打磨

### Discover ✅
- **推荐本轮目标**：**flaky test 根治（主）+ 文档打磨（次）**，预估 +0~1（flaky 根治 +0 但过程价值高，文档打磨 +0~1）
- **flaky 根治优先级提升理由**：迭代8 Test 首次跑即复现 flaky（`HeatmapViewModelTest > nextMonth_fromPreviousMonth_returnsToCurrent`，`IllegalStateException at TestMainDispatcher.kt:67`），证实迭代4 修复方案未彻底根治。每轮 Test subagent 都被 flaky 干扰，根治是后续 11 轮的过程基石
- **根因精确定位**（实读代码确认，非假设）：
  - 迭代4 修复方案（StandardTestDispatcher + first{} + backgroundScope collector）解决了**等待阶段**时序问题，但未解决**teardown 阶段**竞态
  - 根因链：HeatmapViewModel.state = flatMapLatest + WhileSubscribed(5000) → 5s grace period → 测试 teardown 时上游 Room Flow 仍活着 → Room invalidation tracker 在真实 executor 线程触发 invalidation → dispatch 到 viewModelScope（Main = StandardTestDispatcher）→ MainCoroutineRule.finished() 调 resetMain() → Main 变 NoopDispatcher → Room 线程 dispatch 命中 NoopDispatcher → IllegalStateException at TestMainDispatcher.kt:67
  - 实证对比：ExportViewModelTest 同样用真实 Room + StandardTestDispatcher 但不 flaky（因 ExportViewModel 用 MutableStateFlow + 一次性 .first() 取数，无 flatMapLatest + WhileSubscribed 持续订阅，无 teardown 竞态）
- **候选评估**：
  - 候选 1 flaky 根治：+0，工作量 S，风险低，过程价值高 ✅ 主目标
  - 候选 2 热力图年视图：+1~2，工作量 M+-L，UI 不确定性 ⏸ 暂缓（迭代10）
  - 候选 3 搜索：+1~2，工作量 M，复用模式少 ⏸ 暂缓（迭代11）
  - 候选 4 文档打磨：+0~1，工作量 S，风险低 ✅ 次目标（与 flaky 组合 S+S=M）
  - 候选 5 backlog nit：+0~1，风险高于收益 ❌ 不推荐
- **orchestrator 拍板 3 个决策点（全部采纳推荐选项 A）**：
  1. flaky 根治方案 = 方案 A（路由 Room executor 到 StandardTestDispatcher，仅改 HeatmapViewModelTest setup()，不改 MainCoroutineRule，不改生产代码）
  2. 组合方式 = flaky 根治 + 文档打磨组合
  3. 验证标准 = 门2 连续 5 次 --rerun-tasks 全绿

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-flaky-rootfix-and-docs-design.md`（严格遵守约束，**只写 spec 文件，未碰任何代码文件**，全程用符号引用无行号）
- **关键技术验证通过**（Design 阶段反编译 Room 2.6.1 字节码确认）：
  - `InvalidationTracker.refreshRunnable` 经 `database.getQueryExecutor().execute(refreshRunnable)` 调度
  - `RoomDatabase.getQueryExecutor()` 直接返回 `internalQueryExecutor` 字段，无 fallback
  - `RoomDatabase.Builder` 提供 `setQueryExecutor` / `setTransactionExecutor`
  - → 路由 executor 到 StandardTestDispatcher 可控住 invalidation tracker，方案 A 成立，不触发 fallback
- 关键设计决策：
  1. **唯一改动的代码文件**：HeatmapViewModelTest.kt，仅 setup() 加 roomExecutor 适配器（`Executor { cmd -> mainRule.dispatcher.dispatch(EmptyCoroutineContext, cmd) }`）+ setQueryExecutor / setTransactionExecutor（~15 行）。7 个 @Test / teardown() / mainRule 零改动
  2. **三处统一**（Main / TestScope / Room executor 共用 mainRule.dispatcher）是消除 teardown 竞态的关键：测试体内 runTest auto-advance 队列驱动 Room emit；teardown 后队列静止，残留 invalidation 任务永不执行，不命中 NoopDispatcher
  3. **文档打磨**：仅改 AGENT.md 两处——Spec 索引补 iter7/iter8 两条 + 项目结构 data/export/ 与 feature/settings/ 两行注释补 DataImportManager / ImportViewModel
- fallback 路径已写入 spec：方案 A 验证已通过字节码确认，但保留方案 C（HeatmapViewModel 改 WhileSubscribed→Lazily）作为实测兜底，无需重新问 orchestrator
- 风险评估：Room invalidation tracker 可能不用 configured executor（已字节码证伪）/ 路由 executor 后 first{} 可能 hang（现有测试已证可行）/ 5 次重跑仍可能偶发失败（概率问题，fallback 方案 C）

### Coding ✅
- 改动文件清单（对照 spec §五）：
  - **改 2**：
    - `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt`：setup() 加 roomExecutor 适配器（`Executor { command -> mainRule.dispatcher.dispatch(EmptyCoroutineContext, command) }`）+ setQueryExecutor / setTransactionExecutor + 根因注释；7 个 @Test / teardown() / mainRule 零改动；用 fully-qualified `java.util.concurrent.Executor` / `kotlin.coroutines.EmptyCoroutineContext`（避免 import 膨胀）
    - `AGENT.md`：Spec 索引补 iter7/iter8 两条 + 项目结构 data/export/ 补 DataImportManager + feature/settings/ 补 ImportViewModel
  - **复核无改动**：MainCoroutineRule.kt（6 测试共用，blast radius = 0）/ HeatmapViewModel.kt（生产代码，WhileSubscribed(5000) 保留）/ ExportViewModelTest.kt（不 flaky，作对照）/ TimelineViewModelTest.kt + NotesViewModelTest（当前未复现 flaky）/ EventRepositoryHeatmapTest.kt / EventRepository.kt / DAO / Entity / AppDatabase.kt
- **四道门结果（全绿）**：
  - 门1 `compileDebugKotlin`：✅ PASS 690ms（16 up-to-date，main 代码未改）
  - 门2 `testDebugUnitTest --rerun-tasks`（连续 5 次）：✅ 每次全绿（118 tests / 0 failures / 0 errors，无 IllegalStateException at TestMainDispatcher.kt:67）。每次耗时 18s/17s/17s/17s/17s
  - 门3 `assembleDebug`：✅ PASS 1s
  - 门4 `assembleRelease --rerun-tasks`：✅ PASS 59s（产出 app-release.apk 11.8M）
- **flaky 根治验证结论**：5 次全绿 ✅，方案 A 实测有效，无需 fallback 到方案 C，生产代码零改动
- **偏离 spec**：适配器用 spec §4.2.2 精简版（dispatch 直接传 command，无 Runnable 包装）—— spec 明确"二者签名直接桥接"，合理
- **已知局限**：5 次全绿是高置信度非数学证明 / Timeline/Notes 潜在 flaky 风险未处理（当前未复现）/ HeatmapViewModelTest 实际 7 个 @Test（spec §4.2.4 说 8 个是笔误，§7.3 表格列 7 行）

### Test ✅ PASS WITH NOTES
- 独立重跑四道门 + flaky 根治独立验证（顺序，禁止并行）：
  - 门1 `compileDebugKotlin`：✅ PASS 665ms
  - 门2 `testDebugUnitTest --rerun-tasks`（连续 5 次独立验证）：✅ 每次测试执行全绿（118 tests / 0 failures / 0 errors / 0 skipped，无 IllegalStateException）。第5次首次 daemon 卡住 150s 无输出（非测试 hang，非 flaky 复现），StopCommand 后重跑 17s 成功。5 次测试执行全绿证实 flaky 根治方案 A 有效
  - 门3 `assembleDebug`：✅ PASS 968ms
  - 门4 `assembleRelease --rerun-tasks`：✅ PASS 59s（产出 app-release.apk 11.8M）
- spec §4.2/§4.3 全节字面落实，逐项有代码实证：
  - §4.2 flaky 根治：roomExecutor 适配器（HeatmapViewModelTest.kt setup()，`Executor { command -> mainRule.dispatcher.dispatch(EmptyCoroutineContext, command) }`）/ setQueryExecutor + setTransactionExecutor 都配置 / 7 个 @Test 零改动（initialState_isCurrentMonth / previousMonth_decrementsAndEnablesNext / nextMonth_fromPreviousMonth_returnsToCurrent / nextMonth_atCurrentMonth_doesNotAdvance / goToCurrentMonth_fromPrevious_returnsToCurrent / stateCells_shapeIs6x7AndTodayMarked / stateCells_updatesWhenRepoEmitsNewData）/ teardown() + mainRule 零改动 / 根因注释存在
  - §4.3 文档打磨：Spec 索引补 iter7/iter8 两条（AGENT.md Spec 索引段）+ 项目结构 data/export/ 补 DataImportManager + feature/settings/ 补 ImportViewModel + 文件路径存在性 Glob 全部确认
- 硬约束 5 项全 pass：不联网（Grep 全 app/src 无网络库，roomExecutor 仅调 dispatch 无网络）/ 四道门全绿 / Coding≠Test / 全自动 / 不引入新依赖（build.gradle.kts 无新增）
- 范围蔓延检查：迭代9 范围内仅 2 文件改动（HeatmapViewModelTest.kt + AGENT.md），无 spec 外蔓延。仓库累计未提交是迭代7 起的进程 backlog（非本轮单轮）
- Coding 自评核实（逐项独立验证）：
  - 5 次全绿 ✅属实：独立验证 5 次测试执行全绿
  - 118 测试总数 ✅属实：独立 XML 计数 13 testsuite 合计 118
  - 7 个 @Test 零改动 ✅属实：Read HeatmapViewModelTest.kt 数 @Test=7，setup() 是唯一改动点
  - 文档打磨 4 处 ✅属实：Read AGENT.md 确认 Spec 索引 2 条 + 项目结构 2 符号
- 特别验证（代码实证）：
  - flaky 根治方案 A 有效性 ✅：5 次独立重跑全绿，0 次 IllegalStateException，竞态窗口从根上消除
  - roomExecutor 适配器正确性 ✅：mainRule.dispatcher 是 StandardTestDispatcher（非 UnconfinedTestDispatcher）/ dispatch 签名正确 / setQueryExecutor + setTransactionExecutor 都配置
  - 文档打磨准确性 ✅：两条 spec 索引文件名与实际 spec 文件一致 / 项目结构符号与实际 Kotlin 文件一致
  - 生产代码零改动 ✅：HeatmapViewModel.kt WhileSubscribed(5000) 保留，flatMapLatest + stateIn 模式未动
  - MainCoroutineRule.kt 零改动 ✅：blast radius = 0，5 个共用 rule 的测试不受影响
- 发现的问题：
  - **Blocker**：无
  - **Non-blocker（记 backlog）**：
    1. 门2 第5次首次 daemon 卡住 150s 无输出（非代码缺陷、非 flaky 复现，重跑成功）。可能是多次 --rerun-tasks 后 daemon 内存累积。建议后续观察，若复现可考虑 --no-daemon 或 ./gradlew --stop
    2. 进程问题持续：仓库迭代 2-9 大量未提交改动导致 Test 无法 git 隔离单轮归因（迭代7 起已记 backlog）
  - **Note（合理偏离 spec）**：
    1. 适配器用 spec §4.2.2 精简版（dispatch 直接传 command，无 Runnable 包装）—— spec 明确"二者签名直接桥接"，合理
    2. spec §4.2.4/§7.2 说"8 个 @Test"是 spec 笔误，实际 7 个（§7.3 表格列 7 行），Coding 如实声明
- **判定：✅ PASS WITH NOTES。** flaky 根治方案 A 经独立验证有效（5 次测试执行全绿，0 次 IllegalStateException，对比迭代8 Test 首次跑即复现 flaky）/ spec 全节字面落实 / 硬约束 5 项全 pass / 范围蔓延检查通过 / Coding 自评全部属实 / 生产代码零改动 + MainCoroutineRule.kt 零改动 blast radius = 0。**迭代9 flaky 根治 + 文档打磨实现正确，可合并**

### Persist ✅
- 质量评分：94 → **94**（+0，过程改进不加分），详见 `quality-scorecard.md`
  - 代码质量 20（满分保持）：flaky 根治清掉 backlog 里的 flaky-test 条目，但代码质量已满分不加分
  - 文档可维护性 6（保持）：补 Spec 索引 + 项目结构注释是补缺非提升，历史 spec 过时表述 nit 仍存
  - 其余维度不变
- backlog 清理：移除已修项 flaky-test（方案 A 路由 Room executor 根治，5 次独立验证全绿）+ 报告严谨性（flaky 已根治，Coding 报告严谨性问题不再相关）
- backlog 更新：进程问题持续（迭代2-9 大量未提交）+ 门2 第5次 daemon 卡住新增 backlog
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-9 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：flaky test 根治（方案 A 路由 Room executor 到 StandardTestDispatcher，5 次独立验证全绿，迭代4 遗留 flaky 问题彻底解决）+ 文档打磨（AGENT.md Spec 索引补 iter7/iter8 + 项目结构补 DataImportManager/ImportViewModel）
- **里程碑**：**验证门可信度恢复**——后续每轮 Test subagent 不再被 flaky 干扰。flaky 根治是后续 11 轮的过程基石
- **$100 质量线判定**：**✅ 稳固达标**（94 ≥ 90 + 上架成熟度满分 + 无 blocker + 四道门绿含 CI 场景 + 文档基本一致）。剩余 caveat：release 真机验证（需真机）。flaky 已根治不再 caveat
- **按用户停止条件**：已达 $100 质量线且更稳固，但未跑满 20 轮迭代（当前 9 轮），**继续迭代**。后续轮次重点：迭代10 热力图年视图（+1~2）/ 迭代11 搜索（+1~2）/ 迭代12 文档打磨 / 迭代13-19 灵活 / 迭代20 最终验收

---

## 迭代 10 (2026-06-28) — 热力图年视图（方案 C：12 月迷你月历拼贴）

### Discover ✅
- 推荐本轮目标：**热力图年视图（方案 C：12 月迷你月历拼贴，3 列 × 4 行）**，预估加分 +1~2（核心功能完整度 38→39~40 满分）
- 价值理由：年视图是"回看"维度的自然扩展（月→年），用户可一眼扫全年活跃节奏；零联网零新依赖（复用 EventDao/HeatmapCalculator/Cell/色阶方案 A）；零回归（新增独立 aggregateYear/buildYearGrid/HeatmapYearScreen/HeatmapYearViewModel，不改月视图代码）
- 不优先攻搜索的理由：搜索需新增 UI 状态机 + 全文检索逻辑 + 索引决策，M+ 工作量且与年视图无关；年视图复用度高、风险低、加分稳
- 不优先清文档 backlog 的理由：核心功能完整度还有 2 分空间，文档 nit 仅 +0~1
- **orchestrator 拍板 3 个决策点（全部采纳推荐）**：
  1. 年视图布局方案：选项 C（12 月迷你月历拼贴，3×4，竖屏自然滚动，复用月视图 Cell）
  2. 入口方案：选项 3（HeatmapScreen 顶栏"年"跳转按钮 → 独立 HeatmapYearScreen）
  3. 是否组合文档打磨：选项 A（单独做年视图，避免范围蔓延）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-heatmap-year-view-design.md`
- 关键设计决策：
  1. `aggregateYear` 独立实现（复用 `effectiveDurationMs`，不泛化 `aggregateMonth`，归属谓词用 `Year.from` 而非 `YearMonth.from`，避免跨年污染）
  2. `buildYearGrid` 完全复用 `buildGrid`（每月调一次，12 个 MonthGrid 组成 YearGrid）
  3. `HeatmapYearViewModel` 平行 `HeatmapViewModel`（不注入 dispatcher，`currentYear` 计算属性 B2 修复模式，`canGoNext = year < currentYear` 防未来穿越）
  4. `HeatmapScreen.onYearClick` 无默认值（编译期捕获调用方遗漏，与 B2 修复同款硬约束）
  5. Mini 格子 `weight(1f).aspectRatio(1f)` + 1dp 边框，纯展示不可点击（年视图是"扫"不是"点"）
  6. `HeatmapYearViewModelTest` 逐字复用迭代9 方案 A（StandardTestDispatcher + 路由 Room executor + first{} + backgroundScope collector）
- 涉及文件：新增 3（HeatmapYearViewModel/HeatmapYearScreen/HeatmapYearViewModelTest）+ 改 6（EventRepository/HeatmapCalculator/HeatmapScreen/AppNavHost/HeatmapCalculatorTest/EventRepositoryHeatmapTest）
- 复核无改动 9：EventDao / HeatmapModels / AppColors / HeatmapViewModel.kt / aggregateMonth / effectiveDurationMs / MainCoroutineRule / DispatchersModule / build.gradle.kts / AndroidManifest
- 无新增 orchestrator 决策点（设计沿用已拍板方向）

### Coding ✅
- 新增 3 文件 + 改动 6 文件，24 新测试（buildYearGrid 7 + aggregateYear 9 + HeatmapYearViewModel 8），全工程 142 测试
- 三新增文件落地：
  - `HeatmapYearViewModel.kt`：@HiltViewModel + 注入 EventRepository（不注入 dispatcher）+ `_selectedYear`/`state`/`currentYear` 计算属性/`previousYear()`/`nextYear()`/`goToCurrentYear()`/`canGoNext`
  - `HeatmapYearScreen.kt`：RainbowTrim + 顶栏（返回 + "年度回看"）+ YearSwitcher + 12 月 mini 月历拼贴（3×4）+ Legend + MiniMonth（6×7 网格 weight+aspectRatio）+ MiniCell（色阶复用 HeatmapLevel0..4，今天红边框，未来 alpha 0.5）
  - `HeatmapYearViewModelTest.kt`：8 测试，逐字复用方案 A（StandardTestDispatcher + 路由 Room executor + first{} + backgroundScope collector）
- 六改动文件落地：
  - `EventRepository.kt`：+`getDailyActivityForYear(year)` + `aggregateYear`（internal 顶层函数，独立实现复用 `effectiveDurationMs`，归属谓词用 `Year.from`）+ `yearStartEpoch`/`yearEndEpoch` helper
  - `HeatmapCalculator.kt`：+`YearGrid`/`MonthGrid` data class + `buildYearGrid`（遍历 12 月，每月调 `buildGrid` 复用）
  - `HeatmapScreen.kt`：+`onYearClick: () -> Unit` 参数（**无默认值**，编译期捕获调用方遗漏）+ MonthSwitcher Row 末尾"年"跳转按钮
  - `AppNavHost.kt`：+`Routes.HEATMAP_YEAR = "heatmap_year"` + `composable(Routes.HEATMAP_YEAR) { HeatmapYearScreen(...) }` + HeatmapScreen 调用处加 `onYearClick`
  - `HeatmapCalculatorTest.kt`：+7 个 buildYearGrid 测试（12月形状/6×7形状/补位/今天标记/今天出年不标记/活动映射/闰年2-29）
  - `EventRepositoryHeatmapTest.kt`：+9 个 aggregateYear 测试（空/not_started/多事件同日/跨年不污染/进行中clamp/年末边界/时区/出年跳过/闰年2-29）
- Coding 自评：四道门顺序跑全绿（门1 719ms / 门2 19s 142 tests 0 failures / 门3 1s / 门4 1m APK 11.8M），零回归（aggregateMonth/getDailyActivityForMonth/HeatmapViewModel.kt/effectiveDurationMs 全部零改动），复用模式到位（aggregateYear 复用 effectiveDurationMs / buildYearGrid 复用 buildGrid / 方案 A 复用）
- 待 Test subagent 独立复核（不信任 Coding 自评，特别验证零回归声明 + 复用模式落实 + 方案 A 复用是否真有效）

### Test ✅ PASS WITH NOTES
- 独立重跑四道门（顺序，强制 `--rerun-tasks` 排除假绿）全绿：
  - 门1 `compileDebugKotlin` 719ms ✅
  - 门2 `testDebugUnitTest --rerun-tasks` 19s ✅ 142 测试 0 失败 0 @Ignore
  - 门3 `assembleDebug` 1s ✅
  - 门4 `assembleRelease --rerun-tasks` 1m APK 11.8M ✅
- spec §4.1-§4.6 全节字面落实，逐项有代码实证
- 硬约束 5 项全 pass：不联网 / 不引入新依赖 / 不改 DB schema / 不改月视图代码 / 不改 MainCoroutineRule
- Coding 自评全部属实：
  - 24 新测试 / 142 总数核实无误
  - 零回归核实：`aggregateMonth` / `getDailyActivityForMonth` / `HeatmapViewModel.kt` / `effectiveDurationMs` 全部零改动
  - 复用模式核实：`aggregateYear` 复用 `effectiveDurationMs` / `buildYearGrid` 复用 `buildGrid` / 方案 A 复用（HeatmapYearViewModelTest 与 HeatmapViewModelTest setup 一致）
- 特别验证 6 项全 pass：aggregateYear 算法（跨年不污染/进行中 clamp/年末边界/时区/闰年 2-29）/ buildYearGrid 网格（12 月形状 6×7 补位今天标记）/ ViewModel 状态机（currentYear 计算属性 + canGoNext 防未来穿越）/ UI 布局（3×4 拼贴 + MiniMonth + MiniCell 色阶复用）/ 入口链路（HeatmapScreen onYearClick 无默认值 → AppNavHost composable → HeatmapYearScreen）/ 方案 A 复用（HeatmapYearViewModelTest 5 次 --rerun-tasks 稳定，无 IllegalStateException）
- 仅 2 个非阻塞问题：
  - **进程持续**：迭代间不提交导致归因困难（迭代7 起持续问题，本轮延续）
  - **测试方法名略偏 spec**：spec §4.6.3 表格用 `stateMonths_*` 前缀，实际用 `yearGrid_*`；`nextYear_fromPrevious_returnsToCurrent` 实际为 `nextYear_fromPreviousYear_returnsToCurrent`。测试内容与 spec 表格完全一致，命名差异属合理偏离（语义更精准），记 Note 非问题

### Persist ✅
- 质量评分：94 → **96**（+2，核心功能完整度 38→40 满分），详见 `quality-scorecard.md`
- backlog 新增：测试方法名略偏 spec（记 Note，非问题）/ 进程持续（迭代7 起延续）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-10 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：热力图年视图完整实现（方案 C 12 月迷你月历拼贴，3 新增 + 6 改动，24 新测试，四道门全绿），零回归（月视图代码零改动），复用模式到位（aggregateYear/buildYearGrid/方案 A 三重复用）
- **里程碑**：**核心功能完整度满分**（38→40）—— V2 热力图月视图 + V2.1 年视图全部落地，"回看"维度月/年双层闭环
- **$100 质量线判定**：✅ 稳固达标且更优（96 ≥ 90 + 上架成熟度满分 + 无 blocker + 四道门绿含 release + 文档基本一致）。剩余 caveat：release 真机验证（需真机）。flaky 已根治不再 caveat
- **按用户停止条件**：已达 $100 质量线且更优，但未跑满 20 轮迭代（当前 10 轮），**继续迭代**。后续轮次重点：迭代11 搜索（核心功能完整度已满分，加分空间转移到代码质量/文档可维护性）/ 迭代12 文档打磨 / 迭代13-19 灵活 / 迭代20 最终验收

---

## 当前 loop 状态

- 进行中迭代：11（待启动）
- 已完成迭代：10
- 累计质量提升：62 → 96（+34，迭代10 热力图年视图补核心功能完整度最后 2 分到满分）
- **$100 质量线**：✅ 稳固达标且更优（96 ≥ 90，核心功能完整度满分，上架成熟度满分，flaky 已根治，剩 release 真机验证 caveat），但未跑满 20 轮，继续迭代
- 下一动作：迭代 11 Discover，候选目标搜索（核心功能完整度已满分，加分空间转移到代码质量/文档可维护性）/ 文档历史 spec 清理(+0~1) / backlog nit 清理 / UI 测试（需真机，验证门不覆盖）

---

## 迭代 11 (2026-06-28) — V3 搜索（events.title/note + notes.content 全文检索，LIKE 内存过滤）

### Discover ✅
- 推荐本轮目标：**V3 搜索（events.title/note + notes.content 全文检索，LIKE 内存过滤）**，预估加分 +1（文档可维护性 6→7，核心功能完整度已满分）
- 价值理由：用户价值真（记录即审视，能从历史找事/随笔，8 轮迭代后数据已可累积，无搜索是真实痛点）；符合 10 轮规划（11 搜索）；复用模式多（TimelineItem/EventCard/NoteRow/RecordingSheet/NoteEditorSheet）；零联网零新依赖零 schema 变更
- 不优先攻文档/backlog nit 的理由：单做太轻，搜索是 M 工作量有实质价值
- 不优先攻 UI 测试的理由：需真机/模拟器，验证门不覆盖，违背"全自动到失败为止"
- 发现 dead code：`TimelineScreen.kt` 三个 private 函数未使用（nit，本轮不动）
- **orchestrator 拍板 7 个决策点（全部采纳推荐）**：
  1. 搜索入口位置：选项 A（首页顶栏 26dp 放大镜像素方块，与热力图/设置同风格）
  2. 搜索范围：选项 A（events.title + events.note + notes.content 三字段全覆盖）
  3. 检索方式：选项 A（LIKE 内存过滤，不用 FTS4，中文分词复杂度无收益）
  4. 结果展示与点击：选项 A（独立 SearchScreen + 复用 EventCard/NoteRow + 点击复用 RecordingSheet/NoteEditorSheet 编辑）
  5. 匹配高亮：选项 A（不高亮，8-bit 简单为上，先做基础版）
  6. 空查询行为：选项 A（空查询显示最近 N 条，与 NotesScreen "所有随笔"模式一致，N=50）
  7. 是否组合文档打磨：选项 A（单独做搜索，避免范围蔓延）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-search-design.md`
- 关键设计决策：
  1. 数据层零改动（复用 EventRepository.getAllEvents / NoteRepository.getAllNotes 两个既有 Flow，不加 DAO 查询）
  2. SearchViewModel 用 `combine(eventsFlow, notesFlow, _query)` 三流合并（各自 stateIn(WhileSubscribed(5000)) 缓存上游，DB 变化自动驱动刷新）
  3. `filterAndMerge` 提为 `internal` 纯函数（与 aggregateMonth 同模式，无 Android 依赖，纯 JUnit 可测）
  4. `SearchItem` sealed interface 镜像 `TimelineItem`（sortKey + EventItem/NoteItem，独立定义因 TimelineItem 是 private）
  5. SearchScreen 复用 EventCard + NoteRow（NoteRow 需去 private 改 public，TimelineScreen 唯一结构性改动）
  6. 独立 Screen 模式（Routes.SEARCH + composable，与 NotesScreen/HeatmapScreen 路由一致）
  7. 入口位置：TimelineScreen 顶栏热力图方块与设置方块之间插入 26dp 放大镜像素方块
  8. 测试复用迭代 9 方案 A：SearchViewModelTest（Robolectric 13 用例）+ SearchFilterTest（纯 JUnit 10 用例）
- 涉及文件：新增 4（SearchViewModel/SearchScreen/SearchViewModelTest/SearchFilterTest）+ 改 2（TimelineScreen/AppNavHost）+ 复核无改动 11 类
- 一处需 Coding 注意：RecordingSheet viewingDate 占位值 + `if (showEventSheet && editingEvent != null)` 守卫杜绝 initNew() 误创建（spec §八 R3）
- 无新增 orchestrator 决策点（沿用已拍板 7 项，N 具体化为 RECENT_LIMIT=50）

### Coding ✅
- 新增 4 文件 + 改动 2 文件，23 新测试（SearchFilterTest 10 + SearchViewModelTest 13），全工程 165 测试
- 四新增文件落地：
  - `SearchViewModel.kt`：@HiltViewModel + 注入 EventRepository + NoteRepository + `_query` MutableStateFlow + `state` = combine 三流 + `filterAndMerge` internal 纯函数 + `SearchItem` sealed interface + `SearchUiState` + RECENT_LIMIT=50
  - `SearchScreen.kt`：彩虹条 + 顶栏返回 + 2dp 黑边白底直角搜索框 + 清除×按钮 + LazyColumn 结果（复用 EventCard/NoteRow）+ 空状态 + RecordingSheet/NoteEditorSheet overlay（§八 R3 守卫）
  - `SearchFilterTest.kt`：纯 JUnit 10 用例（filterAndMerge 纯函数：空查询最近 N / 标题匹配 / 备注匹配 / 随笔匹配 / 多字段匹配 / 大小写忽略 / 无结果空状态 / 时间倒序 / 特殊字符 / RECENT_LIMIT 边界）
  - `SearchViewModelTest.kt`：Robolectric 方案 A 13 用例（StandardTestDispatcher + Room executor 路由 + first{} + backgroundScope collector）
- 两改动文件落地：
  - `TimelineScreen.kt`：+`onSearchClick: () -> Unit` 参数（**无默认值**，与 onYearClick 同款硬约束）+ 顶栏热力图方块与设置方块之间插入 26dp 放大镜像素方块（Icons.Default.Search 14dp 黑色）+ `NoteRow` 去 `private` 改 public
  - `AppNavHost.kt`：+`Routes.SEARCH = "search"` + `composable(Routes.SEARCH) { SearchScreen(...) }` + TimelineScreen 调用处加 `onSearchClick`
- Coding 自评：四道门顺序跑全绿（门1 3s / 门2 18s 165 tests 0 failures / 门3 1s / 门4 59s APK 11.8M），零回归（TimelineViewModel/HeatmapViewModel/NotesViewModel/RecordingViewModel/DB schema/既有测试全部零改动），复用模式到位（filterAndMerge/SearchItem/方案 A/EventCard+NoteRow/RecordingSheet+NoteEditorSheet 五重复用）
- 偏离 spec：spec §4.4.1 写 `onSearchClick = {}` 有默认值，Coding 按任务描述实现为无默认值（与 onYearClick 一致，编译期安全 > 风格统一）
- 待 Test subagent 独立复核（不信任 Coding 自评，特别验证零回归 + 复用模式 + R3 守卫）

### Test ✅ PASS
- 独立重跑四道门（顺序，强制 `--rerun-tasks` 排除假绿）全绿：
  - 门1 `compileDebugKotlin` 580ms ✅
  - 门2 `testDebugUnitTest --rerun-tasks` 18s ✅ 165 测试 0 失败 0 @Ignore（XML 报告逐 suite 核实：13+8+4+14+22+15+4+5+8+5+20+7+7+10 + 新增 10+13 = 165）
  - 门3 `assembleDebug` 1s ✅
  - 门4 `assembleRelease --rerun-tasks` 59s ✅ APK 11.8M
- Coding 自评全部核实属实：
  - 零回归：TimelineViewModel/HeatmapViewModel/NotesViewModel/RecordingViewModel grep `search` 无匹配；AppDatabase version=2 未变；EventDao/NoteDao 18 条 @Query 无 LIKE 搜索查询；既有 14 个测试 suite 全过且用例数不变
  - 新增 4 + 改 2 文件核实无误
  - 23 新增 / 165 总数核实无误
  - 复用模式全部落实：filterAndMerge internal 纯函数 / SearchItem 镜像 TimelineItem / SearchViewModelTest 逐字复用方案 A / EventCard+NoteRow 真复用（NoteRow 已去 private）/ RecordingSheet+NoteEditorSheet 复用既有模式
  - 8-bit 美学：搜索框 2dp 黑边白底直角 + 放大镜 26dp 像素方块 + 配色全用 AppColors
  - R3 守卫：`if (showEventSheet && editingEvent != null)` 双条件到位
- spec §4.1-§4.5 全节覆盖，逐项有代码实证
- 硬约束 4 项全 pass：不联网（grep 无网络 import）/ 零新依赖（build.gradle.kts 零改动）/ 不改 DB schema（version=2）/ 不改既有 ViewModel
- 偏离 spec 评估：onSearchClick 无默认值合理（任务描述授权 + 编译期安全 > spec 字面默认 {}）
- 仅 2 个非阻塞问题（记 backlog）：
  - TimelineScreen 顶栏回调参数风格不统一（onSearchClick + onYearClick 无默认，其余有默认 {}）—— 编译期安全选择，可后续统一
  - spec §4.4.1 字面与实现偏离，建议后续 spec 修订同步

### Persist ✅
- 质量评分：96 → **97**（+1，文档可维护性 6→7），详见 `quality-scorecard.md`
  - 文档可维护性 +1 来源：spec 落盘（search-design.md）+ AGENT.md Spec 索引补 iter9/10/11 三条 + 项目结构补 feature/search/ + heatmap 行补年视图 + recording 行括号顺手修齐
- backlog 新增：TimelineScreen 顶栏回调参数风格不统一 / spec §4.4.1 字面偏离
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-11 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：V3 搜索完整实现（events.title/note + notes.content 三字段全文检索，LIKE 内存过滤，独立 SearchScreen + 复用 EventCard/NoteRow + 复用 RecordingSheet/NoteEditorSheet 编辑，23 新测试，四道门全绿），零回归（既有 ViewModel/DB schema/既有测试全部零改动），复用模式到位（filterAndMerge/SearchItem/方案 A/EventCard+NoteRow/RecordingSheet+NoteEditorSheet 五重复用）
- **里程碑**：**V3 搜索落地** —— 用户可从历史找事/随笔，"记录即审视"理念补齐检索维度
- **$100 质量线判定**：✅ 稳固达标且更优（97 ≥ 90 + 核心功能完整度满分 + 上架成熟度满分 + 无 blocker + 四道门绿含 release + 文档可维护性 +1）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线且更优，但未跑满 20 轮迭代（当前 11 轮），**继续迭代**。后续轮次重点：迭代12 文档历史 spec 清理 / 迭代13-19 灵活（backlog nit / UI 测试需真机 / dead code 清理 / 其他打磨）/ 迭代20 最终验收

---

## 当前 loop 状态

- 进行中迭代：12（待启动）
- 已完成迭代：11
- 累计质量提升：62 → 97（+35，迭代11 搜索补文档可维护性 +1）
- **$100 质量线**：✅ 稳固达标且更优（97 ≥ 90，核心功能完整度满分，上架成熟度满分，文档可维护性 7/10，flaky 已根治，剩 release 真机验证 caveat），但未跑满 20 轮，继续迭代
- 下一动作：迭代 12 Discover，候选目标文档历史 spec 清理(+0~1) / backlog nit 清理（M1-legacy/B2-legacy/窄屏/顶栏回调风格不统一/spec 字面偏离）/ dead code 清理（TimelineScreen 三 private 函数）/ UI 测试（需真机，验证门不覆盖）

---

## 迭代 12 (2026-06-28) — 文档打磨 + dead code 清理 + 顶栏回调统一 + spec 字面修正

### Discover ✅
- 推荐本轮目标：**文档打磨（主）+ dead code 清理 + 顶栏回调风格统一 + spec 字面偏离修正（组合，全部 S）**，预估加分 +0~1（文档可维护性 7→8）
- 文档过时表述实证：design doc feature 列表缺 notes/search + heatmap 仅写月视图未提年视图；V3 分期未反映数据导入/年视图/搜索已落地；AGENT.md V3 描述未提数据导入/搜索/年视图
- backlog nit 实证：dead code 三个 private 函数零调用 / spec §4.4.1 onSearchClick `= {}` 偏离 / 顶栏回调风格不统一（onSearchClick + onYearClick 无默认，其余 4 个有 `= {}`）
- 不做 M1-legacy/B2-legacy/窄屏的理由：M1 中国不受影响无旧用户 / B2 触及 flaky 稳定区风险 > 收益 / 窄屏需真机回归
- **orchestrator 拍板 6 个决策点**：
  1. 顶栏回调统一方向：**选项 B**（去掉其余 4 个默认值，全无默认值，与 onYearClick/onSearchClick 既定方向一致）—— 偏离 Discover 推荐 A，理由：编译期安全最大化 > 最小改动，与既定方向一致
  2. dead code 删除：选项 A 删除（采纳推荐）
  3. spec §4.4.1 修正：选项 A 改 spec 去 `= {}` 对齐实现（与决策 1 联动）
  4. M1-legacy 不做：采纳推荐
  5. B2-legacy 不做：采纳推荐
  6. 窄屏不做：采纳推荐

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-docs-cleanup-nit-fix-design.md`（85 行，S 工作量）
- 涉及文件：改 3（design doc / AGENT.md / TimelineScreen.kt）+ spec 修正 1（search-design.md）
- 关键确认：dead code 三个函数 grep 零调用，`formatDateCompact` 被 NoteRow 调用保留（注：Test 复核发现实际被顶栏日期徽章调用，spec 文字小偏差不影响功能）；TimelineScreen 唯一调用方 AppNavHost，去默认值安全
- 无新增 orchestrator 决策点

### Coding ✅
- 改 3 文件 + spec 修正 1 文件，无新测试，全工程 165 测试不变
- 改动落地：
  - `docs/2026-06-22-shijiben-design.md`：feature 列表补 notes/search + heatmap 补年视图；V3 分期补数据导入/年视图/搜索三项 `[x]` 已落地
  - `AGENT.md`：V3 描述补「数据导入 + 年视图 + 搜索」已落地
  - `TimelineScreen.kt`：删 3 dead code private 函数（formatDate/hourOfDay/isPastDay，保留 formatDateCompact + isToday）+ 4 顶栏回调去 `= {}` 默认值（onNotesClick/onTimeVizClick/onHeatmapClick/onSettingsClick，与 onSearchClick/onYearClick 一致全无默认值）
  - `2026-06-28-search-design.md`：§4.4.1 onSearchClick 去 `= {}` 对齐实现
- Coding 自评：四道门顺序跑全绿（门1 3s / 门2 19s 165 tests / 门3 1s / 门4 58s APK 11.8M），零回归（ViewModel/DB schema/既有测试零改动），dead code 真零调用（DayProgressBar.kt 有自己的 private 副本不受影响），回调统一 AppNavHost 零调整
- 待 Test subagent 独立复核

### Test ✅ PASS
- 独立重跑四道门（顺序，门2/门4 强制 --rerun-tasks）全绿：
  - 门1 `compileDebugKotlin` 725ms ✅
  - 门2 `testDebugUnitTest --rerun-tasks` 18s ✅ 165 测试 0 失败 0 @Ignore（16 个 TEST-*.xml 汇总确认）
  - 门3 `assembleDebug` 978ms ✅
  - 门4 `assembleRelease --rerun-tasks` 59s ✅ APK 11.8M
- Coding 自评全部核实属实：
  - dead code 删除：grep 确认三函数已删，formatDateCompact/isToday 保留，DayProgressBar.kt 副本不受影响
  - 回调统一：TimelineScreen L97-101 顶栏 5 回调全无 `= {}`；AppNavHost L52-56 调用处全传零调整
  - 文档对齐：design doc feature 列表 + V3 分期补齐；AGENT.md V3 描述补齐
  - spec 修正：search-design.md §4.4.1 onSearchClick 无 `= {}`
  - 零回归：165 测试全过，ViewModel/DB/依赖零改动
- spec §4.1-§4.4 全节覆盖
- 硬约束 4 项全 pass
- 仅 1 条非阻断 note：spec §4.2 原文称 formatDateCompact 被 NoteRow 调用，实际被顶栏日期徽章调用，spec 文字小偏差不影响功能

### Persist ✅
- 质量评分：97 → **98**（+1，文档可维护性 7→8），详见 `quality-scorecard.md`
  - 文档可维护性 +1 来源：design doc 对齐（feature 列表 + V3 分期）+ AGENT.md V3 描述对齐 + spec 字面修正 + 4 条 backlog 闭合（dead code / 顶栏回调风格 / spec 偏离 / 文档过时）
- backlog 闭合 4 条：dead code / 顶栏回调风格 / spec 字面偏离 / 文档过时表述
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-12 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：文档打磨（design doc + AGENT.md 对齐）+ dead code 清理（3 函数删除）+ 顶栏回调统一（4 回调去默认值，全无默认值）+ spec 字面修正（search-design.md §4.4.1）四项组合全落地，四道门全绿，零回归
- **里程碑**：**4 条 backlog 闭合** —— 文档与代码完全一致，dead code 清零，回调风格统一，spec 与实现对齐
- **$100 质量线判定**：✅ 稳固达标且更优（98 ≥ 90 + 核心功能完整度满分 + 上架成熟度满分 + 文档可维护性 8/10 + 无 blocker + 四道门绿含 release）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线且更优，但未跑满 20 轮迭代（当前 12 轮），**继续迭代**。后续轮次重点：迭代13-19 灵活（剩余 backlog nit M1-legacy/B2-legacy/窄屏 评估是否做 / UI 测试需真机 / 其他打磨）/ 迭代20 最终验收

---

## 当前 loop 状态

- 进行中迭代：13（待启动）
- 已完成迭代：12
- 累计质量提升：62 → 98（+36，迭代12 文档打磨 + 4 backlog 闭合补文档可维护性 +1）
- **$100 质量线**：✅ 稳固达标且更优（98 ≥ 90，核心功能完整度满分，上架成熟度满分，文档可维护性 8/10，flaky 已根治，剩 release 真机验证 caveat），但未跑满 20 轮，继续迭代
- 下一动作：迭代 13 Discover，候选目标剩余 backlog nit 评估（M1-legacy/B2-legacy/窄屏，均评估为不做但需复核）/ UI 测试（需真机，验证门不覆盖）/ 其他打磨 / 或开始最终验收准备

---

## 迭代 13 (2026-06-28) — README.md 新增 + AGENT.md 调试技巧/FAQ 补充

### Discover ✅
- **关键发现**：项目根目录完全没有 `README.md` —— 对 98/100、上架成熟度满分的公开仓库是真实可见的文档缺口
- 推荐本轮目标：**新增 README.md（主）+ AGENT.md 补充调试技巧/FAQ（次）组合**，预估加分 +1~2（文档可维护性 8→9~10）
- 价值理由：README.md 是项目根目录标志性缺口，GitHub 仓库首页无渲染；AGENT.md 已完整但可补调试/FAQ；纯文档零代码风险
- 不攻剩余 backlog nit 的理由：M1/B2/窄屏均评估为不做（M1 中国不受影响 / B2 触及 flaky 稳定区 / 窄屏需真机）
- 不攻代码质量改进的理由：已满分，加分空间无
- 不攻最终验收准备的理由：版本号已闭合 / 上架清单已满分 / CHANGELOG 即 scorecard
- **orchestrator 拍板 3 个决策点**：
  1. README.md 内容范围：包含项目简介/功能列表/技术栈/项目结构指向/构建说明指向/使用指南/隐私声明；**不加 License**（付费上架非开源）/ **不加截图占位**（无素材）/ **不加 Contributing**（个人项目）
  2. README.md 与 AGENT.md 内容分工：选项 A（README 精简面向用户，指向 AGENT.md 看开发者内容，避免重复维护）
  3. AGENT.md 补充范围：选项 B（调试技巧段 + 常见问题 FAQ 段）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-readme-and-agent-faq-design.md`
- 涉及文件：新增 1（README.md）+ 改 1（AGENT.md）
- 关键确认：AndroidManifest.xml 零权限声明（README 隐私声明可强引用）/ build.gradle.kts 确认 SDK 版本号 / AGENT.md 现有结构确认插入位置（构建与安装后、分期前）
- 无新增 orchestrator 决策点

### Coding ✅
- 新增 1 文件 + 改动 1 文件，无新测试，全工程 165 测试不变
- 新增 `README.md`（项目根）：七章结构——项目简介（受《奇特的一生》启发 + 8-bit 像素美学 + 纯本地）/ 功能列表（10 项：事件CRUD/状态流转/顺延/随笔/时间轴/时间可视化/热力图月+年/搜索/导出导入/设置）/ 技术栈（Kotlin + Compose BOM 2024.10.01 + Room 2.6.1 + Hilt 2.52 + Coroutines/Flow，AGP 8.7.3 / Kotlin 2.0.21 / JVM 17 / minSdk 26 / targetSdk 34 / compileSdk 34 / versionName 1.0）/ 项目结构（精简 + 指向 AGENT.md）/ 构建说明（debug 命令 + 指向 AGENT.md）/ 使用指南（7 项基础流程）/ 隐私声明（零网络/零权限/数据仅本机/导出用户掌控，强引用 AndroidManifest 零 `<uses-permission>`）
- 改动 `AGENT.md`：在「构建与安装」节后、「分期」节前插入两节——调试技巧（adb 查 DB / logcat 过滤 / 清单查看）+ 常见问题 FAQ（keystore 缺失 / flaky 复发 / 重置生日 / daemon 卡住），引用既有 spec/backlog 不重写
- Coding 自评：四道门顺序跑全绿（门1 751ms / 门2 18s 165 tests / 门3 1s / 门4 APK 11.78M），零回归（纯文档改动），README 面向人类七章完整，AGENT.md 插入位置正确，内容不重复
- 待 Test subagent 独立复核

### Test ✅ PASS WITH NOTES
- 独立重跑四道门（顺序，门2/门4 强制 --rerun-tasks）全绿：
  - 门1 `compileDebugKotlin` 751ms ✅
  - 门2 `testDebugUnitTest --rerun-tasks` 20s ✅ 165 测试 0 失败 0 @Ignore（16 个测试类 XML 累加核实）
  - 门3 `assembleDebug` 1s ✅
  - 门4 `assembleRelease --rerun-tasks` 58s ✅ APK 11.78M
- Coding 自评全部核实属实：
  - README.md 七章结构完整（L1-65），面向人类（无迭代/scorecard 细节），技术栈准确（核对 build.gradle.kts），隐私声明准确（AndroidManifest 零权限），无 License/截图/Contributing
  - AGENT.md 插入位置正确（构建与安装 L84 → 调试技巧 L135 → FAQ L168 → 分期 L182 → Spec 索引 L188 文末未受影响）
  - 调试技巧三类齐全（adb 查 DB / logcat 过滤 / 清单查看）
  - FAQ 四问齐全（keystore 缺失 / flaky 复发 / 重置生日 / daemon 卡住）
  - 内容不重复（README 项目结构/构建说明用精简 + 指向 AGENT.md 模式）
  - 零回归：本次迭代仅文档改动，src/main/java 零改动
- spec §4.1-§4.2 全节覆盖
- 硬约束 4 项全 pass
- 2 个非阻断 notes：
  - git diff 口径差异：git diff 显示 16 个文件是迭代 1-13 累积未提交改动，非本次迭代增量（进程问题迭代7 起持续）
  - 门4 strip 警告：libandroidx.graphics.path.so 无法 strip，非本次引入，构建 SUCCESSFUL

### Persist ✅
- 质量评分：98 → **99**（+1，文档可维护性 8→9），详见 `quality-scorecard.md`
  - 文档可维护性 +1 来源：README.md 新增（面向人类入口文档补齐）+ AGENT.md 补调试技巧/FAQ（开发者文档完善）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-13 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：README.md 新增（七章面向人类入口文档）+ AGENT.md 补调试技巧/FAQ（开发者文档完善），四道门全绿，零回归
- **里程碑**：**面向人类的入口文档补齐** —— GitHub 仓库首页有渲染，项目对外可见性恢复，开发者调试/FAQ 文档完善
- **$100 质量线判定**：✅ 稳固达标且更优（99 ≥ 90 + 核心功能完整度满分 + 上架成熟度满分 + 文档可维护性 9/10 + 无 blocker + 四道门绿含 release）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线且更优，但未跑满 20 轮迭代（当前 13 轮），**继续迭代**。后续轮次重点：迭代14-19 灵活（文档可维护性剩 1 分空间 / 代码质量防御性 / 最终验收准备）/ 迭代20 最终验收

---

## 当前 loop 状态

- 进行中迭代：14（待启动）
- 已完成迭代：13
- 累计质量提升：62 → 99（+37，迭代13 README + AGENT.md 调试/FAQ 补文档可维护性 +1）
- **$100 质量线**：✅ 稳固达标且更优（99 ≥ 90，核心功能完整度满分，上架成熟度满分，文档可维护性 9/10，flaky 已根治，剩 release 真机验证 caveat），但未跑满 20 轮，继续迭代
- 下一动作：迭代 14 Discover，候选目标文档可维护性最后 1 分（如架构文档/用户手册）/ 代码质量防御性改进（边界测试/注释）/ 最终验收准备 / 其他遗漏改进点

---

## 迭代 14 (2026-06-28) — ARCHITECTURE.md 新增（4 视角统一描述）+ AGENT.md 加链接

### Discover ✅
- 推荐本轮目标：**新增架构文档 `docs/ARCHITECTURE.md`**，统一描述 4 个视角（模块依赖图 + 数据流 + 状态管理 + 导航图），预估加分 +1（文档可维护性 9→10，总分 99→100）
- 价值理由：项目缺一份统一架构文档（AGENT.md 是给 AI agent 的项目上下文，README.md 是面向人类的入口，但都没有完整架构视图）；新人/onboarding/AI agent 理解整体架构需读散落各处的代码；纯文档零代码风险
- 不攻剩余 backlog nit 的理由：M1/B2/窄屏均评估为不做（M1 中国不受影响 / B2 触及 flaky 稳定区 / 窄屏需真机）
- 不攻代码质量改进的理由：已满分，加分空间无
- **orchestrator 拍板 4 个决策点（全部采纳推荐选项 A）**：
  1. 架构文档位置：选项 A = `docs/ARCHITECTURE.md`（独立成文）
  2. AGENT.md 加链接：选项 A = 加一行链接（保证可发现性）
  3. 图示方式：选项 A = ASCII art（零依赖纯文本，不用 Mermaid）
  4. 范围：选项 A = 单独做架构文档（避免范围蔓延）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-architecture-doc-design.md`
- Design subagent 实读代码后修正 4 处任务描述与实际代码不符的事实（spec 已按实际代码写）：
  1. 实际有 3 个 Hilt Module（DataModule + DispatchersModule + TimeVizModule），非 2 个
  2. 仅有 @IoDispatcher 一个 qualifier，无 @DefaultDispatcher
  3. getDailyActivityForMonth(YearMonth) / getDailyActivityForYear(Year) 返回 Flow<List<DailyActivity>>，非 Map
  4. HeatmapCalculator.Cell 是 data class（level: Int 0..4），非 sealed interface；sealed interface 清单为 TimelineItem / SearchItem / ExportState / ImportState
- 涉及文件：新增 1（docs/ARCHITECTURE.md）+ 改 1（AGENT.md 加 1 行 blockquote 链接）
- ARCHITECTURE.md 7 节结构：概述 / 模块依赖图 / 数据流 / 状态管理 / 导航图 / 依赖注入（Hilt）/ 关键设计模式
- 无新增 orchestrator 决策点

### Coding ✅
- 新增 1 文件 + 改动 1 文件，无新测试，全工程 165 测试不变
- 新增 `docs/ARCHITECTURE.md`（182 行，7 节，中文，ASCII art 代码块包裹）：
  - §1 概述：MVVM + Repository + 单向数据流 + Hilt + Compose，纯本地无联网
  - §2 模块依赖图：ASCII art 五大块单向依赖（data ← feature ← navigation ← MainActivity，ui/theme 横切，di 横切，ShiJiBenApplication @HiltAndroidApp 入口）
  - §3 数据流：ASCII art Room→DAO→Repository→ViewModel→Compose + Room schema 说明（version=2，两表 events+notes）
  - §4 状态管理：StateFlow/WhileSubscribed(5000)/flatMapLatest/combine/sealed state（TimelineItem/SearchItem/ExportState/ImportState）/纯函数（aggregateMonth/aggregateYear/effectiveDurationMs/filterAndMerge/HeatmapCalculator/TimeVizCalculator）
  - §5 导航图：ASCII art 8 路由 + 跳转关系 + savedStateHandle 特殊链路（HEATMAP 点日期 popBackStack 回 TIMELINE 带 Triple<Int,Int,Int>）
  - §6 依赖注入（Hilt）：@HiltAndroidApp + 3 Module + @IoDispatcher
  - §7 关键设计模式：Repository/纯函数/sealed/8-bit 美学集中/flaky 根治方案 A/DB 迁移历史
- 改动 `AGENT.md`：在「项目概述」节末尾（「事记本是一个纯本地的时间记录应用……」段落之后）、「## 技术栈」之前，加 1 行 blockquote 链接：`> 架构详情见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)（模块依赖图 + 数据流 + 状态管理 + 导航图 4 视角）。`
- Coding 自评：四道门顺序跑全绿（门1 947ms / 门2 23s 165 tests 0 failures / 门3 1s / 门4 1m7s APK 11.8M），零回归（纯文档改动，.kt / build.gradle.kts / AndroidManifest.xml 全零改动）
- 二次实读代码核对：18 文件 + 4 Grep，spec 与代码完全一致
- 一处文档措辞微调（非偏差）：getDailyActivityForMonth(YearMonth) 单参数，与代码一致
- 待 Test subagent 独立复核

### Test ✅ PASS WITH NOTES
- 独立重跑四道门（顺序，门2/门4 强制 --rerun-tasks）全绿：
  - 门1 `compileDebugKotlin` 878ms ✅
  - 门2 `testDebugUnitTest --rerun-tasks` 20s ✅ 165 测试 0 失败 0 @Ignore（16 个 XML 汇总）
  - 门3 `assembleDebug` 1s ✅ debug APK 20M
  - 门4 `assembleRelease --rerun-tasks` 1m6s ✅ release APK 11.8M
- 零代码改动双重确认（mtime + git diff）：16:00 后仅 6 文件改动，全部为文档（ARCHITECTURE.md 新增 + AGENT.md 改 1 行 + loop 自维护 3 文件 + 邻近 spec 1 文件）；.kt / .xml / .kts / .pro 全零改动
- ARCHITECTURE.md 4 视角独立实读代码核对（~16 文件 + 3 Grep）全部与代码一致：
  - 视角1 模块依赖图：Glob 全包路径逐条一致 + Grep `@Module` 命中 3 处
  - 视角2 数据流：EventDao/NoteDao @Query 返回 Flow + Repository 透传 + getDailyActivityForMonth(YearMonth)/getDailyActivityForYear(Year) 返回 Flow<List<DailyActivity>> + TimelineViewModel combine+flatMapLatest+stateIn(WhileSubscribed(5000)) + SearchViewModel combine 三流 + Export/ImportViewModel MutableStateFlow+first()
  - 视角3 状态管理：Grep `sealed interface` 命中 4 处（TimelineItem/SearchItem/ExportState/ImportState）+ HeatmapCalculator.Cell 是 data class + 纯函数清单全 internal 顶层 + 仅 @IoDispatcher 一个 qualifier
  - 视角4 导航图：Routes object 8 常量逐字核对 + startDestination=TIMELINE + 跳转关系 + savedStateHandle["heatmap_target_date"]=Triple<Int,Int,Int>
- AGENT.md 链接核对：位置（项目概述末尾，技术栈前）+ 格式（blockquote）+ 相对路径（docs/ARCHITECTURE.md）全正确
- 硬约束 4 项全 pass：不联网（Grep 无网络库 + AndroidManifest 零 uses-permission）/ 不引入新依赖（build.gradle.kts 零改动）/ 四道门全绿 / Coding≠Test
- spec §4.1/§4.2/§4.3 + 4 个 orchestrator 决策点全 A 落地
- Coding 自评逐项核实全部属实，未发现 self-preferential bias 或造假
- 3 个 Note（合理偏离/正常波动，非问题）：
  1. ARCHITECTURE.md §3 数据流 ViewModel 清单未列 TimeVizViewModel/RecordingViewModel（合理简化，二者为非 DB 驱动的同步 MutableStateFlow，不参与 Room→DAO→Repo→VM 管线）
  2. ARCHITECTURE.md §7 关键设计模式有 6 项（多了「DB 迁移历史」），spec §4.1.4 列 5 项，spec §4.1.5 明确允许 Room schema「可并入数据流或单列」，属 spec 许可范围内合理增强
  3. 四道门耗时与 Coding 报告有秒级波动（正常构建波动）

### Persist ✅
- 质量评分：99 → **100**（+1，文档可维护性 9→10 满分），详见 `quality-scorecard.md`
  - 文档可维护性 +1 来源：ARCHITECTURE.md 新增（4 视角统一描述，模块依赖图+数据流+状态管理+导航图，ASCII art）+ AGENT.md 加链接保证可发现性
  - 所有维度满分（核心功能完整度 40 + 产品愿景达成度 15 + 代码质量 20 + 上架成熟度 15 + 文档可维护性 10 = 100）
- backlog 无新增（Test 3 个 Note 均为合理偏离/正常波动，非问题）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-14 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：ARCHITECTURE.md 新增（4 视角统一描述，182 行，ASCII art）+ AGENT.md 加链接保证可发现性，四道门全绿，零代码改动，4 视角经 Test 独立实读代码核对与代码完全一致
- **里程碑**：**文档可维护性满分**（9→10）—— 总分 99→100 满分，所有维度满分。项目文档体系完整（README.md 面向人类入口 + AGENT.md 面向 AI agent 上下文 + ARCHITECTURE.md 4 视角架构 + specs/ 设计历史 + plans/ 计划历史 + loop/ 迭代日志与评分）
- **$100 质量线判定**：✅ **满分稳固达标**（100/100，所有维度满分 + 无 blocker + 四道门绿含 release + 文档与代码完全一致）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线满分，但未跑满 20 轮迭代（当前 14 轮），**继续迭代**。后续轮次重点：迭代15-19 灵活（剩余 backlog nit M1-legacy/B2-legacy/窄屏 评估是否做 / UI 测试需真机 / 其他打磨 / 防御性改进）/ 迭代20 最终验收

---

## 当前 loop 状态

- 进行中迭代：15（待启动）
- 已完成迭代：14
- 累计质量提升：62 → 100（+38，迭代14 ARCHITECTURE.md 补文档可维护性最后 1 分到满分）
- **$100 质量线**：✅ **满分稳固达标**（100/100，所有维度满分，flaky 已根治，剩 release 真机验证 caveat），但未跑满 20 轮，继续迭代
- 下一动作：迭代 15 Discover，候选目标剩余 backlog nit 评估（M1-legacy/B2-legacy/窄屏，均评估为不做但需复核）/ UI 测试（需真机，验证门不覆盖）/ 防御性改进（边界测试/注释）/ 其他打磨 / 最终验收准备

---

## 迭代 15 (2026-06-28) — NotesViewModel 测试补齐 + colors.xml 死色清理

### Discover ✅
- 推荐本轮目标：**补齐 NotesViewModel 测试覆盖（主）+ colors.xml 死资源清理（次）**，预估加分 0（已达 100/100 满分，过程价值在防回归）
- 价值理由：NotesViewModel 是全项目唯一零测试覆盖的 ViewModel（7 个 ViewModel 中最后一个缺口）；colors.xml 含 7 个 AS 模板默认死色（purple_200/500/700, teal_200/700, black, white）全项目零引用
- 不攻剩余 backlog nit 的理由：M1-legacy（中国不受影响无旧用户）/ B2-legacy（触及 flaky 稳定区风险 > 价值）/ 窄屏（需真机验证违背全自动）
- 不攻 release-verify/splash 手测的理由：需真机，超出自动化边界
- ProGuard 规则复核：完整最小，无缺口
- **orchestrator 拍板 3 个决策点（全部采纳推荐）**：
  1. 本轮目标范围：候选 A（NotesViewModel 测试）+ 候选 B（colors.xml 清理）
  2. NotesViewModel 测试范围：核心状态机 5-6 用例
  3. colors.xml 清理策略：删 7 死色保留空 resources（Coding 可升级为删整个文件）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-notes-vm-test-and-colors-cleanup-design.md`
- Design subagent 实读 NotesViewModel.kt 确认：
  - @HiltViewModel 注入 NoteRepository
  - `allNotes`: stateIn(WhileSubscribed(5000))（需常驻收集者 + first{}）
  - `editing` / `sheetOpen`: asStateFlow()（直读 .value）
  - 5 方法：startCreate / startEdit / closeSheet / suspend save(content): Boolean / suspend delete(note)
  - save 双分支：空内容早返回 false / editing 非空 updateNote / 否则 createNote
- 6 测试用例：startCreate_opensSheetAndEditingNull / startEdit_opensSheetAndEditingSet / closeSheet_closesSheetAndClearsEditing / save_newNote_persistsAndClosesSheet / save_editExisting_updatesAndClosesSheet / delete_removesFromDbAndClosesSheet
- colors.xml 实读确认仅含 7 死色，**决策点 3 升级为删整个文件**（任务描述已授权）
- 涉及文件：新增 1（NotesViewModelTest.kt）+ 删 1（colors.xml）
- 无新增 orchestrator 决策点

### Coding ✅
- 新增 1 测试文件 + 删除 1 资源文件，6 新测试，全工程 171 测试（165 + 6）
- 新增 `app/src/test/java/com/shijiben/feature/notes/NotesViewModelTest.kt`：
  - 方案 A 逐字复用（StandardTestDispatcher + roomExecutor 适配器 + setQueryExecutor + setTransactionExecutor + allowMainThreadQueries + backgroundScope.launch{collect{}} + first{}）
  - allNotes 用常驻收集者 + first{} 等待 Room Flow 落定
  - editing/sheetOpen 直读 .value（asStateFlow）
  - 6 用例覆盖核心状态机 + save 双分支 + 空内容早返回 + delete 落库
  - 用例 5/6 用 createNote 拿 id + getNoteById 取回完整实体喂给 startEdit/delete（避免 id=0 不匹配）
- 删除 `app/src/main/res/values/colors.xml`（整个文件，仅含 7 死色，Grep 全项目零引用）
- Coding 自评：四道门顺序跑全绿（门1 8s / 门2 21s 171 tests 0 failures / 门3 1s APK 19M / 门4 1m20s APK 12M），零生产代码改动
- 二次实读代码核对：NotesViewModel.kt / NoteRepository.kt / NoteDao.kt / NoteEntity.kt / MainCoroutineRule.kt 全部与 spec 一致
- 待 Test subagent 独立复核

### Test ✅ PASS
- 独立重跑四道门（顺序，门2/门4 强制 --rerun-tasks）全绿：
  - 门1 `compileDebugKotlin` 0.95s ✅（UP-TO-DATE）
  - 门2 `testDebugUnitTest --rerun-tasks` 25s ✅ 171 测试 0 失败 0 @Ignore（17 个 XML 汇总）
  - 门3 `assembleDebug` 1s ✅ debug APK 19.0M
  - 门4 `assembleRelease --rerun-tasks` 1m20s ✅ release APK 11.8M
- 零生产代码改动双重确认（mtime + 内容抽样）：NotesViewModel.kt / NoteRepository.kt / NoteDao.kt / NoteEntity.kt / AppDatabase.kt 全部 mtime 早于 Coding 窗口，内容与 spec 一致
- NotesViewModelTest 独立核对：
  - 方案 A 复用：setup() 与 HeatmapViewModelTest / SearchViewModelTest 逐字对齐（StandardTestDispatcher + roomExecutor 适配器 + setQueryExecutor + setTransactionExecutor + allowMainThreadQueries + backgroundScope.launch{collect{}} + first{}）
  - 6 用例与 NotesViewModel 实际方法一致：5 方法存在 + allNotes stateIn 用常驻收集者 + editing/sheetOpen asStateFlow 直读 .value + save 三分支覆盖
  - 测试有效性：6 用例断言针对实际状态变化，无 tautology（用例 4 覆盖空内容早返回 false + 新建 true；用例 5 覆盖 updateNote；用例 6 真正删除并断言 DB 空）
- colors.xml 删除核对：Glob 确认文件已删 + Grep 7 死色零引用（@color/* / R.color.* / 色名三种 grep 全 No matches）+ AppColors.kt 不受影响
- 硬约束 4 项全 pass：不联网 / 不引入新依赖（build.gradle.kts 零改动）/ 四道门全绿 / Coding≠Test
- spec §4.1/§4.2/§4.3 + 3 个 orchestrator 决策点全落地
- Coding 自评逐项核实全部属实，未发现 self-preferential bias 或造假
- 3 个 Note（口径差异/正常方差，非问题）：门1 冷构建 8s vs 缓存 0.95s / 门2 21s vs 25s 正常方差 / release APK 12M vs 11.8M 四舍五入

### Persist ✅
- 质量评分：100 → **100**（+0，已达满分，过程价值在防回归），详见 `quality-scorecard.md`
  - 代码质量 20（满分保持）：NotesViewModel 测试盲区补齐（7 个 ViewModel 全部有测试）+ colors.xml 死色清理，171 测试覆盖
  - 其余维度不变
- backlog 无新增
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-15 大量未提交改动，强烈建议提交基线）

### 质量评估 ✅
- **本轮达成**：NotesViewModel 测试补齐（6 用例，复用方案 A，填补最后一个 ViewModel 测试盲区）+ colors.xml 死色清理（删 7 个 AS 模板默认色），四道门全绿，零生产代码改动
- **里程碑**：**7 个 ViewModel 全部有测试覆盖** —— 防御性巩固完成，测试矩阵无空洞。死资源清理减少 R.color 命名空间污染
- **$100 质量线判定**：✅ **满分稳固达标**（100/100，所有维度满分 + 无 blocker + 四道门绿含 release + 文档与代码完全一致 + 171 测试覆盖）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线满分，但未跑满 20 轮迭代（当前 15 轮），**继续迭代**。后续轮次重点：迭代16-19 灵活（剩余 backlog nit 评估 / 防御性测试加固 / 其他打磨）/ 迭代20 最终验收

---

## 迭代 16（2026-06-28）：KDoc 失准修正 + 死参数清理 + markError 错误路径测试

### Discover ✅
- 扫描发现 3 项防御性改进点（KDoc 失准 + 死参数 + markError 错误路径零覆盖），orchestrator 拍板组合立项
- 候选 A：`TimeVizPrefs.getBirthdayMillis()` 接口 KDoc 写"UTC 00:00"，实际存"当地 00:00"（M1 修复后 `setBirthday` 用 `ZoneId.systemDefault()`）；`TimeVizCalculatorTest` 类 KDoc + `utcMidnightMillis` KDoc 同样失准
- 候选 B：`EventRepository.shiftToTargetDay(e, year, month, day, now)` 的 `now` 参数函数体未使用，唯一调用方 `carryOverNotStarted`
- 候选 C：`ExportViewModel.markError` / `ImportViewModel.markError` 在 SettingsScreen 真实调用（SAF 开流返回 null 时），但零测试覆盖
- 工作量 M，风险全低

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-kdoc-deadparam-markerror-test-design.md`
- Design 阶段实读代码关键发现：
  - `markError` 是**单方法带默认参数**（非重载）：`ExportViewModel.markError(msg: String = "导出失败，请重试")` / `ImportViewModel.markError(msg: String = "导入失败，请重试")`
  - `shiftToTargetDay` 删 `now` 参数时，`carryOverNotStarted` 内 `now` 变量仍用于 `eventDao.updateEventTime(..., now = now)`，**不能误删**
  - `utcMidnightMillis` 函数名/函数体不动（重命名超范围），仅改 KDoc

### Coding ✅
- 5 文件改动（2 生产代码 + 3 测试），零新增文件
- `TimeVizPrefs.kt`：`getBirthdayMillis()` KDoc "UTC 00:00" → "当地 00:00"
- `EventRepository.kt`：`shiftToTargetDay` 删 `now: Long` 参数 + `carryOverNotStarted` 调用处同步；`now` 变量保留（仍用于 `updateEventTime`）
- `TimeVizCalculatorTest.kt`：类头 KDoc + `utcMidnightMillis` 函数 KDoc 修正（函数名/函数体未动）
- `ExportViewModelTest.kt`：+2 markError 用例（默认消息 + 自定义消息）
- `ImportViewModelTest.kt`：+2 markError 用例（默认消息 + 自定义消息）
- 4 用例均用 `runTest(mainRule.dispatcher)` 包裹，复用既有 setup

### Test ✅（独立 subagent 验证）
- 四道门全绿：
  - 门1 `compileDebugKotlin`：BUILD SUCCESSFUL（1s）
  - 门2 `testDebugUnitTest --rerun-tasks`：BUILD SUCCESSFUL（21s），**175 测试全绿** = 171 现有 + 4 新增，0 failures / 0 errors / 0 skipped
  - 门3 `assembleDebug`：BUILD SUCCESSFUL（1s）
  - 门4 `assembleRelease --rerun-tasks`：BUILD SUCCESSFUL（1m12s），`libandroidx.graphics.path.so` strip 警告非错误（exit 0）
- spec §十 6 项核对全过（实读代码非信任 Coding 自评）：
  1. markError 签名与默认消息一致 ✅
  2. shiftToTargetDay 死参数已删 + `now` 变量未误删 ✅
  3. KDoc 修正零逻辑改动 ✅
  4. 零回归（17 suites 全绿）✅
  5. 测试总数 = 175 ✅
  6. 零范围外改动 ✅
- 逐 suite 累加：10+8+10+13+14+20+6+7+13+10+7+5+7+4+22+4+15 → 14+7+6+5+4+7+20+4+10+10+15+13+13+22+8+10+7 = 175

### 质量评估 ✅
- **本轮达成**：三项防御性改进闭合（KDoc 失准修正 + 死参数清理 + markError 4 用例补齐），四道门全绿，零回归
- **里程碑**：M1 修复遗留 KDoc 漏改闭合（`TimeVizPrefs` 接口 + `TimeVizCalculatorTest` 注释对齐实际语义）；`shiftToTargetDay` 死参数清理使签名更诚实；`markError` 错误路径有自动化守护（防 SAF 开流失败回归）
- **$100 质量线判定**：✅ **满分稳固达标**（100/100，所有维度满分 + 无 blocker + 四道门绿含 release + 文档与代码完全一致 + 175 测试覆盖）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线满分，但未跑满 20 轮迭代（当前 16 轮），**继续迭代**。后续轮次重点：迭代17-19 灵活（剩余 backlog nit 评估 / 防御性测试加固 / 其他打磨）/ 迭代20 最终验收
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-16 大量未提交改动，强烈建议提交基线）

---

## 迭代 17（2026-06-28）：TimelineViewModel + RecordingViewModel 测试补齐

### Discover ✅
- 扫描发现测试覆盖不均衡：TimelineViewModel（4 用例）和 RecordingViewModel（5 用例缺错误路径）是当前最薄的两个核心 VM
- 候选 1：TimelineViewModel 测试补齐（quickAddEvent/markInProgress/markCompleted 守卫+成功 / deleteEvent / init{} 顺延 / 日期导航）
- 候选 2：RecordingViewModel 错误路径补齐（save 空标题 / save 编辑不存在 / delete 无 editingId / delete 成功 / initEdit NotStarted 分支）
- orchestrator 拍板组合立项

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-timeline-recording-vm-test-design.md`
- Design 阶段实读源码关键发现：
  - 日期导航方法实际名为 `goToToday()` / `goToPreviousDay()` / `goToNextDay()` / `setDate(year, month, day)`（Discover 猜的 `selectDate`/`previousDay`/`nextDay` 不存在）
  - VM 的 `markInProgress`/`markCompleted` 不委托 Repository，VM 自有逻辑（markInProgress→endTime=null+新startTime；markCompleted→endTime=now+保留startTime），与 Repository 同名方法语义不同
  - `init{}` 顺延测试必须构造新 VM（`@Before` 共享 vm 的 init{} 已对空库跑过）
  - 顺延种子必须显式传 `status=EventStatus.NotStarted.value`（否则 determineStatus 会判 InProgress）
  - 守卫路径不变更 DB，无需 first{} 等待，用 repo 直读断言

### Coding ✅
- 2 测试文件改动，零新增文件，零生产代码改动
- `TimelineViewModelTest.kt`：+9 用例 T1-T9（+3 import：EventStatus/Calendar/TimeZone）
  - T1 守卫空标题 / T2 成功新增
  - T3/T5 守卫不存在 id / T4 markInProgress 成功 / T6 markCompleted 成功
  - T7 deleteEvent 成功 / T8 init{} 顺延（freshVm + 显式 NotStarted 状态）/ T9 日期导航四方法
- `RecordingViewModelTest.kt`：+5 用例 R1-R5（无新增 import）
  - R1 save 空标题返回 false / R2 save 编辑已删除返回 false
  - R3 delete 无 editingId 返回 false / R4 delete 成功返回 true
  - R5 initEdit NotStarted 分支（startMinutes 用 possibleSnaps 集合兼容跨分钟边界）
- 唯一偏差（已修复）：T4/T6 初版用严格时间边界断言，T6 因协程内 now 在 markCompleted 返回后才捕获导致 1ms 越界失败，改为 5s 容差断言（spec §七建议方案）

### Test ✅（独立 subagent 验证）
- 四道门全绿：
  - 门1 `compileDebugKotlin`：BUILD SUCCESSFUL（753ms，UP-TO-DATE）
  - 门2 `testDebugUnitTest --rerun-tasks`：BUILD SUCCESSFUL（19s），**189 测试全绿** = 175 现有 + 14 新增，0 failures / 0 errors / 0 skipped
  - 门3 `assembleDebug`：BUILD SUCCESSFUL（1s）
  - 门4 `assembleRelease --rerun-tasks`：BUILD SUCCESSFUL（59s），`libandroidx.graphics.path.so` strip 警告非错误
- spec §十 8 项核对全过：
  1. TimelineViewModelTest 9 用例与 spec §4.1 一致 ✅
  2. RecordingViewModelTest 5 用例与 spec §4.2 一致 ✅
  3. flaky 稳定区红线（无 backgroundScope.launch / state.collect / 方案 A 标志）✅
  4. 守卫用例不用 first{} 等待（T1/T3/T5 用 repo 直读）✅
  5. T8 用 freshVm + 显式 NotStarted 状态 ✅
  6. 零生产代码改动（两 VM 源文件连历史改动都没有）✅
  7. 零回归（17 suites 全绿）✅
  8. 测试总数 = 189 ✅

### 质量评估 ✅
- **本轮达成**：TimelineViewModel 测试补齐（4→13 用例）+ RecordingViewModel 错误路径补齐（5→10 用例），四道门全绿，零生产代码改动
- **里程碑**：**两个核心 VM 测试覆盖大幅加固** —— TimelineViewModel 错误路径（quickAddEvent 守卫 / markInProgress+markCompleted 不存在 id 守卫 / init{} 顺延 / 日期导航）+ RecordingViewModel 错误路径（save 空标题 / save 编辑不存在 / delete 无 editingId / delete 成功 / initEdit NotStarted 分支）全部有自动化守护
- **$100 质量线判定**：✅ **满分稳固达标**（100/100，所有维度满分 + 无 blocker + 四道门绿含 release + 文档与代码完全一致 + 189 测试覆盖）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线满分，但未跑满 20 轮迭代（当前 17 轮），**继续迭代**。后续轮次重点：迭代18-19 灵活（剩余 backlog nit 评估 / 其他防御性测试 / 其他打磨）/ 迭代20 最终验收
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-17 大量未提交改动，强烈建议提交基线）

---

## 迭代 18（2026-06-28）：Repository 边界测试 + ImportCounts 断言补齐 + room-testing 配置统一

### Discover ✅
- 扫描发现数据层仍有零覆盖盲点：`EventRepository.markNotStarted` 全项目零测试，`deleteEventById` / `updateEvent` / `upsertAll` 仅作 helper 用过未作被测方法；`DataImportManagerTest.applyImport_*` 三个用例仅断言 prefsUpdated，eventsImported / notesImported 从未断言；`app/build.gradle.kts` 的 room-testing 硬编码版本与其他三处 room 依赖风格不一致
- 候选 1：EventRepository / NoteRepository 边界方法测试补齐（约 8-10 用例）
- 候选 2：DataImportManager ImportCounts 三字段断言补齐（3 改 + 1 新）
- 候选 3：build.gradle.kts room-testing 版本引用统一（1 行）
- orchestrator 拍板组合立项（同属"数据层防御性加固"主线，闭合迭代 17 Discover 留下的 Repository 边界盲区 + spec §7.2 ImportCounts 盲区）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-repo-impcounts-roomconfig-test-design.md`
- Design 阶段实读源码关键发现：
  - `applyImport_idConflict_replacesExisting` 实际**连 prefsUpdated 都未断言**（返回值被丢弃），Discover 报告"三个用例仅断言 prefsUpdated"略偏（其中 idConflict 用例更糟）
  - ImportCounts 累加逻辑不查 DB，直接用 `result.events.size` / `result.notes.size` / `result.timeVizPrefs != null` 构造，期望值就是输入列表长度
  - `EventDao.deleteEventById` 返回 Unit（@Query 而非 @Delete），用例只能断言 DB 状态不能断言返回值
  - NoteRepositoryTest 现有 4 用例都通过 `repo.createNote` 间接创建，未直接构造 NoteEntity，需补 import
  - 根 build.gradle.kts `extra["room"] = "2.6.1"` 与 app 硬编码完全一致，改动行为零变化

### Coding ✅
- 4 文件改动（3 测试 + 1 build 配置），零新增文件，零生产代码改动
- `EventRepositoryTest.kt`：+8 用例
  - markNotStarted 成功 + 守卫；markCompleted/markInProgress 守卫
  - deleteEventById 成功；updateEvent 持久化（不断言 updatedAt）；upsertAll 插入 + 冲突替换
- `NoteRepositoryTest.kt`：+2 用例 + 补 import NoteEntity
  - upsertAll 插入 + 冲突替换（显式 id）
- `DataImportManagerTest.kt`：3 改 + 1 新
  - 3 个 applyImport 用例补 eventsImported/notesImported 断言（idConflict 用例同时把返回值改为 `val counts =` 接收）
  - 新增 applyImport_fullDataset_countsAllThree（events+notes+prefs 全有）
- `build.gradle.kts`：1 行改动（room-testing 改 extra 法，其他 5 行硬编码不动）
- 偏差（按 spec 处理）：任务描述列了 `deleteEventById_nonExistentId_isNoOp`，但 spec §4.1.2 实际只有 8 用例，按 spec 实现

### Test ✅（独立 subagent 验证）
- 四道门全绿：
  - 门1 `compileDebugKotlin`：BUILD SUCCESSFUL（1s）
  - 门2 `testDebugUnitTest --rerun-tasks`：BUILD SUCCESSFUL（36s），**200 测试全绿** = 189 现有 + 11 新增，0 failures / 0 errors / 0 skipped
  - 门3 `assembleDebug`：BUILD SUCCESSFUL（1s）
  - 门4 `assembleRelease --rerun-tasks`：BUILD SUCCESSFUL（1m8s），`libandroidx.graphics.path.so` strip 警告非错误
- spec §十 8 项核对全过：
  1. EventRepositoryTest 8 用例与 spec §4.1.2 一致 ✅
  2. NoteRepositoryTest 2 用例与 spec §4.1.3 一致 + import 已补 ✅
  3. DataImportManagerTest 改动与 spec §4.2 一致 + ImportCounts 累加逻辑与断言值一致 ✅
  4. build.gradle.kts 改动与 spec §4.3 一致 + 其他 5 行硬编码未动 ✅
  5. 零生产代码改动（EventRepository.kt/NoteRepository.kt/DataImportManager.kt 不在改动列表）✅
  6. 零回归（17 suites 全绿）✅
  7. 测试总数 = 200 ✅
  8. 未触及 flaky 稳定区（HeatmapViewModelTest/ExportViewModelTest 未改）✅

### 质量评估 ✅
- **本轮达成**：Repository 边界测试补齐（EventRepo +8 / NoteRepo +2 用例）+ ImportCounts 三字段断言闭合（3 改 + 1 新）+ room-testing 配置统一（1 行），四道门全绿，零生产代码改动
- **里程碑**：**数据层防御性加固完成** —— Repository 边界方法（markNotStarted/deleteEventById/updateEvent/upsertAll）全部有直测守护（不再依赖 VM 间接覆盖），ImportCounts 三字段断言完整（eventsImported/notesImported/prefsUpdated 各 4 处断言），room-testing 配置与其他 room 依赖同源防版本漂移
- **$100 质量线判定**：✅ **满分稳固达标**（100/100，所有维度满分 + 无 blocker + 四道门绿含 release + 文档与代码完全一致 + 200 测试覆盖）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线满分，但未跑满 20 轮迭代（当前 18 轮），**继续迭代**。后续轮次重点：迭代19 灵活（剩余 backlog nit 评估 / 其他防御性测试 / 验收准备）/ 迭代20 最终验收
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-18 大量未提交改动，强烈建议提交基线）

---

## 迭代 19（2026-06-28）：SearchFilter+Calculator 边界用例 + CHANGELOG.md 验收准备交付物

### Discover ✅
- 扫描发现纯函数边界盲点：SearchFilter 缺 4 类边界（空输入 / 纯空白 / 同 sortKey 稳定排序 / recentLimit 极端值）；HeatmapCalculator.buildGrid 缺 today 在 yearMonth 之外时不标记的平行用例；TimeVizCalculator.lifeRemaining 缺 `>=` 临界边界
- 候选 1：SearchFilter + Calculator 边界用例补齐（约 7-9 用例）
- 候选 2：CHANGELOG.md 验收准备交付物（项目当前无任何 CHANGELOG，iteration-log 是 AI agent 内部日志非用户/审核向）
- 不推荐候选 3（MigrationTest）：风险中，验收前不宜引入新风险
- orchestrator 拍板候选 1+2 组合立项（沿用防御性测试加固主线 + 为迭代 20 验收铺路）

### Design ✅
- spec 落盘：`docs/superpowers/specs/2026-06-28-searchfilter-calc-changelog-design.md`
- Design 阶段实读源码关键发现：
  - **`SearchFilter.kt` 不存在** —— `filterAndMerge` 实际定义在 `SearchViewModel.kt`（internal fun，同包可见），Discover 报告路径错误
  - **`Iterable.take(n)` 在 n<0 时抛 `IllegalArgumentException`**（Kotlin stdlib `require(n >= 0)`），而非返回空 —— 修正 Discover 报告，拆为 2 用例：zero 返回空 + negative 抛异常
  - **`buildGrid` 的 `isToday = date == today` 不检查 `isInMonth`** —— 对 today 在 yearMonth 之外时全网格无 isToday cell（与 Discover 假设一致）；但备案发现 today 落在补位格会被标记 isToday=true && isInMonth=false（本轮不修，仅备案）
  - **`lifeRemaining` 的 `exceeded = yearsLived >= lifespanYears`** —— 恰好等于时 exceeded=true（与 Discover 假设一致）
  - **`versionName = "1.0"` 非 "1.0.0"** —— CHANGELOG.md 用 v1.0（与 build.gradle.kts + AboutScreen 显示一致）

### Coding ✅
- 4 文件改动（3 测试追加 + 1 新增文档），零生产代码改动，零新增测试基础设施
- `SearchFilterTest.kt`：+5 用例（10→15）
  - emptyQuery_emptyInputs / whitespaceOnlyQuery（纯空白 "   " / "  \t  "）/ eventAndNoteSameSortKey（断言 EventItem 在前 + TimSort 稳定）/ recentLimit_zero / recentLimit_negative（用 JUnit assertThrows，truth-lambda 不可用退回 JUnit fallback）
- `HeatmapCalculatorTest.kt`：+1 用例（22→23）
  - buildGrid_todayOutsideYearMonth_notMarked（yearMonth=2026-07, today=2026-06-28，全 42 格无 isToday）
- `TimeVizCalculatorTest.kt`：+2 用例（14→16）
  - lifeRemaining_exactlyLifespanYears_exceededTrue（1946-06-28 → 80 岁 == lifespan，exceeded=true，验证 `>=` 临界）/ lifeRemaining_oneYearBeforeLifespan_exceededFalse（79 岁，exceeded=false）
- `CHANGELOG.md`（项目根，新建）：v1.0 单版本，6 分类（核心功能/时间可视化/数据可移植性/上架成熟度/文档/质量保障），引用 specs/ 索引不复述，无联网特性描述
- 偏差（spec 内允许）：异常断言用 JUnit `assertThrows` 而非 Truth lambda（truth-lambda 扩展未引入），spec §4.1.5 已把 assertThrows 列为首选写法，与既有 DataImportManagerTest 风格一致

### Test ✅（独立 subagent 验证）
- 四道门全绿：
  - 门1 `compileDebugKotlin`：BUILD SUCCESSFUL（903ms，16 up-to-date）
  - 门2 `testDebugUnitTest --rerun-tasks`：BUILD SUCCESSFUL（21s），**208 测试全绿** = 200 现有 + 8 新增，0 failures / 0 errors / 0 skipped
  - 门3 `assembleDebug`：BUILD SUCCESSFUL（1s）
  - 门4 `assembleRelease --rerun-tasks`：BUILD SUCCESSFUL（1m8s），`libandroidx.graphics.path.so` strip 警告非错误
- spec §十 8 项核对全过：
  1. SearchFilterTest 5 用例与 spec §4.1 一致（含稳定排序顺序 EventItem 在前 + JUnit assertThrows 风格）✅
  2. HeatmapCalculatorTest 1 用例与 spec §4.2 一致（全网格无 isToday cell + gridStart=2026-06-29 确认）✅
  3. TimeVizCalculatorTest 2 用例与 spec §4.3 一致（`>=` 边界 exceeded=true / 79 岁 exceeded=false）✅
  4. CHANGELOG.md 与 spec §4.4 一致（v1.0 非 v1.0.0 + 6 分类 + 引用 specs 索引 + 无联网描述）✅
  5. 零生产代码改动（4 文件 mtime 集中 20:04-20:05，3 个生产代码文件 mtime 早于本轮）✅
  6. 零回归（17 suites 全绿）✅
  7. 测试总数 = 208 ✅
  8. 未触及 flaky 稳定区（HeatmapViewModelTest/ExportViewModelTest 未改）✅

### 质量评估 ✅
- **本轮达成**：SearchFilter+Calculator 边界用例补齐（8 用例）+ CHANGELOG.md 验收交付物新建，四道门全绿，零生产代码改动
- **里程碑**：**纯函数边界覆盖完成 + 验收交付物就位** —— 三处纯函数边界（filterAndMerge 空输入/纯空白/同 sortKey/recentLimit 极端值 / buildGrid today 在 yearMonth 之外 / lifeRemaining `>=` 临界）全部有自动化守护；CHANGELOG.md v1.0 为迭代 20 最终验收提供可追溯的版本变更记录（开发者/AI agent/审核向）
- **$100 质量线判定**：✅ **满分稳固达标**（100/100，所有维度满分 + 无 blocker + 四道门绿含 release + 文档与代码完全一致 + 208 测试覆盖）。剩余 caveat：release 真机验证（需真机）
- **按用户停止条件**：已达 $100 质量线满分，已跑满 19 轮迭代（当前 19 轮），**下一轮迭代 20 是最终验收**（对照 $100 付费上架标准诚实判断）
- 未 commit（按 git 安全协议，待用户明确指示；仓库存在迭代 2-19 大量未提交改动，强烈建议提交基线）

---

## 迭代 20（2026-06-28）：最终验收

### 任务 ✅
独立验收 subagent 跑四道门 + 对照 $100 质量线 6 项判定标准 + 诚实评估剩余 caveat + 给出最终结论。

### 四道门最终状态 ✅
- 门1 `compileDebugKotlin`：BUILD SUCCESSFUL（973ms，16 up-to-date）
- 门2 `testDebugUnitTest --rerun-tasks`：BUILD SUCCESSFUL（21s），**208 测试全绿**，0 failures / 0 errors / 0 skipped
- 门3 `assembleDebug`：BUILD SUCCESSFUL（1s）
- 门4 `assembleRelease --rerun-tasks`：BUILD SUCCESSFUL（1m3s），`libandroidx.graphics.path.so` strip 警告非错误

### $100 质量线判定（6 项全过）✅
1. 总分 ≥ 90：✅（实际 100/100）
2. 产品愿景达成度 ≥ 13/15：✅（实际 15/15，时间可视化 + 热力图月/年双层全实现）
3. 上架成熟度 ≥ 12/15：✅（实际 15/15，隐私政策 + 数据导出/导入 + splash + release 配置四件齐）
4. 无 blocker 级 bug：✅
5. 四道验证门全绿：✅
6. 文档与代码一致：✅（manifest 零权限 / versionName=1.0 / AppDatabase version=2 / release 配置 / feature 文件齐全，ARCHITECTURE 4 视角与代码核对一致）

### 测试矩阵抽查 ✅
- 17 suites 逐 suite 累加 = 208 测试，0 failures / 0 errors / 0 skipped
- TimelineViewModelTest = 13（4 旧 + 9 新）✅
- EventRepositoryTest = 21（13 旧 + 8 新）✅
- SearchFilterTest = 15（10 旧 + 5 新）✅

### 文档清单核对 ✅
6 个核心文档全部存在且非空：
- `/README.md`（用户入口，65 行）
- `/AGENT.md`（AI agent 入口，204 行）
- `/CHANGELOG.md`（验收交付物，迭代19 新建，v1.0 单版本 6 分类）
- `/docs/ARCHITECTURE.md`（架构 4 视角，ASCII art）
- `/docs/superpowers/loop/iteration-log.md`（loop 日志）
- `/docs/superpowers/loop/quality-scorecard.md`（质量评估表）
- `/docs/superpowers/specs/`（21 个 spec 文件）

### 剩余 caveat 诚实评估
| caveat | 严重度 | 是否影响 $100 付费上架 |
|---|---|---|
| **release-verify** | 仅剩 caveat | **需真机确认，唯一硬 caveat** —— R8/ProGuard 配置静态验证通过，但运行期 Hilt/Room/BuildConfig 是否真不崩需真机冷启动确认 |
| daemon-stall | nit | ❌ 不影响（用户不跑 gradle） |
| git 基线 | 流程 | ❌ 不直接影响上架，建议提交归档 |
| M1-legacy | nit | ❌ 不影响（中国 UTC+8 不受影响） |
| B2-legacy | nit | ❌ 不影响（用户操作即刷新） |
| 窄屏 | nit | ❌ 不影响（<320dp 设备极少，需真机） |
| splash 手测 | 验证项 | ❌ 不影响（需真机） |
| UI 测试 0 | 防回归 | ❌ 不阻塞上架（208 单测覆盖 VM+Repository+纯函数边界） |

### 最终结论 ✅

**✅ 可 $100 付费上架（技术上达标，附 1 项需真机确认的硬 caveat）。**

理由（诚实）：
- 6 项质量线判定全过，无一项❌
- 四道门实跑全绿（门2/门4 强制 --rerun-tasks），208 测试 0 失败
- 代码现状与 scorecard / ARCHITECTURE / README / CHANGELOG 自评主张全部核对一致，未发现自评造假
- 硬约束"绝对不做联网"经 manifest 实读确认零权限，遵守
- 上架四件齐（隐私政策 + 数据导出/导入 + splash + release 配置）全部实文件落地
- 产品愿景核心（时间可视化 + 热力图月/年双层）全部实现

**唯一保留**：release APK 未真机启动验证。R8/ProGuard 配置编译期已静态验证有效（门4 强制重跑全绿），但 R8 fullMode 运行期对 Hilt 生成类 / Room 反射 / BuildConfig 的裁剪是否真不崩必须真机 adb install 冷启动确认一次。这是上架前最后一道手测，不阻塞"$100 质量线达成"的判定，但建议真机验证后再正式挂商店。

### 20 轮迭代总结
- 累计测试：208 测试（17 suites，0 failures / 0 errors / 0 skipped）
- 累计 spec：21 个（`docs/superpowers/specs/`）
- 累计文档：6 个核心文档（README / AGENT / CHANGELOG / ARCHITECTURE / iteration-log / quality-scorecard）
- 累计修复 backlog：10 项已关闭（flaky-test / fallback-bug / deprecated / N1 / N3 / appVersion / 顶栏回调 / spec 偏离 / dead code / 文档过时）
- 累计加分：62 → 100（+38）
- 迭代里程碑：迭代6 跨越 $100 线（87→92）→ 迭代14 总分达 100 满分（99→100）→ 迭代15-19 防御性巩固（满分保持 + 测试 175→208 + 验收交付物 CHANGELOG 就位）

### 后续建议（不在本轮范围）
1. **真机验证 release APK**（最高优先级）：adb install release APK 冷启动，确认 Hilt/Room/BuildConfig 运行期正常，闭合唯一硬 caveat
2. **提交 git 基线**：迭代 2-19 改动归档为可追溯版本基线
3. **UI 测试引入**（防回归）：Compose UI Test 覆盖关键流（记录→时间轴→热力图→搜索→导出/导入）
4. **nit 清理**（可选）：M1-legacy / B2-legacy / 窄屏三项 nit 视用户反馈决定是否处理

---

## 当前 loop 状态（最终）

- 进行中迭代：**无（loop 完成）**
- 已完成迭代：**20 / 20** ✅
- 累计质量提升：62 → 100（+38）
- **$100 质量线**：✅ **满分稳固达标**（100/100，所有维度满分，208 测试覆盖，flaky 已根治，6 项判定全过）
- **最终结论**：✅ **可 $100 付费上架**（技术上达标，唯一保留 release APK 真机启动验证需用户执行）
- **Loop 状态**：🟢 **完成** —— 用户停止条件（至少 20 轮 + $100 付费上架质量线）已满足
