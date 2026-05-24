# Shijiben — Agent Guide

**事记本**：按天记录事件的 Android 应用（中文 UI）。

## 项目背景

受 **子弹笔记（Bullet Journal）** 与苏联生物学家 **柳比歇夫** 传记 **《奇特的一生》** 启发：前者强调用简短符号快速记下「做了什么」，按日回看；后者以数十年坚持记录每件事的起止与耗时，形成对自己一生的量化审视。

本 app 取二者交集——**低摩擦记录 + 时间颗粒度**：

- 像子弹笔记一样：选一天、写下一行事件名、尽量快（快捷名称、底部 sheet 即记即走）。
- 像柳比歇夫时间账一样：保留开始/结束时刻与分钟级耗时，支持「进行中」与完成后补全，便于日后统计「时间花在何处」。

产品气质偏克制、日记感，而非重型项目管理；新功能应强化「记一笔」与「按日回顾」，避免堆复杂工作流。

## Stack

- Kotlin · Jetpack Compose · Material 3
- Room → SQLite（`shijiben.db`，表 `events`）
- Navigation Compose · ViewModel + StateFlow
- minSdk 26 · JDK 17

## Layout

```
app/src/main/java/com/doer/shijiben/
├── data/          EventEntity, EventDao, EventRepository, AppDatabase, TimeFormats
├── ui/            EventViewModel, theme/, screens/, navigation/
├── MainActivity.kt
└── ShijibenApplication.kt   # 提供 repository 单例
```


| 屏幕    | 文件                                | 说明                                        |
| ----- | --------------------------------- | ----------------------------------------- |
| 首页列表  | `ui/screens/HomeScreen.kt`        | 日统计、事件列表、底部日期条、FAB 打开编辑                   |
| 新建/编辑 | `ui/screens/EventEditorScreen.kt` | `ModalBottomSheet` 内 `EventEditorContent` |
| 选日    | `ui/screens/DayPickerDialog.kt`   | 共用日期选择                                    |


编辑入口在 `HomeScreen` 的 Bottom Sheet，**不是**独立导航页；`onAddEvent` / `onOpenEvent` 回调目前为空占位。

## Data model

`EventEntity`: `id`, `name`, `startTimeMillis`, `endTimeMillis`, `dayKey`（由开始时间推导）, `status`（`IN_PROGRESS` | `COMPLETED`）。

- 按 `dayKey` 查当日事件；快捷名称来自 `observeRecentDistinctNames()`（去重、按最近使用、最多 10 条）。
- **数据库版本变更**：递增 `AppDatabase` 的 `version`，新增 `Migration`，**禁止** `fallbackToDestructiveMigration()`，以免升级清数据。

## Build

```bash
./gradlew assembleDebug      # 调试 APK
./gradlew assembleRelease    # 正式 APK
./gradlew bundleRelease      # Play AAB
```

输出：`app/build/outputs/apk/{debug,release}/`

## Conventions

- UI 文案与用户可见字符串使用中文。
- 改动尽量小，匹配现有 Compose / Material 风格，避免无关重构。
- 新增事件编辑区保持紧凑（`eventId == null` 时 `isNew` 分支）。
- 列表项单行：左标题（省略号），右「开始—结束」、耗时（分）、箭头。
- `FocusRequester` 必须配合 `Modifier.focusRequester()`，在事件回调里 `requestFocus()`，勿在 composition 中直接请求。

## 常见注意点

- `EventEditorScreen`：`AssistChip` 快捷名仅新建时显示；保存走 `viewModel.upsert`。
- 进行中事件列表右侧耗时显示「进行中」，非实时累计分钟。
- 未配置 release 签名时，`assembleRelease` 可能需本地 keystore 配置。

