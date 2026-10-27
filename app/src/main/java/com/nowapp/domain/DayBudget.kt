package com.nowapp.domain

import java.time.Duration
import java.time.LocalTime

/** Result of the day-budget calculation shown on the Time screen. */
data class DayBudget(
    val plannedMinutes: Int,
    val awakeMinutes: Int,
    /** 0f..1f — clamped, because the progress bar must never look broken. */
    val usedFraction: Float,
    val remainingMinutes: Int,
    val isOverBudget: Boolean,
)

/**
 * Day-budget maths. Deliberately simple and pure so it can be unit tested:
 * the point is to make "how much day is actually left" visible, which is the
 * exact thing time-blindness hides.
 */
object DayBudgetMath {

    /** Default awake window: 07:00 to 23:00. Users can change the wake/finish times. */
    const val DEFAULT_AWAKE_MINUTES = 16 * 60

    fun compute(estimatesMinutes: List<Int>, awakeMinutes: Int = DEFAULT_AWAKE_MINUTES): DayBudget {
        val planned = estimatesMinutes.filter { it > 0 }.sum()
        val budget = awakeMinutes.coerceIn(60, 24 * 60)
        val fraction = if (budget == 0) 0f else (planned.toDouble() / budget).coerceIn(0.0, 1.0).toFloat()
        return DayBudget(
            plannedMinutes = planned,
            awakeMinutes = budget,
            usedFraction = fraction,
            remainingMinutes = budget - planned,
            isOverBudget = planned > budget,
        )
    }

    /** Fraction of the awake window that has already elapsed, for the "day left" bar. */
    fun elapsedFraction(now: LocalTime, dayStart: LocalTime, dayEnd: LocalTime): Float {
        val total = minutesBetween(dayStart, dayEnd)
        if (total <= 0) return 0f
        // Wrap-around arithmetic gives a large positive number for times before the
        // window starts, so that case is handled explicitly.
        val elapsed = if (now.isBefore(dayStart)) 0 else minutesBetween(dayStart, now).coerceIn(0, total)
        return elapsed.toFloat() / total.toFloat()
    }

    /** Minutes between two wall-clock times, wrapping past midnight (23:30 -> 00:30 = 60). */
    fun minutesBetween(from: LocalTime, to: LocalTime): Int {
        var minutes = Duration.between(from, to).toMinutes().toInt()
        if (minutes < 0) minutes += 24 * 60
        return minutes
    }

    /** "2h 15m" / "45m" — no unit soup, no decimals. */
    fun formatDuration(minutes: Int): String {
        val safe = kotlin.math.abs(minutes)
        val h = safe / 60
        val m = safe % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }
}
