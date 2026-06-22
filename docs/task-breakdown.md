## Development Task Breakdown — 事记本 V1（灵魂闭环）

> 前置：阅读 AGENT.md 了解项目上下文。参考 changes.md 了解文件范围。

---

### T-01 项目脚手架与依赖配置

**Priority:** P0
**Status:** pending
**Depends on:** 无

**Content:** 初始化原生 Android 项目，配置 Kotlin、Compose、Room、Hilt 依赖。

**Implementation guidance:**

- 创建 `build.gradle.kts`、`settings.gradle.kts`
- 创建 `app/build.gradle.kts`，声明依赖：
  - `androidx.core:core-ktx`
  - `androidx.lifecycle:lifecycle-runtime-ktx`
  - `androidx.activity:activity-compose`
  - `androidx.compose.ui:ui`、`ui-graphics`、`ui-tooling-preview`、`material3`
  - `androidx.navigation:navigation-compose`
  - `androidx.room:room-runtime`、`room-ktx` + kapt/ksp `room-compiler`
  - `com.google.dagger:hilt-android`、`hilt-android-compiler`
  - `androidx.hilt:hilt-navigation-compose`
  - `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- 创建 `MainActivity.kt`、`ShiJiBenApplication.kt`
- 配置 Hilt Application

**Verification:** Android Studio 能打开项目并同步成功

**Files involved:** `build.gradle.kts`, `settings.gradle.kts`, `app/build.gradle.kts`, `AndroidManifest.xml`, `ShiJiBenApplication.kt`, `MainActivity.kt`

---

### T-02 数据层：Entity 与 Database

**Priority:** P0
**Status:** pending
**Depends on:** T-01

**Content:** 定义 Event/Note/Tag 三个 Room Entity，创建 AppDatabase。

**Implementation guidance:**

- `data/model/EventStatus.kt`：枚举 `NotStarted`、`InProgress`、`Completed`
- `data/local/EventEntity.kt`：字段参照设计 spec，使用 `Long` 时间戳
- `data/local/NoteEntity.kt`
- `data/local/TagEntity.kt`
- `data/local/AppDatabase.kt`：继承 `RoomDatabase`，包含三个 Entity
- 为事件表创建按日期查询的索引

**Verification:** 编译通过，数据库能成功创建

**Files involved:** `data/model/EventStatus.kt`, `data/local/*.kt`

---

### T-03 数据层：DAO 与 Repository

**Priority:** P0
**Status:** pending
**Depends on:** T-02

**Content:** 实现 DAO 和 Repository，提供 CRUD 与业务查询。

**Implementation guidance:**

- `data/local/EventDao.kt`：
  - `getEventsByDate(startOfDay, endOfDay): Flow<List<EventEntity>>`
  - `getOngoingEvent(): Flow<EventEntity?>`
  - `getNotStartedBeforeDate(date): List<EventEntity>`
  - `insertEvent`、`updateEvent`、`deleteEvent`
  - `carryOverNotStarted(date)` — 把往日未开始事件移到今天
- `data/local/NoteDao.kt`、`TagDao.kt`：常规 CRUD
- `data/repository/EventRepository.kt`：封装 DAO，处理时间戳转换
- `data/repository/NoteRepository.kt`、`TagRepository.kt`

**Verification:** 单元测试或手动调用 Repository 方法能正确读写数据

**Files involved:** `data/local/*Dao.kt`, `data/repository/*.kt`

---

### T-04 核心主题：8-bit 活泼复古色板

**Priority:** P0
**Status:** pending
**Depends on:** T-01

**Content:** 定义 8-bit 活泼复古色板和像素风格组件。

**Implementation guidance:**

- `ui/theme/AppColors.kt`：定义 NES 风格高饱和色板
  - 鲜红 #E84A3C、翠绿 #4BC66D、亮紫 #7B5BF5、桃红 #F25CA2、金黄 #F2C94C、靛蓝 #3B82F6
  - 背景米白 #F5F0E8，文字深灰 #2A2828，边框深灰
- `ui/theme/AppTheme.kt`：配置 Material3 主题，字体使用系统无衬线 + 等宽数字
- `ui/theme/PixelComponents.kt`：
  - `PixelButton`：直角按钮 + 硬阴影
  - `PixelCard`：直角卡片 + 2dp 黑色边框
  - `PixelOutlinedButton`

**Verification:** 能在页面上渲染像素边框按钮和卡片，视觉上有 8-bit 感

**Files involved:** `ui/theme/*.kt`

---

### T-05 时间轴主视图

**Priority:** P0
**Status:** pending
**Depends on:** T-03, T-04

**Content:** 实现时间轴主页面。

**Implementation guidance:**

- `feature/timeline/TimelineViewModel.kt`：
  - 管理当前查看日期
  - 通过 Repository 的 Flow 收集事件
  - App 启动时调用 `carryOverNotStarted()`
- `feature/timeline/TimelineScreen.kt`：
  - 顶部 AppBar 显示日期
  - Row 左侧 `DayProgressBar`，右侧 `EventList`
  - 底部"记一笔"按钮
- `feature/timeline/DayProgressBar.kt`：
  - 上下两个正方形色块
  - 蓝色/绿色/米白/红色边框逻辑
- `feature/timeline/EventList.kt`：事件卡片列表
- 空状态文案

**Verification:** 打开 App 看到今天的事件列表 + 左侧时间条；能上下滑动

**Files involved:** `feature/timeline/*.kt`

---

### T-06 记录：时间范围滑块与事件创建

**Priority:** P0
**Status:** pending
**Depends on:** T-05

**Content:** 实现"记一笔"底部弹窗和双行时间滑块。

**Implementation guidance:**

- `feature/recording/RecordingSheet.kt`：
  - 标题输入框
  - `TimeRangeSlider` 组件
  - 标签选择 chips
  - 确定按钮
- `feature/recording/TimeRangeSlider.kt`：
  - 两行：0-12 小时、12-24 小时
  - 开始/结束两个可拖动手柄
  - 默认范围 30 分钟
  - 手柄可在两行间移动
- `feature/recording/RecordingViewModel.kt`：
  - 根据 start/end 时间判断状态（not_started / completed）
  - 插入事件

**Verification:** 点击"记一笔" → 弹窗 → 拖动滑块 → 确定 → 列表出现事件

**Files involved:** `feature/recording/*.kt`

---

### T-07 标签管理

**Priority:** P0
**Status:** pending
**Depends on:** T-03, T-04

**Content:** 实现标签的增删改查和颜色选择。

**Implementation guidance:**

- `feature/tags/TagsViewModel.kt`
- `feature/tags/TagsScreen.kt`：
  - 标签列表，每项显示颜色方块 + 名称
  - 右上角"+"新增
  - 点击编辑
- `feature/tags/TagEditorSheet.kt`：
  - 名称输入框
  - 颜色网格选择器（12 色）
  - 保存/删除按钮
- `feature/recording/RecordingSheet.kt` 中引用标签选择

**Verification:** 能创建标签并选颜色；标签在记录时可选

**Files involved:** `feature/tags/*.kt`

---

### T-08 事件状态流转与自动顺延

**Priority:** P0
**Status:** pending
**Depends on:** T-06, T-07

**Content:** 实现状态流转逻辑和 App 打开时的自动顺延。

**Implementation guidance:**

- 在 Repository 中实现 `carryOverNotStarted()`
- `TimelineViewModel` 初始化时调用
- 事件卡片视觉区分状态：
  - `notStarted`：虚线边框或半透明
  - `completed`：实心
- 事件编辑界面可修改状态

**Verification:** 创建一个昨天的 notStarted 事件，重启 App 后它出现在今天

**Files involved:** `data/repository/EventRepository.kt`, `feature/timeline/*.kt`

---

### T-09 随笔：基础创建与展示

**Priority:** P1
**Status:** pending
**Depends on:** T-05, T-03

**Content:** 实现随笔的创建入口和时间轴右侧点展示。

**Implementation guidance:**

- `feature/notes/NotesViewModel.kt`
- `feature/notes/NoteEditorSheet.kt`：纯文本输入框 + 时间戳
- 创建入口：AppBar 随笔图标
- 时间轴右侧按 timestamp 显示小圆点
- 点击圆点显示内容弹窗

**Verification:** 能创建随笔；随笔在时间轴右侧显示为点；点击展开文字

**Files involved:** `feature/notes/*.kt`, `feature/timeline/EventList.kt`

---

### T-10 导航与整合

**Priority:** P0
**Status:** pending
**Depends on:** T-05, T-07, T-09

**Content:** 配置 Navigation Compose，整合所有页面。

**Implementation guidance:**

- `navigation/AppNavHost.kt`：
  - `timeline` → TimelineScreen
  - `tags` → TagsScreen
  - `notes` → NotesScreen
- `MainActivity.kt` 中设置 `AppNavHost`
- AppBar 添加标签、随笔入口图标

**Verification:** 能在时间轴、标签页、随笔页之间导航；返回行为正确

**Files involved:** `navigation/AppNavHost.kt`, `MainActivity.kt`, `feature/timeline/TimelineScreen.kt`

---

### T-11 V1 整体打磨

**Priority:** P2
**Status:** pending
**Depends on:** T-08, T-09, T-10

**Content:** 整体体验打磨。

**Implementation guidance:**

- 检查 8-bit 主题在所有页面一致性
- 空状态设计
- 首次打开引导
- 在 Android 真机上验证一次完整流程

**Verification:** 完整走一遍记录 → 查看 → 编辑 → 顺延流程，手感流畅

**Files involved:** 各 feature 文件按需微调

---

### Dependency Graph

```
T-01 (脚手架)
  ├── T-02 (Entity) → T-03 (DAO/Repository) → T-05 (时间轴) → T-06 (记录滑块) → T-08 (状态流转)
  └── T-04 (主题) ─┘                    ↑
        ↑                               ├── T-09 (随笔)
        └── T-07 (标签) ─────────────────┘
                                         └── T-10 (导航) → T-11 (打磨)
```

**建议执行顺序：** T-01 → T-02 → T-03 → T-04 → T-05 → T-06 → T-07 → T-08 → T-09 → T-10 → T-11
