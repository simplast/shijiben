# 首页顶部 + 底部输入框重设计

> 日期：2026-06-27
> 状态：待审阅
> 来源：用户反馈"首页顶部设计有点呆板，底部输入框也是"

## 一、背景与目标

时间本（ShiJiBen）首页 `TimelineScreen` 当前问题：

- **顶部日期栏呆板**：4 个 IconButton（★/‹/✎/›）权重相同、左右完全对称、彩虹装饰条仅 4dp 像分隔线、日期无性格。
- **底部输入框呆板**：用 Material3 默认 `OutlinedTextField`，与 app 的 8-bit 像素美学脱节；只有"无时间快记"一个入口；占位文字一成不变。
- **随笔新增层级过深**：顶部 ✎ → NotesScreen → 右上角加号，3 步才能开始写。

目标：让顶部有视觉焦点与像素性格、底部成为"统一记录入口"（事件 + 随笔一步可达），并顺应用户决策移除标签功能。

## 二、范围

本 spec 含三部分，互相独立可分别实现/PR：

- **Part A · 顶部日期栏重设计**（中等）
- **Part B · 底部双 block 统一入口 + 抽屉**（中等）
- **Part C · 移除标签功能**（中等偏大，含 DB 迁移，风险最高）

> C 是用户明确要求"标签功能整个删掉"。它与 A/B 正交，但因 EventEntity.tagId 与 DB 版本耦合，需带迁移。建议 C 单独成 PR，先做或后做均可。

## 三、Part A · 顶部日期栏重设计

### 现状
`TimelineScreen` 顶部 `PixelCard` 内：4dp 彩虹条 + Row(`[★ ‹] 日期 [✎ ›]`)。

### 目标设计：像素徽章头

- **去掉 ★（标签入口）** —— 标签功能整体移除（见 Part C）。
- **去掉 ✎（随笔入口）** —— 已移到底部（见 Part B）。
- **去掉 ‹ › 箭头** —— 不再有显式前后天切换按钮。
- **顶部只剩日期徽章**，居中：
  - 日期装进**黑框硬阴影徽章盒**（2dp 黑边 + 3dp 硬阴影 `#000`），8-bit 感最强。
  - 像素字体（Fusion Pixel），20sp，粗体。
  - 格式沿用 `formatDateCompact`：`6/27/五`。
  - 在今天 → 徽章白底黑字；非今天 → 徽章文字/边框用 `Accent`（橙）色调，提示"不在今天"。
- **彩虹装饰条加厚到 8dp**（现为 4dp），仍是 8 色循环铺满整宽，作为顶部装饰带而非分隔线。
- **日期切换方式**：点击日期徽章 → 打开**日期选择器**（见下）。无滑动、无箭头。
- **回今天**：非今天状态下，点击徽章打开选择器，选择器内"今天"高亮可选；不额外加"回今天"按钮（用户已认可用点日期方式切换）。徽章橙色态即为"不在今天"的视觉信号。
- **时段标记（可选点缀）**：徽章右侧一个极小太阳/月亮像素标记（6:00–18:00 ☀ / 其余 ☾），仅作性格装饰。实现成本高可砍。

### 日期选择器
- 复用 Android `DatePickerDialog`（Material），用 app 主题色（白底、黑字、`Primary` 选中色）。
- 选中日期 → `viewModel.viewingDate` 更新 → 关闭选择器。
- 两个入口都能触发：顶部徽章、底部 📅（见 Part B）。用户明确接受两入口冗余。
- 不自研像素风日历（V2 再做"日历视图"）。

### 改动文件
- `feature/timeline/TimelineScreen.kt`：重写顶部 `PixelCard` 内容（徽章 + 厚彩虹条），删除 ★/✎/‹/›。删除 `onTagsClick` 参数（标签移除）；`onNotesClick` 保留，改由底部 ✎ 触发（见 Part B）。
- 新增日期选择器触发逻辑（`DatePickerDialog` state）。

## 四、Part B · 底部双 block 统一入口 + 抽屉

