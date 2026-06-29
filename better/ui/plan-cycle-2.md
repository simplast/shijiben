# Plan: disabled 颜色硬编码绕过令牌系统

> Cycle 2 | Category: ui | Finding ID: F002

## What & why
`Disabled` 令牌（`Color(0xFFCBD5E1)`）已在 AppColors.kt:43 定义，但 PixelComponents.kt、HeatmapScreen.kt、HeatmapYearScreen.kt 中均使用硬编码 `Color(0xFFCBD5E1)` 而非令牌。此外 disabled 文字/图标色 `Color(0xFF94A3B8)` 在 5 处使用但完全没有对应令牌。这导致 disabled 颜色无法集中管理——若调整 `Disabled` 令牌，硬编码处不会跟随。

## Finding registry entry
- **ID**: F002
- **Impact**: M
- **Effort**: S
- **Evidence**: AppColors.kt:43（Disabled 令牌存在未用）；PixelComponents.kt:53,87,95；HeatmapScreen.kt:161,169,204,211；HeatmapYearScreen.kt:174,182,201,208
- **Confidence**: HIGH

## In-scope files (max 5)
- `app/src/main/java/com/shijiben/ui/theme/AppColors.kt` — 新增 `DisabledText` 令牌
- `app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt` — 替换 3 处硬编码为令牌（同包无需 import）
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` — 加 import + 替换 4 处
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt` — 加 import + 替换 4 处

## Out-of-scope (do NOT touch)
- DebugOverlay.kt（调试覆盖层，17 处硬编码，独立处理）
- TimeRangeSlider.kt（1 处硬编码，与 disabled 无关）

## Steps (max 8)
### Step 1: AppColors.kt 新增 DisabledText 令牌
在 `val Disabled = ...`（line 43）下方添加 `val DisabledText = Color(0xFF94A3B8)  // slate-400，disabled 文字/图标色`
**Verify**: `grep -n "DisabledText" AppColors.kt` → 1 match

### Step 2: PixelComponents.kt 替换硬编码
将 `Color(0xFFCBD5E1)` 全部替换为 `Disabled`（2 处：line 53,87），将 `Color(0xFF94A3B8)` 替换为 `DisabledText`（1 处：line 95）。同包无需加 import。
**Verify**: `grep -n "Color(0xFFCBD5E1)\|Color(0xFF94A3B8)" PixelComponents.kt` → 无匹配

### Step 3: HeatmapScreen.kt 加 import + 替换
添加 `import com.shijiben.ui.theme.Disabled` 和 `import com.shijiben.ui.theme.DisabledText`。将 `Color(0xFFCBD5E1)` → `Disabled`（2 处），`Color(0xFF94A3B8)` → `DisabledText`（2 处）。
**Verify**: `grep -n "Color(0xFFCBD5E1)\|Color(0xFF94A3B8)" HeatmapScreen.kt` → 无匹配

### Step 4: HeatmapYearScreen.kt 加 import + 替换
同 Step 3。
**Verify**: `grep -n "Color(0xFFCBD5E1)\|Color(0xFF94A3B8)" HeatmapYearScreen.kt` → 无匹配

### Step 5: 更新 AGENT.md
在 `## Recent changes (better cycles)` 段顶部追加：
`- ui: disabled 颜色硬编码改用 Disabled/DisabledText 令牌，统一 4 文件 11 处`
**Verify**: `grep -n "DisabledText" AGENT.md` → 无需匹配；`grep -c "ui:" AGENT.md` → 2

## Acceptance criteria (machine-checkable)
ALL must hold after implementation:
- [ ] `grep -rn "Color(0xFFCBD5E1)" app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt app/src/main/java/com/shijiben/feature/heatmap/` → 无匹配
- [ ] `grep -rn "Color(0xFF94A3B8)" app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt app/src/main/java/com/shijiben/feature/heatmap/` → 无匹配
- [ ] `grep -n "DisabledText" app/src/main/java/com/shijiben/ui/theme/AppColors.kt` → 1 match
- [ ] `./gradlew :app:compileDebugKotlin` → exit 0
- [ ] `./gradlew assembleDebug` → exit 0
- [ ] `git status --short` shows ONLY 4 in-scope files + AGENT.md

## Risk
- **Risk of fix**: LOW — 纯颜色值替换，视觉不变（同值）
- **Reversibility**: git checkout is sufficient

## Escape hatches (STOP conditions)
Stop and report back if:
- 替换后编译失败（可能是 import 缺失或拼写错误）
- 发现 `Color(0xFFCBD5E1)` 出现在非 disabled 语境（不应发生）
