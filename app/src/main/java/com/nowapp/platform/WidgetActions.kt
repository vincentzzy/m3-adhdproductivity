package com.nowapp.platform

import android.app.PendingIntent
import android.content.Context
import android.content.Intent

/**
 * The intents the widget fires, all built here so the widget code contains no
 * flags, no request codes and no guessing.
 *
 * Every toggle is an explicit broadcast to our own receiver, so it survives the
 * app process being dead — the system starts the process, the receiver writes the
 * change, and the widget is repainted from the result.
 */
object WidgetActions {

    const val ACTION_TOGGLE_DONE = "com.nowapp.action.WIDGET_TOGGLE_DONE"
    const val EXTRA_TASK_ID = "com.nowapp.extra.TASK_ID"

    private const val FLAGS = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun toggleTask(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, WidgetToggleReceiver::class.java)
            .setAction(ACTION_TOGGLE_DONE)
            .putExtra(EXTRA_TASK_ID, taskId)
        return PendingIntent.getBroadcast(context, taskId.toInt(), intent, FLAGS)
    }

    fun openActivity(
        context: Context,
        activity: Class<*>,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(context, activity)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(context, requestCode, intent, FLAGS)
    }
}
