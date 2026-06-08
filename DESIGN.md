# DESIGN.md — 成哥 · 温暖活力个人管理 App

> 项目类型：移动端个人管理 App（子弹笔记 + 待办清单）
> 设计哲学：Warm & Playful · 简洁 · 活力 · 动力
> 参考品牌：截图参考 — 暖黄极简日程管理风格

---

## 1. Visual Theme & Atmosphere（视觉主题与氛围）

- **设计哲学**: 像清晨第一缕阳光洒进房间——温暖、明亮、让人充满动力。打开 App 的瞬间，感受到的是被阳光拥抱的愉悦感
- **视觉基调**: 暖黄极简主义 — 温暖的金黄色 × 奶油白底色，兼具明媚与舒适
- **核心视觉特征**: 温暖明媚 · 圆润亲和 · 生机活力 · 轻盈透气 · 愉悦动效
- **光影与质感倾向**: 极轻微阴影 + 干净表面，拒绝厚重，追求阳光明媚的通透感
- **动效气质**: 柔和弹性（soft spring），每个交互都有愉悦的微反馈，像阳光跳跃

---

## 2. Color Palette & Roles（调色板与角色）

### Primary Colors（主色）

| CSS 变量 | HEX | 用途 |
|-----------|-----|------|
| `--color-primary` | #F5C469 | 主色 — 按钮、关键操作、活跃态、导航高亮 |
| `--color-primary-soft` | #FEF3D9 | 主色柔和态 — 背景填充、选中背景、标签底色 |
| `--color-primary-dark` | #E5B050 | 主色深色 — 按压态、深底上的主色 |
| `--color-primary-deep` | #C9942E | 主色深金 — 强调文字、特殊标签 |

### Brand & Dark（品牌色与深色）

| CSS 变量 | HEX | 用途 |
|-----------|-----|------|
| `--color-brand` | #F5C469 | 品牌标志色 |
| `--color-dark` | #1A1A2E | 深色文字 / 深色背景文字 |
| `--color-dark-soft` | #4A4A5E | 次要深色文字 |

### Accent / Interactive（强调色与交互色）

| CSS 变量 | HEX | 用途 |
|-----------|-----|------|
| `--color-accent-amber` | #FFB347 | 暖琥珀 — 完成态、星星标记、奖励感 |
| `--color-accent-coral` | #FF8A6B | 珊瑚 — 次要强调、进度条辅助色 |
| `--color-accent-mint` | #7DD3A8 | 清新薄荷 — 成功态、正向反馈 |
| `--color-accent-lavender` | #C4B5E0 | 淡紫 — 特殊标签、分类色 |

### Neutral / Gray Scale（中性灰阶系统）

| CSS 变量 | HEX | 用途 |
|-----------|-----|------|
| `--color-gray-50` | #FFF9F0 | 最浅 — 页面背景（奶油白） |
| `--color-gray-100` | #FDF5E8 | 基础灰 — 卡片背景、区块底色 |
| `--color-gray-200` | #F5EEDF | 浅灰 — 分割线、边框 |
| `--color-gray-300` | #E8DED0 | 中浅灰 — 禁用态、placeholder |
| `--color-gray-400` | #C4B8A8 | 中灰 — 次要文字 |
| `--color-gray-500` | #9A8E7E | 中深灰 — 辅助文字、时间戳 |
| `--color-gray-600` | #6B6258 | 深灰 — 正文 |
| `--color-gray-700` | #4A4A5E | 更深 — 标题 |
| `--color-gray-800` | #1A1A2E | 最深 — 主要标题 |

### Surface & Borders（表面与边框色）

| CSS 变量 | HEX / RGBA | 用途 |
|-----------|-------------|------|
| `--color-surface` | #FFFFFF | 卡片、弹窗表面 |
| `--color-surface-elevated` | #FFFFFF | 提升层表面 |
| `--color-border` | rgba(26, 26, 46, 0.06) | 默认边框 |
| `--color-border-hover` | rgba(26, 26, 46, 0.12) | hover / 聚焦边框 |
| `--color-border-light` | rgba(26, 26, 46, 0.04) | 极轻分割线 |

