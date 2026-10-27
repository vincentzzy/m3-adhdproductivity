package com.nowapp.domain

import java.time.LocalDate

/**
 * Streak with two automatic grace days per month.
 *
 * The point is that a broken streak is never the reason to quit:
 *  - missing a day silently consumes grace and reports "protected";
 *  - grace refills to 2 at the start of each month;
 *  - `best` is never reduced, ever, so the user always keeps a record to look at.
 */
data class StreakState(
    val current: Int = 0,
    val best: Int = 0,
    val graceDaysRemaining: Int = GRACE_DAYS_PER_MONTH,
    val lastCompletedDate: LocalDate? = null,
    /** Which month the grace counter belongs to, as "2026-10". */
    val graceMonthKey: String? = null,
    /** The last date this function has already evaluated, so days are not double-counted. */
    val lastEvaluatedDate: LocalDate? = null,
    /** True when the most recent evaluation had to spend a grace day. */
    val lastDayProtected: Boolean = false,
) {
    companion object {
        const val GRACE_DAYS_PER_MONTH = 2
    }
}

object StreakMath {

    fun monthKey(date: LocalDate): String = "%04d-%02d".format(date.year, date.monthValue)

    /**
     * Advances the streak to [today].
     *
     * [qualifiedToday] is true when at least one task was completed today — one of
     * three counts, which is deliberate.
     */
    fun advance(state: StreakState, today: LocalDate, qualifiedToday: Boolean): StreakState {
        var current = state.current
        var best = state.best
        var grace = state.graceDaysRemaining
        var graceMonth = state.graceMonthKey
        var lastCompleted = state.lastCompletedDate
        var protected = false

        // Grace is per calendar month, so it is refilled whenever the walk crosses
        // into a new month rather than only once at the start.
        fun refillFor(date: LocalDate) {
            val key = monthKey(date)
            if (graceMonth != key) {
                grace = StreakState.GRACE_DAYS_PER_MONTH
                graceMonth = key
            }
        }

        refillFor(state.lastEvaluatedDate ?: today)

        // Walk every day that has not been evaluated yet, up to yesterday.
        var day = (state.lastEvaluatedDate ?: today.minusDays(1)).plusDays(1)
        while (day.isBefore(today)) {
            refillFor(day)
            if (isMissing(lastCompleted, current, day)) {
                if (grace > 0) {
                    grace -= 1
                    protected = true
                } else {
                    current = 0
                }
            }
            day = day.plusDays(1)
        }

        refillFor(today)

        // Idempotent: calling advance() repeatedly on the same day must not inflate
        // the streak. The UI can therefore recompute freely.
        if (qualifiedToday && state.lastCompletedDate != today) {
            current += 1
            lastCompleted = today
            if (current > best) best = current
        }

        return state.copy(
            current = current,
            best = maxOf(best, state.best),
            graceDaysRemaining = grace,
            lastCompletedDate = lastCompleted,
            graceMonthKey = graceMonth,
            lastEvaluatedDate = today,
            lastDayProtected = protected,
        )
    }

    /**
     * A day is missing when the streak is alive but nothing was completed on it.
     * Days before the streak started are not "missed"; they are simply not counted.
     */
    private fun isMissing(lastCompleted: LocalDate?, current: Int, day: LocalDate): Boolean {
        if (current <= 0) return false
        if (lastCompleted == null) return false
        return day.isAfter(lastCompleted)
    }

    /** Human status used by the UI. Never phrased as failure. */
    fun statusLabel(state: StreakState): String = when {
        state.current == 0 -> "Starting fresh — best ${state.best}"
        state.lastDayProtected -> "Protected · ${state.graceDaysRemaining} grace left this month"
        else -> "${state.graceDaysRemaining} grace days left this month"
    }
}
