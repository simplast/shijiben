# 事记本 迭代 8 设计 spec：数据导入（JSON + SAF，备份/恢复闭环）

> 日期：2026-06-28
> 迭代：8（自迭代 loop）
> 范围：1 目标（数据导入 JSON，反向解析迭代 4 的导出格式）
> 前置：迭代 4（数据导出 + flaky 修复）已完成；`DataExportManager` / `ExportViewModel` / `SettingsScreen` 数据导出行 / `@IoDispatcher` 注入 / `TimeVizPrefs` setter 均已就位。
> 验证门（四道，按顺序）：① `./gradlew :app:compileDebugKotlin` → ② `./gradlew :app:testDebugUnitTest --rerun-tasks` → ③ `./gradlew assembleDebug` → ④ `./gradlew :app:assembleRelease`，四道全绿。
> 当前质量评分：92/100；本轮目标 +1~2，完成备份/恢复闭环。

---

## 一、问题

迭代 4 已实现数据**导出**（`DataExportManager.buildJsonString` + `ExportViewModel` + SAF `CreateDocument` + SettingsScreen 数据导出行），用户可把本地快照（events + notes + TimeVizPrefs）序列化为 JSON 写入文件。

但**导入缺失**：用户无法把导出的 JSON 恢复回 app。卸载重装 / 换机（同 app 纯本地、不联网）后，备份文件无法回灌。备份/恢复不闭环，导出能力只完成一半。

本轮补上导入：反向解析迭代 4 的 v1 JSON，按原 ID 落库（冲突 REPLACE 覆盖），可选覆盖 TimeVizPrefs，闭环成立。

---

## 二、目标

1. **A DataImportManager 纯函数**：`parseJsonString(json: String): ImportResult` 纯函数（无 Android 依赖，仅 `org.json`），解析 v1 JSON、校验 `schemaVersion`、返回 `ImportResult`（events/notes/timeVizPrefs?）。单测主目标。
2. **A' DataImportManager 薄 IO**：`applyImport(result, eventRepo, noteRepo, timeVizPrefs): ImportCounts` suspend 函数，调 Repository upsert + TimeVizPrefs setter，返回导入计数；可注入 Repository 测试。
3. **B ImportViewModel 状态机**：`Idle / Importing / Success(events,notes,prefsUpdated) / Error(msg)`，复用 `ExportViewModel` 模式 + `@IoDispatcher` 注入；`import(inputStream)` + `resetState()` + `markError()`。
4. **C TimeVizPrefs setter**：**【复核无改动】** 读码发现 `TimeVizPrefs` 接口与 Impl **已声明并实现** `setBirthdayMillis` / `setLifespanYears`（`TimeVizPrefs.kt:13,19,38-47`）。本轮无需改动该文件，`applyImport` 直接调用既有 setter。
5. **D SettingsScreen 导入行 + SAF OpenDocument + 确认对话框 + Snackbar**：与「数据导出」行并列的「数据导入」行；点行 → 确认对话框 → SAF `OpenDocument("application/json")` → 选文件 → `contentResolver.openInputStream` → `vm.import(stream)` → 状态机 → Snackbar。复用 `SettingsRow` 模式与导出已引入的 `SnackbarHostState`。
6. **E 测试覆盖**：`DataImportManagerTest`（纯函数边界）+ `ImportViewModelTest`（状态机流转），现有测试在四道门下全绿无回归。

> 四个 orchestrator 决策已拍板（全部方案 A），详见 §四各子项标注。

---

## 三、非目标

- **不改 DB schema / Entity / Dao 字段语义**：复用既有 `insertEvent` / `insertNote`（已 `OnConflictStrategy.REPLACE`）；Dao 不新增方法（仅在 Repository 加薄包装 `upsertAll`，见 §4.5）。
- **不改导出格式**：严格反向解析迭代 4 的 v1 schema，不改 `DataExportManager` 的输出结构。
- **不做导入历史 / 增量 / 进度条 / 取消**：一次性整文件导入，本地数据量小瞬间完成。
- **不做 schemaVersion 迁移**：首版只支持 `schemaVersion == 1`，不匹配即 Error（未来版本演进时再做迁移器）。
- **绝对不联网**：仅 SAF 本地文件读取 + 本地 Room/SharedPreferences 写入。
- **不动其他 feature**：不改 Timeline / Heatmap / TimeViz / Recording / Notes / About；不改 `MainCoroutineRule`；不改 `AppNavHost`（`SettingsScreen` 调用签名兼容，新增 ViewModel 参数走默认值）；不改 `build.gradle.kts` / `AndroidManifest.xml`。
- **不引入新外部依赖**：仅用 Android 内置 `org.json` + 已在 deps 的 `ActivityResultContracts` / `material3`（`AlertDialog`/`TextButton` 已被 `TimelineScreen` 引入，非新依赖）。
- **不做 release 签名 / 应用图标 / Compose UI test**：SAF 链路手动验证。

---

## 四、设计

### 4.1 分层

```
SettingsScreen (UI)
   ├─ 导入行 onClick → showImportConfirm = true
   ├─ AlertDialog 确认 → importLauncher.launch(arrayOf("application/json"))
   ├─ rememberLauncherForActivityResult(OpenDocument())
   │     → 回调 uri → contentResolver.openInputStream(uri) → importVm.import(stream)
   ├─ ExportViewModel (既有) + ImportViewModel (新增, hiltViewModel)
   │     importVm.state: StateFlow<ImportState>  // Idle/Importing/Success(e,n,prefs)/Error(msg)
   │     importVm.import(stream) / resetState() / markError()
   └─ DataImportManager (纯解析 + 薄 IO)
         parseJsonString(json)  // 纯函数，单测目标
         applyImport(result, repos, prefs)  // 薄 IO suspend
         readFromStream(input)  // 薄 IO 包装
```

