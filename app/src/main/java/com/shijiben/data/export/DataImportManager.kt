package com.shijiben.data.export

import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.model.EventStatus
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

/**
 * 数据导入：把 v1 JSON 反向解析为本地快照并落库。与 DataExportManager 构成备份/恢复对。
 *
 * - 纯本地、不联网。
 * - 只读 JSON，写 Entity/Dao/Repository；不改 DB schema。
 * - 仅用 Android 内置 org.json，不引入新依赖。
 * - schemaVersion 校验：只支持 v1（= DataExportManager.SCHEMA_VERSION）；不匹配抛异常。
 * - timeVizPrefs 字段以 ImportedTimeVizPrefs 原语形式暴露给调用方，由上层 VM 决定如何写回
 *   （data 层不依赖 feature 层，仿 DataExportManager 接原语的范式）。
 */
object DataImportManager {

    /** 导入文件大小上限：50 MB（远超任何合理备份，足以挡住 OOM 攻击）。 */
    private const val MAX_IMPORT_BYTES = 50L * 1024 * 1024

    /** 单个数组（events / notes）条目上限：10 万条（远超日常使用，挡住数组 DoS）。 */
    private const val MAX_ARRAY_ENTRIES = 100_000

    /** 单字段字符串上限：title 1000 字、note 1 万字、content 10 万字（远超日常使用，挡单字段 OOM）。 */
    private const val MAX_TITLE_LEN = 1_000
    private const val MAX_NOTE_LEN = 10_000
    private const val MAX_CONTENT_LEN = 100_000

    /** 解析后的可选 TimeViz 偏好。null 表示 JSON 中缺失，导入时跳过不改现有。 */
    data class ImportedTimeVizPrefs(
        val birthdayMillis: Long,
        val lifespanYears: Int
    )

    /** parseJsonString 的纯数据产物。 */
    data class ImportResult(
        val schemaVersion: Int,
        val events: List<EventEntity>,
        val notes: List<NoteEntity>,
        val timeVizPrefs: ImportedTimeVizPrefs?
    )

    /** applyImport 的计数结果。prefs 写回由上层 VM 基于 ImportResult.timeVizPrefs 决定。 */
    data class ImportCounts(
        val eventsImported: Int,
        val notesImported: Int
    )

    /**
     * 纯函数：把 JSON 字符串解析为 ImportResult。无 Android 依赖（仅 org.json）。
     *
     * - 损坏 / 非 JSON / 空字符串 → 抛 JSONException（由 VM catch → Error）。
     * - schemaVersion 缺失或不等于 SCHEMA_VERSION → 抛 IllegalArgumentException。
     * - events / notes 数组缺失 → 视为空列表。
     * - timeVizPrefs 缺失 → null（由调用方决定跳过，不改现有）。
     * - endTime / note 字段：JSON null 或 key 缺失 → 实体 null（isNull 同时覆盖两种）。
     * - 其余必填字段（id/title/startTime/status/createdAt/updatedAt；content/timestamp）
     *   缺失 → 抛 JSONException（非标准文件 → Error，保证数据完整性）。
     */
    fun parseJsonString(json: String): ImportResult {
        val root = JSONObject(json)                       // 损坏/空 → JSONException
        val version = root.optInt("schemaVersion", -1)
        if (version != DataExportManager.SCHEMA_VERSION) {
            throw IllegalArgumentException("schemaVersion 不支持: $version")
        }
        val events = parseEvents(root.optJSONArray("events"))
        val notes = parseNotes(root.optJSONArray("notes"))
        val prefs = parsePrefs(root.optJSONObject("timeVizPrefs"))
        return ImportResult(version, events, notes, prefs)
    }

