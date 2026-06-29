# Debug Console Overlay — 设计 spec

> 2026-06-29 · 给开发期（AI agent / 开发者）用的 app 内调试 console，类似前端 vConsole，免去反复 adb logcat。

## 目的

app 内全局悬浮按钮唤起调试面板，收集并展示：
- 未捕获异常（崩溃）的完整 stacktrace，进程死后下次启动仍可查看
- 业务代码 `try-catch` 块主动上报的异常
- 可选的普通日志

仅 debug 构建启用（`BuildConfig.DEBUG` 守卫）；release 构建零代码路径、零悬浮 UI。

## 架构

```
MainActivity.setContent
  └─ if (DEBUG) DebugOverlay { AppNavHost(...) }   // 顶层包裹
       ├─ content()                                  // 实际 app 内容
       ├─ 悬浮小圆点（可拖动 + 点击）
       └─ 展开面板（全屏半透明，LazyColumn 列出 DebugEntry）
DebugLog（单例，全构建初始化，release 为空操作）
  ├─ entries: SnapshotStateList<DebugEntry>  // 内存 ring buffer，上限 200
  ├─ report(t, tag?)                          // 业务 catch 块调用
  ├─ log(msg, level)                          // 普通日志
  ├─ 启动时从 debug-crashes.json 读上次崩溃灌入
  └─ UncaughtExceptionHandler：捕获 → 写文件 → 交回默认 handler
```

## 组件

### DebugLog（`ui/debug/DebugLog.kt`）

单例 object。全构建可见，release 方法体为空操作（业务代码 `DebugLog.report` 调用不崩）。

- `entries: SnapshotStateList<DebugEntry>` — 内存 ring buffer，上限 200，溢出删最旧
- `fun report(t: Throwable, tag: String? = null)` — 存 stacktrace 到 buffer；debug 构建有效
- `fun log(msg: String, level: Level = Level.INFO)` — 普通日志
- `fun install()` — 在 Application.onCreate 调用（仅 debug）：注册 UncaughtExceptionHandler，从 `debug-crashes.json` 读上次崩溃灌入 buffer
- UncaughtExceptionHandler 逻辑：捕获 → 把 stacktrace 序列化写 `debug-crashes.json`（内部存储）→ 调默认 handler 让进程按原逻辑死

`DebugEntry`：`data class DebugEntry(val time: Long, val level: Level, val tag: String?, val summary: String, val stacktrace: String?)`

### DebugOverlay（`ui/debug/DebugOverlay.kt`）

`@Composable fun DebugOverlay(content: @Composable () -> Unit)`

- `Box { content(); if (DEBUG) { FAB + Panel } }`
- 悬浮小圆点：24dp 半透明红，`Modifier.pointerInput` 拖动（存 offset 于 `remember`）+ 点击展开
- 展开面板：全屏半透明背景，顶部 Tab（全部/崩溃/日志），`LazyColumn` 列 `DebugEntry`（时间 + tag + 摘要），点单条展开完整 stacktrace（等宽字体可滚动）
- 关闭按钮回悬浮态

### 注入点

`MainActivity.kt`：
```kotlin
setContent {
    if (BuildConfig.DEBUG) {
        DebugOverlay { AppNavHost(...) }
    } else {
        AppNavHost(...)
    }
}
```

`ShijibenApp.kt`（Application）：`onCreate` 里 `if (BuildConfig.DEBUG) DebugLog.install()`。

## 启用条件

- `BuildConfig.DEBUG` 守卫所有 UI 与 handler 注册
- `DebugLog` 单例本身全构建存在，release 为空操作，保证业务 `DebugLog.report` 不崩
- release APK 无悬浮按钮、无文件读写、无 handler 注册

## 验证

- debug 构建：悬浮按钮可见，点击展开面板
- 人工触发一个 catch 上报（或未捕获异常）：面板能看到 stacktrace
- 重启 app：上次未捕获崩溃仍在列表
- release 构建（assembleRelease）：悬浮按钮不出现，`DebugLog.report` 不崩
- 4 道：compileDebugKotlin + testDebugUnitTest + assembleDebug + assembleRelease 全绿
