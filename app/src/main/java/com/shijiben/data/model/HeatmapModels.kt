package com.shijiben.data.model

import java.time.LocalDate

/** 单日聚合结果（内存聚合产物，不落库） */
data class DailyActivity(
    val date: LocalDate,       // 本地时区日期
    val eventCount: Int,       // 当天归属的事件数（含 not_started，供未来扩展）
    val durationMs: Long       // 当天有效记录时长（仅 in_progress + completed，跨日已截断）
)
