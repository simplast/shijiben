## 1. 多彩色板与像素风 Token

- [x] 1.1 在 `ui/theme/Color.kt` 新增 9 个 8-bit 色彩 Token：PixelSkyBlue、PixelHotPink、PixelCoralRed、PixelLavender、PixelTeal、PixelAmberOrange、PixelMintLight、PixelStarYellow、PixelDeepNavy
- [x] 1.2 新增边框 Token：PixelBorder (= PixelDeepNavy)、PixelBorderLight (= PixelDeepNavy 20% alpha)
- [x] 1.3 定义全局 `PixelShape = RoundedCornerShape(0.dp)` 常量

## 2. 像素风排版

- [x] 2.1 在 `ui/theme/Type.kt` 将 `DisplayFont` 改为 `FontFamily.Monospace`
- [x] 2.2 新增 `PixelDisplay` TextStyle（Monospace / 32sp / Black / letterSpacing 0.05em）
- [x] 2.3 新增 `PixelLabel` TextStyle（Monospace / 10sp / Bold / letterSpacing 0.12em / uppercase）
- [x] 2.4 确认中文正文（BodyFont）保持 FontFamily.Default

## 3. 像素风形状与边框系统

- [x] 3.1 在 HomeScreen.kt 中将所有 `RoundedCornerShape(N.dp)` 替换为 `PixelShape`（0dp 直角）
- [x] 3.2 移除所有 `CardDefaults.cardElevation` 调用（全局去阴影）
- [x] 3.3 为概览卡、进行中卡添加 3dp PixelBorder；为已完成卡、待办卡添加 2dp 彩色边框
- [x] 3.4 按钮、Badge、Input 等组件统一使用 PixelShape + 1-2dp 彩色边框

## 4. 非对称像素概览仪表盘

- [x] 4.1 重写 `TodayOverviewCard`：PixelSkyBlue 背景 + 3dp navy 边框 + 直角矩形
- [x] 4.2 左侧使用 PixelDisplay(32sp) 白色大号时间数字 + PixelLabel "已专注"
- [x] 4.3 右侧堆叠两个彩色徽章：PixelCoralRed（已完成）+ PixelLavender（待办），各带 2dp navy 边框 + 白色文字

## 5. 进行中卡片像素化

- [x] 5.1 ActiveEventCard 背景改为 PixelHotPink 纯色 + 3dp navy 边框 + 直角矩形
- [x] 5.2 呼吸点从 CircleShape 改为 8dp 方块（PixelShape），颜色改为 PixelStarYellow
- [x] 5.3 动画从 tween 平滑过渡改为 keyframes 帧式瞬切（800ms 周期，400ms on / 400ms off）
- [x] 5.4 "结束"按钮改为 PixelTeal 背景 + 2dp navy 边框 + 深色文字 + 直角矩形
- [x] 5.5 计时数字升级为 PixelDisplay 样式（白色 32sp Monospace Black）

## 6. 已完成区块像素化 + 紧凑化

- [x] 6.1 已完成 Card 改为 PixelMintLight 背景 + 2dp PixelTeal 边框 + 直角矩形
- [x] 6.2 事件行高降至 48dp，事件名字号降至 bodySmall (12sp)
- [x] 6.3 左侧色条改为 3dp 宽，颜色按事件顺序轮换 [PixelTeal, PixelSkyBlue, PixelLavender, PixelCoralRed, PixelStarYellow]
- [x] 6.4 时间 meta 文字改为 WarmGray500 + PixelLabel 风格
- [x] 6.5 "再来一次"和删除按钮改为直角方块 + 像素风着色

## 7. 待办区块像素化 + 多彩播放按钮

- [x] 7.1 待办 Card 改为白色背景 + 2dp PixelAmberOrange 边框 + 左侧 5dp 金色边缘条 + 直角矩形
- [x] 7.2 播放按钮从圆形改为 28dp 方块（PixelShape），颜色按顺序轮换 [PixelCoralRed, PixelLavender, PixelTeal, PixelSkyBlue, PixelHotPink]
- [x] 7.3 播放图标改为白色 PlayArrow（tint = Color.White）
- [x] 7.4 为待办行添加 press 状态背景色变化（PixelAmberOrange 15% alpha）

## 8. Section 彩色短线标题

- [x] 8.1 新增 `PixelSectionHeader` Composable：12dp × 3dp 彩色短线 + PixelLabel 大写文字
- [x] 8.2 已完成标题使用 PixelTeal 短线
- [x] 8.3 待办标题使用 PixelAmberOrange 短线
- [x] 8.4 推荐标题使用 PixelCoralRed 短线

## 9. 快速推荐标签色彩化

- [x] 9.1 推荐标签改为直角方块（PixelShape），每个标签使用不同色彩底色（从 PixelCoralRed / PixelTeal / PixelAmberOrange / PixelSkyBlue 中选择）
- [x] 9.2 标签文字改为白色 + PixelLabel 风格

## 10. 顶栏精简

- [x] 10.1 LineTopBar 从 4 个 IconButton 收敛为 2 个：左侧日历（PixelTeal tint）+ 右侧 MoreVert（PixelDeepNavy tint）
- [x] 10.2 添加 DropdownMenu（2dp navy 边框 + 直角 + 白色背景），包含"数据统计"、"导出 CSV"、"导出 JSON"
- [x] 10.3 菜单项点击触发对应功能，点击外部自动收起

## 11. 底部输入栏像素化

- [x] 11.1 QuickNameLine 改为白色背景 + 2dp PixelLavender 边框 + 直角矩形，移除阴影
- [x] 11.2 "+"图标改为 PixelHotPink 着色
- [x] 11.3 提交按钮改为 PixelAmberOrange 背景方块 + 白色播放图标 + 直角矩形

## 12. 区块间距节奏

- [x] 12.1 概览卡到 ActiveEventCard 间距设为 16dp
- [x] 12.2 ActiveEventCard 到已完成区块间距设为 12dp
- [x] 12.3 已完成区块到待办区块间距设为 20dp

## 13. 验证

- [x] 13.1 运行 `./gradlew assembleDebug` 确认编译通过
- [x] 13.2 在模拟器或真机验证首页 8-bit 多彩效果：概览仪表盘、进行中卡片、已完成降权、待办多彩按钮、Section 短线、顶栏精简、底部输入栏
- [x] 13.3 验证切换日期视角（今天/过去/未来）时概览卡片显示/隐藏逻辑正确