数据写入：`EventRepository.upsertAll(events)` / `NoteRepository.upsertAll(notes)`（新增薄包装，调既有 `insertEvent`/`insertNote`，REPLACE 覆盖同 ID）；`TimeVizPrefs.setBirthdayMillis` / `setLifespanYears`（既有）。

### 4.2 包路径决策

`DataImportManager` 放在 **`com.shijiben.data.export`** 包（与 `DataExportManager` 同包），而非新建 `data/import`（`import` 是 Kotlin 关键字，作包名需转义、丑陋）。二者构成备份/恢复对，共享 `DataExportManager.SCHEMA_VERSION` 常量（单一来源），同包最自然。不动 `DataExportManager` 文件本身。

### 4.3 子项 A：DataImportManager.parseJsonString 纯函数

新增 `app/src/main/java/com/shijiben/data/export/DataImportManager.kt`：

```kotlin
package com.shijiben.data.export

import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.feature.timeviz.TimeVizPrefs
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * 数据导入：把 v1 JSON 反向解析为本地快照并落库。与 DataExportManager 构成备份/恢复对。
 *
 * - 纯本地、不联网。
 * - 只读 JSON，写 Entity/Dao/Repository/TimeVizPrefs；不改 DB schema。
 * - 仅用 Android 内置 org.json，不引入新依赖。
 * - schemaVersion 校验：只支持 v1（= DataExportManager.SCHEMA_VERSION）；不匹配抛异常。
 */
object DataImportManager {

    /** 解析后的可选 TimeViz 偏好。null 表示 JSON 中缺失，导入时跳过不改现有。 */
    data class ImportedTimeVizPrefs(
        val birthdayMillis: Long,
        val lifespanYears: Int
    )

    /** parseJsonString 的纯数据产物。 */
    data class ImportResult(
        val schemaVersion: Int,
        val events: List<EventEntity>,
        val notes: List<NoteEntity>,
        val timeVizPrefs: ImportedTimeVizPrefs?
    )

    /** applyImport 的计数结果。 */
    data class ImportCounts(
        val eventsImported: Int,
        val notesImported: Int,
        val prefsUpdated: Boolean
    )

    /**
     * 纯函数：把 JSON 字符串解析为 ImportResult。无 Android 依赖（仅 org.json）。
     *
     * - 损坏 / 非 JSON / 空字符串 → 抛 JSONException（由 VM catch → Error）。
     * - schemaVersion 缺失或不等于 SCHEMA_VERSION → 抛 IllegalArgumentException。
     * - events / notes 数组缺失 → 视为空列表。
     * - timeVizPrefs 缺失 → null（导入跳过，不改现有）。
     * - endTime / note 字段：JSON null 或 key 缺失 → 实体 null（isNull 同时覆盖两种）。
     * - 其余必填字段（id/title/startTime/status/createdAt/updatedAt；content/timestamp）
     *   缺失 → 抛 JSONException（非标准文件 → Error，保证数据完整性）。
     */
    fun parseJsonString(json: String): ImportResult {
        val root = JSONObject(json)                       // 损坏/空 → JSONException
        val version = root.optInt("schemaVersion", -1)
        if (version != DataExportManager.SCHEMA_VERSION) {
            throw IllegalArgumentException("schemaVersion 不支持: $version")
        }
        val events = parseEvents(root.optJSONArray("events"))
        val notes = parseNotes(root.optJSONArray("notes"))
        val prefs = parsePrefs(root.optJSONObject("timeVizPrefs"))
        return ImportResult(version, events, notes, prefs)
    }

    private fun parseEvents(arr: JSONArray?): List<EventEntity> {
        if (arr == null) return emptyList()
        val out = ArrayList<EventEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                EventEntity(
                    id = o.getLong("id"),
                    title = o.getString("title"),
                    startTime = o.getLong("startTime"),
                    endTime = if (o.isNull("endTime")) null else o.getLong("endTime"),
                    status = o.getInt("status"),
                    note = if (o.isNull("note")) null else o.getString("note"),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt")
                )
            )
        }
        return out
    }

    private fun parseNotes(arr: JSONArray?): List<NoteEntity> {
        if (arr == null) return emptyList()
        val out = ArrayList<NoteEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out.add(
                NoteEntity(
                    id = o.getLong("id"),
                    content = o.getString("content"),
                    timestamp = o.getLong("timestamp"),
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt")
                )
            )
        }
        return out
    }

    private fun parsePrefs(o: JSONObject?): ImportedTimeVizPrefs? {
        if (o == null) return null
        return ImportedTimeVizPrefs(
            birthdayMillis = o.getLong("birthdayMillis"),
            lifespanYears = o.getInt("lifespanYears")
        )
    }

    /**
     * 薄 IO：把 ImportResult 落库。
     * - events/notes 用 Repository.upsertAll（保留原 ID，冲突 REPLACE 覆盖，幂等可重复导入）。
     * - timeVizPrefs 非空时覆盖现有生日/寿命（与导出对称）；null 时跳过不改。
     * 返回导入计数。
     */
    suspend fun applyImport(
        result: ImportResult,
        eventRepository: EventRepository,
        noteRepository: NoteRepository,
        timeVizPrefs: TimeVizPrefs
    ): ImportCounts {
        eventRepository.upsertAll(result.events)
        noteRepository.upsertAll(result.notes)
        var prefsUpdated = false
        result.timeVizPrefs?.let {
            timeVizPrefs.setBirthdayMillis(it.birthdayMillis)
            timeVizPrefs.setLifespanYears(it.lifespanYears)
            prefsUpdated = true
        }
        return ImportCounts(
            eventsImported = result.events.size,
            notesImported = result.notes.size,
            prefsUpdated = prefsUpdated
        )
    }

    /** 薄 IO 包装：UTF-8 读取流并关闭。失败抛异常由 VM catch。 */
    fun readFromStream(input: InputStream): String =
        input.use { it.readBytes().toString(Charsets.UTF_8) }
}
```

