# 8-bit 像素字体调研报告

> 调研时间：2026-06-10
> 调研目标：探索适合移动端 App 的像素字体方案，特别是中文支持

---

## 1. 拉丁字符像素字体

### 1.1 流行像素字体对比

| 字体名称 | 设计尺寸 | 字重 | 特点 | 许可证 | 来源 |
|---------|---------|------|------|--------|------|
| **Press Start 2P** | 8×8 | Regular | 经典 8-bit 风格，字宽较宽 | OFL | Google Fonts ✓ |
| **Silkscreen** | 7×7 | Regular/Bold | 紧凑设计，可读性好 | OFL | Google Fonts ✓ |
| **VT323** | 16×16 | Regular | 终端风格，较大尺寸 | OFL | Google Fonts ✓ |
| **Pixelify Sans** | 可变 | Light-Bold | 现代像素风格，多字重 | OFL | Google Fonts ✓ |
| **04b03** | 4×5 | Regular | 极小尺寸，紧凑 | 免费 | 04.jp.org |
| **Dogica** | 8×8 | Regular/Bold | 几何风格，现代感 | 免费 | itch.io |
| **m5x7** | 5×7 | Regular | 极简网格设计 | CC0 | Daniel Linssen |
| **Arcade Classic** | 8×8 | Regular | 街机风格，粗线条 | 免费 | 多个来源 |

### 1.2 字体视觉对比

```
Press Start 2P (8×8, 当前使用):
  ██████  ████  ███████ ██████  ████
  ██   ██ ██ ██ ██      ██     ██  ██
  ██████  ████  ███████ █████  ██
  ██      ██ ██      ██ ██     ██  ██
  ██      ████  ███████ ██████  ████
  特点：经典、辨识度高、字宽固定

Silkscreen (7×7, 更紧凑):
  ████ ███ █████ ████ ███
  █    █   █     █    █
  ███  █   ███   ███  ███
  █    █   █     █    █
  ████ ███ █████ ████ ███
  特点：紧凑、现代、可读性好

VT323 (16×16, 终端风格):
  ████████████  ████████  ██████████
  ██      ██  ██    ██  ██      ██
  ██      ██  ██    ██  ██      ██
  ██      ██  ████████  ██      ██
  ██      ██  ██    ██  ██      ██
  特点：大尺寸、终端感、适合数据展示

Pixelify Sans (可变, 现代感):
  ██████  ██████  ███████  ██████  ██████
  ██   ██ ██   ██ ██       ██      ██   ██
  ██████  ██████  █████    █████   ██████
  ██      ██   ██ ██       ██      ██   ██
  ██      ██   ██ ███████  ██████  ██   ██
  特点：现代、多字重、适合 UI
```

### 1.3 事记本当前方案评估

**当前**：Press Start 2P (仅用于展示级文字)

**优势**：
- ✓ Google Fonts 免费，易于集成
- ✓ 8-bit 风格纯正，辨识度高
- ✓ 支持多语言拉丁字符
- ✓ 社区广泛使用，有参考案例

**劣势**：
- △ 字宽较宽，占用空间大
- △ 仅 Regular 字重，缺乏变化
- △ 小尺寸（<10sp）可读性一般

**建议**：保持 Press Start 2P 作为主展示字体，但可以考虑：
1. 添加 Silkscreen 作为备选（更紧凑，适合小标签）
2. 添加 Pixelify Sans 用于需要多字重的场景

---

## 2. 中文/CJK 像素字体

### 2.1 核心挑战

**问题**：中文字符笔画复杂，像素化后可读性急剧下降。

```
示例："事记本" 在不同像素密度下的表现

高像素密度 (32×32):
  ████████████████████████████████
  ██  ████████████████████████  ██
  ██  ██  ██  ██  ██  ██  ██  ██
  ██  ██  ██  ██  ██  ██  ██  ██
  ████████████████████████████████
  ✓ 可读，但占用空间过大

中等像素密度 (16×16):
  ████████████████
  ██  ██  ██  ██
  ██  ██  ██  ██
  ████████████████
  △ 部分可读，细节丢失

低像素密度 (8×8):
  ████████
  ██  ██
  ██  ██
  ████████
  ✗ 几乎不可读
```

### 2.2 中文字体方案

#### A. 文泉驿微米黑（WenQuanYi Micro Hei）

**特点**：
- 开源中文字体（GPL v3）
- 小尺寸可读性好（最小 12px）
- 字形简洁，笔画清晰
- 支持 GB2312/GBK 字符集

**像素化处理**：
```kotlin
// 在 Compose 中模拟像素感
Text(
    text = "事记本",
    fontFamily = FontFamily(Font(R.font.wenquanyi_micro_hei)),
    fontSize = 16.sp,
    letterSpacing = 0.05.em,  // 增加字距，模拟像素感
    // 禁用抗锯齿（可选，更硬核）
)
```

