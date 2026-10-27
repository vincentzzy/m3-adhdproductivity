package com.nowapp.widget

import android.content.Context
import android.graphics.Color as AndroidColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.cornerRadius
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.nowapp.ServiceLocator
import com.nowapp.domain.BucketRules
import com.nowapp.domain.Task
import com.nowapp.platform.QuickCaptureActivity
import com.nowapp.platform.WidgetActions
import kotlinx.coroutines.launch

/**
 * The 4x2 home-screen widget: today's three things, each with a working
 * checkbox, plus a capture target. This is bucket #8 of the abandonment list —
 * the app has to be where the user already looks.
 */
class NowWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val tasks = runCatching { ServiceLocator.tasks.allOnce() }.getOrElse { emptyList() }
        provideContent { WidgetContent(tasks) }
    }
}

class NowWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowWidget()
}

/** Refreshes every widget instance after a data change. */
object WidgetUpdater {
    fun refresh(context: Context) {
        val app = context.applicationContext
        ServiceLocator.scope.launch {
            runCatching { NowWidget().updateAll(app) }
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
private fun WidgetContent(tasks: List<Task>) {
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
            // Capture target: one tap, straight to the input field.
            Box(
                modifier = GlanceModifier
                    .size(36.dp)
                    .cornerRadius(18.dp)
                    .background(surface)
                    .clickable(actionStartActivity<QuickCaptureActivity>()),
                contentAlignment = Alignment.Center,
            ) {
                Text("+", style = TextStyle(color = accent, fontSize = 20.sp, fontWeight = FontWeight.Medium))
            }
        }

        if (today.isEmpty()) {
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
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Working checkbox: tapping it completes the task without opening the app.
                Box(
                    modifier = GlanceModifier
                        .size(32.dp)
                        .cornerRadius(16.dp)
                        .background(surface)
                        .clickable(
                            actionRunCallback<WidgetActions.ToggleDoneAction>(
                                actionParametersOf(WidgetActions.TaskIdKey to task.id),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (task.isDone) "x" else "", style = TextStyle(color = accent, fontSize = 14.sp))
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
            Text("Later stays quiet", style = metaStyle)
        }
    }
}
