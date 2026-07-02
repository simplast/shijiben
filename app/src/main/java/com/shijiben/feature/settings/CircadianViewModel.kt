package com.shijiben.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shijiben.data.repository.EventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import javax.inject.Inject

/**
 * 本地生物钟 ViewModel。注入 EventRepository + Clock（与 TimeAllocationViewModel 同款）。
 *
 * collect getAllEvents() Flow → map 成 circadianDistribution（按 clock.zone 聚合到 24 桶）
 * → expose StateFlow<List<HourBucket>>。永远 24 桶；loading 期为 emptyList（UI 显示"加载中…"）。
 *
 * 纯只读：不修改任何事件数据，是"事实镜"。
 */
@HiltViewModel
class CircadianViewModel @Inject constructor(
    private val eventRepository: EventRepository,
    private val clock: Clock
) : ViewModel() {

    val buckets: StateFlow<List<CircadianCalculator.HourBucket>> =
        eventRepository.getAllEvents()
            .map { events ->
                CircadianCalculator.circadianDistribution(events, clock.zone)
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )
}
