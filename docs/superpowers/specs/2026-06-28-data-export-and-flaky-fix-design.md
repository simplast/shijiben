# 事记本 迭代 4 设计 spec：flaky test 修复（路径 A）+ 数据导出（JSON + SAF）

> 日期：2026-06-28
> 迭代：4（自迭代 loop）
> 范围：2 目标组合（A 修复 HeatmapViewModelTest flaky / B 数据导出 JSON + SAF）
> 前置：迭代 1（时间可视化）、迭代 2（热力图月视图）、迭代 3（设置页骨架 + 隐私政策 + backlog）已完成；SettingsScreen 已预留「数据导出」注释占位（`:81-82`）。
> 验证门：`./gradlew :app:compileDebugKotlin` + `./gradlew :app:testDebugUnitTest --rerun-tasks` + `./gradlew assembleDebug` 三道全绿，且 HeatmapViewModelTest 不再 flaky（连续 3 次 `--rerun-tasks` 稳定通过）。

---

## 1. 问题

### 1.1 HeatmapViewModelTest flaky

**现象**：`nextMonth_fromPreviousMonth_returnsToCurrent`、`goToCurrentMonth_fromPrevious_returnsToCurrent` 间歇性抛 `IllegalStateException at TestMainDispatcher.kt:67`。

**根因**（已核对源码）：
- `MainCoroutineRule`（`app/src/test/java/com/shijiben/feature/recording/MainCoroutineRule.kt:13-14`）默认 `UnconfinedTestDispatcher()`，被 4 个 VM 测试共用（Heatmap / TimeViz / Timeline / Recording）。
- `HeatmapViewModel.state`（`HeatmapViewModel.kt:39-51`）= `_selectedMonth.flatMapLatest { eventRepository.getDailyActivityForMonth(ym).map{...} }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HeatmapUiState())`。
- flaky 测试的触发序列：`previousMonth()` → `state.first{!isCurrentMonth}`（订阅+收集后取消订阅，WhileSubscribed 进入 5000ms grace）→ `nextMonth()`/`goToCurrentMonth()`（改 `_selectedMonth`，触发 `flatMapLatest` 取消旧内层流 + 启动新内层流）→ `state.first{isCurrentMonth}`（再次订阅）。
- 在 `UnconfinedTestDispatcher` 下，上述「取消旧流 + 启动新流 + WhileSubscribed 订阅抖动」是**重入式**执行的；连续两次 `_selectedMonth` 切换 + 中间订阅/取消，使 TestMainDispatcher 状态机在中途被再次进入，触发 `IllegalStateException`（line 67 附近的状态守卫）。

**已拍板修复路径 A**：仅 `HeatmapViewModelTest` 改用 `StandardTestDispatcher` + 显式 `advanceUntilIdle()`，**不改 `MainCoroutineRule.kt`**（避免影响另外 3 个稳定测试）。

### 1.2 数据导出缺失

- 用户数据全部本地化（events 表 / notes 表 / SharedPreferences 的生日与寿命），但**无任何导出能力**。卸载即失，无备份出口。
- 迭代 3 已在 `SettingsScreen.kt:81-82` 注释预留「数据导出」入口，本轮渲染并接通。

---

## 2. 目标

1. **A flaky 修复**：`HeatmapViewModelTest` 改 `StandardTestDispatcher` + `advanceUntilIdle()` 确定性 await，消除 `IllegalStateException`；其余 3 个 VM 测试零改动；`MainCoroutineRule.kt` 零改动。
2. **B 数据导出**：用 SAF（`ActivityResultContracts.CreateDocument("application/json")`）让用户选导出位置；用 Android 内置 `org.json` 序列化为 JSON；范围 = events + notes + TimeVizPrefs（生日/寿命）；文件名 `shijiben_backup_yyyyMMdd_HHmmss.json`；不联网、不引入新外部依赖、不改 DB schema（只读）。
3. **B 可测性**：JSON 序列化抽成纯函数 `DataExportManager.buildJsonString(...)`，单测覆盖边界；ExportViewModel 状态机（idle/exporting/success/error）可单测（注入 IO dispatcher）。
4. **B 美学与文案**：复用 `SettingsRow` 8-bit 范式；导出中行项置灰 + 「导出中...」尾标；成功/失败用 Snackbar（项目首次引入，免 Scaffold）；文案中性。

---

## 3. 非目标

- **不做导入**（下轮）：JSON 的 `schemaVersion` 仅为导出格式演进预留，本轮不写解析器。
- 不做 release 构建 / 应用图标 / Compose UI test。
- 不改 DB schema、Entity、Dao（只读 `getAllEvents()` / `getAllNotes()`）。
- 不引入新外部依赖：`org.json`（Android 内置）、`ActivityResultContracts`（androidx.activity 自带，已在 deps）、`SnackbarHost`（material3，已在 deps）。唯一新增内部接线是 `@IoDispatcher` 限定符 + 一条 `@Provides`（项目首个 dispatcher 注入，非外部依赖）。
- 不动 `MainCoroutineRule.kt`、不改另外 3 个 VM 测试。
- 不接 `BuildConfig.VERSION_NAME`（`buildFeatures.buildConfig` 未开），`appVersion` 硬编码 `"1.0"`（与 `build.gradle.kts:18` versionName 一致），注释标记下轮。
- 不解决 HeatmapViewModel 跨月停留自动刷新（迭代 3 已标注的局限，本轮不动）。

