# V3 搜索：从历史找事与随笔

日期：2026-06-28
范围：新增「搜索」能力——首页顶栏加入口，新页面 `SearchScreen` 支持 events.title + events.note + notes.content 三字段内存检索（LIKE 子串匹配，大小写忽略），结果按时间倒序混合展示事件与随笔，点击复用 `RecordingSheet` / `NoteEditorSheet` 编辑
前置：2026-06-28 首页构图重平衡 spec（顶栏 26dp 像素方块簇）+ 2026-06-28 热力图 spec（顶栏入口模式）均已落地；`EventRepository.getAllEvents()` / `NoteRepository.getAllNotes()` 已存在；`AppDatabase` version 2；minSdk 26

## 验证门（四道闸全绿）

- `./gradlew :app:compileDebugKotlin`（含 Hilt KSP 类型检查）
- `./gradlew :app:testDebugUnitTest --rerun-tasks`（含新增 `SearchViewModelTest`）
- `./gradlew assembleDebug`
- `./gradlew assembleRelease --rerun-tasks`

## 一、问题

App 目前只能逐日翻看记录（`TimelineScreen` 按日 + `NotesScreen` 全量随笔），用户无法跨日「找一件事」或「找一段随笔」。`MY_ORIGIN_GOAL.md` 强调「记录即审视」——审视的前提是能回看，而回看的前提之一是能检索。当用户记得某件事的关键词（如「跑步」「开会」）或某段随笔的只言片语时，没有快速定位路径：只能逐日翻 `TimelineScreen` 或在 `NotesScreen` 长列表里肉眼扫。

搜索是「轻量存在」哲学的检索侧补全：不做语义分析、不做联网、不做 FTS，只做最朴素的子串匹配——足够把「我记得写过」变成「我找到了」。

## 二、目标

- **搜索范围**：events.title + events.note + notes.content 三字段全覆盖。
- **检索方式**：内存子串匹配（`String.contains(ignoreCase = true)`），大小写忽略，无分词、无 FTS。
- **UI 入口**：首页 `TimelineScreen` 顶栏日期徽章右侧新增 26dp 放大镜像素方块（与热力图/设置同风格），点击进 `SearchScreen`。
- **结果展示**：独立 `SearchScreen`，事件复用 `EventCard`、随笔复用 `NoteRow`，按 `sortKey` 倒序混合排列，统一 `SearchItem` sealed 抽象（与 `TimelineItem` 同模式）。
- **点击行为**：点击事件 → 复用 `RecordingSheet` 编辑；点击随笔 → 复用 `NoteEditorSheet` 编辑。
- **空查询行为**：空查询显示最近 N=50 条记录（事件+随笔混合，按时间倒序），与 `NotesScreen`「所有随笔」默认展示模式一致——不展示空状态，给用户默认可扫内容。
- **可测性**：`SearchViewModelTest` 复用迭代 9 方案 A 测试模式（StandardTestDispatcher + Room executor 路由 + `first{}` + `backgroundScope` collector）。
- **8-bit 美学**：搜索框 2dp 黑边白底直角；放大镜 26dp 像素方块 2dp 黑边；配色全用 `AppColors`。

## 三、非目标

- **不做 FTS4 / 不加索引表**：中文分词复杂度无收益，内存子串匹配够用。
- **不做匹配高亮**：8-bit 简单为上，先做基础版（`AnnotatedString` 高亮留 backlog）。
- **不改 DB schema**：不加表、不加列、不加索引、不加迁移；`AppDatabase` version 保持 2。
- **不改既有 ViewModel**：`TimelineViewModel` / `HeatmapViewModel` / `NotesViewModel` / `RecordingViewModel` 业务逻辑不动。`TimelineScreen` 仅加入口参数 + 顶栏图标块 + `NoteRow` 改 public（去 `private`）。
- **不联网**：纯本地 Room Flow + 内存过滤。
- **不引入新依赖**：`build.gradle.kts` 不动；`Icons.Default.Search` 来自 `material-icons-core`（已随 material3 传递依赖）。
- **不做搜索历史 / 搜索建议 / 联想**：backlog。
- **不做语音搜索 / OCR**：backlog。

## 四、设计

### §4.1 数据层（零改动，复用现有 Flow）

