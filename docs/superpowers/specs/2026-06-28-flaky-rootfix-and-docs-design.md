# 事记本 迭代 9 设计 spec：flaky test 根治 + 文档打磨

> 日期：2026-06-28
> 迭代：9（自迭代 loop）
> 范围：2 目标组合（A flaky test 根治 / B 文档打磨）
> 前置：迭代 1-8 已落地，当前质量评分 94/100，118 个单测全绿（门2 flaky 复现非稳定回归）。
> 验证门：四道门全绿，门2 需连续 5 次 `--rerun-tasks` 全绿才算 flaky 根治。

---

## 一、问题

### 1.1 flaky test 复现

**现象**：`HeatmapViewModelTest` 间歇性抛 `IllegalStateException at TestMainDispatcher.kt:67`，非稳定回归（迭代 4 修复后大幅缓解但未根治，门2 偶发复现）。

**迭代 4 方案为何未根治**：迭代 4 spec（`2026-06-28-data-export-and-flaky-fix-design.md` §4.1）用 `StandardTestDispatcher` + `runTest(mainRule.dispatcher)` + `backgroundScope.launch { vm.state.collect {} }` 常驻订阅 + `vm.state.first { ... }` 确定性等待，解决了**等待阶段**的时序问题（把 `flatMapLatest` 切换 + `WhileSubscribed` 订阅抖动从重入式 Unconfined 改为队列串行化）。但该方案**未解决 teardown 阶段的竞态**——Room 操作仍跑在真实 executor 线程上，不受 StandardTestDispatcher 控制。

**根因链**（Discover 已精确定位）：

1. `HeatmapViewModel.state` = `_selectedMonth.flatMapLatest { eventRepository.getDailyActivityForMonth(ym).map { ... } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeatmapUiState())`（`HeatmapViewModel` 的 `state` 属性）。`eventRepository.getDailyActivityForMonth` 返回 Room-backed `Flow`（`EventRepository.getDailyActivityForMonth`）。
2. `SharingStarted.WhileSubscribed(5000)` = 最后一个订阅者取消后，上游 Flow **保留 5 秒** grace period。
3. 测试结束时：`runTest` 取消 `TestScope` → `backgroundScope.launch { vm.state.collect {} }` 被取消 → `WhileSubscribed` 进入 5s grace → **上游 Room Flow 仍然活着**。
4. Room 的 invalidation tracker 运行在**真实 executor 线程**上（默认 `ArchTaskExecutor.getIOExecutor()` 线程池），在 grace period 内可能触发 invalidation。
5. invalidation 尝试 re-query 并 emit → dispatch 到 `viewModelScope`（Main = `StandardTestDispatcher`）。
6. `MainCoroutineRule.finished()` 调 `Dispatchers.resetMain()` → Main 变为 `NoopDispatcher`。
7. Room 线程的 dispatch 命中 `NoopDispatcher` → `IllegalStateException at TestMainDispatcher.kt:67`。

竞态窗口 = 步骤 4-5（Room 真实线程 dispatch）与步骤 6-7（`resetMain`）的时间差。概率性发生。

**实证对比**：`ExportViewModelTest` 同样用真实 Room + `StandardTestDispatcher`，但**不 flaky**。因为 `ExportViewModel` 用 `MutableStateFlow`（简单状态机）+ 一次性 `eventRepository.getAllEvents().first()` / `noteRepository.getAllNotes().first()` 取数，无 `flatMapLatest` + `WhileSubscribed` 持续订阅，无 teardown 竞态（`first()` 完成即取消订阅，上游 Room Flow 短暂存活但无持续 collector 链路驱动 invalidation emit 到 viewModelScope）。

### 1.2 文档缺口

`AGENT.md` 两处可验证缺口：

1. **§Spec 索引**：缺迭代 7、迭代 8 两条 spec 条目：
   - `2026-06-28-fallback-fix-and-icons-cleanup-design.md`（迭代 7：fallback 修复 + 图标清理）
   - `2026-06-28-data-import-design.md`（迭代 8：数据导入）
2. **§项目结构**：迭代 8 落地后未回填：
   - `data/export/` 注释仅写「数据导出（DataExportManager）」，未含同包的 `DataImportManager`。
   - `feature/settings/` 注释仅写「SettingsScreen, AboutScreen, ExportViewModel」，未含 `ImportViewModel`。

---

## 二、目标

