# Plan: NotesScreen 缺失 RainbowTrim 品牌条

> Cycle 3 | Category: ui | Finding ID: F003

## What & why
NotesScreen.kt 是唯一缺失 `RainbowTrim()` 品牌条的全屏页面。其他 7 个全屏（Timeline、Heatmap、HeatmapYear、TimeViz、Search、Settings、About）均在顶部调用 `RainbowTrim()`，AGENT.md 也明确该组件为"全 app 唯一品牌标识条，各屏幕顶部统一调用"。此外 NotesScreen 的根 Box 未设 `.background(Background)`，与其他屏不一致。

## Finding registry entry
- **ID**: F003
- **Impact**: M
- **Effort**: S
- **Evidence**: NotesScreen.kt:46-47（Box 无 background、Column 直接 padding 无 RainbowTrim）；对比 SearchScreen.kt:76-79（Box 有 background、RainbowTrim 在 Column 顶部）
- **Confidence**: HIGH

## In-scope files (max 5)
- `app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` — 加 RainbowTrim + Background + 结构调整

## Out-of-scope (do NOT touch)
- 其他屏幕文件
- PixelComponents.kt（RainbowTrim 定义处）

## Steps (max 8)
### Step 1: 添加 import
在 NotesScreen.kt import 区添加 `import com.shijiben.ui.theme.Background` 和 `import com.shijiben.ui.theme.RainbowTrim`
**Verify**: `grep -n "RainbowTrim\|Background" NotesScreen.kt` → 有 import 行

### Step 2: 根 Box 加 background
将 `Box(modifier = Modifier.fillMaxSize())` 改为 `Box(modifier = Modifier.fillMaxSize().background(Background))`
**Verify**: `grep -n "background(Background)" NotesScreen.kt` → 1 match

### Step 3: 重构 Column 结构——RainbowTrim 在前，内容 padded Column 在后
将：
```
Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
    PixelCard(...) { ... }
    ...
}
```
改为：
```
Column(modifier = Modifier.fillMaxSize()) {
    RainbowTrim()
    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        PixelCard(...) { ... }
        ...
    }
}
```
即：外层 Column 去掉 padding、加 RainbowTrim()，内容包进新的内层 Column（保留原 12.dp padding）。
**Verify**: `grep -n "RainbowTrim()" NotesScreen.kt` → 1 match（调用处）

### Step 4: 更新 AGENT.md
在 `## Recent changes (better cycles)` 段顶部追加：
`- ui: NotesScreen 补齐 RainbowTrim 品牌条 + Background，与其他 7 屏一致`
**Verify**: `grep -c "ui:" AGENT.md` → 3

## Acceptance criteria (machine-checkable)
ALL must hold after implementation:
- [ ] `grep -n "RainbowTrim()" NotesScreen.kt` → 1 match（调用）
- [ ] `grep -n "background(Background)" NotesScreen.kt` → 1 match
- [ ] `./gradlew :app:compileDebugKotlin` → exit 0
- [ ] `./gradlew assembleDebug` → exit 0
- [ ] `git status --short` shows ONLY NotesScreen.kt + AGENT.md

## Risk
- **Risk of fix**: LOW — 仅添加品牌条和背景色，不改变交互逻辑；Column 嵌套层级 +1
- **Reversibility**: git checkout is sufficient

## Escape hatches (STOP conditions)
Stop and report back if:
- 嵌套 Column 导致编译错误
- NotesScreen 的 LazyColumn 在嵌套后出现 vertical scroll 冲突
