# 2026-06-28 TimelineViewModel 测试补齐 + RecordingViewModel 错误路径补齐 Design（迭代 17）

> 日期：2026-06-28
> 范围：本轮组合两项测试覆盖加固——TimelineViewModel 测试补齐（候选 1）+ RecordingViewModel 错误路径补齐（候选 2）。
> 前置：迭代 16 已闭合 KDoc/死参数/markError 防御性改进，累计 175 个单测全绿。
> 本轮加分 +0（满分溢出），过程价值在错误路径与启动副作用防回归。
> 验证门（四道全绿才算过）：
> 1. `./gradlew :app:compileDebugKotlin`
> 2. `./gradlew :app:testDebugUnitTest --rerun-tasks`
> 3. `./gradlew assembleDebug`
> 4. `./gradlew :app:assembleRelease --rerun-tasks`

---

## 一、背景与目标

迭代 16 已达 175 单测全绿。剩余轮次价值在测试覆盖加固 + 过程价值。Discover 扫描发现两个 ViewModel 存在未覆盖的行为分支，orchestrator 已拍板组合立项：

1. **候选 1（TimelineViewModel 测试补齐）**：`TimelineViewModel` 的 `quickAddEvent` / `markInProgress` / `markCompleted` / `deleteEvent` / `init{}` 启动顺延 / 日期导航方法大量行为分支零测试覆盖，现有 4 个用例仅覆盖 `refresh` 与 `viewingDate` 稳定性 + `goToPreviousDay` 空事件路径。
2. **候选 2（RecordingViewModel 错误路径补齐）**：`RecordingViewModel.save` 的空标题守卫与编辑不存在分支、`delete()` 的无 `editingId` 守卫与成功路径、`initEdit` 的 NotStarted 分支（无 endTime + NotStarted → 用当前时间）均零覆盖。

工作量 M，风险全低：**零生产代码改动**，仅扩测试用例，复用两文件既有 setup，不新增 fixture、不新增文件。

## 二、硬约束

1. **绝对不做任何联网功能** —— 本地化 app 唯一原则。
2. **零生产代码改动**：仅扩测试用例，不改 VM 源码、不改 DAO/Repository、不改 build 配置。
3. **TimelineViewModelTest 沿用现有模式**：`MainCoroutineRule()`（`UnconfinedTestDispatcher`）+ `vm.events.first{}` 短订阅 + `.value` 直读。**不可**引入 `backgroundScope.launch { state.collect{} }` 常驻 collector，**不可**切到方案 A（HeatmapViewModel 那套常驻收集模式），**不触及 flaky 稳定区**。
4. **RecordingViewModelTest 沿用现有模式**：表单 VM 无 Flow 收集，沿用 `MainCoroutineRule()` + `runTest` suspend 调用 + 返回值断言 + `eventRepo.getEventById()` 直读校验。
5. spec 用**符号级引用**（类名/函数名/文件名），不用行号（项目级工程约定，避免代码漂移）。
6. **四道验证门全绿**：compileDebugKotlin + testDebugUnitTest --rerun-tasks + assembleDebug + assembleRelease --rerun-tasks。
7. 测试用例命名风格 `method_behavior_expectedResult`（与既有测试一致）。

## 三、Orchestrator 决策点（候选 1 + 候选 2 组合）

1. **本轮目标范围**：候选 1 + 候选 2（组合两项），均为纯测试加固，零生产代码改动。
2. **候选 1 用例数**：Discover 估"约 6-8"，实读源码后确认为 **9 用例**（+1 来自日期导航：Discover 猜测的方法名 `selectDate`/`previousDay`/`nextDay` 不存在，实读后实际为 `goToToday`/`goToPreviousDay`/`goToNextDay`/`setDate` 共 4 个方法，需 1 个合并用例覆盖，参见 §四设计内容与 §七关键发现）。
3. **候选 2 用例数**：5 用例（与 Discover 估"4-5"一致）。
4. **不切方案 A / 不引入常驻 collector**：TimelineViewModelTest 全部新增用例沿用 `first{}` 短订阅 + `.value` 直读模式。
5. **不动 HeatmapViewModel / flaky 稳定区**：本轮零触及。

