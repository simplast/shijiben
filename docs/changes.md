## Change Impact List — 时间去向聚合（Heatmap 3-tab + TimeAllocation）

> Based on AGENT.md context and spec `docs/superpowers/specs/2026-06-29-time-allocation-design.md`

### New Files
| File Path | Purpose | Key Dependencies |
|-----------|---------|-------------------|
| `app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationCalculator.kt` | object 纯函数：按标题聚合 completed 事件时长，范围过滤（周/月/年/全部），降序返回 | `EventEntity`, `EventStatus`, java.time |
| `app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationViewModel.kt` | @HiltViewModel：管理 `_selectedRange` + `state: StateFlow<TimeAllocationUiState>`，注入 EventRepository + Clock | `EventRepository`, `Clock`(TimeVizModule), `TimeAllocationCalculator` |
| `app/src/main/java/com/shijiben/feature/heatmap/TimeAllocationTab.kt` | Composable：范围选择器（4 PixelOutlinedButton）+ 水平条形图列表 | `TimeAllocationViewModel`, `PixelOutlinedButton`, theme tokens |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapMonthTab.kt` | Composable：提取自现有 HeatmapScreen 主体（MonthSwitcher + WeekHeader + HeatmapGrid + Legend） | `HeatmapViewModel`, 现有 heatmap 私有组件 |
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearTab.kt` | Composable：提取自现有 HeatmapYearScreen 主体（YearSwitcher + 12 月迷你月历拼贴） | `HeatmapYearViewModel`, 现有 year 组件 |
| `app/src/test/java/com/shijiben/feature/heatmap/TimeAllocationCalculatorTest.kt` | 纯 JUnit 单测：9 个测试用例覆盖范围过滤/聚合/排序/边界 | `TimeAllocationCalculator`, `EventEntity` |
| `app/src/test/java/com/shijiben/feature/heatmap/TimeAllocationViewModelTest.kt` | ViewModel 单测：selectRange + 初始加载 | `TimeAllocationViewModel`, fake EventRepository + Clock |

### Modified Files
| File Path | Change Type | What Changes | Risk Level |
|-----------|-------------|--------------|------------|
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapScreen.kt` | refactor | 重构为 tab 容器：移除 MonthSwitcher/HeatmapGrid/Legend 等主体（迁移到 HeatmapMonthTab），新增 TabRow 切换栏 + when 分支渲染 3 tab；移除 `onYearClick` 参数 | high |
| `app/src/main/java/com/shijiben/navigation/AppNavHost.kt` | refactor | 移除 `Routes.HEATMAP_YEAR` 常量；移除 `HeatmapYearScreen` import；移除 HEATMAP_YEAR composable；移除 HeatmapScreen 的 `onYearClick` 传参 | medium |
| `app/src/main/java/com/shijiben/data/repository/EventRepository.kt` | add | 若无 `getAllEvents()` 返回 `Flow<List<EventEntity>>`，则补充（预计已有，需确认） | low |

### Deleted Files
| File Path | Reason |
|-----------|--------|
| `app/src/main/java/com/shijiben/feature/heatmap/HeatmapYearScreen.kt` | 内容迁移到 HeatmapYearTab.kt，年视图改为 HeatmapScreen 内 tab |

### API Changes
| Endpoint | Method | Change | Notes |
|----------|--------|--------|-------|
| N/A（纯本地 app，无 API） | — | — | — |

### Dependency / Config Changes
| Item | Change | Notes |
|------|--------|-------|
| 无 | — | 复用现有依赖（Hilt / Coroutines / Room），无新增依赖 |

### 风险评估

- **高风险**：HeatmapScreen 重构为 tab 容器——需保持月/年视图功能完全不变，仅改变入口方式（从按钮跳转改为 tab 切换）
- **中风险**：AppNavHost 移除 HEATMAP_YEAR 路由——需确认无其他地方引用该路由
- **低风险**：新增 Calculator/ViewModel/Tab——纯新增，不破坏现有功能