- **EventRepository.getAllEvents(): Flow<List<EventEntity>>** —— 已存在，直接复用，不加查询。
- **NoteRepository.getAllNotes(): Flow<List<NoteEntity>>** —— 已存在，直接复用，不加查询。
- **EventDao / NoteDao**：不改。`getAllEvents()` / `getAllNotes()` 已有对应 `@Query("SELECT * FROM ...")`。
- **EventEntity 字段**：`id` / `title` / `startTime` / `endTime` / `status` / `note` / `createdAt` / `updatedAt`（`EventEntity.kt`）。搜索命中字段：`title` / `note`（nullable）。
- **NoteEntity 字段**：`id` / `content` / `timestamp` / `createdAt` / `updatedAt`（`NoteEntity.kt`）。搜索命中字段：`content`。

### §4.2 SearchViewModel

新增 `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt`：

```kotlin
package com.shijiben.feature.search

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository
) : ViewModel() {

    /** 搜索结果统一抽象，与 TimelineItem 同模式 */
    sealed interface SearchItem {
        val sortKey: Long
        data class EventItem(val event: EventEntity) : SearchItem {
            override val sortKey: Long get() = event.startTime
        }
        data class NoteItem(val note: NoteEntity) : SearchItem {
            override val sortKey: Long get() = note.timestamp
        }
    }

    data class SearchUiState(
        val query: String = "",
        val items: List<SearchItem> = emptyList(),
        val isEmpty: Boolean = true   // items 为空（用于区分"无结果"空状态）
    )

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    fun onQueryChange(q: String) { _query.value = q }

    private val eventsFlow: StateFlow<List<EventEntity>> =
        eventRepository.getAllEvents()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val notesFlow: StateFlow<List<NoteEntity>> =
        noteRepository.getAllNotes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val state: StateFlow<SearchUiState> =
        combine(eventsFlow, notesFlow, _query) { events, notes, q ->
            val items = filterAndMerge(events, notes, q)
            SearchUiState(query = q, items = items, isEmpty = items.isEmpty())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState())

    companion object {
        const val RECENT_LIMIT = 50
    }
}

/** 纯函数：过滤 + 合并 + 排序。internal 供单测直接调（与 aggregateMonth 同模式）。 */
internal fun filterAndMerge(
    events: List<EventEntity>,
    notes: List<NoteEntity>,
    query: String,
    recentLimit: Int = SearchViewModel.RECENT_LIMIT
): List<SearchViewModel.SearchItem> {
    val trimmed = query.trim()
    return if (trimmed.isEmpty()) {
        // 空查询：最近 N 条（事件+随笔混合，按 sortKey 倒序）
        (events.map { SearchViewModel.SearchItem.EventItem(it) } +
         notes.map { SearchViewModel.SearchItem.NoteItem(it) })
            .sortedByDescending { it.sortKey }
            .take(recentLimit)
    } else {
        val q = trimmed.lowercase()
        val matchedEvents = events.filter {
            it.title.lowercase().contains(q) ||
            (it.note?.lowercase()?.contains(q) == true)
        }.map { SearchViewModel.SearchItem.EventItem(it) }
        val matchedNotes = notes.filter {
            it.content.lowercase().contains(q)
        }.map { SearchViewModel.SearchItem.NoteItem(it) }
        (matchedEvents + matchedNotes).sortedByDescending { it.sortKey }
    }
}
```

