package com.shijiben.data.export

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.json.JSONException
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.InputStream

/**
 * DataImportManager 单测（spec §7.2）。
 *
 * - parseJsonString 为纯函数单测主目标（仅 org.json，Robolectric 提供）。
 * - 用 DataExportManager.buildJsonString 产合法 JSON 做反向解析，保证 round-trip 对称。
 * - applyImport 的 ID 冲突用例用内存 Room 验证 REPLACE 行为。
 * - prefs 写回行为已上移到 ImportViewModel，由 ImportViewModelTest 覆盖，本测试不再涉及。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DataImportManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var noteRepo: NoteRepository

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepo = EventRepository(db.eventDao())
        noteRepo = NoteRepository(db.noteDao())
    }

    @After
    fun teardown() { db.close() }

    /** 构造合法 v1 JSON（含可空字段非空、prefs），用 buildJsonString 保证对称。 */
    private fun buildValidJson(
        events: List<EventEntity> = emptyList(),
        notes: List<NoteEntity> = emptyList(),
        birthdayMillis: Long = 0L,
        lifespanYears: Int = 80
    ): String = DataExportManager.buildJsonString(
        events = events,
        notes = notes,
        birthdayMillis = birthdayMillis,
        lifespanYears = lifespanYears,
        appVersion = "1.0",
        exportedAt = 1719638400000L
    )

    @Test
    fun parseJsonString_validFullJson_returnsAllFields() {
        val event = EventEntity(
            id = 1, title = "阅读", startTime = 1000L, endTime = 2000L,
            status = 2, note = "memo", createdAt = 3000L, updatedAt = 4000L
        )
        val note = NoteEntity(
            id = 5, content = "想法", timestamp = 5000L, createdAt = 6000L, updatedAt = 7000L
        )
        val json = buildValidJson(
            events = listOf(event),
            notes = listOf(note),
            birthdayMillis = 1234567890L,
            lifespanYears = 99
        )

        val result = DataImportManager.parseJsonString(json)

        assertThat(result.schemaVersion).isEqualTo(1)
        assertThat(result.events).hasSize(1)
        assertThat(result.notes).hasSize(1)
        val e = result.events[0]
        assertThat(e.id).isEqualTo(1L)
        assertThat(e.title).isEqualTo("阅读")
        assertThat(e.startTime).isEqualTo(1000L)
        assertThat(e.endTime).isEqualTo(2000L)
        assertThat(e.status).isEqualTo(2)
        assertThat(e.note).isEqualTo("memo")
        assertThat(e.createdAt).isEqualTo(3000L)
        assertThat(e.updatedAt).isEqualTo(4000L)
        val n = result.notes[0]
        assertThat(n.id).isEqualTo(5L)
        assertThat(n.content).isEqualTo("想法")
        assertThat(n.timestamp).isEqualTo(5000L)
        assertThat(n.createdAt).isEqualTo(6000L)
        assertThat(n.updatedAt).isEqualTo(7000L)
        assertThat(result.timeVizPrefs).isNotNull()
        assertThat(result.timeVizPrefs!!.birthdayMillis).isEqualTo(1234567890L)
        assertThat(result.timeVizPrefs!!.lifespanYears).isEqualTo(99)
    }

    @Test
    fun parseJsonString_nullEndTimeAndNote_parsedAsNull() {
        val event = EventEntity(
            id = 1, title = "进行中", startTime = 100L, endTime = null,
            status = 1, note = null, createdAt = 200L, updatedAt = 300L
        )
        val json = buildValidJson(events = listOf(event))

        val result = DataImportManager.parseJsonString(json)

        assertThat(result.events).hasSize(1)
        assertThat(result.events[0].endTime).isNull()
        assertThat(result.events[0].note).isNull()
    }

    @Test
    fun parseJsonString_eventsMissing_returnsEmptyEvents() {
        // 构造合法 JSON 后删 events key
        val json = JSONObject(buildValidJson()).apply { remove("events") }.toString()

        val result = DataImportManager.parseJsonString(json)

        assertThat(result.events).isEmpty()
        // notes 仍在
        assertThat(result.notes).isEmpty()
        assertThat(result.timeVizPrefs).isNotNull()
    }

    @Test
    fun parseJsonString_notesMissing_returnsEmptyNotes() {
        val json = JSONObject(buildValidJson()).apply { remove("notes") }.toString()

        val result = DataImportManager.parseJsonString(json)

        assertThat(result.notes).isEmpty()
        assertThat(result.events).isEmpty()
    }

    @Test
    fun parseJsonString_timeVizPrefsMissing_returnsNullPrefs() {
        val json = JSONObject(buildValidJson()).apply { remove("timeVizPrefs") }.toString()

        val result = DataImportManager.parseJsonString(json)

        assertThat(result.timeVizPrefs).isNull()
    }

    @Test
    fun parseJsonString_corruptJson_throwsJSONException() {
        assertThrows(JSONException::class.java) {
            DataImportManager.parseJsonString("not a json")
        }
    }

    @Test
    fun parseJsonString_emptyString_throwsJSONException() {
        assertThrows(JSONException::class.java) {
            DataImportManager.parseJsonString("")
        }
    }

    @Test
    fun parseJsonString_schemaVersionMismatch_throwsIllegalArgumentException() {
        val json = JSONObject(buildValidJson()).apply { put("schemaVersion", 2) }.toString()

        assertThrows(IllegalArgumentException::class.java) {
            DataImportManager.parseJsonString(json)
        }
    }

    @Test
    fun parseJsonString_schemaVersionMissing_throwsIllegalArgumentException() {
        val json = JSONObject(buildValidJson()).apply { remove("schemaVersion") }.toString()

        assertThrows(IllegalArgumentException::class.java) {
            DataImportManager.parseJsonString(json)
        }
    }

    @Test
    fun parseJsonString_invalidStatus_throwsIllegalArgumentException() {
        // 构造合法 JSON（status=2）后改 events[0].status=99（非合法 EventStatus 值）
        val event = EventEntity(
            id = 1, title = "x", startTime = 100L, endTime = 200L,
            status = 2, note = null, createdAt = 300L, updatedAt = 400L
        )
        val json = JSONObject(buildValidJson(events = listOf(event))).apply {
            getJSONArray("events").getJSONObject(0).put("status", 99)
        }.toString()

        assertThrows(IllegalArgumentException::class.java) {
            DataImportManager.parseJsonString(json)
        }
    }

    @Test
    fun parseJsonString_partialFieldMissing_throwsJSONException() {
        // 删 event 的 title key → 严格 getString 抛
        val event = EventEntity(
            id = 1, title = "x", startTime = 1L, endTime = 2L,
            status = 2, note = "n", createdAt = 3L, updatedAt = 4L
        )
        val json = JSONObject(buildValidJson(events = listOf(event)))
        json.getJSONArray("events").getJSONObject(0).remove("title")

        assertThrows(JSONException::class.java) {
            DataImportManager.parseJsonString(json.toString())
        }
    }

    @Test
    fun parseJsonString_specialCharacters_roundTrip() {
        val content = "a\"b\\c\nd🎉中文"
        val note = NoteEntity(
            id = 1, content = content, timestamp = 100L,
            createdAt = 200L, updatedAt = 300L
        )
        val event = EventEntity(
            id = 2, title = "标题\"引号\n换行🎉", startTime = 1L, endTime = 2L,
            status = 2, note = content, createdAt = 3L, updatedAt = 4L
        )
        val json = buildValidJson(events = listOf(event), notes = listOf(note))

        val result = DataImportManager.parseJsonString(json)

        assertThat(result.events[0].title).isEqualTo("标题\"引号\n换行🎉")
        assertThat(result.events[0].note).isEqualTo(content)
        assertThat(result.notes[0].content).isEqualTo(content)
    }

    @Test
    fun parseJsonString_roundTripWithExport_symmetric() {
        val e1 = EventEntity(
            id = 10, title = "事件一", startTime = 1000L, endTime = 2000L,
            status = 2, note = "n1", createdAt = 3000L, updatedAt = 4000L
        )
        val e2 = EventEntity(
            id = 11, title = "事件二", startTime = 5000L, endTime = null,
            status = 1, note = null, createdAt = 6000L, updatedAt = 7000L
        )
        val n1 = NoteEntity(
            id = 20, content = "随笔一", timestamp = 8000L,
            createdAt = 9000L, updatedAt = 10000L
        )
        val json = buildValidJson(
            events = listOf(e1, e2),
            notes = listOf(n1),
            birthdayMillis = 999L,
            lifespanYears = 77
        )

        val result = DataImportManager.parseJsonString(json)

        assertThat(result.events).containsExactly(e1, e2).inOrder()
        assertThat(result.notes).containsExactly(n1)
        assertThat(result.timeVizPrefs).isEqualTo(
            DataImportManager.ImportedTimeVizPrefs(birthdayMillis = 999L, lifespanYears = 77)
        )
    }

    @Test
    fun applyImport_idConflict_replacesExisting() = runTest {
        // 预插 id=1 event A
        val eventA = EventEntity(
            id = 1, title = "原数据", startTime = 100L, endTime = 200L,
            status = 2, note = "old", createdAt = 300L, updatedAt = 400L
        )
        eventRepo.upsertAll(listOf(eventA))
        assertThat(eventRepo.getAllEvents().first()).hasSize(1)

        // 导入含 id=1 event B → REPLACE 覆盖
        val eventB = EventEntity(
            id = 1, title = "新数据", startTime = 500L, endTime = 600L,
            status = 2, note = "new", createdAt = 700L, updatedAt = 800L
        )
        val result = DataImportManager.ImportResult(
            schemaVersion = 1,
            events = listOf(eventB),
            notes = emptyList(),
            timeVizPrefs = null
        )

        val counts = DataImportManager.applyImport(result, eventRepo, noteRepo)
        assertThat(counts.eventsImported).isEqualTo(1)
        assertThat(counts.notesImported).isEqualTo(0)

        // 仍 1 行，且字段为 B
        val after = eventRepo.getAllEvents().first()
        assertThat(after).hasSize(1)
        val row = eventRepo.getEventById(1)!!
        assertThat(row.title).isEqualTo("新数据")
        assertThat(row.startTime).isEqualTo(500L)
        assertThat(row.note).isEqualTo("new")

        // 再导一次同文件 → 仍 1 行（幂等）
        DataImportManager.applyImport(result, eventRepo, noteRepo)
        assertThat(eventRepo.getAllEvents().first()).hasSize(1)
    }

    @Test
    fun applyImport_fullDataset_countsEventsAndNotes() = runTest {
        val e1 = EventEntity(
            id = 1, title = "事件一", startTime = 1000L, endTime = 2000L,
            status = 2, note = "n1", createdAt = 3000L, updatedAt = 4000L
        )
        val e2 = EventEntity(
            id = 2, title = "事件二", startTime = 5000L, endTime = null,
            status = 1, note = null, createdAt = 6000L, updatedAt = 7000L
        )
        val n1 = NoteEntity(
            id = 10, content = "随笔", timestamp = 8000L,
            createdAt = 9000L, updatedAt = 10000L
        )
        val result = DataImportManager.ImportResult(
            schemaVersion = 1,
            events = listOf(e1, e2),
            notes = listOf(n1),
            timeVizPrefs = DataImportManager.ImportedTimeVizPrefs(birthdayMillis = 999L, lifespanYears = 77)
        )

        val counts = DataImportManager.applyImport(result, eventRepo, noteRepo)

        assertThat(counts.eventsImported).isEqualTo(2)
        assertThat(counts.notesImported).isEqualTo(1)
        assertThat(eventRepo.getAllEvents().first()).hasSize(2)
        assertThat(noteRepo.getAllNotes().first()).hasSize(1)
    }

    // ===== DoS 防护回归测试（cycle 25）=====
    // readFromStream 字节上限 + parseEvents/parseNotes 数组长度上限

    @Test
    fun readFromStream_exceedsMaxBytes_throwsIllegalArgumentException() {
        // 无限 0 字节流：触发 readFromStream 在 >MAX_IMPORT_BYTES 时抛 IAE，
        // 无需实际分配 50 MB 内存（覆盖 bulk read 路径）
        val infiniteZeroStream = object : InputStream() {
            override fun read(): Int = 0
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                java.util.Arrays.fill(b, off, off + len, 0.toByte())
                return len
            }
        }

        assertThrows(IllegalArgumentException::class.java) {
            DataImportManager.readFromStream(infiniteZeroStream)
        }
    }

    @Test
    fun parseJsonString_tooManyEvents_throwsIllegalArgumentException() {
        // 直接拼字符串构造 100001 个最小 event，避免 100001 个 JSONObject 的内存压力
        // require 检查发生在 parse 循环之前，元素内容不影响测试结果（用 {} 占位）
        val sb = StringBuilder()
        sb.append("{\"schemaVersion\":1,\"events\":[")
        repeat(100_001) { i ->
            if (i > 0) sb.append(',')
            sb.append("{}")
        }
        sb.append("]}")

        assertThrows(IllegalArgumentException::class.java) {
            DataImportManager.parseJsonString(sb.toString())
        }
    }

    @Test
    fun parseJsonString_tooManyNotes_throwsIllegalArgumentException() {
        val sb = StringBuilder()
        sb.append("{\"schemaVersion\":1,\"notes\":[")
        repeat(100_001) { i ->
            if (i > 0) sb.append(',')
            sb.append("{}")
        }
        sb.append("]}")

        assertThrows(IllegalArgumentException::class.java) {
            DataImportManager.parseJsonString(sb.toString())
        }
    }
}
