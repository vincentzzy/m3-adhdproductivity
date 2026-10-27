package com.taskd.app.platform

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.taskd.app.MainActivity
import com.taskd.app.data.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

object Notifications {
    const val CHANNEL = "taskd_anchor"
    const val ID = 41

    fun update(context: Context, count: Int, top: String, taskId: Long?) {
        show(context, count, top.ifBlank { "Nothing needs your attention right now" }, taskId)
    }

    private fun show(context: Context, count: Int, content: String, taskId: Long?) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "TASKD task anchor", NotificationManager.IMPORTANCE_LOW).apply {
                description = "One quiet, ongoing view of today's tasks"
            }
        )
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        fun action(code: String, label: String, request: Int) = NotificationCompat.Action.Builder(
            0, label,
            PendingIntent.getBroadcast(context, request, Intent(context, TaskActionReceiver::class.java)
                .setAction(code).putExtra("taskId", taskId ?: -1L), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        ).build()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Today: $count things")
            .setContentText(content)
            .setContentIntent(open)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(action("DONE", "Done", 1))
            .addAction(action("SNOOZE", "Snooze 30m", 2))
            .addAction(action("FOCUS", "Start focus", 3))
            .build()
        manager.notify(ID, notification)
    }

    fun snooze(context: Context, count: Int, top: String, taskId: Long?) {
        show(context, count, "Quiet for 30 minutes · $top", taskId)
        val restore = PendingIntent.getBroadcast(context, 4, Intent(context, TaskActionReceiver::class.java).setAction("RESTORE"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30 * 60_000L, restore
        )
    }
}

class TaskActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("taskId", -1L)
        if (intent.action == "DONE" && id > 0) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val dao = TaskDatabase.get(context).taskDao()
                    dao.complete(id, System.currentTimeMillis())
                    val today = dao.activeTasks().first().filter { it.bucket == "NOW" || it.bucket == "TODAY" }
                    Notifications.update(context, today.size, today.firstOrNull { it.bucket == "NOW" }?.title.orEmpty(), today.firstOrNull { it.bucket == "NOW" }?.id)
                } finally { pending.finish() }
            }
        } else if (intent.action == "SNOOZE") {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val today = TaskDatabase.get(context).taskDao().activeTasks().first().filter { it.bucket == "NOW" || it.bucket == "TODAY" }
                    val now = today.firstOrNull { it.bucket == "NOW" }
                    Notifications.snooze(context, today.size, now?.title.orEmpty(), now?.id)
                } finally { pending.finish() }
            }
        } else if (intent.action == "RESTORE") {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val today = TaskDatabase.get(context).taskDao().activeTasks().first().filter { it.bucket == "NOW" || it.bucket == "TODAY" }
                    Notifications.update(context, today.size, today.firstOrNull { it.bucket == "NOW" }?.title.orEmpty(), today.firstOrNull { it.bucket == "NOW" }?.id)
                } finally { pending.finish() }
            }
        } else if (intent.action == "FOCUS") {
            context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("focus", true))
        }
    }
}
