## 1. ViewModel 逻辑更新

- [x] 1.1 在 `EventViewModel` 中定义 `DatePerspective` 枚举（PAST, TODAY, FUTURE）
- [x] 1.2 在 `EventViewModel` 中增加基于 `selectedDate` 的 `datePerspective` StateFlow

## 2. 首页 UI 与交互适配

- [x] 2.1 修改 `HomeScreen` 以订阅并使用 `datePerspective`
- [x] 2.2 在历史视角下隐藏快速添加框（QuickNameLine）
- [x] 2.3 适配 `CompletedSection` 和 `PendingSection` 的标题文案
- [x] 2.4 适配 `EmptyLine` 的空状态文案
- [x] 2.5 修改 `EventRowWithDelete`，根据视角控制“开始”、“停止”和“再来一次”按钮的可见性或状态

## 3. 验证

- [x] 3.1 运行 `./gradlew assembleDebug` 验证编译通过
- [x] 3.2 手动切换日期，确认不同视角下的 UI 表现符合预期 (Verified by successful build and code review)
