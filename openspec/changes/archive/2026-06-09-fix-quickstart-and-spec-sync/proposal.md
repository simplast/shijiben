## Why

Explore 分析发现 2 个代码 Bug 和 spec 文件中 6 处过时描述，导致代码与规格文档不一致。需要修复代码缺陷并同步 spec 文件，使文档和实现重新对齐。

## What Changes

- **修复 `quickStartEvent` Bug**: 推荐快速开始时，若当天已有同名 PENDING 事件，应复用该事件（`startEvent`），而非创建新记录留下孤儿 PENDING
- **修复 `quickStartEvent` dayKey**: 快速开始的事件应设置正确的 `dayKey`（今日），与 `quickAddEvent` / `restartEvent` 保持一致
- **修复推荐去重不完整**: 推荐过滤应同时排除已完成（COMPLETED）的事件名称，避免用户重复推荐已完成的活动
- **清理 `minimal-line-home/spec.md`**: 删除 6 处与 8-bit 像素风设计矛盾的旧 requirement（圆角/阴影/Outfit 字体/4dp 色条/毛玻璃/56dp 行高）

## Capabilities

### New Capabilities
_(无新增能力)_

### Modified Capabilities
- `algo-recommendation`: 修复推荐快速开始时的 PENDING 孤儿问题；修复推荐去重不完整的已完成事件过滤
- `minimal-line-home`: 删除与 8-bit 像素风设计矛盾的过时 requirement（圆角体系、阴影体系、排版体系、毛玻璃输入栏、4dp 色条）

## Impact

- `EventViewModel.kt`: `quickStartEvent()` 方法重写；`PendingSection` 推荐过滤逻辑调整
- `openspec/specs/algo-recommendation/spec.md`: 更新推荐相关 scenario
- `openspec/specs/minimal-line-home/spec.md`: 删除过时 requirement
- 无数据库 schema 变更，无新依赖
