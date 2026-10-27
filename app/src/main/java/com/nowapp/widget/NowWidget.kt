package com.nowapp.widget

import android.content.Context
import android.graphics.Color as AndroidColor
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nowapp.MainActivity
import com.nowapp.ServiceLocator
import com.nowapp.domain.BucketRules
import com.nowapp.domain.Task
import com.nowapp.platform.QuickCaptureActivity
import com.nowapp.platform.WidgetActions

/**
 * The 4x2 home-screen widget: today's three things, each with a working
 * checkbox, plus a capture target.
 *
 * Three rules this file learned the hard way:
 *  - a Glance snapshot is a photograph, not a live view, so every action must
 *    re-render explicitly (see [WidgetActions]);
 *  - a control has to *look* like a control: the checkbox is a visible ring, and
 *    the whole row is tappable, because most people tap the text;
 *  - a failure must be visible somewhere. [WidgetDiagnostics] is that somewhere.
 */
class NowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val loaded = runCatching { ServiceLocator.tasks.allOnce() }
        loaded.onFailure {
            Log.e(TAG, "provideGlance could not read tasks", it)
            WidgetDiagnostics.recordError(it.javaClass.simpleName + ": " + (it.message ?: ""))
        }
        val tasks = loaded.getOrDefault(emptyList())
        Log.i(TAG, "provideGlance id=$id tasks=${tasks.size} ok=${loaded.isSuccess}")
        WidgetDiagnostics.recordRefresh()
        val failed = loaded.isFailure
        provideContent { WidgetContent(tasks = tasks, failed = failed) }
    }

    private companion object {
        const val TAG = "NowWidget"
    }
}

class NowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Log.i("NowWidget", "receiver enabled")
        WidgetUpdater.refresh(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: android.appwidget.AppWidgetManager, appWidgetIds: IntArray) {
        Log.i("NowWidget", "onUpdate ids=${appWidgetIds.size}")
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }
}

/** Refreshes every widget instance after a data change. */
object WidgetUpdater {
    fun refresh(context: Context) {
        val app = context.applicationContext
        ServiceLocator.scope.launch {
            runCatching { NowWidget().updateAll(app) }
                .onSuccess {
                    Log.i("NowWidget", "updateAll ok")
                    WidgetDiagnostics.recordRefresh()
                }
                .onFailure {
                    Log.e("NowWidget", "updateAll failed", it)
                    WidgetDiagnostics.recordError(it.javaClass.simpleName + ": " + (it.message ?: ""))
                }
        }
    }
}

private val background = ColorProvider(AndroidColor.parseColor("#F20B0B0F"))
private val surface = ColorProvider(AndroidColor.parseColor("#FF1A1A24"))
private val accent = ColorProvider(AndroidColor.parseColor("#FFB8A1FF"))
private val text = ColorProvider(AndroidColor.parseColor("#FFECECF3"))
private val muted = ColorProvider(AndroidColor.parseColor("#FF9A9AAE"))

private val titleStyle = TextStyle(color = text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
private val bodyStyle = TextStyle(color = text, fontSize = 14.sp)
private val metaStyle = TextStyle(color = muted, fontSize = 12.sp)

@Composable
private fun WidgetContent(tasks: List<Task>, failed: Boolean) {
    val today = BucketRules.todayTasks(tasks)
    val now = BucketRules.nowTask(tasks)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(background)
            .cornerRadius(20.dp)
            .padding(12.dp),
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (today.size == 1) "Today: 1 thing" else "Today: ${today.size} things",
                style = titleStyle,
                modifier = GlanceModifier.defaultWeight(),
            )
            Box(
                modifier = GlanceModifier
                    .size(40.dp)
                    .cornerRadius(20.dp)
                    .background(surface)
                    .semantics { contentDescription = "Add a task" }
                    .clickable(actionStartActivity<QuickCaptureActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(color = accent, fontSize = 20.sp, fontWeight = FontWeight.Medium))
            }
        }

        if (failed) {
            Text(
                text = "Could not read tasks. Open Now once, then refresh the widget.",
                style = TextStyle(color = muted, fontSize = 12.sp),
                modifier = GlanceModifier.padding(top = 10.dp),
            )
        }

        if (today.isEmpty() && !failed) {
            Text(
                text = if (now != null) "Now: ${now.title}" else "Nothing on today. Tap + to add one.",
                style = metaStyle,
                modifier = GlanceModifier.padding(top = 10.dp),
            )
        }

        today.take(3).forEach { task ->
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    // The whole row opens the app: tapping the text and getting
                    // nothing is the most common "the widget is broken" report.
                    .clickable(actionStartActivity<MainActivity>()),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A visible ring, 40dp, that reads as a checkbox before you touch it.
                Box(
                    modifier = GlanceModifier
                        .size(40.dp)
                        .cornerRadius(20.dp)
                        .background(surface)
                        .semantics { contentDescription = "Done: ${task.title}" }
                        .clickable(
                            actionRunCallback<WidgetActions.ToggleDoneAction>(
                                actionParametersOf(WidgetActions.TaskIdKey to task.id),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = GlanceModifier
                            .size(28.dp)
                            .cornerRadius(14.dp)
                            .background(background),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (task.isDone) "✓" else "",
                            style = TextStyle(color = accent, fontSize = 13.sp),
                        )
                    }
                }
                Box(modifier = GlanceModifier.width(10.dp)) {}
                Text(
                    text = task.title,
                    style = bodyStyle,
                    maxLines = 1,
                    modifier = GlanceModifier.defaultWeight(),
                )
            }
        }

        // A tiny, quiet footer. Later never gets a count badge on purpose.
        Box(modifier = GlanceModifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(if (today.size > 3) "and ${today.size - 3} more" else "Later stays quiet", style = metaStyle)
        }
    }
}
