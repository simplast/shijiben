## Context

当前首页使用 Material 3 默认组件风格（圆角、阴影、渐变），与 DESIGN.md 的暖金极简风格对齐。用户希望全面转向 **8-bit 像素扁平多彩** 风格——复古游戏 UI 的趣味感 + 现代扁平布局的清晰度 + 多彩配色的高辨识度。

功能层完全稳定（事件 CRUD、时间追踪、推荐、去重、时间视角），本次改造**仅涉及视觉/布局层**。

约束条件：
- 保持 DESIGN.md 暖调色板作为基础，在此之上扩展多彩色系
- 兼容 minSdk 26
- 不引入新的第三方依赖
- 不改数据库 Schema

## Goals / Non-Goals

**Goals:**
- 全面切换设计语言：圆角 → 直角、阴影 → 边框、渐变 → 纯色、单色 → 多彩
- 每个功能模块拥有独立色彩身份，提升辨识度和信息层级清晰度
- 引入像素风排版（monospace 字体），营造 8-bit 复古游戏 UI 感
- 保持非对称概览仪表盘、Section 装饰短线、已完成降权等布局改进
- 所有改动可通过 `./gradlew assembleDebug` 验证

**Non-Goals:**
- 不改变任何功能行为
- 不修改 ViewModel / EventRepository / Room 数据层
- 不改造 EventEditorScreen / WeeklyReviewScreen / MonthlyReviewScreen
- 不新增导航路由或页面
- 不引入第三方依赖
- 不修改数据库 Schema

## Decisions

### Decision 1: 形状系统 —— 全局直角

**Choice:** 全局替换所有 `RoundedCornerShape` 为 `RoundedCornerShape(0.dp)`。

具体做法：定义一个全局常量并在所有组件中使用：
```kotlin
val PixelShape = RoundedCornerShape(0.dp)
```

涉及替换的组件：Card、Button、IconButton、Badge、Input、DropdownMenu、ModalBottomSheet。

**Rationale:** 8-bit 像素风的核心特征就是方块感。直角矩形传递"像素块"的视觉语言，与圆角的"亲和温暖"形成截然不同的个性。

### Decision 2: 层级系统 —— 边框替代阴影

**Choice:** 移除所有 `elevation` / `shadow`，改用 2-3dp 实色边框区分层级。

```kotlin
// 边框色 Token
val PixelBorder = Color(0xFF1A1A2E)      // dark navy — 主边框
val PixelBorderLight = Color(0x331A1A2E)  // 20% alpha — 轻量分割

// 用法
val pixelBorder = BorderStroke(2.dp, PixelBorder)
val pixelBorderLight = BorderStroke(2.dp, PixelBorderLight)
```

层级规则：
- **高突出卡片**（概览、进行中）：3dp PixelBorder
- **内容卡片**（已完成、待办）：2dp PixelBorderLight 或彩色边框
- **浮动元素**（底部输入栏、DropdownMenu）：2dp 彩色边框（薰衣草紫 / 琥珀橙）
- **内嵌元素**（标签、按钮）：1-2dp 轻量边框

**Rationale:** 像素游戏 UI 不使用阴影——层级靠边框粗细和颜色区分。这与扁平化设计原则一致，同时保持 8-bit 的"描边方块"感。

### Decision 3: 多彩色板 —— 9 色扩展

**Choice:** 在 Color.kt 中扩展 9 个新色彩 Token，形成多彩配色体系：

```kotlin
// ── 8-bit Colorful Palette ──
val PixelSkyBlue = Color(0xFF6BC5F5)     // 概览卡背景
val PixelHotPink = Color(0xFFFF6B9D)     // 进行中卡背景
val PixelCoralRed = Color(0xFFFF8A6B)    // 已完成徽章 / 标签
val PixelLavender = Color(0xFFC4B5E0)    // 待办徽章 / 输入栏边框
val PixelTeal = Color(0xFF4DC9B8)        // 已完成色条 / 标签
val PixelAmberOrange = Color(0xFFFFB347) // 待办边框 / 提交按钮
val PixelMintLight = Color(0xFFE8F8F0)   // 已完成区背景
val PixelStarYellow = Color(0xFFFFD93D)  // 像素星星 / 装饰
val PixelDeepNavy = Color(0xFF1A1A2E)    // 主边框色
```

