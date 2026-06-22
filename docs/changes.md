## Change Impact List — 事记本 V1（灵魂闭环）

> 基于 AGENT.md 上下文和设计 spec 分析。V1 为全新原生 Android 项目，所有变更均为新建文件。

### New Files

| File Path | Purpose | Key Dependencies |
|-----------|---------|-------------------|
| `build.gradle.kts` | 项目级 Gradle 配置 | Android Gradle Plugin、Kotlin |
| `settings.gradle.kts` | Gradle 项目设置 | |
| `app/build.gradle.kts` | App 模块配置与依赖声明 | Compose、Room、Hilt、Navigation |
| `app/src/main/AndroidManifest.xml` | 应用清单 | |
| `app/src/main/java/com/shijiben/ShiJiBenApplication.kt` | Application 类，初始化 Hilt | Hilt |
| `app/src/main/java/com/shijiben/MainActivity.kt` | 入口 Activity | Navigation Compose |
| `app/src/main/java/com/shijiben/ui/theme/AppColors.kt` | 8-bit 活泼复古色板常量 | Compose |
| `app/src/main/java/com/shijiben/ui/theme/AppTheme.kt` | 全局主题配置（字体、边框、颜色） | Compose |
| `app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt` | 像素边框按钮/卡片组件 | Compose |
| `app/src/main/java/com/shijiben/data/local/EventEntity.kt` | Event 表 Room Entity | Room |
| `app/src/main/java/com/shijiben/data/local/NoteEntity.kt` | Note 表 Room Entity | Room |
| `app/src/main/java/com/shijiben/data/local/TagEntity.kt` | Tag 表 Room Entity | Room |
| `app/src/main/java/com/shijiben/data/local/EventDao.kt` | Event DAO | Room、Coroutines |
| `app/src/main/java/com/shijiben/data/local/NoteDao.kt` | Note DAO | Room、Coroutines |
| `app/src/main/java/com/shijiben/data/local/TagDao.kt` | Tag DAO | Room、Coroutines |
| `app/src/main/java/com/shijiben/data/local/AppDatabase.kt` | Room Database 定义 | Room |
| `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` | Event 业务仓库 | DAO、Entity |
| `app/src/main/java/com/shijiben/data/repository/NoteRepository.kt` | Note 业务仓库 | DAO、Entity |
| `app/src/main/java/com/shijiben/data/repository/TagRepository.kt` | Tag 业务仓库 | DAO、Entity |
| `app/src/main/java/com/shijiben/data/model/EventStatus.kt` | 事件状态枚举 | Kotlin |
| `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` | 时间轴主页面 | Compose、ViewModel |
| `app/src/main/java/com/shijiben/feature/timeline/TimelineViewModel.kt` | 时间轴状态管理 | Flow、Repository |
| `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` | 左侧时间条（2 个方块） | Compose |
| `app/src/main/java/com/shijiben/feature/timeline/EventList.kt` | 右侧事件卡片列表 | Compose |
| `app/src/main/java/com/shijiben/feature/recording/RecordingSheet.kt` | 记录入口底部弹窗 | Compose |
| `app/src/main/java/com/shijiben/feature/recording/RecordingViewModel.kt` | 记录状态管理 | Repository |
| `app/src/main/java/com/shijiben/feature/recording/TimeRangeSlider.kt` | 0-12/12-24 时间滑块 | Compose |
| `app/src/main/java/com/shijiben/feature/tags/TagsScreen.kt` | 标签管理页面 | Compose、ViewModel |
| `app/src/main/java/com/shijiben/feature/tags/TagsViewModel.kt` | 标签状态管理 | Repository |
| `app/src/main/java/com/shijiben/feature/tags/TagEditorSheet.kt` | 标签编辑弹窗 | Compose |
| `app/src/main/java/com/shijiben/feature/notes/NotesScreen.kt` | 随笔列表（v2 完整化） | Compose、ViewModel |
| `app/src/main/java/com/shijiben/feature/notes/NoteEditorSheet.kt` | 随笔编辑弹窗 | Compose |
| `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | Navigation Compose 路由配置 | Navigation |

### Modified Files

无（全新项目）

### Deleted Files

无

### API Changes

无（纯本地应用，无网络 API）

### Dependency / Config Changes

| Item | Change | Notes |
|------|--------|-------|
| Android Gradle Plugin | 新增 | 项目基础 |
| Kotlin | 新增 | 开发语言 |
| Jetpack Compose | 新增 | UI 框架 |
| Room | 新增 | SQLite ORM |
| Hilt | 新增 | 依赖注入 |
| Navigation Compose | 新增 | 路由管理 |
| Coroutines / Flow | 新增 | 异步与响应式 |
| Material3 / Material Icons | 新增 | UI 组件 |
