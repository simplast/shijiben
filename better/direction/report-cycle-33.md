# Direction report — cycle 33
Date: 2026-07-02
Rotation 2 closing.

## Theme: Hardening the lifelong promise — data integrity, personalization, serendipity, friction reduction

## What cycles 13 & 23 already proposed (not repeated)

- Week heatmap view (cycle 13, still pending)
- Silent last-backup-time display (cycle 13, still pending)
- Memory Book export — human-readable (cycle 23)
- On This Day / time capsule (cycle 23)
- Quiet day-summary card (cycle 23)
- Pixel day replay (cycle 23)
- Scalable font / accessibility (cycle 23)

This cycle proposes **new** directions only, leaning into the user's "大胆/脑暴" directive.

---

## Suggestions

### 1. 数据健康度 / Data Health Dashboard (factual, opt-in, Settings)
- **What**: A quiet diagnostic screen in Settings showing strictly factual metrics: 最早记录日期、累计事件数、累计随笔数、有记录天数 / 覆盖率（vs 注册天数）、最长空白段（连续无记录天数）、本年/本月记录概览。No streaks, no scoring, no "should". Just: "你的记录现状是这样".
- **Why**: A *lifelong* pure-local tool's biggest risk is silent data loss / gaps the user never notices. Today the user has no way to see "我中间断了 3 周没记" or "我其实只覆盖了 40% 的天数". This is *记录即审视* turned on the recording itself — not judging, just mirroring. Strict factual boundary (no comparison, no goal) keeps it clear of TODO/anxiety. Cheapest possible integrity insurance.
- **Effort**: S-M (reuse existing `getAllEvents` / `getAllNotes` counts + a `GROUP BY date` query; pure function for gap detection; one new Settings sub-screen)
- **Risk**: low — but the boundary is sharp: any "目标覆盖率" or streak framing must be rejected in review.

### 2. 热力图调色板 / Heatmap Palette Switcher (aesthetic personalization)
- **What**: Today the heatmap uses a fixed green scale (`HeatmapLevel0..4`). Add 2-3 alternative 8-bit palettes (e.g. 烈日橙红 / 深海青蓝 / 经典绿) selectable in Settings. Each is a 5-color NES-palette gradient. Stored in `timeviz_prefs`-style SharedPreferences. Default stays 经典绿.
- **Why**: "看见时间去向" is the core reflective surface; letting the user pick its color is low-effort personalization that deepens ownership of the record. Pure aesthetic, zero behavior change, fully honors 8-bit identity (NES palettes are part of the aesthetic). A warm palette might read as "energy", a cool one as "calm" — the user picks what their time *feels* like to them.
- **Effort**: S (one Setting enum; `AppColors.HeatmapLevel0..4` becomes a function of the selected palette; ~5 new color tokens per palette)
- **Risk**: low

### 3. 时间盲盒 / Time Blind Box (playful 8-bit serendipity)
- **What**: Once per calendar month, if the user has ≥60 days of recorded data, a pixel "?" box appears quietly on the home timeline. Tap → reveals a random past day's full record (events + notes), chosen from days with ≥1 event. One reveal per month, then it disappears until next month. No schedule enforcement, no streak, no "you missed last month".
- **Why**: Pure serendipity. "记录即审视" usually means *you* choose to look back; the blind box flips it — *the app* surfaces a forgotten day, unprompted. Across years this becomes "oh, I forgot I did that". Extremely on-brand for 8-bit (mystery box / loot box trope, but wholesome). Honors "轻量存在" — appears once, dismissible, never nags. Bold and novel; nothing in the space does this.
- **Effort**: M (random seed from month+year; query a random day with ≥1 event; one composable card; SharedPreferences for "已开盒" flag per month)
- **Risk**: medium — must not feel gimmicky or pushy. Hard cap once/month, no countdown, no "next box in N days" timer. If the user never taps it, it should vanish silently at month end.