1. **A flaky 根治**：把 `HeatmapViewModelTest` 中 Room 的 query/transaction executor 路由到 `StandardTestDispatcher`，使 Room 所有操作（含 invalidation tracker 的 `refreshRunnable`）都受测试调度器控制，teardown 后 dispatcher 队列不再被推进 → 不会命中 `NoopDispatcher`。仅改 `HeatmapViewModelTest.setup()`（~15 行），不改 `MainCoroutineRule`，不改生产代码。
2. **B 文档打磨**：补 `AGENT.md` 两处缺口（§Spec 索引 +2 条、§项目结构 +2 个符号），仅修可验证缺口，不扫历史 spec。

---

## 三、非目标

- **不改生产代码**（吸取迭代 7 教训：Design subagent 越权改代码导致归因困难）。本 spec 唯一改动的代码文件是 `HeatmapViewModelTest.kt`（测试文件），不碰 `HeatmapViewModel`、`MainCoroutineRule`、任何 ViewModel/Repository/DAO。
- 不改 `MainCoroutineRule.kt`（避免影响 TimeViz/Timeline/Recording/Export/Import 等 5 个共用 rule 的测试）。
- 不改 `ExportViewModelTest`（不 flaky，作为对照保留）。
- 不改 `TimelineViewModelTest` / `NotesViewModelTest`（当前未复现 flaky；其潜在风险在 §六评估，本轮不动）。
- 不引入新依赖（不引 `androidx.arch.core:core-testing` 的 `InstantTaskExecutor`，用纯 `Executor` 适配）。
- 不联网、不改 DB schema、不改 Entity/DAO。
- 不扫历史 spec 全文（仅补 AGENT.md 索引与结构两处）。

---

## 四、设计

### 4.1 关键技术验证结论

**结论：方案 A 有效。Room 2.6.1 invalidation tracker 使用 configured executor。**

验证方法：反编译 `~/.gradle/caches/` 中 `room-runtime-2.6.1-runtime.jar` 的 `InvalidationTracker.class` 与 `RoomDatabase.class`（`javap -c -p`）。证据链：

1. **`InvalidationTracker` 无独立 executor 字段**。其字段列表（`javap -p` 输出）含 `database`、`shadowTablesMap`、`viewTables`、`tableIdLookup`、`tablesNames`、`autoCloser`、`pendingRefresh`、`observedTableTracker`、`observerMap`、`multiInstanceInvalidationClient`、`refreshRunnable` 等，**无任何 `Executor` 字段**。invalidation 调度必经 `database` 引用。
2. **`refreshRunnable` 经 `database.getQueryExecutor()` 调度**。`InvalidationTracker.class` 字节码（`javap -c`）明确：
   ```
   invokevirtual #319  // RoomDatabase.getQueryExecutor:()Executor
   getfield      #170  // Field refreshRunnable:Runnable
   invokeinterface #569 // Executor.execute:(Runnable)V
   ```
   即 `database.getQueryExecutor().execute(refreshRunnable)`。这是 invalidation tracker 的核心刷新路径。
3. **`RoomDatabase.getQueryExecutor()` 直接返回 `internalQueryExecutor` 字段，无 fallback**。字节码：`getfield internalQueryExecutor` → `ifnonnull` → `areturn`；若为 null 抛 `throwUninitializedPropertyAccessException`。说明该字段必在 build 时被赋值，无默认 Arch executor 兜底。
4. **`RoomDatabase.Builder` 提供 `setQueryExecutor(Executor)` / `setTransactionExecutor(Executor)`**（`javap -p` 确认），其私有 `queryExecutor` / `transactionExecutor` 字段在 `build()` 时赋给 `RoomDatabase.internalQueryExecutor` / `internalTransactionExecutor`。
5. **`startMultiInstanceInvalidation` 亦用 `database.getQueryExecutor()`**（字节码 offset 34 处 `invokevirtual getQueryExecutor`），传给 `MultiInstanceInvalidationClient`。本 app 用 `inMemoryDatabaseBuilder`（单实例），不触发多实例 invalidation，无影响。

**推论**：通过 `Room.inMemoryDatabaseBuilder(...).setQueryExecutor(testExecutor).setTransactionExecutor(testExecutor).build()`，Room 所有操作（query、transaction、invalidation tracker 的 `refreshRunnable`、Flow re-query）都经 `testExecutor` 调度。若 `testExecutor` 把 command 包装后 dispatch 到 `StandardTestDispatcher`，则：
- 测试体内：`vm.state.first { ... }` 挂起时 `runTest` 自动 advance `StandardTestDispatcher` 队列 → Room 的 `refreshRunnable` / re-query 任务执行 → emit → `first{}` 恢复。确定性等待，无 hang（与现有 `first{}` 机制一致，仅执行线程从真实线程换为同调度器队列）。
- teardown 后：`runTest` 结束、`TestScope` 取消、`resetMain()` 调用后，`StandardTestDispatcher` 队列不再被任何人 advance → 队列中残留的 Room invalidation 任务**永远不执行** → 不会 emit → 不会 dispatch 到 `viewModelScope` → 不会命中 `NoopDispatcher`。竞态窗口被从根上消除。

