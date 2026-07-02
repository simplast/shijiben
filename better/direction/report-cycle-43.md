# Direction report — cycle 43
Date: 2026-07-02
Rotation 3 closing. Final cycle of the 30-round unattended run.

## Theme: The lifelong mirror — annual ritual, body rhythm, temporal echoes, intentional futures, ambient presence

## What cycles 13 / 23 / 33 already proposed (not repeated)

- Week heatmap view (cycle 13, still pending — heatmap has 月/年/去向 tabs, no 周)
- Silent last-backup-time display (cycle 13, still pending)
- Time allocation by title (cycle 13 → **shipped cycle 29** as TimeAllocationTab)
- Memory Book export — human-readable (cycle 23, pending)
- On This Day / time capsule surfacing (cycle 23, pending)
- Quiet day-summary card (cycle 23, pending)
- Pixel day replay (cycle 23, pending)
- Scalable font / accessibility (cycle 23, pending)
- Data Health Dashboard (cycle 33, pending)
- Heatmap Palette Switcher (cycle 33, pending)
- Time Blind Box — random past day (cycle 33, pending)
- Device Migration Helper (cycle 33, pending)
- On-Device Voice Note (cycle 33, pending)

This cycle proposes **new** directions only, leaning into the user's "大胆/脑暴" directive for the final round.

---

## Suggestions

### 1. 年度时间肖像 / Annual Time Portrait (year-end auto-generated pixel poster)

- **What**: 每年 12 月 31 日（或用户在年底任意时刻主动触发），app 自动生成一张 8-bit 风格的"年度肖像"——一张可导出的像素海报，编码这一年的记录：365 日热力图缩略 + Top 5 时间去向条形 + 总记录时长 + 有记录天数。纯本地 PNG 导出（复用 DataExportManager 的 SAF 写入）。无推送、无提醒，只是设置页多一个"生成 [年] 年肖像"入口，年底那天首页顶部静默出现一个可点的像素礼盒。
- **Why**: 一年结束时，"看见时间去向"应该有一个仪式感的收束。这不是报告卡（不评分、不对比目标），是一张可以保存/设壁纸的纪念物——把数据变成可触摸的记忆。强烈呼应 8-bit 美学（NES 时代年末存档画面的现代回响），也呼应"一生"视角下的年度刻度。比 cycle 23 的 Memory Book 更轻、更视觉、更瞬时——Memory Book 是文字的厚礼，Portrait 是一图的速写。
- **Effort**: M（纯函数从已有 aggregateYear + TimeAllocation 取数；Canvas 绘制像素海报；PNG 导出复用 SAF；一个新 Composable + 一个 Settings 入口）
- **Risk**: low — 纯只读 + 导出，零数据修改。边界：不生成"年度关键词"或"年度总结评语"（那是 AI 评判），只有事实可视化。

### 2. 本地生物钟图谱 / Circadian Fingerprint (factual rhythm mirror)

- **What**: 设置页或时间可视化页新增"我的生物钟"子屏：一个 0-23h 的水平条形图，纵轴是该小时累计记录的事件数 / 时长，由所有 completed 事件聚合而来。纯静态事实图——"你的时间生物钟长这样"。无建议（不说"你熬夜太多"），无对比（不与"健康作息"标准比）。
- **Why**: 长期记录的副产品是浮现个人节律。当前所有回看都是按日/月/年切分，没有一个"按小时分布"的视角。这是"记录即审视"对自身节律的审视——很多人记了半年后才发现"原来我下午 3 点几乎从不记事"。纯事实，最强的不评判边界。实现成本极低（一个 groupBy hour 纯函数 + 一个条形图，复用 TimeAllocationTab 的条形图组件）。
- **Effort**: S（一个 `circadianDistribution(events): List<HourBucket>` 纯函数 + 复用现有条形图 Composable）
- **Risk**: low — 唯一风险是 UI 上误加"建议早睡"之类评语，review 时严格拒绝。

### 3. 去年的今天 / Then & Now dual-pane (temporal echo, on-demand)

- **What**: 时间轴首页，长按"今天"日期标签 → 弹出上下分屏：上半屏显示去年的今天的事件+随笔（若存在），下半屏显示今天。若去年无记录则上半屏显示一句"去年的今天还是空白"。纯手动触发，不主动弹。
- **Why**: cycle 23 的 "On This Day" 是 app 主动 surface；本方向是**用户主动发起**的回望——更轻、更克制。长年使用后，"去年的今天我在做什么"是时间记录独有的情感回响。零提醒、零推送，只在用户想看时出现。比 On This Day 更符合"轻量存在"。
- **Effort**: S-M（新增一个 DAO 查询 `getEventsByDate(lastYear)` + 一个分屏 Composable；复用 EventList 渲染）
- **Risk**: low — 纯只读。注意：不显示"去年今天 vs 今天的时长对比"（那是评判），只并列展示两条事实。

### 4. 时间胶囊信件 / Time Capsule Letter (intentional future-self note)

