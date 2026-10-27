package com.taskd.app.domain

import java.time.LocalDate
import kotlin.math.roundToInt

/** Pure rules live here so the most important anti-abandonment guarantees are testable. */
object Rules {
    /** A deliberately small offline nudge: turn a vague task into concrete next actions. */
    fun breakdown(title: String): List<String> {
        val subject = title.trim().ifBlank { "the task" }
        return listOf("Open what you need for $subject (about 2 minutes)", "Write down the very first small action", "Do that action for five minutes", "Check what the next step should be")
    }

    fun overflowBucket(bucket: String, todayCount: Int): String =
        if (bucket == "TODAY" && todayCount >= 3) "LATER" else bucket

    fun dayBudgetPercent(availableMinutes: Int, plannedMinutes: Int): Int {
        if (availableMinutes <= 0) return 0
        return ((plannedMinutes.coerceAtLeast(0).toFloat() / availableMinutes) * 100)
            .roundToInt().coerceIn(0, 100)
    }

    /** Actual/estimate ratio; clamp outliers so one unusual day cannot dominate forever. */
    fun estimateMultiplier(samples: List<Pair<Int, Int>>): Double? {
        val ratios = samples.filter { (estimate, actual) -> estimate > 0 && actual > 0 }
            .map { (estimate, actual) -> actual.toDouble() / estimate }
        return if (ratios.size < 10) null else ratios.average().coerceIn(0.5, 3.0)
    }

    data class Streak(val current: Int, val best: Int, val graceRemaining: Int, val lastDate: LocalDate?)

    /** A skipped calendar day is protected by one of this month's two automatic grace days. */
    fun advanceStreak(state: Streak, completedDate: LocalDate): Streak {
        if (state.lastDate == completedDate) return state
        val previous = state.lastDate
        if (previous == null) return Streak(1, maxOf(1, state.best), state.graceRemaining, completedDate)
        val gap = (completedDate.toEpochDay() - previous.toEpochDay()).toInt()
        if (gap <= 0) return state
        val monthChanged = previous.month != completedDate.month || previous.year != completedDate.year
        val available = if (monthChanged) 2 else state.graceRemaining
        val missedDays = gap - 1
        return if (missedDays <= available) {
            val current = state.current + 1
            Streak(current, maxOf(state.best, current), available - missedDays, completedDate)
        } else {
            Streak(1, maxOf(1, state.best), maxOf(0, available - minOf(missedDays, available)), completedDate)
        }
    }
}
