package com.nowapp.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks ORDER BY createdAt ASC")
    suspend fun allOnce(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun byId(id: Long): TaskEntity?

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Insert
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM tasks")
    suspend fun clear()
}

@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions ORDER BY startedAt DESC")
    suspend fun allOnce(): List<FocusSessionEntity>

    @Insert
    suspend fun insert(session: FocusSessionEntity): Long

    @Insert
    suspend fun insertAll(sessions: List<FocusSessionEntity>)

    @Query("DELETE FROM focus_sessions")
    suspend fun clear()
}

@Dao
interface StreakDao {
    @Query("SELECT * FROM streak WHERE id = 1")
    fun observe(): Flow<StreakEntity?>

    @Query("SELECT * FROM streak WHERE id = 1")
    suspend fun get(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(state: StreakEntity)

    @Query("DELETE FROM streak")
    suspend fun clear()
}

@Dao
interface AnchorDao {
    @Query("SELECT * FROM anchors ORDER BY timeOfDay ASC")
    fun observeAll(): Flow<List<AnchorEntity>>

    @Query("SELECT * FROM anchors ORDER BY timeOfDay ASC")
    suspend fun allOnce(): List<AnchorEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(anchor: AnchorEntity): Long

    @Query("DELETE FROM anchors WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM anchors")
    suspend fun clear()
}
