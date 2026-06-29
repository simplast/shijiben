package com.shijiben.data.export

import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * DataExportManager.buildJsonString 单测（spec §7.3）。
 * 纯函数，Robolectric 仅提供 org.json 实现。用 JSONObject(jsonStr) 反解析逐字段断言。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataExportManagerTest {

    private fun build(
        events: List<EventEntity> = emptyList(),
        notes: List<NoteEntity> = emptyList(),
        birthdayMillis: Long = 0L,
        lifespanYears: Int = 80,
        appVersion: String = "1.0",
        exportedAt: Long = 1719638400000L
    ): String = DataExportManager.buildJsonString(
        events = events,
        notes = notes,
        birthdayMillis = birthdayMillis,
        lifespanYears = lifespanYears,
        appVersion = appVersion,
        exportedAt = exportedAt
    )

    @Test
    fun buildJsonString_emptyData_hasSchemaAndEmptyArrays() {
        val json = build(events = emptyList(), notes = emptyList(), birthdayMillis = 0L, lifespanYears = 80)
        val root = JSONObject(json)
        assertThat(root.getInt("schemaVersion")).isEqualTo(1)
        assertThat(root.getLong("exportedAt")).isEqualTo(1719638400000L)
        assertThat(root.getString("appVersion")).isEqualTo("1.0")
        assertThat(root.getJSONArray("events").length()).isEqualTo(0)
        assertThat(root.getJSONArray("notes").length()).isEqualTo(0)
        assertThat(root.getJSONObject("timeVizPrefs").getLong("birthdayMillis")).isEqualTo(0L)
        assertThat(root.getJSONObject("timeVizPrefs").getInt("lifespanYears")).isEqualTo(80)
    }

    @Test
    fun buildJsonString_eventWithNullEndTimeAndNote_serializesNull() {
        val event = EventEntity(
            id = 1, title = "进行中", startTime = 100L, endTime = null,
            status = 1, note = null, createdAt = 200L, updatedAt = 300L
        )
        val json = build(events = listOf(event))
        val e = JSONObject(json).getJSONArray("events").getJSONObject(0)
        assertThat(e.isNull("endTime")).isTrue()
        assertThat(e.isNull("note")).isTrue()
    }

    @Test
    fun buildJsonString_eventAllFields_roundTrip() {
        val event = EventEntity(
            id = 42, title = "阅读", startTime = 1000L, endTime = 2000L,
            status = 2, note = "memo", createdAt = 3000L, updatedAt = 4000L
        )
        val json = build(events = listOf(event))
        val e = JSONObject(json).getJSONArray("events").getJSONObject(0)
        assertThat(e.getLong("id")).isEqualTo(42L)
        assertThat(e.getString("title")).isEqualTo("阅读")
        assertThat(e.getLong("startTime")).isEqualTo(1000L)
        assertThat(e.getLong("endTime")).isEqualTo(2000L)
        assertThat(e.getInt("status")).isEqualTo(2)
        assertThat(e.getString("note")).isEqualTo("memo")
        assertThat(e.getLong("createdAt")).isEqualTo(3000L)
        assertThat(e.getLong("updatedAt")).isEqualTo(4000L)
    }

    @Test
    fun buildJsonString_noteSpecialCharacters_escaped() {
        // content 含 " \ \n emoji 中文
        val content = "a\"b\\c\nd🎉中文"
        val note = NoteEntity(
            id = 1, content = content, timestamp = 100L,
            createdAt = 200L, updatedAt = 300L
        )
        val json = build(notes = listOf(note))
        val n = JSONObject(json).getJSONArray("notes").getJSONObject(0)
        assertThat(n.getString("content")).isEqualTo(content)
        assertThat(n.getLong("timestamp")).isEqualTo(100L)
    }

    @Test
    fun buildJsonString_timeVizPrefsSet_storesValues() {
        val json = build(birthdayMillis = 1234567890L, lifespanYears = 99)
        val prefs = JSONObject(json).getJSONObject("timeVizPrefs")
        assertThat(prefs.getLong("birthdayMillis")).isEqualTo(1234567890L)
        assertThat(prefs.getInt("lifespanYears")).isEqualTo(99)
    }

    @Test
    fun buildJsonString_schemaVersionIsOne() {
        val json = build()
        assertThat(JSONObject(json).getInt("schemaVersion"))
            .isEqualTo(DataExportManager.SCHEMA_VERSION)
        assertThat(DataExportManager.SCHEMA_VERSION).isEqualTo(1)
    }

    @Test
    fun buildJsonString_largeDataset_completesUnder500ms() {
        val event = EventEntity(
            id = 1, title = "x", startTime = 1L, endTime = 2L,
            status = 2, note = "n", createdAt = 3L, updatedAt = 4L
        )
        val events = List(5000) { event.copy(id = it.toLong()) }
        val start = System.currentTimeMillis()
        DataExportManager.buildJsonString(
            events = events, notes = emptyList(),
            birthdayMillis = 0L, lifespanYears = 80,
            appVersion = "1.0", exportedAt = 0L
        )
        val elapsed = System.currentTimeMillis() - start
        // perf smoke，宽松阈值防 CI 抖动
        assertThat(elapsed).isLessThan(500L)
    }
}