### Semantic Colors（语义色）

| CSS 变量 | HEX | 用途 |
|-----------|-----|------|
| `--color-success` | #7DD3A8 | 成功 — 任务完成 |
| `--color-warning` | #FFB347 | 警告 — 即将过期 |
| `--color-error` | #FF8A6B | 错误 — 删除、异常 |
| `--color-info` | #7DD3A8 | 信息提示 |

### Shadow Colors（阴影色）

| CSS 变量 | RGBA | 用途 |
|-----------|------|------|
| `--shadow-color-xs` | rgba(26, 26, 46, 0.03) | 极微阴影 |
| `--shadow-color-sm` | rgba(26, 26, 46, 0.05) | 小阴影 |
| `--shadow-color-md` | rgba(26, 26, 46, 0.08) | 中阴影 |
| `--shadow-color-lg` | rgba(26, 26, 46, 0.10) | 大阴影 |
| `--shadow-color-xl` | rgba(26, 26, 46, 0.14) | 特大阴影 |

---

## 3. Typography Rules（排版规则）

### Font Family

```css
--font-primary: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
--font-display: 'Outfit', 'Inter', -apple-system, sans-serif;
--font-mono: 'JetBrains Mono', 'SF Mono', monospace;
```

> `Inter` 负责正文阅读舒适度；`Outfit` 用于展示级标题，圆润几何感匹配温暖活力调性

### Type Scale

| Token | Size | Weight | Line Height | Letter Spacing | 使用场景 |
|-------|------|--------|-------------|----------------|----------|
| `--text-display` | 40px / 2.5rem | 700 (Bold) | 1.15 | -0.02em | 主屏大标题、空状态标题 |
| `--text-hero` | 32px / 2rem | 700 (Bold) | 1.2 | -0.015em | 页面标题、弹窗标题 |
| `--text-h1` | 26px / 1.625rem | 650 (Semibold) | 1.25 | -0.01em | 区块标题 |
| `--text-h2` | 22px / 1.375rem | 600 (Semibold) | 1.3 | 0 | 卡片标题 |
| `--text-h3` | 18px / 1.125rem | 600 (Semibold) | 1.35 | 0 | 组件标题、列表项标题 |
| `--text-body` | 16px / 1rem | 400 (Regular) | 1.55 | 0 | 正文、待办内容 |
| `--text-body-sm` | 14px / 0.875rem | 400 (Regular) | 1.5 | 0 | 辅助文字、时间戳 |
| `--text-caption` | 12px / 0.75rem | 450 (Medium) | 1.4 | 0.01em | 标签、脚注 |
| `--text-nano` | 10px / 0.625rem | 500 (Medium) | 1.3 | 0.02em | 徽章数字、小标记 |

### Label Typography（标签排版 — 截图核心特征）

```css
.label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--color-gray-500);
  line-height: 1.3;
}
```

> 所有小标签采用 **全大写 + 加宽字距（0.1em）** 风格，这是截图中最具辨识度的排版特征

### 设计哲学

- 展示级标题用 **Outfit Bold + 紧字距** 营造活力冲击感
- 正文用 **Inter Regular + 宽松行高** 确保长列表阅读舒适
- 数字和日期用 **mono 字体** 增强可扫描性（如待办数量、剩余天数）
- 标签统一用 **全大写 + 加宽字距**，精致感与截图一致
- 温暖感体现在适度宽松的 `line-height`——呼吸感 = 放松感

---

## 4. Component Stylings（组件样式）

### Buttons（按钮）