每个模块的色彩分配：

| 模块 | 背景色 | 边框色 | 文字/装饰色 |
|------|--------|--------|------------|
| 概览卡 | PixelSkyBlue | PixelDeepNavy 3dp | 白色数字 + 珊瑚红/薰衣草紫徽章 |
| 进行中卡 | PixelHotPink | PixelDeepNavy 3dp | 白色文字 + PixelStarYellow 星星 + PixelMint 按钮 |
| 已完成区 | PixelMintLight | PixelTeal 2dp | 多彩色条（Teal/SkyBlue/Lavender 轮换） |
| 待办区 | White | PixelAmberOrange 2dp + 左侧 5dp Gold 边 | 多彩播放按钮（Coral/Lavender/Teal 轮换） |
| 推荐标签 | 各自色彩底 | 同色系 1dp | 白色文字 |
| 底部输入栏 | White | PixelLavender 2dp | 粉红"+" + 琥珀按钮 |

**Rationale:** 多彩配色让每个功能模块有独立的"色彩身份"。用户快速扫视时，蓝色=概览、粉色=进行中、绿色=已完成、橙色=待办，形成条件反射式的信息识别。

### Decision 4: 排版像素风 —— Monospace 全局化

**Choice:** 将 `DisplayFont` 和 `MonoFont` 统一为 JetBrains Mono（系统等宽字体回退），所有标题和数字使用 monospace 排版。

```kotlin
private val DisplayFont = FontFamily.Monospace   // 像素感标题
private val BodyFont = FontFamily.Default         // 正文保持可读性
private val MonoFont = FontFamily.Monospace       // 数字/时间
```

新增像素风专用 TextStyle：
```kotlin
val PixelDisplay = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 32.sp,
    fontWeight = FontWeight.Black,
    lineHeight = 38.sp,
    letterSpacing = 0.05.em,  // 加宽字距增强像素感
)

val PixelLabel = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    fontWeight = FontWeight.Bold,
    lineHeight = 13.sp,
    letterSpacing = 0.12.em,  // 超宽字距，类似老游戏菜单
)
```

**Rationale:** Monospace 字体的等宽特性天然匹配 8-bit 像素排版。加宽 letter-spacing 模拟老式游戏菜单的文字间距，增强复古感。正文保留默认字体确保长文本可读性。

### Decision 5: 概览卡片 —— 非对称彩色仪表盘

**Choice:** 概览卡片使用天蓝纯色背景 + 3dp navy 边框，内部为非对称两区布局：

```
┌──────────────────────────────────┐  3dp navy border
│  ██████████                      │  Sky Blue bg
│  128m        ┌────────┐          │
│  已专注       │ ✓ 5    │ coral bg │
│              │ 已完成   │          │
│              ├────────┤          │
│              │ ◇ 3    │ lavender │
│              │ 待办    │          │
│              └────────┘          │
└──────────────────────────────────┘
```

- 左侧：`PixelDisplay`(32sp Black) 白色大号时间数字 + PixelLabel "已专注"
- 右上徽章：PixelCoralRed 背景 + 2dp navy 边框 + 白色 "✓ 5 已完成"
- 右下徽章：PixelLavender 背景 + 2dp navy 边框 + 白色 "◇ 3 待办"

### Decision 6: 进行中卡片 —— 玫粉底色 + 像素星星

**Choice:** 纯色 PixelHotPink 背景 + 3dp navy 边框，呼吸动画改为像素风闪烁：