---

## 4. 设计

### 4.1 子项 A：flaky 修复（路径 A）

#### 4.1.1 关键发现

`MainCoroutineRule` 构造函数已声明 `dispatcher: TestDispatcher = UnconfinedTestDispatcher()`（`MainCoroutineRule.kt:13-14`）。因此「改用 StandardTestDispatcher」**不需要修改 rule 文件**，只需 `HeatmapViewModelTest` 实例化时传参：

```kotlin
import kotlinx.coroutines.test.StandardTestDispatcher

@get:Rule
val mainRule = MainCoroutineRule(StandardTestDispatcher())
```

- `Dispatchers.setMain(StandardTestDispatcher())` 由 rule 完成 → `viewModelScope`（`Dispatchers.Main.immediate`）跑在该 dispatcher 上。
- 另外 3 个 VM 测试继续用默认 `UnconfinedTestDispatcher()`，零影响。

#### 4.1.2 调度器统一（关键，否则会 hang）

`runTest { ... }` 默认为自己的 TestScope 创建**另一个** `StandardTestDispatcher`（独立调度器/队列）。若不统一，`state.first{...}` 挂起后，跑在 `Dispatchers.Main`（rule 的 dispatcher）上的上游流永远不会被 runTest 的调度器推进 → 死等。

**统一方式**：把 `mainRule.dispatcher` 作为 `runTest` 的 context 传入，使 TestScope 与 `Dispatchers.Main` 共用同一调度器/队列：

```kotlin
@Test
fun xxx() = runTest(mainRule.dispatcher) {
    ...
}
```

本文件**所有 8 个测试**的 `runTest {` 均改为 `runTest(mainRule.dispatcher) {`。

#### 4.1.3 确定性 await 模式（统一采用）

`state` 是 `WhileSubscribed(5000)`，无人订阅时上游不运行、`state.value` 为占位 `HeatmapUiState()`（空 cells）。为让 `state.value` 始终有意义，每个测试开头挂一个常驻收集者，随后用 `advanceUntilIdle()` 推进、用 `state.value` 断言：

```kotlin
@Test
fun xxx() = runTest(mainRule.dispatcher) {
    backgroundScope.launch { vm.state.collect {} }   // 常驻订阅，保活 WhileSubscribed
    advanceUntilIdle()                                // 让初始 state 落定
    // ... 操作 + advanceUntilIdle() + 断言 vm.state.value
}
```

- `backgroundScope` 由 `TestScope` 提供，测试结束自动取消，不泄漏。
- 该模式把「订阅抖动 + grace 计时 + flatMapLatest 切换」从重入式（Unconfined）改为队列串行化（Standard + advanceUntilIdle），从根上消除 `IllegalStateException`。

> 备选（不推荐）：保留 `state.first{...}`，仅换 dispatcher。`first` 在 StandardTestDispatcher + `runTest(mainRule.dispatcher)` 下通常也能完成（挂起后调度器推进上游），但 `first` 完成即取消订阅、引入 grace 抖动，对两个 flaky 测试仍属不确定序列。故统一采用「常驻收集者 + advanceUntilIdle + state.value」。

#### 4.1.4 逐测试改写

**两个 flaky 测试（完整新写）**：

```kotlin
@Test
fun nextMonth_fromPreviousMonth_returnsToCurrent() = runTest(mainRule.dispatcher) {
    backgroundScope.launch { vm.state.collect {} }
    advanceUntilIdle()
    vm.previousMonth()
    advanceUntilIdle()
    assertThat(vm.state.value.isCurrentMonth).isFalse()
    vm.nextMonth()
    advanceUntilIdle()
    assertThat(vm.state.value.yearMonth).isEqualTo(YearMonth.now(zone))
    assertThat(vm.state.value.canGoNext).isFalse()
}

@Test
fun goToCurrentMonth_fromPrevious_returnsToCurrent() = runTest(mainRule.dispatcher) {
    backgroundScope.launch { vm.state.collect {} }
    advanceUntilIdle()
    vm.previousMonth()
    advanceUntilIdle()
    assertThat(vm.state.value.isCurrentMonth).isFalse()
    vm.goToCurrentMonth()
    advanceUntilIdle()
    assertThat(vm.state.value.yearMonth).isEqualTo(YearMonth.now(zone))
    assertThat(vm.state.value.canGoNext).isFalse()
}
```

**非 flaky 测试（同模式适配，断言不变）**：

