package com.taskd.app.widget

import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.Button
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.defaultWeight
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.unit.ColorProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.taskd.app.MainActivity
import com.taskd.app.platform.QuickCaptureActivity

class TaskdGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: GlanceId) {
        provideContent {
            Column(GlanceModifier.fillMaxSize().background(Color(0xFF20212B)).padding(14.dp), verticalAlignment = Alignment.Vertical.CenterVertically) {
                Text("TASKD · TODAY", style = androidx.glance.text.TextStyle(color = ColorProvider(Color(0xFFB9A7FF))))
                Text("Your next thing is in the app", style = androidx.glance.text.TextStyle(color = ColorProvider(Color.White)))
                Spacer(GlanceModifier.defaultWeight())
                Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                    Button("Open Now", onClick = actionStartActivity<MainActivity>())
                    Spacer(GlanceModifier.width(8.dp))
                    Button("Capture", onClick = actionStartActivity<QuickCaptureActivity>())
                }
            }
        }
    }
}

class TaskdWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskdGlanceWidget()
}
