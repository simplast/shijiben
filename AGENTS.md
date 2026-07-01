# AGENT.md — 事记本（ShiJiBen）项目上下文

> 本文档供 AI agent 阅读，提供项目约定和关键上下文。
> 2026-06-27 更新：标签（Tag）功能已移除，DB v1→v2 迁移见 [docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md](docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md) Part C。

## 项目概述

事记本是一个纯本地的时间记录应用，受《奇特的一生》启发。核心理念：单纯记录生活，不设 TODO、不强迫复盘，通过长期记录看见时间去向，减少内耗。

> 架构详情见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)（模块依赖图 + 数据流 + 状态管理 + 导航图 4 视角）。

## 技术栈

- **平台**：Android 原生应用
- **语言**：Kotlin
- **UI**：Jetpack Compose
- **架构**：MVVM + Repository 模式
- **本地存储**：Room（SQLite 抽象层）
- **依赖注入**：Hilt
- **异步**：Kotlin Coroutines + Flow
- **导航**：Jetpack Navigation Compose

## 项目结构

```
shijiben/
  docs/            # 文档：设计 spec、计划
  app/
    src/main/java/com/shijiben/
      data/
        local/     # Entity、DAO、Database（EventEntity, NoteEntity, EventDao, NoteDao, AppDatabase）
        model/     # EventStatus, HeatmapModels
        repository/# EventRepository, NoteRepository
        export/    # 数据导出/导入（DataExportManager, DataImportManager）
        DataModule.kt   # Hilt 提供方法
      di/              # Hilt 模块（DispatchersModule）
      feature/
        heatmap/   # 热力图回看（3 tab 容器：月/年/去向）（HeatmapScreen, HeatmapViewModel, HeatmapMonthTab, HeatmapYearTab, HeatmapYearViewModel, HeatmapCalculator, TimeAllocationCalculator, TimeAllocationViewModel, TimeAllocationTab）
        notes/     # 随笔列表与编辑（NotesScreen, NoteEditorSheet, NotesViewModel）
        recording/ # 记录弹窗（RecordingSheet, RecordingViewModel, TimeRangeSlider）
        search/    # 搜索（SearchScreen, SearchViewModel）
        settings/  # 设置 + 关于/隐私政策 + 数据导出/导入（SettingsScreen, AboutScreen, ExportViewModel, ImportViewModel）
        timeline/  # 时间轴主视图（TimelineScreen, TimelineViewModel, DayProgressBar）
        timeviz/   # 时间可视化（TimeVizScreen, TimeVizViewModel, TimeVizCalculator, TimeVizPrefs）
      navigation/  # AppNavHost
      ui/theme/    # 8-bit 色板、字体、像素组件（AppColors, AppTheme, PixelComponents）
      ui/debug/    # 调试 overlay（仅 debug 构建）：DebugOverlay（悬浮调试按钮）+ DebugLog（异常 ring buffer + 崩溃持久化）
      MainActivity.kt
      ShiJiBenApplication.kt
    src/main/res/
    build.gradle.kts
    AndroidManifest.xml
  build.gradle.kts
  settings.gradle.kts
```

## 数据模型

两张核心表，详见 [docs/2026-06-22-shijiben-design.md](docs/2026-06-22-shijiben-design.md)：

- **Event**：事件，有 start_time/end_time/status/note（可选）
- **Note**：随笔，有 timestamp/content

### 事件状态流转

`not_started` → `in_progress` → `completed`

- 预写 = not_started
- 开始计时 = in_progress（end_time = null）
- 停止 = completed
- 当天结束仍 not_started → App 打开时自动顺延到次日

## 开发约定

- **开发预览**：日常在 Android Studio 模拟器或真机上运行
- **状态管理**：使用 ViewModel + Flow，避免在 UI 层直接访问数据库
- **数据访问**：通过 Repository 层，不直接写 SQL
- **8-bit 美学**：所有视觉元素集中在 `ui/theme`，色板使用活泼高饱和 NES 风格
- **命名**：文件用 PascalCase（Kotlin 惯例），包名/资源用 snake_case

## 设计哲学（重要）

1. **记录即审视**：App 反映现实，不评判现实
2. **轻量存在**：不制造焦虑，时间可视化静静在那
3. **不是 TODO**：预写事件不是待办，是"我打算做"，没做就顺延，不催不罚

## 构建与安装

修改代码后，通过以下命令构建并安装到已连接的 ADB 设备：

