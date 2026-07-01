# Plan — UI Cycle 18

## Finding
- ID: F018
- Title: NoteEditorSheet 标题残留 `MaterialTheme.typography.titleLarge` 间接引用 + `PixelText` 旧别名，为全 app 唯一 `MaterialTheme.typography.*` 调用点，与 6 屏标准（显式 `20.sp Bold + TextPrimary`）漂移
- Impact: M
- Effort: S
- Evidence:
  - `NoteEditorSheet.kt:64` — `style = MaterialTheme.typography.titleLarge,`（间接引用）
  - `NoteEditorSheet.kt:65` — `color = PixelText`（legacy 别名，AppColors.kt:92 `PixelText = TextPrimary`）
  - `NoteEditorSheet.kt:13` — `import androidx.compose.material3.MaterialTheme`（全 app feature 代码中唯二的 MaterialTheme import，另一处是 MainActivity.kt:9 用 colorScheme，非 typography）
  - `AppTheme.kt:50-54` — `titleLarge = TextStyle(fontFamily = PixelFont, fontWeight = FontWeight.Bold, fontSize = 20.sp)`（间接引用目标，当前恰好 = 20sp Bold）
  - 对比 6 屏标题（一致用显式 `fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary`）：
    - `NotesScreen.kt:67` — `Text("随笔", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)`（同包 sibling，cycle 4/5 已修）
    - `HeatmapScreen.kt:60-65` — "回看"
    - `SettingsScreen.kt:151-156` — "设置"
    - `AboutScreen.kt:66-71`、`TimeVizScreen.kt:91-96`、`SearchScreen.kt:92-97` — 同款

## Root cause
Cycle 4 修复了 `NotesScreen` 列表标题的 `titleLarge`（当时 22sp）漂移，把列表标题改为显式 `16.sp`（随后整个排版系统简化为 12/16/20 三档，6 屏标题统一为显式 `20.sp`）。但 **同包的 `NoteEditorSheet`（从 NotesScreen 调起的编辑弹层）的标题**漏改，仍用 `style = MaterialTheme.typography.titleLarge`。当前 `titleLarge` 恰好 = 20sp Bold，故视觉上"碰巧正确"；但这是潜伏的漂移风险——`titleLarge` 一旦再调（如改字重或字号），NoteEditorSheet 会静默偏离 6 屏，正是 cycle 4 修过的同一类 bug。同时 `color = PixelText` 是 legacy 别名，sibling NotesScreen 及其余 5 屏均已迁到 canonical `TextPrimary`。

## Why visual consistency
- 排版层级：NoteEditorSheet 是从 NotesScreen 调起的 modal，其标题应与父屏标题同一档（20sp Bold），目前靠 `titleLarge` 间接命中，非显式对齐。
- 令牌一致：`PixelText` 是向后兼容别名（AppColors.kt:92），同包 NotesScreen + 5 屏均用 canonical `TextPrimary`，NoteEditorSheet 是该区域唯一残留旧别名的标题。
- 间接引用是全 app 唯一异常点：除 AppTheme.kt 定义处与 MainActivity.kt 用 colorScheme（非 typography）外，NoteEditorSheet 是唯一 feature 代码引用 `MaterialTheme.typography.*` 的地方——6 屏均用显式 fontSize/fontWeight。

## Plan
**Files in scope (1)**:
- `app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt`

**Steps**:
1. 编辑 NoteEditorSheet.kt:62-66 的标题 `Text`，把 `style = MaterialTheme.typography.titleLarge,` 改为 `fontWeight = FontWeight.Bold,` + `fontSize = 20.sp,`，并把 `color = PixelText` 改为 `color = TextPrimary`。结果：
   ```kotlin
   Text(
       text = if (editing != null) "编辑随笔" else "记一笔随笔",
       fontWeight = FontWeight.Bold,
       fontSize = 20.sp,
       color = TextPrimary
   )
   ```
2. 新增 import `androidx.compose.ui.text.font.FontWeight`（按字母序，置于 line 26 `FontFamily` 之后）。
3. 新增 import `com.shijiben.ui.theme.TextPrimary`（按字母序，置于 `PixelTextSecondary` 之前/`PixelText` 相关位置；保留现有 theme imports）。
4. 删除 import `androidx.compose.material3.MaterialTheme`（line 13）——改后该文件无其他 MaterialTheme 引用（ModalBottomSheet/OutlinedTextField/AlertDialog/TextButton/Text 均有各自独立 import，不依赖 MaterialTheme）。
5. 删除 import `com.shijiben.ui.theme.PixelText`（line 34）——改后 `PixelText` 在本文件无其他引用（line 65 是唯一使用点）。`PixelSurface`（line 55 containerColor）与 `PixelTextSecondary`（line 71 timestamp 色）保留，不在本次范围。
6. 更新 AGENT.md `## Recent changes (better cycles)` 段顶部追加：
   `- ui: NoteEditorSheet 标题改用显式 20sp Bold + TextPrimary，移除全 app 唯一的 MaterialTheme.typography.* 间接引用 + PixelText 旧别名（cycle 4 同类漂移的残留实例）`

## Acceptance criteria
- `grep -n "MaterialTheme.typography" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` → 0 matches
- `grep -n "MaterialTheme" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` → 0 matches（含 import）
- `grep -n "fontWeight = FontWeight.Bold, *$\|fontWeight = FontWeight.Bold,$" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` → ≥1 match（标题）
- `grep -n "fontSize = 20.sp" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` → ≥1 match（标题）
- `grep -n "color = TextPrimary" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` → ≥1 match（标题）
- `grep -n "PixelText" app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` → 0 matches（含 import，已迁 TextPrimary）
- `grep -rn "MaterialTheme.typography" app/src/main/java/com/shijiben/feature/` → 0 matches（feature 代码零间接引用）
- Gate: `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease` 全绿（baseline 已知 flaky 例外见 better/baseline.md）

## Risk
- **Risk of fix**: LOW — 纯排版/颜色令牌对齐，视觉零变化（titleLarge 当前 = 20sp Bold = 目标值；PixelText = TextPrimary）。不触碰交互逻辑、不改变 ModalBottomSheet 结构。
- **Reversibility**: git checkout is sufficient.

## Escape hatches
- 若移除 MaterialTheme import 后编译报错（说明有遗漏的 MaterialTheme 引用），停止并报告——不强行删除 import。
- 若发现 NoteEditorSheet 还有其他 `MaterialTheme.typography.*` 使用点（预期无），范围扩大则停止报告。
