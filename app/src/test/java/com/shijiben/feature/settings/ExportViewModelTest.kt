package com.shijiben.feature.settings

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.shijiben.data.local.AppDatabase
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
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import org.json.JSONObject

/**
 * ExportViewModel 单测（spec §7.4）。
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
class ExportViewModelTest {

    @get:Rule
    val mainRule = MainCoroutineRule(StandardTestDispatcher())

    private lateinit var db: AppDatabase
    private lateinit var eventRepo: EventRepository
    private lateinit var noteRepo: NoteRepository
    private lateinit var fakePrefs: FakeTimeVizPrefs
    private lateinit var vm: ExportViewModel

    @Before
    fun setup() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        eventRepo = EventRepository(db.eventDao())
        noteRepo = NoteRepository(db.noteDao())
        fakePrefs = FakeTimeVizPrefs()
        vm = ExportViewModel(eventRepo, noteRepo, fakePrefs, mainRule.dispatcher)
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun export_emptyData_transitionsToSuccessAndWritesValidJson() = runTest(mainRule.dispatcher) {
        val out = ByteArrayOutputStream()
        vm.export(out, "backup.json")
        vm.state.first { it is ExportViewModel.ExportState.Success || it is ExportViewModel.ExportState.Error }

        assertThat(vm.state.value).isEqualTo(ExportViewModel.ExportState.Success("backup.json"))
        val root = JSONObject(String(out.toByteArray(), Charsets.UTF_8))
        assertThat(root.getInt("schemaVersion")).isEqualTo(1)
        assertThat(root.getJSONArray("events").length()).isEqualTo(0)
        assertThat(root.getJSONArray("notes").length()).isEqualTo(0)
    }

    @Test
    fun export_withData_writesAllEventsAndNotes() = runTest(mainRule.dispatcher) {
        eventRepo.createEvent("event1", 1000L, 2000L, "n1")
        eventRepo.createEvent("event2", 3000L, null, null)
        noteRepo.createNote("a note", 5000L)
        fakePrefs.birthday = 1234567890L
        fakePrefs.lifespan = 99

        val out = ByteArrayOutputStream()
        vm.export(out, "backup.json")
        vm.state.first { it is ExportViewModel.ExportState.Success || it is ExportViewModel.ExportState.Error }

        assertThat(vm.state.value).isEqualTo(ExportViewModel.ExportState.Success("backup.json"))
        val root = JSONObject(String(out.toByteArray(), Charsets.UTF_8))
        assertThat(root.getJSONArray("events").length()).isEqualTo(2)
        assertThat(root.getJSONArray("notes").length()).isEqualTo(1)
        assertThat(root.getJSONObject("timeVizPrefs").getLong("birthdayMillis")).isEqualTo(1234567890L)
        assertThat(root.getJSONObject("timeVizPrefs").getInt("lifespanYears")).isEqualTo(99)
    }

    @Test
    fun export_streamThrows_transitionsToError() = runTest(mainRule.dispatcher) {
        val throwingStream = object : OutputStream() {
            override fun write(b: Int) { throw IOException("boom") }
            override fun write(b: ByteArray, off: Int, len: Int) { throw IOException("boom") }
        }
        vm.export(throwingStream, "backup.json")
        vm.state.first { it is ExportViewModel.ExportState.Success || it is ExportViewModel.ExportState.Error }

        assertThat(vm.state.value)
            .isEqualTo(ExportViewModel.ExportState.Error("导出失败，请重试"))
    }

    @Test
    fun export_setsExportingBeforeFinalState() = runTest(mainRule.dispatcher) {
        val states = mutableListOf<ExportViewModel.ExportState>()

        val out = ByteArrayOutputStream()
        vm.export(out, "backup.json")
        // 单 collector 链：onEach 记录历史 + first 等待终态，避免双 collector 竞争导致
        // toList 错过终态 emission（first 完成即恢复 test body，toList 可能未及处理）
        val terminal = vm.state
            .onEach { states.add(it) }
            .first { it is ExportViewModel.ExportState.Success || it is ExportViewModel.ExportState.Error }
        advanceUntilIdle()

        assertThat(states).contains(ExportViewModel.ExportState.Exporting)
        assertThat(terminal).isEqualTo(ExportViewModel.ExportState.Success("backup.json"))
        assertThat(states.last()).isEqualTo(terminal)
    }

    @Test
    fun resetState_returnsToIdle() = runTest(mainRule.dispatcher) {
        val out = ByteArrayOutputStream()
        vm.export(out, "backup.json")
        vm.state.first { it is ExportViewModel.ExportState.Success || it is ExportViewModel.ExportState.Error }
        assertThat(vm.state.value)
            .isInstanceOf(ExportViewModel.ExportState.Success::class.java)

        vm.resetState()
        assertThat(vm.state.value).isEqualTo(ExportViewModel.ExportState.Idle)
    }

    @Test
    fun markError_defaultMessage_transitionsToErrorState() = runTest(mainRule.dispatcher) {
        vm.markError()
        assertThat(vm.state.value).isEqualTo(ExportViewModel.ExportState.Error("导出失败，请重试"))
    }

    @Test
    fun markError_customMessage_transitionsToErrorState() = runTest(mainRule.dispatcher) {
        vm.markError("自定义导出错误")
        assertThat(vm.state.value).isEqualTo(ExportViewModel.ExportState.Error("自定义导出错误"))
    }
}

/** 内存版 TimeVizPrefs，用于测试。 */
private class FakeTimeVizPrefs : TimeVizPrefs {
    var birthday: Long = 0L
    var lifespan: Int = 80
    override fun getBirthdayMillis(): Long = birthday
    override fun setBirthdayMillis(millis: Long) { birthday = millis }
    override fun getLifespanYears(): Int = lifespan
    override fun setLifespanYears(years: Int) { lifespan = years }
}
