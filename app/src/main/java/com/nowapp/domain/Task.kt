package com.nowapp.domain

/** The entire mental model of the app: three buckets, no projects, no tags. */
enum class Bucket { NOW, TODAY, LATER }

/**
 * Domain view of a task. Deliberately flat: there is no project hierarchy,
 * no tags, no priority field, because deciding any of those is the failure
 * mode this app exists to avoid.
 */
data class Task(
    val id: Long,
    val title: String,
    val notes: String = "",
    val bucket: Bucket = Bucket.LATER,
    val estimateMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val dueAt: Long? = null,
    val isHardDeadline: Boolean = false,
    val createdAt: Long = 0L,
    val completedAt: Long? = null,
    val anchorId: Long? = null,
    val parentTaskId: Long? = null,
) {
    val isDone: Boolean get() = completedAt != null
    /** Hard-deadline tasks are the only ones allowed to escalate. */
    val isAtRisk: Boolean get() = isHardDeadline && dueAt != null && completedAt == null
}
