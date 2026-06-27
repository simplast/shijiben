# Plan 013: Update AGENT.md, design spec, and product brief to reflect the tag removal

> **Executor instructions**: Follow this plan step by step. Run every
> verification command and confirm the expected result before moving to the
> next step. If anything in the "STOP conditions" section occurs, stop and
> report — do not improvise. When done, update the status row for this plan
> in `plans/README.md` — unless a reviewer dispatched you and told you they
> maintain the index.
>
> **Drift check (run first)**: `git diff --stat 9da5c72..HEAD -- AGENT.md docs/2026-06-22-shijiben-design.md MY_ORIGIN_GOAL.md`
> If any in-scope file changed since this plan was written, compare the
> "Current state" excerpts against the live file before proceeding; on a
> mismatch, treat it as a STOP condition.

## Status

- **Priority**: P2
- **Effort**: S
- **Risk**: LOW
- **Depends on**: none
- **Category**: docs
- **Planned at**: commit `9da5c72`, 2026-06-27

## Why this matters

The last 3 commits removed the entire Tag feature (deleted `TagEntity`, `TagDao`, `TagRepository`, `TagsScreen`, `TagsViewModel`, `TagEditorSheet`) and ran a Room DB v1→v2 migration that drops the `tags` table and the `events.tagId` column (see `AppDatabase.kt:24-52`). But three docs still describe tags as a core feature:

- `AGENT.md` is the first file any AI agent reads; it lists a `core/` package and `feature/tags/` that don't exist, a 3-table schema that's now 2 tables, and `core/theme` instead of `ui/theme`.
- The design spec at `docs/2026-06-22-shijiben-design.md` is the canonical reference linked from `AGENT.md`; it still defines a Tag entity, a section 六 "标签系统", and a `[x] 标签管理` checkbox marked done.
- `MY_ORIGIN_GOAL.md` (the product brief) lists "事件标签" as a should-have.

An agent or contributor reading any of these will propose "restore tags" or "fix the tags screen" — phantom work that wastes effort and conflicts with the documented user decision (per `docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md:25`: "C 是用户明确要求'标签功能整个删掉'"). Per the playbook, a stale decision doc is itself a finding; this plan reconciles the docs with the shipped code.

## Current state

### `AGENT.md`

The structure tree (lines 22-45) lists packages that don't exist:
- Line 28: `core/        # 主题、8-bit 调色板、共享组件` — no `core/` package; actual is `ui/theme/` (verified via `LS app/src/main/java/com/shijiben/`).
- Line 34: `core/        # 主题、8-bit 调色板、共享组件` (under `features/` heading) — duplicated, also wrong.
- Line 37: `tags/      # 标签管理` — deleted.
- Lines 38-39: `heatmap/`, `time_viz/` — V2 directories that don't exist yet (these are legitimately future, keep them but mark as "(v2 未建)").

The data model section (lines 47-63):
- Line 49: "三张核心表" — now **两张**.
- Line 51: `Event：有 start_time/end_time/status/tag_id` — `tagId` removed (`EventEntity.kt:14-24` has no `tagId`).
- Line 53: lists `Tag` table — deleted from `AppDatabase.kt:14-22`.

The conventions section (line 69): `core/theme` — actual is `ui/theme`.

### `docs/2026-06-22-shijiben-design.md`

- Lines 1-4: header "状态：V1 原型稳定", date "2026-06-22（持续更新）".
- Lines 44-51: structure tree repeats nonexistent `core/`/`features/timeline|recording|tags|heatmap|time_viz`.
- Line 76: `tagId | Long? FK | 关联标签` in the Event table.
- Lines 91-100: full Tag entity definition.
- Lines 157-162 (section 六 "标签系统"): describes tag management as a feature.
- Line 192 (V1 plan): `[x] 标签管理（空起步，增删改着色）` marked done.

### `MY_ORIGIN_GOAL.md`

- Line 25: `- 事件标签` listed under "应该有什么功能" (should-have features).

### The decision record (already correct, do NOT modify)

`docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md` documents the removal decision in its Part C (lines 108-141). This file is the authoritative record — cross-link to it from the other docs.

### Repo conventions to match

- **Doc language**: all three docs are in Simplified Chinese. Match the language and tone (concise, technical, no emojis).
- **Markdown style**: ATX headings (`#`), bullet lists with `-`, tables with `|`. Match the existing formatting in each file.
- **Cross-linking**: `AGENT.md:49` already links to the design doc as `[docs/2026-06-22-shijiben-design.md](docs/2026-06-22-shijiben-design.md)` — use the same relative-path link style.