### 4. 换机迁移向导 / Device Migration Helper (pragmatic data longevity)
- **What**: A guided "换机迁移" wizard in Settings: Step 1 explains "纯本地，换机会丢数据"; Step 2 one-tap JSON export to a user-chosen SAF location; Step 3 shows a copyable "在新手机装本 app → 设置 → 导入 → 选这个文件" instruction. The exported filename includes a date stamp. No cloud, no accounts — just lowers the friction of the existing export+import.
- **Why**: The #1 real-world data-loss vector for a pure-local app is device replacement. Today export/import exists but is two separate, unguided actions. A 3-step wizard turns a scary "will I lose years of records?" moment into a 60-second routine. This is the pragmatic sibling of cycle 23's Memory Book — Memory Book preserves *meaning*; this preserves *the data itself*. Highest urgency for user retention across hardware cycles.
- **Effort**: S (orchestration of existing `DataExportManager` + `ImportViewModel` primitives; one new Settings sub-screen; static instruction text)
- **Risk**: low

### 5. 离线语音随笔 / On-Device Voice Note (friction reduction)
- **What**: In `NoteEditorSheet`, add a microphone affordance that uses Android's on-device `SpeechRecognizer` (offline-capable, no cloud transcript) to dictate note content into the text field. Recognizer choice is platform-dependent; if no on-device recognizer is available, the affordance is hidden (graceful degradation). No audio storage — only the transcribed text is saved.
- **Why**: Capturing a thought at 14:30 should be frictionless; typing on a phone is friction. Voice dictation (on-device only) honors "纯本地、不联网". Especially valuable for longer reflective notes where typing feels like work. Low philosophy risk — it's an input method, not a behavior change. Aligns with "面向你个人内心的软件" — meet the user where they are.
- **Effort**: M (`SpeechRecognizer` integration; microphone permission flow;实时 transcript display; graceful hide if unavailable; one new permission in Manifest)
- **Risk**: medium — must guarantee on-device-only path (some Android `SpeechRecognizer` configs phone home). If on-device can't be guaranteed on all devices, gate behind a Setting "仅离线语音" with clear messaging, or drop the feature. Permission ask must be contextual (only when mic tapped), never upfront.

---

## Explicitly excluded (philosophy guardrails, carried forward)

- **Cloud sync / accounts / community / sharing** — violates 纯本地、不联网、无社区.
- **Push notifications / reminders** — violates 没有提醒功能.
- **Streaks / 连续记录天数** — slips toward 打卡, manufactures anxiety, violates 不评判.
- **Tags / categories** — removed 2026-06-27 by user decision (对纯记录是负担).
- **Review templates / guided journaling** — violates 不强迫复盘.
- **AI analysis / insights / suggestions** — AI suggestion = judgment, violates App 反映现实不评判现实.
- **Quick-record templates** — considered this cycle, rejected: risks re-introducing "categories" through the back door (title templates become implicit categorization). Keep recording deliberately unstructured.

## Summary

Five new directions, all respecting the four philosophy anchors (记录即审视 / 轻量存在 / 不是 TODO / 纯本地无提醒). Ranked by value-for-effort:

1. **Device Migration Helper** (S, low) — most urgent: cheapest insurance against the #1 real data-loss vector. Ship first.
2. **Data Health Dashboard** (S-M, low) — factual integrity mirror; cheapest way to surface silent gaps. Strict factual boundary in review.
3. **Heatmap Palette Switcher** (S, low) — cheapest personalization; deepens ownership of the reflective surface.
4. **Time Blind Box** (M, medium) — the bold/serendipitous pick; uniquely on-brand for 8-bit. Must stay once/month and silently dismissible.
5. **On-Device Voice Note** (M, medium) — friction reduction; must guarantee offline-only path or be gated/dropped.

Cycles 13 & 23's still-pending items (week view, silent backup-time, memory book, on this day, day-summary card, pixel replay, scalable font) remain valid and complementary; not repeated here.
