# DESIGN.md — 事记本 · 8-bit 像素扁平多彩风格

> 项目类型：移动端个人时间管理 App（事件记录 + 时间追踪）
> 设计哲学：8-bit Retro Pixel · Flat & Colorful · Playful & Bold
> 参考风格：复古游戏 UI × 现代扁平设计 × 多彩配色

---

## 1. Visual Theme & Atmosphere（视觉主题与氛围）

- **设计哲学**: 像打开一台复古掌机——方块、像素字、鲜艳色彩，充满游戏感的趣味性
- **视觉基调**: 8-bit 像素扁平主义 — 直角方块 × 像素虚线边框 × 多彩纯色填充，兼具复古趣味与现代清晰
- **核心视觉特征**: 方块直角 · 像素排版 · 多彩配色 · 帧式动画 · 游戏 UI 感
- **光影与质感倾向**: 无阴影、无渐变、无圆角。纯平面色块 + 像素虚线边框，层级靠边框粗细和色彩区分
- **动效气质**: 帧式闪烁（pixel blink），在值之间瞬切而非平滑过渡，模拟老游戏的逐帧动画

---

## 2. Color Palette & Roles（调色板与角色）

### 8-bit Colorful Palette（多彩色板）

| Kotlin Token | HEX | 用途 |
|---|---|---|
| `PixelSkyBlue` | #6BC5F5 | 概览卡背景 |
| `PixelHotPink` | #FF6B9D | 进行中卡背景 / "+"号 |
| `PixelCoralRed` | #FF8A6B | 已完成徽章 / 推荐标签 / Section 短线 |
| `PixelLavender` | #C4B5E0 | 待办徽章 / 输入栏边框 |
| `PixelTeal` | #4DC9B8 | 已完成区背景边框 / 标签 / Section 短线 |
| `PixelAmberOrange` | #FFB347 | 待办区边框 / 提交按钮 / Section 短线 |
| `PixelMintLight` | #E8F8F0 | 已完成区卡片背景 |
| `PixelStarYellow` | #FFD93D | 像素星星 / 装饰 / 进行中指示灯 |
| `PixelDeepNavy` | #1A1A2E | 主边框色 / 深色文字 |

### Primary Colors（基础主色）

| Kotlin Token | HEX | 用途 |
|---|---|---|
| `PrimaryGold` | #F5C469 | 待办区左边缘条 / 装饰元素 |
| `PrimaryGoldSoft` | #FEF3D9 | 淡金背景 |
| `PrimaryGoldDark` | #E5B050 | 按压态 |

### Neutral / Warm Gray（暖灰阶）

| Kotlin Token | HEX | 用途 |
|---|---|---|
| `WarmGray50` | #FFF9F0 | 页面背景（奶油白） |
| `WarmGray100` | #FDF5E8 | 卡片软背景 |
| `WarmGray200` | #F5EEDF | 分割线 |
| `WarmGray300` | #E8DED0 | 禁用态、placeholder |
| `WarmGray500` | #9A8E7E | 辅助文字、时间戳 |

### Border Tokens（边框色）

