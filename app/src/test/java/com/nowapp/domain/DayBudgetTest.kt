package com.nowapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DayBudgetTest {

    @Test
    fun `planned time is the sum of positive estimates`() {
        val budget = DayBudgetMath.compute(listOf(25, 45, null.let { 0 }, 30))
        assertEquals(100, budget.plannedMinutes)
        assertEquals(DayBudgetMath.DEFAULT_AWAKE_MINUTES - 100, budget.remainingMinutes)
        assertFalse(budget.isOverBudget)
    }

    @Test
    fun `used fraction is clamped to one so the bar can never overflow`() {
        val budget = DayBudgetMath.compute(listOf(900, 900))
        assertEquals(1f, budget.usedFraction, 0.0001f)
        assertTrue(budget.isOverBudget)
    }

    @Test
    fun `empty day is zero percent used`() {
        val budget = DayBudgetMath.compute(emptyList())
        assertEquals(0f, budget.usedFraction, 0.0001f)
        assertEquals(DayBudgetMath.DEFAULT_AWAKE_MINUTES, budget.remainingMinutes)
    }

    @Test
    fun `minutes between wall clock times wraps past midnight`() {
        assertEquals(60, DayBudgetMath.minutesBetween(LocalTime.of(23, 30), LocalTime.of(0, 30)))
        assertEquals(120, DayBudgetMath.minutesBetween(LocalTime.of(8, 0), LocalTime.of(10, 0)))
    }

    @Test
    fun `elapsed fraction of the day is proportional`() {
        val fraction = DayBudgetMath.elapsedFraction(
            now = LocalTime.of(15, 0),
            dayStart = LocalTime.of(7, 0),
            dayEnd = LocalTime.of(23, 0),
        )
        assertEquals(0.5f, fraction, 0.0001f)
    }

    @Test
    fun `elapsed fraction before the day starts is zero and after it ends is one`() {
        assertEquals(
            0f,
            DayBudgetMath.elapsedFraction(LocalTime.of(5, 0), LocalTime.of(7, 0), LocalTime.of(23, 0)),
            0.0001f,
        )
        assertEquals(
            1f,
            DayBudgetMath.elapsedFraction(LocalTime.of(23, 30), LocalTime.of(7, 0), LocalTime.of(23, 0)),
            0.0001f,
        )
    }

    @Test
    fun `durations format without unit soup`() {
        assertEquals("45m", DayBudgetMath.formatDuration(45))
        assertEquals("2h", DayBudgetMath.formatDuration(120))
        assertEquals("2h 15m", DayBudgetMath.formatDuration(135))
        assertEquals("0m", DayBudgetMath.formatDuration(0))
    }
}
