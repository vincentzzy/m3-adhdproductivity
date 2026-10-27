package com.nowapp.data

import com.nowapp.data.db.AnchorEntity
import com.nowapp.data.db.FocusSessionEntity
import com.nowapp.data.db.StreakEntity
import com.nowapp.data.db.TaskEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Plain-JSON backup. No binary formats, no cloud, no login: the user picks a
 * file, the app writes it, and the same file can be read back.
 */
object Backup {

    private const val VERSION = 1

    data class Snapshot(
        val tasks: List<TaskEntity>,
        val sessions: List<FocusSessionEntity>,
        val streak: StreakEntity?,
        val anchors: List<AnchorEntity>,
    )

    fun encode(
        tasks: List<TaskEntity>,
        sessions: List<FocusSessionEntity>,
        streak: StreakEntity?,
        anchors: List<AnchorEntity>,
    ): String {
        val root = JSONObject()
        root.put("version", VERSION)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("tasks", JSONArray().apply {
            tasks.forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("notes", task.notes)
                    put("bucket", task.bucket)
                    task.estimateMinutes?.let { put("estimateMinutes", it) }
                    task.actualMinutes?.let { put("actualMinutes", it) }
                    task.dueAt?.let { put("dueAt", it) }
                    put("isHardDeadline", task.isHardDeadline)
                    put("createdAt", task.createdAt)
                    task.completedAt?.let { put("completedAt", it) }
                    task.anchorId?.let { put("anchorId", it) }
                    task.parentTaskId?.let { put("parentTaskId", it) }
                })
            }
        })
        root.put("sessions", JSONArray().apply {
            sessions.forEach { session ->
                put(JSONObject().apply {
                    put("id", session.id)
                    session.taskId?.let { put("taskId", it) }
                    put("plannedMinutes", session.plannedMinutes)
                    put("actualMinutes", session.actualMinutes)
                    put("startedAt", session.startedAt)
                    put("endedAt", session.endedAt)
                })
            }
        })
        streak?.let { s ->
            root.put("streak", JSONObject().apply {
                put("current", s.current)
                put("best", s.best)
                put("graceDaysRemaining", s.graceDaysRemaining)
                s.lastCompletedDate?.let { put("lastCompletedDate", it) }
                s.graceMonthKey?.let { put("graceMonthKey", it) }
                s.lastEvaluatedDate?.let { put("lastEvaluatedDate", it) }
                put("lastDayProtected", s.lastDayProtected)
            })
        }
        root.put("anchors", JSONArray().apply {
            anchors.forEach { anchor ->
                put(JSONObject().apply {
                    put("label", anchor.label)
                    put("daysOfWeek", anchor.daysOfWeek)
                    put("timeOfDay", anchor.timeOfDay)
                    put("enabled", anchor.enabled)
                })
            }
        })
        return root.toString(2)
    }

    /** Throws [IllegalArgumentException] with a human message when the file is not ours. */
    fun decode(json: String): Snapshot {
        val root = runCatching { JSONObject(json) }
            .getOrElse { throw IllegalArgumentException("That file does not look like a Now backup.") }
        val version = root.optInt("version", 0)
        if (version < 1) throw IllegalArgumentException("Unsupported backup version.")

        val tasks = root.optJSONArray("tasks")?.let { array ->
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                TaskEntity(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    notes = o.optString("notes"),
                    bucket = o.optString("bucket", "LATER"),
                    estimateMinutes = if (o.has("estimateMinutes")) o.optInt("estimateMinutes") else null,
                    actualMinutes = if (o.has("actualMinutes")) o.optInt("actualMinutes") else null,
                    dueAt = if (o.has("dueAt")) o.optLong("dueAt") else null,
                    isHardDeadline = o.optBoolean("isHardDeadline", false),
                    createdAt = o.optLong("createdAt"),
                    completedAt = if (o.has("completedAt")) o.optLong("completedAt") else null,
                    anchorId = if (o.has("anchorId")) o.optLong("anchorId") else null,
                    parentTaskId = if (o.has("parentTaskId")) o.optLong("parentTaskId") else null,
                )
            }
        } ?: emptyList()

        val sessions = root.optJSONArray("sessions")?.let { array ->
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                FocusSessionEntity(
                    id = o.optLong("id"),
                    taskId = if (o.has("taskId")) o.optLong("taskId") else null,
                    plannedMinutes = o.optInt("plannedMinutes"),
                    actualMinutes = o.optInt("actualMinutes"),
                    startedAt = o.optLong("startedAt"),
                    endedAt = o.optLong("endedAt"),
                )
            }
        } ?: emptyList()

        val streak = root.optJSONObject("streak")?.let { o ->
            StreakEntity(
                current = o.optInt("current"),
                best = o.optInt("best"),
                graceDaysRemaining = o.optInt("graceDaysRemaining", 2),
                lastCompletedDate = if (o.has("lastCompletedDate")) o.optString("lastCompletedDate") else null,
                graceMonthKey = if (o.has("graceMonthKey")) o.optString("graceMonthKey") else null,
                lastEvaluatedDate = if (o.has("lastEvaluatedDate")) o.optString("lastEvaluatedDate") else null,
                lastDayProtected = o.optBoolean("lastDayProtected", false),
            )
        }

        val anchors = root.optJSONArray("anchors")?.let { array ->
            (0 until array.length()).map { index ->
                val o = array.getJSONObject(index)
                AnchorEntity(
                    label = o.optString("label", "Anchor"),
                    daysOfWeek = o.optString("daysOfWeek", "1,2,3,4,5,6,7"),
                    timeOfDay = o.optString("timeOfDay", "08:00"),
                    enabled = o.optBoolean("enabled", true),
                )
            }
        } ?: emptyList()

        return Snapshot(tasks, sessions, streak, anchors)
    }
}
