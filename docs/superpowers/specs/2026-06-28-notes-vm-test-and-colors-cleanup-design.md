# 2026-06-28 NotesViewModel 测试 + colors.xml 死色清理 Design（迭代 15）

> 本轮目标：补齐 NotesViewModel 测试覆盖（主）+ colors.xml 死资源清理（次）。
> 防御性改进，迭代 14 已达 100/100 满分，本轮加分 0（过程价值：填补最后一个 ViewModel 测试盲区 + 死资源清理，防回归）。

## 一、背景与目标

- 迭代 14 已达 100/100 满分，所有维度满分；剩余 6 轮（15-20）价值在防御性改进 + 过程价值 + 验收准备。
- Discover 扫描发现 `NotesViewModel` 是项目中唯一无专属 ViewModelTest 的 ViewModel——`feature/notes/` 测试目录尚不存在，是 7 个 ViewModel 中最后一个测试盲区。
- Discover 扫描发现 `app/src/main/res/values/colors.xml` 含 7 个 AS 模板默认死色，全项目零引用（项目实际配色在 `app/src/main/java/com/shijiben/ui/theme/AppColors.kt`，全部以 `Color(0x...)` 形式定义）。
- 本轮为防御性巩固：填补测试盲区 + 死资源清理，加分 0，过程价值在防回归与代码卫生。

## 二、硬约束

1. 绝对不做任何联网功能（本地化 app 唯一原则）。
2. 不改任何 `.kt` 生产代码（仅新增测试文件 + 改 `colors.xml` 资源文件）。
3. 四道验证门全绿（门 2/门 4 强制 `--rerun-tasks`）。
4. `NotesViewModelTest` 必须复用迭代 9 方案 A（`StandardTestDispatcher` + 路由 Room executor + `first{}` + `backgroundScope` 常驻收集者），与 `HeatmapViewModelTest` / `SearchViewModelTest` 一致。
5. `colors.xml` 清理：删 7 死色；因文件仅含这 7 色，可升级为删整个文件（决策点 3 升级）。
6. spec 用符号级引用（类名/函数名/文件名），不用行号。

## 三、Orchestrator 决策点（3 项全部采纳推荐）

1. 本轮目标范围：候选 A（NotesViewModel 测试）+ 候选 B（colors.xml 清理）。
2. NotesViewModel 测试范围：核心状态机 5-6 用例（`startCreate` / `startEdit` / `closeSheet` / `save` / `delete`）。
3. colors.xml 清理策略：删 7 死色保留空 `<resources/>`——**Design 阶段实读确认 colors.xml 仅含这 7 色，升级为删整个文件**。

## 四、设计内容

### §4.1 新增文件 `app/src/test/java/com/shijiben/feature/notes/NotesViewModelTest.kt`

#### §4.1.1 被测对象实测签名（来自 `NotesViewModel.kt` 实读）

- 构造：`@HiltViewModel class NotesViewModel @Inject constructor(private val noteRepository: NoteRepository) : ViewModel()`
- StateFlow：
  - `val allNotes: StateFlow<List<NoteEntity>>` —— `noteRepository.getAllNotes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())`（**stateIn + WhileSubscribed，需常驻收集者激活**）
  - `val editing: StateFlow<NoteEntity?>` —— `_editing.asStateFlow()`（`MutableStateFlow<NoteEntity?>(null)`，**asStateFlow，`.value` 可直接读**）
  - `val sheetOpen: StateFlow<Boolean>` —— `_sheetOpen.asStateFlow()`（`MutableStateFlow(false)`，**asStateFlow，`.value` 可直接读**）
- 状态机方法：
  - `fun startCreate()` —— `_editing.value = null; _sheetOpen.value = true`
  - `fun startEdit(note: NoteEntity)` —— `_editing.value = note; _sheetOpen.value = true`
  - `fun closeSheet()` —— `_sheetOpen.value = false; _editing.value = null`
  - `suspend fun save(content: String): Boolean` —— `content.trim()` 为空返回 `false`（不动状态）；`_editing.value` 非空走 `noteRepository.updateNote(existing.copy(content = c))`（编辑分支），否则走 `noteRepository.createNote(content = c, timestamp = System.currentTimeMillis())`（新建分支）；最后 `_sheetOpen.value = false; _editing.value = null; return true`
  - `suspend fun delete(note: NoteEntity)` —— `noteRepository.deleteNoteById(note.id); _sheetOpen.value = false; _editing.value = null`

> 关键差异（影响测试写法）：`allNotes` 是 `stateIn(WhileSubscribed(5000))`，需 `backgroundScope.launch { vm.allNotes.collect {} }` 常驻收集者 + `vm.allNotes.first { ... }` 等待 Room Flow 落定；`editing` / `sheetOpen` 是 `asStateFlow()`，`.value` 可同步直读，无需收集者（加收集者亦无害）。相比 `SearchViewModelTest` 的 combine 状态流，NotesViewModel 无 `flatMapLatest`/`combine` 切换，flaky 风险更低。

