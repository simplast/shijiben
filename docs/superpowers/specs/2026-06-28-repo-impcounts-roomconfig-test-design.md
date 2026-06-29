# 事记本 迭代 18 设计：Repository 边界方法测试 + ImportCounts 三字段断言 + room-testing 版本引用统一

> 日期：2026-06-28
> 范围：本轮三件事组合，全部 S 工作量。① EventRepository / NoteRepository 边界方法测试补齐（候选 1，零生产代码改动，仅扩测试）；② DataImportManager ImportCounts 三字段断言补齐（候选 2，零生产代码改动，仅扩/补测试断言）；③ `app/build.gradle.kts` 中 `room-testing` 版本引用统一为 `rootProject.extra["room"]`（候选 3，1 行 build 配置改动，行为零变化）。
> 前置：迭代 1–17 已落地，当前质量评分已达稳态，189 个单测全绿。
> 迭代：iter 18（Discover 推荐 候选 1 + 候选 2 + 候选 3 组合，orchestrator 已采纳）。

## 验证门（四道闸全绿）

1. `./gradlew :app:compileDebugKotlin`
2. `./gradlew :app:testDebugUnitTest --rerun-tasks`
3. `./gradlew assembleDebug`
4. `./gradlew assembleRelease --rerun-tasks`

---

## 一、背景与目标

数据层是本地化 app 的根基，但目前 Repository 边界方法测试覆盖不均：

1. **EventRepository 边界方法零直测**：`markNotStarted(eventId)` 全项目零测试；`deleteEventById(id)` 仅作为其他测试的 helper 间接用过，无直测；`updateEvent(event)` 无直测；`upsertAll(events)` 无直测（仅 DataImportManagerTest 间接调用）。`markCompleted` / `markInProgress` / `markNotStarted` 三者的 `?: return` 守卫（不存在 id 时 no-op）从未被验证。
2. **NoteRepository.upsertAll 零直测**：仅 DataImportManagerTest 间接调用，无直测。
3. **DataImportManager ImportCounts 三字段断言不完整**：`applyImport` 返回 `ImportCounts(eventsImported, notesImported, prefsUpdated)`，但现有 3 个 `applyImport_*` 用例中，`eventsImported` / `notesImported` 从未被断言；其中 `applyImport_idConflict_replacesExisting` 连 `prefsUpdated` 都未断言（返回值被丢弃，未接收）。
4. **room-testing 版本引用不一致**：`app/build.gradle.kts` 中 `room-runtime` / `room-ktx` / `room-compiler` 三行均用 `${rootProject.extra["room"]}`，唯独 `room-testing` 硬编码 `2.6.1`。当前 `extra["room"] = "2.6.1"`，版本一致、行为零变化，但风格不一致，未来升 room 版本时 `room-testing` 会漏升。

**目标**：
- 候选 1：为 EventRepository 补 8 个边界方法直测 + 守卫测试，为 NoteRepository 补 2 个 upsertAll 直测。
- 候选 2：在 3 个现有 `applyImport_*` 用例中补齐 `eventsImported` / `notesImported`（部分含 `prefsUpdated`）断言；新增 1 个「events+notes+prefs 全有」的完整计数用例。
- 候选 3：把 `room-testing` 那一行改为 `${rootProject.extra["room"]}`，与同文件其他三行 room 依赖风格一致。

## 二、硬约束

1. **绝对不做任何联网功能** —— 本地化 app 唯一原则。
2. **候选 1 + 候选 2 零生产代码改动**（仅扩测试 / 补断言）；**候选 3 仅 1 行 build 配置改动**（版本号当前一致，行为零变化）。
3. **不触及 flaky 稳定区**：不动 HeatmapViewModel 方案 A，不动 HeatmapViewModelTest，不动 ExportViewModelTest 等已稳定测试。
4. **四道验证门全绿**：compileDebugKotlin + testDebugUnitTest --rerun-tasks + assembleDebug + assembleRelease --rerun-tasks。
5. spec 用**符号级引用**（类名 / 函数名 / 文件名），不用行号。
6. 测试用例命名风格 `method_behavior_expectedResult`（与既有测试一致）。
7. 复用既有 setup（`@Before` 中的 Room in-memory + Repository 构造），不新建 fixture、不新增文件、不新增依赖。