## 四、设计内容

### §4.1 TimelineViewModel 用例（9 个，文件 `app/src/test/java/com/shijiben/feature/timeline/TimelineViewModelTest.kt`）

复用既有 `@Before setup()`：已建 `db`（内存 Room，`allowMainThreadQueries`）/ `eventRepo` / `noteRepo` / `vm = TimelineViewModel(eventRepo, noteRepo)`。全部新增用例直接复用，**不新建 fixture**。

> 关键模式约定（全用例遵守）：
> - 守卫路径（`?: return@launch` / `if (title.isBlank()) return`）**不变更 DB**，断言 DB 状态即可，**无需等待**。
> - 成功路径（VM 内 `viewModelScope.launch{}` 会改 DB）用 **`vm.events.first { <条件> }`** 等待 Room Flow 重发并断言新状态——与既有 `refresh_forceReread_picksUpNewlyCreatedEvent` 同模式。
> - 全部用 `= runTest { ... }` 包裹，与既有用例一致。

#### 用例 T1：`quickAddEvent_blankTitle_createsNoEvent`
- **覆盖**：`quickAddEvent` 的 `if (title.isBlank()) return` 守卫。
- **调用**：先 `vm.quickAddEvent("")`，再 `vm.quickAddEvent("   ")`。
- **断言**：`eventRepo.getAllEvents().first()` 为空（DB 无新增）；`vm.events.value` 为空。
- **备注**：空串与纯空白都走 `isBlank()` 守卫，方法体内 `return` 在 `viewModelScope.launch` 之前，**不启动协程**，故无需等待。一个用例覆盖两种输入。

#### 用例 T2：`quickAddEvent_validTitle_createsNotStartedEvent`
- **覆盖**：`quickAddEvent` 成功路径（`createEvent` + `refresh`）。
- **调用**：`vm.quickAddEvent("测试事件")`。
- **断言**：`val list = vm.events.first { it.isNotEmpty() }`；`list` hasSize 1；`list.first().title == "测试事件"`；`list.first().status == EventStatus.NotStarted.value`；`list.first().endTime == null`。
- **备注**：`quickAddEvent` 内部 `createEvent(... status = EventStatus.NotStarted.value, endTime = null)`，断言与源码一致。

#### 用例 T3：`markInProgress_nonExistentEventId_leavesDbUnchanged`
- **覆盖**：`markInProgress` 的 `eventRepository.getEventById(eventId) ?: return@launch` 守卫。
- **setup**：先 `eventRepo.createEvent(title="种子", startTime=now, endTime=null, note=null)` 建一条基线事件，记下其 `id` 与字段快照。
- **调用**：`vm.markInProgress(999_999L)`（不存在的 id）。
- **断言**：`eventRepo.getEventById(999_999L)` 为 null；`eventRepo.getEventById(种子id)` 字段与快照一致（status/startTime/endTime 未变）；`eventRepo.getAllEvents().first()` 仅含种子一条。
- **备注**：守卫路径 `return@launch` 不调 `updateEvent` 也不调 `refresh`，**不变更 DB**，断言时序无关，无需 `first{}` 等待。

#### 用例 T4：`markInProgress_validEventId_setsInProgressWithNullEndTime`
- **覆盖**：`markInProgress` 成功路径。
- **setup**：`val id = eventRepo.createEvent(title="待开始", startTime=now, endTime=null, note=null, status=EventStatus.NotStarted.value)`（显式传 NotStarted，避免 `determineStatus` 把 past/null-end 误判 InProgress）。
- **调用**：`vm.markInProgress(id)`。
- **断言**：`val list = vm.events.first { it.any { e -> e.id == id && e.status == EventStatus.InProgress.value } }`；取该事件，`status == EventStatus.InProgress.value`；`endTime == null`；`startTime` 接近 `System.currentTimeMillis()`（容差几秒）。
- **备注**：VM `markInProgress` 自有逻辑（`startTime=now, endTime=null, status=InProgress`），**不**委托 `EventRepository.markInProgress`（后者保留原 startTime）；断言按 VM 实际行为。