要点：
- `parseJsonString` 为单测主目标（无 Android 依赖，Robolectric 仅提供 `org.json`）。
- `applyImport` 是 suspend 薄 IO，注入 Repository + TimeVizPrefs，可注入内存 Room + fake prefs 测试。
- `readFromStream` 薄包装，不单测（IO 边界）；VM 测试用 `ByteArrayInputStream` 验证读取链路。
- ID 冲突由 DAO 既有 `OnConflictStrategy.REPLACE` 处理（`EventDao.kt:31` / `NoteDao.kt:22`），幂等可重复导入。

### 4.4 子项 B：ImportViewModel 状态机

新增 `app/src/main/java/com/shijiben/feature/settings/ImportViewModel.kt`（与 `ExportViewModel.kt` 同包、同模式）：

```kotlin
package com.shijiben.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.export.DataImportManager
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.di.IoDispatcher
import com.shijiben.feature.timeviz.TimeVizPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

/**
 * 数据导入 ViewModel。负责把用户通过 SAF 选定的输入流读为 JSON、解析、落库。
 *
 * - 状态机：Idle → Importing →（Success(events,notes,prefsUpdated) | Error(msg)）→ resetState() → Idle。
 * - 读流 + 解析 + 落库在 [ioDispatcher] 上执行；生产 Dispatchers.IO，测试注入 StandardTestDispatcher。
 * - 不联网、不读 Uri（inputStream 由 UI 从 SAF Uri 开出后传入）。
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val timeVizPrefs: TimeVizPrefs,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    sealed interface ImportState {
        data object Idle : ImportState
        data object Importing : ImportState
        data class Success(val events: Int, val notes: Int, val prefsUpdated: Boolean) : ImportState
        data class Error(val message: String) : ImportState
    }

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun import(inputStream: InputStream) {
        viewModelScope.launch {
            _state.value = ImportState.Importing
            try {
                val counts = withContext(ioDispatcher) {
                    val json = DataImportManager.readFromStream(inputStream)
                    val result = DataImportManager.parseJsonString(json)
                    DataImportManager.applyImport(result, eventRepository, noteRepository, timeVizPrefs)
                }
                _state.value = ImportState.Success(
                    events = counts.eventsImported,
                    notes = counts.notesImported,
                    prefsUpdated = counts.prefsUpdated
                )
            } catch (e: Exception) {
                _state.value = ImportState.Error("导入失败，请重试")
            }
        }
    }

    /** UI 消费 Success/Error 后调，回 Idle。 */
    fun resetState() { _state.value = ImportState.Idle }

    /** SAF openInputStream 返回 null 等边界由 UI 直接触发 Error。 */
    fun markError(msg: String = "导入失败，请重试") {
        _state.value = ImportState.Error(msg)
    }
}
```

要点：
- 与 `ExportViewModel` 完全对称：同 `@HiltViewModel` + 同四构造参（eventRepo/noteRepo/timeVizPrefs/ioDispatcher），同 sealed state，同 `resetState()`/`markError()`。
- `Success` 携带计数（events/notes/prefsUpdated），供 Snackbar 文案展示。
- `try/catch(Exception)` 兜底：`JSONException`（损坏 JSON）/ `IllegalArgumentException`（schemaVersion）/ `IOException`（读流）/ 任何 DB 异常 → 统一 `Error("导入失败，请重试")`。

### 4.5 子项：Repository 加 upsertAll 薄包装（保留原 ID）

`EventRepository.createEvent`（`EventRepository.kt:37-56`）构造 `EventEntity(id=0,...)` 走 autoGenerate，**不保留导入的原 ID**。导入需按原 ID 落库以触发 REPLACE。故各加一条薄包装，直接调既有 `insertEvent`/`insertNote`（已 REPLACE），传入完整实体（含导入 id）。

**改前** `EventRepository.kt`（`createEvent` 之后，`updateEvent` 之前，约 `:57`）无 upsert 方法。

**改后**（在 `createEvent` 后插入）：

```kotlin
    /** 导入用：按原 ID 批量 upsert（Dao OnConflictStrategy.REPLACE 覆盖同 ID）。保留原 id，幂等可重复导入。 */
    suspend fun upsertAll(events: List<EventEntity>) {
        for (e in events) eventDao.insertEvent(e)
    }
```

**改前** `NoteRepository.kt`（`createNote` 之后，`updateNote` 之前，约 `:30`）无 upsert 方法。

