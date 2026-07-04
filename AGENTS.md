# AGENTS.md — 事记本（ShiJiBen）项目上下文

> 供 AI agent 阅读的项目约定与关键上下文。
> 设计 spec 与变更历史见 `git log`，不在本仓库维护独立文档。

## 项目概述

事记本是一个纯本地 Android 时间记录应用，受《奇特的一生》启发。核心理念：单纯记录生活，不设 TODO、不强迫复盘，通过长期记录看见时间去向，减少内耗。

## 技术栈

- **平台**：Android 原生应用
- **语言**：Kotlin + Jetpack Compose
- **架构**：MVVM + Repository + Hilt DI
- **存储**：Room（SQLite）
- **异步**：Coroutines + Flow
- **导航**：Jetpack Navigation Compose

## 项目结构

`app/src/main/java/com/shijiben/`
- `data/local/`：Entity、DAO、Database（EventEntity, NoteEntity, AppDatabase）
- `data/repository/`：EventRepository, NoteRepository
- `data/export/`：数据导出/导入
- `feature/`：heatmap / notes / recording / search / settings / timeline / timeviz
- `ui/theme/`：8-bit 色板、字体、像素组件
- `navigation/`：AppNavHost
- `MainActivity.kt` / `ShiJiBenApplication.kt`

## 数据模型

两张核心表：
- **Event**：start_time / end_time / status / title / note
- **Note**：timestamp / content

### 事件状态流转

`not_started` → `in_progress` → `completed`

- 预写 = not_started
- 开始计时 = in_progress（end_time = null）
- 停止 = completed
- 当天结束仍 not_started → App 打开时自动顺延到次日

改 status 推算逻辑前务必读 `EventRepository/EventEntity` 中的状态机实现与 guard。

## 开发约定

- 状态管理：ViewModel + Flow，避免 UI 层直接访问数据库
- 数据访问：通过 Repository 层，不直接写 SQL
- 视觉：8-bit 像素美学，所有视觉元素集中在 `ui/theme`
- 命名：文件 PascalCase，包名/资源 snake_case

## 设计哲学

1. **记录即审视**：App 反映现实，不评判现实
2. **轻量存在**：不制造焦虑，时间可视化静静在那
3. **不是 TODO**：预写事件不是待办，没做就顺延，不催不罚

## 构建

```bash
./gradlew assembleDebug
```

## 验证门

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew assembleDebug
./gradlew :app:assembleRelease
```

## 真机交付

新需求通过验证门后，若用户设备已连接 ADB，直接推送安装到真机验证：

```bash
adb devices
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

未连接设备时只完成构建；安装成功后交由用户在真机上确认，才算需求完成。
