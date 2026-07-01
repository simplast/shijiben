# Plan: architecture cycle 19 — F019

## Finding (F019)
**`DataImportManager`（data 层）import 并依赖 `TimeVizPrefs`（feature 层类型）——
违反 `docs/ARCHITECTURE.md:52` 自述的「`data` 层不依赖任何上层」规则。**

`docs/ARCHITECTURE.md` §2 明确声明依赖方向为单向（`data` ← `feature` ← `navigation` ← `MainActivity`），
且 §2 说明文字写「`data` 层不依赖任何上层」。但 `DataImportManager`（位于 `data/export/`）
在签名层面引用了 `feature/timeviz/TimeVizPrefs`——data 层向上依赖 feature 层，方向反了。

同一违反也蔓延到 data 层的测试：`DataImportManagerTest` 同样 import `TimeVizPrefs` 并实现 `FakeTimeVizPrefs`。

### 证据（symbol-level 引用）

文件：`app/src/main/java/com/shijiben/data/export/DataImportManager.kt`

```kotlin
// line 8
import com.shijiben.feature.timeviz.TimeVizPrefs
...
// lines 125-130
suspend fun applyImport(
    result: ImportResult,
    eventRepository: EventRepository,
    noteRepository: NoteRepository,
    timeVizPrefs: TimeVizPrefs         // ← data 层方法签名引用 feature 层类型
): ImportCounts {
    eventRepository.upsertAll(result.events)
    noteRepository.upsertAll(result.notes)
    var prefsUpdated = false
    result.timeVizPrefs?.let {
        timeVizPrefs.setBirthdayMillis(it.birthdayMillis)
        timeVizPrefs.setLifespanYears(it.lifespanYears)
        prefsUpdated = true
    }
    ...
}
```

文件：`app/src/test/java/com/shijiben/data/export/DataImportManagerTest.kt`
```kotlin
// line 12
import com.shijiben.feature.timeviz.TimeVizPrefs
...
// lines 385-392
private class FakeTimeVizPrefs : TimeVizPrefs { ... }
```

文件：`docs/ARCHITECTURE.md`
```
// line 52
**说明**：`data` 层不依赖任何上层；`feature` 层依赖 `data` + `ui/theme`；...
```

### 根因
`TimeVizPrefs` 当初只被 `TimeVizViewModel` 消费，故随手放在 `feature/timeviz/`。
后续迭代加入数据导入功能时，为「对称地把 prefs 写回」让 `DataImportManager.applyImport`
直接接收 `TimeVizPrefs` 参数——data 层由此向上引用了 feature 层。架构文档把
`TimeVizModule` 标为「横切」但未指出此横切已让 data 层产生向上依赖。

### 影响
- **M**：data 层违反自述依赖规则。任何 data 层对 feature 层的 import 都会让重构
  （例如把 `TimeVizPrefs` 改名/迁移到独立 prefs 模块、或被新 prefs 抽象替换）
  倒着牵动 data 层；同时让「data 可独立编译/复用」不成立。
- 行为无影响（当前正确工作），但架构边界已破。

## 修复方案
**把「写 prefs」职责从 `DataImportManager.applyImport` 上移到调用方 `ImportViewModel`。**
- data 层只负责 events/notes 落库（它本就拥有的 Repository 依赖）。
- prefs 写回由 `ImportViewModel` 在调用 `applyImport` 之后就地完成（VM 本就注入了 `timeVizPrefs`）。
- `ImportCounts` 去掉 `prefsUpdated` 字段（data 层不再知道 prefs 概念）；
  `ImportViewModel.ImportState.Success` 保留 `prefsUpdated`，由 VM 基于
  `result.timeVizPrefs != null` 计算。

### 为什么不是「把 TimeVizPrefs 搬到 data 层」
那是另一种修法，但会牵动 8+ 文件（TimeVizPrefs.kt + 4 个 main 消费方 + 4 个 test 消费方）
的 import 路径，且本质是「把 feature 类型降级为 data 类型」——但 `TimeVizPrefs`
语义上确实是 feature/timeviz 的偏好，不应放 data 层。本方案改职责归属而非搬类型，
更小、更准。

### 行为保持
- events/notes 落库顺序与幂等性不变（`upsertAll` 仍先 events 后 notes）。
- prefs 写回时机不变：events/notes 成功后写 prefs；若 `upsertAll` 抛异常，
  prefs 不写（VM 的 try-catch 捕获 → Error 状态）。
- `Success(events, notes, prefsUpdated)` 三字段语义不变；UI 消费方零改动。
- `ImportCounts` 字段缩减是 data 层内部契约变化，仅 `DataImportManagerTest` 与
  `ImportViewModel` 受影响，均在 in-scope 内。

## In-scope files（4）

1. **`app/src/main/java/com/shijiben/data/export/DataImportManager.kt`**
   - 删除 `import com.shijiben.feature.timeviz.TimeVizPrefs`
   - `applyImport` 签名去掉 `timeVizPrefs` 参数；函数体只做 `eventRepository.upsertAll` +
     `noteRepository.upsertAll`，返回 `ImportCounts(eventsImported, notesImported)`
   - `ImportCounts` data class 去掉 `prefsUpdated: Boolean` 字段
   - KDoc 同步更新（去掉「timeVizPrefs 非空时覆盖」一段）