**关键设计点**：
- `combine(eventsFlow, notesFlow, _query)` 三流合并：任一流 emit（DB 变化或 query 变化）即重新过滤。DB 变化自动驱动结果刷新——用户在 `RecordingSheet`/`NoteEditorSheet` 编辑保存后，Room invalidation 触发 Flow re-emit，结果列表自动更新，无需手动 refresh。
- `eventsFlow` / `notesFlow` 各自 `stateIn(WhileSubscribed(5000))` 缓存上游 Room Flow，避免 `combine` 重订阅时重复查库（与 `TimelineViewModel.events` 同模式）。
- `filterAndMerge` 提为 `internal` 纯函数（与 `aggregateMonth` 同模式），无 Android 依赖，可直接 JUnit 测；亦可通过 VM state 测（方案 A）。
- 空查询 `take(RECENT_LIMIT)`：N=50 是「最近可扫」与「不全量加载」的平衡；`LazyColumn` 本身虚拟化，但限制 50 避免极端历史用户首屏卡顿。常量易调。
- `String.contains(ignoreCase = true)`：等价于 `lowercase().contains(q)`，二者皆可；spec 用显式 `lowercase()` 写法便于纯函数测试无歧义。特殊字符（`%` `_` `\`）按字面量匹配——非 SQL LIKE，无转义需求。
- `SearchItem` 镜像 `TimelineItem`（`TimelineScreen.kt` 中 `private sealed interface TimelineItem`）：同 sealed interface + `sortKey` + EventItem/NoteItem 两子类。不直接复用 `TimelineItem` 因其为 `private` 且语义属 timeline 域；搜索域独立定义更清晰。

### §4.3 SearchScreen

新增 `app/src/main/java/com/shijiben/feature/search/SearchScreen.kt`：

```kotlin
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
    notesViewModel: NotesViewModel = hiltViewModel()   // 复用，供 NoteEditorSheet save/delete
)
```

**布局**（自上而下）：

```
┌─────────────────────────────────┐
│ [顶部 8dp 彩虹条]                │  ← 复用 trimColors 循环（与 HeatmapScreen 同）
│ ‹ 搜索                           │  ← 顶栏：返回箭头 + "搜索"标题
│ [2dp 黑色分隔线]                 │
│ ┌─搜索事/随笔...───────────×─┐  │  ← 搜索框：2dp 黑边白底直角 + 清除×按钮
│ └──────────────────────────┘   │
│ [2dp 黑色分隔线]                 │
│                                 │
│ (LazyColumn 结果列表)            │
│ ┌─EventCard────────────────┐    │  ← 复用 EventCard（status/in-progress badge 正常）
│ │ ▶ 标题  时间  时长 badge  │    │
│ └──────────────────────────┘    │
│ ┌─NoteRow──────────────────┐    │  ← 复用 NoteRow（橙✎ + 时间 + 内容首行）
│ │ ✎ 时间  内容首行...       │    │
│ └──────────────────────────┘    │
│ ...                             │
│                                 │
│ (无结果空状态)                   │
│       没有相关记录               │
│       试试其他关键词             │
└─────────────────────────────────┘
```

**关键交互**：
- 搜索框：`BasicTextField`（与 `EntryDrawer` 同款），2dp `Color.Black` 边框 + `Surface` 白底 + `RoundedCornerShape(0.dp)` 直角；placeholder "搜索事/随笔..." 用 `TextTertiary`；`imeAction = ImeAction.Search`，`keyboardActions` 不触发额外动作（结果实时跟随输入）。
- 清除按钮：query 非空时显示 `×`（`Icons.Default.Clear` 或自绘 ×），点击清空 query 并聚焦回搜索框。
- 结果列表：`LazyColumn`，`items(state.items, key = { it.sortKey.toString() + it.javaClass.simpleName })` 防 key 冲突。
  - `SearchItem.EventItem` → `EventCard(event = it.event, onClick = { editingEvent = it.event; showEventSheet = true }, onStart = {}, onStop = {}, onLongClick = {}, now = now)`。搜索结果中 `onStart`/`onStop`/`onLongClick` 置空 lambda（不暴露计时/删除操作，仅查看+编辑）。
  - `SearchItem.NoteItem` → `NoteRow(note = it.note, onClick = { notesViewModel.startEdit(it.note); showNoteSheet = true })`。
- 空状态：`state.isEmpty` 为 true 时居中显示 "没有相关记录" + "试试其他关键词"，`TextSecondary` 15sp。
- `now`：`val now = remember { System.currentTimeMillis() }` 快照。`EventCard` 仅对 in-progress 事件用 `now` 显示运行时长；搜索结果以历史 completed 事件为主，快照可接受。不起 60s 定时器（非主场景，避免过度设计）。

**Sheet 编辑复用**（与 `TimelineScreen` 同模式）：

```kotlin
var showEventSheet by remember { mutableStateOf(false) }
var editingEvent by remember { mutableStateOf<EventEntity?>(null) }
var showNoteSheet by remember { mutableStateOf(false) }
val editingNote by notesViewModel.editing.collectAsStateWithLifecycle()