- `initialState_isCurrentMonth`：常驻订阅 + `advanceUntilIdle()` + 断言 `state.value` 的 yearMonth/isCurrentMonth/canGoNext。
- `previousMonth_decrementsAndEnablesNext`：订阅 + advance → `previousMonth()` + advance → 断言 `state.value`（minusMonths(1)、isCurrentMonth=false、canGoNext=true）。
- `nextMonth_atCurrentMonth_doesNotAdvance`：订阅 + advance → 记 `before = state.value` → `nextMonth()` + advance → 断言 `after.yearMonth == before.yearMonth` 且 `after.canGoNext == false`。
- `stateCells_shapeIs6x7AndTodayMarked`：订阅 + advance → 断言 `state.value.cells` 6×7、今日格 1 个且 date = today。
- `stateCells_updatesWhenRepoEmitsNewData`：订阅 + advance → `eventRepo.createEvent(...)` + advance → 断言 `state.value.cells` 今日格 level=1、durationMs=3600_000。

#### 4.1.5 import 增补

`HeatmapViewModelTest.kt` 加：`import kotlinx.coroutines.test.StandardTestDispatcher`、`import kotlinx.coroutines.launch`。`backgroundScope`/`advanceUntilIdle`/`runTest(context)` 均无需额外 import。

#### 4.1.6 非破坏性确认

- `MainCoroutineRule.kt` 不动 → TimeViz/Timeline/Recording 测试零影响。
- HeatmapViewModelTest 内所有测试断言语义与原一致，仅 await 方式从 `first{...}` 换为 `state.value`（常驻订阅保证其有效）。
- 验证门要求连续 3 次 `--rerun-tasks` 全绿以证非 flaky。

---

### 4.2 子项 B：数据导出架构

#### 4.2.1 分层

```
SettingsScreen (UI)
   ├─ rememberLauncherForActivityResult(CreateDocument("application/json"))
   │     → launcher.launch(fileName)
   │     → 回调 uri → contentResolver.openOutputStream(uri) → vm.export(stream)
   ├─ ExportViewModel (hiltViewModel)
   │     state: StateFlow<ExportState>  // Idle / Exporting / Success(fileName) / Error(msg)
   │     export(outputStream) / resetState()
   └─ DataExportManager (纯序列化 + IO 写入)
         buildJsonString(...)  // 纯函数，单测目标
```

数据收集（只读）：`EventRepository.getAllEvents().first()`、`NoteRepository.getAllNotes().first()`、`TimeVizPrefs.getBirthdayMillis()` / `getLifespanYears()`。

#### 4.2.2 DataExportManager（纯函数 + 薄 IO）

新增 `app/src/main/java/com/shijiben/data/export/DataExportManager.kt`：

```kotlin
package com.shijiben.data.export

import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream

object DataExportManager {

    /** 导出格式版本（独立于 Room DB version=2）。演进时递增。 */
    const val SCHEMA_VERSION = 1

    /**
     * 纯函数：把快照数据序列化为 JSON 字符串。无 Android 依赖（仅 org.json）。
     * 可空字段（endTime/note）传 null → JSONObject.put(key, null) → 序列化为 JSON null。
     */
    fun buildJsonString(
        events: List<EventEntity>,
        notes: List<NoteEntity>,
        birthdayMillis: Long,
        lifespanYears: Int,
        appVersion: String,
        exportedAt: Long
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", exportedAt)
        root.put("appVersion", appVersion)

        val eventsArr = JSONArray()
        for (e in events) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("title", e.title)
            o.put("startTime", e.startTime)
            o.put("endTime", e.endTime)       // Long? → null 安全
            o.put("status", e.status)
            o.put("note", e.note)             // String? → null 安全
            o.put("createdAt", e.createdAt)
            o.put("updatedAt", e.updatedAt)
            eventsArr.put(o)
        }
        root.put("events", eventsArr)

        val notesArr = JSONArray()
        for (n in notes) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("content", n.content)
            o.put("timestamp", n.timestamp)
            o.put("createdAt", n.createdAt)
            o.put("updatedAt", n.updatedAt)
            notesArr.put(o)
        }
        root.put("notes", notesArr)

        val prefs = JSONObject()
        prefs.put("birthdayMillis", birthdayMillis)
        prefs.put("lifespanYears", lifespanYears)
        root.put("timeVizPrefs", prefs)

        return root.toString()
    }

    /** 薄 IO 包装：UTF-8 写入流并关闭。失败抛异常由调用方 catch。 */
    fun writeToStream(json: String, out: OutputStream) {
        out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }
}
```

- `buildJsonString` 为单测主目标（边界：空数据 / 可空字段 / 特殊字符 / prefs 未设 / 大量数据 / 字段齐全）。
- `writeToStream` 薄包装，不单测（IO 边界）；ExportViewModelTest 用 `ByteArrayOutputStream` 验证写入内容。

#### 4.2.3 ExportViewModel（状态机）

新增 `app/src/main/java/com/shijiben/feature/settings/ExportViewModel.kt`：

