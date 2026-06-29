# 事记本 迭代 5 设计：Splash 接入 + 文档对齐 + nit 清理（含 appVersion 接 BuildConfig）

> 日期：2026-06-28
> 范围：本轮三件事——(A) 给 App 接入 8-bit 风格启动屏；(B) 把 AGENT.md / 设计文档与代码现状对齐；(C) 清一批 nit（删未使用 import、复核 await 模式、把硬编码版本号接到 BuildConfig）。
> 前置：迭代 1–4 已落地，当前质量评分 83/100，95 个单测全绿。
> 验证门（三道全绿才算过）：
> 1. `./gradlew :app:compileDebugKotlin`
> 2. `./gradlew :app:testDebugUnitTest --rerun-tasks`
> 3. `./gradlew assembleDebug`

---

## 一、问题

1. **splash 缺失**：应用图标已 8-bit 定制（`ic_launcher_foreground.xml` 深灰方块 + 金对角线，`ic_launcher_background.xml` 米白），但冷启动无 splash——系统直接用 `Theme.ShiJiBen`（`Theme.Material.Light.NoActionBar`）的默认 windowBackground，启动瞬间是白屏，与 8-bit 米白 + 像素图标的整体调性脱节，且没有 `core-splashscreen` 依赖。
2. **文档与代码漂移**：
   - `AGENT.md` 项目结构缺 `data/export/`（DataExportManager）、`di/`（DispatchersModule）、`data/model/HeatmapModels.kt`，`feature/settings/` 未列 ExportViewModel。
   - `AGENT.md` Spec 索引缺迭代 4 的 `2026-06-28-data-export-and-flaky-fix-design.md`。
   - `AGENT.md` V3 分期未标"已落地"（V2 已标），而 V3 的"设置页 + 数据导出/备份"事实上均已落地。
   - `docs/2026-06-22-shijiben-design.md` 项目结构写 `features/`（实际 `feature/`）、`core/theme`（实际 `ui/theme`），第 129 行 spec 链接路径多一层 `docs/`（与第 116 行不一致），V3 未标"已落地"。
   - `plans/README.md` 011–014 已 DONE（迭代 3 已改），本轮仅复核，预计无改动。
3. **nit**：
   - `ExportViewModelTest.kt` 第 15、16 行有 2 处未使用 import（`kotlinx.coroutines.flow.toList` / `kotlinx.coroutines.launch`）。
   - `HeatmapViewModelTest.kt` 7 个测试里 6 个用 `first{}`、1 个用 `advanceUntilIdle + state.value`，需评估是否要统一。
   - `ExportViewModel.APP_VERSION = "1.0"` 与 `AboutScreen "版本 1.0.0"` 都是硬编码，注释里都写了"下轮接 BuildConfig.VERSION_NAME"，本轮就是那个"下轮"。

## 二、目标

- **A. Splash 接入（S）**：引 `androidx.core:core-splashscreen:1.0.1`，加 `Theme.ShiJiBen.Splash`，复用米白背景 + 居中 `ic_launcher_foreground`，与 launcher 图标视觉完全一致，零设计成本。MainActivity `installSplashScreen()` 后切回 `Theme.ShiJiBen`。
- **B. 文档对齐（S）**：逐文件修齐 AGENT.md / docs/2026-06-22-shijiben-design.md 与代码现状；复核 plans/README.md。
- **C. nit 清理（S）**：删 2 处未使用 import；await 模式经评估保留并说明理由；`buildConfig = true` + `BuildConfig.VERSION_NAME` 接掉两处硬编码版本号。

## 三、非目标

- 不新增任何 UI 组件、不改任何业务逻辑、不动 DB schema。
- 不联网（`core-splashscreen` 是 androidx 官方纯本地依赖；`BuildConfig` 是 AGP 内置能力）。
- 不给 splash 写单测（UI 层，手动验证）。
- 不重做图标资产（复用现有 `ic_launcher_*`）。
- 不统一 await 模式（见 §C.2 评估结论：保留并说明）。