#### §4.1.2 数据层实测签名（来自 `NoteRepository` / `NoteDao` / `NoteEntity` / `AppDatabase` 实读）

- `AppDatabase.noteDao(): NoteDao`（in-memory builder 用 `Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)`）
- `NoteRepository`：`getAllNotes(): Flow<List<NoteEntity>>`、`createNote(content: String, timestamp: Long): Long`、`updateNote(note: NoteEntity)`、`deleteNoteById(id: Long)`
- `NoteDao`：`getAllNotes()` (`ORDER BY timestamp DESC`)、`insertNote(note): Long` (`OnConflictStrategy.REPLACE`)、`updateNote(note)`、`deleteNoteById(id)`、`getNoteById(id): NoteEntity?`
- `NoteEntity`：`id: Long`（`@PrimaryKey(autoGenerate = true)`，默认 `0`）、`content: String`、`timestamp: Long`、`createdAt: Long`、`updatedAt: Long`

#### §4.1.3 测试基础设施（逐字复用方案 A，与 `SearchViewModelTest` setup 对齐）

- 类注解：`@OptIn(ExperimentalCoroutinesApi::class)` + `@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [33])`
- `@get:Rule val mainRule = MainCoroutineRule(StandardTestDispatcher())`
- 字段：`private lateinit var db: AppDatabase`、`private lateinit var noteRepo: NoteRepository`、`private lateinit var vm: NotesViewModel`
- `@Before setup()`：
  1. `val ctx = ApplicationProvider.getApplicationContext<Context>()`
  2. 方案 A roomExecutor 适配器：`val roomExecutor = java.util.concurrent.Executor { command -> mainRule.dispatcher.dispatch(kotlin.coroutines.EmptyCoroutineContext, command) }`
  3. `db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).setQueryExecutor(roomExecutor).setTransactionExecutor(roomExecutor).allowMainThreadQueries().build()`
  4. `noteRepo = NoteRepository(db.noteDao())`
  5. `vm = NotesViewModel(noteRepo)`
- `@After teardown()`：`db.close()`
- 每个测试体：`runTest(mainRule.dispatcher) { backgroundScope.launch { vm.allNotes.collect {} } ; ... }`（`allNotes` 必须常驻收集者保活 WhileSubscribed(5000)；`editing`/`sheetOpen` 直读 `.value` 即可）

#### §4.1.4 测试用例清单（6 个，覆盖 5 方法 + save 双分支 + 初始状态前置断言）

命名风格对齐 `SearchViewModelTest`（`method_behavior_expectedResult`）：

| # | 测试方法名 | 步骤 | 预期断言 |
|---|-----------|------|----------|
| 1 | `startCreate_opensSheetAndEditingNull` | 前置断言初始状态；调 `vm.startCreate()` | 前置：`vm.sheetOpen.value == false`、`vm.editing.value == null`、`vm.allNotes.first { true }` 为空列表；后置：`vm.sheetOpen.value == true`、`vm.editing.value == null` |
| 2 | `startEdit_opensSheetAndEditingSet` | 构造 `NoteEntity(...)`；调 `vm.startEdit(note)` | `vm.sheetOpen.value == true`、`vm.editing.value == note` |
| 3 | `closeSheet_closesSheetAndClearsEditing` | 先 `vm.startEdit(note)` 打开；调 `vm.closeSheet()` | `vm.sheetOpen.value == false`、`vm.editing.value == null` |
| 4 | `save_newNote_persistsAndClosesSheet` | 调 `vm.save("   ")`（空白）；再调 `vm.save("随笔内容")` | 空白：返回 `false`、`vm.sheetOpen.value` 仍为当前状态（未开则 `false`）；保存：返回 `true`、`vm.allNotes.first { it.isNotEmpty() }` 含一条且 `content == "随笔内容"`、`vm.sheetOpen.value == false`、`vm.editing.value == null` |
| 5 | `save_editExisting_updatesAndClosesSheet` | 先 `noteRepo.createNote(...)` 插入并 `first{}` 等待落定；`vm.startEdit(existing)`；调 `vm.save("更新内容")` | 返回 `true`；`vm.allNotes.first { it.any { n -> n.id == existing.id && n.content == "更新内容" } }` 命中；列表总数不变；`vm.sheetOpen.value == false`、`vm.editing.value == null` |
| 6 | `delete_removesFromDbAndClosesSheet` | 先 `noteRepo.createNote(...)` 插入并 `first{}` 等待；`vm.startEdit(note)` 打开 sheet；调 `vm.delete(note)` | `vm.allNotes.first { it.isEmpty() }` 为空（或不含该 id）；`vm.sheetOpen.value == false`、`vm.editing.value == null` |

