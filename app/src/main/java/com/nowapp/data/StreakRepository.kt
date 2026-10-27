package com.nowapp.data

import com.nowapp.data.db.StreakDao
import com.nowapp.data.db.StreakEntity
import com.nowapp.domain.StreakMath
import com.nowapp.domain.StreakState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Persists StreakState and applies the grace-day rules from [StreakMath]. */
class StreakRepository(private val dao: StreakDao) {

    val state: Flow<StreakState> = dao.observe().map { it?.toDomain() ?: StreakState() }

    suspend fun current(): StreakState = dao.get()?.toDomain() ?: StreakState()

    suspend fun markQualified(today: LocalDate, now: Long = System.currentTimeMillis()) {
        evaluate(today, qualifiedToday = true)
    }

    /**
     * Called on app start and from the daily worker: rolls the streak forward,
     * spending grace days for any days that were skipped.
     */
    suspend fun rollForward(today: LocalDate) = evaluate(today, qualifiedToday = false)

    private suspend fun evaluate(today: LocalDate, qualifiedToday: Boolean) {
        val current = current()
        val next = StreakMath.advance(current, today, qualifiedToday)
        dao.put(next.toEntity())
    }

    suspend fun replace(state: StreakState) = dao.put(state.toEntity())

    suspend fun clear() = dao.clear()
}

fun StreakEntity.toDomain(): StreakState = StreakState(
    current = current,
    best = best,
    graceDaysRemaining = graceDaysRemaining,
    lastCompletedDate = lastCompletedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    graceMonthKey = graceMonthKey,
    lastEvaluatedDate = lastEvaluatedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    lastDayProtected = lastDayProtected,
)

fun StreakState.toEntity(): StreakEntity = StreakEntity(
    current = current,
    best = best,
    graceDaysRemaining = graceDaysRemaining,
    lastCompletedDate = lastCompletedDate?.toString(),
    graceMonthKey = graceMonthKey,
    lastEvaluatedDate = lastEvaluatedDate?.toString(),
    lastDayProtected = lastDayProtected,
)
