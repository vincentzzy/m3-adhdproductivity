package com.nowapp.platform

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nowapp.MainActivity
import com.nowapp.R

/**
 * The anchor: exactly one persistent, ongoing notification that states today's
 * count and the single top task, with Done / Snooze 30m / Start focus.
 *
 * It is rebuilt on every state change (see [NotificationUpdater]) and restored
 * after reboot, so the app is never out of sight for long.
 */
object Notifications {

    const val CHANNEL_ANCHOR = "anchor"
    const val CHANNEL_PINGS = "pings"
    const val ONGOING_ID = 1001
    const val PING_ID = 2001

    const val ACTION_DONE = "com.nowapp.action.DONE"
    const val ACTION_SNOOZE = "com.nowapp.action.SNOOZE"
    const val ACTION_START_FOCUS = "com.nowapp.action.START_FOCUS"

    const val EXTRA_FOCUS_TASK_ID = "focus_task_id"
    const val EXTRA_OPEN_FOCUS = "open_focus"

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Low importance + silent: the ongoing notification is a reference, not an alert.
        val anchor = NotificationChannel(
            CHANNEL_ANCHOR,
            "Today's anchor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "One quiet, always-present line about today."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }

        val pings = NotificationChannel(
            CHANNEL_PINGS,
            "Anchor pings",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "At most three timed reminders a day, each naming one task."
            setShowBadge(true)
            enableVibration(true)
        }

        manager.createNotificationChannel(anchor)
        manager.createNotificationChannel(pings)
    }

    /** Rebuilds the single ongoing notification from current state. */
    fun showOngoing(
        context: Context,
        todayCount: Int,
        topTaskId: Long?,
        topTaskTitle: String?,
        snoozedUntilText: String? = null,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            pendingFlags(),
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ANCHOR)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(
                if (todayCount == 1) "Today: 1 thing" else "Today: $todayCount things"
            )
            .setContentText(
                when {
                    snoozedUntilText != null -> "Snoozed until $snoozedUntilText"
                    topTaskTitle != null -> topTaskTitle
                    else -> "Nothing on today. One tap to add."
                }
            )
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    when {
                        snoozedUntilText != null -> "Snoozed until $snoozedUntilText. It will come back on its own."
                        topTaskTitle != null -> "Next: $topTaskTitle"
                        else -> "Nothing on today. Open Now and add one thing."
                    }
                )
            )
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setColor(0xFFB8A1FF.toInt())

        if (topTaskId != null) {
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_tile,
                    "Done",
                    broadcast(context, ACTION_DONE, topTaskId, 10),
                )
            )
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_tile,
                    "Snooze 30m",
                    broadcast(context, ACTION_SNOOZE, topTaskId, 11),
                )
            )
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_tile,
                    "Start focus",
                    PendingIntent.getActivity(
                        context,
                        12,
                        Intent(context, MainActivity::class.java)
                            .putExtra(EXTRA_OPEN_FOCUS, true)
                            .putExtra(EXTRA_FOCUS_TASK_ID, topTaskId)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                        pendingFlags(),
                    ),
                )
            )
        }

        // FLAG_NO_CLEAR is the strongest "do not swipe me away" signal an app can
        // set for a non-foreground notification. Android 14+ still lets the user
        // dismiss it manually; NotificationUpdater re-posts within 15 minutes.
        builder.setFlag(android.app.Notification.FLAG_NO_CLEAR, true)

        runCatching { manager.notify(ONGOING_ID, builder.build()) }
    }

    /** A single ping that names exactly one task, never a list. */
    fun showPing(context: Context, taskTitle: String, taskId: Long?, isEscalation: Boolean) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return

        val open = PendingIntent.getActivity(
            context,
            20,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            pendingFlags(),
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_PINGS)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(if (isEscalation) "This one has a deadline" else "One thing for now")
            .setContentText(taskTitle)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setColor(0xFFB8A1FF.toInt())

        if (taskId != null) {
            builder.addAction(
                NotificationCompat.Action(R.drawable.ic_tile, "Done", broadcast(context, ACTION_DONE, taskId, 21))
            )
            builder.addAction(
                NotificationCompat.Action(R.drawable.ic_tile, "Snooze 30m", broadcast(context, ACTION_SNOOZE, taskId, 22))
            )
        }
        runCatching { manager.notify(PING_ID, builder.build()) }
    }

    fun clearPing(context: Context) {
        NotificationManagerCompat.from(context).cancel(PING_ID)
    }

    private fun broadcast(context: Context, action: String, taskId: Long, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, NotificationActionReceiver::class.java)
                .setAction(action)
                .putExtra("task_id", taskId),
            pendingFlags(),
        )

    private fun pendingFlags(): Int =
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
}
