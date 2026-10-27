package com.nowapp.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A tiny, in-memory record of what the widget has actually been asked to do.
 *
 * The widget is the one part of this app that can fail silently: it runs in a
 * launcher-driven broadcast, it has no visible error surface, and a stack trace
 * goes to logcat where nobody will look. Settings shows this instead, so
 * "the widget doesn't work" becomes a sentence with a number in it.
 */
object WidgetDiagnostics {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _status = MutableStateFlow("Not checked yet")
    val status: StateFlow<String> = _status

    @Volatile private var lastRefreshAt: Long? = null
    @Volatile private var lastError: String? = null
    @Volatile private var refreshCount: Int = 0

    fun recordRefresh() {
        lastRefreshAt = System.currentTimeMillis()
        refreshCount += 1
        rebuild()
    }

    fun recordError(message: String) {
        lastError = message.take(160)
        rebuild()
    }

    private fun rebuild() {
        val at = lastRefreshAt?.let { timeFormat.format(Date(it)) }
        _status.value = buildString {
            if (at == null) append("No refresh requested since launch") else append("Last refresh $at")
            append(" · ").append(refreshCount).append(if (refreshCount == 1) " push" else " pushes")
            lastError?.let { append("\nLast error: ").append(it) }
        }
    }

    /** Counts the widget instances the launcher currently holds. */
    suspend fun probe(context: Context): Int {
        val manager = GlanceAppWidgetManager(context.applicationContext)
        return runCatching { manager.getGlanceIds(NowWidget::class.java).size }
            .onFailure { recordError(it.javaClass.simpleName + ": " + (it.message ?: "no message")) }
            .getOrDefault(0)
            .also { count ->
                _status.value = buildString {
                    append(if (count == 0) "No widget on the home screen" else "$count widget on screen")
                    append(" · ")
                    append(_status.value.substringBefore("\nLast error"))
                }
            }
    }
}
