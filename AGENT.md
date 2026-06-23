# AGENT.md — 事记本（ShiJiBen）项目上下文

> 本文档供 AI agent 阅读，提供项目约定和关键上下文。

## 项目概述

事记本是一个纯本地的时间记录应用，受《奇特的一生》启发。核心理念：单纯记录生活，不设 TODO、不强迫复盘，通过长期记录看见时间去向，减少内耗。

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
  design/          # 设计思考、HTML mockup
  app/
    src/main/java/com/shijiben/
      core/        # 主题、8-bit 调色板、共享组件
        theme/     # 色板、字体、像素风格
        ui/        # 共享 UI 组件
      data/        # Room 数据库、Entity、DAO、Repository
        local/     # Entity、DAO、Database
        repository/# Repository 实现
      features/
        timeline/  # 时间轴主视图
        recording/ # 记录功能
        tags/      # 标签管理
        heatmap/   # 热力图（v2）
        time_viz/  # 时间可视化（v2）
    src/main/res/  # Android 资源
    build.gradle.kts
    AndroidManifest.xml
  build.gradle.kts
  settings.gradle.kts
```

## 数据模型

三张核心表，详见 [docs/2026-06-22-shijiben-design.md](docs/2026-06-22-shijiben-design.md)：

- **Event**：事件，有 start_time/end_time/status/tag_id
- **Note**：随笔，有 timestamp/content
- **Tag**：标签，有 name/color/sort_order

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
- **8-bit 美学**：所有视觉元素集中在 `core/theme`，色板使用活泼高饱和 NES 风格
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

## 分期

- **V1**：时间轴 + 记录 + 标签 + 8-bit 主题 + 随笔基础
- **V2**：热力图 + 时间可视化 + 随笔完整化
- **V3**：搜索、导出、备份