| 变体 | 背景 | 文字色 | 边框 | 圆角 | Padding | Hover | Active |
|------|------|--------|------|------|---------|-------|--------|
| Primary | `--color-primary` | #FFFFFF | none | 16px | 14px 28px | 亮度 94% | `--color-primary-dark` |
| Secondary | transparent | `--color-dark` | `--color-border` 1.5px | 16px | 14px 28px | border `--color-primary-soft` | bg `var(--color-primary-soft)` |
| Ghost | transparent | `--color-gray-600` | none | 16px | 14px 20px | bg `var(--color-gray-100)` | bg `var(--color-gray-200)` |
| Danger | #FFFFFF | `--color-error` | `--color-error` 1.5px | 16px | 14px 28px | bg `rgba(255, 138, 107, 0.06)` | bg `rgba(255, 138, 107, 0.12)` |
| Icon-only | transparent | `--color-gray-500` | none | 50% | 10px | bg `var(--color-gray-100)` | bg `var(--color-gray-200)` |
| Floating | `--color-primary` | #FFFFFF | none | 50% | 16px | 亮度 94% | `--color-primary-dark` |

```css
/* Primary Button */
.btn-primary {
  background: var(--color-primary);
  color: #FFFFFF;
  border: none;
  border-radius: 16px;
  padding: 14px 28px;
  font-size: 16px;
  font-weight: 600;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.btn-primary:hover { filter: brightness(0.94); transform: scale(1.02); }
.btn-primary:active { background: var(--color-primary-dark); transform: scale(0.98); }

/* Floating Action Button */
.btn-floating {
  background: var(--color-primary);
  color: #FFFFFF;
  border: none;
  border-radius: 50%;
  padding: 16px;
  box-shadow: 0 4px 16px rgba(245, 196, 105, 0.4);
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.btn-floating:hover { transform: scale(1.08); }
.btn-floating:active { transform: scale(0.95); }
```

### Cards（卡片）

```css
.card {
  background: var(--color-surface);
  border: none;
  border-radius: 20px;
  padding: 20px;
  box-shadow:
    0 2px 8px var(--shadow-color-xs),
    0 4px 16px var(--shadow-color-sm);
  transition: all 0.2s ease;
}
.card--elevated {
  box-shadow:
    0 4px 12px var(--shadow-color-sm),
    0 8px 24px var(--shadow-color-md);
}
.card--interactive:hover {
  box-shadow:
    0 6px 16px var(--shadow-color-md),
    0 12px 32px var(--shadow-color-lg);
  transform: translateY(-2px);
}
.card--yellow {
  background: var(--color-primary);
  color: #FFFFFF;
  box-shadow: 0 4px 16px rgba(245, 196, 105, 0.35);
}
```

### Inputs（输入框）

```css
.input {
  background: var(--color-gray-100);
  border: 1.5px solid transparent;
  border-radius: 16px;
  padding: 16px 18px;
  font-size: 16px;
  color: var(--color-gray-800);
  transition: all 0.2s ease;
}
.input::placeholder { color: var(--color-gray-400); }
.input:focus {
  background: var(--color-surface);
  border-color: var(--color-primary);
  box-shadow: 0 0 0 4px var(--color-primary-soft);
  outline: none;
}
.input--error {
  border-color: var(--color-error);
  box-shadow: 0 0 0 4px rgba(255, 138, 107, 0.1);
}
```

### Navigation（底部导航栏）

```css
.tab-bar {
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
  padding: 8px 0 calc(8px + env(safe-area-inset-bottom));
  display: flex;
  justify-content: space-around;
  border-radius: 24px 24px 0 0;
  box-shadow: 0 -2px 12px var(--shadow-color-sm);
}
.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: var(--color-gray-400);
  font-size: 11px;
  font-weight: 500;
  transition: all 0.2s ease;
  padding: 8px 16px;
  border-radius: 16px;
  min-width: 56px;
}
.tab-item--active {
  color: var(--color-gray-800);
  background: var(--color-primary-soft);
}
.tab-item--center {
  background: var(--color-primary);
  color: #FFFFFF;
  border-radius: 20px;
  padding: 10px 20px;
  margin-top: -16px;
  box-shadow: 0 4px 16px rgba(245, 196, 105, 0.4);
}
.tab-item--center:hover {
  transform: scale(1.08);
  filter: brightness(0.94);
}
.tab-item--center:active {
  transform: scale(0.95);
}
```

