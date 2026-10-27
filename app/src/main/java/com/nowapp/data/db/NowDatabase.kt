package com.nowapp.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        TaskEntity::class,
        FocusSessionEntity::class,
        StreakEntity::class,
        AnchorEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NowDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun focusDao(): FocusDao
    abstract fun streakDao(): StreakDao
    abstract fun anchorDao(): AnchorDao

    companion object {
        @Volatile
        private var instance: NowDatabase? = null

        fun get(context: Context): NowDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NowDatabase::class.java,
                "now.db",
            ).build().also { instance = it }
        }
    }
}
