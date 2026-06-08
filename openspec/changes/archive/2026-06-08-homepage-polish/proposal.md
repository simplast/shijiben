## Why

当前首页功能完备但视觉质感不够高级（"不够高级"），存在以下具体问题：

1. **待办 Item 控制冗余**：每条 PENDING item 同时显示状态文字"待开始"、文字按钮"开始"和删除按钮，信息密度过高，视觉嘈杂。
2. **快速开始行为错误**：建议快速开始的 pill 点击后是"添加到待办"而非"直接开始计时"，违背"快速"的本意。
3. **同名待办重复**：用户多次添加同名待办（如两次"阅读"）会创建多条重复记录，缺乏去重逻辑。
4. **状态颜色区分弱**：PENDING/IN_PROGRESS/COMPLETED 三项仅通过 3dp 细线颜色区分，视觉辨识度不足。
5. **缺少今日概览**：首页没有展示"今天已专注 X 分钟、完成 Y 件事"的摘要区块，缺乏掌控感。
6. **卡片层次扁平 + 色彩体系未对齐 DESIGN.md**：当前使用 Indigo/Violet 冷色调，DESIGN.md 明确要求 Coral/暖灰的 Warm & Playful 调性。

## What Changes

### 一、对齐 DESIGN.md 色彩体系（基础层）
- 重写 `ui/theme/Color.kt`：主色改为 `#FF7A5A`（Coral），背景改为 `#FCFAF7`，文字改为 `#2D2A24`，引入 `#FFB347`（琥珀）、`#4CAF78`（成功绿）等语义色。
- 更新 `ui/theme/Theme.kt`：lightColorScheme 和 darkColorScheme 全部映射到新色板，确保 Material 3 组件自动继承新色彩。

### 二、待办 Item 精简 + 状态色重构
- 移除 PENDING item 的"待开始"状态文字（所在 Section 标题已表明身份）。
- "开始"按钮改为纯图标（PlayArrow IconButton），减少视觉噪音。
- 左侧色条从 3dp 加粗到 4dp，颜色按状态区分：
  - PENDING → `#FFB347` 琥珀（暗示"等待行动"）
  - IN_PROGRESS → `#FF7A5A` Coral（主色，活跃中）
  - COMPLETED → `#4CAF78` 翠绿（已完成）

### 三、快速开始 → 直接开始计时
- PendingSection 中"建议快速开始"pill 的点击行为从 `onAdd`（quickAddEvent）改为 `onQuickStart`（quickStartEvent）。
- ViewModel 已有 `quickStartEvent(name)`，该函数自动停止当前活动事件并创建新的 IN_PROGRESS 事件。
- Pill 视觉微调：增加闪电 ⚡ 暗示，圆角改为 100px 全圆角（符合 DESIGN.md badge 规范）。

### 四、同名待办去重
- `quickAddEvent(name)` 在插入前检查当日是否已存在同名 PENDING 事件，存在则静默跳过。
- 去重基于 `trim() + equals(ignoreCase=true)`。

### 五、今日概览 Hero 区块
- 在 ActiveEventCard 上方新增 `TodayOverview` 区块，展示：
  - 今日已专注总时长（大号数字）
  - 已完成事件数
  - 待办剩余数
- 仅在 TODAY 视角显示，过去/未来日期隐藏。
- 设计遵循 DESIGN.md：暖白色卡片 + 微阴影 + 圆角 16px，珊瑚色数字强调。

### 六、卡片层次优化 + 底部输入栏毛玻璃
- 待办 Card 使用更突出的 surface（白色 + shadow-sm），已完成 Card 更低调（surfaceVariant 淡色）。
- QuickNameLine 底部浮动栏增加半透明模糊背景效果，贴合 DESIGN.md 的"轻量毛玻璃提升质感"原则。

## Capabilities

### New Capabilities
- `homepage-polish`：首页精致化改造——色彩重设、待办精简、快速开始修复、去重、概览、卡片层次。

### Modified Capabilities
- `minimal-line-home`：新增今日概览区块、待办 Item 精简、状态色区分、卡片层次、毛玻璃输入栏等行为变更。
- `algo-recommendation`：建议快速开始 pill 的行为从"添加待办"改为"直接开始计时"。

## Impact

- `ui/theme/Color.kt`：**完全重写**，对齐 DESIGN.md 暖色调色板。
- `ui/theme/Theme.kt`：**重写** lightColorScheme / darkColorScheme，映射新色板。
- `ui/screens/HomeScreen.kt`：**深度修改**——新增 TodayOverview、重构 EventRowWithDelete、修改 quick-start pill 行为、调整卡片样式、改 QuickNameLine。
- `ui/EventViewModel.kt`：**小幅修改**——quickAddEvent 增加同名去重逻辑。
- 数据库 Schema：**不变**（无需 Migration）。
- 其他 Screen：**不变**（Weekly/Monthly 使用 M3 组件，自动继承新色彩）。
