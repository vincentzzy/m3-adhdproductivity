package com.nowapp.platform

import android.content.Context
import com.nowapp.ServiceLocator
import com.nowapp.domain.BucketRules
import com.nowapp.widget.WidgetUpdater
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Keeps the ongoing notification and the widget in sync with the database.
 *
 * One collection for the whole process: every task change and preference change
 * funnels into a single rebuild. That is what makes the notification trustworthy
 * — it is never stale, and the user never has to open the app to check it.
 */
object NotificationUpdater {

    private const val TIME_FORMAT = "HH:mm"
    private var started = false

    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        ServiceLocator.scope.launch {
            combine(ServiceLocator.tasks.tasks, ServiceLocator.prefs.prefs) { tasks, prefs ->
                tasks to prefs
            }.collect { (tasks, prefs) ->
                val todayTasks = BucketRules.todayTasks(tasks)
                val nowTask = BucketRules.nowTask(tasks)
                val top = nowTask ?: todayTasks.firstOrNull()

                val snoozeActive = top != null &&
                    prefs.snoozeTaskId == top.id &&
                    prefs.snoozeUntil > System.currentTimeMillis()
                val snoozeText = if (snoozeActive) {
                    Instant.ofEpochMilli(prefs.snoozeUntil)
                        .atZone(ZoneId.systemDefault())
                        .toLocalTime()
                        .format(DateTimeFormatter.ofPattern(TIME_FORMAT))
                } else {
                    null
                }

                Notifications.showOngoing(
                    context = app,
                    todayCount = todayTasks.size,
                    topTaskId = top?.id,
                    topTaskTitle = top?.title,
                    snoozedUntilText = snoozeText,
                )

                WidgetUpdater.refresh(app)
                PingScheduler.scheduleNext(app)
            }
        }
    }
}
