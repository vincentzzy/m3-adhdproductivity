package com.taskd.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.taskd.app.domain.Rules
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settings by preferencesDataStore("taskd_preferences")

class TaskRepository(context: Context) {
    private val app = context.applicationContext
    private val dao = TaskDatabase.get(app).taskDao()
    val tasks: Flow<List<TaskEntity>> = dao.activeTasks()
    val completedCount: Flow<Int> = dao.completedCount()
    val calmMode: Flow<Boolean> = app.settings.data.map { it[booleanPreferencesKey("calm_mode")] ?: false }
    val focusLength: Flow<Int> = app.settings.data.map { it[intPreferencesKey("focus_minutes")] ?: 25 }

    suspend fun add(title: String, requestedBucket: String? = null) {
        val clean = title.trim()
        if (clean.isBlank()) return
        val nowCount = dao.inBucket("NOW").size
        val todayCount = dao.inBucket("TODAY").size
        val desired = requestedBucket ?: when {
            nowCount == 0 -> "NOW"
            todayCount < 3 -> "TODAY"
            else -> "LATER"
        }
        val bucket = when (desired) {
            "NOW" -> if (nowCount > 0) Rules.overflowBucket("TODAY", todayCount) else "NOW"
            "TODAY" -> Rules.overflowBucket("TODAY", todayCount)
            else -> desired
        }
        dao.insert(TaskEntity(title = clean, bucket = bucket))
    }

    suspend fun move(task: TaskEntity, bucket: String) {
        val count = dao.inBucket(bucket).count { it.id != task.id }
        if ((bucket == "NOW" && count >= 1) || (bucket == "TODAY" && count >= 3)) return
        dao.update(task.copy(bucket = bucket))
    }
    suspend fun done(id: Long) = dao.complete(id, System.currentTimeMillis())
    suspend fun setCalm(enabled: Boolean) { app.settings.edit { it[booleanPreferencesKey("calm_mode")] = enabled } }
    suspend fun setFocusMinutes(minutes: Int) { app.settings.edit { it[intPreferencesKey("focus_minutes")] = minutes.coerceIn(5, 120) } }
}
