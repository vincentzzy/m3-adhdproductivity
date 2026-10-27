package com.nowapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StreakMathTest {

    private val day1: LocalDate = LocalDate.of(2026, 10, 1)

    @Test
    fun `completing consecutive days grows the streak and the best record`() {
        var state = StreakState()
        state = StreakMath.advance(state, day1, qualifiedToday = true)
        state = StreakMath.advance(state, day1.plusDays(1), qualifiedToday = true)
        state = StreakMath.advance(state, day1.plusDays(2), qualifiedToday = true)
        assertEquals(3, state.current)
        assertEquals(3, state.best)
        assertFalse(state.lastDayProtected)
    }

    @Test
    fun `one missed day spends grace and reports protected instead of broken`() {
        var state = StreakMath.advance(StreakState(), day1, true)
        state = StreakMath.advance(state, day1.plusDays(1), true)
        // Jump two days: one day was skipped entirely.
        state = StreakMath.advance(state, day1.plusDays(3), true)
        assertTrue(state.lastDayProtected)
        assertEquals(1, state.graceDaysRemaining)
        assertEquals(3, state.current)
    }

    @Test
    fun `three missed days exhaust grace and reset current but keep best`() {
        var state = StreakMath.advance(StreakState(), day1, true)
        state = StreakMath.advance(state, day1.plusDays(1), true)
        state = StreakMath.advance(state, day1.plusDays(2), true)
        assertEquals(3, state.best)
        // Four untouched days: 2 grace days absorb two, the rest break the streak.
        state = StreakMath.advance(state, day1.plusDays(6), false)
        assertEquals(0, state.current)
        assertEquals(3, state.best)
        assertEquals(0, state.graceDaysRemaining)
    }

    @Test
    fun `grace refills at the start of a new month`() {
        var state = StreakMath.advance(StreakState(), day1, true)
        state = StreakMath.advance(state, day1.plusDays(1), false) // spend nothing yet: same day
        state = StreakMath.advance(state, LocalDate.of(2026, 11, 2), false)
        assertEquals(StreakState.GRACE_DAYS_PER_MONTH, state.graceDaysRemaining)
    }

    @Test
    fun `advancing twice on the same qualified day does not double count`() {
        var state = StreakMath.advance(StreakState(), day1, true)
        state = StreakMath.advance(state, day1, true)
        state = StreakMath.advance(state, day1, true)
        assertEquals(1, state.current)
    }

    @Test
    fun `a fresh state advances without inventing history`() {
        val state = StreakMath.advance(StreakState(), day1, qualifiedToday = false)
        assertEquals(0, state.current)
        assertEquals(0, state.best)
        assertEquals(StreakState.GRACE_DAYS_PER_MONTH, state.graceDaysRemaining)
    }

    @Test
    fun `best record is never reduced by a later collapse`() {
        var state = StreakState()
        repeat(5) { offset -> state = StreakMath.advance(state, day1.plusDays(offset.toLong()), true) }
        assertEquals(5, state.best)
        state = StreakMath.advance(state, day1.plusDays(30), false)
        assertEquals(5, state.best)
    }

    @Test
    fun `month key is stable and zero padded`() {
        assertEquals("2026-10", StreakMath.monthKey(LocalDate.of(2026, 10, 5)))
        assertEquals("2026-01", StreakMath.monthKey(LocalDate.of(2026, 1, 5)))
    }
}