#### 用例 T5：`markCompleted_nonExistentEventId_leavesDbUnchanged`
- **覆盖**：`markCompleted` 的 `getEventById(eventId) ?: return@launch` 守卫。
- **setup**：同 T3，建一条基线事件并快照。
- **调用**：`vm.markCompleted(999_999L)`。
- **断言**：`eventRepo.getEventById(999_999L)` 为 null；种子事件字段与快照一致；`getAllEvents().first()` 仅含种子。
- **备注**：同 T3，守卫路径不变更 DB，无需等待。

#### 用例 T6：`markCompleted_validEventId_setsCompletedWithEndTime`
- **覆盖**：`markCompleted` 成功路径。
- **setup**：`val id = eventRepo.createEvent(title="进行中", startTime=now-60_000, endTime=null, note=null, status=EventStatus.InProgress.value)`。
- **调用**：`vm.markCompleted(id)`。
- **断言**：`val list = vm.events.first { it.any { e -> e.id == id && e.status == EventStatus.Completed.value } }`；取该事件，`status == EventStatus.Completed.value`；`endTime != null` 且接近 `System.currentTimeMillis()`；`startTime` 保持原值（VM 未改 startTime）。
- **备注**：VM `markCompleted` 自有逻辑（`endTime=now, status=Completed`，保留原 startTime），**不**委托 `EventRepository.markCompleted`（后者在 endTime 非空时保留原 endTime）；断言按 VM 实际行为。

#### 用例 T7：`deleteEvent_validEventId_removesEvent`
- **覆盖**：`deleteEvent` 成功路径（`deleteEventById` + `refresh`）。
- **setup**：`val id = eventRepo.createEvent(title="待删", startTime=now, endTime=null, note=null)`。
- **调用**：`vm.deleteEvent(id)`。
- **断言**：`vm.events.first { it.isEmpty() }`（等待删除反映到 Flow）；并 `eventRepo.getEventById(id)` 为 null。
- **备注**：`deleteEvent` 无守卫，直接删。用 `first { it.isEmpty() }` 等待 Room 重发空列表，再用 repo 直读二次确认。

#### 用例 T8：`init_carriesOverPastNotStartedEventToToday`
- **覆盖**：`init{}` 启动自动顺延（`viewModelScope.launch { eventRepository.carryOverNotStarted(today) }`）。
- **关键设计**：**不**复用 `@Before` 的 `vm`（其 `init{}` 已在 setup 时对空库跑过，无法顺延后插入的事件）。须在测试体内**先播种再构造新 VM**。
- **setup（测试体内）**：
  - 用 `Calendar` 构造"昨天 10:30"的时间戳 `yesterdayStart`。
  - `val id = eventRepo.createEvent(title="待顺延", startTime=yesterdayStart, endTime=null, note=null, status=EventStatus.NotStarted.value)`。
    - **必须显式传 `status=EventStatus.NotStarted.value`**：`createEvent` 默认走 `determineStatus`，对 past-start + null-end 会判为 InProgress，导致 `carryOverNotStarted`（仅查 `status=NotStarted`）查不到。