### 4.2 子项 A：flaky 根治（方案 A）

#### 4.2.1 改动点

仅改 `HeatmapViewModelTest.setup()`（约 15 行）。新增一个 `Executor` 适配器，把 Room executor 命令桥接到 `mainRule.dispatcher`（`StandardTestDispatcher`），并用 `setQueryExecutor` / `setTransactionExecutor` 注入。

#### 4.2.2 testExecutor 适配器

`CoroutineDispatcher.dispatch(context, block: Runnable)` 是 `StandardTestDispatcher` 的底层入队 API；`Executor.execute(command: Runnable)` 是 Room 调用方。二者签名直接桥接，无需新建 CoroutineScope（避免 scope 生命周期与 TestScope 不一致导致的泄漏）：

```kotlin
val roomExecutor = java.util.concurrent.Executor { command ->
    mainRule.dispatcher.dispatch(kotlin.coroutines.EmptyCoroutineContext, command)
}
```

要点：
- `mainRule.dispatcher` 即 `MainCoroutineRule` 构造时传入的 `StandardTestDispatcher`，与 `Dispatchers.setMain(...)`、`runTest(mainRule.dispatcher)` 共用同一调度器/队列。三处统一（Main / TestScope / Room executor）是消除竞态的关键。
- `dispatch(EmptyCoroutineContext, command)` 直接把 `command` 入 `StandardTestDispatcher` 队列，不创建 coroutine。`runTest` 在测试体 suspend 时 auto-advance 队列，teardown 后队列静止。
- 不用 `CoroutineScope(dispatcher).launch { command.run() }`——那会创建不属于 `TestScope` 的协程，teardown 时不受控，反而引入新泄漏。

#### 4.2.3 setup() 改写

`HeatmapViewModelTest.setup()` 现状（符号引用）：
```
db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
    .allowMainThreadQueries()
    .build()
```

改后：
```kotlin
@Before
fun setup() {
    val ctx = ApplicationProvider.getApplicationContext<Context>()
    val roomExecutor = java.util.concurrent.Executor { command ->
        mainRule.dispatcher.dispatch(kotlin.coroutines.EmptyCoroutineContext, command)
    }
    db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
        .setQueryExecutor(roomExecutor)
        .setTransactionExecutor(roomExecutor)
        .allowMainThreadQueries()
        .build()
    eventRepo = EventRepository(db.eventDao())
    vm = HeatmapViewModel(eventRepo)
}
```

新增 import（测试文件内）：
- `java.util.concurrent.Executor`（或用 fully-qualified，避免 import 膨胀——实现侧二选一，实现 agent 自决）
- `kotlin.coroutines.EmptyCoroutineContext`（同上）

#### 4.2.4 其余测试方法零改动

`HeatmapViewModelTest` 的 8 个 `@Test` 方法、`teardown()`、`mainRule` 声明、`@Config(sdk=[33])`、`@RunWith(RobolectricTestRunner::class)` 均**不动**。现有 `backgroundScope.launch { vm.state.collect {} }` + `vm.state.first { ... }` 模式（迭代 4 落地）保留——它解决等待阶段时序，本 spec 解决 teardown 阶段竞态，二者互补。

#### 4.2.5 不改 MainCoroutineRule 的理由

`MainCoroutineRule` 被 6 个测试共用（Heatmap / TimeViz / Timeline / Recording / Export / Import）。其 `finished()` 调 `Dispatchers.resetMain()` 是 JUnit TestWatcher 标准行为，改它会波及所有共用方。teardown 竞态的根因在 Room executor 走真实线程，而非 rule 本身——把 executor 路由到 rule 的 dispatcher 即可对症，无需动 rule。

### 4.3 子项 B：文档打磨

仅改 `AGENT.md`，两处：

#### 4.3.1 §Spec 索引补 2 条

在现有末尾（`release-build-config-design.md` 条目之后）追加：

```markdown
- [2026-06-28-fallback-fix-and-icons-cleanup-design.md](docs/superpowers/specs/2026-06-28-fallback-fix-and-icons-cleanup-design.md) — fallback 修复 + 应用图标清理
- [2026-06-28-data-import-design.md](docs/superpowers/specs/2026-06-28-data-import-design.md) — 数据导入（JSON 解析 + SAF 读取 + DB 写入）
```

