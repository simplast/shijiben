## 0. 准备工作

- [x] 0.1 备份当前 HomeScreen.kt 及主题文件，确认 `./gradlew assembleDebug` 可正常编译
- [x] 0.2 在 `ui/theme/` 下创建新文件占位：`PixelIcons.kt`、`PixelComponents.kt`、`PixelBorder.kt`

## 1. 色彩系统重构：海蓝系夏日色板

- [x] 1.1 重写 `ui/theme/Color.kt`：新增海蓝系主色（SeaBlue / LightSeaBlue / MintBlue / MistBlue）
- [x] 1.2 新增点缀色（SunYellow / CoralOrange）和特殊色（LightPink）
- [x] 1.3 更新基础色：CreamWhite（奶油白背景）、DeepTeal（深青蓝替代纯黑）
- [x] 1.4 更新暖灰阶（WarmGray50/100/200/30/500）以匹配新基调
- [x] 1.5 为每个主色增加明暗变体（Dark/Light），用于按压态/悬停态
- [x] 1.6 统一语义色 Token（Success / Warning / Error / Info / Active / Pending）
- [x] 1.7 验证编译通过：`./gradlew assembleDebug`

## 2. 双线边框系统

- [x] 2.1 在 `PixelBorder.kt` 实现 `doublePixelBorder()` Modifier，支持外线颜色/宽度 + 内线颜色/宽度
- [x] 2.2 定义三级边框常量：Primary（2+1dp）、Secondary（1.5+0.5dp）、Tertiary（1+0.5dp）
- [x] 2.3 在简单组件上验证双线边框效果（如 Box 容器）
- [x] 2.4 验证编译通过

## 3. 像素图标库（Canvas 自绘）

- [x] 3.1 新建 `PixelIcons.kt`，定义图标组件框架
- [x] 3.2 实现 Play（右向三角形）图标
- [x] 3.3 实现 Stop（实心方块）图标
- [x] 3.4 实现 Close/Delete（X 形）图标
- [x] 3.5 实现 Check（对勾折线）图标
- [x] 3.6 实现 Add（加号）图标
- [x] 3.7 实现 More（竖排三点）图标
- [x] 3.8 实现 Refresh（环形箭头，简化版）图标
- [x] 3.9 实现 Diamond（空心菱形）图标
- [x] 3.10 实现 Pause（两条竖线）图标
- [x] 3.11 实现 Calendar（方块+横线）图标
- [x] 3.12 实现 Star（简化五角星）图标
- [x] 3.13 统一定义图标尺寸规范（大/中/小）
- [x] 3.14 验证编译通过

## 4. 像素组件库

- [x] 4.1 在 `PixelComponents.kt` 实现 `PixelCard` 组件，支持 Primary/Secondary/Tertiary 三级
- [x] 4.2 实现 `PixelButton`（文字按钮）
- [x] 4.3 实现 `PixelIconButton`（图标按钮）
- [x] 4.4 实现 `PixelSectionHeader`（区块标题，彩色短线 + 文字）
- [x] 4.5 实现 `PixelBadge`（徽章/标签）
- [x] 4.6 实现 `PixelInput`（输入框，基于 BasicTextField + 双线边框）
- [x] 4.7 实现 `PixelDialog`（对话框）
- [x] 4.8 统一按压反馈：颜色翻转 + 轻微缩放
- [x] 4.9 验证编译通过

## 5. 首页重设计

- [x] 5.1 `TodayOverviewCard` 改用 `PixelCard` + 海蓝背景 + 双线边框
- [x] 5.2 概览卡右侧徽章改用 `PixelBadge`，调整色彩（珊瑚橙已完成 / 浅海蓝待办）
- [x] 5.3 `ActiveEventCard` 改用 `PixelCard` + 淡粉背景 + 双线边框
- [x] 5.4 进行中卡"结束"按钮改用 `PixelButton`（海蓝色）
- [x] 5.5 闪烁指示灯颜色改为 SunYellow（日光黄），保持帧式动画
- [x] 5.6 `CompletedSection` 改用 `PixelCard` + 薄荷蓝背景 + 双线边框
- [x] 5.7 已完成行操作按钮改用像素图标（Refresh / Delete），尺寸增大到 28dp
- [x] 5.8 已完成行左边色条更新为新色板（5 色轮换）
- [x] 5.9 `PendingSection` 改用 `PixelCard` + 奶油白背景 + 双线边框
- [x] 5.10 待办行播放按钮改用 `PixelIconButton`，尺寸增大到 32dp
- [x] 5.11 待办行删除按钮改用像素 X 图标，与播放按钮视觉对称
- [x] 5.12 推荐标签改用 `PixelBadge`
- [x] 5.13 Section 标题改用 `PixelSectionHeader`，增加中文标题
- [x] 5.14 顶栏日历图标和更多图标改用像素图标
- [x] 5.15 底部输入栏改用 `PixelInput`
- [x] 5.16 下拉菜单（MoreVert）改用像素风格
- [x] 5.17 调整整体间距节奏，确保双线边框下视觉平衡
- [x] 5.18 验证编译通过

## 6. 其他页面适配

- [x] 6.1 `WeeklyReviewScreen` 应用新组件和色板
- [x] 6.2 `MonthlyReviewScreen` 应用新组件和色板
- [x] 6.3 `EventEditorScreen`（底部 sheet）应用新组件和色板
- [x] 6.4 `DayPickerDialog` 应用新组件和色板
- [x] 6.5 验证各页面风格统一

## 7. 动画与交互细节打磨

- [x] 7.1 列表项进入动画（帧式淡入）
- [x] 7.2 按钮按压态优化（颜色翻转 + 缩放）
- [x] 7.3 状态切换动画（待办 → 进行中 → 已完成）
- [x] 7.4 数字变化动画（计时数字跳变效果）
- [x] 7.5 验证动画流畅且不破坏像素感

## 8. 走查与修正

- [x] 8.1 全局走查：检查所有页面的圆角、阴影、渐变是否已清除
- [x] 8.2 检查所有图标是否都已替换为像素风格
- [x] 8.3 检查边框是否统一使用双线边框体系
- [x] 8.4 检查色彩是否符合海蓝系夏日基调
- [x] 8.5 检查中文排版与像素字体的搭配
- [ ] 8.6 不同屏幕尺寸下的验证（小屏/大屏）
- [x] 8.7 最终编译验证：`./gradlew assembleDebug`
- [ ] 8.8 真机/模拟器视觉验收
