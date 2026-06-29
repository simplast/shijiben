# 2026-06-28 KDoc 失准修正 + 死参数清理 + markError 错误路径测试 Design（迭代 16）

> 日期：2026-06-28
> 范围：本轮组合三项防御性改进——KDoc 失准修正（候选 A）+ 死参数清理（候选 B）+ markError 错误路径测试（候选 C）。
> 前置：迭代 15 已达 100/100 满分，7 个 ViewModel 全部有测试覆盖，累计 171 个单测全绿。
> 本轮加分 +0（满分溢出），过程价值在文档准确性 + 代码整洁度 + 错误路径防回归。
> 验证门（四道全绿才算过）：
> 1. `./gradlew :app:compileDebugKotlin`
> 2. `./gradlew :app:testDebugUnitTest --rerun-tasks`
> 3. `./gradlew assembleDebug`
> 4. `./gradlew :app:assembleRelease --rerun-tasks`

---

## 一、背景与目标

迭代 15 已达 100/100 满分，所有维度满分稳固。剩余 5 轮（16–20）价值在防御性改进 + 过程价值 + 验收准备。Discover 扫描发现 3 项防御性改进点，orchestrator 已拍板组合立项：

1. **候选 A（KDoc 失准修正）**：`TimeVizPrefs` 接口 KDoc 写"生日 UTC 00:00 millis"，但 M1 修复（迭代 3）后 `TimeVizViewModel.setBirthday` 实际存"当地 00:00 millis"（用 `ZoneId.systemDefault()`）。`TimeVizCalculatorTest` 的类 KDoc 与辅助函数 `utcMidnightMillis` 的 KDoc 也同样失准，提及"UTC 00:00"与实际语义不符。`TimeVizCalculator.lifeRemaining` 的 `@param birthdayMillis` KDoc 已正确写"当地 00:00"（作对比基准）。
2. **候选 B（死参数清理）**：`EventRepository.shiftToTargetDay(e, year, month, day, now)` 的 `now: Long` 参数在函数体内未被使用（函数仅用 `e.startTime` / `e.endTime` / `year` / `month` / `day` 计算）。`internal` 可见性，全项目仅 `carryOverNotStarted` 一个调用方。
3. **候选 C（markError 错误路径测试补齐）**：`ExportViewModel.markError` / `ImportViewModel.markError` 两个方法在 `SettingsScreen` 真实调用（SAF `openOutputStream` / `openInputStream` 返回 null 时），但零测试覆盖。

工作量 M，风险全低：候选 A/B 改生产代码（仅注释 + 死参数，零逻辑改动），候选 C 仅加测试（零生产代码改动）。

## 二、硬约束

1. **绝对不做任何联网功能** —— 本地化 app 唯一原则。
2. **候选 A/B 改生产代码**（KDoc 注释 + 死参数清理），**候选 C 仅加测试**（零生产代码改动）。
3. spec 用**符号级引用**（类名/函数名/文件名），不用行号（项目级工程约定，避免代码漂移）。
4. **四道验证门全绿**：compileDebugKotlin + testDebugUnitTest --rerun-tasks + assembleDebug + assembleRelease --rerun-tasks。
5. **markError 测试复用既有 setup**：复用 `ExportViewModelTest` / `ImportViewModelTest` 既有 setup（`MainCoroutineRule(StandardTestDispatcher())` + 内存 Room + fake `TimeVizPrefs`），不重新搭测试基础设施。
6. `shiftToTargetDay` 是 `internal` 函数，删除参数仅影响一个调用方（`carryOverNotStarted`），零外部依赖。

## 三、Orchestrator 决策点（4 项全部采纳推荐）

1. **本轮目标范围**：候选 A + B + C（组合三项）。
2. **markError 测试范围**：每个 VM 2 用例（默认消息 + 自定义消息），共 4 用例。
3. **TimeVizCalculatorTest 注释失准一并修**：与 `TimeVizPrefs` KDoc 同轮修齐。
4. **EventRepository 边界方法测试不做**：价值低，不在本轮范围。

## 四、设计内容

### §4.1 候选 A：KDoc 失准修正

#### §4.1.1 TimeVizPrefs.kt 接口 KDoc 修正

- **文件**：`app/src/main/java/com/shijiben/feature/timeviz/TimeVizPrefs.kt`
- **修改对象**：`TimeVizPrefs` 接口中 `getBirthdayMillis()` 的 KDoc。
- **改前**：`/** 生日 UTC 00:00 millis，0 表示未设 */`
- **改后**：`/** 生日当地 00:00 millis，0 表示未设 */`
- **理由**：`TimeVizViewModel.setBirthday` 内部用 `LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()` 计算并存入 prefs，存的是**当地 00:00 millis**，非 UTC 00:00。M1 修复（迭代 3）后 `TimeVizCalculator.lifeRemaining` 的 `@param birthdayMillis` KDoc 已改为"当地 00:00"，但 `TimeVizPrefs` 接口 KDoc 漏改。
- **零逻辑改动**：仅改注释文字。