> 说明：用例 1 将「初始状态断言」作为前置并入 `startCreate` 测试（标准做法），既覆盖初始态又覆盖 `startCreate`，使总用例数控制在 6（符合决策点 2 的 5-6 用例）。`save` 双分支（新建/编辑）各占一例，覆盖 `save` 的 `if (existing != null)` 两条路径 + 空内容早返回路径。

#### §4.1.5 测试模式细节（与 `SearchViewModelTest` 一致）

- `runTest(mainRule.dispatcher)` 统一 TestScope 与 `viewModelScope` 调度器。
- `backgroundScope.launch { vm.allNotes.collect {} }` 常驻收集者保活 `WhileSubscribed(5000)`，让 `first{}` 临时订阅不触发 grace 抖动。
- `vm.allNotes.first { 条件 }`（suspending wait）等待 Room Flow 初始查询 / invalidation 重发落定，替代 `advanceUntilIdle() + .value`（Room Flow 初始查询在 Room 自己的 executor 上异步执行，`advanceUntilIdle()` 只推进 StandardTestDispatcher 队列不等待 Room 线程）。
- `editing` / `sheetOpen` 用 `.value` 直读（asStateFlow 同步可见），无需 `first{}`。
- `save` / `delete` 是 `suspend` 函数，在 `runTest` 的 TestScope 内直接调用即可。
- 构造 `NoteEntity` 时 `id` 传 `0`（autoGenerate），`createdAt`/`updatedAt` 传 `0L` 或 `System.currentTimeMillis()` 均可（测试不关心）；从 `noteRepo.createNote(...)` 拿返回的 `id` 重建实体用于 `startEdit`/`delete`，或用 `noteRepo.getNoteById(id)` 取回完整实体。

### §4.2 改动文件 `app/src/main/res/values/colors.xml`

- 实读确认：该文件仅含 7 个 `<color>` 条目（`purple_200` / `purple_500` / `purple_700` / `teal_200` / `teal_700` / `black` / `white`），无其他内容。
- 引用核对（全项目 Grep）：
  - `purple_200|purple_500|purple_700|teal_200|teal_700` → 仅 `colors.xml` 自身命中（定义处），无任何 `.kt` / `.xml` 引用。
  - `@color/black|@color/white|R.color.black|R.color.white|@color/purple|@color/teal|R.color.purple|R.color.teal` → 零命中。
  - `app/src/main/res/values/themes.xml` 仅定义 `Theme.ShiJiBen` / `Theme.ShiJiBen.Splash`，不引用任何 `@color/...`。
  - `app/src/main/java/com/shijiben/ui/theme/AppColors.kt` 全部以 `Color(0x...)` 定义（如 `val Primary = Color(0xFFEF4444)`），与 `colors.xml` 无关；其中 `RainbowPurple` / `PixelPurple` 是独立 `Color(0x...)` 值，非资源引用。
- 清理动作：**删整个 `colors.xml` 文件**（决策点 3 升级——文件仅含死色，保留空 `<resources/>` 无意义，删文件更彻底）。
- 不改 `AppColors.kt` / `themes.xml` / `strings.xml` / `AndroidManifest.xml` / 任何 drawable。

### §4.3 不改动文件清单（复核）

- 所有 `.kt` 生产代码：零改动（含 `NotesViewModel.kt` / `NoteRepository.kt` / `NoteDao.kt` / `NoteEntity.kt` / `AppDatabase.kt` / `AppColors.kt` 等）。
- `build.gradle.kts` / `settings.gradle.kts` / `gradle/libs.versions.toml`：零改动。
- `AndroidManifest.xml`：零改动。
- 其他资源文件（`strings.xml` / `themes.xml` / drawables）：零改动。
- 其他测试文件（含 `MainCoroutineRule.kt`）：零改动。

## 五、涉及文件清单

| 类型 | 文件 | 改动 |
|------|------|------|
| 新增 | `app/src/test/java/com/shijiben/feature/notes/NotesViewModelTest.kt` | 6 测试用例，复用方案 A |
| 删除 | `app/src/main/res/values/colors.xml` | 删整个文件（仅含 7 死色，全项目零引用） |

## 六、验证标准

1. 四道门全绿：
   - `./gradlew :app:compileDebugKotlin`
   - `./gradlew :app:testDebugUnitTest --rerun-tasks`（预期 171 测试 = 165 现有 + 6 新增）
   - `./gradlew assembleDebug`
   - `./gradlew :app:assembleRelease --rerun-tasks`