## 四、设计

### A. Splash 接入

#### A.1 依赖

`app/build.gradle.kts` dependencies 区（第 51–86 行）加一行。为与项目用 `rootProject.extra` 管版本的惯例一致，推荐在根 `build.gradle.kts` 加一个 extra 项：

根 `build.gradle.kts`（第 9–17 行 extra 区末尾）加：
```kotlin
extra["splashscreen"] = "1.0.1"
```

`app/build.gradle.kts` dependencies 内（建议放在 `activity-compose` 那行之后，第 59 行下）加：
```kotlin
implementation("androidx.core:core-splashscreen:${rootProject.extra["splashscreen"]}")
```

> 备选：若不想动根 build.gradle.kts，可直接 `implementation("androidx.core:core-splashscreen:1.0.1")` 硬编码。两选一，Coding subagent 自取，推荐 extra 法。

兼容性：`core-splashscreen:1.0.1` 要求 compileSdk ≥ 34，本项目 compileSdk = 34 ✓；minSdk 26 也兼容（库内部按 API level 分派，12+ 走 SplashScreen API，12 以下回退到 windowBackground）。

#### A.2 splash_background drawable（新增）

新增 `app/src/main/res/drawable/splash_background.xml`：layer-list，底层 `ic_launcher_background`（米白 #F5F0E8 全填充 vector，作 layer-list 顶层 item 会拉伸铺满屏幕）+ 上层 `ic_launcher_foreground` 居中。

```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <!-- 米白底，复用 launcher 背景资产 -->
    <item android:drawable="@drawable/ic_launcher_background" />
    <!-- 居中像素图标：深灰方块 + 金对角线，与 launcher 图标一致 -->
    <item
        android:drawable="@drawable/ic_launcher_foreground"
        android:gravity="center" />
</layer-list>
```

设计要点：
- 零新增颜色资源：米白底直接复用 `ic_launcher_background`（其本身是 108×108 全填充 #F5F0E8 的 vector，作 drawable item 会按 layer-list 默认行为拉伸铺满 window）。
- `ic_launcher_foreground` 是 108×108 vector，方块在 viewport (30,30)-(78,78) 即 48×48 居中；`android:gravity="center"` 以其 intrinsic size（108dp）居中显示，视觉与 adaptive icon 一致。
- 8-bit 一致：米白 + 像素方块 + 金对角线，与全 app 8-bit 调性一致。

#### A.3 Theme.ShiJiBen.Splash（改 themes.xml）

`app/src/main/res/values/themes.xml`（当前仅 1 个 style，第 3 行）追加 splash 主题：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.ShiJiBen" parent="android:Theme.Material.Light.NoActionBar" />

    <style name="Theme.ShiJiBen.Splash" parent="Theme.ShiJiBen">
        <item name="android:windowBackground">@drawable/splash_background</item>
        <item name="postSplashScreenTheme">@style/Theme.ShiJiBen</item>
    </style>