#### §4.1.2 TimeVizCalculatorTest.kt 注释失准修正

- **文件**：`app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt`
- **修改对象 1（类 KDoc）**：`TimeVizCalculatorTest` 类头 KDoc 中提及"utcMidnightMillis 模拟 VM setBirthday 存储的「UTC 00:00」millis"的描述。
  - 改前语义：声称 `setBirthday` 存储"UTC 00:00"millis。
  - 改后语义：明确 `setBirthday` 实际存储"当地 00:00"millis；测试因 `@Before` 把默认时区固定为 UTC，故"当地 00:00"与"UTC 00:00"在测试环境下数值相同，`utcMidnightMillis` 辅助函数产出的值恰好等于 `setBirthday` 会存的值。
- **修改对象 2（辅助函数 KDoc）**：`utcMidnightMillis` 函数的 KDoc `/** 「UTC 00:00」millis，模拟 VM setBirthday 的存储格式。 */`。
  - 改后语义：说明该函数产出 UTC 00:00 millis；因测试固定时区为 UTC，此值等于 `setBirthday` 在该时区下存储的"当地 00:00"millis，故可模拟 `setBirthday` 的存储值。
- **不改对象**：`utcMidnightMillis` 函数名与函数体保持不变（重命名属代码改动超范围，且函数名本身描述的是其产出"UTC 00:00"millis 的行为，准确；失准的是 KDoc 对它与 `setBirthday` 关系的描述）。
- **理由**：测试在 `@Before` 中 `TimeZone.setDefault(TimeZone.getTimeZone("UTC"))` 固定时区为 UTC，使 `ZoneId.systemDefault()` 在被测函数中行为可预测。在该固定时区下，`setBirthday` 存的"当地 00:00"== "UTC 00:00"，故测试全过。但 KDoc 声称 `setBirthday` 存"UTC 00:00"是语义错误，误导读者以为生产代码用 UTC 基准。
- **零逻辑改动**：仅改注释文字，不改测试代码与断言。

### §4.2 候选 B：shiftToTargetDay 死参数清理

- **文件**：`app/src/main/java/com/shijiben/data/repository/EventRepository.kt`
- **修改对象 1（函数签名）**：`EventRepository.shiftToTargetDay` 删除 `now: Long` 参数。
  - 改前签名：`internal fun shiftToTargetDay(e: EventEntity, year: Int, month: Int, day: Int, now: Long): Pair<Long, Long?>?`
  - 改后签名：`internal fun shiftToTargetDay(e: EventEntity, year: Int, month: Int, day: Int): Pair<Long, Long?>?`
- **修改对象 2（调用方同步）**：`carryOverNotStarted` 中调用 `shiftToTargetDay` 处去掉 `now` 实参。
  - 改前调用：`val shifted = shiftToTargetDay(e, targetYear, targetMonth, targetDay, now)`
  - 改后调用：`val shifted = shiftToTargetDay(e, targetYear, targetMonth, targetDay)`
- **理由**：`shiftToTargetDay` 函数体仅用 `e.startTime` / `e.endTime` / `year` / `month` / `day` 通过 `Calendar` 计算新时刻，`now` 参数从未被读取。`now` 在 `carryOverNotStarted` 中仍用于 `eventDao.updateEventTime(..., now = now)`，故仅从 `shiftToTargetDay` 调用处去掉，不影响 `carryOverNotStarted` 内 `now` 变量的其他用途。
- **调用方确认**：Grep `shiftToTargetDay` 全项目，唯一调用点在 `carryOverNotStarted`；`EventRepositoryTest` 中仅有一处注释提及该函数名，非调用。`internal` 可见性确保无外部模块依赖。
- **零逻辑改动**：删未使用参数 + 同步调用方，计算逻辑零改动。

### §4.3 候选 C：markError 错误路径测试补齐

#### §4.3.0 markError 方法签名确认（Design 阶段实读）

- **ExportViewModel.markError**：`fun markError(msg: String = "导出失败，请重试")` — **有默认参数**，无参调用 `markError()` 等价于 `markError("导出失败，请重试")`。方法体：`_state.value = ExportState.Error(msg)`。同步方法（非 suspend，不启动协程）。
- **ImportViewModel.markError**：`fun markError(msg: String = "导入失败，请重试")` — **有默认参数**，无参调用 `markError()` 等价于 `markError("导入失败，请重试")`。方法体：`_state.value = ImportState.Error(msg)`。同步方法。
- **SettingsScreen 真实调用场景**：
  - 导出：`context.contentResolver.openOutputStream(uri)` 返回 null → `viewModel.markError()`（用默认消息）。
  - 导入：`context.contentResolver.openInputStream(uri)` 返回 null → `importViewModel.markError()`（用默认消息）。
  - 生产路径只用默认消息；自定义消息分支为防御覆盖 + 未来扩展。