> 底部导航采用 **5 tab 布局**，中间 tab 为突出的黄色胶囊按钮，其余 tab 活跃态为黄色 soft 背景

### Calendar Strip（日历选择条）

```css
.calendar-strip {
  background: var(--color-primary);
  border-radius: 20px;
  padding: 16px 20px;
  box-shadow: 0 4px 16px rgba(245, 196, 105, 0.35);
}
.calendar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.calendar-header-label {
  font-size: 10px;
  font-weight: 500;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.8);
}
.calendar-days {
  display: flex;
  justify-content: space-between;
  gap: 4px;
}
.calendar-day {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border-radius: 16px;
  color: rgba(255, 255, 255, 0.7);
  transition: all 0.2s ease;
}
.calendar-day--number {
  font-size: 16px;
  font-weight: 600;
}
.calendar-day--label {
  font-size: 10px;
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.calendar-day--active {
  background: #FFFFFF;
  color: var(--color-gray-800);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
```

### Progress Ring（环形进度条）

```css
.progress-ring {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.progress-ring__svg {
  transform: rotate(-90deg);
}
.progress-ring__track {
  stroke: var(--color-gray-200);
  stroke-width: 4;
  fill: none;
}
.progress-ring__progress {
  stroke: var(--color-primary);
  stroke-width: 4;
  fill: none;
  stroke-linecap: round;
  transition: stroke-dashoffset 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.progress-ring__label {
  position: absolute;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-gray-800);
}
.progress-ring__sublabel {
  position: absolute;
  bottom: 20%;
  font-size: 10px;
  color: var(--color-gray-400);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
```

### Event Card（事件卡片）

```css
.event-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: var(--color-surface);
  border-radius: 20px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px var(--shadow-color-xs);
  transition: all 0.2s ease;
}
.event-card__time {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-gray-400);
  min-width: 48px;
  text-align: right;
}
.event-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.event-card__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-gray-800);
}
.event-card__time-range {
  font-size: 12px;
  color: var(--color-gray-400);
}
.event-card__avatars {
  display: flex;
  align-items: center;
}
.event-card__avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: 2px solid var(--color-surface);
  margin-left: -8px;
}
.event-card__avatar:first-child {
  margin-left: 0;
}
.event-card__avatar-more {
  font-size: 10px;
  color: var(--color-gray-400);
  margin-left: 8px;
}
```

### Badges / Tags（徽章与标签）

```css
.badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.3;
}
.badge--priority-high { background: #FFF0E8; color: #E85D3A; }
.badge--priority-medium { background: #FFF8E8; color: #E8A53A; }
.badge--priority-low { background: #E8F8F0; color: #5DB87A; }
.badge--category { background: var(--color-gray-100); color: var(--color-gray-600); }
.badge--count {
  background: var(--color-primary);
  color: #FFFFFF;
  min-width: 20px;
  height: 20px;
  justify-content: center;
  padding: 0 6px;
}
```

### Modals / Dialogs（弹窗）

```css
.modal-overlay {
  background: rgba(26, 26, 46, 0.35);
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
}
.modal-content {
  background: var(--color-surface);
  border-radius: 24px;
  padding: 24px;
  box-shadow:
    0 8px 32px var(--shadow-color-lg),
    0 2px 8px var(--shadow-color-md);
  animation: modalEnter 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes modalEnter {
  from { opacity: 0; transform: scale(0.92) translateY(12px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
```

---

## 5. Layout Principles（布局原则）

### Spacing System

- **基数**: 4px
- **尺度表**: 2 / 4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64 / 80

