## 1. Data Layer - 推荐算法

- [x] 1.1 在 `EventDao.kt` 中添加 `getTopEventNamesLast14Days(limit: Int = 5): Flow<List<String>>` 查询方法
- [x] 1.2 在 `EventRepository.kt` 中添加对应的 repository 方法
- [x] 1.3 验证：运行 `./gradlew assembleDebug`，确保 Room schema 生成正确

## 2. ViewModel - 状态管理

- [x] 2.1 在 `EventViewModel.kt` 中添加 `recommendedEventNames: StateFlow<List<String>>` 状态
- [x] 2.2 添加 `completedEventsForSelectedDay: StateFlow<List<EventEntity>>` 分离已完成事件
- [x] 2.3 添加 `pendingEventsForSelectedDay: StateFlow<List<EventEntity>>` 分离未完成事件
- [x] 2.4 添加 `restartEvent(event: EventEntity)` 方法（创建新的 PENDING 事件）
- [x] 2.5 验证：编译通过

## 3. HomeScreen - 布局重构

- [x] 3.1 重构 `HomeScreen.kt`：移除 `FrequentNameLines` 组件
- [x] 3.2 新增 `CompletedSection` 组件（顶部已完成区块）
- [x] 3.3 新增 `PendingSection` 组件（底部待办区块）
- [x] 3.4 新增 `RecommendationItem` 组件（推荐项展示）
- [x] 3.5 新增 `EventRowWithDelete` 组件（带删除按钮的事件行）
- [x] 3.6 验证：运行 `./gradlew assembleDebug`

## 4. 智能合并 - 默认行为

- [x] 4.1 在 `EventViewModel.stopActiveEvent()` 中添加合并检查逻辑
- [x] 4.2 合并条件：事件名称相同 && 间隔 < 5 分钟
- [x] 4.3 验证：创建两个同名事件，确认合并行为正确

## 5. 删除功能 - 简化操作

- [x] 5.1 在 ViewModel 中添加 `deleteEvent(event: EventEntity)` 方法
- [x] 5.2 如果事件处于 IN_PROGRESS 状态，先停止再删除
- [x] 5.3 在 `EventRowWithDelete` 中添加删除按钮（无确认弹窗）
- [x] 5.4 验证：编译通过，手动测试删除功能

## 6. UI 细节调整

- [x] 6.1 调整顶部操作栏：移除"整理记录"按钮（智能合并已默认化）
- [x] 6.2 已完成事件显示"再来一次"按钮
- [x] 6.3 待办事件显示"开始"和"删除"按钮
- [x] 6.4 进行中事件显示"结束"和"删除"按钮
- [x] 6.5 验证：运行 `./gradlew assembleDebug`，视觉检查

## 7. 最终验证

- [x] 7.1 运行 `./gradlew assembleDebug` 确认编译成功
- [x] 7.2 测试完整流程：添加待办 → 开始 → 结束 → 再来一次
- [x] 7.3 测试日期切换后推荐列表是否正确刷新
- [x] 7.4 确认统计页面和导出功能正常工作
