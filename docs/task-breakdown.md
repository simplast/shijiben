## Development Task Breakdown — 事记本 V1（灵魂闭环）

> 前置：阅读 AGENT.md 了解项目上下文。参考 changes.md 了解文件范围。

---

### T-01 项目脚手架与依赖配置
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** 无

**Content:** 初始化 Flutter 项目，配置依赖，搭建目录结构。

**Implementation guidance:**
- 运行 `flutter create . --org com.shijiben --platforms=android,web` 初始化项目
- 在 `pubspec.yaml` 中添加依赖：drift、sqlite3_flutter_libs、flutter_riverpod、go_router、drift_dev (dev)、build_runner (dev)、flutter_lints (dev)
- 创建目录结构：`lib/core/{theme,widgets,utils,router}`、`lib/data/{database,tables,daos,models}`、`lib/features/{timeline,recording,tags,notes}`
- 在 `lib/main.dart` 中初始化 `ProviderScope` 和数据库连接

**Verification:** `flutter run -d chrome` 能启动空白应用，无报错；`flutter pub get` 成功

**Files involved:** `pubspec.yaml`, `lib/main.dart`

---

### T-02 数据层：表定义与数据库
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-01

**Content:** 定义 Event/Note/Tag 三张 drift 表，创建数据库连接。

**Implementation guidance:**
- 在 `lib/data/models/event_status.dart` 定义枚举：`notStarted`、`inProgress`、`completed`，使用 drift 的 `int` 转换
- 在 `lib/data/tables/events_table.dart` 定义 `Events` 表，字段参照设计 spec：id (autoIncrement)、title (text)、startTime (dateTime)、endTime (dateTimeNullable)、status (int, 默认 notStarted)、tagId (intNullable)、note (textNullable)、createdAt、updatedAt
- 在 `lib/data/tables/notes_table.dart` 定义 `Notes` 表：id、content、timestamp、createdAt、updatedAt
- 在 `lib/data/tables/tags_table.dart` 定义 `Tags` 表：id、name、color (int)、sortOrder (int, 默认0)、createdAt、updatedAt
- 在 `lib/data/database/app_database.dart` 创建 `AppDatabase` 类继承 `GeneratedDatabase`，包含三张表
- 运行 `dart run build_runner build` 生成代码

**Verification:** 代码生成无报错；数据库能在 Web 和 Android 上打开

**Files involved:** `lib/data/models/event_status.dart`, `lib/data/tables/*.dart`, `lib/data/database/app_database.dart`

---

### T-03 数据层：DAO
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-02

**Content:** 为三张表实现 DAO，提供 CRUD 和业务查询。

**Implementation guidance:**
- `lib/data/daos/event_dao.dart`：
  - `getEventsByDate(DateTime date)` — 查询某天的事件（按 startTime 排序）
  - `getOngoingEvent()` — 查询 status=inProgress 的事件
  - `getNotStartedBeforeDate(DateTime date)` — 查询某天之前未开始的事件（用于顺延）
  - `insertEvent(...)`、`updateEvent(...)`、`deleteEvent(id)`
  - `startEvent(id)` — 状态改 inProgress，endTime 置 null
  - `completeEvent(id, endTime)` — 状态改 completed，写入 endTime
  - `carryOverNotStarted()` — 把之前日期的 notStarted 事件 startTime 改为今天相同时刻
- `lib/data/daos/note_dao.dart`：`getNotesByDate(date)`、`insertNote(...)`、`updateNote(...)`、`deleteNote(id)`
- `lib/data/daos/tag_dao.dart`：`getAllTags()`、`insertTag(...)`、`updateTag(...)`、`deleteTag(id)`
- 所有 DAO 使用 drift 的 `@DriftAccessor` 注解，返回 `Stream` 以支持响应式查询

**Verification:** 单元测试或手动调用 DAO 方法能正确读写数据

**Files involved:** `lib/data/daos/*.dart`

---

### T-04 核心主题：8-bit 柔和复古色板
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-01

**Content:** 定义 8-bit 柔和复古色板和像素风格组件。

**Implementation guidance:**
- 在 `lib/core/theme/app_colors.dart` 定义色板：
  - 预定义 12-16 个柔和复古色（饱和度适中，如：暗红 #B85450、靛蓝 #5060B8、森绿 #50A868、琥珀 #D8A050、紫罗兰 #8050A0、青灰 #5088A0 等）
  - 背景色：米白 #F5F0E8 或深色 #2A2828（可选暗色模式）
  - 文字色、边框色
- 在 `lib/core/theme/app_theme.dart` 配置全局 ThemeData：复古字体（如 Press Start 2P 或 VT323，通过 google_fonts 包引入）、像素风组件默认样式
- 在 `lib/core/theme/pixel_border.dart` 实现 `PixelBorder` 组件：用 `CustomPaint` 绘制硬边框（无圆角，阶梯感拐角）
- 在 `lib/core/widgets/pixel_button.dart` 和 `pixel_card.dart` 封装常用组件

