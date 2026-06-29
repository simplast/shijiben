## Acceptance Test Document — 时间去向聚合（Heatmap 3-tab + TimeAllocation）

> **How to use:** Follow each test case (TC-xx) step by step. Mark each as PASS or FAIL.
> **Prerequisites:** 安装 debug APK 到已连接设备/模拟器；预置测试数据（若干 completed 事件，标题含"阅读"/"运动"/"工作"等，时间分布在本周/本月/本年/历史）

### 测试数据准备

在时间轴页通过底部"记事"入口创建以下事件（标题 + 起止时间）：
- 事件 A：标题"阅读"， startTime=今天 09:00，duration=2h（completed）
- 事件 B：标题"运动"， startTime=今天 18:00，duration=45m（completed）
- 事件 C：标题"阅读"， startTime=昨天 10:00，duration=1h（completed，用于验证同标题聚合）
- 事件 D：标题"工作"， startTime=上周 09:00，duration=8h（completed，用于验证本周范围排除）
- 事件 E：标题"进行中事项"，startTime=现在，duration=1h（in_progress，用于验证不聚合非 completed）

---

### TC-01 热力图页显示 3 tab 切换栏
| Item | Content |
|------|---------|
| **Route** | 启动 app → 首页（时间轴）→ 顶栏点热力图入口进入 `heatmap` 路由 |
| **Steps** | 1. 启动 app，进入时间轴首页<br>2. 点击顶栏热力图入口（放大镜旁）<br>3. 观察热力图页顶部 |
| **Expected** | 顶栏下方显示 3 个 tab：「月」「年」「去向」，默认选中「月」tab，月视图（MonthSwitcher + 热力图网格 + Legend）可见 |
| **Notes** | tab 切换栏样式：选中=Primary 背景+白字，未选中=Surface 背景+TextPrimary |

---

### TC-02 切换到「年」tab 显示年视图
| Item | Content |
|------|---------|
| **Route** | `heatmap`（已进入） |
| **Steps** | 1. 点击「年」tab<br>2. 观察页面主体 |
| **Expected** | 主体显示年视图（YearSwitcher + 12 月迷你月历拼贴），与重构前 HeatmapYearScreen 视觉一致；年视图点日期方块仍能 popBackStack 回时间轴并跳转选中日期 |
| **Notes** | 验证年视图功能完全不变，仅入口从按钮跳转改为 tab 切换 |

---

### TC-03 切换到「去向」tab 显示聚合页
| Item | Content |
|------|---------|
| **Route** | `heatmap`（已进入） |
| **Steps** | 1. 点击「去向」tab<br>2. 观察页面主体 |
| **Expected** | 主体显示去向页：顶部 4 个范围选择器（本周/本月/本年/全部，默认选中「本月」）+ 下方水平条形图列表；列表按总时长降序排列，每行显示标题+像素方块条+时长 |
| **Notes** | 若本月有数据则显示列表；若无数据则显示空状态 |

---

### TC-04 本月范围聚合验证
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「去向」tab |
| **Steps** | 1. 确认选中「本月」<br>2. 观察列表 |
| **Expected** | 列表显示本月 completed 事件按标题聚合：事件 A+C 标题相同"阅读"应合并为一行，总时长=3h（2h+1h）；事件 B"运动"单独一行=45m；事件 D"工作"（上周）不在本月范围不显示；事件 E（in_progress）不显示；列表降序：阅读(3h) > 运动(45m) |
| **Notes** | 验证同标题聚合 + 范围过滤 + 排除非 completed + 降序排列 |

---

### TC-05 切换到「本周」范围
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「去向」tab |
| **Steps** | 1. 点击「本周」按钮<br>2. 观察列表变化 |
| **Expected** | 列表更新为本周 completed 事件聚合：事件 A"阅读"2h + 事件 B"运动"45m + 事件 C"阅读"1h（若昨天在本周内）；事件 D"工作"（上周）不在本周不显示；同标题"阅读"合并=3h；降序：阅读(3h) > 运动(45m) |
| **Notes** | 范围切换实时更新列表；按钮选中态变为 Primary 背景 |

---

### TC-06 切换到「本年」范围
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「去向」tab |
| **Steps** | 1. 点击「本年」按钮<br>2. 观察列表 |
| **Expected** | 列表显示本年所有 completed 事件聚合：包含"阅读"/"运动"/"工作"等本年事件；事件 E（in_progress）不显示；降序排列 |
| **Notes** | 范围比本月更大，可能显示更多标题 |

