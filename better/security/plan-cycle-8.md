# Plan: security cycle 8 — F008

## Finding (F008)
`DataImportManager.parseEvents` 直接读取 `status = o.getInt("status")` 但**不校验是否为合法 EventStatus 值**。
恶意/损坏的导入文件可注入任意 int（如 99），污染事件状态机。

### 证据（symbol-level 引用）
文件：`app/src/main/java/com/shijiben/data/export/DataImportManager.kt`

- `parseEvents` 内：`status = o.getInt("status")`（无校验）
- `EventStatus`（`app/src/main/java/com/shijiben/data/model/EventStatus.kt`）仅 3 个合法值：
  `NotStarted(0)` / `InProgress(1)` / `Completed(2)`
- `EventStatus.fromValue` 会 clamp 非法值到 NotStarted（防御性），但 import 路径**未使用它**

### 影响（信任边界：导入来源不明的 JSON）
1. `carryOverNotStarted` 按 `NotStarted.value(0)` 过滤 → status=99 事件永远不被顺延，卡在原日，污染时间轴
2. UI 按 status 分支渲染可能落入 else/fallback（误渲染或崩溃）
3. 按 status 查询（如 `getOngoingEvent`）会漏掉/误处理 status=99 事件

### 现有校验先例
- `parseJsonString` 对 `schemaVersion` 校验：不匹配 → `throw IllegalArgumentException`
- 文档注释：「非标准文件 → Error，保证数据完整性」
- → 非法 status 应同样**拒绝**（throw），而非静默 clamp

## 修复
在 `parseEvents` 读取 `status` 后，校验是否为合法 EventStatus 值，不合法则 `require` 抛 IllegalArgumentException。

### 改动点（symbol-level）
`DataImportManager.parseEvents` 内，`o.getInt("status")` 之后新增校验：

```kotlin
status = o.getInt("status").also {
    require(EventStatus.entries.any { es -> es.value == it }) {
        "非法 status 值: $it"
    }
},
```

+ 新增 import：`import com.shijiben.data.model.EventStatus`

## 回归测试
在 `DataImportManagerTest` 新增测试，沿用 `parseJsonString_schemaVersionMismatch_throwsIllegalArgumentException` 模式：
`parseJsonString_invalidStatus_throwsIllegalArgumentException`

- 构造合法 JSON（status=2），改 events[0].status=99
- 断言：`assertThrows(IllegalArgumentException::class.java) { DataImportManager.parseJsonString(json) }`

## 风险评估
- 影响面：仅 `parseEvents` 新增 1 行校验 + 1 个 import
- 不改 export 路径（export 只写合法 status，来自 Entity）
- 不改 DB schema、不改 EventRepository
- 现有测试全用合法 status（0/1/2）→ 不受影响
- `parseJsonString_roundTripWithExport_symmetric` 仍通过（export 写合法值）

## Gate
`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
