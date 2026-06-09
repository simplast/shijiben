## MODIFIED Requirements

### Requirement: 今日概览摘要区块

首页 SHALL 在今日视角下显示一个非对称仪表盘概览卡片，使用 8-bit 像素扁平风格。

#### Scenario: 今日视角显示概览卡片
- **WHEN** 用户选择今天
- **THEN** 首页上方显示非对称概览卡片，左侧为大号白色时间数字（PixelDisplay 32sp Black），右侧为两个垂直堆叠的彩色徽章
- **AND** 卡片背景使用 PixelSkyBlue + 3dp PixelDeepNavy 边框
- **AND** 右上角徽章使用 PixelCoralRed 背景显示已完成数
- **AND** 右下角徽章使用 PixelLavender 背景显示待办数
- **AND** 卡片使用直角矩形（0dp 圆角）

#### Scenario: 非今日视角隐藏概览卡片
- **WHEN** 用户选择过去或未来日期
- **THEN** 概览卡片不显示

#### Scenario: 概览数字实时更新
- **WHEN** 用户完成或添加事件
- **THEN** 概览卡片中的数字同步更新

### Requirement: 卡片视觉层次

首页的待办卡片和已完成卡片 SHALL 使用 8-bit 边框系统和直角矩形区分层级。

#### Scenario: 待办卡片更突出
- **WHEN** 首页显示待办 Section
- **THEN** 待办 Card 使用白色背景 + 2dp PixelAmberOrange 边框 + 左侧 5dp 金色边缘条
- **AND** 卡片为直角矩形（0dp 圆角）
- **AND** 无阴影

#### Scenario: 已完成卡片更低调
- **WHEN** 首页显示已完成 Section
- **THEN** 已完成 Card 使用 PixelMintLight 背景 + 2dp PixelTeal 边框
- **AND** 列表项高度为 48dp（紧凑模式）
- **AND** 事件名字号为 bodySmall (12sp)
- **AND** 左侧色条 3dp 宽，颜色按事件顺序轮换多彩色
- **AND** 卡片为直角矩形（0dp 圆角）

### Requirement: Active event self-timing

进行中的事件 SHALL 以 8-bit 像素风 Hero Card 展示。

#### Scenario: Active event 显示为像素风 Hero Card
- **WHEN** 事件正在进行中 (status is IN_PROGRESS)
- **THEN** 显示为 PixelHotPink 纯色背景 + 3dp PixelDeepNavy 边框的直角矩形卡片
- **AND** 左侧有 PixelStarYellow 方块闪烁指示灯
- **AND** 卡片内文字为白色

#### Scenario: 结束按钮样式
- **WHEN** Hero Card 显示结束按钮
- **THEN** 按钮使用 PixelTeal 背景 + 2dp PixelDeepNavy 边框 + 深色文字
- **AND** 按钮为直角矩形（0dp 圆角）

### Requirement: 首页区块间距节奏

首页各区块之间 SHALL 使用差异化间距制造视觉段落感。

#### Scenario: 概览到进行中卡片间距
- **WHEN** 首页同时显示概览卡片和 ActiveEventCard
- **THEN** 两者之间的间距为 16dp

#### Scenario: 进行中到已完成区块间距
- **WHEN** 首页同时显示 ActiveEventCard 和已完成区块
- **THEN** 两者之间的间距为 12dp

#### Scenario: 已完成到待办区块间距
- **WHEN** 首页同时显示已完成区块和待办区块
- **THEN** 两者之间的间距为 20dp

### Requirement: 底部输入栏毛玻璃质感

底部快捷输入栏 SHALL 采用 8-bit 像素扁平风格。

#### Scenario: 浮动输入栏像素风样式
- **WHEN** 底部 QuickNameLine 显示
- **THEN** 使用白色背景 + 2dp PixelLavender 边框 + 直角矩形（0dp 圆角）
- **AND** "+"图标使用 PixelHotPink 着色
- **AND** 提交按钮使用 PixelAmberOrange 背景方块 + 白色播放图标
- **AND** 无阴影，无毛玻璃效果

### Requirement: Not-started events behave as todos

未开始的事件在首页显示为像素风待办事项。

#### Scenario: Pending item 像素风样式
- **WHEN** 事件状态为 PENDING
- **THEN** 事件行显示：左侧 3dp PixelAmberOrange 色条、事件名称、方块形播放按钮（28dp，多彩背景 + 白色三角形）、删除按钮
- **AND** 播放按钮使用直角矩形（0dp 圆角）
- **AND** 每个播放按钮使用不同的色彩背景（从 PixelCoralRed / PixelLavender / PixelTeal / PixelSkyBlue / PixelHotPink 中轮换）
