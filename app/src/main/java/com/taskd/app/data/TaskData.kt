package com.taskd.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    val bucket: String = "LATER",
    val estimateMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val dueAt: Long? = null,
    val isHardDeadline: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val anchorId: Long? = null,
    val parentTaskId: Long? = null
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: Long? = null,
    val plannedMinutes: Int,
    val actualMinutes: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long? = null
)

@Entity(tableName = "anchors")
data class AnchorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    /** Comma-separated ISO weekday numbers; kept simple for a single-user local schedule. */
    val daysOfWeek: String,
    val timeOfDay: String
)

@Entity(tableName = "streak_state")
data class StreakStateEntity(
    @PrimaryKey val id: Int = 1,
    val current: Int = 0,
    val best: Int = 0,
    val graceDaysRemaining: Int = 2,
    val lastCompletedDate: String? = null
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE completedAt IS NULL ORDER BY createdAt DESC")
    fun activeTasks(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE completedAt IS NULL AND bucket = :bucket ORDER BY createdAt DESC")
    suspend fun inBucket(bucket: String): List<TaskEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insert(task: TaskEntity): Long
    @Update suspend fun update(task: TaskEntity)
    @Query("UPDATE tasks SET completedAt = :whenDone WHERE id = :id") suspend fun complete(id: Long, whenDone: Long)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM tasks") suspend fun exportAll(): List<TaskEntity>
    @Query("SELECT COUNT(*) FROM tasks WHERE completedAt IS NOT NULL") fun completedCount(): Flow<Int>
}

@Dao
interface FocusDao {
    @Insert suspend fun insert(session: FocusSessionEntity): Long
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC") fun sessions(): Flow<List<FocusSessionEntity>>
}

@Database(entities = [TaskEntity::class, FocusSessionEntity::class, AnchorEntity::class, StreakStateEntity::class], version = 1, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusDao(): FocusDao
    companion object {
        @Volatile private var INSTANCE: TaskDatabase? = null
        fun get(context: Context): TaskDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context.applicationContext, TaskDatabase::class.java, "taskd.db")
                .fallbackToDestructiveMigration().build().also { INSTANCE = it }
        }
    }
}