- **调用**：`val freshVm = TimelineViewModel(eventRepo, noteRepo)`（构造即触发 `init{}` 顺延）。
- **断言**：`val list = freshVm.events.first { it.isNotEmpty() }`；`list` hasSize 1；`list.first().id == id`；该事件 `startTime` 已落在"今天"（用 `Calendar` 解析 `startTime`，年/月/日 == 今天的 `vm.viewingDate.value` 三元组）；`status` 仍为 `EventStatus.NotStarted.value`（`updateEventTime` 不改 status）。
- **备注**：`freshVm.events` 默认 viewingDate=今天，顺延后事件归今天，`first{}` 返回即顺延已完成。`@Before` 的共享 `vm` 不参与断言，无干扰。

#### 用例 T9：`dateNavigation_changesAndRestoresViewingDate`
- **覆盖**：`setDate` / `goToNextDay` / `goToPreviousDay` / `goToToday` 四个实际存在的导航方法（Discover 猜的 `selectDate`/`previousDay`/`nextDay` 不存在）。
- **调用与断言**（纯同步，`viewingDate` 是 StateFlow，直接读 `.value`）：
  - `val today = vm.viewingDate.value`。
  - `vm.setDate(2025, 1, 15)` → `vm.viewingDate.value == Triple(2025, 1, 15)`。
  - `vm.goToNextDay()` → `== Triple(2025, 1, 16)`。
  - `vm.goToPreviousDay()` → `== Triple(2025, 1, 15)`。
  - `vm.goToPreviousDay()` → `== Triple(2025, 1, 14)`。
  - `vm.goToToday()` → `== today`。
- **备注**：`goToPreviousDay` 已有部分覆盖（既有 `goToPreviousDay_thenRefresh_stillEmitsEmptyWhenNoEvents` 仅验空事件，未验日期实际回退）；本用例补齐 4 方法日期实际位移 + `goToToday` 复位。`shiftDay` 跨月/跨年由 `Calendar` 处理，本用例用 1 月中旬避免跨月边界，专注方法语义。

### §4.2 RecordingViewModel 用例（5 个，文件 `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt`）

复用既有 `@Before setup()`：已建 `db` / `eventRepo` / `vm = RecordingViewModel(eventRepo)`。全部新增用例直接复用，**不新建 fixture**。

> 既有用例已覆盖：`save` 编辑 InProgress/Completed 事件、`save` 新建事件、`initEdit` 5 小时事件、`initNew` durationMax。本轮补**错误路径 + NotStarted 分支 + delete**。

#### 用例 R1：`save_blankTitle_returnsFalse`
- **覆盖**：`save` 的 `if (title.isEmpty()) return false` 守卫。
- **调用 1**：`vm.initNew()`（title="")，不调 `onTitleChange`，`val ok = vm.save(today)`。
- **断言 1**：`ok` isFalse；`eventRepo.getAllEvents().first()` 为空（无新建）。
- **调用 2**：`vm.onTitleChange("   ")`，`val ok2 = vm.save(today)`。
- **断言 2**：`ok2` isFalse；DB 仍为空。
- **备注**：`save` 内 `title = _title.value.trim()`，纯空白 trim 后为空 → 守卫命中。`today` 三元组用 `Calendar` 取当前年/月/日（与既有用例同模式）。

#### 用例 R2：`save_editingDeletedEvent_returnsFalse`
- **覆盖**：`save` 编辑模式下 `eventRepository.getEventById(eid) ?: return false` 守卫。
- **setup**：`val id = eventRepo.createEvent(title="原标题", startTime=now-60_000, endTime=null, note=null)`；`val event = eventRepo.getEventById(id)!!`；`vm.initEdit(event)`（设置 `editingId=id`）。
- **调用**：`eventRepo.deleteEventById(id)`（直接删，模拟事件被外部移除）；`val ok = vm.save(today)`。
- **断言**：`ok` isFalse。
- **备注**：`save` 内 `getEventById(eid)` 返回 null → `return false`，不调 `updateEvent`/`createEvent`。无需第二个 VM，路径简洁可行（实读后确认）。

