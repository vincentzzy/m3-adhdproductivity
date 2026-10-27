package com.nowapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.nowapp.domain.Bucket
import com.nowapp.domain.Task

/**
 * Task table. The bucket is stored as its name so the database stays readable
 * and a future bucket change cannot silently reorder rows.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val notes: String = "",
    val bucket: String = Bucket.LATER.name,
    val estimateMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val dueAt: Long? = null,
    val isHardDeadline: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    val anchorId: Long? = null,
    val parentTaskId: Long? = null,
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val taskId: Long? = null,
    val plannedMinutes: Int,
    val actualMinutes: Int,
    val startedAt: Long,
    val endedAt: Long,
)

/** Single-row table (id is always 1) holding the streak. */
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 1,
    val current: Int = 0,
    val best: Int = 0,
    val graceDaysRemaining: Int = 2,
    val lastCompletedDate: String? = null,
    val graceMonthKey: String? = null,
    val lastEvaluatedDate: String? = null,
    val lastDayProtected: Boolean = false,
)

/**
 * Anchors are the user's own daily touch-points ("waking", "after class",
 * "evening"). They drive the three timed pings, nothing else.
 */
@Entity(tableName = "anchors")
data class AnchorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val label: String,
    /** ISO day-of-week numbers, comma separated: "1,2,3,4,5". */
    val daysOfWeek: String = "1,2,3,4,5,6,7",
    /** "07:30" */
    val timeOfDay: String = "08:00",
    val enabled: Boolean = true,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    notes = notes,
    bucket = runCatching { Bucket.valueOf(bucket) }.getOrDefault(Bucket.LATER),
    estimateMinutes = estimateMinutes,
    actualMinutes = actualMinutes,
    dueAt = dueAt,
    isHardDeadline = isHardDeadline,
    createdAt = createdAt,
    completedAt = completedAt,
    anchorId = anchorId,
    parentTaskId = parentTaskId,
)

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    title = title,
    notes = notes,
    bucket = bucket.name,
    estimateMinutes = estimateMinutes,
    actualMinutes = actualMinutes,
    dueAt = dueAt,
    isHardDeadline = isHardDeadline,
    createdAt = createdAt,
    completedAt = completedAt,
    anchorId = anchorId,
    parentTaskId = parentTaskId,
)
