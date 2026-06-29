# Plan: TimeVizScreen 的 PixelOutlinedButtonLocal 与共享组件漂移

> Cycle 1 | Category: ui | Finding ID: F001

## What & why
TimeVizScreen.kt 内有一个本地副本 `PixelOutlinedButtonLocal`（line 267），注释声称"与 PixelOutlinedButton 同风格，本地实现避免改 PixelComponents.kt"，但实际已漂移：字号 14.sp vs 共享组件 16.sp（PixelComponents.kt:98），垂直 padding 8.dp vs 10.dp（PixelComponents.kt:90）。这导致 TimeViz 的"设置生日"/"修改生日"按钮比其他屏幕（NoteEditorSheet、RecordingSheet）的同款按钮更小，破坏视觉一致性。应删除本地副本，改用共享 `PixelOutlinedButton`。

## Finding registry entry
- **ID**: F001
- **Impact**: M
- **Effort**: S
- **Evidence**: TimeRangeSlider.kt 不相关；TimeVizScreen.kt:267,285（local 14sp）、PixelComponents.kt:76,98（shared 16sp）
- **Confidence**: HIGH

## In-scope files (max 5)
- `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt` — 删除 PixelOutlinedButtonLocal 函数 + 改两处调用为共享 PixelOutlinedButton + 添加 import

## Out-of-scope (do NOT touch)
- `app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt` — 共享组件无需改动
- 其他 feature 屏幕文件

## Steps (max 8)
### Step 1: 添加 PixelOutlinedButton 的 import
在 TimeVizScreen.kt 的 theme import 区（line 45-52 附近）添加：
`import com.shijiben.ui.theme.PixelOutlinedButton`
**Verify**: `grep -n "PixelOutlinedButton" TimeVizScreen.kt` → 至少 1 行 import

### Step 2: 替换第一处调用（"设置生日"按钮）
将 line 135-138 的 `PixelOutlinedButtonLocal(text = "设置生日", onClick = { showDatePicker = true })` 改为 `PixelOutlinedButton(text = "设置生日", onClick = { showDatePicker = true })`
**Verify**: `grep -n "PixelOutlinedButtonLocal" TimeVizScreen.kt` → 仅剩函数定义处，调用处已无

### Step 3: 替换第二处调用（"修改生日"按钮）
将 line 164-167 的 `PixelOutlinedButtonLocal(text = "修改生日", onClick = { showDatePicker = true })` 改为 `PixelOutlinedButton(text = "修改生日", onClick = { showDatePicker = true })`
**Verify**: `grep -n "PixelOutlinedButtonLocal" TimeVizScreen.kt` → 仅剩函数定义

### Step 4: 删除 PixelOutlinedButtonLocal 函数
删除 line 265-289 的整个函数（含上方注释）。
**Verify**: `grep -n "PixelOutlinedButtonLocal" TimeVizScreen.kt` → 无匹配

### Step 5: 更新 AGENT.md
在 `## Recent changes (better cycles)` 段追加一行：
`- ui: 移除 TimeVizScreen 的 PixelOutlinedButtonLocal 本地副本，改用共享 PixelOutlinedButton 统一按钮风格`
**Verify**: `grep -n "PixelOutlinedButtonLocal" AGENT.md` → 无需匹配；`grep -n "ui:" AGENT.md` → 有新增行

## Acceptance criteria (machine-checkable)
ALL must hold after implementation:
- [ ] `grep -n "PixelOutlinedButtonLocal" TimeVizScreen.kt` → 无匹配（本地副本已删除）
- [ ] `grep -n "PixelOutlinedButton" TimeVizScreen.kt` → 有 import + 至少 2 处调用
- [ ] `./gradlew :app:compileDebugKotlin` → exit 0
- [ ] `./gradlew assembleDebug` → exit 0
- [ ] `git status --short` shows ONLY TimeVizScreen.kt + AGENT.md modified

## Risk
- **Risk of fix**: LOW — 替换为同风格共享组件，按钮会略大（16sp/10dp vs 14sp/8dp），但不影响布局正确性
- **Reversibility**: git checkout is sufficient

## Escape hatches (STOP conditions)
Stop and report back if:
- PixelOutlinedButton 的签名不支持 (text, onClick) 两参调用（需检查参数默认值）
- 替换后编译失败且无法通过简单调整解决