```css
--space-0\.5: 2px;
--space-1: 4px;
--space-2: 8px;
--space-3: 12px;
--space-4: 16px;
--space-5: 20px;
--space-6: 24px;
--space-8: 32px;
--space-10: 40px;
--space-12: 48px;
--space-16: 64px;
--space-20: 80px;
```

### Grid System

- **列数**: 4 列（手机竖屏）/ 6 列（手机横屏 & 平板）
- **间距**: 16px
- **安全边距**: 20px（左右各）

### Container

- `max-width`: 480px（移动端 App 标准宽度）
- `padding`: 0 20px

### Section Spacing

- 列表项间距: 12px
- 区块间距: 24px
- 大区块间距: 40px
- 页面顶部安全区: `safe-area-inset-top` + 20px
- 页面底部安全区: `safe-area-inset-bottom` + 8px（适配 Home Indicator）

### 留白哲学

"阳光需要空间才能照进来"——每个 UI 区块之间保留充足间距，避免拥挤感。子弹笔记的核心理念是"清晰"，视觉上的留白 = 心理上的放松。列表项高度不低于 56px，确保手指触摸舒适且视觉透气。

---

## 6. Depth & Elevation（深度与层级）

### Shadow System

```css
--shadow-xs: 0 2px 4px rgba(26, 26, 46, 0.03);
--shadow-sm: 0 2px 8px rgba(26, 26, 46, 0.05), 0 1px 2px rgba(26, 26, 46, 0.03);
--shadow-md: 0 4px 12px rgba(26, 26, 46, 0.06), 0 2px 4px rgba(26, 26, 46, 0.04);
--shadow-lg: 0 8px 24px rgba(26, 26, 46, 0.08), 0 4px 8px rgba(26, 26, 46, 0.04);
--shadow-xl: 0 12px 40px rgba(26, 26, 46, 0.10), 0 6px 16px rgba(26, 26, 46, 0.05);
--shadow-2xl: 0 20px 60px rgba(26, 26, 46, 0.12), 0 8px 24px rgba(26, 26, 46, 0.06);
```

### Surface Layers

| Layer | 使用场景 | 背景 | 阴影 |
|-------|----------|------|------|
| background | 页面主背景 | `--color-gray-50` | none |
| surface | 卡片、列表项 | `--color-surface` | `--shadow-sm` |
| elevated | FAB、浮动工具栏 | `--color-surface-elevated` | `--shadow-lg` |
| overlay | 弹窗、底部 Sheet | `--color-surface` | `--shadow-xl` |
| modal | 全屏模态 | `--color-surface` | `--shadow-2xl` |

### Z-index Scale

```css
--z-base: 1;
--z-dropdown: 100;
--z-sticky: 200;
--z-fab: 300;        /* Floating Action Button */
--z-overlay: 400;    /* 半透明遮罩 */
--z-modal: 500;      /* 弹窗本体 */
--z-toast: 600;      /* Toast 提示 */
--z-tooltip: 700;    /* 工具提示 */
```

### Backdrop Effects

```css
--blur-sm: blur(4px);
--blur-md: blur(12px);
--blur-lg: blur(24px);
--blur-xl: blur(40px);
```

> 毛玻璃用于 Tab Bar 和 Overlay，保持底层内容隐约可见，增强层级感

---

## 7. Do's and Don'ts（设计规范与禁忌）

### Do's ✅

1. **使用大圆角传递温暖** — 卡片 20px、按钮 16px、头像 50%，圆润 = 亲和力
2. **标签全大写 + 加宽字距** — 这是截图最核心的排版特征，必须保留
3. **用弹簧动画做微反馈** — 按钮点击用 `cubic-bezier(0.34, 1.56, 0.64, 1)`，拒绝线性
4. **列表项保留充足高度** — 最小 56px，避免误触
5. **主色仅用于关键操作** — 一个页面只突出 1-2 个 Primary 动作
6. **完成态给予视觉奖励** — 打卡完成用闪烁动画 + 琥珀色星星
7. **中间导航 tab 突出设计** — 5 tab 布局时中间 tab 用黄色胶囊高亮
8. **空状态要有温度** — 用插画 + 鼓励文案，不要裸显示"暂无数据"
9. **日历选择条用黄色背景** — 日期格子选中态白色背景，非选中透明+白色文字
10. **环形进度条用于任务完成率** — 进度色用主色，背景轨用浅灰

