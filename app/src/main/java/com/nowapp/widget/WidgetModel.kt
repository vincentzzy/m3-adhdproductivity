package com.nowapp.widget

import com.nowapp.domain.BucketRules
import com.nowapp.domain.Task

/**
 * Everything the widget needs to draw, decided by pure Kotlin so it can be tested
 * without a launcher, a device or an Android runtime.
 *
 * The widget itself is a dumb renderer: it takes the rows below and puts them in
 * three fixed view slots. That split is the reason a widget bug is now a unit-test
 * failure instead of a screenshot of "Couldn't add widget".
 */
data class WidgetRow(val taskId: Long, val title: String)

data class WidgetModel(
    val header: String,
    val rows: List<WidgetRow>,
    /** Shown when there is nothing on today; null means "no message". */
    val emptyMessage: String?,
    val footer: String,
) {
    companion object {
        const val MAX_ROWS = 3

        fun build(tasks: List<Task>): WidgetModel {
            val today = BucketRules.todayTasks(tasks)
            val now = BucketRules.nowTask(tasks)

            val rows = today.take(MAX_ROWS).map { WidgetRow(it.id, it.title) }

            val empty = when {
                rows.isNotEmpty() -> null
                now != null -> "Now: ${now.title}"
                else -> "Tap + to add one."
            }

            val footer = when {
                today.size > MAX_ROWS -> "and ${today.size - MAX_ROWS} more"
                else -> "Later stays quiet"
            }

            val header = when (today.size) {
                0 -> "Today is clear"
                1 -> "Today: 1 thing"
                else -> "Today: ${today.size} things"
            }

            return WidgetModel(header = header, rows = rows, emptyMessage = empty, footer = footer)
        }
    }
}