if (showEventSheet) {
    RecordingSheet(
        viewingDate = todayTriple(),   // editingEvent != null 时 initEdit 用事件自身时间，viewingDate 不生效
        editingEvent = editingEvent,
        onDismiss = { showEventSheet = false; editingEvent = null },
        onSaved = { showEventSheet = false; editingEvent = null }   // Flow 自动刷新，无需手动 refresh
    )
}
if (showNoteSheet) {
    NoteEditorSheet(
        editing = editingNote,
        onDismiss = { showNoteSheet = false; notesViewModel.closeSheet() },
        onSave = { content -> notesViewModel.save(content) },   // save 内部用 _editing.value
        onDelete = { note -> notesViewModel.delete(note) }
    )
}
```

- `RecordingSheet` 自带 `viewModel: RecordingViewModel = hiltViewModel()`，`editingEvent != null` 时走 `initEdit(event)` 分支（用事件自身 startTime/endTime/note），`viewingDate` 参数被忽略——传 `todayTriple()` 占位即可。
- `NoteEditorSheet` 依赖 `notesViewModel.editing` StateFlow：点击随笔先调 `notesViewModel.startEdit(note)` 设 `_editing`，sheet 内 `onSave` 调 `notesViewModel.save(content)` 读 `_editing` 决定 update/create。`closeSheet()` 清 `_editing` + `_sheetOpen`。
- 保存后无需手动 refresh：Room invalidation → `eventsFlow`/`notesFlow` re-emit → `combine` 重算 → `state.items` 自动更新。

**NoteRow 复用前提**：`TimelineScreen.kt` 中 `NoteRow` 当前为 `private`，需改为 public（去 `private` 关键字）。`EventCard` 已是 public。改动见 §4.4。

### §4.4 入口与导航

#### 4.4.1 TimelineScreen 顶栏加入口

`TimelineScreen.kt` 改动（最小化，3 处）：

1. 新增参数：`onSearchClick: () -> Unit`（加在 `onHeatmapClick` 之后）。
2. 顶栏 `Row`（左侧日期徽章 + 热力图方块 + 设置方块簇）在热力图方块与设置方块之间插入搜索方块：

```kotlin
// 热力图方块（现有）保持不变
Box(modifier = Modifier.size(26.dp).border(2.dp, Color.Black).background(Surface)
    .clickable { onHeatmapClick() }, ...) { /* 2×2 小绿块 */ }