</resources>
```

- `parent="Theme.ShiJiBen"`：继承应用主题所有属性，仅覆盖 windowBackground。
- `postSplashScreenTheme`：core-splashscreen 定义的 attr，`installSplashScreen()` 调用后系统据此切回应用主题。显式写 `@style/Theme.ShiJiBen`（虽与 parent 同名，但显式更清晰，避免依赖隐式回退）。

#### A.4 AndroidManifest.xml（改 MainActivity theme）

`app/src/main/AndroidManifest.xml` 第 16 行：
```xml
<!-- 改前 -->
android:theme="@style/Theme.ShiJiBen"
<!-- 改后 -->
android:theme="@style/Theme.ShiJiBen.Splash"
```

只改 `<activity>` 的 theme（第 16 行），`<application>` 的 theme（第 11 行）保持 `@style/Theme.ShiJiBen` 不变。

#### A.5 MainActivity.installSplashScreen（改 MainActivity.kt）

`app/src/main/java/com/shijiben/MainActivity.kt`：

加 import（第 3 行 `android.os.Bundle` 下）：
```kotlin
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
```

`onCreate`（第 16–17 行）在 `super.onCreate` **前**调 `installSplashScreen()`：
```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    setContent {
        // ... 原逻辑不动
    }
}
```

时序硬约束：`installSplashScreen()` 必须在 `super.onCreate(savedInstanceState)` 之前调用，否则抛异常 / 不生效。spec 明确标注。

#### A.6 buildConfig 开关（与 C.3 合并）

`app/build.gradle.kts` 第 27–29 行：
```kotlin
// 改前
buildFeatures {
    compose = true
}
// 改后
buildFeatures {
    compose = true
    buildConfig = true
}
```

此改动同时服务于 §C.3（BuildConfig.VERSION_NAME）。`buildConfig = true` 让 AGP 生成 `com.shijiben.BuildConfig`（namespace = `com.shijiben`，见 build.gradle.kts 第 10 行）。

### B. 文档对齐

#### B.1 AGENT.md 项目结构（第 23–49 行）

当前 `data/` 块缺 `export/`，`model/` 只写 `EventStatus`；缺 `di/`；`feature/settings/` 未列 ExportViewModel。

改 `data/` 块（第 28–32 行）为：
```
      data/
        local/     # Entity、DAO、Database（EventEntity, NoteEntity, EventDao, NoteDao, AppDatabase）
        model/     # EventStatus, HeatmapModels
        repository/# EventRepository, NoteRepository
        export/    # 数据导出（DataExportManager）
        DataModule.kt   # Hilt 提供方法
```

在 `feature/` 块前（第 32 行后、第 33 行前）加 `di/`：
```
      di/              # Hilt 模块（DispatchersModule）
```

改 `feature/settings/` 行（第 37 行）为：
```
        settings/  # 设置 + 关于/隐私政策 + 数据导出（SettingsScreen, AboutScreen, ExportViewModel）