**改后**（在 `createNote` 后插入）：

```kotlin
    /** 导入用：按原 ID 批量 upsert（Dao OnConflictStrategy.REPLACE 覆盖同 ID）。保留原 id，幂等可重复导入。 */
    suspend fun upsertAll(notes: List<NoteEntity>) {
        for (n in notes) noteDao.insertNote(n)
    }
```

- 循环单条插入而非 DAO 批量 `@Insert(List)`：避免改 Dao（最小改动），本地个人量级（百~千条）毫秒级，性能足够。
- 不改 Dao、不改 schema、不改 Entity。

### 4.6 子项 C：TimeVizPrefs setter —— 复核无改动

读码确认 `TimeVizPrefs.kt`：
- 接口已声明 `setBirthdayMillis(millis: Long)`（`:13`）与 `setLifespanYears(years: Int)`（`:19`）。
- `TimeVizPrefsImpl` 已实现（`:38-40` / `:45-47`，`prefs.edit().putLong/putInt().apply()`）。

**本轮不改该文件**。`DataImportManager.applyImport` 直接调用既有 setter 覆盖现有生日/寿命（orchestrator 决策 2：方案 A 导入覆盖；JSON 中缺失时 `ImportedTimeVizPrefs` 为 null → 跳过不改）。

> 注：迭代 4 的导出 spec 曾把 setter 列为「下轮要做」，实际迭代 4 一并落地了 setter。本轮无需重复劳动。

### 4.7 子项 D：SettingsScreen 导入行 + SAF OpenDocument + 确认对话框 + Snackbar

`app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` 改动。

**orchestrator 决策 3（方案 A）：导入入口 = 设置页「数据导入」行，与「数据导出」并列。**
**orchestrator 决策 4（方案 A）：有确认对话框（"导入将覆盖同 ID 数据，确认？"）。**

#### 4.7.1 签名加 ImportViewModel 默认参数（AppNavHost 调用不变）

**改前**（`SettingsScreen.kt:57-61`）：
```kotlin
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel()
) {
```

**改后**：
```kotlin
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onAboutClick: () -> Unit,
    viewModel: ExportViewModel = hiltViewModel(),
    importViewModel: ImportViewModel = hiltViewModel()
) {
```

#### 4.7.2 收集 importState（在 `:62` state 收集之后加）

**改前**（`:62`）：
```kotlin
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
```

**改后**：
```kotlin
    val state by viewModel.state.collectAsStateWithLifecycle()
    val importState by importViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
```

#### 4.7.3 SAF OpenDocument launcher + 确认对话框状态（在导出 launcher `:69-79` 之后加）

导出用 `CreateDocument`，导入用 `OpenDocument()`（对称）。`OpenDocument.launch(Array<String>)` 传 MIME 数组。

**新增**（紧随导出 `launcher`/`pendingName` 块之后）：
```kotlin
    // 导入：OpenDocument 选文件 → openInputStream → importVm.import(stream)
    var showImportConfirm by remember { mutableStateOf(false) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val stream = context.contentResolver.openInputStream(uri)
            if (stream != null) importViewModel.import(stream)
            else importViewModel.markError()
        }
    }
```

要点：
- `OpenDocument()` 不需任何 manifest 权限（SAF，与 `CreateDocument` 同机制）。
- `openInputStream` 返回 null 或抛异常 → `markError()`；读流/解析/落库异常由 VM 内 `catch` 兜为 Error。
- `uri == null`（用户取消选择器）→ 无操作，无 Snackbar。

#### 4.7.4 消费 importState 一次性状态（在导出 `LaunchedEffect(state)` `:82-94` 之后加）

**新增**：
```kotlin
    LaunchedEffect(importState) {
        when (val s = importState) {
            is ImportViewModel.ImportState.Success -> {
                val msg = buildString {
                    append("已导入 ${s.events} 条事件、${s.notes} 条笔记")
                    if (s.prefsUpdated) append("（含偏好）")
                }
                snackbarHostState.showSnackbar(msg)
                importViewModel.resetState()
            }
            is ImportViewModel.ImportState.Error -> {
                snackbarHostState.showSnackbar(s.message)
                importViewModel.resetState()
            }
            else -> {}
        }
    }
```

复用导出已引入的同一个 `snackbarHostState`（瞬态反馈，导出/导入不会同时进行）。

#### 4.7.5 导入行（与导出行并列，紧随导出行 `:132-147` 之后）

**新增**（在导出 `SettingsRow(...)` 闭合括号之后，Column 内）：
```kotlin
                SettingsRow(
                    title = "数据导入",
                    enabled = state !is ExportViewModel.ExportState.Exporting &&
                        importState !is ImportViewModel.ImportState.Importing,
                    onClick = { showImportConfirm = true },
                    trailing = {
                        if (importState is ImportViewModel.ImportState.Importing) {
                            Text("导入中...", fontSize = 12.sp, color = TextTertiary)
                        } else {
                            Text("›", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextTertiary)
                        }
                    }
                )
```

要点：
- 点行不直接启动 SAF，而是 `showImportConfirm = true` 弹确认对话框（决策 4）。
- `enabled` 同时受导出/导入进行中态约束，避免并发。
- 复用 `SettingsRow`（`SettingsScreen.kt:179-209`）的 8-bit 2dp 黑边白底直角范式，与导出行视觉一致。

#### 4.7.6 确认对话框（在最外层 `Box` `:96` 内，与 `SnackbarHost` `:151` 同层）