// 新增：搜索入口（放大镜像素方块，与热力图/设置同风格）
Box(
    modifier = Modifier
        .padding(start = 6.dp)
        .size(26.dp)
        .border(2.dp, Color.Black)
        .background(Surface)
        .clickable { onSearchClick() },
    contentAlignment = Alignment.Center
) {
    Icon(
        Icons.Default.Search,
        contentDescription = "搜索",
        tint = Color.Black,
        modifier = Modifier.size(14.dp)
    )
}
// 设置方块（现有）保持不变，padding(start=6.dp) 可保留或微调避免双倍间距
```

- 26dp Box + 2dp 黑边 + 白底 + 14dp 黑色 `Icons.Default.Search`，与设置方块（`Icons.Default.Settings` 14dp 黑色）完全同款。
- 位置：热力图（日期相关簇）与设置（系统簇）之间，语义属「跨日检索」，与热力图「跨日回看」相邻合理。
- `padding(start = 6.dp)` 与既有方块间距一致。

3. `NoteRow` 改 public：`private fun NoteRow` → `fun NoteRow`，供 `SearchScreen` import 复用。`TimelineItem` 保持 `private`（搜索不直接用，自有 `SearchItem`）。

#### 4.4.2 AppNavHost 路由

`navigation/AppNavHost.kt` 改动：

1. `Routes` object 加：`const val SEARCH = "search"`。
2. `composable(Routes.TIMELINE)` 块的 `TimelineScreen(...)` 调用加 `onSearchClick = { navController.navigate(Routes.SEARCH) }`。
3. 新增 `composable(Routes.SEARCH) { SearchScreen(onBack = { navController.popBackStack() }) }`。

- 独立 Screen 模式（非 Sheet 模式）：搜索是独立场景，返回栈清晰；与 `NotesScreen` / `HeatmapScreen` 一致。
- `SearchScreen` 内的 `RecordingSheet` / `NoteEditorSheet` 是 `SearchScreen` 内部 overlay，不占路由（与 `TimelineScreen` 内 sheet 同模式）。

### §4.5 测试

#### 测试模式（复用迭代 9 方案 A）

`SearchViewModelTest` 复用 `HeatmapViewModelTest`（`app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt`）已验证的方案 A：

- `@get:Rule val mainRule = MainCoroutineRule(StandardTestDispatcher())`
- `setup()`：`Room.inMemoryDatabaseBuilder` + `.setQueryExecutor(roomExecutor)` + `.setTransactionExecutor(roomExecutor)` 把 Room executor 路由到 `mainRule.dispatcher`，根治 teardown grace-period 竞态。
- `runTest(mainRule.dispatcher)` 统一 TestScope 与 viewModelScope 调度器。
- 每个测试 `backgroundScope.launch { vm.state.collect {} }` 保活 `WhileSubscribed(5000)`。
- `vm.state.first { it.items.isNotEmpty() }` 等 suspending wait 替代 `advanceUntilIdle()` + `state.value`，确定性等待 Room Flow 落定。
- 注入真实 `EventRepository(db.eventDao())` + `NoteRepository(db.noteDao())`，非 fake——验证端到端 Flow 链路。

#### 新增测试文件

**1. `app/src/test/java/com/shijiben/feature/search/SearchViewModelTest.kt`**（Robolectric，方案 A）

| 测试方法名 | 验证点 |
|---|---|
| `emptyQuery_returnsRecentRecordsLimited` | 空查询时 `state.items` 非空，size <= `RECENT_LIMIT`(50)，按 `sortKey` 倒序 |
| `emptyQuery_mixesEventsAndNotes` | 空查询结果同时含 `EventItem` 与 `NoteItem`（混合） |
| `titleMatch_returnsEvent` | query 命中某事件 `title` → `state.items` 含对应 `EventItem` |
| `eventNoteMatch_returnsEvent` | query 命中某事件 `note`（非 title）→ `state.items` 含对应 `EventItem` |
| `noteContentMatch_returnsNote` | query 命中某随笔 `content` → `state.items` 含对应 `NoteItem` |
| `multiFieldMatch_returnsFromMultipleSources` | query 同时命中事件 title + 随笔 content → `state.items` 同时含 `EventItem` + `NoteItem` |
| `caseInsensitive_match` | query "ABC" 命中 title/content 含 "abc" / "ABC" / "Abc" 的记录 |
| `noMatch_emptyState` | query 不命中任何记录 → `state.items` 为空，`state.isEmpty` 为 true |
| `resultsSortedDescending` | 多条命中时按 `sortKey`（startTime/timestamp）倒序 |
| `queryChange_updatesResults` | 先输入 "x" 得结果，再改为 "y" → 结果随 query 变化 |
| `dbChange_updatesResults` | 插入新事件匹配当前 query → `state.items` 自动新增（Flow 响应式） |
| `queryWithSpaces_trimmedBeforeMatch` | query "  abc  " 等价 "abc" 匹配 |
| `specialChars_matchedLiterally` | query 含 "100%" / "a_b" / "\\" → 按字面量子串匹配（非 SQL LIKE 通配） |

**2. `app/src/test/java/com/shijiben/feature/search/SearchFilterTest.kt`**（纯 JUnit，无 Robolectric，测 `filterAndMerge` internal 函数）

| 测试方法名 | 验证点 |
|---|---|
| `emptyQuery_returnsRecentLimitedAndSorted` | 空查询返回 <= recentLimit 条，倒序 |
| `emptyQuery_takeRecentLimit` | 输入 > recentLimit 条 → 恰好 take recentLimit 条 |
| `titleMatch` | title contains query → 命中 |
| `eventNoteMatch` | note contains query → 命中（note null 不崩） |
| `noteContentMatch` | content contains query → 命中 |
| `caseInsensitive` | 大小写忽略 |
| `noMatch_emptyList` | 无命中 → 空 List |
| `sortedDescending` | 命中多条 → sortKey 倒序 |
| `specialCharsLiteral` | "100%" 等按字面量匹配 |
| `nullEventNoteHandled` | event.note = null 时 title 不匹配 → 不命中，不崩 NPE |

- 纯函数测试不依赖 Room/Robolectric，速度快；与 `EventRepositoryHeatmapTest` 测 `aggregateMonth` 同模式。

## 五、涉及文件清单

### 新增（4）

| 文件 | 用途 |
|---|---|
| `app/src/main/java/com/shijiben/feature/search/SearchViewModel.kt` | `@HiltViewModel` + `SearchItem` sealed interface + `SearchUiState` + `filterAndMerge` internal 纯函数 |
| `app/src/main/java/com/shijiben/feature/search/SearchScreen.kt` | UI：彩虹条 + 顶栏 + 搜索框 + LazyColumn 结果 + 空状态 + RecordingSheet/NoteEditorSheet overlay |
| `app/src/test/java/com/shijiben/feature/search/SearchViewModelTest.kt` | VM 测试（Robolectric 方案 A，13 用例） |
| `app/src/test/java/com/shijiben/feature/search/SearchFilterTest.kt` | 纯函数测试（JUnit，10 用例） |

### 改（2）

| 文件 | 改动 |
|---|---|
| `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` | ① 新增 `onSearchClick` 参数；② 顶栏热力图与设置方块间插入 26dp 放大镜像素方块；③ `NoteRow` 去 `private` 改 public（供 SearchScreen 复用） |
| `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | ① `Routes.SEARCH = "search"`；② `composable(Routes.SEARCH)`；③ timeline composable 传 `onSearchClick` |