```kotlin
package com.shijiben.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.export.DataExportManager
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.feature.timeviz.TimeVizPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val timeVizPrefs: TimeVizPrefs,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    sealed interface ExportState {
        data object Idle : ExportState
        data object Exporting : ExportState
        data class Success(val fileName: String) : ExportState
        data class Error(val message: String) : ExportState
    }

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun export(outputStream: OutputStream, fileName: String) {
        viewModelScope.launch {
            _state.value = ExportState.Exporting
            try {
                withContext(ioDispatcher) {
                    val events = eventRepository.getAllEvents().first()
                    val notes = noteRepository.getAllNotes().first()
                    val json = DataExportManager.buildJsonString(
                        events = events,
                        notes = notes,
                        birthdayMillis = timeVizPrefs.getBirthdayMillis(),
                        lifespanYears = timeVizPrefs.getLifespanYears(),
                        appVersion = APP_VERSION,
                        exportedAt = System.currentTimeMillis()
                    )
                    DataExportManager.writeToStream(json, outputStream)
                }
                _state.value = ExportState.Success(fileName)
            } catch (e: Exception) {
                _state.value = ExportState.Error("导出失败，请重试")
            }
        }
    }

    /** UI 消费 Success/Error 后调，回 Idle。 */
    fun resetState() { _state.value = ExportState.Idle }

    /** SAF openOutputStream 返回 null 等边界由 UI 直接触发 Error。 */
    fun markError(msg: String = "导出失败，请重试") {
        _state.value = ExportState.Error(msg)
    }

    companion object {
        // 与 build.gradle.kts versionName 一致；下轮接 BuildConfig.VERSION_NAME
        const val APP_VERSION = "1.0"
        private val FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        fun generateFileName(): String =
            "shijiben_backup_${LocalDateTime.now().format(FILE_FMT)}.json"
    }
}
```

要点：
- `@IoDispatcher` 注入：序列化 + 取数 + 写流在 IO 线程，生产 `Dispatchers.IO`，测试传 `UnconfinedTestDispatcher` → 确定性。
- 状态机：Idle → Exporting →（Success(fileName) | Error(msg)）→ resetState() → Idle。
- `outputStream` 由 UI 从 SAF Uri 开出后传入；写流失败（IOException 等）→ Error。
- `fileName` 由 UI 用 `generateFileName()` 生成并传入（与 SAF launcher 建议名一致），Success 文案引用它。

#### 4.2.4 @IoDispatcher 注入（项目首个 dispatcher 注入）

新增 `app/src/main/java/com/shijiben/di/DispatchersModule.kt`：