### Don'ts ❌

1. **不要使用冷灰色** — 所有灰阶必须偏暖调，拒绝 `#EEEEEE` 等冷灰
2. **不要使用尖锐直角** — 所有交互元素圆角不低于 12px
3. **不要过度使用主色** — `--color-primary` 只用于可交互元素和关键区块，不要大面积平铺
4. **不要用纯黑色文字** — 用深暖灰 `#1A1A2E` 替代 `#000000`
5. **不要忽略触摸反馈** — 每个可点击元素必须有 visual feedback（hover/active）
6. **不要使用弹入动画** — 内容出现用优雅 fade+slide，拒绝突兀
7. **不要在列表上使用大阴影** — 列表项用 `--shadow-sm`，大阴影留给弹窗
8. **不要过度使用 emoji/图标** — 表达温暖靠色彩和圆角，不是靠花哨 icon
9. **不要忽略日历选择器的选中态对比** — 选中/非选中必须有明确视觉区分
10. **不要让进度条太细** — 环形进度条 stroke-width 不低于 4px

---

## 8. Responsive Behavior（响应式行为）

### Breakpoints

| 断点 | 宽度 | 目标设备 |
|------|------|----------|
| mobile-sm | 0 - 374px | 小屏 iPhone SE |
| mobile | 375px - 428px | 标准手机 |
| tablet | 429px - 768px | 平板竖屏 |
| desktop | 769px+ | 平板横屏 / 桌面（PWA） |

### Touch Targets

- **最小触摸目标**: 44px × 44px（Apple HIG 标准）
- **列表项最小高度**: 56px
- **按钮最小宽度**: 48px
- **图标按钮**: 44px × 44px（含 padding）

### 折叠策略

| 断点 | 行为 |
|------|------|
| mobile-sm | 4 列网格 → 2 列，安全边距收缩到 16px |
| mobile | 标准 4 列，边距 20px |
| tablet | 6 列网格，支持分栏布局（侧栏 + 主内容） |
| desktop | 限制内容宽度 480px + 两侧留白，或自适应宽屏模式 |

### Font Scaling

- 手机端: 使用标准 Type Scale（如上表）
- 平板端: `--text-display` 放大到 48px，`--text-body` 保持 16px
- 辅助功能: 支持系统 Dynamic Type 自动放大（使用 `rem` 单位）
- 最小字体: 10px（`--text-nano`），低于此值不可读

---

## 9. Agent Prompt Guide（AI 代理提示指南）

### Quick Reference

```
项目: 温暖活力个人管理 App（子弹笔记 + 待办）
主色: #F5C469 (warm gold) | 暖背景: #FFF9F0
字体: Inter (body) + Outfit (display)
圆角体系: 卡片 20px, 按钮 16px, badge 100px, 导航 24px
间距基数: 4px
标签风格: 全大写 + letter-spacing: 0.1em
动效: cubic-bezier(0.34, 1.56, 0.64, 1) spring curve
阴影: 暖调 rgba(26, 26, 46, ...) 替代冷灰
```

### Component Prompts（组件 Prompt 示例）

> 以下 Prompt 可直接复制给 Cursor / Claude Code 使用：

