package com.nowapp

import android.content.Context
import com.nowapp.data.AnchorRepository
import com.nowapp.data.FocusRepository
import com.nowapp.data.PrefsRepository
import com.nowapp.data.StreakRepository
import com.nowapp.data.TaskRepository
import com.nowapp.data.db.NowDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container. One app, one user: a DI framework would be
 * maintenance burden with no payoff (see DECISIONS.md).
 */
object ServiceLocator {

    private lateinit var appContext: Context

    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val context: Context get() = appContext

    val database: NowDatabase by lazy { NowDatabase.get(appContext) }
    val prefs: PrefsRepository by lazy { PrefsRepository(appContext) }
    val streak: StreakRepository by lazy { StreakRepository(database.streakDao()) }
    val tasks: TaskRepository by lazy { TaskRepository(database.taskDao(), database.focusDao(), streak) }
    val focus: FocusRepository by lazy { FocusRepository(database.focusDao()) }
    val anchors: AnchorRepository by lazy { AnchorRepository(database.anchorDao()) }
}