2. **`app/src/main/java/com/shijiben/feature/settings/ImportViewModel.kt`**
   - `import()` 内 `withContext(ioDispatcher)` 块改为：
     ```kotlin
     val json = DataImportManager.readFromStream(inputStream)
     val result = DataImportManager.parseJsonString(json)
     val counts = DataImportManager.applyImport(result, eventRepository, noteRepository)
     val prefsUpdated = result.timeVizPrefs != null
     if (prefsUpdated) {
         result.timeVizPrefs!!.let {
             timeVizPrefs.setBirthdayMillis(it.birthdayMillis)
             timeVizPrefs.setLifespanYears(it.lifespanYears)
         }
     }
     ImportState.Success(counts.eventsImported, counts.notesImported, prefsUpdated)
     ```
   - 其余（state 机、markError、resetState）不变

3. **`app/src/test/java/com/shijiben/data/export/DataImportManagerTest.kt`**
   - 删除 `import com.shijiben.feature.timeviz.TimeVizPrefs`
   - 删除 `fakePrefs` 字段 + `setup()` 中的初始化 + 文件末尾 `FakeTimeVizPrefs` 类
   - `applyImport_idConflict_replacesExisting`：调用去掉 `fakePrefs` 实参；
     删除 `counts.prefsUpdated` 断言
   - `applyImport_prefsNull_doesNotChangePrefs`：**删除整个测试**（行为已不属于 DataImportManager）
   - `applyImport_prefsNonNull_overwritesPrefs`：**删除整个测试**（行为迁移到 ImportViewModelTest）
   - `applyImport_fullDataset_countsAllThree`：改名 `applyImport_fullDataset_countsEventsAndNotes`；
     调用去 `fakePrefs`；删 `counts.prefsUpdated` 与 `fakePrefs.birthday/lifespan` 断言；
     保留 events=2 / notes=1 断言
   - 类 KDoc 去掉「fake TimeVizPrefs」一句

4. **`app/src/test/java/com/shijiben/feature/settings/ImportViewModelTest.kt`**
   - 保留 `ImportFakeTimeVizPrefs`（VM 仍依赖 `TimeVizPrefs`，跨 feature 引用属另一发现，本周期不动）
   - 现有 `import_validData_transitionsToSuccessAndPersists` 已断言
     `fakePrefs.birthday == 123L` / `prefsUpdated == true`——覆盖从 DataImportManager
     迁来的 prefs 写回行为，无需新增
   - 现有 `import_timeVizPrefsMissing_doesNotChangePrefs` 已覆盖 prefs 缺失路径
   - **无需新增测试**（行为迁移后由既有 VM 测试覆盖）

## 步骤（≤8）
1. 编辑 `DataImportManager.kt`：去 import、改 `applyImport` 签名与函数体、缩 `ImportCounts`、更新 KDoc
2. 编辑 `ImportViewModel.kt`：在 `import()` 内接管 prefs 写回 + 计算 `prefsUpdated`
3. 编辑 `DataImportManagerTest.kt`：去 import/字段/Fake 类、删 2 个 prefs 测试、改 2 个 applyImport 测试
4. 静态检查 `ImportViewModelTest.kt`：确认既有测试已覆盖迁移行为（无编辑或仅注释更新）
5. `./gradlew :app:compileDebugKotlin`
6. `./gradlew :app:testDebugUnitTest --rerun-tasks`
7. `./gradlew assembleDebug`
8. `./gradlew :app:assembleRelease`

## 风险评估
- **行为零变化**：events/notes/pres 落库顺序、幂等性、错误处理、Success 三字段全部保留。
- **测试覆盖不退化**：prefs 写回行为由 `ImportViewModelTest` 既有 2 个测试覆盖
  （`import_validData_transitionsToSuccessAndPersists` + `import_timeVizPrefsMissing_doesNotChangePrefs`）。
- `ImportCounts` 字段缩减是 data 层内部契约，仅 4 个 in-scope 文件受影响。
- 无新抽象、无新文件、无 Hilt 变更。

## Gate
```
./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease
```

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `HeatmapYearViewModelTest > yearGrid_updatesWhenRepoEmitsNewData`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
- `TimelineViewModelTest > markInProgress_validEventId_setsInProgressWithNullEndTime`

## 相关但 out-of-scope（供后续 cycle）
- **F020 候选**：`feature/settings/ExportViewModel` + `ImportViewModel` +
  `feature/heatmap/TimeAllocationViewModel` 均跨 feature 引用 `TimeVizPrefs`/`Clock`
  （由 `feature/timeviz/TimeVizModule` 提供）——feature 模块互依。修法是把
  `TimeVizPrefs`/`Clock` provider 上移到 `di/` 或新建 `data/prefs/`，但牵动 8+ 文件，
  超出本周期 5-file 上限。
- `DataExportManager.buildJsonString` 已正确地只接 primitives（`birthdayMillis: Long, lifespanYears: Int`），
  无 data→feature 引用——`DataExportManager` 是 `DataImportManager` 应仿效的范式。