## Commands you will need

| Purpose   | Command                                          | Expected on success |
|-----------|--------------------------------------------------|---------------------|
| Typecheck | `./gradlew :app:compileDebugKotlin`              | exit 0 (docs don't affect build, but confirms no file was accidentally moved/renamed) |
| Build     | `./gradlew assembleDebug`                        | exit 0              |

## Scope

**In scope** (the only files you should modify):
- `AGENT.md`
- `docs/2026-06-22-shijiben-design.md`
- `MY_ORIGIN_GOAL.md`

**Out of scope** (do NOT touch):
- `docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md` — this is the decision record and is already correct.
- Files under `plans/` — they are historical done-plan records, not living docs; stale references there are frozen history.
- Any source code, build files, or resources.
- Re-adding tags — the removal was a documented user decision; this plan records it, not reverts it.

## Git workflow

- Branch: `advisor/013-docs-tag-removal`
- Single commit; message style: `docs: align AGENT/design/origin-goal with tag removal (plan 013)` (matches recent `docs:` style, e.g. `docs: 首页顶/底重设计 spec + 忽略 .superpowers 草稿`).
- Do NOT push or open a PR unless the operator instructed it.

## Steps

### Step 1: Update `AGENT.md`

Edit `AGENT.md` to match the actual project state. Specifically:

1. **Project structure tree (lines 22-45)**: rewrite to match the actual layout under `app/src/main/java/com/shijiben/`. The real packages are:
   ```
   shijiben/
     docs/            # 文档：设计 spec、计划
     app/
       src/main/java/com/shijiben/
         data/
           local/     # Entity、DAO、Database（EventEntity, NoteEntity, EventDao, NoteDao, AppDatabase）
           model/     # EventStatus
           repository/# EventRepository, NoteRepository
           DataModule.kt   # Hilt 提供方法
         feature/
           notes/     # 随笔列表与编辑（NotesScreen, NoteEditorSheet, NotesViewModel）
           recording/ # 记录弹窗（RecordingSheet, RecordingViewModel, TimeRangeSlider）
           timeline/  # 时间轴主视图（TimelineScreen, TimelineViewModel, DayProgressBar）
         navigation/  # AppNavHost
         ui/theme/    # 8-bit 色板、字体、像素组件（AppColors, AppTheme, PixelComponents）
         MainActivity.kt
         ShiJiBenApplication.kt
       src/main/res/
       build.gradle.kts
       AndroidManifest.xml
     build.gradle.kts
     settings.gradle.kts
   ```
   Remove the `core/` line and the `tags/` line. Keep `heatmap/` and `time_viz/` out of the tree (they don't exist); if mentioned, mark them as "V2 未建" in prose, not in the tree.

2. **Data model section (lines 47-63)**:
   - Change "三张核心表" → "两张核心表".
   - In the Event bullet (line 51), remove `tag_id`. The bullet becomes: `Event：有 start_time/end_time/status/note（可选）`.
   - Delete the `Tag` bullet entirely (line 53).
   - Keep the `Note` bullet unchanged.

3. **事件状态流转 section (lines 55-62)**: unchanged — status rules are correct.

4. **开发约定 section (line 69)**: change `core/theme` → `ui/theme`.

5. **Add a one-line note** at the top of the file (after the `> 本文档供 AI agent 阅读...` line) recording the tag removal:
   ```
   > 2026-06-27 更新：标签（Tag）功能已移除，DB v1→v2 迁移见 [docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md](docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md) Part C。
   ```

**Verify**:
- `grep -n "tag_id\|TagEntity\|TagDao\|TagRepository\|TagsScreen\|tags/\|core/" AGENT.md` → the only matches should be inside the new "2026-06-27 更新" note (which mentions the removal) and the cross-link.
- `./gradlew :app:compileDebugKotlin` → exit 0 (sanity: no file accidentally renamed).

### Step 2: Update `docs/2026-06-22-shijiben-design.md`

1. **Header (lines 1-4)**: add an update note after the status line:
   ```
   > 2026-06-27 更新：标签功能已整体移除（用户决策），DB v1→v2 迁移见 [docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md](superpowers/specs/2026-06-27-top-bottom-redesign-design.md) Part C。本文档下方仍保留历史标签描述，仅供回溯。
   ```
   Actually — prefer to **delete** the stale tag content rather than annotate-and-keep, because keeping it leaves the contradiction visible. So: add the update note, AND make the edits below to remove the tag content.

2. **Structure tree (lines 44-51)**: same corrections as AGENT.md Step 1 — remove `core/`, remove `tags/`, mark `heatmap/` and `time_viz/` as "(V2 未建)" in prose or remove from the tree.

3. **Event table (lines 67-79)**: delete the `tagId | Long? FK | 关联标签` row (line 76).

4. **Tag entity table (lines 91-100)**: delete the entire `### Tag（标签）` subsection.

5. **Section 六 "标签系统" (lines 157-162)**: delete the entire section. Renumber subsequent sections (七 → 六, 八 → 七, etc.) OR keep numbering and leave a one-line `## 六 标签系统（已移除，见顶部 2026-06-27 更新）` placeholder. Prefer the placeholder approach — it preserves the historical section anchors that other docs might reference.

6. **V1 plan checklist (line 192)**: change `[x] 标签管理（空起步，增删改着色）` to `[~] ~~标签管理~~（已于 2026-06-27 移除）`.

**Verify**:
- `grep -n "tagId\|TagEntity\|标签管理\|tags/" docs/2026-06-22-shijiben-design.md` → only matches should be the "2026-06-27 更新" note, the section 六 placeholder, and the struck-through V1 checklist line.
- `./gradlew :app:compileDebugKotlin` → exit 0.

### Step 3: Update `MY_ORIGIN_GOAL.md`

Edit line 25 (`- 事件标签`) to record the removal decision. Since this is the "origin goal" doc (a personal record of intent, not a spec), prefer annotation over deletion:

```
- 事件标签（已于 2026-06-27 移除：用户决定标签功能对纯记录的诉求是负担，见 docs/superpowers/specs/2026-06-27-top-bottom-redesign-design.md Part C）
```

**Verify**: `grep -n "事件标签" MY_ORIGIN_GOAL.md` → exactly one match, the annotated line above.

### Step 4: Full sanity gate

**Verify**:
- `./gradlew :app:compileDebugKotlin` → exit 0
- `./gradlew assembleDebug` → exit 0
- No source files modified: `git status --short` shows only `AGENT.md`, `docs/2026-06-22-shijiben-design.md`, `MY_ORIGIN_GOAL.md` (and `plans/README.md` for the status row).

## Test plan

- **No tests.** This is a docs-only change; no test asserts on doc content.
- Verification: the gate above passes and the grep checks in Steps 1-3 return only the intended matches.

## Done criteria

ALL must hold:

- [ ] `grep -n "tag_id\|TagEntity\|TagDao\|TagRepository\|TagsScreen\|tags/\|core/" AGENT.md` returns no matches outside the "2026-06-27 更新" note and its cross-link
- [ ] `grep -n "tagId\|TagEntity\|标签管理" docs/2026-06-22-shijiben-design.md` returns only the update note, the section 六 placeholder, and the struck-through V1 checklist line
- [ ] `grep -n "事件标签" MY_ORIGIN_GOAL.md` returns exactly one match (the annotated line)
- [ ] `./gradlew :app:compileDebugKotlin` exits 0
- [ ] `./gradlew assembleDebug` exits 0
- [ ] No files outside the in-scope list are modified (`git status`)
- [ ] `plans/README.md` status row for 013 updated

## STOP conditions

Stop and report back (do not improvise) if:

- The doc line numbers in "Current state" don't match the live file (drifted since this plan was written) — re-locate by content and proceed, but if the structure has changed substantially, report.
- You find tag references in OTHER docs (e.g. `README.md` if one exists, or files under `docs/`) not listed in this plan — report the full list; do not edit out-of-scope files.
- A source file or build file appears to need changes to match the docs (it doesn't — the code is correct, the docs are wrong) — STOP, this means the codebase has drifted in an unexpected direction.
- The design doc has other sections that reference tags not enumerated in this plan (e.g. a section 八 reference to "标签着色") — report and extend the plan rather than leaving contradictions.

## Maintenance notes

- **The `heatmap/` and `time_viz/` directories** are listed in `AGENT.md:38-39` and the design doc as V2 features. They do NOT exist yet. This plan marks them as "(V2 未建)" in prose but does not remove them — they are legitimate forward-looking references that ground future direction work (findings DIR-01, DIR-02). A reviewer should confirm the prose marking is clear.
- **The `plans/` historical records** (e.g. `plans/008-delete-confirmation-dialog.md` references `TagEditorSheet.kt`) are NOT updated by this plan — they are frozen history of completed work. If a future reader is confused, the "2026-06-27 更新" note in `AGENT.md` and the design doc points them to the decision record.
- **If tags are ever re-added**, this requires a v3 migration (re-creating the `tags` table and `events.tagId`), reverting the docs, and a new design decision record. The removal is not free to reverse.
