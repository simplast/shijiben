package com.shijiben.test

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * 共享 JUnit TestRule：绑定 TestDispatcher 到 Dispatchers.Main。
 *
 * 8 个 ViewModel 测试共用（heatmap/timeline/search/notes/recording/settings/timeviz 等）。
 * 之前放在 feature.recording 包下导致跨包 import，现移到 com.shijiben.test 共享。
 *
 * 默认 UnconfinedTestDispatcher；需要队列串行化的测试（flaky 根治场景）传 StandardTestDispatcher。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainCoroutineRule(
    val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