#### 用例 R3：`delete_withoutEditingId_returnsFalse`
- **覆盖**：`delete()` 的 `val eid = editingId ?: return false` 守卫。
- **调用**：`vm.delete()`（fresh `vm`，未 `initEdit`/`initNew` 后未设 editingId——`initNew` 也置 `editingId=null`，但本用例直接用 setup 的 `vm`，`editingId` 默认 null）。
- **断言**：返回 isFalse。
- **备注**：`delete()` 是 suspend，`runTest` 内直接调用。无 DB 副作用，无需额外断言。

#### 用例 R4：`delete_afterInitEdit_removesEventAndReturnsTrue`
- **覆盖**：`delete()` 成功路径。
- **setup**：`val id = eventRepo.createEvent(title="待删", startTime=now, endTime=null, note=null)`；`val event = eventRepo.getEventById(id)!!`；`vm.initEdit(event)`。
- **调用**：`val ok = vm.delete()`。
- **断言**：`ok` isTrue；`eventRepo.getEventById(id)` 为 null。
- **备注**：`delete()` 内 `deleteEventById(eid)` 后返回 true。用 repo 直读确认删除。

#### 用例 R5：`initEdit_notStartedEventWithNullEndTime_usesCurrentTimeAndZeroDuration`
- **覆盖**：`initEdit` 的 NotStarted 分支（`event.endTime == null && event.status == EventStatus.NotStarted.value` → 用当前时间 snap 到 15 分钟，`durationMinutes=0`，`durationMax=180`）。
- **setup**：用 `Calendar` 构造"今天 02:00"时间戳 `fixedStart`；`val id = eventRepo.createEvent(title="待办", startTime=fixedStart, endTime=null, note=null, status=EventStatus.NotStarted.value)`；`val event = eventRepo.getEventById(id)!!`。
- **调用**：在调用前后各取一次 `Calendar` 当前分钟数（防止跨分钟边界）：
  - `val nowMin1 = cal1.hour*60 + cal1.minute`
  - `vm.initEdit(event)`
  - `val nowMin2 = cal2.hour*60 + cal2.minute`
  - `val possibleSnaps = setOf(((nowMin1/15)*15).coerceIn(0,1440), ((nowMin2/15)*15).coerceIn(0,1440))`
- **断言**：
  - `vm.durationMinutes.value == 0`（确定性）。
  - `vm.durationMax.value == 180`（确定性，等于 `NEW_EVENT_DURATION_MAX`）。
  - `possibleSnaps.contains(vm.startMinutes.value)` 为 true（验证"用当前时间"而非事件 startTime 的 120 分钟）。
- **备注（关键设计发现）**：`initEdit` 的 **NotStarted 分支与 InProgress 分支产出的 `durationMinutes`(0) 与 `durationMax`(180) 完全相同**，二者唯一区别是 `startMinutes` 来源（NotStarted 用当前时间，InProgress 用 `event.startTime` 的时分）。因此**仅断言 duration/max 无法区分两分支**，必须断言 `startMinutes` 反映当前时间。`possibleSnaps` 集合兼容跨分钟边界，避免 flaky。事件 startTime 固定 02:00（120 分），与"当前时间 snap"几乎不可能相等，进一步保证分支区分力。

### §4.3 不改动文件清单（复核）

本轮**仅改 2 个测试文件**，其余零改动：

- `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt` — 零改动
- `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` — 零改动
- `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` — 零改动（`shiftToTargetDay` 的 `now` 参数已于迭代 16 删除，当前签名 `shiftToTargetDay(e, year, month, day)`，本轮不再动）
- `app/src/main/java/com/shijiben/data/local/EventDao.kt` / `EventEntity.kt` / `EventStatus.kt` — 零改动
- `app/src/main/java/com/shijiben/feature/recording/MainCoroutineRule.kt` — 零改动
- `app/build.gradle.kts` / `AndroidManifest.xml` / 资源文件 — 零改动
- 其他测试文件（`HeatmapViewModelTest` 等 flaky 稳定区）— 零改动
- 零新增文件