#### §4.3.1 ExportViewModelTest 补 2 用例

- **文件**：`app/src/test/java/com/shijiben/feature/settings/ExportViewModelTest.kt`
- **复用 setup**：`@Before setup()` 已建 `db` / `eventRepo` / `noteRepo` / `fakePrefs` / `vm`（`ExportViewModel(eventRepo, noteRepo, fakePrefs, mainRule.dispatcher)`），直接复用，不新增 fixture。
- **新增用例 1**：`markError_defaultMessage_transitionsToErrorState`
  - 调 `vm.markError()`（无参，走默认消息）。
  - 断言 `vm.state.value` == `ExportViewModel.ExportState.Error("导出失败，请重试")`。
- **新增用例 2**：`markError_customMessage_transitionsToErrorState`
  - 调 `vm.markError("自定义导出错误")`。
  - 断言 `vm.state.value` == `ExportViewModel.ExportState.Error("自定义导出错误")`。
- **测试风格**：`markError` 是同步方法，调完即可断言；为与既有测试一致，用 `runTest(mainRule.dispatcher)` 包裹（虽无异步等待需求，但保持文件内测试统一风格）。

#### §4.3.2 ImportViewModelTest 补 2 用例

- **文件**：`app/src/test/java/com/shijiben/feature/settings/ImportViewModelTest.kt`
- **复用 setup**：`@Before setup()` 已建 `db` / `eventRepo` / `noteRepo` / `fakePrefs` / `vm`（`ImportViewModel(eventRepo, noteRepo, fakePrefs, mainRule.dispatcher)`），直接复用。
- **新增用例 1**：`markError_defaultMessage_transitionsToErrorState`
  - 调 `vm.markError()`（无参，走默认消息）。
  - 断言 `vm.state.value` == `ImportViewModel.ImportState.Error("导入失败，请重试")`。
- **新增用例 2**：`markError_customMessage_transitionsToErrorState`
  - 调 `vm.markError("自定义导入错误")`。
  - 断言 `vm.state.value` == `ImportViewModel.ImportState.Error("自定义导入错误")`。
- **测试风格**：同 §4.3.1，`runTest(mainRule.dispatcher)` 包裹。

### §4.4 不改动文件清单（复核）

本轮仅改 5 个文件，其余零改动：

- `app/build.gradle.kts` — 零改动
- `app/src/main/AndroidManifest.xml` — 零改动
- 资源文件（drawable/values/themes 等） — 零改动
- `SettingsScreen.kt` — 零改动（markError 调用点不动）
- 其他测试文件 — 零改动
- `EventRepositoryTest.kt` — 零改动（其中提及 `shiftToTargetDay` 的是注释，非调用，删参数不影响）

## 五、涉及文件清单

| 类型 | 文件 | 改动 |
|------|------|------|
| 改（生产代码） | `app/src/main/java/com/shijiben/feature/timeviz/TimeVizPrefs.kt` | `getBirthdayMillis()` KDoc：UTC 00:00 → 当地 00:00 |
| 改（生产代码） | `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` | `shiftToTargetDay` 删 `now` 参数 + `carryOverNotStarted` 调用处同步 |
| 改（测试） | `app/src/test/java/com/shijiben/feature/timeviz/TimeVizCalculatorTest.kt` | 类 KDoc + `utcMidnightMillis` KDoc 注释失准修正 |
| 改（测试） | `app/src/test/java/com/shijiben/feature/settings/ExportViewModelTest.kt` | +2 markError 用例 |
| 改（测试） | `app/src/test/java/com/shijiben/feature/settings/ImportViewModelTest.kt` | +2 markError 用例 |

合计 5 文件改动（2 生产代码 + 3 测试），零新增文件。

## 六、验证标准

1. **四道门全绿**：
   - `./gradlew :app:compileDebugKotlin`（验证 `shiftToTargetDay` 删参数后编译通过 + Hilt KSP 不受影响）
   - `./gradlew :app:testDebugUnitTest --rerun-tasks`（预期 175 测试 = 171 现有 + 4 新增，全绿）
   - `./gradlew assembleDebug`
   - `./gradlew :app:assembleRelease --rerun-tasks`
2. **markError 4 用例全过**：ExportViewModelTest 2 用例 + ImportViewModelTest 2 用例。
3. **零回归**：现有 171 测试全过（`TimeVizCalculatorTest` 在固定 UTC 时区下逻辑零改动，全过；`EventRepositoryTest` 中 `carryOverNotStarted` 相关测试因 `shiftToTargetDay` 仅删未使用参数，计算结果零变化，全过）。
4. **shiftToTargetDay 删参数后编译通过**：调用方 `carryOverNotStarted` 已同步改，编译期会报错若有遗漏。
5. **KDoc/注释修正不影响测试**：`TimeVizCalculatorTest` 仅改注释文字，测试代码与断言零改动。

