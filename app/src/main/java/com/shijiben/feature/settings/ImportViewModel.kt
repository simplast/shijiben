package com.shijiben.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.export.DataImportManager
import com.shijiben.data.repository.EventRepository
import com.shijiben.data.repository.NoteRepository
import com.shijiben.di.IoDispatcher
import com.shijiben.feature.timeviz.TimeVizPrefs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject

/**
 * 数据导入 ViewModel。负责把用户通过 SAF 选定的输入流读为 JSON、解析、落库。
 *
 * - 状态机：Idle → Importing →（Success(events,notes,prefsUpdated) | Error(msg)）→ resetState() → Idle。
 * - 读流 + 解析 + 落库在 [ioDispatcher] 上执行；生产 Dispatchers.IO，测试注入 StandardTestDispatcher。
 * - 不联网、不读 Uri（inputStream 由 UI 从 SAF Uri 开出后传入）。
 */
@HiltViewModel
class ImportViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val noteRepository: NoteRepository,
    private val timeVizPrefs: TimeVizPrefs,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    sealed interface ImportState {
        data object Idle : ImportState
        data object Importing : ImportState
        data class Success(val events: Int, val notes: Int, val prefsUpdated: Boolean) : ImportState
        data class Error(val message: String) : ImportState
    }

    private val _state = MutableStateFlow<ImportState>(ImportState.Idle)
    val state: StateFlow<ImportState> = _state.asStateFlow()

    fun import(inputStream: InputStream) {
        viewModelScope.launch {
            _state.value = ImportState.Importing
            try {
                val success = withContext(ioDispatcher) {
                    val json = DataImportManager.readFromStream(inputStream)
                    val result = DataImportManager.parseJsonString(json)
                    val counts = DataImportManager.applyImport(result, eventRepository, noteRepository)
                    val prefsUpdated = result.timeVizPrefs != null
                    if (prefsUpdated) {
                        result.timeVizPrefs!!.let {
                            timeVizPrefs.setBirthdayMillis(it.birthdayMillis)
                            timeVizPrefs.setLifespanYears(it.lifespanYears)
                        }
                    }
                    ImportState.Success(
                        events = counts.eventsImported,
                        notes = counts.notesImported,
                        prefsUpdated = prefsUpdated
                    )
                }
                _state.value = success
            } catch (e: Exception) {
                _state.value = ImportState.Error("导入失败，请重试")
            }
        }
    }

    /** UI 消费 Success/Error 后调，回 Idle。 */
    fun resetState() { _state.value = ImportState.Idle }

    /** SAF openInputStream 返回 null 等边界由 UI 直接触发 Error。 */
    fun markError(msg: String = "导入失败，请重试") {
        _state.value = ImportState.Error(msg)
    }
}