## 三、Orchestrator 决策点

orchestrator 已拍板：**候选 1 + 候选 2 + 候选 3 组合**，一次性落地。三件事互相独立、无耦合，可并行实现，但同属「数据层防御性加固 + 断言完整性 + 配置一致性」主题，合并为一个迭代交付。

- 候选 1 决策：补 Repository 边界方法直测 + `?: return` 守卫测试，覆盖 markNotStarted / deleteEventById / updateEvent / upsertAll / 三守卫。
- 候选 2 决策：补 ImportCounts 三字段断言（3 改 + 1 新），让 `eventsImported` / `notesImported` 不再是「未被验证的黑盒」。
- 候选 3 决策：room-testing 版本引用统一为 extra 法，与 room-runtime / room-ktx / room-compiler 风格对齐。

## 四、设计内容

### §4.1 候选 1：EventRepository / NoteRepository 边界方法测试

#### §4.1.1 测试基础设施（复用既有，零新增）

两个测试类已有完整 setup，直接复用：

- `EventRepositoryTest`：`@Before` 已建 `db`（Room in-memory）+ `dao`（`db.eventDao()`）+ `repo`（`EventRepository(dao)`）；`@After` 关 db。`@RunWith(RobolectricTestRunner::class)` + `@Config(sdk = [33])`。
- `NoteRepositoryTest`：`@Before` 已建 `db` + `repo`（`NoteRepository(db.noteDao())`）；`@After` 关 db。同上 runner / config。

既有模式（必须沿用）：
- 协程测试用 `runTest { ... }`。
- 断言用 `com.google.common.truth.Truth.assertThat`。
- 预插数据用 `dao.insertEvent(EventEntity(...))` 直接插（绕过 `repo.createEvent` 的 status 自动推断），以精确控制 `status` 字段（参见既有 `carryOverNotStarted_*` 用例的预插方式）。
- 验证 DB 状态用 `repo.getEventById(id)` 或 `repo.getAllEvents().first()`。
- `EventEntity` 构造需显式传全部字段：`id, title, startTime, endTime?, status, note?, createdAt, updatedAt`。
- `NoteEntity` 构造需显式传：`id, content, timestamp, createdAt, updatedAt`。

#### §4.1.2 EventRepositoryTest 新增用例（8 个）

**1. `markNotStarted_setsStatusToNotStarted`**
- 预插：`repo.createEvent("test", now - 1000, null, null, null)` 产出一个 InProgress 事件（startTime 过去 + endTime null → `determineStatus` 返回 InProgress），取 `id`。
- 操作：`repo.markNotStarted(id)`。
- 期望：`repo.getEventById(id)!!.status == EventStatus.NotStarted.value`。

**2. `markNotStarted_nonExistentId_isNoOp`**
- 预置：DB 空。
- 操作：`repo.markNotStarted(9999L)`（不存在 id，触发 `?: return` 守卫）。
- 期望：不抛异常；`repo.getAllEvents().first()` 仍为空。

**3. `markCompleted_nonExistentId_isNoOp`**
- 预置：DB 空。
- 操作：`repo.markCompleted(9999L)`。
- 期望：不抛异常；`repo.getAllEvents().first()` 仍为空。

**4. `markInProgress_nonExistentId_isNoOp`**
- 预置：DB 空。
- 操作：`repo.markInProgress(9999L)`。
- 期望：不抛异常；`repo.getAllEvents().first()` 仍为空。

**5. `deleteEventById_removesEvent`**
- 预插：`repo.createEvent("test", now - 1000, now - 500, null, null)` 产出一个 Completed 事件，取 `id`。
- 操作：`repo.deleteEventById(id)`。
- 期望：`repo.getEventById(id)` 为 null；`repo.getAllEvents().first()` 为空。