**优势**：
- ✓ 可读性最佳
- ✓ 开源免费
- ✓ 小尺寸表现好

**劣势**：
- △ 不是真正的像素字体
- △ 需要通过渲染技巧模拟像素感

#### B. 方正像素字系列

**特点**：
- 真正的中文字体（每字 16×16 或更大网格）
- 笔画经过像素化设计
- 商业字体，需要授权

**常见版本**：
- 方正像素 12（12px 设计）
- 方正像素 16（16px 设计）
- 方正像素 24（24px 设计）

**优势**：
- ✓ 真正的像素风格
- ✓ 视觉效果纯正

**劣势**：
- ✗ 商业授权，成本高
- ✗ 字符集可能不完整
- ✗ 小尺寸可读性一般

#### C. 汉仪像素字系列

**特点**：类似方正，商业字体

#### D. Zpix（最佳像素字体）

**特点**：
- 开源中文字体（MIT 许可证）
- 基于 16×16 像素网格设计
- GitHub: https://github.com/SolidZORO/zpix-pixel-font
- 支持 GB2312 字符集（6763 字）

**优势**：
- ✓ 开源免费
- ✓ 真正的像素风格
- ✓ 社区活跃

**劣势**：
- △ 字符集有限（可能缺少生僻字）
- △ 16px 最小尺寸，占用空间大

### 2.3 推荐方案：混合策略

**最佳实践**：不同层级使用不同字体策略

```
┌─────────────────────────────────────────────┐
│ 层级          │ 字体方案                │ 原因      │
├─────────────────────────────────────────────┤
│ 英文标题/数字  │ Press Start 2P        │ 8-bit 感 │
│ 中文标题       │ 系统字体 + 加粗        │ 可读性   │
│                │ + 像素化边框装饰       │          │
│ 中文正文       │ 系统默认字体           │ 可读性   │
│ 中文标签       │ 系统字体 + 全大写间距  │ 平衡     │
│ 装饰性中文     │ Zpix（如授权允许）     │ 风格统一 │
└─────────────────────────────────────────────┘
```

**事记本当前方案**（✓ 已经是最佳实践）：
```kotlin
val PixelFont = FontFamily(Font(R.font.press_start_2p))  // 英文/数字
val BodyFont = FontFamily.Default                         // 中文正文
```

---

## 3. 字体渲染技术

### 3.1 像素完美渲染

**问题**：现代高分屏（2x/3x）上，像素字体可能出现模糊或不对齐。

#### A. 整数缩放

```kotlin
// ✓ 正确：使用整数 sp
Text(
    text = "12:30",
    fontSize = 16.sp  // 整数
)

// ✗ 错误：使用小数 sp
Text(
    text = "12:30",
    fontSize = 15.5.sp  // 会导致亚像素渲染
)
```

#### B. 像素对齐

```kotlin
// ✓ 正确：使用整数 dp
Modifier.offset(x = 8.dp, y = 12.dp)

// ✗ 错误：使用小数 dp
Modifier.offset(x = 8.5.dp, y = 12.3.dp)
```

#### C. 禁用抗锯齿（可选，更硬核）

```kotlin
// 在 Canvas 绘制时禁用抗锯齿
Canvas(modifier = Modifier.fillMaxSize()) {
    drawContext.canvas.nativeCanvas.apply {
        isAntiAlias = false  // 禁用抗锯齿
    }
    // 绘制像素图形
}
```

### 3.2 Android 屏幕密度适配

**挑战**：Android 设备 DPI 多样（mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi）

**方案 1：矢量像素风格**（事记本当前方案，✓ 推荐）
```kotlin
// 使用 Compose 绘制几何形状
Box(
    modifier = Modifier
        .size(48.dp)  // 基于 dp，自动适配
        .background(Color.Red)
)
```

**方案 2：位图缩放**
```kotlin
// 设计时以 1x 像素网格绘制，导出 2x/3x/4x 资源
// 缩放时使用最近邻插值
Image(
    painter = painterResource(id),
    contentDescription = null,
    contentScale = ContentScale.Fit,
    filterQuality = FilterQuality.None  // 关键：禁用双线性过滤
)
```

### 3.3 字体渲染设置

```kotlin
// 推荐的字体渲染配置
@Composable
fun PixelText(
    text: String,
    fontSize: TextUnit,
    fontFamily: FontFamily = PixelFont
) {
    Text(
        text = text,
        fontSize = fontSize,
        fontFamily = fontFamily,
        // 禁用字体缩放（可选）
        style = LocalTextStyle.current.copy(
            platformStyle = PlatformTextStyle(
                includeFontPadding = false
            )
        ),
        // 禁用文本装饰
        textDecoration = TextDecoration.None,
        // 使用硬边缘渲染
        softWrap = false
    )
}
```

---

## 4. 排版层级

### 4.1 推荐层级系统