`AlertDialog` 已被 `TimelineScreen.kt:34,350` 引入，非项目首用。复用其范式（`onDismissRequest`/`title`/`text`/`confirmButton`/`dismissButton`）。

**新增**（在 `SnackbarHost(...)` 之前或之后，同 Box 内）：
```kotlin
        if (showImportConfirm) {
            AlertDialog(
                onDismissRequest = { showImportConfirm = false },
                title = { Text("导入数据？", color = TextPrimary) },
                text = { Text("导入将覆盖同 ID 数据，确认？", color = TextSecondary) },
                confirmButton = {
                    TextButton(onClick = {
                        showImportConfirm = false
                        importLauncher.launch(arrayOf("application/json"))
                    }) { Text("导入", color = Error) }
                },
                dismissButton = {
                    TextButton(onClick = { showImportConfirm = false }) {
                        Text("取消", color = TextSecondary)
                    }
                }
            )
        }
```

- 确认按钮「导入」用 `Error` 色（覆盖操作具破坏性，与 `TimelineScreen` 删除确认同语义），取消用 `TextSecondary`。
- 确认 → 关弹窗 → `importLauncher.launch(arrayOf("application/json"))`；取消 / 点外区 → `showImportConfirm = false`，无后续操作。

#### 4.7.7 新增 import（顶部 import 区）

`SettingsScreen.kt` 顶部需补：
- `import androidx.compose.material3.AlertDialog`
- `import androidx.compose.material3.TextButton`
- `import com.shijiben.ui.theme.Error`
- `import com.shijiben.ui.theme.TextSecondary`

（`TextPrimary`/`TextTertiary`/`Surface`/`Background`/`Disabled` 已在 `:42-46` 导入；`Error`/`TextSecondary` 在 `AppColors` 已定义，被 `TimelineScreen` 使用。）

### 4.8 子项 F：导入流程时序

```
用户点「数据导入」行
   → showImportConfirm = true（弹 AlertDialog）
用户点「导入」确认
   → showImportConfirm = false
   → importLauncher.launch(arrayOf("application/json"))（SAF OpenDocument 选文件器）
用户选文件
   → 回调 uri
   → context.contentResolver.openInputStream(uri)
   → stream != null → importViewModel.import(stream)
        → VM: _state = Importing
        → withContext(ioDispatcher):
              json = DataImportManager.readFromStream(stream)      // UTF-8 读流
              result = DataImportManager.parseJsonString(json)     // 纯解析 + 校验 schemaVersion
              counts = DataImportManager.applyImport(result, eventRepo, noteRepo, prefs)
                    → eventRepo.upsertAll(events)   // REPLACE 覆盖同 ID
                    → noteRepo.upsertAll(notes)     // REPLACE 覆盖同 ID
                    → prefs?.let { timeVizPrefs.setBirthday/setLifespan }  // 覆盖或跳过
        → _state = Success(events, notes, prefsUpdated)
   → LaunchedEffect(importState) 消费 → Snackbar「已导入 N 条事件、M 条笔记（含偏好）」→ resetState()
任一步异常（损坏 JSON / schemaVersion 不匹配 / 空文件 / IO / DB）
   → VM catch(Exception) → _state = Error("导入失败，请重试")
   → LaunchedEffect → Snackbar「导入失败，请重试」→ resetState()
用户取消 SAF 选文件（uri == null）
   → 无操作，无 Snackbar
用户取消确认对话框（点取消 / 点外区）
   → showImportConfirm = false，无操作
```

### 4.9 JSON Schema（反向解析目标，与迭代 4 导出严格对称）

```json
{
  "schemaVersion": 1,
  "exportedAt": 1719638400000,
  "appVersion": "1.0",
  "events": [
    { "id": 1, "title": "阅读", "startTime": 1719600000000, "endTime": null,
      "status": 1, "note": null, "createdAt": 1719600000000, "updatedAt": 1719600000000 }
  ],
  "notes": [
    { "id": 1, "content": "今天的一些想法", "timestamp": 1719600000000,
      "createdAt": 1719600000000, "updatedAt": 1719600000000 }
  ],
  "timeVizPrefs": { "birthdayMillis": 0, "lifespanYears": 80 }
}
```

解析字段对照（与导出 §4.3 表逐字段对称）：

| 字段 | 解析方式 | 缺失行为 |
|------|----------|----------|
| schemaVersion | `optInt("schemaVersion", -1)`，≠1 抛 IllegalArgumentException | 缺失 → -1 → 抛 |
| exportedAt | 不解析（仅元数据） | 忽略 |
| appVersion | 不解析（仅元数据） | 忽略 |
| events[] | `optJSONArray` → 逐项 `getLong/getString/getInt` | 数组缺失 → 空列表 |
| events[].endTime | `isNull("endTime") ? null : getLong` | null 或缺失 → null |
| events[].note | `isNull("note") ? null : getString` | null 或缺失 → null |
| events[].{id,title,startTime,status,createdAt,updatedAt} | 严格 `getLong/getString/getInt` | 缺失 → JSONException |
| notes[] | `optJSONArray` → 逐项 | 数组缺失 → 空列表 |
| notes[].{id,content,timestamp,createdAt,updatedAt} | 严格 getter | 缺失 → JSONException |
| timeVizPrefs | `optJSONObject` → null 则跳过 | 缺失 → null，不改现有 |
| timeVizPrefs.{birthdayMillis,lifespanYears} | `getLong/getInt` | prefs 在但子字段缺失 → JSONException |

