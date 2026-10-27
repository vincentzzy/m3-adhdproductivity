package com.taskd.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RulesTest {
    @Test fun todayOverflowWaitsInLater() {
        assertEquals("LATER", Rules.overflowBucket("TODAY", 3))
        assertEquals("TODAY", Rules.overflowBucket("TODAY", 2))
        assertEquals("NOW", Rules.overflowBucket("NOW", 8))
    }

    @Test fun budgetIsBoundedAndSafeForZero() {
        assertEquals(0, Rules.dayBudgetPercent(0, 100))
        assertEquals(50, Rules.dayBudgetPercent(120, 60))
        assertEquals(100, Rules.dayBudgetPercent(60, 120))
    }

    @Test fun multiplierWaitsForTenSamplesAndUsesActualOverEstimate() {
        val nine = List(9) { 10 to 15 }
        assertNull(Rules.estimateMultiplier(nine))
        assertEquals(1.5, Rules.estimateMultiplier(nine + (10 to 15))!!, .0001)
    }

    @Test fun streakUsesAutomaticGraceThenResetsWithoutShowingFailure() {
        val initial = Rules.Streak(5, 5, 2, LocalDate.parse("2026-06-01"))
        val protected = Rules.advanceStreak(initial, LocalDate.parse("2026-06-03"))
        assertEquals(6, protected.current)
        assertEquals(1, protected.graceRemaining)
        val beyondGrace = Rules.advanceStreak(protected, LocalDate.parse("2026-06-06"))
        assertEquals(1, beyondGrace.current)
        assertEquals(6, beyondGrace.best)
    }
}
