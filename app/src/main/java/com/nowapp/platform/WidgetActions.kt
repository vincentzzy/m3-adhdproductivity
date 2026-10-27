package com.nowapp.platform

import android.content.Context
import android.util.Log
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.nowapp.ServiceLocator
import com.nowapp.widget.NowWidget
import com.nowapp.widget.WidgetDiagnostics

/**
 * Actions the home-screen widget can run without opening the app.
 *
 * Every callback ends by asking the widget to re-render itself. Glance does not
 * do this implicitly: without the explicit `update`, the database changes and the
 * home screen keeps showing the old picture until something else happens to
 * trigger a refresh. That is indistinguishable from "the widget is broken".
 */
object WidgetActions {

    private const val TAG = "NowWidget"

    val TaskIdKey = ActionParameters.Key<Long>("task_id")

    /** Ticks a task off straight from the widget. */
    class ToggleDoneAction : ActionCallback {
        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            val taskId = parameters[TaskIdKey]
            Log.i(TAG, "ToggleDoneAction taskId=$taskId glanceId=$glanceId")
            if (taskId == null) {
                WidgetDiagnostics.recordError("Tap arrived without a task id")
                return
            }
            runCatching {
                val task = ServiceLocator.tasks.allOnce().firstOrNull { it.id == taskId }
                if (task == null) {
                    WidgetDiagnostics.recordError("Task $taskId no longer exists")
                    return
                }
                if (task.isDone) {
                    ServiceLocator.tasks.uncomplete(taskId)
                } else {
                    ServiceLocator.tasks.complete(taskId)
                }
            }.onFailure {
                Log.e(TAG, "ToggleDoneAction failed", it)
                WidgetDiagnostics.recordError(it.javaClass.simpleName + ": " + (it.message ?: ""))
            }

            // Re-render immediately, including the case where the row has to disappear.
            runCatching { NowWidget().update(context.applicationContext, glanceId) }
                .onSuccess { WidgetDiagnostics.recordRefresh() }
                .onFailure {
                    Log.e(TAG, "update after action failed", it)
                    WidgetDiagnostics.recordError(it.javaClass.simpleName + ": " + (it.message ?: ""))
                }
        }
    }
}