- 呼吸点从 `CircleShape` 改为 `PixelShape`（方块），颜色改为 PixelStarYellow
- 闪烁动画保持 `infiniteTransition` 但改为更"8-bit"的 step 式动画（`keyframes` 替代 `tween`，在两个值之间瞬切）
- "结束"按钮：PixelTeal 背景 + 2dp navy 边框 + 深色文字

```kotlin
// 像素风闪烁：在两个关键帧之间瞬切，而非平滑过渡
val alpha by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 800
            0f at 0
            1f at 400
            0f at 401
        },
        repeatMode = RepeatMode.Restart
    ),
    label = "pixelBlink"
)
```

### Decision 7: Section 标题 —— 彩色短线组件

**Choice:** 新增 `PixelSectionHeader` Composable：

```kotlin
@Composable
fun PixelSectionHeader(title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(12.dp).height(3.dp).background(accent))
        Spacer(Modifier.width(8.dp))
        Text(
            text = title.uppercase(),
            style = PixelLabel,
            color = PixelDeepNavy,
        )
    }
}
```

色彩分配：已完成 → PixelTeal，待办 → PixelAmberOrange，推荐 → PixelCoralRed。

### Decision 8: 已完成区块 —— 紧凑化 + 多彩色条

**Choice:**
- 行高 48dp，事件名字号 bodySmall (12sp)
- 左侧色条 3dp 宽，颜色按事件顺序轮换 `[PixelTeal, PixelSkyBlue, PixelLavender, PixelCoralRed, PixelStarYellow]`（取模循环）
- 时间 meta 文字使用 WarmGray500 + PixelLabel 风格
- 背景：PixelMintLight + 2dp PixelTeal 边框

### Decision 9: 待办区块 —— 彩色播放按钮

**Choice:**
- 白色背景 + 2dp PixelAmberOrange 边框 + 左侧 5dp 金色实色边缘条
- 播放按钮改为 28dp 方块（非圆形），每个按钮使用不同色彩背景：按 `[PixelCoralRed, PixelLavender, PixelTeal, PixelSkyBlue, PixelHotPink]` 轮换
- 播放图标使用白色三角形（`Icons.Default.PlayArrow` + `tint = Color.White`）
- 背景：White + 2dp PixelAmberOrange 边框

### Decision 10: 顶栏精简 + 底部输入栏色彩化

**Choice:**
- 顶栏：左侧像素日历图标（PixelTeal tint） + 中间日期文字 + 右侧 MoreVert 菜单（PixelDeepNavy），DropdownMenu 使用 2dp navy 边框
- 底部输入栏：2dp PixelLavender 边框 + 白色背景 + 粉红"+"号（PixelHotPink） + 琥珀色方块提交按钮（PixelAmberOrange 背景 + 白色播放图标）

## Risks / Trade-offs

- **[Risk]**: 全局直角可能导致某些 Material 3 组件（如 OutlinedTextField）的默认圆角难以完全覆盖。
  - **Mitigation**: 对 OutlinedTextField 使用 `shape = PixelShape` 参数覆盖，对 DropdownMenu 使用自定义 `MenuStyle`。逐个组件排查并覆盖。

- **[Risk]**: 多彩色板可能导致色彩过多、视觉疲劳。
  - **Mitigation**: 主背景保持 Cream (#FFF9F0)，彩色仅用于卡片/区块/按钮。每个色彩有明确的功能语义（蓝=概览、粉=进行中、绿=已完成、橙=待办），不是装饰性用色。

- **[Risk]**: Monospace 字体在中文字符下可能显示异常（中文字符通常为全角，monospace 不影响中文宽度）。
  - **Mitigation**: Monospace 仅用于数字和英文标签（时间、统计数据、section 标题），中文正文保留默认字体。

- **[Risk]**: 像素风闪烁动画（keyframes 瞬切）可能不如渐变动画流畅。
  - **Mitigation**: 闪烁仅用于进行中卡片的小方块指示灯（8dp），不影响主内容区域。保持 800ms 周期，频率不高。
