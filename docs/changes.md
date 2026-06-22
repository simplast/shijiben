## Change Impact List — 事记本 V1（灵魂闭环）

> 基于 AGENT.md 上下文和设计 spec 分析。V1 为全新项目，所有变更均为新建文件。

### New Files

| File Path | Purpose | Key Dependencies |
|-----------|---------|-------------------|
| `pubspec.yaml` | 项目配置与依赖声明 | flutter, drift, riverpod, go_router |
| `lib/main.dart` | App 入口，初始化数据库与路由 | drift, riverpod, go_router |
| `lib/data/database/app_database.dart` | drift 数据库定义，连接管理 | drift |
| `lib/data/tables/events_table.dart` | Event 表定义 | drift |
| `lib/data/tables/notes_table.dart` | Note 表定义 | drift |
| `lib/data/tables/tags_table.dart` | Tag 表定义 | drift |
| `lib/data/daos/event_dao.dart` | Event 数据访问层（CRUD + 状态流转 + 顺延） | drift, events_table |
| `lib/data/daos/note_dao.dart` | Note 数据访问层 | drift, notes_table |
| `lib/data/daos/tag_dao.dart` | Tag 数据访问层 | drift, tags_table |
| `lib/data/models/event_status.dart` | 事件状态枚举 | dart |
| `lib/core/theme/app_colors.dart` | 8-bit 柔和复古色板常量 | flutter |
| `lib/core/theme/app_theme.dart` | 全局主题配置（字体、边框、颜色） | flutter, app_colors |
| `lib/core/theme/pixel_border.dart` | 像素边框绘制组件 | flutter |
| `lib/core/widgets/pixel_button.dart` | 8-bit 风格按钮 | flutter, pixel_border |
| `lib/core/widgets/pixel_card.dart` | 8-bit 风格卡片容器 | flutter, pixel_border |
| `lib/features/timeline/timeline_screen.dart` | 时间轴主视图页面 | flutter, riverpod, event_dao, note_dao |
| `lib/features/timeline/widgets/day_section.dart` | 单日时间轴区块（日期头 + 事件块 + 随笔点） | flutter |
| `lib/features/timeline/widgets/event_block.dart` | 事件块组件（按 tag 着色，高度=时长） | flutter, app_colors |
| `lib/features/timeline/widgets/note_dot.dart` | 随笔点组件（右侧小圆点，点开展开） | flutter |
| `lib/features/timeline/widgets/current_time_line.dart` | 当前时间指示线 | flutter |
| `lib/features/timeline/widgets/ongoing_status_bar.dart` | 进行中事件顶部状态条 | flutter, riverpod |
| `lib/features/timeline/controllers/timeline_controller.dart` | 时间轴状态管理（日期切换、数据加载） | riverpod, event_dao, note_dao |
| `lib/features/recording/recording_sheet.dart` | 记录入口底部弹窗（标题/标签/时间输入） | flutter, riverpod, tag_dao |
| `lib/features/recording/realtime_timer_controller.dart` | 实时计时逻辑（开始/停止/刷新） | riverpod, event_dao |
| `lib/features/recording/controllers/event_editor_controller.dart` | 事件编辑状态管理 | riverpod, event_dao, tag_dao |
| `lib/features/tags/tags_screen.dart` | 标签管理页面（列表 + 增删改） | flutter, riverpod, tag_dao |
| `lib/features/tags/tag_editor_sheet.dart` | 标签编辑弹窗（名称 + 颜色选择） | flutter, app_colors |
| `lib/features/tags/controllers/tag_controller.dart` | 标签状态管理 | riverpod, tag_dao |
| `lib/features/notes/note_editor_sheet.dart` | 随笔创建/编辑弹窗 | flutter, riverpod, note_dao |
| `lib/features/notes/controllers/note_controller.dart` | 随笔状态管理 | riverpod, note_dao |
| `lib/core/utils/auto_carry.dart` | 未开始事件自动顺延逻辑 | event_dao |
| `lib/core/router/app_router.dart` | 路由配置 | go_router |

### Modified Files
无（全新项目）

### Deleted Files
无

### API Changes
无（纯本地应用，无网络 API）

### Dependency / Config Changes
| Item | Change | Notes |
|------|--------|-------|
| flutter SDK | 新增 | 项目基础 |
| drift | 新增 | SQLite ORM |
| sqlite3_flutter_libs | 新增 | SQLite 原生库 |
| riverpod | 新增 | 状态管理 |
| flutter_riverpod | 新增 | Riverpod Flutter 集成 |
| go_router | 新增 | 路由管理 |
| drift_dev | 新增 (dev) | drift 代码生成 |
| build_runner | 新增 (dev) | 代码生成工具 |
| flutter_lints | 新增 (dev) | 代码规范 |
