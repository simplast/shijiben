## Context

DESIGN.md 定义了 Warm & Playful 设计哲学（暖调极简主义），当前 UI 使用 Indigo/Violet 冷色调且许多细节不符合规范。本次改造是色彩体系 + 交互细节的全面对齐。

## Goals / Non-Goals

**Goals:**
- 将全部色彩 Token 从 Indigo 冷调切换为 Coral 暖调（DESIGN.md §2）
- 精简待办 Item：去状态文字、按钮改图标、色条加粗 + 语义着色
- "建议快速开始" pill 点击 → 直接开始计时（而非添加待办）
- `quickAddEvent` 同名去重
- 新增今日概览区块（今日视角专属）
- 卡片层次区分 + QuickNameLine 毛玻璃效果

**Non-Goals:**
- 不改造顶部日期栏（明确排除）
- 不修改数据库 Schema 或 Room Migration
- 不改 EventEditorScreen / WeeklyReviewScreen / MonthlyReviewScreen
- 不增加新的导航路由
- 不引入新的第三方依赖

## Decisions

### Decision 1: 色彩体系完全对齐 DESIGN.md

**Choice:** 重写 `Color.kt` 和 `Theme.kt`，全面采用 DESIGN.md 的暖色调色板。

**Design tokens mapping:**

| DESIGN.md Token | Kotlin Name | HEX | 用途 |
|---|---|---|---|
| `--color-primary` | `PrimaryCoral` | `#FF7A5A` | 主色 |
| `--color-primary-soft` | `PrimaryCoralSoft` | `#FFE8E0` | 柔和背景 |
| `--color-primary-dark` | `PrimaryCoralDark` | `#E85D3A` | 按压态 |
| `--color-dark` | `DarkWarm` | `#2D2A24` | 主文字 |
| `--color-dark-soft` | `DarkWarmSoft` | `#4A453E` | 次要文字 |
| `--color-accent-amber` | `AccentAmber` | `#FFB347` | 完成/星星 |
| `--color-accent-teal` | `AccentTeal` | `#4DC9B8` | 链接/次要操作 |
| `--color-accent-lavender` | `AccentLavender` | `#B8A9E8` | 特殊标签 |
| `--color-gray-50` | `GrayWarm50` | `#FCFAF7` | 页面背景 |
| `--color-gray-100` | `GrayWarm100` | `#F8F5F0` | 卡片背景 |
| `--color-success` | `SuccessGreen` | `#4CAF78` | 完成态 |
| `--color-warning` | `WarningAmber` | `#FFB347` | 警告 |
| `--color-error` | `ErrorCoral` | `#E85D3A` | 错误 |

ActiveEventCard 渐变色：`#FF7A5A` → `#FFB347` → `#4DC9B8`（暖珊瑚 → 琥珀 → 清新，替代当前冷蓝紫渐变）。

**Rationale:** DESIGN.md 作为项目设计唯一真理源，必须全面对齐。暖色调传递"温暖、掌控感"，符合子弹笔记的产品哲学。

### Decision 2: 待办 Item 极简化

**Choice:** PENDING item 仅保留：左侧 4dp 琥珀色条 + 事件名称 + PlayArrow 图标按钮 + 删除图标按钮。移除"待开始"状态文字、"开始"文字标签。

**Rationale:**
- Section 标题"今日待办"已表明身份，"待开始"是冗余信息。
- 图标按钮节省空间，视觉更干净。
- 整行 click 进编辑、PlayArrow 启动计时、Delete 删除——三态分离清晰。

### Decision 3: 快速开始 Pill 直接启动

**Choice:** PendingSection 新增 `onQuickStart: (String) -> Unit` 回调，pill 点击调用它而非 `onAdd`。HomeScreen 将 `onQuickStart` 映射到 `viewModel.quickStartEvent(name)`。

ViewModel 的 `quickStartEvent` 逻辑：停止当前 active → 创建新 IN_PROGRESS 事件（已有，无需改动）。

**Rationale:** "快速开始"的本意是一键进入专注状态，而非两跳操作。降低"开始记录时间"的摩擦。

### Decision 4: 去重策略 —— 静默跳过

**Choice:** `quickAddEvent` 在 upsert 前扫描当日 PENDING 列表，若 `sameName(trim + ignoreCase)` 已存在则直接 return（不创建、不提示）。

**Rationale:** 子弹笔记的哲学是"一个任务一条线"，同名重复通常是无意之举。静默跳过不打断用户流程，如果用户确实需要两段独立"阅读"时间，可以修改名称区分（如"阅读《三体》"、"阅读《原则》"）。

**Alternative considered:** Toast 提示"已存在"。Rejected——静默更符合"不打扰"的设计哲学。

### Decision 5: 今日概览区块

**Choice:** 在 ActiveEventCard 上方（仅 TODAY 视角）新增 `TodayOverviewCard`，三列布局：已专注时长（大号数字）、已完成数、待办数。

数据来源：`viewModel.completedEventsForSelectedDay` 计算总分钟数 + `viewModel.pendingEventsForSelectedDay` 计数。

**Visual spec:**
- 卡片：`color-surface` 白底 + `shadow-sm` + `RoundedCornerShape(16.dp)`
- 数字用 Coral 主色 + Outfit 风格大号字体（h1 级别）
- 标签用 `caption` 级暖灰色文字
- padding: 16dp 水平，20dp 垂直

**Rationale:** 类似 Apple Health 的摘要卡片给予用户"掌控一切"的心理锚点。仅今天显示避免历史/未来日期的信息噪音。

### Decision 6: 卡片层次 + 毛玻璃

**Choice:**
- PENDING Card：白色 surface + `shadow-sm`
- COMPLETED Card：`grayWarm100` 淡背景 + 无阴影（更低视觉比重）
- QuickNameLine：半透明背景 + `backdrop-filter` 等价实现（Compose 中通过 alpha + surface 颜色模拟）

**Rationale:** 待办是"行动召唤"，应该突出；已完成是"历史记录"，应该退后。毛玻璃增强底部输入栏的浮动感。

## Risks / Trade-offs

- **[Risk]**: 色彩体系全面切换可能导致某些深色模式下对比度不足。
  - **Mitigation**: 在 Theme.kt 中单独为 darkColorScheme 设置适配色值（如 Coral 的 dark 变体、暖灰 dark 系列），参考 DESIGN.md 中暗色模式规范。

- **[Risk]**: today 之外的其他屏幕（Weekly/Monthly）使用 MaterialTheme.colorScheme 自动继承新色，可能出现意料外的视觉变化。
  - **Mitigation**: 新色板与 M3 组件语义一致（primary=主操作色、surface=卡片底、onSurface=文字），理论上无破坏性变化。但仍需 `assembleDebug` 后在各屏幕验证。

- **[Risk]**: 毛玻璃效果在低端 Android 设备可能有性能问题。
  - **Mitigation**: 使用 alpha + 半透明色模拟，不引入真正的 RenderScript blur。如果设备不支持，自动降级到纯色背景。