#### 4.3.2 §项目结构补 2 个符号

- `data/export/` 行注释：`# 数据导出/导入（DataExportManager, DataImportManager）`
- `feature/settings/` 行注释：`# 设置 + 关于/隐私政策 + 数据导出/导入（SettingsScreen, AboutScreen, ExportViewModel, ImportViewModel）`

理由：`DataImportManager` 与 `DataExportManager` 同包（`com.shijiben.data.export`，已核对路径 `app/src/main/java/com/shijiben/data/export/DataImportManager.kt`）；`ImportViewModel` 与 `ExportViewModel` 同包（`com.shijiben.feature.settings`，路径 `app/src/main/java/com/shijiben/feature/settings/ImportViewModel.kt`）。注释合并到现有行，不新增目录条目（避免制造不存在的目录）。

---

## 五、涉及文件清单

### 改

| 子项 | 文件 | 改动 |
|------|------|------|
| A | `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt` | 仅 `setup()`：加 `roomExecutor` 适配器 + `setQueryExecutor` / `setTransactionExecutor`（~15 行）。`@Test` 方法、`teardown()`、`mainRule` 零改动。 |
| B | `AGENT.md` | §Spec 索引 +2 条；§项目结构 2 行注释补符号。 |

### 新增

无。

### 复核无改动

| 文件 | 理由 |
|------|------|
| `MainCoroutineRule.kt` | 6 测试共用，teardown 竞态根因在 Room executor 非 rule；不动。 |
| `HeatmapViewModel.kt` | 生产代码不动（严约束）。`state` 属性 `WhileSubscribed(5000)` 保留。 |
| `ExportViewModelTest.kt` | 不 flaky（`MutableStateFlow` + 一次性 `first()`），作对照保留。 |
| `TimelineViewModelTest.kt` / `NotesViewModelTest.kt` | 当前未复现 flaky，本轮不动（风险见 §六）。 |
| `EventRepositoryHeatmapTest.kt` | 纯 JUnit（测 `effectiveDurationMs` / `aggregateMonth` 顶层函数，无 Room 无 dispatcher），无关。 |
| `EventRepository.kt` / DAO / Entity / `AppDatabase.kt` | 不动。 |

---

## 六、边界情况

1. **方案 A 验证后无效的 fallback**：本 spec §4.1 已通过反编译字节码确认 `refreshRunnable` 经 `database.getQueryExecutor()` 调度，方案 A 有效。**若实现阶段实测发现仍 flaky**（极小概率，例如 Room 内部另有独立线程路径未被反编译覆盖），fallback 到方案 C：改 `HeatmapViewModel.state` 的 `stateIn` 策略 `WhileSubscribed(5000)` → `SharingStarted.Lazily`（1 行生产代码）。Lazily 无 grace period，teardown 时上游立即停。**方案 C 连带风险**：`TimelineViewModel` 的 `events` 与另一 flow（均 `flatMapLatest` + `WhileSubscribed(5000)`）、`NotesViewModel` 的 flow（`WhileSubscribed(5000)`）共 4 处 `stateIn` 同模式，若改需评估是否一并改（否则 Timeline/Notes 潜在 flaky 不除）。此连带是本 spec 选方案 A（test-only）而非方案 C 的核心理由。若 fallback 触发，需 orchestrator 重新拍板方案 C 的改动范围（仅 Heatmap / 还是含 Timeline+Notes）。
2. **路由 executor 后 `first{}` 模式可能 hang**：若 `runTest` 不自动 advance `StandardTestDispatcher` 队列，Room 的 `refreshRunnable` 入队后永不执行，`first{}` 死等。**缓解**：`runTest(mainRule.dispatcher)` 在测试体 suspend 时会 auto-advance 队列（kotlinx.coroutines.test 标准行为），现有 8 个测试已依赖此机制（`first{}` 挂起 → 队列推进 → Room emit → `first{}` 恢复）。改 executor 仅把 Room 任务从真实线程换到同队列，auto-advance 机制不变，预期无 hang。若实测 hang，检查是否 `setQueryExecutor` 与 `setTransactionExecutor` 都已设置（只设一个会导致 transaction 走默认真实线程）。
3. **连续 5 次重跑仍可能偶发失败**：flaky 是概率问题，5 次全绿是"高置信度根治"而非"数学证明根治"。若 5 次中第 N 次失败，需分析堆栈是否仍为 `TestMainDispatcher.kt:67`（同根因 → 方案 A 未完全生效，考虑 fallback）还是新错误（不同根因 → 另行定位）。本 spec 不预设 5 次必绿，仅以 5 次全绿作为达标判据。
4. **文档打磨范围蔓延**：仅修 §Spec 索引缺 2 条 + §项目结构缺 2 符号这 2 处可验证缺口。不扫历史 spec 正文、不补其它章节（如开发约定/构建命令已完备）、不为迭代 7/8 spec 补回填。若实现中发现 AGENT.md 其它明显错误（如错链/错包名），记录但不在此轮修（避免范围蔓延）。
5. **`allowMainThreadQueries` 与 executor 交互**：`allowMainThreadQueries()` 允许 Room 在主线程同步查询。路由 executor 后，主线程查询仍走 `roomExecutor` → `mainRule.dispatcher.dispatch(...)` 入队，由 `runTest` advance 执行。`allowMainThreadQueries` 不绕过 executor（它仅放宽 threading 检查，不改 executor 路由）。预期无冲突。
6. **`@After db.close()` 与队列残留任务**：`db.close()` 可能触发 invalidation tracker 的清理路径。路由 executor 后，任何 close 期间的 dispatch 入 `StandardTestDispatcher` 队列，teardown 后静止不执行。`db.close()` 本身是同步调用（关闭 SQLite 连接），不依赖 executor。预期无死锁。

