# 首页构图与留白重平衡（方案 C）

日期：2026-06-28
范围：`TimelineScreen` 首页视觉构图优化，聚焦「构图与留白失衡」痛点
前置：2026-06-27 顶/底重设计 spec（Part A 顶部徽章、Part B 底部双 block + 抽屉）已完成

## 问题

首页竖向比例失衡：顶部彩虹条 + 居中大徽章（含 3dp 硬阴影）占约 20%，底部彩虹条 + 双 block 占约 20%，中间内容只剩 60%。横向也有浪费：左侧 `DayProgressBar` 占 40dp 宽（16dp 方块 + 16dp 数字标签），挤压右侧事件列表。彩虹条在顶栏、底栏、抽屉三处重复出现，视觉噪音盖过内容。

## 目标

- 中间内容区竖向占比从 ~60% 提到 ~72%。
- 列表横向多拿 ~20dp 宽，padding 更舒展。
- 彩虹条作为品牌标识只保留一处（顶部），其余位置用 2dp 黑色顶边收口。
- 不改变现有交互逻辑：日期切换仍点徽章弹日历选择器；底部双 block 入口/抽屉行为不变。

## 非目标

- 不改 `EventCard` / `NoteRow` 内部信息结构（标题、时间 badge、状态图标保持原样）。
- 不引入左右滑动手势翻日、不加 ‹ › 箭头（用户已确认保持现状）。
- 不动 `RecordingSheet` / `NoteEditorSheet` / `DateSelectorDialog`。
- 不改 `TimelineViewModel` 数据流（统计文案可直接由现有 `events` / `notes` 列表 size 计算，无需新增仓库方法）。

## 设计

### 第 1 节 · 整体比例与彩虹条裁剪

竖向占比从 `顶 20% / 中 60% / 底 20%` 调到 `顶 12% / 中 72% / 底 16%`，中间净增约 12% 高度。

彩虹条从 3 处裁到 1 处：
- **保留**：顶部 `PixelCard` 内的 8dp 彩虹条（品牌标识）。
- **移除**：`BottomEntryBar` 顶部的 3dp `RainbowTrim` 调用。
- **移除**：`EntryDrawer` 顶部的 3dp `RainbowTrim` 调用。
- 底栏与抽屉改用 2dp 黑色顶边收口：底栏需新增一个 2dp 黑色 `Box` 置顶（替代原彩虹条位置）；抽屉已有 2dp 黑色顶边 `Box`（`EntryDrawer` 里 `height(2.dp).background(Color.Black)`），保留它、仅删掉其下方的 `RainbowTrim`。

`RainbowTrim` 私有 composable 若变为无调用方，删除该函数及其依赖，避免死代码。

### 第 2 节 · 顶栏一条带

将 `TimelineScreen` 内顶部的 `PixelCard { ... 居中大徽章 ... }` 替换为一条横向 strip（高约 36dp）：

```
[顶部 8dp 彩虹条]
[strip: 12dp 左 padding | 日期徽章(小) ........ 12dp 右 padding | 统计文案(小灰) ]
[2dp 黑色底分隔线]
```

- **左**：日期徽章。保留 2dp 黑边白底像素风，但缩小（padding `horizontal=12, vertical=4` → 字号约 13sp），去掉 3dp 硬阴影 `Box`。点击仍触发 `showDatePicker = true`，弹 `DateSelectorDialog`。
- **右**：统计文案，`TextTertiary` 色、约 11sp、`FontWeight.Medium`。
  - 有记录：`"{事件数} 件事 · {随笔数} 条随笔"`（事件数 = `events.size`，随笔数 = `notes.size`）。
  - 空日（两者都为 0）：`"还没有记录"`。
  - 今天与过去日都显示；统计基于当前 `viewingDate` 的数据，无需额外查询。
- strip 容器：`background(Surface)`，底部 2dp 黑色分隔线（`Box` 高 2dp `background(Color.Black)`）。

### 第 3 节 · 中部 — 进度栏瘦身 + 列表留白

**`DayProgressBar` 改动：**
- 外层 `Column` 调用处宽度由 `40.dp` 改为 `20.dp`（在 `TimelineScreen` 的 `Row` 里把左 `Box` 的 `width(40.dp)` 改 `width(20.dp)`，相应 `padding(start=12.dp, top=12.dp)` 保留或微调）。
- 方块 `size(16.dp)` → `size(8.dp)`。
- **删除小时数字标签**：移除 `if (hour % 6 == 0) { Text("00"...) } else { Spacer }` 整段，方块直接纵向 `SpaceEvenly` 排列。三态逻辑不变：
  - `hasRecord` → `RainbowHourColors[hour % 8]` 实色。
  - 已过去（`isPast`）→ `baseColor.copy(alpha = 0.3f)` 淡色。
  - 未来 → `BorderLight` 浅灰。
  - `isNow`（今天且当前小时）→ 保留 2dp 黑边 `border`。
