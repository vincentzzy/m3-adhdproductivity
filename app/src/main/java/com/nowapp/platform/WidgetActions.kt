package com.nowapp.platform

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import com.nowapp.ServiceLocator

/** Actions the home-screen widget can run without opening the app. */
object WidgetActions {

    val TaskIdKey = ActionParameters.Key<Long>("task_id")

    /** Ticks a task off straight from the widget. */
    class ToggleDoneAction : ActionCallback {
        override suspend fun onAction(
            context: Context,
            glanceId: GlanceId,
            parameters: ActionParameters,
        ) {
            val taskId = parameters[TaskIdKey] ?: return
            val tasks = ServiceLocator.tasks.allOnce()
            val task = tasks.firstOrNull { it.id == taskId } ?: return
            if (task.isDone) {
                ServiceLocator.tasks.uncomplete(taskId)
            } else {
                ServiceLocator.tasks.complete(taskId)
            }
        }
    }
}