---

## 七、测试清单

### 7.1 验证门（四道全绿）

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --rerun-tasks   # 连续 5 次全绿
./gradlew assembleDebug
./gradlew :app:assembleRelease
```

### 7.2 flaky 根治达标判据

门2 连续 **5 次** `./gradlew :app:testDebugUnitTest --rerun-tasks` 全绿，且 `HeatmapViewModelTest` 8 个测试每次均通过、无 `IllegalStateException at TestMainDispatcher.kt:67`。5 次全绿 = 根治达标。

### 7.3 HeatmapViewModelTest 回归（断言语义不变）

`setup()` 改 executor 路由后，8 个 `@Test` 零改动，断言与迭代 4 一致：

| 测试方法 | 验证点 |
|----------|--------|
| `initialState_isCurrentMonth` | 当前月 / isCurrentMonth=true / canGoNext=false |
| `previousMonth_decrementsAndEnablesNext` | 上一月 / canGoNext=true |
| `nextMonth_fromPreviousMonth_returnsToCurrent` | 下一月回到当前 / canGoNext=false |
| `nextMonth_atCurrentMonth_doesNotAdvance` | 当前月 nextMonth() 不前进 |
| `goToCurrentMonth_fromPrevious_returnsToCurrent` | 回当前月 |
| `stateCells_shapeIs6x7AndTodayMarked` | 6×7 格 / 今日格 1 个 |
| `stateCells_updatesWhenRepoEmitsNewData` | createEvent 后今日格 level=1 / durationMs=3600_000 |

### 7.4 文档打磨验证

- `AGENT.md` §Spec 索引含 `fallback-fix-and-icons-cleanup-design.md` 与 `data-import-design.md` 两条，链接可解析（相对路径正确）。
- `AGENT.md` §项目结构 `data/export/` 行含 `DataImportManager`，`feature/settings/` 行含 `ImportViewModel`。
- 不引入新 spec 文件、不删现有 spec 文件。

---

## 八、orchestrator 决策点

已拍板（本 spec 直接采用，无需重新确认）：

1. **flaky 根治方案 = 选项 A**：路由 Room query/transaction executor 到 `StandardTestDispatcher`，仅改 `HeatmapViewModelTest.setup()`，不改 `MainCoroutineRule`，不改生产代码。本 spec §4.1 已反编译验证方案 A 成立前提（invalidation tracker 用 configured executor）。
2. **组合方式 = 选项 A**：flaky 根治 + 文档打磨组合（S+S=M，一轮可控）。
3. **验证标准 = 选项 A**：门2 连续 5 次 `--rerun-tasks` 全绿才算根治。

新决策点（若触发 fallback，需 orchestrator 拍板）：

4. **（仅 fallback 触发时）方案 C 改动范围**：若方案 A 实测无效需 fallback 到方案 C（`WhileSubscribed(5000)` → `Lazily`），改 `HeatmapViewModel.state` 一处，还是连带 `TimelineViewModel`（2 处 `stateIn`）+ `NotesViewModel`（1 处 `stateIn`）共 4 处一并改？默认建议：仅改 Heatmap（最小化生产代码改动），Timeline/Notes 待其复现 flaky 再处理。
