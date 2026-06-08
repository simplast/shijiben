# minimal-line-home Spec Delta

## MODIFIED Requirements

### Requirement: Not-started events behave as todos

未开始的事件在首页显示为精简待办事项，带状态色条和图标操作按钮。

#### Scenario: Pending item shows minimal controls
- **WHEN** an event has not been started (status is PENDING)
- **THEN** its row shows: left accent bar (4dp, amber #FFB347), event name, a PlayArrow icon button (no text label), and a delete icon button
- **AND** no status text like "待开始" is displayed

#### Scenario: Start pending event via icon
- **WHEN** the user taps the PlayArrow icon on a pending event row
- **THEN** the event status becomes IN_PROGRESS and timing begins from the current time

#### Scenario: Accent bar colors updated for Warm Gold theme
- **WHEN** an event is displayed with a status accent bar
- **THEN** PENDING uses amber (#FFB347)
- **AND** IN_PROGRESS uses warm gold dark (#E5B050)
- **AND** COMPLETED uses mint green (#7DD3A8)

### Requirement: Active event self-timing

An in-progress event SHALL be prominently displayed as a visual "Hero Card" at the top of the homepage, featuring a warm gold gradient background (#F5C469 → #FFB347 → #7DD3A8), breathing animation, real-time timer, and quick action controls.

#### Scenario: Active event shown as Hero Card with warm gold gradient
- **WHEN** an event is in progress (status is IN_PROGRESS)
- **THEN** it is displayed in a dedicated card at the top of the homepage with a warm gold gradient background (#F5C469 → #FFB347 → #7DD3A8) and a breathing animation indicator

### Requirement: 今日概览摘要区块

首页 SHALL 在今日视角下显示一个摘要卡片，展示当日专注时长、完成数和待办数，使用暖金主色和 labelUppercase 排版。

#### Scenario: 今日视角显示概览卡片
- **WHEN** 用户选择今天
- **THEN** 首页上方（ActiveEventCard 之前）显示一个三列概览卡片，分别展示"已专注 X 分钟"、"完成 Y 件"、"待办 Z 件"
- **AND** 数字使用 PrimaryGold (#F5C469) 主色
- **AND** 标签使用 labelUppercase 排版（10sp / Medium / uppercase / letter-spacing 0.1em）
- **AND** 卡片圆角 20dp

### Requirement: 卡片视觉层次

首页的待办卡片和已完成卡片 SHALL 有明确的视觉层次区分，使用暖调阴影系统。

#### Scenario: 待办卡片更突出
- **WHEN** 首页显示待办 Section
- **THEN** 待办 Card 使用白色 surface 背景 + shadow-sm 暖调阴影
- **AND** 卡片圆角 20dp

#### Scenario: 已完成卡片更低调
- **WHEN** 首页显示已完成 Section
- **THEN** 已完成 Card 使用暖灰淡色背景 (#FDF5E8)，无阴影
- **AND** 卡片圆角 20dp

### Requirement: 底部输入栏毛玻璃质感

底部快捷输入栏 SHALL 具有半透明毛玻璃浮动效果，使用暖金色焦点色。

#### Scenario: 浮动输入栏显示毛玻璃效果
- **WHEN** 底部 QuickNameLine 显示
- **THEN** 其背景为半透明 surface 色 + 轻微阴影，产生浮动于内容之上的视觉感受
- **AND** 输入框聚焦时边框色为 PrimaryGold (#F5C469)，焦点光晕为 PrimaryGoldSoft (#FEF3D9)
- **AND** 输入框圆角 16dp

### Requirement: Coral 暖色主题

整个应用 SHALL 使用 DESIGN.md 定义的 Warm Gold 暖色调色板。

#### Scenario: 主色为暖金色
- **WHEN** 任何界面使用 primary 颜色
- **THEN** 显示为暖金色 #F5C469（非珊瑚橙 #FF7A5A）

#### Scenario: 页面背景为暖奶油白
- **WHEN** 任何界面渲染背景色
- **THEN** 使用暖奶油白 #FFF9F0（非 #FCFAF7）

#### Scenario: 深色模式适配
- **WHEN** 系统处于深色模式
- **THEN** Warm Gold 主色保持可辨识，暗色背景使用 #1A1A2E 暖深色调

## ADDED Requirements

### Requirement: 排版体系

首页 SHALL 使用 Outfit（展示标题）+ Inter（正文）+ JetBrains Mono（数字/时间）三字体系统，并定义 9 级 Type Scale。

#### Scenario: 展示级标题使用 Outfit 字体
- **WHEN** 首页渲染 display/hero/h1 级标题
- **THEN** 使用 Outfit 字体族（Bold/Semibold）

#### Scenario: 正文使用 Inter 字体
- **WHEN** 首页渲染正文内容
- **THEN** 使用 Inter 字体族（Regular/Medium）

#### Scenario: 数字和时间使用 JetBrains Mono
- **WHEN** 首页渲染计时数字、统计数据
- **THEN** 使用 JetBrains Mono 字体族

### Requirement: 标签排版规范

首页所有区块标签 SHALL 使用全大写 + 加宽字距的 labelUppercase 排版风格。

#### Scenario: 区块标题使用全大写标签
- **WHEN** 首页显示 Section 标题（如"今日待办"、"已完成"、"建议快速开始"）
- **THEN** 标签使用 10sp / Medium / uppercase / letter-spacing 0.1em 排版
- **AND** 颜色为 WarmGray500

### Requirement: 圆角与间距体系

首页 SHALL 使用 DESIGN.md 定义的圆角和间距体系。

#### Scenario: 卡片圆角
- **WHEN** 首页渲染任何卡片组件
- **THEN** 卡片圆角为 20dp

#### Scenario: 按钮圆角
- **WHEN** 首页渲染任何按钮组件
- **THEN** 按钮圆角为 16dp

#### Scenario: 安全边距
- **WHEN** 首页渲染内容区域
- **THEN** 左右安全边距为 20dp

#### Scenario: 列表项最小高度
- **WHEN** 首页渲染事件列表项
- **THEN** 列表项最小高度为 56dp

### Requirement: 暖调阴影系统

首页 SHALL 使用暖调阴影系统替代默认冷灰阴影。

#### Scenario: 卡片使用暖调阴影
- **WHEN** 首页渲染带阴影的卡片
- **THEN** 阴影使用暖调 rgba(26,26,46,...) 而非默认冷灰色

#### Scenario: 阴影层级
- **WHEN** 首页组件需要不同高度的阴影
- **THEN** 使用 6 级暖调阴影（xs/sm/md/lg/xl/xxl）