**6. `updateEvent_persistsModifiedFields`**
- 预插：`repo.createEvent("原标题", now - 1000, now - 500, null, null)`，取 `id`，`val original = repo.getEventById(id)!!`。
- 操作：`repo.updateEvent(original.copy(title = "新标题", note = "新备注"))`。
- 期望：`repo.getEventById(id)` 的 `title == "新标题"`、`note == "新备注"`；`startTime` / `endTime` / `status` 保持原值；`updatedAt` 已被 `updateEvent` 内部刷新（`>= original.updatedAt`，不需精确比对，重点验字段持久化）。

**7. `upsertAll_insertsAllEvents`**
- 预置：DB 空。
- 操作：构造 3 个带显式 id 的 `EventEntity`（id=1/2/3，其余字段任意合法值），`repo.upsertAll(listOf(e1, e2, e3))`。
- 期望：`repo.getAllEvents().first().size == 3`；`repo.getEventById(1)` / `getEventById(2)` / `getEventById(3)` 均非 null。

**8. `upsertAll_replacesOnIdConflict`**
- 预插：`dao.insertEvent(e1)`，e1 id=1, title="原"。
- 操作：`repo.upsertAll(listOf(e1prime))`，e1prime id=1, title="新"，其余字段与 e1 不同。
- 期望：`repo.getAllEvents().first().size == 1`（REPLACE 不新增）；`repo.getEventById(1)!!.title == "新"`（字段为 e1prime）。

#### §4.1.3 NoteRepositoryTest 新增用例（2 个）

**1. `upsertAll_insertsAllNotes`**
- 预置：DB 空。
- 操作：构造 2 个带显式 id 的 `NoteEntity`（id=10/11），`repo.upsertAll(listOf(n1, n2))`。
- 期望：`repo.getAllNotes().first().size == 2`；`repo.getNoteById(10)` / `getNoteById(11)` 均非 null。

**2. `upsertAll_replacesOnIdConflict`**
- 预插：`repo.createNote("原", now)` 产出 n（自动 id），取 `id`；或直接 `db.noteDao().insertNote(NoteEntity(id=10, ...))` 预插。
- 操作：`repo.upsertAll(listOf(n10prime))`，n10prime 与预插同 id 但 `content` 不同。
- 期望：`repo.getAllNotes().first().size == 1`；`repo.getNoteById(id)!!.content` 为新值。
- 实现建议：用显式 id 预插（`db.noteDao().insertNote(NoteEntity(id=10, content="原", ...))`），再 upsertAll 同 id 的 `content="新"`，断言 size==1 且 content=="新"。

### §4.2 候选 2：DataImportManager ImportCounts 三字段断言

#### §4.2.1 ImportCounts 数据结构与累加逻辑（实读确认）

`DataImportManager.ImportCounts` 是 data class，三字段：
- `eventsImported: Int`
- `notesImported: Int`
- `prefsUpdated: Boolean`

`DataImportManager.applyImport` 内部累加逻辑（实读确认，**不查 DB，直接用输入列表 size**）：
```
eventsImported = result.events.size
notesImported = result.notes.size
prefsUpdated = result.timeVizPrefs != null（进入 let 块即为 true）
```

故期望值 = 输入 `ImportResult` 的 `events.size` / `notes.size` / `timeVizPrefs != null`，与输入直接对应，无需查 DB 反推。

#### §4.2.2 现有用例当前断言状态（实读确认，与 Discover 报告略有出入）

| 用例 | 当前 eventsImported | 当前 notesImported | 当前 prefsUpdated |
|------|---------------------|--------------------|--------------------|
| `applyImport_idConflict_replacesExisting` | 未断言（返回值未接收） | 未断言 | 未断言 |
| `applyImport_prefsNull_doesNotChangePrefs` | 未断言 | 未断言 | 已断言（false） |
| `applyImport_prefsNonNull_overwritesPrefs` | 未断言 | 未断言 | 已断言（true） |