---

## 五、涉及文件清单

| 类别 | 文件 | 改动 |
|------|------|------|
| 新增 | `app/src/main/java/com/shijiben/data/export/DataImportManager.kt` | 纯函数 parseJsonString + 薄 IO applyImport/readFromStream + ImportResult/ImportCounts/ImportedTimeVizPrefs data class |
| 新增 | `app/src/main/java/com/shijiben/feature/settings/ImportViewModel.kt` | 状态机 Idle/Importing/Success/Error，import/resetState/markError |
| 新增 | `app/src/test/java/com/shijiben/data/export/DataImportManagerTest.kt` | 纯函数边界单测 |
| 新增 | `app/src/test/java/com/shijiben/feature/settings/ImportViewModelTest.kt` | 状态机流转单测 |
| 改 | `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` | 加 `upsertAll(events)` 薄包装（调既有 insertEvent，REPLACE） |
| 改 | `app/src/main/java/com/shijiben/data/repository/NoteRepository.kt` | 加 `upsertAll(notes)` 薄包装（调既有 insertNote，REPLACE） |
| 改 | `app/src/main/java/com/shijiben/feature/settings/SettingsScreen.kt` | 加 ImportViewModel 参数 + importState 收集 + OpenDocument launcher + 确认对话框 + 导入行 + importState LaunchedEffect + 顶部 import |

**复核无改动**（读码确认已具备所需能力）：

| 文件 | 原因 |
|------|------|
| `app/src/main/java/com/shijiben/feature/timeviz/TimeVizPrefs.kt` | setter 已存在（`:13,19,38-47`），applyImport 直接调用 |
| `app/src/main/java/com/shijiben/data/local/EventDao.kt` / `NoteDao.kt` | `insertEvent`/`insertNote` 已 `OnConflictStrategy.REPLACE`（`EventDao.kt:31` / `NoteDao.kt:22`），无需加方法 |
| `app/src/main/java/com/shijiben/data/local/EventEntity.kt` / `NoteEntity.kt` | 字段齐全，构造即可 |
| `app/src/main/java/com/shijiben/data/export/DataExportManager.kt` | 复用 `SCHEMA_VERSION` 常量，不改 |
| `app/src/main/java/com/shijiben/di/DispatchersModule.kt` | `@IoDispatcher` 已提供，ImportViewModel 复用注入 |
| `app/src/main/java/com/shijiben/feature/settings/ExportViewModel.kt` | 不动 |
| `app/src/test/java/com/shijiben/feature/recording/MainCoroutineRule.kt` | 不动 |
| `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | `SettingsScreen` 调用签名兼容（新参数默认值） |
| `app/build.gradle.kts` / `AndroidManifest.xml` | 无新依赖/权限（SAF + AlertDialog 均已在 deps） |

---

## 六、边界情况

1. **损坏 JSON / 非 JSON 文件** → `JSONObject(json)` 抛 `JSONException` → VM `catch` → `Error("导入失败，请重试")` → Snackbar。单测覆盖。
2. **schemaVersion 不匹配**（含缺失） → `optInt` 得 -1 或他值 → `IllegalArgumentException` → Error。单测覆盖（不匹配 / 缺失两例）。
3. **events / notes 数组缺失** → `optJSONArray` 返回 null → 视为空列表导入（`ImportResult.events/notes = emptyList`）。单测覆盖。
4. **timeVizPrefs 缺失** → `optJSONObject` 返回 null → `ImportResult.timeVizPrefs = null` → `applyImport` 跳过 setter，不改现有。单测覆盖。
5. **ID 冲突** → 既有 `OnConflictStrategy.REPLACE` 覆盖同 ID 行；幂等可重复导入同一文件。单测覆盖（导入同 ID 两次，最终行数 = 1 且字段为后导入值）。
6. **空文件** → `JSONObject("")` 抛 `JSONException` → Error。单测覆盖（`parseJsonString("")` 抛）。
7. **大文件 / 性能** → 本地个人时间记录量级（百~千条），`org.json` 一次性解析为内存对象毫秒级；导入瞬间完成，不做进度条。沿用迭代 4 的 perf smoke 阈值思路。
8. **用户取消 SAF 选文件** → launcher 回调 `uri == null` → 不调 `import`，状态保持 Idle，无 Snackbar。
9. **用户取消确认对话框**（点取消 / 点外区 / 返回） → `showImportConfirm = false`，不启动 SAF，无操作。
10. **openInputStream 返回 null / 抛异常** → UI 调 `markError()` → Error → Snackbar。
11. **必填字段缺失**（如 event 缺 title） → `getString("title")` 抛 `JSONException` → Error（非标准文件，拒绝导入以保证完整性）。单测覆盖。
12. **可空字段缺失**（endTime/note key 不存在） → `isNull("endTime")` 对缺失 key 也返回 true → 实体 null（健壮，兼容手写 JSON）。
13. **导入中重复点击** → 导入行 `enabled = ... && importState !is Importing` → 不可再点；导出行同理受约束，避免导出/导入并发。
14. **配置变更（旋转）** → `state` 是 StateFlow（VM 存活）；SAF launcher 由 `rememberLauncherForActivityResult` 管理，配置变更后 Activity Result 恢复回调；导入中旋转 → VM 继续，UI 重订阅 state 显示 Importing。
15. **schemaVersion 与 Room version 区分** → JSON schemaVersion=1（导出格式版本），Room DB version=2（数据库版本），二者独立；本轮只支持 v1，未来导出格式演进时递增 SCHEMA_VERSION 并在此加迁移。

---

## 七、测试清单

### 7.1 验证门（四道，按顺序）

1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:testDebugUnitTest --rerun-tasks`（含新增测试 + 现有测试全绿无回归）
3. `./gradlew assembleDebug`
4. `./gradlew :app:assembleRelease`