- 理由：进度栏只负责「全天一览」，精确时刻交给事件卡片的时间显示。

**`EventList` 改动：**
- 外层 `Column` 的列表区 `Box`（右半 `weight(1f)`）padding 由 `start=8.dp, top=12.dp, end=12.dp` 改为 `start=12.dp, top=12.dp, end=12.dp`（左右统一 12dp）。
- `EventCard` / `NoteRow` 的 `padding(bottom = 6.dp)` 改为 `padding(bottom = 8.dp)`，卡片间距更松。

### 第 4 节 · 底栏 — 去彩虹条 + 压到 34dp

**`BottomEntryBar` 改动：**
- 删除 `Column { RainbowTrim(3.dp); Row(...) }` 里的 `RainbowTrim` 调用。
- `Row` 高度 `48.dp` → `34.dp`，`padding(horizontal=8.dp, vertical=4.dp)` 改为 `padding(horizontal=6.dp, vertical=4.dp)`。
- 两侧图标 `Box` 的 `size(36.dp)` → `size(26.dp)`，内部 `Icon` 的 `Modifier.size(20.dp)` → `size(14.dp)`。
- 两侧输入框触发器 `Box` 的 `height(36.dp)` → `height(26.dp)`，占位 `Text` 字号 `14.sp` → `12.sp`。
- 顶边：`Column` 顶部加 2dp 黑色 `Box`（`Modifier.fillMaxWidth().height(2.dp).background(Color.Black)`）替代原彩虹条。
- **触控区**：图标与触发器视觉缩到 26dp，但 `clickable` 的命中区用外层 `Box` 的 `padding` 或 `minimumInteractiveComponentSize` 扩到约 40dp，保证好点。

**`EntryDrawer` 改动：**
- 删除 `RainbowTrim(3.dp)` 调用，保留其上方的 2dp 黑色顶边 `Box`（已存在）。

## 涉及文件

| 文件 | 改动 |
|---|---|
| `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt` | 顶栏 strip 重写；`DayProgressBar` 调用处宽度 40→20dp；`EventList` 列表区 padding；`BottomEntryBar` 去 RainbowTrim + 压高 + 缩图标；`EntryDrawer` 去 RainbowTrim；统计文案；删除无用的 `RainbowTrim` 函数 |
| `app/src/main/java/com/shijiben/feature/timeline/DayProgressBar.kt` | 方块 16→8dp；删除小时数字标签分支 |

无需改 `TimelineViewModel`（统计直接用 `events.size` / `notes.size`）。

## 边界情况

- **空日**：统计显示「还没有记录」；`DayProgressBar` 全部方块为未来态（`BorderLight`）；`EventList` 已有空状态文案「今天还是空白」，保留。
- **过去日**：`DayProgressBar` 全部为「已过去」淡色态（现有逻辑 `isPast` 覆盖全天）；统计显示该日计数。
- **事件数/随笔数为 0 但另一项非 0**：文案按实际计数拼，如 `"0 件事 · 2 条随笔"`。若希望更自然，可写成「2 条随笔」省略 0 项——本 spec 取显式拼接（简单、一致），后续可调。
- **进度栏溢出**：24 个 8dp 方块 = 192dp，加 `SpaceEvenly` 间距后远小于中间区高度（~72% 屏高），不会溢出。
- **短屏幕 / 字体缩放**：底栏 34dp 在大字体下可能偏紧，但双 block 文案为占位提示非正文，且命中区已扩到 ~40dp，可接受。

## 测试

- 验证门：`./gradlew :app:compileDebugKotlin`（类型检查）、`./gradlew :app:testDebugUnitTest`（现有单测）、`./gradlew assembleDebug`（构建）。
- 无 Compose UI 测试配置；本 spec 为纯视觉/布局调整，不新增 UI 测试。
- 手动验证：今天/过去日/空日三种状态下，顶栏统计、进度栏三态、列表留白、底栏触控是否符合预期。
- 现有 `TimelineViewModelTest` 不受影响（未改 VM）。
