# AGENT.md — 事记本（ShiJiBen）项目上下文

> 本文档供 AI agent 阅读，提供项目约定和关键上下文。
> 2026-06-27 更新：标签（Tag）功能已移除，DB v1→v2 迁移见 `AppDatabase.kt` 的 `MIGRATION_1_2`（建新表-拷数据-删旧-改名）。

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
      util/        # 共享工具函数（DateUtils：todayTriple/isToday/isPastDay，3 feature 文件共用）
      MainActivity.kt
      ShiJiBenApplication.kt
    src/main/res/
    build.gradle.kts
    AndroidManifest.xml
  build.gradle.kts
  settings.gradle.kts
```

## 数据模型

两张核心表（见 `EventEntity` / `NoteEntity` 与 [ARCHITECTURE.md](docs/ARCHITECTURE.md) §1）：

- **Event**：事件，有 start_time/end_time/status/note（可选）
- **Note**：随笔，有 timestamp/content

### 事件状态流转

`not_started` → `in_progress` → `completed`

- 预写 = not_started
- 开始计时 = in_progress（end_time = null）
- 停止 = completed
- 当天结束仍 not_started → App 打开时自动顺延到次日

> 状态不变式 + guard + save() 降级规则详见 [ARCHITECTURE.md §8 事件状态机](docs/ARCHITECTURE.md#8-事件状态机)。改 status 推算逻辑前务必读该节。

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
- 若需重新生成 keystore，用 `keytool` 生成自签名 dev keystore，写入 `keystore.properties` 即可。

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
A：release 走 fallback 产出 unsigned APK，构建仍成功，不阻塞 CI/全新 clone。若需签名 APK，按上方「Keystore 配置」子节重新生成 keystore。

**Q2：flaky test 是否复发？**
A：迭代 9 方案 A 根治（路由 Room executor 到 StandardTestDispatcher，从根上消除 teardown 竞态），5 次独立验证全绿，未复发。详见 [ARCHITECTURE.md](docs/ARCHITECTURE.md) §7.5。

**Q3：如何重置生日（TimeViz）？**
A：`adb shell pm clear com.shijiben` 清除 app 全部数据（含 SharedPreferences 与 Room DB），重开 app 触发首次设置流程重新输入生日。注意：会同时清空所有事件与随笔，操作前请先导出备份。

**Q4：测试 daemon 卡住怎么办？**
A：`./gradlew --stop` 停 daemon 后重试，或本次构建加 `--no-daemon` 跑一次。来自 quality-scorecard backlog `daemon-stall`（迭代 9 观察到的 daemon 内存累积现象，非代码缺陷，重跑可成功）。

## 分期

- **V1**：时间轴 + 记录 + 8-bit 主题 + 随笔基础
- **V2**：时间可视化（今天/今年/一生）+ 热力图月视图回看 + 随笔完整化（已落地）
- **V3**：设置页 + 数据导出/备份 + 数据导入 + 年视图 + 搜索 + 其他打磨（设置页 + 数据导出/导入 + 年视图 + 搜索已落地）

> 设计 spec 与变更历史见 `git log`，不在本仓库维护独立文档。架构详情见 [ARCHITECTURE.md](docs/ARCHITECTURE.md)。
