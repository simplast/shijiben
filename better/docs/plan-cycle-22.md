# Plan: docs cycle 22 — F022

## Finding
- ID: F022
- 标题：AGENT.md 项目结构树 `feature/heatmap/` 行 + Spec 索引未同步 cycle 13 的「HeatmapScreen 重构为 3 tab 容器 + TimeAllocation」变更——结构树列出已不存在的 `HeatmapYearScreen.kt`，漏列 5 个实际文件；Spec 索引漏列 `2026-06-29-time-allocation-design.md` 和 `2026-06-29-debug-console-overlay-design.md` 两个 2026-06-29 spec
- evidence:
  - `AGENTS.md:38` — 结构树写 `feature/heatmap/   # 热力图回看（月视图 + 年视图）（HeatmapScreen, HeatmapViewModel, HeatmapYearScreen, HeatmapYearViewModel, HeatmapCalculator）`
  - 实际 `app/src/main/java/com/shijiben/feature/heatmap/` 含 9 个文件：`HeatmapCalculator.kt` / `HeatmapMonthTab.kt` / `HeatmapScreen.kt` / `HeatmapViewModel.kt` / `HeatmapYearTab.kt` / `HeatmapYearViewModel.kt` / `TimeAllocationCalculator.kt` / `TimeAllocationTab.kt` / `TimeAllocationViewModel.kt`
  - `feature/heatmap/HeatmapScreen.kt:42` — `var selectedTab by remember { mutableIntStateOf(0) } // 0=月, 1=年, 2=去向`（实际是 3 tab，非「月视图 + 年视图」）
  - `docs/superpowers/specs/2026-06-29-time-allocation-design.md` 存在但 `AGENTS.md` Spec 索引（行 191-205）无该条目
  - `docs/superpowers/specs/2026-06-29-debug-console-overlay-design.md` 存在但 `AGENTS.md` Spec 索引同样无该条目
- impact：M（AI agent 读 AGENTS.md 时被「`HeatmapYearScreen`」误导去查找不存在的文件、不知有 `TimeAllocation*` 系列、且无法从 Spec 索引找到 TimeAllocation 与 debug overlay 两个最近 feature 的设计文档；AGENT.md 自身「Recent changes」段已写明此重构但结构树/索引未同步，自相矛盾）
- effort：S

## 现状

### 结构树漂移（AGENTS.md:38）
当前行：
```
heatmap/   # 热力图回看（月视图 + 年视图）（HeatmapScreen, HeatmapViewModel, HeatmapYearScreen, HeatmapYearViewModel, HeatmapCalculator）
```

实际代码（`app/src/main/java/com/shijiben/feature/heatmap/`）：
- `HeatmapScreen.kt` — 3 tab 容器（月/年/去向），`selectedTab` 0/1/2
- `HeatmapMonthTab.kt` — 月 tab 主体
- `HeatmapYearTab.kt` — 年 tab 主体（**注意：原 `HeatmapYearScreen.kt` 已被重命名为 Tab，独立路由已废弃**）
- `HeatmapViewModel.kt` — 月 tab VM
- `HeatmapYearViewModel.kt` — 年 tab VM
- `HeatmapCalculator.kt` — object，月历 6×7 网格 + 色阶
- `TimeAllocationCalculator.kt` — object，按标题聚合 completed 事件时长
- `TimeAllocationViewModel.kt` — 去向 tab VM
- `TimeAllocationTab.kt` — 去向 tab 主体（范围选择器 + 水平条形图列表）

漂移点：
1. `HeatmapYearScreen` 不存在（已重命名为 `HeatmapYearTab`，因 HeatmapScreen 从独立路由的 Screen 改为 tab 容器）
2. 漏列 5 个文件：`HeatmapMonthTab` / `HeatmapYearTab` / `TimeAllocationCalculator` / `TimeAllocationTab` / `TimeAllocationViewModel`
3. 描述「月视图 + 年视图」漏第 3 tab「去向」
4. AGENT.md「Recent changes」段第 209 行已写明「HeatmapScreen 重构为 3 tab 容器（月/年/去向）...废弃 HeatmapYearScreen 独立路由」——结构树与 Recent changes 自相矛盾

对照 `docs/ARCHITECTURE.md:62` 已正确列出全部 9 个文件并标注「3 tab 容器」——只有 AGENT.md 漂移。

