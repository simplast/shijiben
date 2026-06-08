## Why

DESIGN.md 已从珊瑚橙 (#FF7A5A) 切换为暖金色 (#F5C469) 主色调，但首页代码仍使用旧的 Coral 色系。视觉身份与设计规范严重脱节，具体问题如下：

1. **主色体系不匹配**：DESIGN.md 主色为 `#F5C469`（暖金），代码仍用 `#FF7A5A`（珊瑚橙），两者是截然不同的视觉身份。
2. **圆角体系偏小**：DESIGN.md 规定卡片 20dp、按钮 16dp，当前代码卡片仅 16dp、按钮 12dp，不够圆润亲和。
3. **排版层级缺失**：DESIGN.md 要求 Outfit 展示字体 + Inter 正文字体 + 全大写标签风格，当前代码使用默认 M3 Typography，无自定义字体、无 label 排版规范。
4. **间距体系不系统**：安全边距 14dp vs DESIGN.md 的 20dp，列表项高度 48dp vs 56dp，整体偏紧凑缺乏呼吸感。
5. **阴影系统不规范**：代码用 `CardDefaults.cardElevation(defaultElevation = 1.dp)`，DESIGN.md 定义了 6 级暖调阴影 Token。
6. **缺少日历选择条组件**：DESIGN.md 新增了 Calendar Strip（黄色背景横向日历），当前仍用弹窗式 DayPickerDialog。
7. **Hero Card 渐变色不匹配**：当前 `#FF7A5A → #FFB347 → #4DC9B8`，DESIGN.md 要求以暖金色为主体的渐变。
8. **推荐 Chip 和 Badge 色彩基于旧主色**：PrimaryCoralSoft 等需替换为 PrimaryGoldSoft。

## What Changes

### 一、色彩体系全面切换：Coral → Warm Gold
- 重写 `ui/theme/Color.kt`：主色从 `#FF7A5A` 改为 `#F5C469`，soft/dark 变体同步更新。
- 背景色从 `#FCFAF7` 调整为 `#FFF9F0`，灰阶系统全面对齐 DESIGN.md 暖调灰阶。
- 更新 `ui/theme/Theme.kt`：lightColorScheme / darkColorScheme 映射新色板。
- Hero Card 渐变色更新为暖金主体渐变。

### 二、排版体系建立
- 引入 `Outfit` 字体族（展示标题）+ `Inter` 字体族（正文）+ `JetBrains Mono`（数字/时间）。
- 在 `ui/theme/Type.kt` 中定义 9 级 Type Scale（display → nano）。
- 新增 Label 排版规范：10sp / Medium / uppercase / letter-spacing 0.1em。

### 三、圆角体系升级
- 卡片圆角：16dp → 20dp
- 按钮圆角：12dp → 16dp
- Badge / Chip：维持 100dp 全圆角
- 导航栏顶部圆角：新增 24dp

### 四、间距与布局对齐
- 安全边距：14dp → 20dp
- 列表项最小高度：48dp → 56dp
- 卡片内边距增大：12dp → 20dp
- 区块间距标准化：区块间 24dp，列表项间 12dp

### 五、阴影系统规范化
- 用 DESIGN.md 6 级阴影 Token 替代 `CardDefaults.cardElevation(defaultElevation = 1.dp)`。
- 卡片 shadow 使用暖调 rgba(26,26,46,...) 替代默认冷灰阴影。

### 六、首页组件视觉重构
- **TodayOverviewCard**：主色数字 + 暖金 soft 背景 + 20dp 圆角 + label 标签。
- **ActiveEventCard**：暖金主体渐变 + 20dp 圆角。
- **EventRowWithDelete**：行高增至 56dp，色条色值更新（PENDING→琥珀、ACTIVE→暖金深色、COMPLETED→薄荷绿）。
- **PendingSection 推荐区**：section label 改为全大写 + 加宽字距，Chip 色彩基于新主色。
- **CompletedSection**：淡底色更新为 `#FDF5E8`，label 改为全大写。
- **QuickNameLine**：16dp 圆角输入框 + 暖金焦点色 + 更大内边距。

### 七、日历选择条（可选，视实现难度）
- 将 DayPickerDialog 入口改为顶部 Calendar Strip 组件。
- 黄色背景圆角条，7 天横向排列，选中态白色背景。
- 如果实现难度过高，可在本次仅调整日历图标色值，Strip 组件留待后续迭代。

## Capabilities

### New Capabilities
- `warm-gold-theme`：暖金色设计体系——色彩、排版、圆角、间距、阴影全面对齐 DESIGN.md。

### Modified Capabilities
- `minimal-line-home`：主色切换、圆角增大、间距调整、排版升级、组件视觉重构、标签风格更新。

## Impact

- `ui/theme/Color.kt`：**完全重写**，Coral → Warm Gold 全套 Token。
- `ui/theme/Theme.kt`：**重写** lightColorScheme / darkColorScheme 映射。
- `ui/theme/Type.kt`：**重写**，引入 Outfit/Inter/JetBrains Mono + 9 级 Type Scale。
- `ui/screens/HomeScreen.kt`：**深度修改**——所有组件圆角、间距、色值、排版、阴影全面更新。
- `ui/EventViewModel.kt`：**不变**。
- 数据库 Schema：**不变**。
- 其他 Screen：**暂不变**（使用 M3 组件会自动继承新色板，但布局细节不在本次范围）。