```kotlin
package com.shijiben.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

- 非外部依赖；仅为可测性把 `Dispatchers.IO` 抽出。不动 `DataModule`、不动 `TimeVizModule`。

#### 4.2.5 SettingsScreen 改动

`app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt`：

1. 签名加 ViewModel 默认参数（hiltViewModel 在 NavHost 内可用，AppNavHost 调用处无需改）：
   ```kotlin
   @Composable
   fun SettingsScreen(
       onBack: () -> Unit,
       onAboutClick: () -> Unit,
       viewModel: ExportViewModel = hiltViewModel()
   )
   ```

2. 顶部加 `val state by viewModel.state.collectAsStateWithLifecycle()`、`val context = LocalContext.current`、`val snackbarHostState = remember { SnackbarHostState() }`。

3. SAF launcher + pendingName（在 composable 顶层）。ExportViewModel 需补一个 `fun markError(msg: String = "导出失败，请重试") { _state.value = ExportState.Error(msg) }`，供 `openOutputStream` 返回 null 时触发：
   ```kotlin
   var pendingName by remember { mutableStateOf<String?>(null) }
   val launcher = rememberLauncherForActivityResult(
       ActivityResultContracts.CreateDocument("application/json")
   ) { uri ->
       val name = pendingName ?: ExportViewModel.generateFileName()
       if (uri != null) {
           val stream = context.contentResolver.openOutputStream(uri)
           if (stream != null) viewModel.export(stream, name)
           else viewModel.markError()
       }
       pendingName = null
   }
   ```
   要点：`pendingName` 在 onClick 时写入、launcher 回调里读取，保证 `launcher.launch(name)` 建议名与 `export(stream, name)` 传入名一致（避免两次 `generateFileName()` 产生不同时间戳）。

4. 行项渲染（替换 `:81-82` 注释）：
   ```kotlin
   SettingsRow(
       title = "数据导出",
       enabled = state !is ExportViewModel.ExportState.Exporting,
       onClick = {
           val name = ExportViewModel.generateFileName()
           pendingName = name
           launcher.launch(name)
       },
       trailing = {
           if (state is ExportViewModel.ExportState.Exporting) {
               Text("导出中...", fontSize = 12.sp, color = TextTertiary)
           } else {
               Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
           }
       }
   )
   ```

5. Snackbar 反馈（LaunchedEffect 消费一次性状态）：
   ```kotlin
   LaunchedEffect(state) {
       when (val s = state) {
           is ExportViewModel.ExportState.Success -> {
               snackbarHostState.showSnackbar("已导出到 ${s.fileName}")
               viewModel.resetState()
           }
           is ExportViewModel.ExportState.Error -> {
               snackbarHostState.showSnackbar(s.message)
               viewModel.resetState()
           }
           else -> {}
       }
   }
   ```

6. 在最外层 `Box`（既有 `:45`）底部放 SnackbarHost：
   ```kotlin
   SnackbarHost(
       hostState = snackbarHostState,
       modifier = Modifier.align(Alignment.BottomCenter)
   )
   ```

7. `SettingsRow` 签名扩展（`:109-141`）：
   ```kotlin
   @Composable
   private fun SettingsRow(
       title: String,
       onClick: () -> Unit,
       enabled: Boolean = true,
       trailing: @Composable (() -> Unit)? = null
   ) {
       Surface(
           color = if (enabled) SurfaceColor else Disabled,   // 导出中置灰
           shape = RoundedCornerShape(0.dp),
           border = androidx.compose.foundation.BorderStroke(2.dp, Color.Black),
           shadowElevation = 2.dp,
           modifier = Modifier
               .fillMaxWidth()
               .clickable(enabled = enabled, onClick = onClick)
       ) {
           Row(
               modifier = Modifier.fillMaxWidth().padding(12.dp),
               verticalAlignment = Alignment.CenterVertically,
               horizontalArrangement = Arrangement.SpaceBetween
           ) {
               Text(
                   text = title,
                   fontSize = 15.sp,
                   fontWeight = FontWeight.SemiBold,
                   color = if (enabled) TextPrimary else TextTertiary
               )
               if (trailing != null) trailing()
               else Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
           }
       }
   }
   ```
   既有两行（关于事记本 / 隐私政策）调用不传新参数，依赖默认值，零改动。`Disabled` 主题色由迭代 3 已加（`AppColors.kt`）。

#### 4.2.6 不需改 AppNavHost

`AppNavHost.kt:72-77` 的 `SettingsScreen(onBack=..., onAboutClick=...)` 调用不变（viewModel 默认参数）。hiltViewModel 在 `composable(Routes.SETTINGS){}` 内正常工作（与 HeatmapScreen/TimeVizScreen 同机制）。

#### 4.2.7 权限

SAF（`ACTION_CREATE_DOCUMENT`）不需任何声明权限，不需在 AndroidManifest 改动。

---

### 4.3 JSON Schema

```json
{
  "schemaVersion": 1,
  "exportedAt": 1719638400000,
  "appVersion": "1.0",
  "events": [
    {
      "id": 1,
      "title": "阅读",
      "startTime": 1719600000000,
      "endTime": null,
      "status": 1,
      "note": null,
      "createdAt": 1719600000000,
      "updatedAt": 1719600000000
    }
  ],
  "notes": [
    {
      "id": 1,
      "content": "今天的一些想法",
      "timestamp": 1719600000000,
      "createdAt": 1719600000000,
      "updatedAt": 1719600000000
    }
  ],
  "timeVizPrefs": {
    "birthdayMillis": 0,
    "lifespanYears": 80
  }
}
```

字段对齐：

| 字段 | 类型 | 来源 | 说明 |
|------|------|------|------|
| schemaVersion | Int | 常量 1 | 导出格式版本，**独立于 Room DB version(=2)**；演进递增 |
| exportedAt | Long | `System.currentTimeMillis()` | 导出时刻 |
| appVersion | String | 硬编码 "1.0" | 与 versionName 一致；下轮接 BuildConfig |
| events[].id | Long | EventEntity.id | |
| events[].title | String | EventEntity.title | |
| events[].startTime | Long | EventEntity.startTime | epoch millis |
| events[].endTime | Long? | EventEntity.endTime | null = 进行中 |
| events[].status | Int | EventEntity.status | 0=NotStarted,1=InProgress,2=Completed |
| events[].note | String? | EventEntity.note | |
| events[].createdAt | Long | | |
| events[].updatedAt | Long | | |
| notes[].id | Long | NoteEntity.id | |
| notes[].content | String | NoteEntity.content | |
| notes[].timestamp | Long | NoteEntity.timestamp | 当时的时刻戳 |
| notes[].createdAt | Long | | |
| notes[].updatedAt | Long | | |
| timeVizPrefs.birthdayMillis | Long | TimeVizPrefs.getBirthdayMillis() | 0 = 未设 |
| timeVizPrefs.lifespanYears | Int | TimeVizPrefs.getLifespanYears() | 默认 80 |

- 顶层字段顺序：`schemaVersion` 在最前，便于未来导入时先判版本。
- 可空字段用 JSON `null`（`org.json` `put(key, null)` → `JSONObject.NULL` → 序列化为 `null`）。

---

### 4.4 SAF 集成（不可单测，手动验证）

链路：`SettingsRow onClick` → `launcher.launch(fileName)` → 系统文件选择器 → 用户选位置 → 回调 `Uri` → `contentResolver.openOutputStream(uri)` → `vm.export(stream, fileName)` → VM 在 IO 线程序列化 + 写流 → `Success(fileName)` → Snackbar「已导出到 {fileName}」。

- `ActivityResultContracts.CreateDocument("application/json")` 固定 MIME；文件名由 `launcher.launch(name)` 建议，用户可改名（改名不影响导出，仅影响 Success 文案显示建议名——可接受）。
- `openOutputStream` 可能抛 `SecurityException`/`FileNotFoundException` → 被 VM `try/catch(Exception)` 兜为 `Error`。
- 不读 Uri、不持久化权限（一次性写入即可）。

---

### 4.5 测试边界

| 层 | 可单测 | 方式 |
|----|--------|------|
| `DataExportManager.buildJsonString` | ✅ | 纯函数，Robolectric 提供 `org.json`；用 `JSONObject(jsonStr)` 反解析断言字段 |
| `ExportViewModel` 状态机 + 写流 | ✅ | 注入内存 Room（EventRepository/NoteRepository）+ fake `TimeVizPrefs` + `@IoDispatcher` 传 `UnconfinedTestDispatcher`；`export(ByteArrayOutputStream, name)` 断言 Idle→Exporting→Success 且流内容 == `buildJsonString` 输出 |
| Repository 取数 | ✅（已有） | `EventRepositoryTest` / `NoteRepositoryTest` 已覆盖 `getAllEvents`/`getAllNotes` 范式，本轮不重复 |
| SAF launcher → Uri → contentResolver | ❌ | Robolectric 难模拟 `ActivityResultContracts.CreateDocument`；手动/device 验证 |

---

### 4.6 8-bit 美学

- 数据导出行项复用 `SettingsRow`（2dp 黑边白底直角 + 左标题 + 右尾标）。
- 导出中：行项 `Disabled` 底色 + 标题 `TextTertiary` + 不可点 + 尾标「导出中...」替换「›」。
- 不引入 `CircularProgressIndicator`（与项目像素风不一致），用文字「导出中...」表达。
- Snackbar：用 Material3 `SnackbarHost` 默认样式（圆角）。**已知 aesthetic 偏差**：Snackbar 圆角与全 app 直角 8-bit 风不完全一致，但它是瞬态反馈（2-3s 消失），引入自定义方形 Snackbar 成本高于收益，接受默认样式。若需严格一致，可后续把 Snackbar 容器套 2dp 黑边直角 `Surface`（非本轮目标）。
- `RainbowTrim`、顶栏、2dp 分隔线均不变。

---

### 4.7 文案（中性）

| 场景 | 文案 |
|------|------|
| 行项标题 | 数据导出 |
| 导出中尾标 | 导出中... |
| 成功 Snackbar | 已导出到 {fileName} |
| 失败 Snackbar | 导出失败，请重试 |
| 建议文件名 | shijiben_backup_yyyyMMdd_HHmmss.json |

不卖萌、不焦虑、不夸大。`fileName` 为完整文件名（含 `.json`），用户改名后 Success 文案仍显示建议名（可接受，因 VM 不感知最终文件名）。

---

## 5. 涉及文件清单

| 子项 | 文件 | 改动类型 |
|------|------|----------|
| A | `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt` | 改（rule 传 StandardTestDispatcher + runTest(mainRule.dispatcher) + 常驻收集者 + advanceUntilIdle + state.value；8 个测试统一模式） |
| B | `app/src/main/java/com/shijiben/data/export/DataExportManager.kt` | 新增 |
| B | `app/src/main/java/com/shijiben/feature/settings/ExportViewModel.kt` | 新增 |
| B | `app/src/main/java/com/shijiben/di/DispatchersModule.kt` | 新增（@IoDispatcher 限定符 + provideIoDispatcher） |
| B | `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` | 改（渲染数据导出行 + SAF launcher + Snackbar + SettingsRow 加 enabled/trailing + ViewModel 参数 + markError） |
| B-测试 | `app/src/test/java/com/shijiben/data/export/DataExportManagerTest.kt` | 新增 |
| B-测试 | `app/src/test/java/com/shijiben/feature/settings/ExportViewModelTest.kt` | 新增 |

**不改**：`MainCoroutineRule.kt`、`AppNavHost.kt`、`build.gradle.kts`、`AndroidManifest.xml`、所有 Entity/Dao/Repository、`TimeVizPrefs`、另外 3 个 VM 测试、`DataModule`、`TimeVizModule`。

---

## 6. 边界情况

1. **空数据库导出**：events=[] / notes=[] / birthdayMillis=0 → JSON 含空数组 + `timeVizPrefs.birthdayMillis=0, lifespanYears=80`。合法。
2. **可空字段**：`endTime=null`（进行中事件）、`note=null` → 序列化为 JSON `null`；反解析 `JSONObject.isNull("endTime")` 为 true。单测覆盖。
3. **特殊字符**：note/content 含 `"`、`\`、换行、emoji、中文、控制字符 → `org.json` 自动转义；单测用 `JSONObject(content)` 反解析比对。
4. **TimeVizPrefs 未设生日**：birthdayMillis=0，lifespanYears=80（默认）。JSON 如实记录 0/80，不臆造。
5. **大数据量**：本地个人时间记录量级（百~数千条），org.json 在 IO 线程构建为内存字符串，毫秒级；单测加 5000 条 perf smoke 兜底。极端量级（万+）内存占用可接受（纯本地、手动触发）。
6. **OutputStream 异常**：`openOutputStream` 抛异常 / 返回 null / 写入时 IOException → VM `catch(Exception)` → `Error("导出失败，请重试")`，状态回 Idle（经 resetState）。不写半截文件（org.json 先构完整字符串再一次性 `write(bytes)`，要么全写要么抛）。
7. **导出中重复点击**：行项 `enabled = state !is Exporting` → 不可再点；即便绕过，VM 内 `viewModelScope.launch` 串行（无并发写同流，最坏两次导出叠加，UI 已防）。
8. **SAF 用户取消**：launcher 回调 `uri == null` → 不调 `export`，状态保持 Idle，无 Snackbar。`pendingName` 清空。
9. **配置变更（旋转）**：`state` 是 StateFlow（VM 存活）；SAF launcher 由 `rememberLauncherForActivityResult` 管理，配置变更后 Activity Result 会恢复回调。导出中旋转 → VM 继续，UI 重订阅 state 显示 Exporting。
10. **flaky 修复跨月边界**：`currentMonth` 仍为计算属性（迭代 3），测试在 `runTest` 虚拟时间内瞬间完成，不跨真实自然月；`YearMonth.now(zone)` 在测试期内稳定。
11. **schemaVersion 与 Room version 区分**：JSON schemaVersion=1 是导出格式版本，Room DB version=2 是数据库版本，二者独立，spec/注释显式说明，避免下轮混淆。

