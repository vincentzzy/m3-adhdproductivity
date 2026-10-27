package com.nowapp.domain

/**
 * The hard caps, and how the app resolves overflow without asking the user a question.
 *
 * Rules encoded here (and unit tested):
 *  - TODAY holds at most 3 items. Overflow waits silently in LATER.
 *  - NOW holds at most 1 item. Promoting a second item pushes the old one out.
 *  - Nothing is ever deleted or hidden by these moves, and nothing turns red:
 *    a task that does not fit is simply "carried over" and keeps its neutral look.
 */
object BucketRules {

    const val TODAY_CAP = 3
    const val NOW_CAP = 1

    /** Result of a bucket move: the new task list plus an optional neutral message. */
    data class MoveResult(val tasks: List<Task>, val message: String? = null)

    /**
     * Moves [taskId] into [target], enforcing both caps by swapping rather than refusing.
     * Swapping is the key anti-abandonment choice: a full TODAY never produces a
     * blocking error, it produces a quiet exchange.
     */
    fun move(tasks: List<Task>, taskId: Long, target: Bucket): MoveResult {
        val moving = tasks.firstOrNull { it.id == taskId }
            ?: return MoveResult(tasks, null)
        if (moving.bucket == target) return MoveResult(tasks, null)

        var result = tasks.map { if (it.id == taskId) it.copy(bucket = target) else it }
        var message: String? = null

        when (target) {
            Bucket.NOW -> {
                // Only one NOW: the previous inhabitant steps back to TODAY if it fits.
                val others = result.filter { it.bucket == Bucket.NOW && it.id != taskId && !it.isDone }
                others.forEach { previous ->
                    val todayCount = result.count { it.bucket == Bucket.TODAY && !it.isDone }
                    val destination = if (todayCount < TODAY_CAP) Bucket.TODAY else Bucket.LATER
                    result = result.map { if (it.id == previous.id) it.copy(bucket = destination) else it }
                    message = "\"${previous.title.take(24)}\" moved to ${destination.label.lowercase()}"
                }
            }

            Bucket.TODAY -> {
                val today = result.filter { it.bucket == Bucket.TODAY && !it.isDone && it.id != taskId }
                if (today.size >= TODAY_CAP) {
                    // The oldest TODAY item is the one that has been waiting longest.
                    val victim = today.minByOrNull { it.createdAt } ?: today.first()
                    result = result.map { if (it.id == victim.id) it.copy(bucket = Bucket.LATER) else it }
                    message = "Today was full — \"${victim.title.take(24)}\" is waiting in Later"
                }
            }

            Bucket.LATER -> Unit // Always allowed. Later never nags.
        }
        return MoveResult(result, message)
    }

    /**
     * Where a freshly captured task lands. Capture never asks the user to choose,
     * so the default is the quiet bucket; the user can opt into TODAY with one
     * extra tap on the capture row. If TODAY is full the task silently waits in LATER.
     */
    fun destinationForCapture(tasks: List<Task>, userChoseToday: Boolean): Bucket {
        if (!userChoseToday) return Bucket.LATER
        val todayCount = tasks.count { it.bucket == Bucket.TODAY && !it.isDone }
        return if (todayCount < TODAY_CAP) Bucket.TODAY else Bucket.LATER
    }

    /** The single NOW task, if any. Completed tasks never occupy NOW. */
    fun nowTask(tasks: List<Task>): Task? = tasks.firstOrNull { it.bucket == Bucket.NOW && !it.isDone }

    fun todayTasks(tasks: List<Task>): List<Task> =
        tasks.filter { it.bucket == Bucket.TODAY && !it.isDone }.sortedBy { it.createdAt }

    fun laterTasks(tasks: List<Task>): List<Task> =
        tasks.filter { it.bucket == Bucket.LATER && !it.isDone }.sortedBy { it.createdAt }

    /** How many of today's three are done — 1 of 3 is presented as a good day. */
    fun completedTodayCount(tasks: List<Task>, dayStartMillis: Long): Int =
        tasks.count { it.completedAt != null && it.completedAt >= dayStartMillis }
}

/** The [Bucket.logical] label used in neutral, non-judgemental copy. */
val Bucket.label: String
    get() = when (this) {
        Bucket.NOW -> "Now"
        Bucket.TODAY -> "Today"
        Bucket.LATER -> "Later"
    }

/** Lowercase form for inline sentences. */
val Bucket.logical: String get() = label.lowercase()