```
┌───────────────────────────────────────────────────┐
│ Token          │ Size │ Font              │ 用途     │
├───────────────────────────────────────────────────┤
│ Display        │ 32sp │ Press Start 2P    │ 统计数字 │
│ H1             │ 24sp │ Press Start 2P    │ 页面标题 │
│ H2             │ 18sp │ Press Start 2P    │ 卡片标题 │
│ H3             │ 16sp │ Press Start 2P    │ 组件标题 │
│ Body           │ 14sp │ 系统字体          │ 正文     │
│ BodySmall      │ 12sp │ 系统字体          │ 辅助文字 │
│ Label          │ 10sp │ Press Start 2P    │ 标签     │
└───────────────────────────────────────────────────┘
```

### 4.2 关键原则

1. **像素字体最小尺寸**：10sp（低于此不可读）
2. **中文字体最小尺寸**：12sp
3. **行高计算**：字体大小 × 1.5（像素字体需要更多行高）
4. **字距调整**：
   - 像素字体：letterSpacing = 0.05em（加宽）
   - 中文字体：letterSpacing = 0（默认）

### 4.3 中文标题装饰技巧

由于中文使用系统字体，可以通过装饰元素增强像素感：

```
方案 1：方块装饰
┌──────────────────┐
│ ■ 今日概览 ■      │  ← 方块 + 系统字体
└──────────────────┘

方案 2：像素边框
╔══════════════════╗
║  本周统计         ║  ← 像素虚线边框
╚══════════════════╝

方案 3：彩色短线
─── 已完成 ───      ← 3dp 彩色短线 + 系统字体
```

---

## 5. 字体配对建议

### 5.1 事记本当前配对（✓ 推荐）

```
英文/数字展示：Press Start 2P (8-bit 风格)
中文正文：系统默认字体 (保证可读性)
```

**优势**：
- ✓ 简洁明了，易于维护
- ✓ 中文可读性最佳
- ✓ 符合平台设计规范

### 5.2 备选方案

**方案 A：添加 Silkscreen 作为紧凑备选**
```kotlin
val CompactPixelFont = FontFamily(Font(R.font.silkscreen))

// 用于空间受限的小标签
Text(
    text = "NEW",
    fontFamily = CompactPixelFont,
    fontSize = 10.sp
)
```

**方案 B：添加 Pixelify Sans 用于多字重场景**
```kotlin
val ModernPixelFont = FontFamily(Font(R.font.pixelify_sans))

// 用于需要粗体/细体变化的场景
Text(
    text = "Important",
    fontFamily = ModernPixelFont,
    fontWeight = FontWeight.Bold
)
```

---

## 6. 实际案例

### 6.1 成功使用像素字体的中文 App

**Case 1: 某些独立游戏**
- 使用 Zpix 或方正像素字
- 仅用于标题和装饰，正文用系统字体

**Case 2: 复古风格工具 App**
- 英文/数字用 Press Start 2P
- 中文用文泉驿微米黑 + 像素化渲染

**Case 3: 事记本（当前）**
- 英文/数字用 Press Start 2P
- 中文用系统默认字体
- 通过边框和装饰增强像素感

### 6.2 失败案例警示

**避免**：
- ✗ 全文使用像素中文字体（可读性差）
- ✗ 像素字体小于 10sp（无法辨认）
- ✗ 在动画中使用像素字体（渲染问题）
- ✗ 忽略屏幕密度适配（模糊/锯齿）

---

## 7. 资源链接

### 字体下载
- **Google Fonts**: https://fonts.google.com
  - Press Start 2P ✓
  - Silkscreen ✓
  - VT323 ✓
  - Pixelify Sans ✓

- **Zpix**: https://github.com/SolidZORO/zpix-pixel-font
- **文泉驿**: http://wenq.org/
- **DaFont Pixel Fonts**: https://www.dafont.com/theme.php?cat=302

### 字体工具
- **FontForge**: https://fontforge.org/ — 字体编辑器
- **Glyphs**: https://glyphsapp.com/ — 专业字体设计工具
- **Font Squirrel**: https://www.fontsquirrel.com/ — 免费商用字体

---

## 8. 总结与建议

### 8.1 事记本当前方案评估

**当前方案**：Press Start 2P (英文/数字) + 系统字体 (中文)

**评分**：9/10

**优势**：
- ✓ 简洁有效，易于维护
- ✓ 中文可读性最佳
- ✓ 符合平台规范

**可优化**：
- △ 考虑添加 Silkscreen 作为紧凑备选
- △ 中文标题可增加装饰元素增强像素感

### 8.2 最终建议

1. **保持当前方案**：已经是最佳实践，无需大改
2. **可选增强**：
   - 添加 Silkscreen 字体（用于 <10sp 的小标签）
   - 中文标题使用装饰技巧（方块、边框、短线）
3. **避免**：
   - 不要尝试用像素字体渲染中文正文
   - 不要使用小于 10sp 的像素字体
   - 不要禁用系统字体缩放（影响无障碍）
