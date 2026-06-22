# AGENT.md — 事记本（ShiJiBen）项目上下文

> 本文档供 AI agent 阅读，提供项目约定和关键上下文。

## 项目概述
时间本是一个纯本地的时间记录应用，受《奇特的一生》启发。核心理念：单纯记录生活，不设 TODO、不强迫复盘，通过长期记录看见时间去向，减少内耗。

## 技术栈
- **框架**：Flutter（Android 优先，支持 Web 预览开发）
- **语言**：Dart
- **本地存储**：SQLite + drift（类型安全 ORM）
- **状态管理**：Riverpod
- **路由**：go_router

## 项目结构
```
shijiben/
  docs/            # 文档：设计 spec、计划
  design/          # 设计思考、HTML mockup
  lib/
    core/          # 主题、8-bit 调色板、共享组件
      theme/       # 色板、字体、像素边框组件
      widgets/     # 共享 UI 组件
    data/          # drift 数据库、表定义、DAO
      database/
      tables/
      daos/
    features/
      timeline/    # 时间轴主视图
      recording/   # 三种记录方式
      tags/        # 标签管理
      heatmap/     # 热力图（v2）
      time_viz/    # 时间可视化（v2）
    main.dart
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
- **开发预览**：`flutter run -d chrome`，日常 UI 迭代在浏览器中完成
- **真机测试**：阶段性在 Android 设备上验证
- **状态管理**：使用 Riverpod provider，避免在 widget 中直接访问数据库
- **数据访问**：通过 DAO 层，不直接写 SQL
- **8-bit 美学**：所有视觉元素集中在 `core/theme`，色板使用柔和复古风格
- **命名**：文件用 snake_case，类用 PascalCase

## 设计哲学（重要）
1. **记录即审视**：App 反映现实，不评判现实
2. **轻量存在**：不制造焦虑，时间可视化静静在那
3. **不是 TODO**：预写事件不是待办，是"我打算做"，没做就顺延，不催不罚

## 分期
- **V1**：时间轴 + 三种记录 + 标签 + 8-bit 主题 + 随笔基础
- **V2**：热力图 + 时间可视化 + 随笔完整化
- **V3**：搜索、导出、备份