## 五、涉及文件清单

| 类型 | 文件 | 改动 |
|------|------|------|
| 改（测试） | `app/src/test/java/com/shijiben/feature/timeline/TimelineViewModelTest.kt` | +9 用例（T1–T9） |
| 改（测试） | `app/src/test/java/com/shijiben/feature/recording/RecordingViewModelTest.kt` | +5 用例（R1–R5） |

合计 **2 文件改动**（均为测试），**零新增文件**，**零生产代码改动**。

## 六、验证标准

1. **四道门全绿**：
   - `./gradlew :app:compileDebugKotlin`（零生产代码改动，必过；仅校验测试编译）
   - `./gradlew :app:testDebugUnitTest --rerun-tasks`（预期 **189 测试 = 175 现有 + 14 新增**，全绿）
   - `./gradlew assembleDebug`
   - `./gradlew :app:assembleRelease --rerun-tasks`
2. **新增 14 用例全过**：TimelineViewModelTest 9 + RecordingViewModelTest 5。
3. **零回归**：现有 175 测试全过（不触及任何生产代码，不触及 flaky 稳定区，不切方案 A）。
4. **测试总数核对**：175 + 14 = 189。
5. **模式一致性**：TimelineViewModelTest 新增用例全部用 `first{}` 短订阅 / `.value` 直读，**无** `backgroundScope.launch{collect{}}` 常驻 collector；RecordingViewModelTest 新增用例全部用 `runTest` + suspend 调用 + 返回值/repo 直读断言。

## 七、风险评估

| 风险 | 影响 | 缓解 |
|------|------|------|
| `init{}` 顺延用例时序：`freshVm` 构造后顺延协程未跑完就断言 | 用例 T8 偶发失败 | `freshVm.events.first { it.isNotEmpty() }` 本身即等待：Flow 非空当且仅当顺延已把事件移到今天。与既有 `refresh_forceReread` 同模式，已验证可行。 |
| `initEdit` NotStarted 的 `startMinutes` 用当前时间，断言易 flaky | 用例 R5 跨分钟边界失败 | 用 `possibleSnaps` 集合（调用前后两次取当前分钟 snap）兼容跨分钟；`duration=0`/`durationMax=180` 为确定性断言，作主断言。 |
| `markInProgress`/`markCompleted` 成功路径断言 `startTime`/`endTime` 接近 `now` | 时间容差失败 | 用"几秒内"宽容差（`abs(ts - now) < 5_000`），VM 内取的是 `System.currentTimeMillis()`，测试与 VM 同进程，容差安全。 |
| 守卫路径用例误用 `first{}` 等待（守卫不变更 DB，Flow 不重发） | 守卫用例卡死/超时 | §4.1 明确：守卫用例（T1/T3/T5）用 `eventRepo` 直读断言，**不用** `first{}` 等待；成功用例才用 `first{}`。 |
| 误把 `markInProgress`/`markCompleted` 当作委托 `EventRepository.markInProgress`/`markCompleted` | 断言与实际不符 | 实读确认 VM 自有逻辑（见 §七关键发现 2）；spec §4.1 T4/T6 已按 VM 实际行为断言。 |
| 误用 Discover 猜的方法名 `selectDate`/`previousDay`/`nextDay` | 编译失败 | 实读确认实际为 `setDate`/`goToPreviousDay`/`goToNextDay`/`goToToday`；spec §4.1 T9 已用实际名。 |
| 顺延用例种子事件被 `determineStatus` 误判 InProgress | 顺延不触发，T8 失败 | `createEvent` 显式传 `status=EventStatus.NotStarted.value`，绕过 `determineStatus`；spec §4.1 T8 已标注。 |

### 关键设计发现（实读源码后，与 Discover 报告假设不符之处）

