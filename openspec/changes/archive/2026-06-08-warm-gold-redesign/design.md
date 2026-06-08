## Context

DESIGN.md 已完成从珊瑚橙到暖金色的视觉身份切换。本次改动是将首页代码全面对齐新规范的落地实现——色彩、排版、圆角、间距、阴影五个维度的系统性重构，不涉及新功能。

## Goals / Non-Goals

**Goals:**
- 色彩 Token 全面从 Coral (#FF7A5A) 切换为 Warm Gold (#F5C469)
- 建立完整的自定义排版体系（Outfit + Inter + JetBrains Mono）
- 圆角、间距、阴影对齐 DESIGN.md 规范
- 首页所有组件视觉重构（卡片、列表项、Chip、输入框、概览卡、Hero Card）
- 标签排版统一为全大写 + letter-spacing 0.1em

**Non-Goals:**
- 不改造首页以外的页面（Weekly/Monthly/Editor 仅继承新色板，布局不改）
- 不新增日历选择条组件（Calendar Strip），保持现有 DayPickerDialog
- 不修改数据库 Schema 或 Room Migration
- 不修改 EventViewModel 业务逻辑
- 不引入新的导航路由或页面
- 不引入付费字体文件（使用 Google Fonts 开源方案或系统回退）

## Decisions

### Decision 1: 主色体系切换

**Choice:** 将 `PrimaryCoral` (#FF7A5A) 替换为 `PrimaryGold` (#F5C469)，同时更新 soft/dark/deep 变体。

**Token mapping:**

| 旧 Token | 旧 HEX | 新 Token | 新 HEX | 用途 |
|----------|--------|----------|--------|------|
| `PrimaryCoral` | `#FF7A5A` | `PrimaryGold` | `#F5C469` | 主色 |
| `PrimaryCoralSoft` | `#FFE8E0` | `PrimaryGoldSoft` | `#FEF3D9` | 柔和背景 |
| `PrimaryCoralDark` | `#E85D3A` | `PrimaryGoldDark` | `#E5B050` | 按压态 |
| — | — | `PrimaryGoldDeep` | `#C9942E` | 深金强调 |
| `WarmGray50` | `#FCFAF7` | `WarmGray50` | `#FFF9F0` | 页面背景 |
| `WarmGray100` | `#F8F5F0` | `WarmGray100` | `#FDF5E8` | 卡片淡底 |
| `StatusActive` | `#FF7A5A` | `StatusActive` | `#E5B050` | 进行中色条 |
| `SuccessGreen` | `#4CAF78` | `AccentMint` | `#7DD3A8` | 完成态 |
| `ErrorCoral` | `#E85D3A` | `AccentCoral` | `#FF8A6B` | 错误/删除 |
| `AccentAmber` | `#FFB347` | `AccentAmber` | `#FFB347` | 待办/星星（不变） |
| `AccentTeal` | `#4DC9B8` | `AccentMint` | `#7DD3A8` | 统一为薄荷绿 |

Hero Card 渐变：`#F5C469 → #FFB347 → #7DD3A8`（暖金 → 琥珀 → 薄荷，替代珊瑚→琥珀→青色）。

**Rationale:** DESIGN.md 是唯一真理源。暖金色比珊瑚橙更明亮、更温暖，在浅色背景上更突出，符合"阳光"的视觉隐喻。

### Decision 2: 排版体系

**Choice:** 引入三字体系统：

| 字体 | 用途 | 来源 |
|------|------|------|
| `Outfit` | 展示标题（display/hero/h1） | Google Fonts (OFL) |
| `Inter` | 正文（body/caption/nano） | Google Fonts (OFL) |
| `JetBrains Mono` | 数字、时间、代码 | Google Fonts (OFL) |

在 `Type.kt` 中定义 9 级 Type Scale，并新增 `labelUppercase` TextStyle：
```kotlin
val labelUppercase = TextStyle(
    fontSize = 10.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 0.1.em,
    lineHeight = 13.sp,
)
```

**Rationale:** DESIGN.md §3 明确要求 Outfit 的圆润几何感匹配温暖活力调性。全大写标签是参考截图最核心的排版特征，必须保留。

**Alternative considered:** 仅使用系统默认字体 + 修改 fontWeight/size。Rejected——无法达到 DESIGN.md 要求的视觉辨识度。

### Decision 3: 圆角与间距体系

**Choice:**
- 卡片圆角：16dp → 20dp
- 按钮圆角：12dp → 16dp
- 安全边距：14dp → 20dp
- 列表项最小高度：48dp → 56dp
- 卡片内边距：12dp/6dp → 20dp

**Rationale:** DESIGN.md §5 要求"阳光需要空间才能照进来"——更宽松的间距和更大的圆角传递温暖与亲和感。56dp 列表项既满足 44dp 最小触摸目标，又有 12dp 呼吸空间。

### Decision 4: 阴影系统

**Choice:** 定义 6 级阴影常量，使用暖调 rgba(26,26,46,...)：

```kotlin
object WarmShadow {
    val xs = 0.5.dp   // rgba(26,26,46,0.03)
    val sm = 1.dp     // rgba(26,26,46,0.05)
    val md = 2.dp     // rgba(26,26,46,0.08)
    val lg = 4.dp     // rgba(26,26,46,0.10)
    val xl = 8.dp     // rgba(26,26,46,0.14)
    val xxl = 16.dp   // rgba(26,26,46,0.18)
}
```

卡片默认使用 `sm`，弹窗使用 `xl`，替代 `CardDefaults.cardElevation(defaultElevation = 1.dp)` 的冷灰阴影。

**Rationale:** DESIGN.md §6 定义了暖调阴影系统。默认 M3 阴影偏冷灰，与新暖色调不协调。

### Decision 5: 日历选择条暂不实现

**Choice:** 本次保持 DayPickerDialog 不变，仅更新日历图标色值为新主色。

**Rationale:** Calendar Strip 组件涉及布局重构（顶部横向日历替代弹窗），工作量显著且需要新的状态管理。本次聚焦色彩+排版+间距的系统性对齐，Strip 组件可独立提案。

### Decision 6: 深色模式

**Choice:** darkColorScheme 主色使用 `#F5C469`（保持原色，暖金在深色上天然高对比度），暗色背景调整为 `#1A1A2E`（偏冷深蓝灰，与暖金形成互补），surface 调整为 `#262340`。

**Rationale:** 暖金色在深色背景上天然醒目，无需降低亮度。`#1A1A2E` 偏冷的深底色让暖金更加突出，形成温暖/冷静的对比张力。

## Risks / Trade-offs

- **[Risk]**: 引入 3 个字体文件会增加 APK 体积（预估 +300KB~1MB）。
  - **Mitigation**: 使用 Compose 的 `GoogleFont` provider 按需下载，或仅打包 Regular/Bold 两个 weight。如果体积敏感，可退回到系统字体 + 自定义 weight/size 的简化方案。

- **[Risk]**: 间距/圆角/行高增大后，首页信息密度下降，可能需要更多滚动。
  - **Mitigation**: 首页是"今日概览 + 待办 + 已完成"的结构，信息量有限，20dp 边距和 56dp 行高不会导致溢出。实际验证后再微调。

- **[Risk]**: 其他页面自动继承新色板后可能出现视觉不一致（如 Weekly/Monthly 使用旧圆角/间距）。
  - **Mitigation**: 本次仅保证首页完全对齐。其他页面因使用 M3 组件会自动获得新色彩，布局细节在后续迭代中逐步对齐。

- **[Risk]**: 暖金色 (#F5C469) 作为按钮背景时白色文字对比度可能不足。
  - **Mitigation**: 检查 WCAG AA 对比度。暖金 (#F5C469) + 白色 (#FFFFFF) 对比度约 1.9:1，不达标。调整方案：按钮文字改用 `#1A1A2E` 深色，或使用 `PrimaryGoldDark` (#E5B050) 作为按钮背景提升对比度。
