# 减少黑色边框滥用设计文档

## 背景
App 中大量使用 2dp `Color.Black` 边框，导致视觉重点被淹没，整体显得沉重。需要系统性地减少黑边，让真正的重点（选中状态、进行中事件、关键分隔）更突出。

## 设计原则
- **装饰性元素**不再用黑边描边，靠形状/颜色/背景区分。
- **交互容器**的黑边统一换成主题浅灰 `Border`，降低视觉重量。
- **真正需要强调的部位**保留黑边或主题色边框：底部双区黑色竖分隔线、热力图 Tab 选中状态的 Primary 红边框。

## 具体改动

### 一、完全移除黑边（装饰/数据展示）

| 文件 | 元素 | 说明 |
|------|------|------|
| `feature/heatmap/HeatmapCommon.kt:55` | 热力图图例色块 | 仅保留色块本身 |
| `feature/heatmap/HeatmapCommon.kt:79` | 上一页/下一页箭头方块 | 用背景/图标表示可点击 |
| `feature/heatmap/TimeAllocationTab.kt:185` | 去向页排名徽章 | 彩色背景即可 |
| `feature/heatmap/TimeAllocationTab.kt:218` | 去向页横向条形图外框 | 去掉外框，内部色块直接展示 |
| `feature/settings/CircadianScreen.kt:152` | 生物钟页横向条形图外框 | 同上 |
| `feature/timeviz/TimeVizScreen.kt:297` | + / - 步进器方块 | 彩色背景按钮 |
| `feature/timeline/TimelineScreen.kt:601` | 随笔行左侧 ✎ 方块 | 保留 Accent 橙色背景，去掉黑边 |

### 二、黑边改为浅灰 `Border`

| 文件 | 元素 |
|------|------|
| `feature/timeline/TimelineScreen.kt:175` | 顶栏日期徽章 |
| `feature/timeline/TimelineScreen.kt:193` | 顶栏热力图入口方块 |
| `feature/timeline/TimelineScreen.kt:214` | 顶栏搜索入口方块 |
| `feature/timeline/TimelineScreen.kt:231` | 顶栏设置入口方块 |
| `feature/timeline/TimelineScreen.kt:729` | 底部日历按钮 |
| `feature/timeline/TimelineScreen.kt:745` | 底部事件输入框触发器 |
| `feature/timeline/TimelineScreen.kt:776` | 底部随笔输入框触发器 |
| `feature/timeline/TimelineScreen.kt:792` | 底部随笔列表按钮 |
| `feature/timeline/TimelineScreen.kt:850` | 抽屉标签徽章 |
| `feature/timeline/TimelineScreen.kt:867` | 抽屉输入框 |
| `feature/search/SearchScreen.kt:113` | 搜索页搜索输入框 |
| `feature/heatmap/HeatmapScreen.kt:99` | 热力图 Tab 未选中标签 |
| `ui/theme/PixelComponents.kt:28` | `PixelCard` 默认边框色 |

### 三、保留黑边或主题色

| 文件 | 元素 | 理由 |
|------|------|------|
| `feature/timeline/TimelineScreen.kt:760` | 底部事件/随笔区 2dp 黑色竖分隔线 | 强结构分隔，突出双入口 |
| `feature/heatmap/HeatmapScreen.kt:99` | 热力图 Tab 选中标签的 Primary 红边框 | 选中态重点 |

## 预期效果
- 顶部按钮、底部输入栏从"黑框一片"变成"浅灰细线"，与浅灰背景更融洽。
- 图表类元素去掉外框后，数据色块本身成为视觉主体。
- 真正的强调元素（红色选中边框、黑色竖分隔线、进行中事件）自然凸显。

## 验证
- 编译通过。
- 各相关界面目视检查：黑边不再泛滥，重点仍然清晰。

## 涉及文件
- `app/src/main/java/com/shijiben/feature/timeline/TimelineScreen.kt`
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt`
- `app/src/main/java/com/shijiben/feature/heatmap/HeatmapCommon.kt`
- `app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationTab.kt`
- `app/src/main/java/com/shijiben/feature/timeviz/TimeVizScreen.kt`
- `app/src/main/java/com/shijiben/feature/settings/CircadianScreen.kt`
- `app/src/main/java/com/shijiben/feature/search/SearchScreen.kt`
- `app/src/main/java/com/shijiben/ui/theme/PixelComponents.kt`
