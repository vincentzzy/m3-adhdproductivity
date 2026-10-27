package com.nowapp.data

import com.nowapp.data.db.FocusDao
import com.nowapp.data.db.FocusSessionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class FocusSession(
    val id: Long,
    val taskId: Long?,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startedAt: Long,
    val endedAt: Long,
)

class FocusRepository(private val dao: FocusDao) {
    val sessions: Flow<List<FocusSession>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun record(taskId: Long?, plannedMinutes: Int, actualMinutes: Int, startedAt: Long, endedAt: Long): Long =
        dao.insert(
            FocusSessionEntity(
                taskId = taskId,
                plannedMinutes = plannedMinutes,
                actualMinutes = actualMinutes,
                startedAt = startedAt,
                endedAt = endedAt,
            )
        )

    suspend fun allOnce(): List<FocusSession> = dao.allOnce().map { it.toDomain() }
}

fun FocusSessionEntity.toDomain(): FocusSession = FocusSession(
    id = id,
    taskId = taskId,
    plannedMinutes = plannedMinutes,
    actualMinutes = actualMinutes,
    startedAt = startedAt,
    endedAt = endedAt,
)
