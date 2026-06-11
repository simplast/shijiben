## Context

事记本已完成第一轮 8-bit 像素风格改版（homescreen-8bit-colorful），当前使用 9 色多彩色板 + 像素虚线边框 + Press Start 2P 字体。但设计系统仍有多处不一致：Material 图标与像素风格冲突、虚线边框视觉不完整、色彩偏糖果风而非预期的夏日海边感、组件散落在各 screen 文件中缺乏统一规范。

本次改选仅涉及视觉/组件层，功能行为完全不变。

约束条件：
- 保持本地优先、无后端、纯 Kotlin/Compose 的技术栈
- 不引入第三方依赖
- 不改数据库 Schema
- 不改变任何功能逻辑
- 兼容 minSdk 26
- 中文用户界面

## Goals / Non-Goals

**Goals:**
- 建立系统化的像素 UI 组件库（PixelCard、PixelButton、PixelIcon 等）
- 将色彩基调从"多彩糖果风"调整为"夏日海边清新风"（海蓝+薄荷绿为主）
- 虚线边框升级为双线边框，提升完整感和游戏感
- 核心交互图标全部 Canvas 自绘，替换 Material Icons
- 统一首页、周报、月报、编辑器的设计语言
- 保持 Press Start 2P + 系统字体的混合排版策略

**Non-Goals:**
- 不改变任何功能行为（事件 CRUD、时间追踪、推荐算法等均不变）
- 不修改 ViewModel / Repository / Room 数据层
- 不新增页面或导航路由
- 不引入第三方图标库或设计框架
- 不做 Wear OS、桌面端等多平台适配
- 不做深色模式（本次仅定浅色基调，深色模式留待后续）

## Decisions

### Decision 1: 色彩系统 — 夏日海边青绿色系

**Choice:** 以海蓝 + 薄荷绿为主色调，日光黄 + 珊瑚橙为点缀，淡粉保留作特殊状态色。

```kotlin
// 主色系（海与天空）
val SeaBlue = Color(0xFF4DB6AC)         // 海蓝 — 主色、大面积使用
val LightSeaBlue = Color(0xFF80CBC4)   // 浅海蓝 — 卡片背景
val MintBlue = Color(0xFFB2DFDB)       // 薄荷蓝 — 浅色背景
val MistBlue = Color(0xFFE0F2F1)       // 雾蓝 — 最浅背景/分割线

// 点缀色（阳光与沙滩）
val SunYellow = Color(0xFFFFD54F)      // 日光黄 — 强调、装饰
val CoralOrange = Color(0xFFFF8A65)    // 珊瑚橙 — 重要操作、警告

// 特殊状态色（极少使用）
val LightPink = Color(0xFFF48FB1)      // 淡粉 — 仅用于"进行中"状态

// 基础色
val CreamWhite = Color(0xFFFFF8E1)     // 奶油白 — 页面背景
val DeepTeal = Color(0xFF263238)       // 深青蓝 — 文字/边框（替代纯黑）

// 暖灰阶（保持暖调）
val WarmGray50 = CreamWhite
val WarmGray100 = Color(0xFFFFF3D6)
val WarmGray200 = Color(0xFFF5E6C8)
val WarmGray300 = Color(0xFFE8D5B0)
val WarmGray500 = Color(0xFF8D6E63)
```

**色彩明暗变体**（按压态/悬停态）：
```
SeaBlue → SeaBlueDark = #3BA99C (按压) → SeaBlueLight = #80CBC4 (悬停)
CoralOrange → CoralOrangeDark = #FF7043 → CoralOrangeLight = #FFAB91
SunYellow → SunYellowDark = #FFCA28 → SunYellowLight = #FFEE58
```

**Rationale:**
- 蓝绿色系天然带来"海边、清爽、夏日"的联想，与时间管理的"冷静、专注"气质契合
- 减少粉紫色使用，避免"公主风、糖果感"
- 暖黄+珊瑚橙作点缀，保持活泼感，不沉闷
- 奶油白背景比纯白更有温度，符合日系像素游戏的暖调感

### Decision 2: 边框系统 — 双线像素边框

**Choice:** 用双线边框（外层主色 + 内层高光）替代当前的虚线边框。

