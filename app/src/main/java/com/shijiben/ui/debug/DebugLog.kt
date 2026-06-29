package com.shijiben.ui.debug

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.shijiben.BuildConfig
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

enum class DebugLevel { INFO, WARN, ERROR }

data class DebugEntry(
    val time: Long,
    val level: DebugLevel,
    val tag: String?,
    val summary: String,
    val stacktrace: String?
)

/**
 * 全局调试日志单例。debug 构建收集异常/日志到内存 ring buffer；
 * release 构建所有方法为空操作，业务代码调用安全。
 *
 * 主要给开发期（AI agent / 开发者）用：catch 到的异常扔这里，
 * 配合 [DebugOverlay] 在 app 内直接看 stacktrace，免去反复 adb logcat。
 */
object DebugLog {
    private const val MAX = 200
    private const val CRASH_FILE = "debug-last-crash.txt"

    /** 内存 ring buffer，UI 层直接观察此列表。 */
    val entries: SnapshotStateList<DebugEntry> = mutableStateListOf()

    private var appContext: Context? = null

    /** 在 Application.onCreate 调用（仅 debug 生效）。注册未捕获异常处理 + 读上次崩溃。 */
    fun install(context: Context) {
        if (!BuildConfig.DEBUG) return
        appContext = context.applicationContext
        loadLastCrash()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            persistCrash(throwable)
            previous?.uncaughtException(thread, throwable)
        }
    }

    /** 业务 catch 块主动上报异常。release 构建空操作。 */
    fun report(t: Throwable, tag: String? = null) {
        if (!BuildConfig.DEBUG) return
        add(DebugLevel.ERROR, tag, "${t.javaClass.simpleName}: ${t.message}", stacktraceOf(t))
    }

    /** 普通日志。release 构建空操作。 */
    fun log(msg: String, level: DebugLevel = DebugLevel.INFO, tag: String? = null) {
        if (!BuildConfig.DEBUG) return
        add(level, tag, msg, null)
    }

    private fun add(level: DebugLevel, tag: String?, summary: String, stacktrace: String?) {
        entries.add(DebugEntry(System.currentTimeMillis(), level, tag, summary, stacktrace))
        while (entries.size > MAX) entries.removeAt(0)
    }

    private fun stacktraceOf(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return sw.toString()
    }

    /** 未捕获异常时把 stacktrace 写文件，进程死后下次启动 [loadLastCrash] 读回。 */
    private fun persistCrash(t: Throwable) {
        val ctx = appContext ?: return
        try {
            val content = buildString {
                append("TIME=${System.currentTimeMillis()}\n")
                append("SUMMARY=${t.javaClass.simpleName}: ${t.message}\n")
                append("STACKTRACE_START\n")
                append(stacktraceOf(t))
                append("\nSTACKTRACE_END\n")
            }
            File(ctx.filesDir, CRASH_FILE).writeText(content)
        } catch (_: Throwable) { }
    }

    private fun loadLastCrash() {
        val ctx = appContext ?: return
        try {
            val file = File(ctx.filesDir, CRASH_FILE)
            if (!file.exists()) return
            val content = file.readText()
            file.delete()
            val time = Regex("TIME=(\\d+)").find(content)?.groupValues?.get(1)?.toLongOrNull()
                ?: System.currentTimeMillis()
            val summary = Regex("SUMMARY=(.*)").find(content)?.groupValues?.get(1)
                ?: "Unknown crash"
            val stack = Regex("STACKTRACE_START\\n(.*)\\nSTACKTRACE_END", RegexOption.DOT_MATCHES_ALL)
                .find(content)?.groupValues?.get(1) ?: ""
            entries.add(DebugEntry(time, DebugLevel.ERROR, "UncaughtException", summary, stack))
        } catch (_: Throwable) { }
    }
}