- **What**: 随笔编辑器新增"封存至"选项——写一封随笔，设定"启封日期"（如 1 年后 / 5 年后 / 某个具体日期）。封存后该随笔在启封日期前**不可读**（列表中显示为锁定的像素信封 🔒，不可点开），到期自动解锁。纯本地，无提醒——到期不通知，只是用户哪天打开列表会看到它"开了"。
- **Why**: 与 cycle 23/33 的回望类方向相反——这是**面向未来的刻意书写**。时间记录通常是"现在记、过去看"，时间胶囊增加"现在写、未来自己读"的维度。强烈呼应"一生"尺度（5 年后的自己读今天的信）。封存而非删除的设计让它不同于普通随笔。情感浓度高，但完全可选、零打扰——不封存就当普通随笔用。
- **Effort**: M（NoteEntity 加 `sealedUntil: Long?` 字段 + DB migration v2→v3；NoteDao 查询过滤 `sealedUntil > now` 的内容但保留可见的锁条目；NoteEditorSheet 加日期选择器；NotesScreen 加锁定态渲染）
- **Risk**: medium — 主要是 DB migration 风险 + "封存不可读"的加密强度（纯本地无加密，理论上可被 adb 读取——需在 UI 明确"封存仅为 UI 级别，非加密"，避免给用户虚假安全感）。哲学上安全：是用户主动选择封存，不是 app 替用户决定。

### 5. 环境像素屏保 / Ambient Pixel Kiosk (quiet always-on presence)

- **What**: 设置页新增"环境模式"入口 → 进入全屏沉浸视图：缓慢自动滚动的一生时间块网格（TimeViz 的"一生"视图放大版），背景配 8-bit 暗色调，可作为桌面摆件/充电时的环境屏。无任何输入入口，点击屏幕退出。可选开启"当前小时高亮"——在一生网格上脉动当前位置。
- **Why**: "时间可视化静静在那"的物理化。当前所有视图都需要主动打开 app 切 tab；环境模式让时间可视化成为空间里的存在——像一座数字时钟，但是你自己的一生。极具 8-bit 感（NES 启动画面的沉静版）。最"大胆"的一选，因为最不像功能、最像艺术品。
- **Effort**: M（复用 TimeViz 一生网格 Canvas + 自动滚动动画 + 沉浸全屏 flag；无新数据）
- **Risk**: medium-high — (1) 耗电/烧屏（OLED），需文档化"建议充电时使用"+ 限制亮度；(2) 可能被视为"花哨"，但用户明确说"大胆一点"。若实现，必须保证零通知、零后台——只是打开 app 时的一个全屏视图，退出即止。

---

## Explicitly excluded (philosophy guardrails, carried forward from 13/23/33)

- **Cloud sync / accounts / community / sharing** — violates 纯本地、不联网、无社区.
- **Push notifications / reminders** — violates 没有提醒功能.
- **Streaks / 连续记录天数** — slips toward 打卡, manufactures anxiety, violates 不评判.
- **Tags / categories** — removed 2026-06-27 by user decision (对纯记录是负担).
- **Review templates / guided journaling** — violates 不强迫复盘.
- **AI analysis / insights / suggestions** — AI suggestion = judgment, violates App 反映现实不评判现实.
- **Quick-record templates** — re-introduces implicit categorization.

## Summary

Five new directions, all respecting the four philosophy anchors (记录即审视 / 轻量存在 / 不是 TODO / 纯本地无提醒). Ranked by value-for-effort:

1. **Circadian Fingerprint** (S, low) — cheapest factual mirror of an uncovered dimension (hour-of-day). Ship first; pure function + reused bar chart.
2. **Then & Now dual-pane** (S-M, low) — user-initiated temporal echo; lighter than cycle 23's On This Day; high emotional payoff per effort.
3. **Annual Time Portrait** (M, low) — year-end ritual; uniquely 8-bit; turns data into a memento. Pair with Memory Book (cycle 23) for text+image year-end pair.
4. **Time Capsule Letter** (M, medium) — introduces future-facing dimension; highest novelty; requires DB migration + honest "UI-level seal" messaging.
5. **Ambient Pixel Kiosk** (M, medium-high) — boldest, most art-like; literalizes "静静在那"; manage OLED/battery risk or drop.

Cycles 13/23/33's still-pending items (week view, silent backup-time, memory book, on-this-day surfacing, day-summary card, pixel replay, scalable font, data health dashboard, palette switcher, blind box, migration helper, voice note) remain valid and complementary; not repeated here.

---

## 30-round rotation closing note

This completes the user-requested 30-round unattended rotation (cycles 14–43, three full passes over correctness → security → performance → ux → ui → architecture → tests → dx → docs → direction). Across the rotation, the codebase gained: 4 correctness bug fixes, 4 security hardening passes, performance wins (buildYearGrid O(12N)→O(N), nowState draw-phase), UX refinements (date-aware empty states), UI consistency (PixelCard 2dp black standard, legacy alias purge), architecture DRY (util.DateUtils), test coverage (determineStatus全分支, TimeVizCalculator, TimeAllocationTab), dx (shared MainCoroutineRule), and docs (状态机 §8, util/ 记录). The direction category produced 15 cumulative suggestions across 3 reports; none are auto-implemented — all await user decision.