### 复核无改动

- `app/build.gradle.kts`（零新依赖）
- `app/src/main/java/com/shijiben/data/local/AppDatabase.kt`（version 保持 2）
- `app/src/main/java/com/shijiben/data/local/EventDao.kt` / `NoteDao.kt`（不加查询）
- `app/src/main/java/com/shijiben/data/local/EventEntity.kt` / `NoteEntity.kt`（不改表）
- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` / `NoteRepository.kt`（不加方法）
- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt`（不改）
- `app/src/main/java/com/shijiben/feature/notes/NotesViewModel.kt`（不改，SearchScreen 注入复用）
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` / `RecordingSheet.kt`（不改，SearchScreen 复用）
- `app/src/main/java/com/shijiben/ui/theme/AppColors.kt`（不改，搜索框/图标全用现有色）
- `MainActivity.kt`（不改，`AppNavHost` 自动接新路由）
- 任何既有测试文件（不改）

## 六、边界情况

### 查询相关
- **空查询**：返回最近 50 条（事件+随笔混合，倒序）。`LazyColumn` 渲染，性能无忧。
- **超长 query**：`String.contains` 复杂度 O(n·m)，最坏 n=全量事件数、m=query 长度。本地 app 数据量级（千条），可接受；无长度限制。
- **特殊字符**：`%` `_` `\` `(` `)` 等按字面量子串匹配——非 SQL LIKE，无转义/通配语义。
- **前后空格**：`filterAndMerge` 内 `query.trim()` 后匹配；空格 only 的 query 等价空查询（返回最近 N）。
- **大小写**：`lowercase()` 后匹配，中英文均适用（中文 `lowercase()` 无变化）。
- **单字段 vs 多字段**：同一 query 命中事件 title + 事件 note + 随笔 content → 三类结果合并倒序；同一条事件 title 与 note 同时命中只算一条 `EventItem`（去重由 `filter` + `map` 自然保证）。

### 编辑后刷新
- **事件编辑保存**：`RecordingSheet` `onSaved` 关闭 sheet → Room update → `eventsFlow` re-emit → `combine` 重算 → `state.items` 自动反映新 title/note。无需 `refresh()` 调用。
- **随笔编辑保存/删除**：`notesViewModel.save/delete` → Room write → `notesFlow` re-emit → 同上自动刷新。
- **返回 SearchScreen**：独立 Screen 模式下，`SearchScreen` 未被销毁（仅 sheet overlay 关闭），`state` 订阅持续有效，结果即时更新。

### 事件与随笔混合排序
- `sortKey`：`EventItem.sortKey = event.startTime`，`NoteItem.sortKey = note.timestamp`。两者同为 epoch millis，直接 `sortedByDescending` 混排。
- 同一时刻事件与随笔：`sortedByDescending` 稳定排序保持插入顺序（事件先于随笔，因 `matchedEvents + matchedNotes` 拼接顺序）。

### in-progress 事件
- `EventCard` 对 status==1 显示「自 HH:mm + 运行中时长 badge」，`now` 用快照 `System.currentTimeMillis()`。搜索结果中 in-progress 事件时长为快照值，不实时刷新——可接受（搜索非主计时场景）。

### 空数据库
- 首次启动无任何记录：空查询 `state.items` 为空 → 显示「没有相关记录」（与有记录但无匹配的空状态文案相同，简化处理）。

## 七、测试清单

### 自动化（见 §4.5）
- `SearchViewModelTest`：13 用例（VM Flow 链路 + 8 项核心过滤逻辑 + 边界）
- `SearchFilterTest`：10 用例（纯函数过滤逻辑）

### 手动验证
1. 首页顶栏放大镜方块可见，26dp 2dp 黑边白底，与热力图/设置方块风格一致。
2. 点击放大镜进 `SearchScreen`，顶栏返回箭头可返回首页。
3. 空查询显示最近记录（事件+随笔混合，倒序），最多 50 条。
4. 输入关键词实时过滤，结果跟随输入变化（无需点搜索按钮）。
5. 输入不命中关键词 → 显示「没有相关记录」。
6. 大小写忽略：输入 "ABC" 能匹配 "abc"。
7. 点击事件结果 → `RecordingSheet` 弹出，预填事件信息；改 title 保存 → 返回列表结果更新。
8. 点击随笔结果 → `NoteEditorSheet` 弹出，预填内容；改内容保存 → 返回列表结果更新。
9. 删除随笔（在 `NoteEditorSheet` 内）→ 返回列表该随笔消失。
10. 事件 in-progress 状态在搜索结果中显示运行中 badge。
11. 搜索框 × 清除按钮可清空 query 回到最近记录。
12. `./gradlew assembleRelease --rerun-tasks` 通过（R8/ProGuard 不破坏 `filterAndMerge` internal 可见性）。

## 八、风险与备选

### R1: combine 时序抖动
- **风险**：`combine(eventsFlow, notesFlow, _query)` 三流，`eventsFlow`/`notesFlow` 各自 `stateIn(WhileSubscribed(5000))` 有 grace period。query 变化快时可能触发多次重算。
- **缓解**：`filterAndMerge` 是纯内存操作（无 IO），重算成本极低；`LazyColumn` diff 自动增量更新。无需 debounce。
- **备选**：若实测卡顿，加 `_query.debounce(150).distinctUntilChanged()` 再 combine。当前不做（过度设计）。

### R2: NoteRow 改 public 影响面
- **风险**：`TimelineScreen.NoteRow` 去 `private` 后，理论上任何模块可调。但项目无其他调用方，且 NoteRow 签名稳定（`note: NoteEntity, onClick: () -> Unit`）。
- **缓解**：可接受。若强求封装，可抽 `NoteRow` 到 `feature/notes/NoteRow.kt` 共享文件——但 Discover 报告判定范围 M，不引入文件移动，保持最小改动。
- **备选**：SearchScreen 自定义 `NoteRow`（复制 ~30 行）——代码重复但零改动 TimelineScreen。本 spec 选「改 public」因 Discover 报告明确「复用 NoteRow」。

### R3: RecordingSheet viewingDate 占位
- **风险**：`RecordingSheet(viewingDate = todayTriple(), editingEvent = ...)` 传占位日期。若 `editingEvent == null`（不应发生，搜索只编辑已存在事件）会走 `initNew()` 用 viewingDate 建新事件——意外创建。
- **缓解**：`SearchScreen` 中 `RecordingSheet` 仅在 `editingEvent != null` 时渲染（`if (showEventSheet && editingEvent != null)`），杜绝 `initNew()` 路径。
- **备选**：无。

### R4: Sheet 模式 vs 独立 Screen 模式
- **决策**：独立 `SearchScreen`（非首页 sheet overlay）。
- **理由**：搜索是独立场景，需全屏空间展示结果列表；与 `NotesScreen`/`HeatmapScreen` 路由模式一致；返回栈清晰（popBackStack 回首页）。
- **备选（未采）**：首页 `SearchSheet` ModalBottomSheet——省一个路由但挤压结果列表空间，且 sheet 内再叠 `RecordingSheet`/`NoteEditorSheet` 双层 sheet 体验差。

### R5: Icons.Default.Search 可用性
- **风险**：极低概率 `material-icons-core` 未含 `Search`（实际含，是核心图标）。
- **缓解**：若编译失败，退回 `Icons.Default.DateRange` 或自绘放大镜（`Canvas` 画圆+柄）。spec 默认用 `Icons.Default.Search`。

### R6: 全量 getAllEvents/getAllNotes 性能
- **风险**：用户数据量极大（万条+）时，`getAllEvents()` Flow 首次加载 + 内存过滤可能慢。
- **缓解**：本地 app 数据量级实际千条内；`LazyColumn` 虚拟化渲染；`filterAndMerge` 纯内存 O(n) 可接受。
- **备选（未采）**：DAO 层加 `LIKE` 查询下推 DB——但 Discover 决策明确「内存过滤，不加 DB 查询」，且 LIKE 无法跨 events+notes 两表联合，仍需内存合并。保持纯内存。
