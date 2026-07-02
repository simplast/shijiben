# Findings: security

## F008 — DONE
- 文件：`app/src/main/java/com/shijiben/data/export/DataImportManager.kt`
- 符号：`DataImportManager.parseEvents`
- 问题：`status = o.getInt("status")` 无校验——恶意导入文件可注入任意 int（如 99），污染事件状态机（carryOverNotStarted 永不顺延、UI 误渲染、status 查询漏处理）
- 修复：读取后 `require(EventStatus.entries.any { it.value == status })`，不合法抛 IllegalArgumentException（与 schemaVersion 校验先例一致）
- 回归测试：`DataImportManagerTest.parseJsonString_invalidStatus_throwsIllegalArgumentException`
- 计划：`better/security/plan-cycle-8.md`

## F015 — AndroidManifest allowBackup=true 违反隐私政策承诺（自动云备份+adb backup 泄露）
- status: DONE (cycle 15)
- evidence: app/src/main/AndroidManifest.xml:6
- impact: M

## F025 — Import 文件 DoS：readFromStream 无大小上限 + parseEvents/parseNotes 无数组长度上限
- status: DONE (cycle 25)
- evidence: app/src/main/java/com/shijiben/data/export/DataImportManager.kt:141
- impact: M

## F035 — Import 信任边界：时间戳与字符串长度无校验（负时间戳/负时长/单字段 OOM）
- status: DONE (cycle 35)
- 文件：`app/src/main/java/com/shijiben/data/export/DataImportManager.kt`
- 符号：`parseEvents` / `parseNotes`
- 问题：导入解析器对 Long 时间戳与 String 字段无任何校验——(1) 负 startTime/timestamp 会让 Calendar/java.time 崩或产生非法日期；(2) endTime < startTime 产生负时长事件，污染热力图聚合（effectiveDurationMs coerceAtLeast(0) 兜底但数据语义错）与状态机不变式；(3) 单字段超长字符串（50MB 文件内一个 49MB title）绕过数组长度上限造成单字段 OOM。
- 修复：parseEvents 加 startTime>=0、endTime!=null 时 endTime>=startTime、title<=1000、note<=10000 校验；parseNotes 加 timestamp>=0、content<=100000 校验。均用 require 抛 IllegalArgumentException（被 ImportViewModel catch 走 Error 提示）。
- 回归测试：DataImportManagerTest 新增 7 用例（负 startTime、endTime<startTime、endTime==startTime 边界接受、超长 title/note/content、负 timestamp）
- evidence: DataImportManager.kt parseEvents/parseNotes；DataImportManagerTest F035 节
- impact: M（外部输入信任边界加固；防数据污染 + 单字段 OOM）
