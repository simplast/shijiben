# Plan — UI Cycle 4

## Finding
- ID: F004
- Title: NotesScreen 标题字号（22sp）与其他 5 屏（16sp）不一致
- Impact: M
- Effort: S
- Evidence:
  - NotesScreen.kt:62 — `Text("随笔", style = MaterialTheme.typography.titleLarge, color = PixelText)`
  - AppTheme.kt:50-54 — `titleLarge = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 22.sp)`
  - 对比 5 屏标题（一致用 `fontWeight = FontWeight.Bold, fontSize = 16.sp`）：
    - HeatmapScreen.kt:76-81 "回看"
    - SettingsScreen.kt:151-156 "设置"
    - AboutScreen.kt:66-71 "关于事记本"
    - TimeVizScreen.kt:91-96 "时间可视化"
    - SearchScreen.kt:92-97 "搜索"

## Root cause
NotesScreen 是历史遗留代码，使用 `MaterialTheme.typography.titleLarge`（22sp）做标题；其他 5 屏重构后已统一为显式 `Bold + 16sp`，NotesScreen 漏改。导致 NotesScreen 标题视觉上明显比其他屏大 6sp。

## Plan
**Files in scope (1)**:
- `app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt`

**Steps**:
1. 编辑 NotesScreen.kt:62 — 把 `Text("随笔", style = MaterialTheme.typography.titleLarge, color = PixelText)` 改为 `Text("随笔", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PixelText)`。
   - 保留 `color = PixelText`（PixelText 是 TextPrimary 的别名，AppColors.kt:92），最小化改动范围。
   - 不迁移 PixelText → TextPrimary（避免扩大到 4 行：62, 73, 109, 118）。
2. 新增 import `androidx.compose.ui.text.font.FontWeight`（按字母序，放在 FontFamily 之后、TextOverflow 之前，即 line 27 之后）。
3. 删除 import `androidx.compose.material3.MaterialTheme`（line 21）— 该 import 仅 line 62 一处使用，改后无引用。

## Acceptance criteria
- `grep -n "MaterialTheme.typography.titleLarge" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → 0 matches
- `grep -n "fontWeight = FontWeight.Bold, fontSize = 16.sp" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → ≥1 match (line ~62)
- `grep -n "import androidx.compose.ui.text.font.FontWeight" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → 1 match
- `grep -n "import androidx.compose.material3.MaterialTheme" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → 0 matches
- Gate: `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease` 全绿（除 baseline 已知 flaky 例外）。
