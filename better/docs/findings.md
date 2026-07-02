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

## F032 — ARCHITECTURE.md 新增 §8 事件状态机（不变式 + guard + 状态图）
- status: DONE (cycle 32)
- 文件：`docs/ARCHITECTURE.md`（新增 §8）+ `AGENTS.md`（事件状态流转节加交叉引用）
- 符号：`EventRepository.determineStatus` / `RecordingViewModel.save` / `carryOverNotStarted` / `markInProgress` / `markCompleted` / `markNotStarted`
- 问题：事件状态机的 3 个状态不变式（Completed 必须有 endTime、InProgress 必须 endTime=null）+ save() 的两条防非法状态降级 guard（duration=0+原Completed→InProgress、duration>0+start>now→NotStarted）来自 cycle 25/26 的 bug 修复，但完全未文档化。AGENTS.md 只有一行 `not_started → in_progress → completed`，ARCHITECTURE.md §4 只讲 UI 状态管理。改 status 推算逻辑时极易破坏不变式。
- 修复：ARCHITECTURE.md 新增 §8「事件状态机」——含 ASCII 状态图、状态不变式表、determineStatus 纯函数伪码、4 个手动转移方法、save() 的 3 条 guard 规则，并标注 guard 来源（cycle 25/26 bug 修复）。AGENTS.md 事件状态流转节加 blockquote 交叉引用指向 §8。
- evidence：docs/ARCHITECTURE.md §8（L185-240）；AGENTS.md L73 交叉引用
- impact：M（文档化关键不变式，防止未来改动破坏状态机）
