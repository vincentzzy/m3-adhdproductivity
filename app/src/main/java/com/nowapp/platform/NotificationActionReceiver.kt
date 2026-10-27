package com.nowapp.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nowapp.ServiceLocator
import com.nowapp.domain.BucketRules
import kotlinx.coroutines.launch

/**
 * Handles the notification's Done / Snooze actions without opening the app.
 * This is the whole point of the ongoing notification: acting on it must cost
 * one tap and zero context switches.
 */
class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val taskId = intent.getLongExtra("task_id", -1L)
        ServiceLocator.scope.launch {
            try {
                when (intent.action) {
                    Notifications.ACTION_DONE -> {
                        val target = resolveTaskId(taskId)
                        if (target != null) ServiceLocator.tasks.complete(target)
                        Notifications.clearPing(context)
                    }

                    Notifications.ACTION_SNOOZE -> {
                        val target = resolveTaskId(taskId)
                        if (target != null) {
                            ServiceLocator.prefs.snooze(target, System.currentTimeMillis() + 30 * 60 * 1000L)
                        }
                        Notifications.clearPing(context)
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun resolveTaskId(preferred: Long): Long? {
        if (preferred > 0) return preferred
        val tasks = ServiceLocator.tasks.allOnce()
        return (BucketRules.nowTask(tasks) ?: BucketRules.todayTasks(tasks).firstOrNull())?.id
    }
}
