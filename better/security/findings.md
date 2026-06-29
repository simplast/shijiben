# Findings: security

## F008 — DONE
- 文件：`app/src/main/java/com/shijiben/data/export/DataImportManager.kt`
- 符号：`DataImportManager.parseEvents`
- 问题：`status = o.getInt("status")` 无校验——恶意导入文件可注入任意 int（如 99），污染事件状态机（carryOverNotStarted 永不顺延、UI 误渲染、status 查询漏处理）
- 修复：读取后 `require(EventStatus.entries.any { it.value == status })`，不合法抛 IllegalArgumentException（与 schemaVersion 校验先例一致）
- 回归测试：`DataImportManagerTest.parseJsonString_invalidStatus_throwsIllegalArgumentException`
- 计划：`better/security/plan-cycle-8.md`
