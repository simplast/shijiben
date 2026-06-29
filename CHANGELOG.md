# 更新日志 (CHANGELOG)

> 本文件记录「史记本」(shijiben) 的版本演进，面向开发者 / AI agent / 应用商店审核材料。
> 终端用户向文档见 [README.md](./README.md)。
> 格式遵循 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，版本号对齐
> `app/build.gradle.kts` 的 `versionName`。

## [v1.0] - 2026-06-28

### 核心功能
- 事件 CRUD + 耗时 + 状态流转（进行中/已完成/未开始）+ 顺延
- 随笔（notes）独立 CRUD，与事件并列展示
- 时间轴（Timeline）按日聚合，事件 + 随笔混合渲染
- V3 搜索：events.title/note + notes.content 全文检索（LIKE 内存过滤）
- 热力图回看：月视图（6×7 网格）+ 年视图（12 月迷你月历拼贴）

### 时间可视化
- 今天还剩多少时间（"24h 0m" 格式）
- 今年还剩多少时间（"今年还有 N 天 M 小时" 格式）
- 这一生还剩多少时间（yearsLived / yearsRemaining / exceeded 三字段，含未来生日防御 clamp）

### 数据可移植性
- 数据导出：JSON + SAF（系统文件选择器），含 appVersion / exportedAt 元数据
- 数据导入：JSON 导入，REPLACE 幂等，备份/恢复闭环
- 卸载/换机不丢数据

### 上架成熟度
- release 构建配置：签名 + 混淆 + 资源缩减 + ProGuard
- keystore.properties 缺失时 fallback 产出 unsigned APK（CI/全新克隆不破）
- splash 启动屏（米白 + 像素图标 → 首页无缝切换）
- 隐私政策页（本地化 app，无联网，无数据上报）

### 文档
- README.md（面向人类的仓库入口）
- AGENT.md（面向 AI agent 的项目上下文 + 调试技巧 + FAQ）
- ARCHITECTURE.md（4 视角：模块依赖图 + 数据流 + 状态管理 + 导航图，ASCII art）
- docs/superpowers/specs/（18+ 轮设计 spec 历史）
- docs/superpowers/loop/（iteration-log.md 迭代日志 + quality-scorecard.md 质量评分）

### 质量保障
- 208 测试覆盖（7 个 ViewModel + Repository 边界 + 纯函数单测，含本轮 +8 边界用例）
- 四道验证门全绿：compileDebugKotlin / testDebugUnitTest --rerun-tasks / assembleDebug / assembleRelease --rerun-tasks
- flaky test 根治（迭代9 方案 A：路由 Room executor 到 StandardTestDispatcher，5 次独立验证全绿）
- 已知 caveat：release APK 真机启动验证（需真机，orchestrator 无法执行）