```bash
# 构建 debug APK
./gradlew assembleDebug

# 安装到已连接的设备（需先 adb 连接）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Release 构建

Release 通道开启 R8 混淆 + 资源缩减，签名配置从本地 `keystore.properties` 读取。

```bash
# 构建 release APK（无 keystore.properties 时产出未签名 APK，构建不破）
./gradlew :app:assembleRelease
```

已签名 release APK 安装：

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

#### Keystore 配置（仅本地）

Release 签名凭据存于项目根下 `keystore.properties`（gitignored，不入库）：

```properties
storeFile=keystore/release.jks
storePassword=shijiben
keyAlias=release
keyPassword=shijiben
```

- `keystore/release.jks` 为自签名 dev keystore，**非生产发布密钥**。
- 全新 clone 无 `keystore.properties` 时 release APK 仍可构建（未签名），不阻塞 CI。
- 若需重新生成 keystore，参考 [docs/superpowers/specs/2026-06-28-release-build-config-design.md](docs/superpowers/specs/2026-06-28-release-build-config-design.md) §A.1。

### 验证门（四道全绿才算过）

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew assembleDebug
./gradlew :app:assembleRelease
```

## 调试技巧

### adb 查 Room DB（debug 构建 run-as）

```bash
# 看库文件列表
adb shell run-as com.shijiben ls databases/

# 直接执行 SQL（也可 pull 出来用 DB Browser 查看）
adb shell run-as com.shijiben sqlite3 databases/shijiben.db "SELECT * FROM events LIMIT 5;"
```

### logcat 过滤

```bash
# 按 ViewModel 标签
adb logcat -s TimelineViewModel HeatmapViewModel TimeVizViewModel

# 按应用 PID（更全）
adb shell pidof com.shijiben   # 拿到 pid
adb logcat --pid=<pid>
```

### 清单与安装信息

```bash
# 看 applicationId / minSdk / targetSdk / 权限
aapt dump badging app/build/outputs/apk/debug/app-debug.apk

# 看安装后实际信息
adb shell dumpsys package com.shijiben
```

## 常见问题 FAQ

**Q1：keystore.properties 缺失怎么办？**
A：release 走 fallback 产出 unsigned APK，构建仍成功，不阻塞 CI/全新 clone。若需签名 APK，参考 [docs/superpowers/specs/2026-06-28-release-build-config-design.md](docs/superpowers/specs/2026-06-28-release-build-config-design.md) §A.1 重新生成（即上方「Keystore 配置」子节）。

**Q2：flaky test 是否复发？**
A：迭代 9 方案 A 根治（路由 Room executor 到 StandardTestDispatcher，从根上消除 teardown 竞态），5 次独立验证全绿，未复发。详见 [docs/superpowers/specs/2026-06-28-flaky-rootfix-and-docs-design.md](docs/superpowers/specs/2026-06-28-flaky-rootfix-and-docs-design.md)。

**Q3：如何重置生日（TimeViz）？**
A：`adb shell pm clear com.shijiben` 清除 app 全部数据（含 SharedPreferences 与 Room DB），重开 app 触发首次设置流程重新输入生日。注意：会同时清空所有事件与随笔，操作前请先导出备份。

**Q4：测试 daemon 卡住怎么办？**
A：`./gradlew --stop` 停 daemon 后重试，或本次构建加 `--no-daemon` 跑一次。来自 quality-scorecard backlog `daemon-stall`（迭代 9 观察到的 daemon 内存累积现象，非代码缺陷，重跑可成功）。

## 分期

- **V1**：时间轴 + 记录 + 8-bit 主题 + 随笔基础
- **V2**：时间可视化（今天/今年/一生）+ 热力图月视图回看 + 随笔完整化（已落地）
- **V3**：设置页 + 数据导出/备份 + 数据导入 + 年视图 + 搜索 + 其他打磨（设置页 + 数据导出/导入 + 年视图 + 搜索已落地）

## Spec 索引

