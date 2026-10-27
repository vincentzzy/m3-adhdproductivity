package com.nowapp.domain

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Hours/minutes remaining to a deadline. Never negative, never red. */
data class Countdown(val hours: Long, val minutes: Long, val isPast: Boolean) {
    fun label(): String = when {
        isPast -> "passed"
        hours <= 0 && minutes <= 0 -> "due now"
        hours <= 0 -> "${minutes}m"
        else -> "${hours}h ${minutes}m"
    }
}

object TimeMath {

    fun countdown(now: Instant, deadline: Instant): Countdown {
        val duration = Duration.between(now, deadline)
        if (duration.isNegative) return Countdown(0, 0, isPast = true)
        return Countdown(duration.toHours(), duration.toMinutes() % 60, isPast = false)
    }

    /** Convenience overload for callers that already have epoch millis. */
    fun countdown(nowMillis: Long, deadlineMillis: Long): Countdown =
        countdown(Instant.ofEpochMilli(nowMillis), Instant.ofEpochMilli(deadlineMillis))

    /** The next deadline that still matters: earliest due task that is not done. */
    fun nextDeadline(tasks: List<Task>): Long? =
        tasks.filter { it.dueAt != null && !it.isDone }.minOfOrNull { it.dueAt!! }

    fun startOfDayMillis(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Long =
        date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun today(zone: ZoneId = ZoneId.systemDefault()): LocalDate = LocalDate.now(zone)

    fun toLocalTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalTime =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalTime()

    fun toLocalDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()

    /** Epoch millis for a wall-clock time today (or the next occurrence if it already passed). */
    fun nextOccurrenceMillis(
        time: LocalTime,
        now: LocalDateTime = LocalDateTime.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        var candidate = now.toLocalDate().atTime(time)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return candidate.atZone(zone).toInstant().toEpochMilli()
    }

    /** "07:00" style, for anchors and quiet hours. */
    fun formatClock(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)
}
