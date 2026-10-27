package com.nowapp.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.nowapp.MainActivity
import com.nowapp.R
import com.nowapp.ServiceLocator
import com.nowapp.domain.Task
import com.nowapp.platform.QuickCaptureActivity
import com.nowapp.platform.WidgetActions
import kotlinx.coroutines.launch

/**
 * The 4x2 home-screen widget: today's three things, each with a working checkbox.
 *
 * Built with plain [RemoteViews]. A launcher inflates this layout directly, so the
 * only way it can fail to appear is a missing resource — there is no composition,
 * no snapshot and no framework hand-off in between. "Couldn't add widget" is
 * exactly the failure a declarative widget framework can produce and a plain
 * RemoteViews layout cannot.
 */
class NowWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        Log.i(TAG, "onUpdate ids=${appWidgetIds.size}")
        WidgetUpdater.refresh(context)
    }

    override fun onEnabled(context: Context) {
        Log.i(TAG, "onEnabled")
        WidgetUpdater.refresh(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        // Resizing is a common reason a widget "loses" its content.
        Log.i(TAG, "options changed for $appWidgetId")
        WidgetUpdater.refresh(context)
    }

    private companion object {
        const val TAG = "NowWidget"
    }
}

/** Pushes the current database state to every placed widget instance. */
object WidgetUpdater {

    /** Fire-and-forget; safe to call from the main thread or a broadcast. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        ServiceLocator.scope.launch { paint(app) }
    }

    /** Suspending repaint, used where the caller must finish the work first. */
    suspend fun paint(context: Context) {
        val app = context.applicationContext
        val manager = AppWidgetManager.getInstance(app)
        val ids = runCatching {
            manager.getAppWidgetIds(ComponentName(app, NowWidgetProvider::class.java))
        }.getOrDefault(IntArray(0))

        if (ids.isEmpty()) {
            Log.i("NowWidget", "refresh skipped: no widget instances")
            return
        }

        val tasks = runCatching { ServiceLocator.tasks.allOnce() }
            .onFailure {
                Log.e("NowWidget", "could not read tasks", it)
                WidgetDiagnostics.recordError(app, it)
            }
            .getOrDefault(emptyList())

        var painted = 0
        ids.forEach { id ->
            runCatching { manager.updateAppWidget(id, render(app, tasks)) }
                .onSuccess { painted += 1 }
                .onFailure {
                    Log.e("NowWidget", "refresh failed for $id", it)
                    WidgetDiagnostics.recordError(app, it)
                }
        }
        if (painted > 0) {
            Log.i("NowWidget", "refreshed $painted widget(s)")
            WidgetDiagnostics.recordRefresh(app)
        }
    }
}

/**
 * Renders one widget snapshot from data already in hand. Pure view work: no I/O,
 * no coroutines, nothing that can block a broadcast.
 */
internal fun render(context: Context, tasks: List<Task>): RemoteViews {
    val views = RemoteViews(context.packageName, R.layout.widget_today)
    val model = WidgetModel.build(tasks)

    views.setTextViewText(R.id.widget_header, model.header)
    views.setTextViewText(R.id.widget_footer, model.footer)
    views.setOnClickPendingIntent(
        R.id.widget_add,
        WidgetActions.openActivity(context, QuickCaptureActivity::class.java, requestCode = 9001),
    )

    val rowIds = intArrayOf(R.id.widget_row1, R.id.widget_row2, R.id.widget_row3)
    val checkIds = intArrayOf(R.id.widget_check1, R.id.widget_check2, R.id.widget_check3)
    val titleIds = intArrayOf(R.id.widget_title1, R.id.widget_title2, R.id.widget_title3)

    rowIds.forEachIndexed { index, rowId ->
        val row = model.rows.getOrNull(index)
        if (row == null) {
            views.setViewVisibility(rowId, View.GONE)
        } else {
            views.setViewVisibility(rowId, View.VISIBLE)
            views.setTextViewText(titleIds[index], row.title)
            views.setTextViewText(checkIds[index], "")
            views.setOnClickPendingIntent(
                checkIds[index],
                WidgetActions.toggleTask(context, row.taskId),
            )
            // Tapping the text opens the app: a dead tap reads as a broken widget.
            views.setOnClickPendingIntent(
                titleIds[index],
                WidgetActions.openActivity(context, MainActivity::class.java, requestCode = 9100 + index),
            )
        }
    }

    val empty = model.emptyMessage
    if (empty == null) {
        views.setViewVisibility(R.id.widget_empty, View.GONE)
    } else {
        views.setViewVisibility(R.id.widget_empty, View.VISIBLE)
        views.setTextViewText(R.id.widget_empty, empty)
    }

    return views
}