- [2026-06-27-top-bottom-redesign-design.md](docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md) — 顶底重设计 + 标签移除
- [2026-06-28-homepage-composition-rebalance-design.md](docs/superpowers/specs/2026-06-28-homepage-composition-rebalance-design.md) — 首页构图重平衡（底部双 block 替代悬浮加号）
- [2026-06-28-time-visualization-design.md](docs/superpowers/specs/2026-06-28-time-visualization-design.md) — 时间可视化
- [2026-06-28-heatmap-design.md](docs/superpowers/specs/2026-06-28-heatmap-design.md) — 热力图月视图
- [2026-06-28-settings-privacy-backlog-design.md](docs/superpowers/specs/2026-06-28-settings-privacy-backlog-design.md) — 设置页 + 隐私政策 + backlog 修复
- [2026-06-28-data-export-and-flaky-fix-design.md](docs/superpowers/specs/2026-06-28-data-export-and-flaky-fix-design.md) — 数据导出（JSON + SAF）+ flaky test 修复
- [2026-06-28-splash-docs-nit-cleanup-design.md](docs/superpowers/specs/2026-06-28-splash-docs-nit-cleanup-design.md) — Splash 接入 + 文档对齐 + nit 清理（本 spec）
- [2026-06-28-release-build-config-design.md](docs/superpowers/specs/2026-06-28-release-build-config-design.md) — Release 构建配置（签名 + 混淆 + 缩减 + ProGuard 规则）（本 spec）
- [2026-06-28-fallback-fix-and-icons-cleanup-design.md](docs/superpowers/specs/2026-06-28-fallback-fix-and-icons-cleanup-design.md) — fallback 修复 + 应用图标清理
- [2026-06-28-data-import-design.md](docs/superpowers/specs/2026-06-28-data-import-design.md) — 数据导入（JSON 解析 + SAF 读取 + DB 写入）
- [2026-06-28-flaky-rootfix-and-docs-design.md](docs/superpowers/specs/2026-06-28-flaky-rootfix-and-docs-design.md) — flaky test 根治（路由 Room executor）+ 文档打磨
- [2026-06-28-heatmap-year-view-design.md](docs/superpowers/specs/2026-06-28-heatmap-year-view-design.md) — 热力图年视图（12 月迷你月历拼贴）
- [2026-06-28-search-design.md](docs/superpowers/specs/2026-06-28-search-design.md) — 搜索（events.title/note + notes.content 全文检索，LIKE 内存过滤）
- [2026-06-29-time-allocation-design.md](docs/superpowers/specs/2026-06-29-time-allocation-design.md) — 时间去向聚合（HeatmapScreen 3 tab 容器 + TimeAllocationCalculator/ViewModel/Tab）
- [2026-06-29-debug-console-overlay-design.md](docs/superpowers/specs/2026-06-29-debug-console-overlay-design.md) — 调试控制台悬浮 overlay（DebugOverlay + DebugLog ring buffer）