1. **日期导航方法名不符**：Discover 猜 `selectDate`/`previousDay`/`nextDay`，实读 `TimelineViewModel` 实际为 `goToToday()` / `goToPreviousDay()` / `goToNextDay()` / `setDate(year, month, day)`，共 4 个方法。spec T9 用实际名。
2. **VM 的 `markInProgress`/`markCompleted` 不委托 Repository**：`TimelineViewModel.markInProgress` 自有逻辑（`startTime=now, endTime=null, status=InProgress`），`markCompleted` 自有逻辑（`endTime=now, status=Completed`，保留原 startTime）。而 `EventRepository.markInProgress`/`markCompleted` 是另一套（repo 的 `markCompleted` 在 endTime 非空时保留原 endTime；repo 的 `markInProgress` 保留原 startTime）。**断言必须按 VM 实际行为**，不能照搬 repo 语义。这是 Discover 报告未提及的细节。
3. **`init{}` 顺延测试必须构造新 VM**：`@Before` 的共享 `vm` 其 `init{}` 已在 setup 时对空库执行过（no-op），无法顺延测试体内后插入的事件。T8 必须在测试体内先播种再 `TimelineViewModel(eventRepo, noteRepo)` 构造 `freshVm`。
4. **顺延种子必须显式传 NotStarted 状态**：`EventRepository.createEvent` 默认 `determineStatus` 对 past-start + null-end 判 InProgress；不显式传 `status=EventStatus.NotStarted.value` 则 `carryOverNotStarted`（仅查 status=0）查不到，顺延不触发。
5. **`initEdit` NotStarted 与 InProgress 两分支的 duration/max 完全相同**：两分支都产出 `durationMinutes=0` + `durationMax=180`，唯一区别是 `startMinutes` 来源。因此 R5 **必须**断言 `startMinutes` 反映当前时间，否则无法区分两分支、无法证明走了 NotStarted 分支。
6. **`shiftToTargetDay` 已无 `now` 参数**：迭代 16 已删除，当前签名 `shiftToTargetDay(e, year, month, day)`。本轮零生产代码改动确认。
7. **守卫路径不变更 DB**：T1/T3/T5 的守卫（`isBlank` return / `?: return@launch`）在协程启动前或 `updateEvent` 前返回，不调 `refresh`、不改 DB。断言用 repo 直读，时序无关，无需 `first{}` 等待——这是与成功路径用例的关键区别。

## 八、预估加分

+0（已达 100/100 满分，加分溢出）。

过程价值：
- 错误路径防回归：`quickAddEvent` 空标题、`markInProgress`/`markCompleted` 不存在 id、`save` 空标题/编辑不存在、`delete()` 无 editingId 共 6 条错误分支获自动化守护。
- 启动副作用防回归：`init{}` 自动顺延 NotStarted 事件到今天——核心业务逻辑首获测试覆盖。
- 行为分支防回归：`deleteEvent`、日期导航 4 方法、`initEdit` NotStarted 分支补齐。
- 测试覆盖广度：TimelineViewModelTest 从 4 → 13 用例，RecordingViewModelTest 从 5 → 10 用例。

## 九、Coding subagent 注意事项

