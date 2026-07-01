# Direction report — cycle 23
Date: 2026-07-02

## Theme: From daily recording to lifelong reflection — bridging years, preserving memory, deepening the quiet glance

## Context: what exists now

V1/V2/V3 all landed. Current surfaces:

- **Timeline** (day) — home, with date nav, `DayProgressBar`, events + notes inline, bottom dual-block entry drawer.
- **Recording** — event create/edit, status flow `not_started → in_progress → completed`, auto-deferred to next day.
- **Heatmap** — 3 tabs: 月 (month grid) / 年 (year mini-cal mosaic) / 去向 (time allocation by title; ranges: week/month/year/all).
- **TimeViz** — 今天 / 今年 / 一生 (progress bars + lifespan stepper).
- **Notes** — 随笔 list + editor.
- **Search** — LIKE full-text across `events.title`/`events.note` + `notes.content`.
- **Settings** — JSON export/import (SAF), about, privacy.

## What cycle 13 already proposed (not repeated here)

- **Week heatmap view** — still pending (heatmap has 月/年/去向, no 周 tab). Valid, complementary, left for the user to pick up.
- **Time allocation by title** — **DONE** (cycle 22, `TimeAllocationTab`).
- **Silent last-backup-time display** — still pending. Valid, complementary.

This cycle proposes **new** directions only.

---

## Suggestions

### 1. 记忆簿 / Memory Book Export (human-readable, app-independent)
- **What**: Beyond the current JSON export (machine-only, unreadable without this exact app), generate a human-readable Markdown (or HTML) "memory book" — one document per year or custom date range — weaving events and notes into a chronological narrative: date headings, event lines with times + durations, notes inline by timestamp. Saved via the existing SAF flow. Printable, archivable, survives the app.
- **Why**: Pure-local + no-cloud means data longevity rests *entirely* on the user. JSON dies with the app; a printable year-book lets the record outlive the app. For a tool meant to span decades this is existential, not nice-to-have. It honors "记录即审视" — you can hold your year in your hands — and aligns with "轻量存在" (optional, on-demand, never pushed). Infrastructure (`DataExportManager`, SAF) already exists; this adds a second serializer alongside `buildJsonString`.
- **Effort**: M
- **Risk**: low

### 2. 往年今日 / Time Capsule ("On This Day")
- **What**: After ≥1 year of use, a quiet "往年今日" card appears on the home timeline, showing events & notes from 1/2/3… years ago on today's month+day. Tap to expand, or jump to that past date's full timeline view. No card renders when no prior-year data exists for that date — so a new user sees nothing, no empty placeholders.
- **Why**: This is a *lifelong* tool; its deepest value emerges across years, not days. "看见时间去向" over the arc of years is "记录即审视" at its highest — revisiting past selves. Today nothing bridges year-over-year: the year heatmap shows *coverage*, not *what you were doing* a year ago today. Non-anxious by design — it only shows what was, never what "should" be. The original goal statement ("一段长期的记录会让我们自然的意识到问题在哪儿") is exactly this: long records surface patterns gently, without being told.
- **Effort**: M (new date-range query across past years for same month+day; small reusable card on timeline; reuse existing `heatmap_target_date` cross-screen jump mechanism)
- **Risk**: low

### 3. 今日小结卡 / Quiet Day-Summary Card (factual, on home)
- **What**: A small, dismissible factual card at the bottom of *today's* timeline: "今天 · N 件事 · Xh 已记录 · M 条随笔 · 去向：阅读 1h、工作 3h …". Reuses `TimeAllocationCalculator` + existing counts. Only renders for "today" and only when there's data. Strictly no streaks, no goals, no comparison to past days, no scoring.
- **Why**: The original goal is "回顾一周自己的生活" — but回顾 starts with回顾一天. Today's `StatsText` already shows raw counts ("N 件事 · M 条随笔"); a slightly richer factual wrap deepens "看见时间去向" without crossing into evaluation. "轻量存在" — it just sits there; glance if you want, dismiss if you don't. No new screen, no new navigation — it densifies the home the user already opens.
- **Effort**: S
- **Risk**: low — but the boundary is sharp: it must stay strictly factual. Any "比昨天多/少" or streak framing must be rejected in review, or it slips toward TODO/anxiety.

