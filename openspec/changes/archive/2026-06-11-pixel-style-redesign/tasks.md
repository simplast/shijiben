## 1. 字体集成

- [x] 1.1 下载 Press Start 2P .ttf 字体文件并放入 `app/src/main/res/font/pressstart2p.ttf`
- [x] 1.2 修改 `Type.kt` 中 `PixelDisplay` 使用 `FontFamily(Font(R.font.pressstart2p))`
- [x] 1.3 修改 `Type.kt` 中 `PixelLabel` 使用 `FontFamily(Font(R.font.pressstart2p))`

## 2. 像素图标重写

- [x] 2.1 创建 `PixelIcons.kt` 中的 8x8 像素网格绘制工具函数
- [x] 2.2 重写 `PixelPlayIcon` 为 8x8 像素网格
- [x] 2.3 重写 `PixelStopIcon` 为 8x8 像素网格
- [x] 2.4 重写 `PixelCloseIcon` 为 8x8 像素网格
- [x] 2.5 重写 `PixelCheckIcon` 为 8x8 像素网格
- [x] 2.6 重写 `PixelAddIcon` 为 8x8 像素网格
- [x] 2.7 重写 `PixelMoreIcon` 为 8x8 像素网格
- [x] 2.8 重写 `PixelPauseIcon` 为 8x8 像素网格
- [x] 2.9 重写 `PixelRefreshIcon` 为 8x8 像素网格
- [x] 2.10 重写 `PixelCalendarIcon` 为 8x8 像素网格
- [x] 2.11 重写 `PixelStatsIcon` 为 8x8 像素网格

## 3. 边框简化

- [x] 3.1 修改 `PixelComponents.kt` 中 `PixelIconButton` 增加 `borderless: Boolean = false` 参数
- [x] 3.2 修改 `PixelIconButton` 当 `borderless=true` 时不应用 `tertiaryDoubleBorder`

## 4. 底部输入栏统一

- [x] 4.1 修改 `HomeScreen.kt` 中 `QuickNameLine` 的 "+" 按钮为 40dp 方形无边框
- [x] 4.2 修改 `HomeScreen.kt` 中 `QuickNameLine` 的提交按钮为 40dp 无边框
- [x] 4.3 修改 `HomeScreen.kt` 中 `QuickNameLine` 的输入框高度为 40dp

## 5. Pending 列表按钮调整

- [x] 5.1 修改 `HomeScreen.kt` 中 `PendingEventRow` 的 Play 按钮为无边框
- [x] 5.2 修改 `HomeScreen.kt` 中 `PendingEventRow` 的 Delete 按钮为无边框
- [x] 5.3 调整 Play 和 Delete 按钮间距为 8dp

## 6. 验证

- [x] 6.1 运行 `./gradlew assembleDebug` 确认编译通过
- [x] 6.2 检查所有图标在不同尺寸下显示正常
- [x] 6.3 检查 Press Start 2P 字体在英文数字上正确渲染
