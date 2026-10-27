package com.nowapp.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nowapp.ServiceLocator
import com.nowapp.widget.WidgetDiagnostics
import com.nowapp.widget.WidgetUpdater
import kotlinx.coroutines.launch

/**
 * Completes (or un-completes) a task without opening the app, then repaints.
 *
 * The repaint is the part that used to be missing: a widget is a picture, so the
 * picture has to be redrawn explicitly when the data behind it changes.
 */
class WidgetToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != WidgetActions.ACTION_TOGGLE_DONE) return

        val taskId = intent.getLongExtra(WidgetActions.EXTRA_TASK_ID, -1L)
        Log.i("NowWidget", "toggle received taskId=$taskId")

        if (taskId <= 0L) {
            WidgetDiagnostics.recordError(context, "tap arrived without a task id")
            return
        }

        val pending = goAsync()
        val app = context.applicationContext

        ServiceLocator.scope.launch {
            try {
                val task = ServiceLocator.tasks.allOnce().firstOrNull { it.id == taskId }
                if (task == null) {
                    WidgetDiagnostics.recordError(context, "task $taskId no longer exists")
                } else if (task.isDone) {
                    ServiceLocator.tasks.uncomplete(taskId)
                } else {
                    ServiceLocator.tasks.complete(taskId)
                }
            } catch (throwable: Throwable) {
                Log.e("NowWidget", "toggle failed", throwable)
                WidgetDiagnostics.recordError(context, throwable)
            } finally {
                WidgetUpdater.paint(app)
                pending.finish()
            }
        }
    }
}