```
Prompt 1 — 今日待办列表卡片:
"使用 DESIGN.md 规范，创建一个『今日待办』列表卡片组件。包含：左侧复选框（圆角12px, 主色勾选）、中间任务标题（text-body）、右侧优先级徽章（badge--priority-high/medium/low）。已完成任务用 gray-300 划线。圆角20px卡片，shadow-sm。用 spring 动效。"

Prompt 2 — 底部导航栏:
"基于 DESIGN.md 的 tab-bar 样式，创建5个 tab 的底部导航：今日、子弹笔记、习惯、统计、设置。活跃态用 primary-soft 背景 + 深色文字，中间 tab 为突出的黄色胶囊按钮（带阴影上浮效果）。支持 safe-area-inset-bottom。"

Prompt 3 — 日历选择条:
"使用 calendar-strip 规范，创建一周的日历选择条。黄色背景圆角矩形，顶部有 THIS WEEK / NEXT WEEK 标签（全大写+加宽字距），下方7个日期格子。选中日期白色背景+深色文字，非选中透明+白色半透明文字。"

Prompt 4 — 新建子弹笔记条目弹窗:
"使用 modal-overlay 和 modal-content 规范，创建一个新建子弹笔记条目的底部 Sheet。顶部有取消/保存按钮，输入框用 input 样式（圆角16px, gray-100背景），类别选择用 badge--category 标签组。modalEnter 动画入场。"

Prompt 5 — 打卡完成庆祝动画:
"创建一个打卡完成的庆祝状态组件：主色圆形背景，中间白色对勾，周围散落琥珀色星星粒子动画。用 spring curve 做缩放 bounce。"

Prompt 6 — 空状态页面:
"创建一个温暖的空状态：中央插画占位（圆角20px），display 级标题'今天还没有任务'，body 级辅助文字'点右下角的 + 开始记录吧'，底部一个 Primary 按钮。用暖灰 50 背景。"

Prompt 7 — 环形进度统计:
"创建一个环形进度组件，显示今日完成率。stroke 用 gray-200，progress 用 primary，中心显示百分比数字（Outfit Bold）。支持 spring 动画填入。下方显示 'completed' 全大写标签。"

Prompt 8 — 事件时间线卡片:
"使用 event-card 规范，创建时间线形式的事件卡片。左侧显示时间（10 AM），中间是白色圆角卡片包含事件标题（Rental App）、时间范围（9:30 AM - 10:30 AM）、右侧是参与人头像组。卡片shadow-xs，圆角20px。"

Prompt 9 — 任务进度卡片组:
"创建任务进度卡片布局：左侧是一个大的黄色卡片（card--yellow）显示 Walk App 进度，包含任务描述、剩余天数标签（全大写）、参与人头像、环形进度条。右侧是白色小卡片网格显示其他任务。整体圆角20px。"
```

### Iteration Guide（AI 生成 UI 时的迭代建议）

1. **先结构后视觉** — 第一轮聚焦布局和间距对齐，第二轮再打磨色彩和阴影
2. **色彩检查** — 确保所有交互元素都使用了设计系统的颜色变量，没有硬编码颜色
3. **圆角一致性** — 全局搜索 `border-radius`，确保只有 12px / 16px / 20px / 24px / 100px 五种值
4. **触摸友好审核** — 检查所有可点击元素高度是否 ≥ 44px
5. **动画不要过度** — 每个页面最多 2-3 个主动画元素，其余用微过渡
6. **暖灰替换检查** — 确认没有使用 `#EEEEEE` 或 `#F5F5F5` 等冷灰色，全部替换为暖调灰
7. **字体分层** — 确认标题使用 Outfit，正文使用 Inter，数字使用 JetBrains Mono
8. **毛玻璃适度** — backdrop-filter 仅用于 Tab Bar、Overlay、Sheet 拖拽柄，不要用于卡片
9. **空状态覆盖** — 确保每个列表屏都有空状态设计（插画 + 文案 + CTA）
10. **动效曲线统一** — 所有交互动效使用 `cubic-bezier(0.34, 1.56, 0.64, 1)`，所有出现动效使用 `ease-out`
11. **标签风格检查** — 所有小标签必须全大写 + letter-spacing: 0.1em，这是核心视觉特征
12. **导航突出检查** — 5 tab 布局时确认中间 tab 有黄色胶囊高亮效果