    private fun parseEvents(arr: JSONArray?): List<EventEntity> {
        if (arr == null) return emptyList()
        require(arr.length() <= MAX_ARRAY_ENTRIES) {
            "数组条目过多（>${MAX_ARRAY_ENTRIES}）：${arr.length()}"
        }
        val out = ArrayList<EventEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val status = o.getInt("status")
            // 信任边界校验：status 必须是合法 EventStatus 值，否则拒绝导入（与 schemaVersion 校验先例一致）
            require(EventStatus.entries.any { it.value == status }) {
                "非法 status 值: $status"
            }
            val title = o.getString("title")
            require(title.length <= MAX_TITLE_LEN) {
                "title 过长（>${MAX_TITLE_LEN}）：${title.length}"
            }
            val startTime = o.getLong("startTime")
            require(startTime >= 0) { "startTime 不能为负：$startTime" }
            val endTime = if (o.isNull("endTime")) null else o.getLong("endTime")
            // 不变式：endTime 非空时必须 >= startTime（防负时长污染热力图与状态机）
            if (endTime != null) {
                require(endTime >= startTime) {
                    "endTime($endTime) < startTime($startTime)：负时长事件"
                }
            }
            val note = if (o.isNull("note")) null else o.getString("note")
            if (note != null) {
                require(note.length <= MAX_NOTE_LEN) {
                    "note 过长（>${MAX_NOTE_LEN}）：${note.length}"
                }
            }
            out.add(
                EventEntity(
                    id = o.getLong("id"),
                    title = title,
                    startTime = startTime,
                    endTime = endTime,
                    status = status,
                    note = note,
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt")
                )
            )
        }
        return out
    }

    private fun parseNotes(arr: JSONArray?): List<NoteEntity> {
        if (arr == null) return emptyList()
        require(arr.length() <= MAX_ARRAY_ENTRIES) {
            "数组条目过多（>${MAX_ARRAY_ENTRIES}）：${arr.length()}"
        }
        val out = ArrayList<NoteEntity>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val content = o.getString("content")
            require(content.length <= MAX_CONTENT_LEN) {
                "content 过长（>${MAX_CONTENT_LEN}）：${content.length}"
            }
            val timestamp = o.getLong("timestamp")
            require(timestamp >= 0) { "timestamp 不能为负：$timestamp" }
            out.add(
                NoteEntity(
                    id = o.getLong("id"),
                    content = content,
                    timestamp = timestamp,
                    createdAt = o.getLong("createdAt"),
                    updatedAt = o.getLong("updatedAt")
                )
            )
        }
        return out
    }

    private fun parsePrefs(o: JSONObject?): ImportedTimeVizPrefs? {
        if (o == null) return null
        return ImportedTimeVizPrefs(
            birthdayMillis = o.getLong("birthdayMillis"),
            lifespanYears = o.getInt("lifespanYears")
        )
    }

    /**
     * 薄 IO：把 ImportResult 的 events/notes 落库。
     * - events/notes 用 Repository.upsertAll（保留原 ID，冲突 REPLACE 覆盖，幂等可重复导入）。
     * - timeVizPrefs 由调用方（ImportViewModel）基于 result.timeVizPrefs 自行写回；
     *   data 层不接触 feature 层 prefs 类型。
     * 返回导入计数。
     */
    suspend fun applyImport(
        result: ImportResult,
        eventRepository: EventRepository,
        noteRepository: NoteRepository
    ): ImportCounts {
        eventRepository.upsertAll(result.events)
        noteRepository.upsertAll(result.notes)
        return ImportCounts(
            eventsImported = result.events.size,
            notesImported = result.notes.size
        )
    }

    /** 薄 IO 包装：UTF-8 读取流并关闭。超过 [MAX_IMPORT_BYTES] 抛 IllegalArgumentException 由 VM catch。 */
    fun readFromStream(input: InputStream): String {
        return input.use { stream ->
            val out = java.io.ByteArrayOutputStream()
            val chunk = ByteArray(8 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(chunk)
                if (read == -1) break
                total += read
                if (total > MAX_IMPORT_BYTES) {
                    throw IllegalArgumentException("导入文件过大（>$MAX_IMPORT_BYTES 字节）")
                }
                out.write(chunk, 0, read)
            }
            out.toString(Charsets.UTF_8.name())
        }
    }
}
