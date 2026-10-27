package com.nowapp.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * The widget's flight recorder.
 *
 * A widget can fail in a process of its own, with no visible surface and no user
 * standing in front of it. So the last refresh, the last error and the number of
 * widget instances are written to a small file in the app's own storage — settings
 * reads that file, which means a crash in the widget's process is still readable
 * from the app afterwards.
 */
object WidgetDiagnostics {

    private const val FILE_NAME = "widget_diagnostics.txt"
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private val _status = MutableStateFlow("Not checked yet")
    val status: StateFlow<String> = _status

    fun recordRefresh(context: Context) {
        write(context, refreshLine(System.currentTimeMillis()))
    }

    fun recordError(context: Context, throwable: Throwable) {
        val detail = throwable.javaClass.simpleName + ": " + (throwable.message ?: "no message")
        recordError(context, detail)
    }

    fun recordError(context: Context, message: String) {
        write(context, errorLine(System.currentTimeMillis(), message))
    }

    /** Reads the recorder, counts the placed widgets, and republishes the summary. */
    suspend fun probe(context: Context): Int {
        val app = context.applicationContext
        val count = runCatching {
            AppWidgetManager.getInstance(app)
                .getAppWidgetIds(ComponentName(app, NowWidgetProvider::class.java))
                .size
        }.getOrDefault(0)
        _status.value = summaryFor(app, count)
        return count
    }

    /** Called by the settings sheet on open, without needing a suspend context. */
    fun refreshStatus(context: Context) {
        val app = context.applicationContext
        val count = runCatching {
            AppWidgetManager.getInstance(app)
                .getAppWidgetIds(ComponentName(app, NowWidgetProvider::class.java))
                .size
        }.getOrDefault(0)
        _status.value = summaryFor(app, count)
    }

    private fun summaryFor(context: Context, count: Int): String {
        val lines = read(context).lines()
        val refresh = lines.firstOrNull { it.startsWith("refresh=") }?.removePrefix("refresh=")
        val error = lines.firstOrNull { it.startsWith("error=") }?.removePrefix("error=")

        return buildString {
            append(if (count == 0) "No widget on the home screen" else "$count widget on screen")
            append(" · ")
            append(if (refresh.isNullOrBlank()) "no refresh recorded yet" else "last refresh $refresh")
            if (!error.isNullOrBlank()) {
                append("\nLast error: ")
                append(error.take(200))
            }
        }
    }

    private fun refreshLine(now: Long): String = "refresh=${timeFormat.format(Date(now))}"

    private fun errorLine(now: Long, message: String): String =
        "error=${timeFormat.format(Date(now))} ${message.replace('\n', ' ').take(200)}"

    /**
     * Keeps a two-line record: the newest refresh, and the newest error until a
     * successful refresh clears it.
     */
    private fun write(context: Context, newLine: String) {
        runCatching {
            val file = File(context.applicationContext.filesDir, FILE_NAME)
            val existing = if (file.exists()) file.readLines() else emptyList()
            val refresh = if (newLine.startsWith("refresh=")) {
                newLine
            } else {
                existing.firstOrNull { it.startsWith("refresh=") } ?: ""
            }
            val error = if (newLine.startsWith("error=")) {
                newLine
            } else {
                // A successful refresh clears the previous error.
                ""
            }
            file.writeText(listOf(refresh, error).filter { it.isNotBlank() }.joinToString("\n"))
            _status.value = summaryFor(context, currentCount(context))
        }
    }

    private fun currentCount(context: Context): Int = runCatching {
        AppWidgetManager.getInstance(context.applicationContext)
            .getAppWidgetIds(ComponentName(context.applicationContext, NowWidgetProvider::class.java))
            .size
    }.getOrDefault(0)

    private fun read(context: Context): String = runCatching {
        File(context.applicationContext.filesDir, FILE_NAME).takeIf { it.exists() }?.readText() ?: ""
    }.getOrDefault("")
}
