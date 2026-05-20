package com.doer.shijiben.data

import kotlinx.coroutines.flow.Flow

class EventRepository(private val dao: EventDao) {
    fun observeEventsForDay(dayKey: String): Flow<List<EventEntity>> =
        dao.observeEventsForDay(dayKey)

    fun observeRecentDistinctEventNames(): Flow<List<String>> =
        dao.observeRecentDistinctNames()

    suspend fun getById(id: Long): EventEntity? = dao.getById(id)

    suspend fun upsert(event: EventEntity) {
        val withDay = event.withDerivedDayKey()
        if (event.id == 0L) {
            dao.insert(withDay)
        } else {
            dao.update(withDay)
        }
    }

    suspend fun delete(event: EventEntity) {
        dao.delete(event)
    }

    private fun EventEntity.withDerivedDayKey(): EventEntity =
        copy(dayKey = TimeFormats.dayKeyFromMillis(startTimeMillis))
}