### 现状
底部一个 `OutlinedTextField`（Material 默认）做"无时间快记"，trailingIcon 小 `+`。

### 目标设计：紧凑双 block 底栏 + 展开抽屉

#### B1. 底栏（常态）
- 高度 = 正常 menu 高度（约 48dp），单行。**不是打字处，是功能入口**。
- 布局（图标在两端、输入框在中间）：

  ```
  [📅][ 记事输入框占位 ] | [ 随笔输入框占位 ][✎]
  ```

- 左 block：`[📅 日历图标][记事输入框]`；右 block：`[随笔输入框][✎ 随笔图标]`；中间 2dp 黑色竖分隔线。
- **图标**（2dp 黑边方框，36×36dp）：
  - 📅 → `Primary`（红）底白图标。点击 → 打开日期选择器（同 Part A）。
  - ✎ → `Accent`（橙）底白图标。点击 → 跳转 `NotesScreen`（随笔列表页）。
- **输入框**（2dp 黑边、白底、单行、像素字体粗体、灰占位）：是**点击触发器**，不可直接打字。
  - 记事占位固定：`在做什么？`
  - 随笔占位固定：`写点什么...`
  - 点击记事输入框 → 打开**记事抽屉**。
  - 点击随笔输入框 → 打开**随笔抽屉**。
- 顶部加 3dp 彩虹条（8 色循环）与顶栏呼应，强化像素系统一致性。
- 颜色身份区分：记事=红、随笔=橙（用户已确认区分）。

#### B2. 抽屉（展开态）
- 从底部弹出，**展开时底栏隐藏**（抽屉替代底栏位置）。
- 抽屉内容极简，**无提交按钮、无 ✕**：
  - 顶部一行：标签徽章（`记事` 红底 / `随笔` 橙底，2dp 黑边）。
  - 下方：多行输入框（2dp 黑边、白底、像素字体粗体、min-height ~96dp），自动聚焦弹键盘。
- **提交**：输入法自带「完成」（IME `Done`）→ 提交。
  - 记事 → `viewModel.quickAddEvent(text)`（新增 `not_started` 无时间事件，已有方法）。
  - 随笔 → `notesViewModel.save(content)`（新增随笔带时间戳，已有方法）。
  - 提交后清空**该抽屉**的草稿、关闭抽屉、恢复底栏。
- **关闭**：点抽屉外部空白 → 关闭。无 ✕。
- **草稿缓存**：关闭时若未提交，**不清空**，缓存当前文本；下次打开同抽屉恢复草稿。两种抽屉各自独立缓存。
- 多行 + IME Done 提交的取舍：随笔无法用键盘换行（Done 即提交）。用户已明确接受。若将来需要换行，再议。

#### B3. 记事/计时模型（用户思路澄清）
- 新增事件 = 加一件"未开始"的事（无时间、`status=0`）。
- 要开始：在事件卡片上点 ▶ 开始按钮（已存在）→ `markInProgress` 自动计时。
- 时间设置（开始/持续时长滑块）只在 `RecordingSheet` 手动编辑时出现，不在底部入口出现。
- **本 spec 不改计时逻辑**，仅把"新增无时间事件"入口放到底部记事抽屉。

### 改动文件
- `feature/timeline/TimelineScreen.kt`：删除现有底部 `OutlinedTextField`；新增双 block 底栏 + 抽屉 composable + 抽屉草稿 state（`remember` 两个草稿字符串 + 抽屉开合 state）。
- `feature/notes/NotesViewModel.kt`：`save()` 已存在，确认可被 TimelineScreen 直接调用（当前 TimelineScreen 已注入 `notesViewModel`）。
- 日期选择器：与 Part A 共用触发函数。

## 五、Part C · 移除标签功能

用户明确要求"标签功能整个删掉"。涉及 16 处引用。

### 删除文件
- `feature/tags/TagsScreen.kt`
- `feature/tags/TagsViewModel.kt`
- `feature/tags/TagEditorSheet.kt`
- `data/local/TagEntity.kt`
- `data/local/TagDao.kt`
- `data/repository/TagRepository.kt`