**Verification:** 能在页面上渲染像素边框按钮和卡片，视觉上有 8-bit 感

**Files involved:** `lib/core/theme/app_colors.dart`, `lib/core/theme/app_theme.dart`, `lib/core/theme/pixel_border.dart`, `lib/core/widgets/pixel_button.dart`, `lib/core/widgets/pixel_card.dart`

---

### T-05 时间轴主视图
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-03, T-04

**Content:** 实现时间轴主页面，展示事件块、随笔点、当前时间线。

**Implementation guidance:**
- `lib/features/timeline/controllers/timeline_controller.dart`：用 Riverpod `StateNotifier` 管理当前查看日期、事件列表、随笔列表；监听 DAO 的 Stream
- `lib/features/timeline/timeline_screen.dart`：
  - `CustomScrollView` 纵向滚动，每天一个 section
  - 顶部 AppBar 显示日期 + "今天剩余 Xh Ym"（轻量展示）
  - FAB 悬浮按钮（记录入口）
- `lib/features/timeline/widgets/day_section.dart`：
  - 固定高度的时间轴（1 小时 = 60px，一天 24h = 1440px）
  - 整点刻度线 + 时间标签（0:00, 6:00, 12:00, 18:00）
  - 事件块定位：top = (startTime.hour * 60 + startTime.minute)，height = duration in minutes
- `lib/features/timeline/widgets/event_block.dart`：按 tag 颜色填充，显示标题；进行中的块延伸到当前时间
- `lib/features/timeline/widgets/note_dot.dart`：时间轴右侧的小圆点，点击展开文字
- `lib/features/timeline/widgets/current_time_line.dart`：横线标示当前时刻，仅今天显示
- `lib/features/timeline/widgets/ongoing_status_bar.dart`：顶部状态条"正在：xxx 已 23min"，点击停止

**Verification:** 打开 App 看到今天的时间轴，整点有刻度，当前时间有指示线；能上下滚动查看其他日期

**Files involved:** `lib/features/timeline/**/*.dart`

---

### T-06 记录：实时计时
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-05

**Content:** 实现 FAB → 开始计时 → 填信息 → 计时中 → 停止的完整流程。

**Implementation guidance:**
- `lib/features/recording/realtime_timer_controller.dart`：
  - `startEvent(title, tagId)` — 创建 status=inProgress 的事件，startTime=now，endTime=null
  - `stopEvent()` — 调用 `completeEvent(id, now)`
  - `Stream<int>` 每秒刷新已计时分钟数（用于 UI 更新）
- `lib/features/recording/recording_sheet.dart`：底部弹窗，包含标题输入框、标签选择器（横向滚动 chips）、"开始"按钮
- 点击 FAB → 弹出 recording_sheet → 填写 → 点"开始" → 弹窗关闭，顶部状态条出现，时间轴上出现实时增长的块
- 点击状态条 → 弹出确认 → 停止计时 → 事件变为 completed

**Verification:** 能开始计时，状态条和时间轴块实时更新，停止后事件固定为已完成

**Files involved:** `lib/features/recording/realtime_timer_controller.dart`, `lib/features/recording/recording_sheet.dart`

---

### T-07 记录：事后补录与提前写
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-06

**Content:** 实现长按时间轴空白处新建事件，支持过去和未来时间。

**Implementation guidance:**
- 在 `day_section.dart` 的空白区域添加 `GestureDetector`，长按触发 `recording_sheet`
- `recording_sheet` 增加时间选择模式：
  - 长按位置对应的时间作为默认 startTime
  - 可手动调整 startTime 和 endTime
  - 如果 startTime 在未来 → 状态默认 notStarted（提前写）
  - 如果 startTime 在过去 → 状态默认 completed（补录）
- `lib/features/recording/controllers/event_editor_controller.dart`：管理编辑状态，调用 `insertEvent` 或 `updateEvent`
- 已有事件点击 → 进入编辑模式（可修改标题/时间/标签/删除）

**Verification:** 长按时间轴能新建事件；选过去时间直接 completed；选未来时间 notStarted；点击已有事件能编辑

**Files involved:** `lib/features/recording/recording_sheet.dart`, `lib/features/recording/controllers/event_editor_controller.dart`, `lib/features/timeline/widgets/day_section.dart`

---

### T-08 事件状态流转与自动顺延
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-07

**Content:** 实现状态流转逻辑和 App 打开时的自动顺延。

**Implementation guidance:**
- 在 `event_dao.dart` 的 `carryOverNotStarted()` 中：
  - 查询所有 startTime < 今天 且 status = notStarted 的事件
  - 将 startTime 改为今天相同时刻（保留 hour/minute）
  - 更新 updatedAt
