# Findings: docs

## F012 — DONE (cycle 12)
- 文件：`AGENT.md`「项目结构」节 + `docs/ARCHITECTURE.md` §2 实际包路径列表
- 符号：`ui/debug/` 模块（DebugOverlay.kt + DebugLog.kt）未在文档中记录
- 问题：`MainActivity.kt:29` 用 `DebugOverlay { AppNavHost() }` 包裹 app，`ShiJiBenApplication.kt:12` 调 `DebugLog.install(this)`，但 AGENT.md 项目结构树和 ARCHITECTURE.md 包路径列表均无 `ui/debug/`——AI agent/开发者不知道有 app 内调试控制台
- 修复：AGENT.md 项目结构树补充 `ui/debug/` 节点；ARCHITECTURE.md §2 包路径列表补充 `ui/debug/` 条目（含 DebugOverlay/DebugLog 职责说明）
- evidence：AGENT.md「项目结构」树形图 + docs/ARCHITECTURE.md §2 包路径列表均缺 ui/debug/，对照 MainActivity.kt:29 / ShiJiBenApplication.kt:12 实际使用
- impact：M

## F022 — AGENT.md heatmap 结构树 + Spec 索引未同步 cycle 13 TimeAllocation 重构
- status: DONE (cycle 22)
- evidence: AGENTS.md:38
- impact: M
