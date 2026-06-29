# Plan — UI Cycle 5

## Finding
- ID: F005
- Title: NotesScreen 标题栏结构与 5 屏标准结构漂移（PixelCard 包裹 + 无 2dp 黑色分隔线 + 12dp 外 padding）
- Impact: M
- Effort: M
- Evidence:
  - NotesScreen.kt:52-67 — 标题栏包裹在 `PixelCard(shadow=false)` 内，外层 `Column(padding=12.dp)`
  - PixelComponents.kt:25-42 — PixelCard 默认 1dp Border 灰边（borderColor=Border=#E5E7EB），shadow=false 时无阴影但有边
  - 对比 5 屏标准结构（一致用 `Row(background=SurfaceColor) { IconButton, Text } → Box(height=2dp, background=Color.Black)`）：
    - SearchScreen.kt:79-100 (Row + 2dp divider)
    - AboutScreen.kt:53-74 (Row + 2dp divider)
    - TimeVizScreen.kt:78-99 (Row + 2dp divider)
    - SettingsScreen.kt (Row + 2dp divider)
    - HeatmapScreen.kt (Row + 2dp divider)
  - NotesScreen 缺失：标题栏无 SurfaceColor 背景（PixelCard 自带 Surface 但有 1dp 灰边）、无 2dp 黑色分隔线、外层 12dp padding 导致标题栏不贴边

## Root cause
NotesScreen 是历史遗留结构，标题栏包裹在 PixelCard 内以强调"工具栏"卡片区；其他 5 屏重构后已统一为扁平 Row + 2dp 黑色分隔线的标准结构。NotesScreen 漏改，导致视觉上 NotesScreen 标题栏呈"灰边盒子"，而其他 5 屏呈"扁平工具栏 + 黑色下划线"。

## Plan
**Files in scope (1)**:
- `app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt`

**Steps**:
1. 重构 NotesScreen 标题栏结构，对齐其他 5 屏标准：
   - 删除 `PixelCard(shadow=false) { Row(padding=8.dp, SpaceBetween) { ... } }` 包裹
   - 直接在 `Column(fillMaxSize)` 内（RainbowTrim 之后）放 `Row(fillMaxWidth, background=SurfaceColor, verticalAlignment=CenterVertically)`，包含：
     - `IconButton { Icon(back, contentDescription="返回", tint=TextPrimary) }`（新增 tint）
     - `Text("随笔", fontWeight=Bold, fontSize=16.sp, color=PixelText)`（沿用 Cycle 4 结果）
     - `Spacer(Modifier.weight(1f))`（把 Add 推到右侧）
     - `IconButton { Icon(Add, contentDescription="新增") }`
   - 在 Row 之后加 `Box(Modifier.fillMaxWidth().height(2.dp).background(Color.Black))`（2dp 黑色分隔线，对齐其他 5 屏）
2. 把原 `Column(padding=12.dp)` 内的剩余内容（Spacer + if/else empty/LazyColumn）抽出，放到独立的 `Column(padding=12.dp)` 中（保持原 12dp 内容 padding，但标题栏不再被这个 padding 包裹）。
3. 新增 import `androidx.compose.ui.graphics.Color`（用于 Color.Black 分隔线）。
4. 新增 import `androidx.compose.ui.text.font.FontWeight` — 已在 Cycle 4 加过，无需再加。
5. 新增 import `com.shijiben.ui.theme.Surface as SurfaceColor` — 需确认是否已有。当前文件没有该 import，需要加。
6. PixelCard import 保留（NoteRow 仍用）。
7. Arrangement import 保留（LazyColumn 用 spacedBy）。

## Acceptance criteria
- `grep -n "PixelCard" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → 仅 NoteRow 内 1 处使用（line ~104），不在标题栏内
- `grep -n "background(SurfaceColor)" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → ≥1 match（标题 Row）
- `grep -n "height(2.dp).background(Color.Black)" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → ≥1 match（分隔线 Box）
- `grep -n "tint = TextPrimary" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → ≥1 match（back 图标）
- `grep -n "Spacer(Modifier.weight(1f))" app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` → ≥1 match（标题与 Add 之间）
- Gate: `./gradlew :app:compileDebugKotlin && ./gradlew assembleDebug && ./gradlew :app:assembleRelease` 全绿（testDebugUnitTest 仅 baseline 已知 flaky 例外）。