### 4. 像素日重放 / Pixel Day Replay
- **What**: A "▶ 重放" affordance on the timeline that animates the day's events filling onto the `DayProgressBar` at accelerated speed (e.g. ~10s = 24h), with a sweeping "now" cursor and event titles popping in as their start-times arrive. Pure visualization — reads the same data, changes nothing. 8-bit NES-cutscene aesthetic; chime optional.
- **Why**: "看见时间去向" is currently *static*. A replay offers an embodied, temporal experience of how a day *unfolded* — you watch the day fill in, see the gaps, see the rhythm. It fits the 8-bit identity (NES cutscenes / demoscene) better than any other feature could. Pure observation, no judgment. Bold and novel — nothing else in the space does this.
- **Effort**: M-L (Compose animation: timed progress sweep + staggered event pop-in; pausable; must respect Compose performance on long event lists)
- **Risk**: medium — novelty/effort, and it must not feel gimmicky. Should default to opt-in / behind a tap so it never becomes noise on every screen open.

### 5. 可缩放字体 / Accessibility — Scalable Font Mode
- **What**: The 8-bit pixel font (Fusion Pixel 12px) is fixed and ignores Android's font scale. Add a setting (default off) to swap to a system sans-serif family that respects `sp` scaling. Pixel font stays the default identity; the scalable mode is opt-in. Applies app-wide via the existing `AppTheme` typography hook.
- **Why**: A *lifelong* tool must age with its user. Over decades, eyesight changes; the fixed 12px pixel font that feels charming at 33 may become unreadable at 63. Accessibility here isn't a "feature" — it's inclusion, and it's the only thing standing between the user and "I can't read my own record anymore". Aligns with "面向你个人内心的软件" — your software should adapt to you, not the reverse. No philosophy conflict: it changes presentation, not behavior.
- **Effort**: S-M (one setting flag; `AppTheme` branches typography; audit hardcoded `sp` usages — most already use `sp`, the pixel font's fixed 12px is the main thing to override)
- **Risk**: low

---

## Explicitly excluded (philosophy guardrails, carried from cycle 13)

- **Cloud sync / accounts / community / sharing** — violates "纯本地、不联网、无社区".
- **Push notifications / reminders** — violates "没有提醒功能".
- **Streaks / 连续记录天数** — slips toward 打卡, manufactures anxiety, violates "不评判".
- **Tags / categories** — removed 2026-06-27 by user decision ("对纯记录是负担").
- **Review templates / guided journaling** — violates "不强迫复盘".
- **AI analysis / insights / suggestions** — AI suggestion = judgment, violates "App 反映现实，不评判现实".

## Summary

Five new directions, all respecting the four philosophy anchors (记录即审视 / 轻量存在 / 不是 TODO / 纯本地无提醒). Ranked by value-for-effort:

1. **Memory Book export** (M, low) — most essential: a local-only lifelong tool must let its record outlive the app. Highest urgency.
2. **On This Day** (M, low) — deepest *reflective* value, unlocks after a year of use; lay the groundwork now so value compounds.
3. **Quiet Day-Summary Card** (S, low) — cheapest, densifies the home the user already opens; sharp factual boundary required in review.
4. **Scalable Font** (S-M, low) — inclusion for a tool meant to span decades; cheap insurance.
5. **Pixel Day Replay** (M-L, medium) — the bold/novel pick; fits the 8-bit identity uniquely, but should be opt-in.

Cycle 13's still-pending week view and silent backup-time display remain valid and complementary; they are not repeated here.