| Kotlin Token | 值 | 用途 |
|---|---|---|
| `PixelBorder` | PixelDeepNavy (#1A1A2E) | 主边框 — 高突出卡片 |
| `PixelBorderLight` | PixelDeepNavy 20% alpha | 轻量分割 — 内嵌元素 |

### Semantic Colors（语义色）

| 语义 | Token | HEX |
|---|---|---|
| 成功 | `AccentMint` | #7DD3A8 |
| 警告 | `AccentAmber` | #FFB347 |
| 错误 | `AccentCoral` | #FF8A6B |

### Module Color Assignment（模块色彩分配表）

| 模块 | 背景色 | 边框 | 文字/装饰色 |
|---|---|---|---|
| 概览卡 | PixelSkyBlue | 3dp navy 像素虚线 | 白色数字 + CoralRed/Lavender 徽章 |
| 进行中卡 | PixelHotPink | 3dp navy 像素虚线 | 白色文字 + StarYellow 方块灯 + Mint 按钮 |
| 已完成区 | PixelMintLight | 2dp Teal 像素虚线 | 多彩色条轮换（Teal/SkyBlue/Lavender/CoralRed/StarYellow） |
| 待办区 | White | 2dp AmberOrange 像素虚线 + 5dp Gold 左边 | 多彩播放按钮轮换（CoralRed/Lavender/Teal/SkyBlue/HotPink） |
| 推荐标签 | 各自色彩底 | 1dp navy 像素虚线 | 白色文字 |
| 底部输入栏 | White | 2dp Lavender 像素虚线 | HotPink "+" + AmberOrange 提交按钮 |

---

## 3. Typography Rules（排版规则）

### Font Family

```kotlin
val PixelFont = FontFamily(Font(R.font.press_start_2p))  // 8-bit 像素字体
val DisplayFont = PixelFont   // 标题、大号数字
val MonoFont = PixelFont      // 计时数字、统计数据
val BodyFont = FontFamily.Default  // 中文正文（确保可读性）
```

> `Press Start 2P` 负责所有展示级文字（标题、数字、标签），营造 8-bit 游戏 UI 感。中文正文保留系统默认字体确保可读性。

### Type Scale

| Token | Size | Weight | Line Height | Letter Spacing | 使用场景 |
|---|---|---|---|---|---|
| `PixelDisplay` | 32sp | Black | 38sp | 0.05em | 概览时间数字、进行中计时 |
| `textH1` | 26sp | SemiBold | 32sp | -0.01em | 区块标题 |
| `textH2` | 22sp | SemiBold | 28sp | 0 | 卡片标题 |
| `textH3` | 18sp | SemiBold | 24sp | 0 | 组件标题 |
| `textBody` | 16sp | Normal | 25sp | 0 | 正文（中文用 BodyFont） |
| `textBodySmall` | 12sp | Normal | 17sp | 0 | 已完成事件名、辅助文字 |
| `PixelLabel` | 10sp | Bold | 13sp | 0.12em | Section 标题、标签（全大写） |

### 像素排版规则

- 展示级标题和数字用 **Press Start 2P + 加宽字距(0.05em)** — 像素感核心
- Section 标签用 **PixelLabel + 全大写 + 超宽字距(0.12em)** — 模拟老游戏菜单
- 中文正文用 **系统默认字体** — 确保汉字可读性（像素字体不支持中文）
- 时间 meta 信息用 **PixelLabel + WarmGray500** — 像素风时间戳

---

## 4. Shape & Border System（形状与边框系统）

### Shape — 全局直角

```kotlin
val PixelShape = RoundedCornerShape(0.dp)  // 所有组件使用
```

**规则**: 所有 Card、Button、Badge、Input、DropdownMenu 统一使用 `PixelShape`（0dp 圆角），无例外。

### Border — 像素虚线替代实线

不使用 `BorderStroke` 实线边框，改用自定义 `pixelBorder` Modifier 绘制像素风虚线：

```kotlin
fun Modifier.pixelBorder(
    color: Color,
    width: Dp = 2.dp,
    pixelSize: Dp = 4.dp,  // 像素块大小
    gap: Dp = 2.dp,          // 像素间隔
): Modifier = this.drawBehind { ... }
```

层级规则：
- **高突出卡片**（概览、进行中）：`pixelBorder(PixelDeepNavy, 3.dp)`
- **内容卡片**（已完成、待办）：`pixelBorder(彩色, 2.dp)`
- **浮动元素**（底部输入栏、菜单）：`pixelBorder(彩色, 2.dp)`
- **内嵌元素**（标签、按钮）：`pixelBorder(彩色, 1.dp)`

### No Shadows（无阴影）

全局禁用 `elevation` / `box-shadow`。层级完全靠边框粗细和色彩区分。

### No Gradients（无渐变）

所有背景使用纯色填充（`solid Color`）。禁止 `Brush.linearGradient` 和 `Brush.radialGradient`。

---

## 5. Component Stylings（组件样式）

### TodayOverviewCard（概览仪表盘）

- 布局：非对称两区 — 左侧大号白色时间数字（PixelDisplay 32sp），右侧垂直堆叠两个彩色徽章
- 背景：PixelSkyBlue + 3dp navy 像素虚线边框
- 已完成徽章：PixelCoralRed 背景 + 2dp navy 边框 + 白色文字
- 待办徽章：PixelLavender 背景 + 2dp navy 边框 + 白色文字

### ActiveEventCard（进行中卡）

- 背景：PixelHotPink 纯色 + 3dp navy 像素虚线
- 呼吸灯：8dp 方块（PixelShape）+ PixelStarYellow + 帧式闪烁（keyframes 800ms 瞬切）
- 计时数字：PixelDisplay 32sp 白色
- 结束按钮：PixelTeal 背景 + 2dp navy 边框 + 深色文字 + 直角矩形

### CompletedSection（已完成区）

- 卡片：PixelMintLight 背景 + 2dp PixelTeal 像素虚线
- 行高：48dp（紧凑模式）
- 事件名：bodySmall 12sp
- 色条：3dp 宽，颜色按事件顺序轮换 [Teal, SkyBlue, Lavender, CoralRed, StarYellow]
- 操作按钮：24dp 直角方块，restart + delete 统一尺寸

### PendingSection（待办区）

- 卡片：White 背景 + 2dp PixelAmberOrange 像素虚线 + 5dp PixelStarYellow 左边缘
- 播放按钮：22dp 方块 + 多彩背景（[CoralRed, Lavender, Teal, SkyBlue, HotPink] 轮换）+ 12dp 白色三角
- 删除按钮：28dp + 14dp 图标（与播放按钮视觉对称）
- press 反馈：PixelAmberOrange 15% alpha 背景

### PixelSectionHeader（Section 标题）

- 12dp × 3dp 彩色短线 + PixelLabel 大写文字
- 已完成 → PixelTeal，待办 → PixelAmberOrange，推荐 → PixelCoralRed

### QuickNameLine（底部输入栏）

- White 背景 + 2dp PixelLavender 像素虚线
- "+"图标：PixelHotPink
- 提交按钮：PixelAmberOrange 方块 + 白色播放三角

### TopBar（顶栏）

- 左侧：日历图标（PixelTeal tint）
- 中间：日期文字
- 右侧：MoreVert 菜单（PixelDeepNavy tint）→ DropdownMenu（2dp navy 像素虚线）

### Badges / Tags（标签）

- 直角方块（PixelShape）+ 多彩纯色背景 + 1dp navy 像素虚线
- 文字：白色 + PixelLabel 风格
- 每个标签使用不同色彩底色

---

## 6. Layout Principles（布局原则）

### Spacing System

- **基数**: 4dp
- **尺度表**: 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48

### Section Spacing Rhythm（区块间距节奏）

- 概览卡 → 进行中卡：16dp
- 进行中卡 → 已完成区：12dp
- 已完成区 → 待办区：20dp
- 列表项间距：0dp（由卡片内部 Hairline 分隔）
- 页面水平安全边距：20dp

### 方块哲学

"像素方块是游戏的基石"——每个 UI 元素都是一个方块，方块与方块之间的间距创造了节奏感。已完成区块使用 48dp 紧凑行高（"历史记录退后"），待办区块使用 48dp 标准行高（"行动召唤突出"）。

---

## 7. Animation（动画）

### 帧式闪烁（Pixel Blink）

进行中卡片的呼吸灯使用 keyframes 帧式动画，在值之间瞬切：

```kotlin
val alpha by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = keyframes {
            durationMillis = 800
            0f at 0
            1f at 400
            0f at 401  // 瞬切，非平滑
        },
        repeatMode = RepeatMode.Restart
    ),
    label = "pixelBlink"
)
```

### Press Scale（按压缩放）

可点击元素按压时缩放至 97%，使用 spring 弹性曲线。

---

## 8. Do's and Don'ts（设计规范与禁忌）

### Do's

1. **使用直角方块** — 所有元素 0dp 圆角，PixelShape 统一
2. **使用像素虚线边框** — `pixelBorder()` Modifier，禁止实线 `BorderStroke`
3. **使用纯色填充** — 所有背景 `solid Color`，禁止渐变
4. **使用像素字体** — Press Start 2P 用于标题和数字，系统字体用于中文正文
5. **每个模块用不同色彩** — 蓝=概览、粉=进行中、绿=已完成、橙=待办
6. **标签全大写 + 超宽字距** — PixelLabel 10sp + letterSpacing 0.12em
7. **帧式动画** — 闪烁/指示灯用 keyframes 瞬切，模拟 8-bit
8. **操作按钮对称** — 同行的图标按钮保持统一视觉尺寸
9. **层级靠边框和色彩** — 3dp navy = 高突出，2dp 彩色 = 内容区，1dp = 内嵌
10. **空状态要有温度** — 用像素风文案，不要裸显示"暂无数据"

### Don'ts

1. **不要使用圆角** — 所有 border-radius 必须为 0dp
2. **不要使用阴影** — 全局禁用 elevation / box-shadow
3. **不要使用渐变** — 禁止 linearGradient / radialGradient
4. **不要使用实线边框** — 必须使用 pixelBorder 像素虚线
5. **不要使用平滑动画** — 闪烁效果必须用 keyframes 瞬切
6. **不要用像素字体渲染中文** — 中文字符用系统默认字体
7. **不要冷灰色** — 所有灰阶保持暖调
8. **不要纯色黑文字** — 用 PixelDeepNavy (#1A1A2E)
9. **不要忽略触摸反馈** — 每个可点击元素必须有 press scale 或背景色变化
10. **不要在同区块内使用过多色彩** — 每个模块一个主色 + 少量装饰色，不是彩虹

---

## 9. Agent Prompt Guide（AI 代理提示指南）

### Quick Reference

```
项目: 事记本 — 8-bit 像素扁平多彩时间管理 App
色板: 9 色多彩（SkyBlue/HotPink/CoralRed/Lavender/Teal/AmberOrange/MintLight/StarYellow/DeepNavy）
基础: PrimaryGold (#F5C469) + WarmGray 暖灰阶
字体: Press Start 2P (pixel) + 系统默认 (中文 body)
形状: 全局 0dp 圆角 (PixelShape)
边框: pixelBorder() 像素虚线，禁止 BorderStroke 实线
背景: 纯色填充，禁止渐变
阴影: 无
动画: keyframes 帧式瞬切
层级: 3dp navy = 高突出，2dp 彩色 = 内容，1dp = 内嵌
```

### Component Prompts

```
Prompt 1 — 概览仪表盘:
"使用 DESIGN.md 8-bit 规范，创建非对称概览卡片。PixelSkyBlue 背景 + 3dp navy pixelBorder。左侧 PixelDisplay(32sp) 白色时间数字，右侧堆叠 PixelCoralRed 已完成徽章和 PixelLavender 待办徽章。直角方块，无阴影。"

Prompt 2 — 进行中像素卡:
"创建进行中事件卡。PixelHotPink 纯色背景 + 3dp navy pixelBorder。左侧 8dp PixelStarYellow 方块闪烁灯（keyframes 800ms 瞬切）。白色 PixelDisplay 计时数字。PixelTeal 直角结束按钮 + 2dp navy pixelBorder。"

Prompt 3 — 已完成紧凑列表:
"创建已完成区。PixelMintLight 背景 + 2dp Teal pixelBorder。48dp 行高，12sp 事件名。3dp 多彩色条按 [Teal, SkyBlue, Lavender, CoralRed, StarYellow] 轮换。24dp 统一尺寸操作按钮。直角方块。"

Prompt 4 — 待办多彩列表:
"创建待办区。White 背景 + 2dp AmberOrange pixelBorder + 5dp StarYellow 左边缘。22dp 方块播放按钮，多彩背景 [CoralRed, Lavender, Teal, SkyBlue, HotPink] 轮换 + 12dp 白色三角。28dp 删除按钮保持对称。直角方块。"

Prompt 5 — Section 像素标题:
"创建 PixelSectionHeader。12dp × 3dp 彩色短线 + PixelLabel(10sp Bold) 大写文字。已完成用 Teal，待办用 AmberOrange，推荐用 CoralRed。全大写 + letterSpacing 0.12em。"
```
