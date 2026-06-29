# 事记本（ShiJiBen）

事记本是一个纯本地的时间记录 Android app，受《奇特的一生》启发，采用 8-bit 像素美学。它不设 TODO、不强迫复盘，所有数据仅存在你的设备上。核心理念：单纯记录生活，让记录本身成为习惯，通过长期记录看见时间去向，减少内耗。

## 功能列表

- **事件记录**：创建/编辑/删除事件，含起止时间与耗时；状态由保存时根据事实自动推算（未开始 / 进行中 / 已结束），无需手动维护。
- **自动顺延**：当天结束时仍为「未开始」的事件，下次打开 app 时自动顺延到次日相同时刻——不让预写的事彻底消失在意识之外。
- **随笔**：创建、列表查看、编辑、删除文字随笔，与事件独立成表。
- **时间轴主视图**：24 小时彩虹色时间条 + 单行紧凑事件卡片 + 像素山水背景；当前小时有红色边框标记。
- **时间可视化**：今天剩余时间 / 今年剩余时间 / 一生剩余时间，静静可见，不制造焦虑。
- **热力图回看**：月视图按天展示记录覆盖比例（空白方块 = 未记录的时间），年视图用 12 个迷你月历拼贴一整年。
- **搜索**：同时检索事件标题/备注与随笔内容，关键词命中即返回。
- **数据导出**：将事件与随笔导出为 JSON 文件，通过 Android SAF 由用户自选存储位置。
- **数据导入**：从 JSON 文件恢复数据，REPLACE 幂等策略，重复导入安全。
- **设置**：含关于页与隐私政策入口。

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose（BOM 2024.10.01）
- **架构**：MVVM + Repository
- **本地存储**：Room 2.6.1
- **依赖注入**：Hilt 2.52
- **异步**：Kotlin Coroutines / Flow
- **导航**：Jetpack Navigation Compose
- **构建**：AGP 8.7.3，Kotlin 2.0.21，JVM 17
- **SDK**：minSdk 26 / targetSdk 34 / compileSdk 34
- **版本**：versionName 1.0

## 项目结构

代码组织为三大块：`data/`（数据库、Entity、DAO、Repository、导出导入）、`feature/`（按功能划分的屏幕与 ViewModel：timeline / recording / notes / heatmap / timeviz / search / settings）、`ui/theme/`（8-bit 色板、像素组件、字体配置集中管理）。

完整的目录树与各模块职责说明见 [AGENT.md](AGENT.md) 的「项目结构」节。

## 构建说明

快速构建并安装 debug APK：

```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

完整的 Release 配置（签名 / R8 混淆 / 资源缩减 / Keystore）、四道验证门（compileDebugKotlin / testDebugUnitTest / assembleDebug / assembleRelease）的命令与含义，见 [AGENT.md](AGENT.md) 的「构建与安装」节。

## 使用指南

- **记一件事**：首页底部「记事」入口 → 弹窗输入标题 → 像素滑块选开始时间与持续时间 → 保存。状态由 app 自动推算。
- **写一段随笔**：首页底部「随笔」入口 → 输入内容 → 保存。
- **看时间去哪了**：进入时间可视化页，看今天 / 今年 / 一生剩余时间。
- **回看记录**：热力图月视图按天看记录覆盖比例，切到年视图看整年 12 个月迷你月历拼贴。
- **搜索历史**：搜索页输入关键词，事件与随笔同时返回命中结果。
- **备份数据**：设置 → 导出，选 SAF 提供的存储位置保存 JSON 文件。
- **恢复数据**：设置 → 导入，选先前导出的 JSON 文件，REPLACE 幂等。

## 隐私声明

事记本是纯本地 app：

- **零网络请求**：无任何 HTTP 调用、远程 API、云同步、推送通道、统计 SDK、广告 SDK。
- **零权限声明**：`AndroidManifest.xml` 不含任何 `<uses-permission>`（无 INTERNET 等）。
- **数据仅存本机**：所有事件与随笔存储在设备本地的 Room 数据库中，卸载即失，请定期导出备份。
- **导出文件由用户掌控**：导出时由用户通过系统文件选择器（SAF）自选存储位置，不经任何第三方。