```

#### B.2 AGENT.md Spec 索引（第 99–105 行）

在列表末尾（第 105 行后）补迭代 4 spec：
```markdown
- [2026-06-28-data-export-and-flaky-fix-design.md](docs/superpowers/specs/2026-06-28-data-export-and-flaky-fix-design.md) — 数据导出（JSON + SAF）+ flaky test 修复
```

并补本轮 spec（迭代 5）：
```markdown
- [2026-06-28-splash-docs-nit-cleanup-design.md](docs/superpowers/specs/2026-06-28-splash-docs-nit-cleanup-design.md) — Splash 接入 + 文档对齐 + nit 清理（本 spec）
```

#### B.3 AGENT.md V3 分期（第 97 行）

```markdown
<!-- 改前 -->
- **V3**：设置页 + 数据导出/备份 + 其他打磨
<!-- 改后 -->
- **V3**：设置页 + 数据导出/备份 + 其他打磨（设置页 + 数据导出已落地）
```

#### B.4 plans/README.md（复核，预计无改动）

复核结果：011–014 均为 DONE（第 24–27 行），状态正确，本轮不动。

#### B.5 docs/2026-06-22-shijiben-design.md

- 第 46 行 `features/` → `feature/`（与实际目录名一致）。
- 第 62 行 `core/theme` → `ui/theme`（与实际目录一致；AGENT.md 第 41 行已是 `ui/theme`）。
- 第 129 行 spec 链接路径 `(docs/superpowers/specs/2026-06-28-homepage-composition-rebalance-design.md)` → `(superpowers/specs/2026-06-28-homepage-composition-rebalance-design.md)`（该文件本身在 `docs/` 下，相对路径不应再带 `docs/`；与第 116 行同链接写法对齐）。
- 第 185–189 行 V3 分期标注已落地：
  ```markdown
  ### V3：打磨（部分已落地）

  - [x] 设置页（已落地骨架）
  - [x] 数据导出/备份
  - [ ] 其他根据使用反馈迭代
  ```

### C. nit 清理

#### C.1 删 ExportViewModelTest 未使用 import

`app/src/test/java/com/shijiben/feature/settings/ExportViewModelTest.kt` 第 15、16 行：
```kotlin
import kotlinx.coroutines.flow.toList   // 第 15 行，删（全文件无 toList 调用）
import kotlinx.coroutines.launch        // 第 16 行，删（全文件无裸 launch 调用，只有 ExportViewModel 内部用 launch）
```

保留：
- 第 13 行 `kotlinx.coroutines.flow.first`（第 77/96/113/129/141 行在用）
- 第 14 行 `kotlinx.coroutines.flow.onEach`（第 128 行在用）
- 第 18 行 `kotlinx.coroutines.test.advanceUntilIdle`（第 130 行在用）

#### C.2 HeatmapViewModelTest await 模式评估（保留，不改）

`app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt` 7 个测试：
- 6 个用 `vm.state.first { ... }`（suspending wait）等终态：`initialState`、`previousMonth_decrementsAndEnablesNext`、`nextMonth_fromPreviousMonth_returnsToCurrent`、`goToCurrentMonth_fromPrevious_returnsToCurrent`、`stateCells_shapeIs6x7AndTodayMarked`、`stateCells_updatesWhenRepoEmitsNewData`。
- 1 个用 `advanceUntilIdle() + before/after state.value 比对`：`nextMonth_atCurrentMonth_doesNotAdvance`（第 106–116 行）。

**评估结论：当前混合模式合理，保留。** 理由：
- `nextMonth_atCurrentMonth_doesNotAdvance` 的测试意图是验证 **no-op**——当前月 `canGoNext == false`，调 `nextMonth()` 不应造成任何状态变化。
- `first{}` 语义是"等待某个状态出现"。该测试初始 `state` 已经是 `isCurrentMonth=true`，若用 `first { it.isCurrentMonth }` 会立即返回（初始值就满足），根本没等到 `nextMonth()` 的 `viewModelScope.launch` 执行，无法验证"nextMonth 没改它"。
- 正确姿势是：记录 `before = state.value` → 调 `nextMonth()` → `advanceUntilIdle()` 让 `nextMonth` 内部的 launch 跑完 → 比对 `after == before`。这正是当前实现。
- 强行统一为 `first{}` 会破坏该测试的语义。故保留混合模式，并在测试注释里补一句说明（注释微调，非逻辑改动）。

> 给该测试第 107 行注释补一句：当前月 nextMonth 是 no-op，无新 emission，故用 advanceUntilIdle + 比对 before/after 而非 first{}。Coding subagent 自行判断是否补注释，不强求。

#### C.3 appVersion 接 BuildConfig

**前置：** 先做 §A.6（`buildConfig = true`），AGP 才会生成 `com.shijiben.BuildConfig`（含 `VERSION_NAME = "1.0"`，取自 build.gradle.kts 第 18 行 `versionName = "1.0"`）。

**ExportViewModel.kt**（`app/src/main/java/com/shijiben/feature/settings/ExportViewModel.kt`）：

加 import（第 5 行附近，`com.shijiben.data.export.DataExportManager` 上下）：
```kotlin
import com.shijiben.BuildConfig
```

companion object（第 80–83 行）：
```kotlin
// 改前
companion object {
    // 与 build.gradle.kts versionName 一致；下轮接 BuildConfig.VERSION_NAME
    const val APP_VERSION = "1.0"
    private val FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun generateFileName(): String =
        "shijiben_backup_${LocalDateTime.now().format(FILE_FMT)}.json"
}
// 改后
companion object {
    private val FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

    fun generateFileName(): String =
        "shijiben_backup_${LocalDateTime.now().format(FILE_FMT)}.json"
}
```

第 60 行调用点：
```kotlin
// 改前
appVersion = APP_VERSION,
// 改后
appVersion = BuildConfig.VERSION_NAME,
```

> 注：`ExportViewModelTest` 不直接断言 `appVersion` 字段值（测试只校验 `schemaVersion/events/notes/timeVizPrefs`，见 ExportViewModelTest 第 80–103 行），故接 BuildConfig 不影响现有测试断言。

**AboutScreen.kt**（`app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt`）：

加 import（第 30 行 `com.shijiben.ui.theme.Background` 上下）：
```kotlin
import com.shijiben.BuildConfig
```

第 93–98 行版本号：
```kotlin
// 改前
// 版本号硬编码 1.0.0，下轮接 BuildConfig.VERSION_NAME（本轮不动 build.gradle.kts）
Text(
    text = "版本 1.0.0",
    fontSize = 12.sp,
    color = TextTertiary
)
// 改后
Text(
    text = "版本 ${BuildConfig.VERSION_NAME}",
    fontSize = 12.sp,
    color = TextTertiary
)
```

> 一致性说明：改后 AboutScreen 与 ExportViewModel 都显示 `BuildConfig.VERSION_NAME = "1.0"`，两处版本号一致（此前 AboutScreen 写 "1.0.0"、ExportViewModel 写 "1.0" 是历史不一致，本轮顺带修齐）。

## 六、涉及文件清单

### 改
- `app/build.gradle.kts` — §A.6 开 `buildConfig = true`；§A.1 加 `core-splashscreen` 依赖
- `app/src/main/AndroidManifest.xml` — §A.4 MainActivity theme 改 `@style/Theme.ShiJiBen.Splash`
- `app/src/main/res/values/themes.xml` — §A.3 加 `Theme.ShiJiBen.Splash`
- `app/src/main/java/com/shijiben/MainActivity.kt` — §A.5 加 `installSplashScreen()`
- `app/src/main/java/com/shijiben/feature/settings/ExportViewModel.kt` — §C.3 删 `APP_VERSION`，改用 `BuildConfig.VERSION_NAME`
- `app/src/main/java/com/shijiben/feature/settings/AboutScreen.kt` — §C.3 版本号接 `BuildConfig.VERSION_NAME`
- `app/src/test/java/com/shijiben/feature/settings/ExportViewModelTest.kt` — §C.1 删 2 处未使用 import
- `AGENT.md` — §B.1 项目结构 / §B.2 Spec 索引 / §B.3 V3 分期
- `docs/2026-06-22-shijiben-design.md` — §B.5 修目录名 / 链接路径 / V3 标注
- 根 `build.gradle.kts` — §A.1 加 `extra["splashscreen"] = "1.0.1"`（若选 extra 法）

### 新增
- `app/src/main/res/drawable/splash_background.xml` — §A.2 splash windowBackground drawable

### 复核无改动
- `app/src/test/java/com/shijiben/feature/heatmap/HeatmapViewModelTest.kt` — §C.2 保留混合 await 模式
- `plans/README.md` — §B.4 011–014 已 DONE，复核通过

## 七、边界情况

1. **splash 在 API 26–30 的回退**：`core-splashscreen` 在 API 31+ 走系统 SplashScreen API，API 26–30 回退到 `windowBackground`（即本 spec 的 `splash_background`）。两层都走米白 + 居中图标，视觉一致。
2. **`installSplashScreen()` 时序**：必须在 `super.onCreate()` 前，否则库抛 `IllegalStateException`。spec §A.5 已标注。
3. **BuildConfig 包名**：namespace = `com.shijiben`（build.gradle.kts 第 10 行），故 import 路径为 `com.shijiben.BuildConfig`，与 MainActivity/ExportViewModel 同包，无需跨包。
4. **AboutScreen 是无 ViewModel 的 stateless composable**：直接 `BuildConfig.VERSION_NAME` 字符串插值即可，无需经 ViewModel。
5. **ExportViewModelTest 不断言 appVersion**：接 BuildConfig 不破坏断言（见 §C.3 注）。
6. **splash 与首屏色差**：splash 米白 + 像素图标 → 首屏 `Surface(color = MaterialTheme.colorScheme.background)`。需确认 AppTheme 的 background 也是米白系（应一致，手动验证）。

## 八、测试清单

### 自动化（回归保护，无新增测试）
- 现有 95 个单测在三道门下全绿：
  - `./gradlew :app:compileDebugKotlin`（含 BuildConfig 生成 + Hilt KSP）
  - `./gradlew :app:testDebugUnitTest --rerun-tasks`（含 ExportViewModelTest 删 import 后、HeatmapViewModelTest 混合 await 模式）
  - `./gradlew assembleDebug`（含 splash 主题、manifest、drawable 资源）

### 手动验证（splash，无单测）
- 冷启动 App：看到米白背景 + 居中深灰方块 + 金对角线 splash，随后无缝切到时间轴首页。
- warm start 不应再显示 splash（系统默认行为）。
- splash 视觉与 launcher 图标一致。
- 首屏 background 色与 splash 米白不冲突。

## 九、风险与回退

| 风险 | 影响 | 缓解 / 回退 |
|------|------|------------|
| `core-splashscreen:1.0.1` 与 compileSdk 34 不兼容 | 编译失败 | 已确认 1.0.1 要求 compileSdk ≥ 34，本项目 34 ✓。回退：删依赖 + splash 主题 + manifest 改回，即回到无 splash 状态。 |
| `buildConfig = true` 影响 Hilt KSP | 编译失败 / 注解处理异常 | `buildConfig = true` 仅让 AGP 生成 BuildConfig.java，不干预 KSP 处理 Hilt。三道门第一道（compileDebugKotlin）即可暴露。回退：关 buildConfig + APP_VERSION 改回硬编码。 |
| `installSplashScreen()` 时序错 | 运行时崩溃 | spec §A.5 明确"super.onCreate 前"。Coding 实现后 assembleDebug + 冷启动手测验证。 |
| `splash_background` layer-list 在低 API 拉伸异常 | splash 视觉错位 | `ic_launcher_background` 是全填充 vector，拉伸铺满即米白；`ic_launcher_foreground` 用 `gravity=center` 按 intrinsic size 居中。手测 API 26 模拟器验证。回退：改用 `<bitmap>` + 颜色常量。 |
| await 模式保留后被误改 | flaky 复现 | spec §C.2 已说明保留理由，Coding 不动 HeatmapViewModelTest。三道门第二道验证。 |

整体回退策略：三改动相对独立——splash（A）、文档（B）、nit（C）可分别 revert。若 `buildConfig = true` 出问题，单独回 A.6 + C.3（恢复硬编码），其余保留。

## 十、附录：需 orchestrator 拍板的点

无。本 spec 所有决策（splash 复用现有资产、await 模式保留、版本号统一为 `BuildConfig.VERSION_NAME`、splashscreen 版本用 extra 管理）均沿用 orchestrator 已拍板方向，无新增分歧。

---

## 十一、实现顺序建议（给 Coding subagent）

1. **C.3 + A.6 先行**：`buildConfig = true` → ExportViewModel / AboutScreen 接 BuildConfig → 跑 compileDebugKotlin 验证 BuildConfig 生成 + Hilt 不受影响。
2. **C.1**：删 ExportViewModelTest 2 处 import → 跑 testDebugUnitTest 验证。
3. **A**：加 splashscreen 依赖 → 新建 splash_background.xml → 加 Theme.ShiJiBen.Splash → 改 manifest → 改 MainActivity → 跑 assembleDebug + 手测冷启动。
4. **B**：改 AGENT.md / docs/2026-06-22-shijiben-design.md → 复核 plans/README.md。
5. **三道门最终验证**：compileDebugKotlin + testDebugUnitTest --rerun-tasks + assembleDebug 全绿。
