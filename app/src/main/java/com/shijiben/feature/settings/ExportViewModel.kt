package com.shijiben.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.BuildConfig
import com.shijiben.data.export.DataExportManager
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.di.IoDispatcher
import com.shijiben.feature.timeviz.TimeVizPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * 数据导出 ViewModel。负责把本地数据序列化为 JSON 并写入用户通过 SAF 选定的输出流。
 *
 * - 状态机：Idle → Exporting →（Success(fileName) | Error(msg)）→ resetState() → Idle。
 * - 序列化 + 取数 + 写流在 [ioDispatcher] 上执行；生产 Dispatchers.IO，测试注入 UnconfinedTestDispatcher。
 * - 不联网、不读 Uri（outputStream 由 UI 从 SAF Uri 开出后传入）。
 */
@HiltViewModel
class ExportViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val timeVizPrefs: TimeVizPrefs,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    sealed interface ExportState {
        data object Idle : ExportState
        data object Exporting : ExportState
        data class Success(val fileName: String) : ExportState
        data class Error(val message: String) : ExportState
    }

    private val _state = MutableStateFlow<ExportState>(ExportState.Idle)
    val state: StateFlow<ExportState> = _state.asStateFlow()

    fun export(outputStream: OutputStream, fileName: String) {
        viewModelScope.launch {
            _state.value = ExportState.Exporting
            try {
                withContext(ioDispatcher) {
                    val events = eventRepository.getAllEvents().first()
                    val notes = noteRepository.getAllNotes().first()
                    val json = DataExportManager.buildJsonString(
                        events = events,
                        notes = notes,
                        birthdayMillis = timeVizPrefs.getBirthdayMillis(),
                        lifespanYears = timeVizPrefs.getLifespanYears(),
                        appVersion = BuildConfig.VERSION_NAME,
                        exportedAt = System.currentTimeMillis()
                    )
                    DataExportManager.writeToStream(json, outputStream)
                }
                _state.value = ExportState.Success(fileName)
            } catch (e: Exception) {
                _state.value = ExportState.Error("导出失败，请重试")
            }
        }
    }

    /** UI 消费 Success/Error 后调，回 Idle。 */
    fun resetState() { _state.value = ExportState.Idle }

    /** SAF openOutputStream 返回 null 等边界由 UI 直接触发 Error。 */
    fun markError(msg: String = "导出失败，请重试") {
        _state.value = ExportState.Error(msg)
    }

    companion object {
        private val FILE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")

        fun generateFileName(): String =
            "shijiben_backup_${LocalDateTime.now().format(FILE_FMT)}.json"
    }
}
