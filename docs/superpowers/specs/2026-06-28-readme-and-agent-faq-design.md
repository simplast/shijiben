# README.md 新增 + AGENT.md 调试/FAQ 补充 设计 spec

> 日期：2026-06-28
> 迭代：13（Design subagent 落 spec）
> 范围：S+（纯文档，零代码改动）
> 前置：迭代 12 已完成（文档可维护性 8/10），项目根目录无 README.md
> 验证门：四道闸 baseline 验证零回归（本轮无代码改动，预期全绿）

## 一、问题

1. **项目根目录无 README.md**：公开仓库（98/100、上架成熟度满分）首屏可见的真实文档缺口。访客落地页无引导，需点进 AGENT.md / docs/ 才能了解项目。
2. **AGENT.md 缺调试技巧 + FAQ**：现有 9 节覆盖概述/技术栈/结构/数据模型/约定/哲学/构建/分期/Spec 索引，但开发者常见操作（adb 查 DB / logcat 过滤 / 清单查看）和常见疑问（keystore 缺失 / flaky 是否复发 / 重置生日 / daemon 卡住）散落各 spec 或仅在 quality-scorecard backlog 中，无集中入口。

## 二、目标

1. **新增 README.md**（项目根）：面向人类用户/开发者的精简入口，简介 + 功能 + 使用 + 隐私，开发者内容指向 AGENT.md（避免重复维护）。
2. **AGENT.md 补两节**：调试技巧段 + 常见问题 FAQ 段，集中入口，引用既有 spec/scorecard 不重写。

## 三、非目标

- 不加 License（付费上架 app，非开源）
- 不加截图占位（无截图素材，文字描述即可）
- 不加 Contributing（个人项目，非开源协作）
- 不改业务代码、不改 DB schema、零新依赖
- README 不面向 AI agent（AGENT.md 才面向 AI），不提及内部迭代/scorecard 细节
- 不写 README 全文（本 spec 给大纲，Coding subagent 据此填充）

## 四、设计

### §4.1 README.md 结构（项目根，面向用户/开发者）

| 章节 | 内容大纲 |
|------|----------|
| 标题 + 一句话简介 | 事记本（ShiJiBen）—— 纯本地时间记录 app，受《奇特的一生》启发，8-bit 像素美学。不设 TODO、不强迫复盘，记录即审视。 |
| 功能列表 | 事件 CRUD + 耗时记录 + 状态自动流转（not_started/in_progress/completed）+ 自动顺延；随笔（创建/列表/编辑/删除）；时间轴主视图（24h 彩虹色条 + 单行紧凑卡片 + 像素山水背景）；时间可视化（今天/今年/一生剩余时间）；热力图回看（月视图按天覆盖比例 + 年视图 12 月拼贴）；搜索（事件标题/备注 + 随笔内容全文检索）；数据导出（JSON + SAF）+ 数据导入；设置（含隐私政策入口） |
| 技术栈 | Kotlin + Jetpack Compose + Room 2.6.1 + Hilt 2.52 + Coroutines/Flow + Navigation Compose；minSdk 26 / targetSdk 34 / compileSdk 34；JVM 17 |
| 项目结构 | 一段精简描述（data / feature / ui/theme 三大块），完整结构指向 `AGENT.md` 的"项目结构"节，避免重复维护 |
| 构建说明 | 给 debug 构建命令（`./gradlew assembleDebug` + `adb install`），完整构建/Release/Keystore/四道验证门指向 `AGENT.md` 的"构建与安装"节 |
| 使用指南 | 记事：底部"记事" block → 弹窗输入标题 + 像素滑块选开始时间/持续时间 → 保存（状态自动推算）；随笔：底部"随笔" block → 输入内容；时间可视化：TimeViz 页看今天/今年/一生剩余时间；热力图回看：月视图按天看记录覆盖比例，年视图 12 月迷你月历拼贴；搜索：搜索页输入关键词，同时检索事件和随笔；备份：设置 → 导出 JSON 到 SAF（用户选位置）/ 导入 JSON 恢复（REPLACE 幂等） |
| 隐私声明 | 纯本地 app，无任何网络请求 / 远程 API / 云同步 / 推送 / 统计 SDK；`AndroidManifest.xml` 零权限声明（无 INTERNET 等）；数据仅存本机；导出文件由用户主动选择存储位置，不经任何第三方 |

### §4.2 AGENT.md 补充（插入位置：`## 构建与安装` 节之后、`## 分期` 节之前）

**新增节 A：`## 调试技巧`**

- **adb 查 DB**（debug 构建可 run-as）：
  - `adb shell run-as com.shijiben ls databases/`（看库文件）
  - `adb shell run-as com.shijiben sqlite3 databases/shijiben.db "SELECT * FROM events LIMIT 5;"`（或 pull 出来用 DB Browser）
- **logcat 过滤**：
  - 按 ViewModel 标签：`adb logcat -s TimelineViewModel HeatmapViewModel TimeVizViewModel`
  - 按应用 PID：`adb shell pidof com.shijiben` → `adb logcat --pid=<pid>`
- **清单查看**：
  - `aapt dump badging app/build/outputs/apk/debug/app-debug.apk`（看 applicationId / minSdk / targetSdk / 权限）
  - `adb shell dumpsys package com.shijiben`（看安装后实际信息）

**新增节 B：`## 常见问题 FAQ`**

- **Q1：keystore.properties 缺失怎么办？** → A：release 走 fallback 产 unsigned APK，构建不破。需签名则参考 [2026-06-28-release-build-config-design.md](docs/superpowers/specs/2026-06-28-release-build-config-design.md) §A.1 重新生成（指向 AGENT.md 既有"Keystore 配置"子节）
- **Q2：flaky test 是否复发？** → A：迭代 9 方案 A 根治（路由 Room executor 到 StandardTestDispatcher），5 次独立验证全绿。详见 [2026-06-28-flaky-rootfix-and-docs-design.md](docs/superpowers/specs/2026-06-28-flaky-rootfix-and-docs-design.md)
- **Q3：如何重置生日（TimeViz）？** → A：`adb shell pm clear com.shijiben` 清除 app 数据（含 SharedPreferences），重开 app 触发首次设置流程
- **Q4：测试 daemon 卡住怎么办？** → A：`./gradlew --stop` 停 daemon，或加 `--no-daemon` 跑一次。来自 quality-scorecard backlog `daemon-stall`（迭代 9 观察，非代码缺陷）

## 五、涉及文件清单

| 操作 | 文件 | 说明 |
|------|------|------|
| 新增 | `README.md`（项目根） | 面向用户/开发者入口，按 §4.1 大纲填充 |
| 修改 | `AGENT.md` | 在"构建与安装"与"分期"之间插入"调试技巧"+"常见问题 FAQ"两节 |

零代码改动，零新依赖。

## 六、边界情况

- README 与 AGENT.md 内容不重复：README 项目结构/构建说明用"一段精简 + 指向 AGENT.md"模式，各司其职
- README 不提及内部迭代细节（迭代编号 / scorecard / backlog ID），保持面向用户的纯净入口
- FAQ 引用 quality-scorecard backlog（`daemon-stall`）和既有 spec（release-build-config / flaky-rootfix），不重写内容
- AGENT.md 插入位置不破坏现有 Spec 索引锚点（新节在"分期"之前，Spec 索引仍在文末）

## 七、测试清单

本轮无新测试，无代码改动。验证门四道闸 baseline 验证零回归：

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew assembleDebug
./gradlew :app:assembleRelease
```

预期：四道全绿（与迭代 12 结束状态一致）。文档改动不影响编译/测试/打包。
