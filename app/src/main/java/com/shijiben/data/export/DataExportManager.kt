package com.shijiben.data.export

import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStream

/**
 * 数据导出：把本地快照（events + notes + TimeVizPrefs）序列化为 JSON 并写入流。
 *
 * - 纯本地、不联网。
 * - 只读 Entity/Dao/Repository，不改 DB schema。
 * - 仅用 Android 内置 org.json，不引入新依赖。
 *
 * schemaVersion 是导出格式版本，**独立于 Room DB version**；演进时递增。
 */
object DataExportManager {

    /** 导出格式版本（独立于 Room DB version=2）。演进时递增。 */
    const val SCHEMA_VERSION = 1

    /**
     * 纯函数：把快照数据序列化为 JSON 字符串。无 Android 依赖（仅 org.json）。
     * 可空字段（endTime/note）传 null → JSONObject.put(key, null) → 序列化为 JSON null。
     */
    fun buildJsonString(
        events: List<EventEntity>,
        notes: List<NoteEntity>,
        birthdayMillis: Long,
        lifespanYears: Int,
        appVersion: String,
        exportedAt: Long
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", SCHEMA_VERSION)
        root.put("exportedAt", exportedAt)
        root.put("appVersion", appVersion)

        val eventsArr = JSONArray()
        for (e in events) {
            val o = JSONObject()
            o.put("id", e.id)
            o.put("title", e.title)
            o.put("startTime", e.startTime)
            o.put("endTime", e.endTime)       // Long? → null 安全
            o.put("status", e.status)
            o.put("note", e.note)             // String? → null 安全
            o.put("createdAt", e.createdAt)
            o.put("updatedAt", e.updatedAt)
            eventsArr.put(o)
        }
        root.put("events", eventsArr)

        val notesArr = JSONArray()
        for (n in notes) {
            val o = JSONObject()
            o.put("id", n.id)
            o.put("content", n.content)
            o.put("timestamp", n.timestamp)
            o.put("createdAt", n.createdAt)
            o.put("updatedAt", n.updatedAt)
            notesArr.put(o)
        }
        root.put("notes", notesArr)

        val prefs = JSONObject()
        prefs.put("birthdayMillis", birthdayMillis)
        prefs.put("lifespanYears", lifespanYears)
        root.put("timeVizPrefs", prefs)

        return root.toString()
    }

    /** 薄 IO 包装：UTF-8 写入流并关闭。失败抛异常由调用方 catch。 */
    fun writeToStream(json: String, out: OutputStream) {
        out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
    }
}