> 与 Discover 报告「三个测试仅断言 prefsUpdated」略有出入：`applyImport_idConflict_replacesExisting` 实际连 `prefsUpdated` 都未断言（`applyImport` 返回值被丢弃）。本 spec 据实补齐。

#### §4.2.3 改动 1：`applyImport_idConflict_replacesExisting` 补三字段断言

该用例输入：`events=[eventB]`（id=1, 5 个字段非空）, `notes=[]`, `timeVizPrefs=null`。

改动：
- 把第一次 `DataImportManager.applyImport(result, eventRepo, noteRepo, fakePrefs)` 调用改为接收返回值：`val counts = DataImportManager.applyImport(...)`。
- 在调用后补三行断言：
  - `assertThat(counts.eventsImported).isEqualTo(1)`
  - `assertThat(counts.notesImported).isEqualTo(0)`
  - `assertThat(counts.prefsUpdated).isFalse()`
- 原有 DB 状态断言（`after.hasSize(1)` / `row.title == "新数据"` / 幂等再导一次）全部保留不动。
- 第二次「幂等再导」的 `applyImport` 调用保持原样（返回值仍可丢弃），本 spec 不强求对其补断言。

#### §4.2.4 改动 2：`applyImport_prefsNull_doesNotChangePrefs` 补两字段断言

该用例输入：`events=[]`, `notes=[]`, `timeVizPrefs=null`。已接收 `counts`。

改动：在现有 `assertThat(counts.prefsUpdated).isFalse()` 后补：
- `assertThat(counts.eventsImported).isEqualTo(0)`
- `assertThat(counts.notesImported).isEqualTo(0)`

#### §4.2.5 改动 3：`applyImport_prefsNonNull_overwritesPrefs` 补两字段断言

该用例输入：`events=[]`, `notes=[]`, `timeVizPrefs=nonNull`。已接收 `counts`。

改动：在现有 `assertThat(counts.prefsUpdated).isTrue()` 后补：
- `assertThat(counts.eventsImported).isEqualTo(0)`
- `assertThat(counts.notesImported).isEqualTo(0)`

#### §4.2.6 新增用例：`applyImport_fullDataset_countsAllThree`

完整计数用例，events + notes + prefs 全有，三字段全断言。

- 输入：
  - `e1 = EventEntity(id=1, title="事件一", startTime=1000L, endTime=2000L, status=2, note="n1", createdAt=3000L, updatedAt=4000L)`
  - `e2 = EventEntity(id=2, title="事件二", startTime=5000L, endTime=null, status=1, note=null, createdAt=6000L, updatedAt=7000L)`
  - `n1 = NoteEntity(id=10, content="随笔", timestamp=8000L, createdAt=9000L, updatedAt=10000L)`
  - `result = ImportResult(schemaVersion=1, events=listOf(e1, e2), notes=listOf(n1), timeVizPrefs=ImportedTimeVizPrefs(birthdayMillis=999L, lifespanYears=77))`
- 操作：`val counts = DataImportManager.applyImport(result, eventRepo, noteRepo, fakePrefs)`。
- 期望：
  - `counts.eventsImported == 2`
  - `counts.notesImported == 1`
  - `counts.prefsUpdated == true`
- 可选附加断言（不强求，但建议补以增强信心）：`eventRepo.getAllEvents().first().size == 2`、`noteRepo.getAllNotes().first().size == 1`、`fakePrefs.birthday == 999L`、`fakePrefs.lifespan == 77`。

### §4.3 候选 3：build.gradle.kts room-testing 版本引用统一

#### §4.3.1 实读确认

- 根 `build.gradle.kts` 已定义 `extra["room"] = "2.6.1"`。
- `app/build.gradle.kts` dependencies 区：
  - `room-runtime` / `room-ktx` / `room-compiler` 三行均用 `${rootProject.extra["room"]}`。
  - `room-testing` 硬编码 `2.6.1`。
