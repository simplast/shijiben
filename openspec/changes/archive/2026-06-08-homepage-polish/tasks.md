## 1. 色彩体系重设（DESIGN.md 对齐）

- [x] 1.1 重写 `ui/theme/Color.kt`：引入 Coral 暖色系全部 Token，保留 ActiveGradient 新渐变，废弃旧的 Indigo/Violet Token
- [x] 1.2 重写 `ui/theme/Theme.kt`：lightColorScheme 和 darkColorScheme 全部映射新色板

## 2. 待办 Item 精简 + 状态颜色重构

- [x] 2.1 重构 `EventRowWithDelete`：PENDING item 移除"待开始"状态文字，将"开始"文字按钮改为纯图标 IconButton
- [x] 2.2 左侧色条加粗至 4dp（原 3dp），颜色按状态区分：琥珀(PENDING) / Coral(IN_PROGRESS) / 翠绿(COMPLETED)
- [x] 2.3 已完成 item 的"再来一次"按钮也精简为图标按钮

## 3. 快速开始 → 直接计时

- [x] 3.1 `PendingSection` 新增 `onQuickStart: (String) -> Unit` 回调参数
- [x] 3.2 Pill 点击从 `onAdd(name)` 改为 `onQuickStart(name)`
- [x] 3.3 `HomeScreen` 中 `onQuickStart` 映射到 `viewModel.quickStartEvent(name)`
- [x] 3.4 Pill 视觉微调：增加全圆角 100px + 边框，视觉上暗示"点击即执行"

## 4. 同名待办去重

- [x] 4.1 `EventViewModel.quickAddEvent(name)` 中增加同名去重判断：扫描当日 PENDING 列表，trim+ignoreCase 匹配则静默 return

## 5. 今日概览 Hero 区块

- [x] 5.1 新增 `TodayOverviewCard` Composable：三列展示"专注时长 / 已完成 / 待办"
- [x] 5.2 仅 TODAY 视角在 ActiveEventCard 上方渲染，其余视角隐藏
- [x] 5.3 数据从 ViewModel 已有 StateFlow 派生（completedEventsForSelectedDay + pendingEventsForSelectedDay）

## 6. 卡片层次 + 底部输入栏质感

- [x] 6.1 PendingSection Card：白色 surface + shadow-sm 提升视觉权重
- [x] 6.2 CompletedSection Card：GrayWarm100 淡底 + 无阴影，降低视觉权重
- [x] 6.3 QuickNameLine：半透明 surface 背景 + 轻微阴影，模拟毛玻璃浮动感
- [x] 6.4 调整 LazyColumn bottom padding 适配新的卡片高度（目标 90dp → 100dp）

## 7. 构建验证

- [x] 7.1 `./gradlew assembleDebug` 确保零编译错误
- [ ] 7.2 在 Today / Past / Future 三个视角手动验证 UI 正确性
- [ ] 7.3 验证深色模式下的色彩对比度
