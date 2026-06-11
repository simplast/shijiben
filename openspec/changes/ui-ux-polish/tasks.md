## 1. 无障碍标签

- [x] 1.1 修改 `PixelComponents.kt` 中 `PixelIconButton` 增加 `contentDescription: String? = null` 参数
- [x] 1.2 修改 `PixelIconButton` 使用 `Modifier.semantics { contentDescription = this@PixelIconButton.contentDescription }`
- [x] 1.3 更新 `HomeScreen.kt` 中所有 PixelIconButton 调用添加 contentDescription
- [x] 1.4 更新 `EventEditorScreen.kt` 中所有 PixelIconButton 调用添加 contentDescription
- [x] 1.5 更新 `DayPickerDialog.kt` 中所有 PixelIconButton 调用添加 contentDescription

## 2. 触摸目标尺寸

- [x] 2.1 检查并调整 `HomeScreen.kt` 中所有按钮最小尺寸为 44dp
- [x] 2.2 检查并调整 `EventEditorScreen.kt` 中所有按钮最小尺寸为 44dp
- [x] 2.3 检查并调整 `DayPickerDialog.kt` 中所有按钮最小尺寸为 44dp
- [x] 2.4 检查并调整 `MonthlyReviewScreen.kt` 中所有按钮最小尺寸为 44dp
- [x] 2.5 检查并调整 `WeeklyReviewScreen.kt` 中所有按钮最小尺寸为 44dp

## 3. 间距优化

- [x] 3.1 调整 `HomeScreen.kt` 中 CompletedEventRow 按钮间距为 8dp
- [x] 3.2 调整 `HomeScreen.kt` 中 PendingEventRow 按钮间距为 8dp
- [x] 3.3 调整 `HomeScreen.kt` 中 QuickNameLine 按钮间距为 8dp

## 4. 动画统一

- [x] 4.1 修改 `PixelComponents.kt` 中 `PixelButton` 使用 spring 动画 (dampingRatio=0.6f, stiffness=300f)
- [x] 4.2 修改 `PixelComponents.kt` 中 `PixelIconButton` 使用 spring 动画

## 5. 验证

- [x] 5.1 运行 `./gradlew assembleDebug` 确认编译通过
- [x] 5.2 检查所有按钮尺寸 ≥ 44dp
- [x] 5.3 检查所有图标按钮有 contentDescription
