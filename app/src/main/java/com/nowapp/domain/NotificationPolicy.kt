package com.nowapp.domain

import java.time.LocalDate
import java.time.LocalTime

/** Quiet hours, allowed to wrap past midnight (e.g. 23:00 -> 08:00). */
data class QuietHours(val start: LocalTime = LocalTime.of(21, 30), val end: LocalTime = LocalTime.of(8, 0)) {
    fun contains(time: LocalTime): Boolean =
        if (start <= end) time >= start && time < end
        else time >= start || time < end
}

/** How many pings have been spent today. Persisted per calendar day. */
data class PingQuota(
    val dayKey: String,
    val timedPings: Int = 0,
    val escalations: Int = 0,
) {
    val total: Int get() = timedPings + escalations
}

/**
 * The notification budget. Rules are hard, small numbers so the user can trust
 * that the app will never become a source of noise:
 *
 *  - at most 3 timed anchor pings per day;
 *  - at most 2 escalating pings for a hard-deadline task, then silence;
 *  - never more than 5 notifications in total per day;
 *  - quiet hours and "quiet for today" beat everything.
 */
object NotificationPolicy {

    const val MAX_TIMED_PINGS = 3
    const val MAX_ESCALATIONS = 2
    const val MAX_PER_DAY = 5

    fun dayKey(date: LocalDate): String = date.toString()

    /** Resets the counters when the calendar day (or the quiet-for-today flag day) changes. */
    fun normalize(quota: PingQuota, today: LocalDate): PingQuota =
        if (quota.dayKey == dayKey(today)) quota else PingQuota(dayKey(today))

    fun canSendTimed(
        quota: PingQuota,
        quietHours: QuietHours,
        quietForToday: Boolean,
        now: LocalTime,
    ): Boolean {
        if (quietForToday) return false
        if (quietHours.contains(now)) return false
        if (quota.total >= MAX_PER_DAY) return false
        return quota.timedPings < MAX_TIMED_PINGS
    }

    fun canEscalate(
        quota: PingQuota,
        quietHours: QuietHours,
        quietForToday: Boolean,
        now: LocalTime,
    ): Boolean {
        if (quietForToday) return false
        if (quietHours.contains(now)) return false
        if (quota.total >= MAX_PER_DAY) return false
        return quota.escalations < MAX_ESCALATIONS
    }

    fun recordTimed(quota: PingQuota): PingQuota = quota.copy(timedPings = quota.timedPings + 1)

    fun recordEscalation(quota: PingQuota): PingQuota = quota.copy(escalations = quota.escalations + 1)

    /** Whether a task may still escalate: two attempts, then the app goes quiet. */
    fun mayEscalateFor(previousEscalationsForTask: Int): Boolean =
        previousEscalationsForTask < MAX_ESCALATIONS

    /** Minutes until the next anchor, or null when the day's anchors are done. */
    fun nextAnchorIn(now: LocalTime, anchors: List<LocalTime>): Int? {
        val next = anchors.filter { it > now }.minOrNull() ?: return null
        return DayBudgetMath.minutesBetween(now, next)
    }
}
