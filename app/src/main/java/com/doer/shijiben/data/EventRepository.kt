package com.doer.shijiben.data

import kotlinx.coroutines.flow.Flow

class EventRepository(
    private val dao: EventDao,
    private val goalDao: GoalDao
) {
    fun observeEventsForDay(dayKey: String): Flow<List<EventEntity>> =
        dao.observeEventsForDay(dayKey)

    fun observeRecentDistinctEventNames(): Flow<List<String>> =
        dao.observeRecentDistinctNames()

    fun observeActiveEvent(): Flow<EventEntity?> = dao.observeActiveEvent()

    fun observeEventsInRange(startDay: String, endDay: String): Flow<List<EventEntity>> =
        dao.observeEventsInRange(startDay, endDay)

    fun observeGoalsForWeek(weekKey: String): Flow<List<GoalEntity>> =
        goalDao.observeGoalsForWeek(weekKey)

    suspend fun upsertGoal(goal: GoalEntity) = goalDao.upsert(goal)
    suspend fun deleteGoal(goal: GoalEntity) = goalDao.delete(goal)

    suspend fun getAllEvents(): List<EventEntity> = dao.getAllEvents()

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