- 在 `lib/main.dart` 或 `timeline_controller.dart` 初始化时调用 `carryOverNotStarted()`
- 事件块视觉区分状态：
  - `notStarted`：虚线边框或半透明
  - `inProgress`：实心 + 实时增长
  - `completed`：实心
- 在事件编辑界面提供"开始"按钮（notStarted → inProgress）和"完成"按钮（inProgress → completed）

**Verification:** 创建一个昨天的 notStarted 事件，重启 App 后它出现在今天；事件块按状态有不同视觉

**Files involved:** `lib/data/daos/event_dao.dart`, `lib/features/timeline/widgets/event_block.dart`, `lib/features/recording/recording_sheet.dart`

---

### T-09 标签管理
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-04, T-03

**Content:** 实现标签的增删改查和颜色选择。

**Implementation guidance:**
- `lib/features/tags/controllers/tag_controller.dart`：Riverpod 管理，监听 `tagDao.getAllTags()` 的 Stream
- `lib/features/tags/tags_screen.dart`：
  - 标签列表（按 sortOrder 排序），每项显示颜色色块 + 名称
  - 点击进入编辑，滑动删除
  - 右上角"+"新增标签
- `lib/features/tags/tag_editor_sheet.dart`：
  - 名称输入框
  - 颜色选择器：从 `app_colors.dart` 的预设色板中选（网格展示 12-16 个色块）
  - 保存/删除按钮
- 在 `recording_sheet.dart` 的标签选择器中引用同一套色板

**Verification:** 能创建标签并选颜色；标签在记录时可选；删除标签后关联事件 tagId 置空

**Files involved:** `lib/features/tags/**/*.dart`

---

### T-10 随笔：基础创建与展示
**Priority:** P1
**Status:** 🟢 pure frontend
**Depends on:** T-05, T-03

**Content:** 实现随笔的创建入口和时间轴右侧点展示。

**Implementation guidance:**
- `lib/features/notes/controllers/note_controller.dart`：Riverpod 管理，`insertNote(content, timestamp)`、`updateNote`、`deleteNote`
- `lib/features/notes/note_editor_sheet.dart`：纯文本输入框 + 时间戳（默认 now），保存按钮
- 创建入口：时间轴 AppBar 上的随笔图标按钮，或长按时间轴右侧区域
- `note_dot.dart` 完善：
  - 时间轴右侧按 timestamp 定位的小圆点
  - 点击展开显示文字内容（弹出小卡片或底部 sheet）
  - 长按可编辑/删除

**Verification:** 能创建随笔；随笔在时间轴右侧显示为点；点击展开文字；能编辑删除

**Files involved:** `lib/features/notes/**/*.dart`, `lib/features/timeline/widgets/note_dot.dart`

---

### T-11 路由与整合
**Priority:** P0
**Status:** 🟢 pure frontend
**Depends on:** T-05, T-09

**Content:** 配置路由，整合所有页面，确保导航流畅。

**Implementation guidance:**
- `lib/core/router/app_router.dart`：使用 go_router 定义路由
  - `/` → timeline_screen（主页）
  - `/tags` → tags_screen（标签管理）
- 在 `main.dart` 中用 `MaterialApp.router` 包装
- 时间轴 AppBar 添加标签管理入口（图标按钮 → 跳转 `/tags`）
- 确保返回行为正确（标签页返回时间轴）

**Verification:** 能在时间轴和标签页之间导航；返回键/手势行为正确

**Files involved:** `lib/core/router/app_router.dart`, `lib/main.dart`, `lib/features/timeline/timeline_screen.dart`

---

### T-12 V1 整体打磨
**Priority:** P2
**Status:** 🟢 pure frontend
**Depends on:** T-08, T-09, T-10, T-11

**Content:** 整体体验打磨，确保手感顺滑。

**Implementation guidance:**
- 检查 8-bit 主题在所有页面一致性
- 时间轴滚动性能优化（懒加载日期 section）
- 空状态设计（无事件时时间轴的提示文案，如"今天还是空白，记一笔吧"）
- 首次打开引导（空标签 + 空事件时的友好提示）
- 在 Android 真机上验证一次完整流程

**Verification:** 完整走一遍记录 → 查看 → 编辑 → 顺延流程，手感流畅无卡顿

**Files involved:** 各 feature 文件按需微调

---

### Dependency Graph

```
T-01 (脚手架)
  ├── T-02 (表定义) → T-03 (DAO) → T-05 (时间轴) → T-06 (实时计时) → T-07 (补录/预写) → T-08 (状态流转)
  └── T-04 (主题) ─┘                    ↑
        ↑                               ├── T-10 (随笔)
        └── T-09 (标签) ─────────────────┘
                                         └── T-11 (路由) → T-12 (打磨)
```

**建议执行顺序：** T-01 → T-02 → T-03 → T-04 → T-05 → T-06 → T-07 → T-08 → T-09 → T-10 → T-11 → T-12