---

## 7. 测试清单

### 7.1 验证门

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest --rerun-tasks`（连续 3 次全绿，证 HeatmapViewModelTest 非 flaky）
- `./gradlew assembleDebug`

### 7.2 HeatmapViewModelTest（改写，断言语义不变）

| 测试方法 | 改动 |
|----------|------|
| `initialState_isCurrentMonth` | runTest(mainRule.dispatcher) + 常驻订阅 + advanceUntilIdle + state.value |
| `previousMonth_decrementsAndEnablesNext` | 同上模式 |
| `nextMonth_fromPreviousMonth_returnsToCurrent`（flaky） | 完整重写（见 §4.1.4） |
| `nextMonth_atCurrentMonth_doesNotAdvance` | 同上模式 |
| `goToCurrentMonth_fromPrevious_returnsToCurrent`（flaky） | 完整重写（见 §4.1.4） |
| `stateCells_shapeIs6x7AndTodayMarked` | 同上模式 |
| `stateCells_updatesWhenRepoEmitsNewData` | 同上模式（createEvent 后 advanceUntilIdle 再断 state.value） |

### 7.3 DataExportManagerTest（新增，Robolectric）

| 测试方法 | 内容 |
|----------|------|
| `buildJsonString_emptyData_hasSchemaAndEmptyArrays` | 空 events/notes，birthdayMillis=0 → schemaVersion=1、exportedAt、appVersion 齐全，events/notes 为空数组，timeVizPrefs.birthdayMillis=0 |
| `buildJsonString_eventWithNullEndTimeAndNote_serializesNull` | event endTime=null、note=null → `JSONObject.isNull("endTime")` / `isNull("note")` 为 true |
| `buildJsonString_eventAllFields_roundTrip` | 完整 event 反解析逐字段比对（id/title/startTime/endTime/status/note/createdAt/updatedAt） |
| `buildJsonString_noteSpecialCharacters_escaped` | content 含 `"a\"b` `\n` emoji `中文` → 反解析 == 原文 |
| `buildJsonString_timeVizPrefsSet_storesValues` | birthdayMillis=非0、lifespanYears=99 → 反解析比对 |
| `buildJsonString_schemaVersionIsOne` | `JSONObject(json).getInt("schemaVersion") == 1` |
| `buildJsonString_largeDataset_completesUnder500ms` | 5000 events 构建耗时 < 500ms（perf smoke，宽松阈值防 CI 抖动） |