- `rootProject.extra["room"]` 在 `testImplementation` 作用域可见（与同文件 `testImplementation` 区其他依赖一致，如 `kotlinx-coroutines-test` 也用 extra 风格的版本——虽该行硬编码 `1.8.1`，但 `rootProject.extra` 在 dependencies 块任意位置均可访问，已由 `room-runtime` 等三行验证）。
- 当前 `extra["room"] = "2.6.1"` 与硬编码 `2.6.1` 一致，改后行为零变化。

#### §4.3.2 改动（1 行）

`app/build.gradle.kts` dependencies 区 `room-testing` 行：

```kotlin
// 改前
testImplementation("androidx.room:room-testing:2.6.1")
// 改后
testImplementation("androidx.room:room-testing:${rootProject.extra["room"]}")
```

仅此 1 行。与同文件 `room-runtime` / `room-ktx` / `room-compiler` 三行风格一致。

### §4.4 不改动文件清单

本轮**不动**以下文件（明确排除，避免触及 flaky 稳定区 / 超范围）：

- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt`（候选 1 零生产代码改动）
- `app/src/main/java/com/shijiben/data/repository/NoteRepository.kt`（候选 1 零生产代码改动）
- `app/src/main/java/com/shijiben/data/export/DataImportManager.kt`（候选 2 零生产代码改动）
- `app/src/main/java/com/shijiben/data/local/EventDao.kt` / `NoteDao.kt` / `EventEntity` / `NoteEntity` / `AppDatabase`（不动 DAO / Entity / schema）
- `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt`（flaky 稳定区，不动）
- `app/src/test/java/com/shijiben/feature/settings/ExportViewModelTest.kt`（已稳定，不动）
- 其他所有 `feature/**` ViewModel / Screen / 测试（本轮范围仅数据层 + build 配置）
- 根 `build.gradle.kts`（`extra["room"]` 已存在，无需新增）
- `AGENT.md` / `docs/2026-06-22-shijiben-design.md`（本轮无文档对齐需求）

## 五、涉及文件清单

本轮 **4 文件改动，零新增文件**：

| 文件 | 改动类型 | 改动内容 |
|------|----------|----------|
| `app/src/test/java/com/shijiben/data/repository/EventRepositoryTest.kt` | 新增测试方法 | §4.1.2，8 个 `@Test` |
| `app/src/test/java/com/shijiben/data/repository/NoteRepositoryTest.kt` | 新增测试方法 | §4.1.3，2 个 `@Test` |
| `app/src/test/java/com/shijiben/data/export/DataImportManagerTest.kt` | 改 3 用例 + 新增 1 用例 | §4.2.3–4.2.6，3 处补断言 + 1 个新 `@Test` |
| `app/build.gradle.kts` | 1 行改动 | §4.3.2，`room-testing` 版本引用改 extra 法 |

## 六、验证标准

### 6.1 四道门全绿

1. `./gradlew :app:compileDebugKotlin` —— 验证 build 配置改动（room-testing 引用）+ 测试代码编译通过。
2. `./gradlew :app:testDebugUnitTest --rerun-tasks` —— 验证新增 11 个 `@Test` + 3 处补断言全绿，且既有 189 个测试无回归。
3. `./gradlew assembleDebug` —— 验证 debug APK 构建不受 build 配置改动影响。
4. `./gradlew assembleRelease --rerun-tasks` —— 验证 release 构建（含 proguard / 资源收缩）不受影响。

### 6.2 测试总数

- 当前：189 个单测。
- 新增：11 个 `@Test` 方法（8 EventRepository + 2 NoteRepository + 1 DataImportManager）。
- 预期总数：**189 + 11 = 200 个单测**。
- 另有 3 个既有 `@Test` 方法补断言（方法数不变，仅断言行增加）。

### 6.3 行为零变化确认

- 候选 1 / 2：零生产代码改动，运行时行为零变化。
- 候选 3：`extra["room"] = "2.6.1"` 与原硬编码 `2.6.1` 一致，依赖解析结果相同，构建产物零变化。

## 七、风险评估

| 风险 | 影响 | 缓解 |
|------|------|------|
| 新增 Repository 测试用例因 Robolectric / Room in-memory 时序问题 flaky | 测试偶发失败 | 沿用既有 `EventRepositoryTest` / `NoteRepositoryTest` 已验证的 setup 模式（`allowMainThreadQueries` + `runTest`），不引入新范式；四道门第二道 `--rerun-tasks` 强制重跑可暴露 flaky。 |
| `upsertAll_replacesOnIdConflict` 测试依赖 `OnConflictStrategy.REPLACE`，若 DAO 注解被改则失败 | 测试失败 | `EventDao.insertEvent` / `NoteDao.insertNote` 均显式 `@Insert(onConflict = OnConflictStrategy.REPLACE)`，本轮不动 DAO，注解稳定。 |
| `updateEvent_persistsModifiedFields` 对 `updatedAt` 断言过严导致 flaky | 测试偶发失败 | spec 明确「`updatedAt` 已被刷新，不需精确比对，重点验字段持久化」，断言 `title` / `note` 即可，不比对 `updatedAt` 精确值。 |
| `room-testing` 改 extra 法后，若 `rootProject.extra["room"]` 在 `testImplementation` 不可见导致编译失败 | build 失败 | 同文件 `room-runtime` / `room-ktx` / `room-compiler` 三行已在 `implementation` 区用 `${rootProject.extra["room"]}` 验证可见性；`testImplementation` 与 `implementation` 同属 dependencies 块，`rootProject.extra` 在整个块内可见。四道门第一道即可暴露。 |
| 候选 2 改动 `applyImport_idConflict_replacesExisting` 时遗漏原有 DB 状态断言 | 回归保护丢失 | spec §4.2.3 明确「原有 DB 状态断言全部保留不动」，Coding subagent 仅在第一次 `applyImport` 调用处补 `val counts =` 接收 + 三行断言，不改原有断言行。 |

整体回退策略：三件事互相独立——候选 1（Repository 测试）、候选 2（ImportCounts 断言）、候选 3（build 配置）可分别 revert。若某候选出问题，单独回退该候选的文件改动即可，其余保留。

## 八、预估加分

**+0 分**（过程价值，无用户可见功能变化）。

过程价值在三方面：
1. **数据层防御性加固**：Repository 边界方法（markNotStarted / deleteEventById / updateEvent / upsertAll）从「零直测」到「有直测」，`?: return` 守卫从「未验证」到「显式 no-op 验证」，未来重构 Repository 时有回归保护网。
2. **断言完整性**：ImportCounts 的 `eventsImported` / `notesImported` 不再是「未被验证的黑盒」，导入计数契约被显式锁定。
3. **配置一致性**：room-testing 与 room-runtime / room-ktx / room-compiler 风格对齐，未来升 room 版本时四行一起走，不会漏升 room-testing。

## 九、Coding subagent 注意事项

1. **零生产代码改动**：候选 1 + 2 只动测试文件，**绝对不要**改 `EventRepository.kt` / `NoteRepository.kt` / `DataImportManager.kt`。若发现测试暴露了生产代码 bug，**停下报告**，不要自行修生产代码。

2. **沿用既有 setup**：`EventRepositoryTest` / `NoteRepositoryTest` / `DataImportManagerTest` 的 `@Before` / `@After` 已完备，**不要**改 setup，**不要**新建 fixture 类，**不要**新增 helper 函数（除非多个新用例共享明显的构造逻辑，且与既有风格一致）。

3. **预插数据用 `dao.insertEvent(EventEntity(...))`**：需要精确控制 `status` 字段时（如 `markNotStarted_setsStatusToNotStarted` 需要一个 InProgress 事件），用 `repo.createEvent(...)` 让 `determineStatus` 自动推断即可（startTime 过去 + endTime null → InProgress）；但 `upsertAll_*` 用例需要显式 id，必须用 `dao.insertEvent(EventEntity(id=..., ...))` 直接插。参照既有 `carryOverNotStarted_*` 用例的预插方式。

4. **`updateEvent_persistsModifiedFields` 不要比对 `updatedAt` 精确值**：`updateEvent` 内部 `event.copy(updatedAt = System.currentTimeMillis())`，`runTest` 下时间可能被虚拟化，精确比对易 flaky。断言 `title` / `note` 已持久化即可，最多断言 `updatedAt >= original.updatedAt`（也不强求）。

5. **`applyImport_idConflict_replacesExisting` 改动要谨慎**：该用例有两次 `applyImport` 调用（第二次验幂等）。**只在第一次调用处**接收 `counts` 并补三字段断言，第二次保持原样（返回值可丢弃）。原有所有 DB 状态断言（`after.hasSize(1)` / `row.title` / `row.startTime` / `row.note` / 幂等 `hasSize(1)`）**全部保留不动**。

6. **候选 3 仅 1 行**：只改 `app/build.gradle.kts` 中 `room-testing` 那一行，**不要**顺带改 `kotlinx-coroutines-test` / `truth` / `robolectric` 等其他硬编码版本（那些不在本轮范围，超范围改动违反「不做不相关改动」原则）。

7. **测试命名严格用 `method_behavior_expectedResult`**：与既有用例一致（如 `markCompleted_setsStatusAndFillsEndTime`、`deleteNoteById_removesNote`）。本 spec §4.1.2 / §4.1.3 / §4.2.6 已给出建议命名，可直接用或按相同风格微调。

8. **import 复用**：`EventRepositoryTest` 已 import `EventEntity` / `EventStatus` / `first` / `runTest` / `Truth` / `Calendar` / `TimeZone`；`NoteRepositoryTest` 已 import `first` / `runTest` / `Truth` / `Calendar` / `TimeZone`，但**未 import `NoteEntity`**——`upsertAll_insertsAllNotes` / `upsertAll_replacesOnIdConflict` 需要构造 `NoteEntity`，需补 `import com.shijiben.data.local.NoteEntity`。`DataImportManagerTest` 已 import 所有需要的类型，无需新增 import。

## 十、Test subagent 注意事项

1. **四道门必须全绿**，缺一不可。特别注意第四道 `assembleRelease --rerun-tasks`——候选 3 改了 build 配置，release 构建需验证 proguard / 资源收缩不受影响。

2. **测试总数核对**：跑完第二道门后，确认测试总数 = 200（189 既有 + 11 新增）。若数量不符，排查是否有用例被错误跳过（如 `@Test` 漏写、命名冲突）。

3. **候选 3 行为零变化确认**：第一道门（compileDebugKotlin）通过即说明 `rootProject.extra["room"]` 在 `testImplementation` 可见；第二道门通过即说明 room-testing 依赖解析正常（`room-testing` 提供 `Room.inMemoryDatabaseBuilder` 等测试 API，若版本引用错误会导致编译失败）。

4. **flaky 监控**：新增的 Repository 测试用 Room in-memory + Robolectric，与既有 `EventRepositoryTest` / `NoteRepositoryTest` 同模式。若第二道门 `--rerun-tasks` 出现偶发失败，重点排查 `updateEvent_persistsModifiedFields`（若 Coding 误加了 `updatedAt` 精确比对）和 `upsertAll_replacesOnIdConflict`（若 REPLACE 行为异常）。

5. **ImportCounts 断言核对**：跑完后，grep `DataImportManagerTest.kt` 中 `eventsImported` / `notesImported` 出现次数——应分别在 4 个用例中出现（3 改 + 1 新），`prefsUpdated` 应在 4 个用例中出现（`idConflict` 补上后 + `prefsNull` + `prefsNonNull` + `fullDataset`）。

6. **不动 flaky 稳定区**：确认 `HeatmapViewModelTest.kt` / `ExportViewModelTest.kt` 文件未被修改（git diff 应只有 4 个文件：3 测试 + 1 build 配置）。