## 七、风险评估

| 风险 | 影响 | 缓解 |
|------|------|------|
| `shiftToTargetDay` 删参数遗漏调用方 | 编译失败 | Grep 已确认仅 `carryOverNotStarted` 一个调用方；`internal` 可见性无外部依赖。编译期门 1 即可暴露。 |
| `markError` 方法签名与预期不符 | 测试编译失败 | Design 阶段已实读确认：两 VM 均为 `markError(msg: String = "默认消息")` 单方法带默认参数，非重载。Coding 按此签名写测试。 |
| `TimeVizCalculatorTest` 注释修正影响测试逻辑 | 测试失败 | 仅改 KDoc 注释文字，测试代码与断言零改动。 |
| markError 默认消息与自定义消息分支不明确 | 测试断言错 | 已确认默认参数值：Export "导出失败，请重试"，Import "导入失败，请重试"。 |
| `utcMidnightMillis` 函数名本身失准 | 读者困惑 | 函数名描述其产出"UTC 00:00"millis 的行为，准确；失准的是 KDoc 对它与 `setBirthday` 关系的描述。本轮只改 KDoc，不重命名（重命名超范围）。 |

## 八、预估加分

+0（已达 100/100 满分，加分溢出）。

过程价值：
- 文档准确性：M1 修复遗留漏改闭合（`TimeVizPrefs` 接口 KDoc 与 `TimeVizCalculatorTest` 注释对齐实际语义）。
- 代码整洁度：`shiftToTargetDay` 死参数清理，签名更诚实。
- 错误路径防回归：`markError` 4 用例补齐，SAF 开流失败路径有自动化守护。

## 九、Coding subagent 注意事项

1. **markError 方法签名已确认**：两 VM 均为 `fun markError(msg: String = "默认消息")` 单方法带默认参数（非重载）。无参调用走默认消息，传参走自定义消息。按 §4.3.1 / §4.3.2 写测试。
2. **shiftToTargetDay 删参数**：先 Grep `shiftToTargetDay` 确认仅 `carryOverNotStarted` 一个调用方（`EventRepositoryTest` 中是注释非调用），删函数签名 `now: Long` 参数 + 改调用处去掉 `now` 实参。**注意**：`carryOverNotStarted` 内 `now` 变量仍用于 `eventDao.updateEventTime(..., now = now)`，不要误删该变量的其他用途。
3. **KDoc/注释修正**：仅改文字，不改代码逻辑、不改测试断言。
   - `TimeVizPrefs.kt`：`getBirthdayMillis()` 的 KDoc "UTC 00:00" → "当地 00:00"。
   - `TimeVizCalculatorTest.kt`：类 KDoc + `utcMidnightMillis` 函数 KDoc 中关于 `setBirthday` 存储格式的描述，从"UTC 00:00"修正为反映"当地 00:00"的实际语义（并说明测试固定 UTC 时区故两者数值相同）。
4. **markError 测试复用既有 setup**：不重新搭测试基础设施，读 `ExportViewModelTest` / `ImportViewModelTest` 既有 `@Before setup()` 直接复用 `vm` 实例。
5. **测试用例命名**：`method_behavior_expectedResult` 风格（与既有测试一致）。
6. **测试包裹风格**：用 `runTest(mainRule.dispatcher)` 包裹 markError 测试，与文件内既有测试风格统一（虽 markError 同步无需异步等待）。
7. **不动 `utcMidnightMillis` 函数名/函数体**：仅改其 KDoc。

## 十、Test subagent 注意事项

1. **独立重跑四道门**（顺序执行，门 2 / 门 4 强制 `--rerun-tasks`）。
2. **独立核对 markError 4 用例**与实际方法签名一致：`ExportViewModel.markError(msg: String = "导出失败，请重试")` / `ImportViewModel.markError(msg: String = "导入失败，请重试")`，确认测试断言的默认消息与默认参数值一致。
3. **核对 `shiftToTargetDay` 死参数已删** + `carryOverNotStarted` 调用处已同步 + 编译通过（门 1 验证）。
4. **核对 KDoc/注释修正不影响测试逻辑**：`TimeVizCalculatorTest` 测试代码与断言零改动，仅 KDoc 文字变化。
5. **核对零回归**：现有 171 测试全过。
6. **核对测试总数**：171 + 4 = 175。
7. **核对 `carryOverNotStarted` 内 `now` 变量仍用于 `updateEventTime`**：确认删参数未误伤该变量其他用途。
