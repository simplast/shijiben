## 1. 修复 quickStartEvent Bug

- [x] 1.1 在 `EventViewModel.quickStartEvent()` 中，先查找当天同名 PENDING 事件（使用 `pendingEventsForSelectedDay.value` 匹配 `name.trim().equals(trimmed, ignoreCase = true)`）
- [x] 1.2 若找到同名 PENDING，调用 `startEvent(existingEvent)` 复用已有记录，而非创建新事件
- [x] 1.3 若未找到同名 PENDING，创建新 IN_PROGRESS 事件时设置 `dayKey = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)`（不再用空字符串）

## 2. 修复推荐去重过滤

- [x] 2.1 在 `HomeScreen.kt` 的 `HomeScreen()` Composable 中提取 `completedNames = completedEvents.map { it.name.trim() }.toSet()`
- [x] 2.2 将 `completedNames` 作为新参数传入 `PendingSection`
- [x] 2.3 在 `PendingSection` 的 `validRecommendations` 过滤中，合并 `existingNames + completedNames` 排除已完成名称

## 3. 清理 minimal-line-home/spec.md 过时 Requirement

- [x] 3.1 从 `openspec/specs/minimal-line-home/spec.md` 删除 "排版体系" Requirement（Outfit/Inter/JetBrains Mono）
- [x] 3.2 删除 "标签排版规范" Requirement（WarmGray500 / 0.1em）
- [x] 3.3 删除 "圆角与间距体系" Requirement（20dp/16dp/56dp）
- [x] 3.4 删除 "暖调阴影系统" Requirement（6 级暖调阴影）
- [x] 3.5 更新 "Minimal line-frame homepage" Requirement 描述，移除 "rounded corners, subtle shadows" 引用
- [x] 3.6 更新 "Pending accent bar" scenario 中 "4dp wide" → "3dp wide"

## 4. 验证

- [x] 4.1 运行 `./gradlew assembleDebug` 确认编译通过