### Spec 索引漂移（AGENTS.md:191-205）
Spec 索引共 13 条，全部为 2026-06-27 / 2026-06-28 spec。实际 `docs/superpowers/specs/` 目录有 2 个 2026-06-29 spec 未入索引：
- `2026-06-29-time-allocation-design.md` — 时间去向聚合设计（cycle 13 落地的 major feature）
- `2026-06-29-debug-console-overlay-design.md` — Debug Console Overlay 设计（cycle 12 F012 落地的 ui/debug/ 模块对应 spec）

## 修复方案

### 1. AGENTS.md:38 — 修正 heatmap 结构树
旧：
```
heatmap/   # 热力图回看（月视图 + 年视图）（HeatmapScreen, HeatmapViewModel, HeatmapYearScreen, HeatmapYearViewModel, HeatmapCalculator）
```
新：
```
heatmap/   # 热力图回看（3 tab 容器：月/年/去向）（HeatmapScreen, HeatmapMonthTab, HeatmapYearTab, TimeAllocationTab, HeatmapViewModel, HeatmapYearViewModel, TimeAllocationViewModel, HeatmapCalculator, TimeAllocationCalculator）
```

### 2. AGENTS.md:205 后 — 追加 2 条 2026-06-29 spec
在 `2026-06-28-search-design.md` 行后追加：
```
- [2026-06-29-debug-console-overlay-design.md](docs/superpowers/specs/2026-06-29-debug-console-overlay-design.md) — Debug Console Overlay（app 内悬浮调试面板，对应 ui/debug/ 模块）
- [2026-06-29-time-allocation-design.md](docs/superpowers/specs/2026-06-29-time-allocation-design.md) — 时间去向聚合（HeatmapScreen 重构为 3 tab 容器 + TimeAllocationCalculator/ViewModel/Tab）
```

## In-scope files
1. `AGENTS.md` — 修正 heatmap 结构树 + Spec 索引追加 2 条

## Steps
1. 编辑 `AGENTS.md:38`：替换 heatmap 行的文件列表（`HeatmapYearScreen` → `HeatmapYearTab` + 补 `HeatmapMonthTab` / `TimeAllocationTab` / `TimeAllocationViewModel` / `TimeAllocationCalculator`）+ 描述从「月视图 + 年视图」改为「3 tab 容器：月/年/去向」
2. 编辑 `AGENTS.md:205` 后：追加 `2026-06-29-debug-console-overlay-design.md` 与 `2026-06-29-time-allocation-design.md` 两条 spec 索引
3. 运行 `./gradlew :app:compileDebugKotlin` 确认文档修改不影响编译（文档修改不影响代码，但跑一遍验证不破坏）

## Acceptance criteria
- AGENTS.md 结构树 `feature/heatmap/` 行不再出现 `HeatmapYearScreen`（已不存在的文件名）
- AGENTS.md 结构树列出全部 9 个实际文件或其主要子集（至少含 `HeatmapMonthTab` / `HeatmapYearTab` / `TimeAllocationTab` / `TimeAllocationCalculator` / `TimeAllocationViewModel` 这 5 个此前漏列的）
- AGENTS.md 结构树描述含「3 tab」或「去向」字样（不再仅「月视图 + 年视图」）
- AGENTS.md Spec 索引含 `2026-06-29-time-allocation-design.md` 和 `2026-06-29-debug-console-overlay-design.md` 两条
- gate compileDebugKotlin 绿

## 零行为变化说明
仅文档修改，零代码变化，零行为变化。修正 AGENT.md 与代码/Spec 目录的真实漂移。

## 文件引用
- `AGENTS.md:38`（结构树 heatmap 行，漂移源）
- `AGENTS.md:191-205`（Spec 索引段，缺 2 条）
- `AGENTS.md:209`（Recent changes 段已写明重构，与结构树矛盾，证明这是漏更新而非未发生）
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt:42`（实际 3 tab 容器证据）
- `app/src/main/java/com/shijiben/feature/heatmap/`（实际 9 个 .kt 文件证据）
- `docs/superpowers/specs/2026-06-29-time-allocation-design.md`（存在但索引漏）
- `docs/superpowers/specs/2026-06-29-debug-console-overlay-design.md`（存在但索引漏）
- `docs/ARCHITECTURE.md:62`（已正确更新，对照证明 AGENT.md 漂移）