## Recent changes (better cycles)
- security: DataImportManager 导入 DoS 防护——readFromStream 改 8KB 分块读取并加 50MB 字节上限 + parseEvents/parseNotes 加 10 万条数组长度上限，超限抛 IllegalArgumentException（被 ImportViewModel catch 走 Error 提示）+ 新增 3 个回归测试
- tests: 为 TimeVizCalculator.todayProgress/yearProgress 新增 7 个零覆盖纯函数单测（00:00/12:00/23:59:59 三点 + 年初/年中/年末三点 + 闰年 vs 非闰年分母比值=365/366），UTC 时区固定，全区间断言防浮点抖动
- feat: 时间去向聚合——HeatmapScreen 重构为 3 tab 容器（月/年/去向），新增 TimeAllocationCalculator（纯函数按标题聚合 completed 事件时长）+ TimeAllocationViewModel + TimeAllocationTab（范围选择器+水平条形图列表），废弃 HeatmapYearScreen 独立路由
- docs: AGENT.md 项目结构树 + ARCHITECTURE.md §2 包路径列表补充 `ui/debug/` 模块（DebugOverlay + DebugLog）——此前 MainActivity/Application 实际使用但文档未记录，AI agent/开发者不知道有 app 内调试控制台
- dx: 测试依赖版本管理统一——5 处 testImplementation 硬编码版本（junit/robolectric/androidx.test:core/coroutines-test/truth）改用 rootProject.extra 引用，新增 4 个 extra key，coroutines-test 复用既有 coroutines key——零行为变化，消除主/测试版本不同步风险
- ux: RecordingSheet 保存按钮加 enabled=title.isNotBlank()——title 为空时按钮禁用并显示 Disabled 灰色，提供即时视觉反馈（此前按钮始终可点但 save() 静默失败无反馈）
- architecture: 移除 EventRepository.shiftToTargetDay 的死代码 cal2（创建并赋值 e.endTime 但从未读取，推测为「保留钟点」改「保留时长」后的残留）——零行为变化
- security: DataImportManager.parseEvents 新增 status 信任边界校验——非法值（非 0/1/2）抛 IllegalArgumentException 拒绝导入，防止恶意文件污染事件状态机 + 新增回归测试
- tests: 为 EventRepository.shiftToTargetDay 新增 5 个直接单测（hour:minute 保留 / 跨月边界 / null endTime / 已到目标日 / 时长保留），此前仅通过慢速 DB 集成测试间接覆盖
- correctness: RecordingViewModel.save() 修复非法状态——编辑 Completed 事件并把 duration 调到 0 时，降级为 InProgress（避免保存 Completed+endTime=null，违反 EventRepository 不变式）+ 新增回归测试
- correctness: RecordingViewModel.save() 新增 `start > now → NotStarted` 分支——未来开始时间的事件（duration>0）此前误标 InProgress，破坏状态机不变式并虚增热力图时长，现与 determineStatus 不变式对齐 + 新增回归测试
- ui: NotesScreen 标题栏重构为 Row + SurfaceColor + 2dp 黑色分隔线，与其他 5 屏标准结构一致（移除 PixelCard 包裹 + 加 tint=TextPrimary + 加 Color/Surface/TextPrimary imports）
- ui: NotesScreen 标题字号统一为 16sp Bold，与其他 5 屏一致（移除 titleLarge 22sp + MaterialTheme import）
- ui: NotesScreen 补齐 RainbowTrim 品牌条 + Background，与其他 7 屏一致
- ui: disabled 颜色硬编码改用 Disabled/DisabledText 令牌，统一 4 文件 11 处
- ui: 移除 TimeVizScreen 的 PixelOutlinedButtonLocal 本地副本，改用共享 PixelOutlinedButton 统一按钮风格
- ux: TimelineScreen 点击随笔改为内联打开 NoteEditorSheet（对齐 SearchScreen 模式）——EventList.onNoteClick 改 (NoteEntity)->Unit 转发具体随笔，新增 showNoteSheet/editingNote 状态 + NoteEditorSheet 渲染块，save/delete 后调 viewModel.refresh() 同步；底部 ✎ 图标仍跳转随笔列表（view-all 意图）
- security: AndroidManifest allowBackup 改 false + fullBackupContent=false + dataExtractionRules=null——关闭 Auto Backup（Google Drive 上传）与 adb backup 提取，对齐隐私政策「数据不离开本设备、无备份」承诺
- ui: NoteEditorSheet 标题移除最后一个 MaterialTheme.typography.titleLarge 残留——改用显式 20sp Bold + TextPrimary，与 6 屏标准一致（PixelText 旧别名→TextPrimary 令牌）
- docs: AGENT.md 结构树 heatmap 行同步 cycle 13 重构（HeatmapYearScreen→HeatmapYearTab + 补 5 个 TimeAllocation/MonthTab 文件 + 标注 3 tab 容器）+ Spec 索引补 2 条 2026-06-29 spec（time-allocation + debug-console-overlay）
- correctness: initEdit Completed 分支 coerceIn(300,1440)→(0,1440) + TimeRangeSlider ABS_MIN 300→0——修复编辑凌晨 5 点前已完成事件时 startTime 被静默钳制到 5:00 的数据损坏 + 新增回归测试
- performance: TimelineScreen 60s `now` tick 改为 `State<Long>` 透传——drawBehind 读 nowState（顶栏进度条 draw-phase 重绘不重组）、新增 RowScope.StatsText + ElapsedBadge 叶子组合件独读 nowState.value、EventCard/EventList/DayProgressBar 参数改 nowState，整树 20 卡片/分钟重组降至 1（仅 in-progress）
- architecture: DataImportManager 去除对 feature 层 TimeVizPrefs 的依赖——applyImport 签名移除 timeVizPrefs 参数 + ImportCounts 去 prefsUpdated 字段，prefs 写回职责上移到 ImportViewModel（data 层零向上引用，仿 DataExportManager 接原语范式）
- dx: 依赖版本管理迁移到 Gradle version catalog（libs.versions.toml）——root/app build 脚本删除 14 处 extra 定义 + 21 处 rootProject.extra 引用，改用类型安全 libs.xxx 访问器，IDE 自动补全 + 编译期拼写检查，零行为变化