```kotlin
// 双线边框 Modifier
fun Modifier.doublePixelBorder(
    outerColor: Color,
    innerColor: Color = Color.White,
    outerWidth: Dp = 2.dp,
    innerWidth: Dp = 1.dp,
): Modifier = this.drawBehind {
    // 外层边框
    drawRect(outerColor, style = Stroke(outerWidth.toPx()))
    // 内层边框（向内偏移 outerWidth）
    val inset = outerWidth.toPx()
    drawRect(
        innerColor,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2, size.height - inset * 2),
        style = Stroke(innerWidth.toPx())
    )
}
```

**三级边框体系：**

| 级别 | 外线 | 内线 | 用途 |
|------|------|------|------|
| 一级（高突出） | 2dp 海蓝/主色 | 1dp 白色 | 概览卡、进行中卡、对话框 |
| 二级（内容区） | 1.5dp 主色 | 0.5dp 同色系浅色 | 列表区、卡片组 |
| 三级（内嵌） | 1dp 主色 | 0.5dp 白色/透明 | 按钮、标签、徽章 |

**Rationale:**
- 双线边框是复古游戏 UI 的经典元素（对话框、窗口、面板），比单线更有"游戏感"
- 内层白色高光线模拟"凸起"的立体感，不使用阴影也能创造层次
- 实线条比虚线更完整、更"干净"，不会给人"没画完"的感觉

### Decision 3: 图标系统 — Canvas 自绘像素几何图标

**Choice:** 核心交互图标全部用 Canvas 绘制的简单几何形状，完全替换 Material Icons。

**图标清单（约 12-15 个）：**

| 图标名 | 形状 | 复杂度 | 用途 |
|--------|------|--------|------|
| Play | 右向三角形 | ★ | 开始、播放、提交 |
| Stop | 实心方块 | ★ | 停止、结束 |
| Close / Delete | X 形（四臂） | ★ | 删除、关闭 |
| Refresh | 环形箭头（简化） | ★★ | 再来一次、重做 |
| Check | 折线（对勾） | ★ | 完成、确认 |
| Diamond | 空心菱形 | ★ | 待办、未开始 |
| Star | 五角星（简化） | ★★ | 收藏、重要、成就 |
| Pause | 两条竖线 | ★ | 暂停 |
| More | 竖排三点 | ★ | 更多菜单 |
| Add | 加号 + | ★ | 添加、新建 |
| Calendar | 方块 + 横线条 | ★★ | 日历、日期 |
| Stats / Chart | 柱状图（三根柱） | ★★ | 统计、数据 |

**实现方式：**
```kotlin
// PixelIcons.kt
object PixelIcons {
    val Play: ImageVector
        get() = ImageVector.Builder(...).apply {
            // 用路径绘制右向三角形
        }.build()
}

// 或用 Canvas 直接绘制
@Composable
fun PixelPlayIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)
    }
}
```

**图标尺寸规范：**
- 大图标（按钮内）：20dp / 24dp
- 中图标（列表操作）：16dp / 18dp
- 小图标（装饰）：12dp / 14dp

**Rationale:**
- 简单几何图标与 8-bit 方块美学高度统一，不会"出戏"
- Canvas 自绘可精确控制每个像素，真正做到"像素完美"
- 零资源体积，颜色可动态定制
- 数量少（~12个核心图标），开发成本可控
- 功能/装饰类图标后续可引入 Kenney CC0 素材补充

### Decision 4: 组件体系化 — Pixel UI 组件库

**Choice:** 抽取独立的像素 UI 组件库，替代散落在各 screen 文件中的临时代码。

**组件清单：**

```kotlin
// PixelCard.kt — 像素卡片
@Composable
fun PixelCard(
    modifier: Modifier = Modifier,
    level: CardLevel = CardLevel.Secondary,  // Primary / Secondary / Tertiary
    backgroundColor: Color,
    borderColor: Color,
    content: @Composable () -> Unit
)

// PixelButton.kt — 像素按钮
@Composable
fun PixelButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    content: @Composable () -> Unit
)

@Composable
fun PixelIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    contentColor: Color,
    size: Dp = 32.dp,
)

// PixelIcon.kt — 像素图标
object PixelIcons {
    val Play, Stop, Close, Check, Refresh, Star, More, Add, Calendar, Stats, Pause, Diamond
}

// PixelBorder.kt — 双线边框 Modifier
fun Modifier.doublePixelBorder(...)

// PixelSectionHeader.kt — 区块标题
@Composable
fun PixelSectionHeader(title: String, accent: Color)

// PixelInput.kt — 像素输入框
@Composable
fun PixelInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
)

// PixelBadge.kt — 像素徽章
@Composable
fun PixelBadge(
    text: String,
    backgroundColor: Color,
    contentColor: Color = Color.White,
)
```