---

### TC-07 切换到「全部」范围
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「去向」tab |
| **Steps** | 1. 点击「全部」按钮<br>2. 观察列表 |
| **Expected** | 列表显示所有历史 completed 事件聚合，无时间过滤；同标题合并求和；降序排列 |
| **Notes** | 验证 ALL 范围不过滤 |

---

### TC-08 水平条形图视觉验证
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「去向」tab → 任意范围 |
| **Steps** | 1. 观察每行条形图<br>2. 对比不同标题的条形宽度 |
| **Expected** | 每行：标题（左）+ 像素方块条（中）+ 时长（右）；条形宽度按 `totalMs / maxMs` 占比填充（最长项=100%）；前景 Primary 色，背景 Surface 色；8-bit 直角风格；时长格式 `Xh Ym`（hours=0 时只显示分） |
| **Notes** | 验证视觉与 spec 一致 |

---

### TC-09 月 tab 功能回归验证
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「月」tab |
| **Steps** | 1. 切换回「月」tab<br>2. 点击 MonthSwitcher 的 ‹ / › 切换月份<br>3. 点「本月」按钮回当前月<br>4. 点击热力图某日期方块 |
| **Expected** | 月视图功能与重构前完全一致：月份切换正常；点击日期方块 popBackStack 回时间轴并跳转到选中日期；MonthSwitcher 视觉不变 |
| **Notes** | 验证重构未破坏月视图功能 |

---

### TC-10 年 tab 功能回归验证
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「年」tab |
| **Steps** | 1. 切换到「年」tab<br>2. 点击 YearSwitcher 的 ‹ / › 切换年份<br>3. 点击年视图中某日期方块 |
| **Expected** | 年视图功能与重构前完全一致：年份切换正常；点击日期方块 popBackStack 回时间轴并跳转；12 月迷你月历拼贴视觉不变 |
| **Notes** | 验证重构未破坏年视图功能 |

---

### TC-11 导航回归：HEATMAP_YEAR 路由已移除
| Item | Content |
|------|---------|
| **Route** | `heatmap` |
| **Steps** | 1. 进入热力图页<br>2. 观察顶栏 |
| **Expected** | 顶栏无"年"跳转按钮（原 MonthSwitcher 内的"年"按钮已移除，年视图改为 tab）；返回按钮正常 popBackStack |
| **Notes** | 验证 HEATMAP_YEAR 路由废弃后导航正常 |

---

### TC-12 空数据状态
| Item | Content |
|------|---------|
| **Route** | `heatmap` →「去向」tab |
| **Steps** | 1. 清空数据库（adb shell pm clear com.shijiben）<br>2. 进入热力图「去向」tab<br>3. 切换各范围 |
| **Expected** | 列表为空时显示空状态（如"暂无记录"或空白，spec 未严格定义空状态文案，按实现验证） |
| **Notes** | 验证空数据不崩溃 |

---

### Appendix

#### 路由汇总
| Route | Tab | 说明 |
|-------|-----|------|
| `timeline` | — | 首页，点热力图入口跳 `heatmap` |
| `heatmap` | 月/年/去向 | 3 tab 容器，HEATMAP_YEAR 已废弃 |
| `heatmap` → 月 tab | — | 默认 tab，月视图热力图 |
| `heatmap` → 年 tab | — | 年视图 12 月拼贴 |
| `heatmap` → 去向 tab | — | 时间去向聚合（范围选择器+条形图） |

#### 状态流
```
not_started → in_progress → completed
              (endTime=null)  (有 endTime，参与聚合)
```
去向聚合只包含 `completed` 事件（InProgress/NotStarted 无确定时长，不聚合）。

#### 测试数据矩阵
| 事件 | 标题 | 时间 | 状态 | 本周 | 本月 | 本年 | 全部 |
|------|------|------|------|------|------|------|------|
| A | 阅读 | 今天 09:00, 2h | completed | ✓ | ✓ | ✓ | ✓ |
| B | 运动 | 今天 18:00, 45m | completed | ✓ | ✓ | ✓ | ✓ |
| C | 阅读 | 昨天 10:00, 1h | completed | ✓(若本周) | ✓ | ✓ | ✓ |
| D | 工作 | 上周 09:00, 8h | completed | ✗ | ✗ | ✓(若本年) | ✓ |
| E | 进行中事项 | 现在, 1h | in_progress | ✗(不聚合) | ✗ | ✗ | ✗ |
