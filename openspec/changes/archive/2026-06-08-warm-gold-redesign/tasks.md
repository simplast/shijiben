## 1. 色彩体系切换（Coral → Warm Gold）

- [ ] 1.1 重写 `ui/theme/Color.kt`：PrimaryCoral 全套 → PrimaryGold 全套，灰阶更新，状态色更新
- [ ] 1.2 重写 `ui/theme/Theme.kt`：lightColorScheme / darkColorScheme 映射新色板
- [ ] 1.3 Hero Card 渐变色更新为暖金主体渐变 (#F5C469 → #FFB347 → #7DD3A8)
- [ ] 1.4 处理按钮文字对比度问题：暖金按钮使用深色文字 (#1A1A2E)

## 2. 排版体系建立

- [ ] 2.1 在 `ui/theme/Type.kt` 中引入 Outfit/Inter/JetBrains Mono 字体族
- [ ] 2.2 定义 9 级 Type Scale（display → nano），映射到 M3 Typography 属性
- [ ] 2.3 新增 `labelUppercase` TextStyle（10sp / Medium / uppercase / letter-spacing 0.1em）
- [ ] 2.4 评估字体引入方式（GoogleFont provider vs 本地打包），控制 APK 体积

## 3. 圆角与间距体系对齐

- [ ] 3.1 卡片圆角全局从 16dp 改为 20dp
- [ ] 3.2 按钮圆角从 12dp 改为 16dp
- [ ] 3.3 安全边距从 14dp 改为 20dp
- [ ] 3.4 EventRowWithDelete 行高从 48dp 增至 56dp
- [ ] 3.5 卡片内边距增大至 20dp

## 4. 阴影系统规范化

- [ ] 4.1 定义 `WarmShadow` 常量对象（6 级阴影，暖调 rgba）
- [ ] 4.2 卡片 shadow-sm 替代 `CardDefaults.cardElevation(defaultElevation = 1.dp)`
- [ ] 4.3 ActiveEventCard 使用 shadow-md
- [ ] 4.4 QuickNameLine 使用 shadow-sm

## 5. 首页组件视觉重构

- [ ] 5.1 TodayOverviewCard：主色数字 + PrimaryGoldSoft 背景 + 20dp 圆角 + labelUppercase 标签
- [ ] 5.2 ActiveEventCard：暖金渐变 + 20dp 圆角 + 呼吸动画保持
- [ ] 5.3 EventRowWithDelete：56dp 行高 + 色条色值更新（PENDING=琥珀, ACTIVE=暖金深, COMPLETED=薄荷）
- [ ] 5.4 PendingSection：section label 改为 labelUppercase，Chip 底色改为 PrimaryGoldSoft
- [ ] 5.5 CompletedSection：淡底色改为 #FDF5E8，label 改为 labelUppercase
- [ ] 5.6 QuickNameLine：16dp 圆角输入框 + PrimaryGold 焦点色 + 20dp 内边距
- [ ] 5.7 LineTopBar：图标色值更新为 WarmGray600，日期文字更新为 Outfit 字体

## 6. 构建验证

- [ ] 6.1 `./gradlew assembleDebug` 确保零编译错误
- [ ] 6.2 在 Today / Past / Future 三个视角验证 UI 正确性
- [ ] 6.3 验证深色模式下色彩对比度
- [ ] 6.4 验证按钮文字对比度（暖金背景上的文字可读性）