**文件结构：**
```
ui/theme/
  ├── Color.kt           （重写：海蓝色系）
  ├── Type.kt            （微调：字阶）
  ├── Shape.kt           （PixelShape 等）
  ├── PixelIcons.kt      （新增：像素图标库）
  ├── PixelComponents.kt （新增：PixelCard/PixelButton 等组件）
  ├── PixelBorder.kt     （新增：双线边框 Modifier）
  └── Theme.kt           （适配调整）
```

**Rationale:**
- 组件化后，首页、周报、月报、编辑器可复用同一套组件，确保风格统一
- 修改设计系统只需改组件文件，不用每个 screen 都改
- 为后续新增页面/功能提供一致的组件基础
- 符合 Compose 的组合式设计理念

### Decision 5: 首页布局 — 色彩更饱满，节奏更紧凑

**Choice:** 在现有布局基础上，通过双线边框和更丰富的色彩分布，增强"彩虹感"和游戏感。

**概览卡**
- 背景：SeaBlue（海蓝）
- 边框：双线（2dp DeepTeal + 1dp 白色）
- 左侧：白色大号时间数字 + 像素标签"已专注"
- 右侧：两个彩色徽章（珊瑚橙已完成 + 浅海蓝待办）
- 内边距略增，让双线边框有呼吸空间

**进行中卡**
- 背景：LightPink（淡粉，特殊状态色，少量保留）
- 边框：双线（2dp DeepTeal + 1dp 白色）
- 闪烁指示灯：SunYellow 方块，帧式闪烁
- 计时数字：白色大号
- 结束按钮：SeaBlue 背景 + 白色文字 + 双线边框

**已完成区**
- 背景：MintBlue（薄荷蓝，浅色）
- 边框：双线（1.5dp SeaBlue + 0.5dp 白色）
- 行高：48dp（保持紧凑）
- 左边色条：5 色轮换（海蓝/浅海蓝/日光黄/珊瑚橙/薄荷绿）
- 操作按钮：像素图标（Refresh/Delete），尺寸增大到 28dp

**待办区**
- 背景：CreamWhite（奶油白）
- 边框：双线（1.5dp SeaBlue + 0.5dp 白色）
- 左边条：SunYellow（日光黄）5dp
- 播放按钮：彩色方块 + 白色三角图标，尺寸增大到 32dp
- 删除按钮：珊瑚橙 X 形图标，尺寸与播放按钮对称

**Section 标题**
- 彩色短线 + 像素大写英文标签 + 中文标题
- 颜色与区块主题色一致

### Decision 6: 其他页面适配策略

**周报/月报页面**
- 应用同一套 PixelCard、PixelSectionHeader 组件
- 图表/统计部分用像素风格柱状图/进度条替代现有样式
- 保持数据展示逻辑不变

**事件编辑器**
- 底部 sheet 改用双线边框
- 输入框改用 PixelInput
- 按钮改用 PixelButton

## Risks / Trade-offs

- **[Risk]** Canvas 自绘图标的工作量和效果可能与预期有差距
  - **Mitigation**: 先实现最高频的 4-5 个图标（Play/Stop/Delete/Check/Add）验证效果，其余暂用简化形状或保留 Material 图标待后续替换

- **[Risk]** 双线边框在小尺寸组件（如 24dp 图标按钮）上可能显粗
  - **Mitigation**: 三级边框内线使用 0.5dp 或透明内线，仅保留外线；或小尺寸组件用单线边框

- **[Risk]** 色彩从多色糖果系转向海蓝系，用户可能需要适应期
  - **Mitigation**: 保持"多彩"的核心特征（色条、按钮仍用多色轮换），只是主基调从粉紫转向蓝绿；非破坏性变更

- **[Risk]** 组件抽取可能引入回归 bug
  - **Mitigation**: 组件化与视觉重构分开进行，先抽取组件保持视觉不变，再统一改样式；每个阶段用 assembleDebug 验证

- **[Trade-off]** 用 Canvas 自绘图标 vs 引入 Kenney 素材
  - 自绘：零体积、风格完全可控、无版权问题；但开发成本高，复杂图标难画
  - 素材：快速可用、风格成熟；需要引入 PNG 资源，像素缩放需处理
  - **选择**: 核心交互图标自绘（保证统一），装饰/功能图标后续再评估是否引入素材