### 7.4 ExportViewModelTest（新增，Robolectric + MainCoroutineRule）

注入：内存 `AppDatabase` → `EventRepository` / `NoteRepository`；fake `TimeVizPrefs`（实现接口，可控 birthday/lifespan）；`@IoDispatcher` 传 `UnconfinedTestDispatcher()`（或 `mainRule.dispatcher`）。

| 测试方法 | 内容 |
|----------|------|
| `export_emptyData_transitionsToSuccessAndWritesValidJson` | 空库，`export(ByteArrayOutputStream, name)` → state 最终 Success(name)；流内容反解析为合法 JSON 且 events/notes 为空数组 |
| `export_withData_writesAllEventsAndNotes` | 预插 2 events + 1 note + 设 prefs → 流 JSON 反解析 events.size==2、notes.size==1、timeVizPrefs 值正确 |
| `export_streamThrows_transitionsToError` | 传入会抛 IOException 的 OutputStream（如 `object : OutputStream(){ override write(){ throw IOException() } }`）→ state Error("导出失败，请重试") |
| `export_setsExportingBeforeFinalState` | 在 Unconfined 下，先断言至少出现过 Exporting（用 `backgroundScope` 收集 state 序列，或断言最终非 Idle 且成功/失败） |
| `resetState_returnsToIdle` | 到 Success 后 `resetState()` → state Idle |

