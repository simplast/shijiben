# Plan: docs cycle 12 — F012

## Finding
- ID: F012
- 标题：`ui/debug/` 模块（DebugOverlay + DebugLog）未在 AGENT.md / ARCHITECTURE.md 中记录——代码与文档漂移
- evidence：
  - `AGENT.md`「项目结构」节列出 `ui/theme/` 但无 `ui/debug/`
  - `docs/ARCHITECTURE.md` 模块依赖图（§2）和实际包路径列表均无 `ui/debug/`
  - 实际代码：`MainActivity.kt:29` 用 `DebugOverlay { AppNavHost() }` 包裹整个 app 内容；`ShiJiBenApplication.kt:12` 调用 `DebugLog.install(this)` 注册全局异常处理
- impact：M（AI agent/开发者不知道有调试控制台，排查问题时可能错过 app 内悬浮调试按钮这个工具）
- effort：S

## 现状
`ui/debug/` 目录含两个文件：
- `DebugOverlay.kt`：`@Composable fun DebugOverlay(content)`，包裹 app 内容，仅 `BuildConfig.DEBUG` 时叠加可拖动悬浮小圆点，点击展开全屏调试面板
- `DebugLog.kt`：`object DebugLog`，内存 ring buffer（200 条）+ 未捕获异常持久化到 `debug-last-crash.txt`，release 构建所有方法空操作

消费方：
- `MainActivity.kt:29` — `DebugOverlay { AppNavHost() }`
- `ShiJiBenApplication.kt:12` — `DebugLog.install(this)`

文档缺失：
- `AGENT.md`「项目结构」树形图无 `ui/debug/`
- `AGENT.md`「项目结构」树形图 `ui/theme/` 注释为「8-bit 色板、字体、像素组件」，但未提 `ui/debug/`
- `docs/ARCHITECTURE.md` §2 模块依赖图无 `ui/debug/`，§2 实际包路径列表无 `ui/debug/`

## 修复方案
在 AGENT.md 和 ARCHITECTURE.md 补充 `ui/debug/` 模块记录。

## In-scope files
1. `AGENT.md` —「项目结构」树形图补充 `ui/debug/` 节点
2. `docs/ARCHITECTURE.md` — §2 实际包路径列表补充 `ui/debug/` 条目

## Steps
1. `AGENT.md`：在「项目结构」树形图的 `ui/theme/` 行后追加 `ui/debug/    # 调试 overlay（仅 debug 构建）：DebugOverlay（悬浮调试按钮）+ DebugLog（异常 ring buffer + 崩溃持久化）`
2. `docs/ARCHITECTURE.md`：在 §2 实际包路径列表的 `- ui/theme/...` 行后追加 `- ui/debug/：DebugOverlay（@Composable，仅 DEBUG 包裹 app 内容叠加悬浮按钮）+ DebugLog（object，内存 ring buffer + 未捕获异常持久化，release 空操作）。`
3. 运行 gate 验证文档修改不破坏构建（文档修改不影响编译，但跑 compileDebugKotlin 确认无意外）

## Acceptance criteria
- gate COMPILE/ASSEMBLE 全绿
- AGENT.md「项目结构」含 `ui/debug/` 条目
- ARCHITECTURE.md §2 含 `ui/debug/` 条目

## 零行为变化说明
仅文档修改，零代码变化，零行为变化。
