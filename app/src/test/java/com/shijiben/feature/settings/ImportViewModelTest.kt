package com.shijiben.feature.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.export.DataExportManager
import com.shijiben.data.local.AppDatabase
import com.shijiben.data.local.EventEntity
import com.shijiben.data.local.NoteEntity
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.test.MainCoroutineRule
import com.shijiben.feature.timeviz.TimeVizPrefs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * ImportViewModel 单测（spec §7.3）。
 *
 * 注入：内存 Room → EventRepository/NoteRepository；fake TimeVizPrefs；@IoDispatcher 传
 * mainRule.dispatcher（StandardTestDispatcher）→ 确定性，且无重入执行。
 *
 * 用 StandardTestDispatcher + runTest(mainRule.dispatcher) 统一 TestScope 与 viewModelScope 调度器。
 * 用 vm.state.first{...}（suspending wait）替代 advanceUntilIdle() 等待终态：
 * Room Flow 的 getAllEvents().first()/getAllNotes().first() 在 Room executor（真实线程）上异步执行，
 * advanceUntilIdle() 只推进 StandardTestDispatcher 队列不会等待 Room 线程；
 * first{} 挂起测试体后 runBlocking 会处理 Room 线程 dispatch 回来的任务。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImportViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var noteRepo: NoteRepository
    private lateinit var fakePrefs: ImportFakeTimeVizPrefs
    private lateinit var vm: ImportViewModel

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepo = EventRepository(db.eventDao())
        noteRepo = NoteRepository(db.noteDao())
        fakePrefs = ImportFakeTimeVizPrefs()
        vm = ImportViewModel(eventRepo, noteRepo, fakePrefs, mainRule.dispatcher)
    }

    @After
    fun teardown() { db.close() }

    private fun buildJson(
        events: List<EventEntity> = emptyList(),
        notes: List<NoteEntity> = emptyList(),
        birthdayMillis: Long = 0L,
        lifespanYears: Int = 80,
        schemaVersionOverride: Int? = null
    ): String {
        val json = DataExportManager.buildJsonString(
            events = events,
            notes = notes,
            birthdayMillis = birthdayMillis,
            lifespanYears = lifespanYears,
            appVersion = "1.0",
            exportedAt = 1719638400000L
        )
        return if (schemaVersionOverride != null) {
            org.json.JSONObject(json).apply { put("schemaVersion", schemaVersionOverride) }.toString()
        } else json
    }

    private fun stream(s: String): InputStream = ByteArrayInputStream(s.toByteArray(Charsets.UTF_8))

    @Test
    fun import_validData_transitionsToSuccessAndPersists() = runTest(mainRule.dispatcher) {
        val e1 = EventEntity(
            id = 1, title = "事件1", startTime = 1000L, endTime = 2000L,
            status = 2, note = "n1", createdAt = 3000L, updatedAt = 4000L
        )
        val e2 = EventEntity(
            id = 2, title = "事件2", startTime = 5000L, endTime = null,
            status = 1, note = null, createdAt = 6000L, updatedAt = 7000L
        )
        val n1 = NoteEntity(
            id = 10, content = "随笔", timestamp = 8000L,
            createdAt = 9000L, updatedAt = 10000L
        )
        val json = buildJson(
            events = listOf(e1, e2),
            notes = listOf(n1),
            birthdayMillis = 123L,
            lifespanYears = 33
        )

        vm.import(stream(json))
        val terminal = vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }

        assertThat(terminal).isEqualTo(
            ImportViewModel.ImportState.Success(events = 2, notes = 1, prefsUpdated = true)
        )
        // 落库校验
        assertThat(eventRepo.getAllEvents().first()).hasSize(2)
        assertThat(noteRepo.getAllNotes().first()).hasSize(1)
        assertThat(fakePrefs.birthday).isEqualTo(123L)
        assertThat(fakePrefs.lifespan).isEqualTo(33)
    }

    @Test
    fun import_corruptJson_transitionsToError() = runTest(mainRule.dispatcher) {
        vm.import(stream("not a json"))
        val terminal = vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }

        assertThat(terminal).isEqualTo(ImportViewModel.ImportState.Error("导入失败，请重试"))
        // DB 无改动
        assertThat(eventRepo.getAllEvents().first()).isEmpty()
        assertThat(noteRepo.getAllNotes().first()).isEmpty()
    }

    @Test
    fun import_emptyStream_transitionsToError() = runTest(mainRule.dispatcher) {
        vm.import(stream(""))
        val terminal = vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }

        assertThat(terminal).isEqualTo(ImportViewModel.ImportState.Error("导入失败，请重试"))
    }

    @Test
    fun import_schemaMismatch_transitionsToError() = runTest(mainRule.dispatcher) {
        val json = buildJson(schemaVersionOverride = 2)

        vm.import(stream(json))
        val terminal = vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }

        assertThat(terminal).isEqualTo(ImportViewModel.ImportState.Error("导入失败，请重试"))
    }

    @Test
    fun import_timeVizPrefsMissing_doesNotChangePrefs() = runTest(mainRule.dispatcher) {
        // 预设 fakePrefs
        fakePrefs.birthday = 999L
        fakePrefs.lifespan = 88
        // 删 timeVizPrefs key
        val json = org.json.JSONObject(buildJson()).apply { remove("timeVizPrefs") }.toString()

        vm.import(stream(json))
        val terminal = vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }

        assertThat(terminal).isEqualTo(
            ImportViewModel.ImportState.Success(events = 0, notes = 0, prefsUpdated = false)
        )
        // prefs 未被改
        assertThat(fakePrefs.birthday).isEqualTo(999L)
        assertThat(fakePrefs.lifespan).isEqualTo(88)
    }

    @Test
    fun import_idConflict_replacesExisting() = runTest(mainRule.dispatcher) {
        // 预插 id=1 event A
        val eventA = EventEntity(
            id = 1, title = "原", startTime = 100L, endTime = 200L,
            status = 2, note = "old", createdAt = 300L, updatedAt = 400L
        )
        eventRepo.upsertAll(listOf(eventA))
        assertThat(eventRepo.getAllEvents().first()).hasSize(1)

        // 导入含 id=1 event B
        val eventB = EventEntity(
            id = 1, title = "新", startTime = 500L, endTime = 600L,
            status = 2, note = "new", createdAt = 700L, updatedAt = 800L
        )
        val json = buildJson(events = listOf(eventB))

        vm.import(stream(json))
        vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }

        // 仍 1 行，字段为 B
        assertThat(eventRepo.getAllEvents().first()).hasSize(1)
        val row = eventRepo.getEventById(1)!!
        assertThat(row.title).isEqualTo("新")
        assertThat(row.note).isEqualTo("new")

        // 再导一次同文件 → 仍 1 行（幂等）
        vm.resetState()
        vm.import(stream(json))
        vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }
        assertThat(eventRepo.getAllEvents().first()).hasSize(1)
    }

    @Test
    fun import_setsImportingBeforeFinalState() = runTest(mainRule.dispatcher) {
        val json = buildJson()
        val states = mutableListOf<ImportViewModel.ImportState>()

        vm.import(stream(json))
        // 单 collector 链：onEach 记录历史 + first 等待终态，避免双 collector 竞争
        val terminal = vm.state
            .onEach { states.add(it) }
            .first { it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error }
        advanceUntilIdle()

        assertThat(states).contains(ImportViewModel.ImportState.Importing)
        assertThat(terminal).isInstanceOf(ImportViewModel.ImportState.Success::class.java)
        assertThat(states.last()).isEqualTo(terminal)
    }

    @Test
    fun resetState_returnsToIdle() = runTest(mainRule.dispatcher) {
        vm.import(stream(buildJson()))
        vm.state.first {
            it is ImportViewModel.ImportState.Success || it is ImportViewModel.ImportState.Error
        }
        assertThat(vm.state.value)
            .isInstanceOf(ImportViewModel.ImportState.Success::class.java)

        vm.resetState()
        assertThat(vm.state.value).isEqualTo(ImportViewModel.ImportState.Idle)
    }

    @Test
    fun markError_defaultMessage_transitionsToErrorState() = runTest(mainRule.dispatcher) {
        vm.markError()
        assertThat(vm.state.value).isEqualTo(ImportViewModel.ImportState.Error("导入失败，请重试"))
    }

    @Test
    fun markError_customMessage_transitionsToErrorState() = runTest(mainRule.dispatcher) {
        vm.markError("自定义导入错误")
        assertThat(vm.state.value).isEqualTo(ImportViewModel.ImportState.Error("自定义导入错误"))
    }
}

/** 内存版 TimeVizPrefs，用于测试。 */
private class ImportFakeTimeVizPrefs : TimeVizPrefs {
    var birthday: Long = 0L
    var lifespan: Int = 80
    override fun getBirthdayMillis(): Long = birthday
    override fun setBirthdayMillis(millis: Long) { birthday = millis }
    override fun getLifespanYears(): Int = lifespan
    override fun setLifespanYears(years: Int) { lifespan = years }
}
