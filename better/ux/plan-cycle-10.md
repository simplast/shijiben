# Plan: ux cycle 10 — F010

## Finding (F010)
`RecordingSheet` 的"保存"按钮**始终可点**，title 为空时 `save()` 返回 false 但用户无任何反馈——按钮看起来可点，点了没反应，不知道为什么。

### 证据（symbol-level 引用）
文件：`app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt`

- `PixelButton(text="保存", onClick={...})`（操作按钮区）—— **无 `enabled` 参数**，默认 `true`
- `RecordingViewModel.save()`：`if (title.isEmpty()) return false` —— title 空时静默失败
- 结果：用户在 title 为空时点"保存"，按钮无视觉反馈，点击无效果，无错误提示

### 现有基础设施
- `PixelButton` 已支持 `enabled: Boolean = true` 参数（`PixelComponents.kt`）
- `enabled=false` 时背景色用 `Disabled` 令牌（UI Cycle 2 已加），点击不响应
- `title` 已作为 state 收集：`val title by viewModel.title.collectAsStateWithLifecycle()`

## 修复
给"保存"按钮加 `enabled = title.isNotBlank()`。

### 改动点（symbol-level）
`RecordingSheet` 操作按钮区的 `PixelButton(text="保存", ...)` 调用，新增参数：

```kotlin
PixelButton(
    text = "保存",
    onClick = { ... },
    modifier = Modifier.weight(1f).height(48.dp),
    backgroundColor = Primary,
    enabled = title.isNotBlank()   // ← 新增
)
```

`isNotBlank()` 与 `save()` 的 `title.trim().isEmpty()` 校验语义一致
（空或纯空白 → 不可保存 → 按钮禁用 + Disabled 灰色视觉反馈）。

## 风险评估
- 一行参数新增，零行为分支变化（save() 内部仍有 title 校验作为兜底）
- `title` 已是 collected state → recomposition 自动驱动按钮 enabled 切换
- 现有测试不涉及 UI 层 → 不受影响
- 无新测试需要（UI 状态绑定，无独立逻辑）

## Gate
`./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest --rerun-tasks && ./gradlew assembleDebug && ./gradlew :app:assembleRelease`

已知 baseline flaky（视为 PASS）：
- `HeatmapYearViewModelTest > yearGrid_todayMarkedExactlyOnce`
- `TimelineViewModelTest > init_carriesOverPastNotStartedEventToToday`
