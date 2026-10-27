package com.nowapp.data

import com.nowapp.data.db.FocusSessionEntity
import com.nowapp.data.db.FocusDao
import com.nowapp.data.db.TaskDao
import com.nowapp.data.db.TaskEntity
import com.nowapp.data.db.toDomain
import com.nowapp.data.db.toEntity
import com.nowapp.domain.Breakdown
import com.nowapp.domain.Bucket
import com.nowapp.domain.BucketRules
import com.nowapp.domain.EstimateSample
import com.nowapp.domain.Task
import com.nowapp.domain.TimeMath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Single source of truth for tasks.
 *
 * Every mutation funnels through here so that bucket caps and streak updates
 * cannot be bypassed by a UI shortcut.
 */
class TaskRepository(
    private val taskDao: TaskDao,
    private val focusDao: FocusDao,
    private val streakRepository: StreakRepository,
) {
    val tasks: Flow<List<Task>> = taskDao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun allOnce(): List<Task> = taskDao.allOnce().map { it.toDomain() }

    /**
     * Instant capture. One call, no required fields beyond the title, and the
     * bucket is decided for the user by [BucketRules.destinationForCapture].
     */
    suspend fun capture(title: String, intoToday: Boolean = false, now: Long = System.currentTimeMillis()): Long {
        val trimmed = title.trim().ifEmpty { return -1L }
        val existing = allOnce()
        val bucket = BucketRules.destinationForCapture(existing, intoToday)
        return taskDao.insert(
            TaskEntity(
                title = trimmed.take(200),
                bucket = bucket.name,
                createdAt = now,
            )
        )
    }

    suspend fun complete(taskId: Long, actualMinutes: Int? = null, now: Long = System.currentTimeMillis()) {
        val task = taskDao.byId(taskId) ?: return
        taskDao.update(task.copy(completedAt = now, actualMinutes = actualMinutes ?: task.actualMinutes))
        streakRepository.markQualified(TimeMath.today(), now)
    }

    suspend fun uncomplete(taskId: Long) {
        val task = taskDao.byId(taskId) ?: return
        taskDao.update(task.copy(completedAt = null))
    }

    /** Returns a neutral message when the hard cap forced a swap. */
    suspend fun move(taskId: Long, target: Bucket): String? {
        val before = allOnce()
        val result = BucketRules.move(before, taskId, target)
        // Only write the rows whose bucket actually changed.
        for (updated in result.tasks) {
            val old = before.firstOrNull { it.id == updated.id } ?: continue
            if (old.bucket != updated.bucket) {
                taskDao.byId(updated.id)?.let { entity ->
                    taskDao.update(entity.copy(bucket = updated.bucket.name))
                }
            }
        }
        return result.message
    }

    suspend fun rename(taskId: Long, title: String) {
        val task = taskDao.byId(taskId) ?: return
        taskDao.update(task.copy(title = title.trim().take(200)))
    }

    suspend fun setActual(taskId: Long, minutes: Int?) {
        val task = taskDao.byId(taskId) ?: return
        taskDao.update(task.copy(actualMinutes = minutes?.takeIf { it > 0 }))
    }

    suspend fun setEstimate(taskId: Long, minutes: Int?) {
        val task = taskDao.byId(taskId) ?: return
        taskDao.update(task.copy(estimateMinutes = minutes?.takeIf { it > 0 }))
    }

    suspend fun setDue(taskId: Long, dueAt: Long?, hardDeadline: Boolean) {
        val task = taskDao.byId(taskId) ?: return
        taskDao.update(task.copy(dueAt = dueAt, isHardDeadline = hardDeadline))
    }

    suspend fun setNotes(taskId: Long, notes: String) {
        val task = taskDao.byId(taskId) ?: return
        taskDao.update(task.copy(notes = notes.take(4000)))
    }

    suspend fun delete(taskId: Long) = taskDao.delete(taskId)

    /**
     * Applies a breakdown the only way that keeps the app usable: the first step
     * (<= 5 minutes) becomes the task itself, the rest become its notes check-list.
     * One tap, no new rows competing for the three TODAY slots.
     */
    suspend fun applyBreakdown(taskId: Long, breakdown: Breakdown) {
        val task = taskDao.byId(taskId) ?: return
        val first = breakdown.steps.first()
        val rest = breakdown.steps.drop(1)
        val notes = buildString {
            appendLine("Steps:")
            rest.forEachIndexed { index, step -> appendLine("${index + 2}. ${step.text} (${step.minutes}m)") }
        }
        taskDao.update(
            task.copy(
                title = first.text.take(200),
                notes = notes,
                estimateMinutes = first.minutes,
            )
        )
    }

    /** Estimate-vs-actual samples for the calibration multiplier. */
    fun samples(tasks: List<Task>): List<EstimateSample> = tasks.mapNotNull { task ->
        val estimate = task.estimateMinutes
        val actual = task.actualMinutes
        if (estimate != null && actual != null && estimate > 0 && actual > 0) {
            EstimateSample(estimate, actual)
        } else {
            null
        }
    }

    suspend fun logFocusSession(session: FocusSessionEntity): Long = focusDao.insert(session)

    suspend fun replaceAll(tasks: List<Task>, sessions: List<FocusSessionEntity>) {
        taskDao.clear()
        focusDao.clear()
        taskDao.insertAll(tasks.map { it.toEntity() })
        if (sessions.isNotEmpty()) focusDao.insertAll(sessions)
    }

    suspend fun replaceAllRaw(entities: List<TaskEntity>, sessions: List<FocusSessionEntity>) {
        taskDao.clear()
        focusDao.clear()
        taskDao.insertAll(entities)
        if (sessions.isNotEmpty()) focusDao.insertAll(sessions)
    }

    suspend fun clearAll() {
        taskDao.clear()
        focusDao.clear()
    }

    /** Completed today, used for "1 of 3 is a good day". */
    fun completedToday(tasks: List<Task>, today: LocalDate = TimeMath.today()): Int {
        val start = TimeMath.startOfDayMillis(today)
        return BucketRules.completedTodayCount(tasks, start)
    }
}
