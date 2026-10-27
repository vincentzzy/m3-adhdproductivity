package com.nowapp.platform

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.nowapp.ServiceLocator
import com.nowapp.domain.BucketRules
import com.nowapp.domain.TimeMath
import com.nowapp.widget.WidgetUpdater
import java.util.concurrent.TimeUnit

/**
 * Housekeeping that must survive the app not being opened:
 * rolls the streak forward (spending grace days if needed), re-posts the
 * ongoing notification and refreshes the widget.
 */
class DailyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        return runCatching {
            val today = TimeMath.today()
            ServiceLocator.streak.rollForward(today)
            Notifications.ensureChannels(context)

            val tasks = ServiceLocator.tasks.allOnce()
            val todayTasks = BucketRules.todayTasks(tasks)
            val top = BucketRules.nowTask(tasks) ?: todayTasks.firstOrNull()
            Notifications.showOngoing(
                context = context,
                todayCount = todayTasks.size,
                topTaskId = top?.id,
                topTaskTitle = top?.title,
            )
            PingScheduler.scheduleNext(context)
            WidgetUpdater.refresh(context)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val NAME = "now-daily"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<DailyWorker>(6, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