> ExportViewModel 用 `MainCoroutineRule()`（默认 Unconfined 即可，无 flatMapLatest/WhileSubscribed 问题）。`viewModelScope.launch` 在 Unconfined 下即时跑到 `withContext(ioDispatcher)`（ioDispatcher 传 UnconfinedTestDispatcher → 不切线程）→ 确定性。

### 7.5 手动验证（SAF，device）

1. 设置 → 数据导出 → 弹文件选择器，建议名 `shijiben_backup_yyyyMMdd_HHmmss.json`。
2. 选 Downloads/某目录 → Snackbar「已导出到 shijiben_backup_...json」。
3. 用文件管理器打开该 JSON：校验 schemaVersion/exportedAt/appVersion/events/notes/timeVizPrefs 结构与 DB 一致。
4. 空库导出：JSON 含空数组，不崩。
5. 取消选择器：无 Snackbar，无崩溃。
6. 导出中再点行项：置灰不可点。
7. 连续两次 `--rerun-tasks` HeatmapViewModelTest 全绿。

---

## 8. 风险与回退

1. **路径 A 隐性风险（非 flaky 测试 hang）**：StandardTestDispatcher + `runTest(mainRule.dispatcher)` 下，若某非 flaky 测试的 `state.value` 因订阅未保活而读到占位值 → 断言失败。回退：该测试补 `backgroundScope.launch{ vm.state.collect {} }`（§4.1.3 已统一采用，应已规避）。若仍 hang，检查 Room Flow 是否在 `allowMainThreadQueries` 下随 `advanceUntilIdle` 推进。
2. **路径 A 备选**：若 StandardTestDispatcher 反而引入新问题，回退到「保留 `state.first{...}` + 仅换 dispatcher + `runTest(mainRule.dispatcher)`」最小改法（§4.1.3 备选）。极端情况回退到 Unconfined + 在 flaky 测试加 `runCurrent()`/`yield()`——但此为最后手段，预期不需要。
3. **SAF 不可单测**：UI 链路仅手动验证，有回归盲区。缓解：ExportViewModel 注入 OutputStream + dispatcher，把可测逻辑剥离到 VM 层单测；UI 仅做 launcher→stream 接线（极薄）。
4. **JSON 大数据内存**：org.json 一次性构建字符串，极端量级（万+ events）内存峰值。本地个人量级安全；若反馈问题，下轮改 `JsonWriter` 流式写（org.json 无流式 API，需换 Moshi——违反零依赖，故本轮不做）。
5. **Snackbar 圆角偏差**：接受默认样式；若审美不可接受，回退方案用自定义 `Surface`(2dp 黑边直角) 包裹 Text 做瞬态浮层（非本轮目标）。
6. **@IoDispatcher 注入新增**：若 Hilt 编译报 `CoroutineDispatcher` 多绑定——不会，仅一条 `@IoDispatcher` 限定 provides。回退：若不想加 module，可让 ExportViewModel 不注入、硬编码 `Dispatchers.IO`，牺牲 VM 单测确定性（DataExportManager 纯函数单测仍保底）。
7. **appVersion 硬编码漂移**：硬编码 "1.0" 可能与未来 versionName 失步。下轮开 `buildConfig` 后接 `BuildConfig.VERSION_NAME`。本轮注释标记。

---

## 9. 附录：需 orchestrator 拍板的点

1. **Snackbar 默认圆角 vs 自定义方形**（§4.6）：默认 Snackbar 圆角与 8-bit 直角风有偏差。默认采用 Material3 默认 Snackbar（成本最低）；若要求严格 8-bit，需加自定义 `Surface` 包裹（约 +15 行，非本轮目标）。请确认接受默认。
2. **@IoDispatcher 注入是否可接受**（§4.2.4）：为 ExportViewModel 可单测，新增 `di/DispatchersModule.kt`（限定符 + 1 条 provides），非外部依赖。若希望零新增内部接线，回退为硬编码 `Dispatchers.IO` + 牺牲VM单测（仅保 DataExportManager 纯函数单测）。默认采用注入方案。
3. **appVersion 硬编码 "1.0"**（§4.2.3）：与 AboutScreen 显示的 "1.0.0" 不一致（AboutScreen 为展示串，JSON 为机器读 versionName）。本轮 JSON 用 "1.0"（对齐 build.gradle versionName），下轮统一接 BuildConfig。请确认接受此不一致或要求统一为 "1.0.0"。
4. **Success 文案显示建议文件名 vs 实际文件名**（§4.7）：VM 不感知用户在 SAF 里改的最终名，Snackbar 显示 `launcher.launch` 时的建议名。请确认可接受。