1. **绝对不切方案 A / 不引入常驻 collector**：TimelineViewModelTest 新增用例**全部**用 `vm.events.first { <条件> }` 短订阅或 `vm.viewingDate.value` / `vm.refreshTrigger.value` 直读。**严禁** `backgroundScope.launch { vm.events.collect{} }` 或任何常驻 collector，**严禁**把 TimelineViewModelTest 切到 HeatmapViewModel 的方案 A 模式。这是 flaky 稳定区红线。
2. **守卫用例不用 `first{}` 等待**：T1（`quickAddEvent` 空标题）、T3/T5（`markInProgress`/`markCompleted` 不存在 id）的守卫不变更 DB、不调 `refresh`，**用 `eventRepo` 直读断言**（`getAllEvents().first()` / `getEventById()`）。误用 `vm.events.first{}` 等待会因 Flow 不重发而卡死。
3. **成功用例用 `first{}` 等待**：T2/T4/T6/T7/T8 的成功路径会改 DB，用 `vm.events.first { <条件> }` 等待 Room Flow 重发并断言。条件要精确（如 `it.any { e -> e.id == id && e.status == InProgress }`），避免在旧状态上提前返回。
4. **T8 必须构造新 VM**：不复用 `@Before` 的 `vm`；测试体内先播种"昨天 NotStarted"事件（**显式传 `status=EventStatus.NotStarted.value`**），再 `val freshVm = TimelineViewModel(eventRepo, noteRepo)`，再用 `freshVm.events.first{}` 断言。
5. **T4/T6 断言按 VM 实际行为**：VM `markInProgress` 设 `startTime=now, endTime=null`；VM `markCompleted` 设 `endTime=now`、保留原 `startTime`。**不要**照搬 `EventRepository.markInProgress`/`markCompleted` 语义。
6. **T9 用实际方法名**：`setDate` / `goToNextDay` / `goToPreviousDay` / `goToToday`，**不要**用 Discover 猜的 `selectDate`/`previousDay`/`nextDay`（不存在，编译失败）。
7. **R5 必须断言 `startMinutes` 反映当前时间**：NotStarted 与 InProgress 分支的 duration/max 相同，仅 `startMinutes` 来源不同。用 `possibleSnaps` 集合（调用前后两次取当前分钟 snap）兼容跨分钟边界。事件 startTime 固定 02:00 与当前时间区分。
8. **R2 路径**：`initEdit(event)` → `eventRepo.deleteEventById(id)` → `vm.save(today)` 返回 false。无需第二个 VM。
9. **`today` 三元组**：RecordingViewModelTest 用例需 `val today = Calendar.getInstance(TimeZone.getDefault()).let { Triple(it.get(YEAR), it.get(MONTH)+1, it.get(DAY_OF_MONTH)) }`，与既有用例同模式。
10. **测试命名**：`method_behavior_expectedResult`，与既有用例一致。
11. **零生产代码改动**：仅扩测试，不动任何 `.kt` 生产文件、不动 build/manifest/资源。
12. **复用 setup**：两测试文件 `@Before setup()` 已建好 `db`/`repo`/`vm`，直接复用，**不新建 fixture**、**不新增文件**。

## 十、Test subagent 注意事项

1. **独立重跑四道门**（顺序执行，门 2 / 门 4 强制 `--rerun-tasks`）。
2. **核对测试总数**：175 + 14 = 189。若实际 ≠ 189，定位差异用例。
3. **核对零生产代码改动**：`git diff -- app/src/main` 应为空；仅 2 个测试文件有 diff。
4. **核对未切方案 A**：`git diff` TimelineViewModelTest 中**不得**出现 `backgroundScope.launch` / `state.collect` 常驻 collector；新增用例应为 `first{}` / `.value` 模式。
5. **核对 T8 用了 freshVm**：T8 不得复用 `@Before` 的 `vm`，须在测试体内 `TimelineViewModel(eventRepo, noteRepo)` 构造。
6. **核对 T4/T6 断言**：与 VM 实际行为一致（`markInProgress`→`endTime=null`+新 `startTime`；`markCompleted`→`endTime=now`+保留 `startTime`）。
7. **核对 R5 断言**：含 `startMinutes` 当前时间断言（`possibleSnaps` 集合），不仅断言 duration/max。
8. **核对方法名**：T9 用 `setDate`/`goToNextDay`/`goToPreviousDay`/`goToToday`，无 `selectDate`/`previousDay`/`nextDay`。
9. **核对零回归**：现有 175 测试全过；HeatmapViewModelTest 等 flaky 稳定区零改动。
10. **跨分钟 flaky 复核**：若 R5 偶发失败，确认 `possibleSnaps` 集合取了调用前后两次 snap；若仍 flaky，扩大为三次采样。