2. `NotesViewModelTest` 6 用例全过。
3. 零回归（现有 165 测试全过；`.kt` 生产代码零改动）。
4. `colors.xml` 删除后编译通过（无遗漏引用——已 Grep 全项目确认零引用，编译期会立即报错兜底）。
5. 方案 A 模式复用（setup 与 `HeatmapViewModelTest` / `SearchViewModelTest` 一致：`StandardTestDispatcher` + `roomExecutor` 适配器 + `setQueryExecutor` + `setTransactionExecutor` + `backgroundScope.launch{ collect{} }` + `first{}`）。

## 七、风险评估

- 风险 1：`NotesViewModel` 状态机复杂度超预期 —— 缓解：Design 阶段已实读 `NotesViewModel.kt` 全文，方法/状态机/分支已逐字记录在 §4.1.1，Coding 按此落地即可。
- 风险 2：方案 A 复现 flaky —— 缓解：方案 A 已在 `HeatmapViewModelTest`（7 例）+ `SearchViewModelTest`（13 例）共 20 例 5x 验证稳定；且 `NotesViewModel` 不用 `flatMapLatest` / `combine`（仅 `stateIn`），flaky 风险更低。
- 风险 3：`colors.xml` 删除后有隐藏引用 —— 缓解：Design 阶段已 Grep 全项目（含 `@color/` / `R.color.` 两种引用形式 + `black`/`white`/`purple`/`teal` 全部色名）确认零引用；编译期资源缺失会立即报错兜底。
- 风险 4：测试数统计偏差 —— 缓解：Test subagent 独立 XML 计数核实（165 现有 + 6 新增 = 171）。
- 风险 5：`save` 内 `System.currentTimeMillis()` 不可控 —— 缓解：测试不断言 `timestamp`/`createdAt`/`updatedAt` 具体值，只断言 `content` 与列表成员关系，时间戳由 `NoteRepository.createNote` 内部填入不影响断言。

## 八、预估加分

+0（已达 100/100 满分，加分溢出）。过程价值：填补最后一个 ViewModel 测试盲区（NotesViewModel）+ 死资源清理（colors.xml），巩固防回归基线。

## 九、Coding subagent 注意事项

1. **必须实读 `NotesViewModel.kt`** 以代码为准（spec §4.1.1 已逐字记录方法签名与状态机，若有漂移以代码为准调整测试）。
2. **复用方案 A**：读 `HeatmapViewModelTest.kt` / `SearchViewModelTest.kt` 的 `setup()` 作为模板，逐字对齐（`StandardTestDispatcher` + `roomExecutor` 适配器 + `setQueryExecutor` + `setTransactionExecutor` + `allowMainThreadQueries` + `backgroundScope.launch{ collect{} }` + `first{}`）。
3. **`allNotes` vs `editing`/`sheetOpen` 收集策略差异**：`allNotes` 是 `stateIn(WhileSubscribed(5000))` 必须常驻收集者 + `first{}` 等待；`editing`/`sheetOpen` 是 `asStateFlow()` 可直读 `.value`。不要对 `editing`/`sheetOpen` 用 `first{}`（虽然也能跑，但 `.value` 更直接）。
4. **`colors.xml` 清理**：已确认文件仅含 7 死色，**删整个文件**（决策点 3 升级）；不要保留空 `<resources/>`。
5. **不改任何 `.kt` 生产代码**。
6. **测试用例命名**：参考 `SearchViewModelTest` 风格（`method_behavior_expectedResult`），6 用例名见 §4.1.4。
7. **`save` 空内容子断言**：用例 4 先 `vm.save("   ")` 断言返回 `false` 且 sheet 状态不变，再 `vm.save("随笔内容")` 断言成功路径——覆盖 `save` 早返回分支。
8. **`NoteEntity` 构造**：从 DB 取回实体用 `noteRepo.getNoteById(id)` 或用返回的 `id` 重建，避免 `startEdit`/`delete` 时 `id` 不匹配。

## 十、Test subagent 注意事项

1. 独立重跑四道门（顺序执行，门 2/门 4 强制 `--rerun-tasks`）。
2. 独立核对 `NotesViewModelTest` 6 用例与 `NotesViewModel` 实际方法/分支一致（5 方法 + save 双分支 + 空内容早返回 + 初始状态前置断言）。
3. 核对方案 A 复用（setup 与 `HeatmapViewModelTest` / `SearchViewModelTest` 逐字对齐）。
4. 核对 `colors.xml` 已删除且全项目零引用（Grep `@color/` / `R.color.` + 7 色名）。
5. 核对零回归（`.kt` 生产代码零改动 + 现有 165 测试全过）。
6. 核对测试总数（165 + 6 = 171）。
7. 核对 `allNotes` 用 `first{}` 等待、`editing`/`sheetOpen` 用 `.value` 直读（收集策略差异）。