### 修改文件
- `data/local/EventEntity.kt`：删除 `val tagId: Long?` 字段。
- `data/local/AppDatabase.kt`：`entities` 移除 `TagEntity::class`，`version = 1 → 2`，移除 `tagDao()`。
- `data/local/EventDao.kt`：移除任何 tag 相关查询/字段引用（核对）。
- `data/repository/EventRepository.kt`：移除 tagId 透传。
- `data/DataModule.kt`：移除 `TagRepository` 提供。
- `feature/recording/RecordingViewModel.kt`：移除 `tagRepository`、`tags`、`selectedTagId`、`onTagSelected`，及 save 时的 `tagId =` 赋值（2 处：新增/编辑）。
- `feature/timeline/TimelineScreen.kt`：移除 ★ `IconButton` 与 `onTagsClick` 参数（Part A 已含）。
- `feature/timeline/TimelineViewModel.kt`：核对移除 tag 引用。
- `navigation/AppNavHost.kt`：移除 `Routes.TAGS`、`TagsScreen` composable、`onTagsClick` 传参。
- `ui/theme/AppColors.kt`：`TagColors` / `TagColorPalette` 若无其他引用则删；保留其他彩虹色。

### DB 迁移（风险点）
- 版本 1→2。SQLite < 3.35 不支持 `DROP COLUMN`，需用 Room 标准"建新表-拷数据-删旧-改名"迁移：
  1. 建 `events_new`（无 `tagId`，保留索引）。
  2. `INSERT INTO events_new SELECT id,title,startTime,endTime,status,note,createdAt,updatedAt FROM events`。
  3. `DROP TABLE events`、`DROP TABLE tags`。
  4. `ALTER TABLE events_new RENAME TO events`。
  5. 重建索引。
- 在 `AppDatabase` 加 `Migration(1, 2)` 并注册到 `Room.databaseBuilder`。
- 不用 `fallbackToDestructiveMigration`（会丢用户已记录事件，不可接受）。
- 更新 `app/schemas/` 导出 schema v2。

## 六、不在范围内

- 不自研像素风日历组件（用 Material `DatePickerDialog`）。
- 不改事件计时逻辑、不改 `RecordingSheet` 时间滑块。
- 不改 `DayProgressBar`、`EventCard` 视觉（除 tag 移除的连带清理）。
- 不做热力图、时间可视化（V2）。
- 时段太阳/月亮标记为可选点缀，可砍。

## 七、风险与开放问题

1. **DB 迁移**是最高风险点：迁移写错可能丢数据。建议 Part C 单独 PR，迁移上线前在装有旧数据的设备验证。
2. **IME Done 提交随笔**牺牲了键盘换行。用户已确认，但需在实现后实测体验。
3. **草稿缓存**仅内存态（`remember`），App 重启后清空。若要持久化草稿属额外范围，暂不做。
4. **时段太阳/月亮标记**：可选，实现成本与价值不成比例时可砍。
5. 顶部日期徽章"非今天=橙色"是否足以替代"回今天"快捷操作，需实测确认。

## 八、验收标准

- **Part A**：顶部仅显示居中日期徽章（黑框硬阴影）+ 8dp 彩虹条；无 ★/✎/‹/›；点徽章开日期选择器并正确切换 `viewingDate`；非今天徽章呈橙色态。
- **Part B**：底栏 48dp 双 block，`[📅][记事框] | [随笔框][✎]`；点输入框开对应抽屉、底栏隐藏；IME 完成提交并清草稿、点空白关闭并留草稿；📅 开日期选择器、✎ 进 NotesScreen；记事提交生成 `not_started` 事件、随笔提交生成随笔。
- **Part C**：标签相关文件/字段/路由全删；DB 1→2 迁移成功，旧事件数据（无 tagId）保留；App 编译通过、标签无残留引用。
- 整体：像素美学一致（直角、硬阴影、彩虹条、像素字体），无 Material 默认控件裸露在底栏。