### 7.2 DataImportManagerTest（新增，Robolectric，纯函数）

`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk=[33])`，对齐 `DataExportManagerTest` 范式。用 `DataExportManager.buildJsonString` 产出合法 JSON 做反向解析，保证 round-trip 对称。

| 测试方法 | 内容 |
|----------|------|
| `parseJsonString_validFullJson_returnsAllFields` | 用 buildJsonString 产合法 JSON → parse → events/notes/prefs 字段逐项比对（含非空 endTime/note） |
| `parseJsonString_nullEndTimeAndNote_parsedAsNull` | event endTime=null/note=null → parse → 实体对应字段 null |
| `parseJsonString_eventsMissing_returnsEmptyEvents` | 删 events key → parse → events=emptyList，notes 正常 |
| `parseJsonString_notesMissing_returnsEmptyNotes` | 删 notes key → parse → notes=emptyList |
| `parseJsonString_timeVizPrefsMissing_returnsNullPrefs` | 删 timeVizPrefs key → parse → timeVizPrefs=null |
| `parseJsonString_corruptJson_throwsJSONException` | 传 `"not a json"` → assertThrows JSONException |
| `parseJsonString_emptyString_throwsJSONException` | 传 `""` → assertThrows JSONException（空文件） |
| `parseJsonString_schemaVersionMismatch_throwsIllegalArgumentException` | 改 schemaVersion=2 → assertThrows IllegalArgumentException |
| `parseJsonString_schemaVersionMissing_throwsIllegalArgumentException` | 删 schemaVersion key → assertThrows IllegalArgumentException |
| `parseJsonString_partialFieldMissing_throwsJSONException` | event 缺 title → assertThrows JSONException |
| `parseJsonString_specialCharacters_roundTrip` | note content 含 `"a\"b\nc🎉中文` → export→parse → 等于原文 |
| `parseJsonString_roundTripWithExport_symmetric` | 构造 events+notes+prefs → buildJsonString → parse → 结果与原实体逐字段相等 |

### 7.3 ImportViewModelTest（新增，Robolectric + MainCoroutineRule）

对齐 `ExportViewModelTest` 范式：`MainCoroutineRule(StandardTestDispatcher())` + `runTest(mainRule.dispatcher)` + 内存 Room + `FakeTimeVizPrefs` + `@IoDispatcher` 传 `mainRule.dispatcher`。用 `vm.state.first { it is Success || it is Error }` 等待终态（Room Flow 在 Room executor 异步执行，`advanceUntilIdle` 不等 Room 线程，沿用 ExportViewModelTest 的 suspending wait 模式）。

| 测试方法 | 内容 |
|----------|------|
| `import_validData_transitionsToSuccessAndPersists` | 预构 JSON（含 2 events + 1 note + prefs）→ `import(ByteArrayInputStream(json))` → state Success(2,1,true)；落库后 `eventRepo.getAllEvents().first().size==2`、`noteRepo.getAllNotes().first().size==1`、fakePrefs 值被覆盖 |
| `import_corruptJson_transitionsToError` | 传非 JSON 流 → state Error("导入失败，请重试")；DB 无改动 |
| `import_emptyStream_transitionsToError` | 传空流 → state Error |
| `import_schemaMismatch_transitionsToError` | 传 schemaVersion=2 的 JSON → state Error |
| `import_timeVizPrefsMissing_doesNotChangePrefs` | JSON 无 timeVizPrefs，预设 fakePrefs.birthday=999 → import Success(_,_,prefsUpdated=false)；fakePrefs.birthday 仍 999（未改） |
| `import_idConflict_replacesExisting` | DB 预插 id=1 event A；导入 JSON 含 id=1 event B → 落库后 `getEventById(1)` == B（REPLACE 覆盖）；再导一次同文件 → 仍 1 行（幂等） |
| `import_setsImportingBeforeFinalState` | 用 `state.onEach{ add }.first{ terminal }` 收集序列 → states 含 Importing 且终态 Success |
| `resetState_returnsToIdle` | 到 Success 后 `resetState()` → state Idle |

> `FakeTimeVizPrefs` 复用 `ExportViewModelTest` 末尾的 private 实现模式（接口四方法，内存字段）。

### 7.4 现有测试无回归

现有测试（含迭代 4 的 `DataExportManagerTest` / `ExportViewModelTest` / 修复后的 `HeatmapViewModelTest`）在四道门下全绿。本轮新增不改动这些文件，`EventRepository`/`NoteRepository` 仅追加方法（既有方法签名不变），无破坏性。

### 7.5 手动验证（SAF，device）

1. 设置 → 数据导入 → 弹确认对话框「导入数据？/ 导入将覆盖同 ID 数据，确认？」。
2. 点「导入」→ SAF 文件选择器（仅显示 application/json）。
3. 选迭代 4 导出的 `shijiben_backup_*.json` → Snackbar「已导入 N 条事件、M 条笔记（含偏好）」。
4. 校验：事件列表/热力图/笔记/时间可视化寿命卡均恢复至导出时状态。
5. 同 ID 重复导入：行数不变，字段为后导入值（幂等）。
6. 选损坏 JSON → Snackbar「导入失败，请重试」。
7. 取消 SAF 选择器 / 取消确认对话框 → 无 Snackbar，无崩溃。
8. 导入中再点行项：置灰不可点；导出中导入行亦置灰。

---

## 八、硬约束逐项核对清单

| 硬约束 | 核对 |
|--------|------|
| 1. 绝对不联网 | ✅ 仅 SAF 本地读文件 + 本地 Room/SharedPreferences 写；DataImportManager 仅 org.json；ImportViewModel 无网络 API；SettingsScreen 仅 contentResolver.openInputStream（本地 Uri）。无任何网络权限/调用。 |
| 2. 验证门四道全绿 | ✅ 顺序：① compileDebugKotlin → ② testDebugUnitTest --rerun-tasks → ③ assembleDebug → ④ assembleRelease。新增测试在 ② 通过；不引入新依赖/权限，③④ 构建不受影响。 |
| 3. Coding ≠ Test subagent | ✅ 本 spec 不指定 subagent 身份；Coding 与 Test 由 orchestrator 分派不同 subagent 执行。spec 本身仅为设计文档。 |
| 4. 全自动到失败为止 | ✅ 设计无手动步骤介入构建/测试；SAF 链路为运行时用户交互（非 CI 门），CI 门四道均自动化。 |
| 不改 DB schema | ✅ 无 Migration、无 Entity 字段变更、无 Dao 新方法；仅 Repository 加薄包装调既有 insert（REPLACE）。 |
| 不改导出格式 | ✅ 严格反向解析迭代 4 v1；DataExportManager 文件不改。 |
| 不做导入历史/增量/进度条 | ✅ 一次性整文件导入。 |
| 不做 schemaVersion 迁移 | ✅ 只支持 v1，≠1 即 Error。 |
| 不动其他 feature | ✅ 仅改 settings/export/import 相关；Timeline/Heatmap/TimeViz/Recording/Notes/About 零改动。 |

---

## 九、风险与回退

1. **Repository upsertAll 循环单条性能**：极端量级（万+）单条 insert 循环慢于批量 `@Insert(List)`。本地个人量级安全；若反馈问题，回退方案：Dao 加 `@Insert(onConflict=REPLACE) suspend fun insertAll(events: List<EventEntity>)` 批量（改 Dao，但非 schema 变更）。本轮不做。
2. **OpenDocument 不可单测**：UI 链路（launcher → Uri → openInputStream）仅手动验证，有回归盲区。缓解：ImportViewModel 注入 InputStream + dispatcher，可测逻辑剥离到 VM/DataImportManager 层单测；UI 仅做 launcher→stream 接线（极薄）。
3. **严格解析拒绝部分字段缺失**：必填字段缺失即 Error。若未来需兼容第三方/手写 JSON 的宽松字段，回退为 `optXxx` 带默认。本轮选严格以保证恢复数据完整性。
4. **REPLACE 覆盖不可逆**：导入同 ID 会覆盖现有数据，无撤销。由确认对话框（决策 4）+「导入将覆盖同 ID 数据」文案明示用户；幂等可重复导入。若需「保留原数据」语义，需换决策 1 方案（本轮 orchestrator 已拍板方案 A 合并 REPLACE，不改）。
5. **ImportViewModel 与 ExportViewModel 共用 SnackbarHostState**：二者不会同时进行（行项 enabled 互斥约束），无 Snackbar 冲突。
6. **AlertDialog 主题色**：`Error`/`TextSecondary` 已在 `AppColors` 定义（`TimelineScreen` 已用），无新主题色。Snackbar 沿用迭代 4 已接受的 Material3 默认圆角样式（已知 aesthetic 偏差，瞬态可接受）。
7. **buildConfig 已开**：`build.gradle.kts:63` `buildConfig = true`，`ExportViewModel` 已用 `BuildConfig.VERSION_NAME`；导入不依赖 appVersion，无影响。

---

## 十、附录：需 orchestrator 拍板的点

本轮四个决策点已由 orchestrator 全部拍板（方案 A）：

1. **导入覆盖策略**：方案 A 合并（保留原 ID，冲突 REPLACE 覆盖）—— 已拍板。spec 落地：`Repository.upsertAll` 调既有 `insertEvent`/`insertNote`（REPLACE）。
2. **timeVizPrefs 导入**：方案 A 导入（覆盖现有生日/寿命）—— 已拍板。spec 落地：`applyImport` 调既有 `setBirthdayMillis`/`setLifespanYears`；JSON 缺失时跳过。
3. **导入入口**：方案 A 设置页「数据导入」行（与「数据导出」并列）—— 已拍板。spec 落地：`SettingsScreen` 加导入行。
4. **确认对话框**：方案 A 有确认对话框 —— 已拍板。spec 落地：`AlertDialog`「导入将覆盖同 ID 数据，确认？」。

**读码发现的新事实（非新决策，仅通报）**：`TimeVizPrefs` 的 setter 在迭代 4 已一并落地（接口 + Impl 均已存在），故本轮原计划的「子项 C 加 setter」退化为「复核无改动」，无需 orchestrator 重新决策。

**预计无新增决策点。**
