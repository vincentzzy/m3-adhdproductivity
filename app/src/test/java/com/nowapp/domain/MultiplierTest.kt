package com.nowapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MultiplierTest {

    private fun samples(count: Int, estimate: Int, actual: Int) =
        List(count) { EstimateSample(estimateMinutes = estimate, actualMinutes = actual) }

    @Test
    fun `no multiplier until ten samples`() {
        assertNull(Multiplier.multiplier(samples(9, estimate = 20, actual = 30)))
        assertTrue(Multiplier.multiplier(samples(10, estimate = 20, actual = 30)) != null)
    }

    @Test
    fun `multiplier is the mean ratio of actual to estimate`() {
        val mixed = List(5) { EstimateSample(20, 40) } + List(5) { EstimateSample(20, 20) }
        assertEquals(1.5, Multiplier.multiplier(mixed)!!, 0.0001)
    }

    @Test
    fun `zero estimates are ignored rather than treated as infinite`() {
        val withZero = samples(10, 20, 30) + EstimateSample(0, 60)
        assertEquals(1.5, Multiplier.multiplier(withZero)!!, 0.0001)
    }

    @Test
    fun `adjusted estimate rounds to five minutes and respects the floor`() {
        assertEquals(30, Multiplier.adjustedEstimate(20, 1.5))
        assertEquals(5, Multiplier.adjustedEstimate(1, 1.0))
        assertEquals(20, Multiplier.adjustedEstimate(20, null))
    }

    @Test
    fun `multiplier is clamped so one wild session cannot wreck planning`() {
        val absurd = samples(10, 5, 300)
        assertEquals(100, Multiplier.adjustedEstimate(20, Multiplier.multiplier(absurd)))
    }

    @Test
    fun `best multiplier picks the window closest to one`() {
        val samples = List(10) { EstimateSample(10, 30) } + List(10) { EstimateSample(10, 11) }
        val best = Multiplier.bestMultiplier(samples)!!
        assertTrue(best < 1.2)
    }

    @Test
    fun `explanation never blames the user`() {
        listOf(0.5, 1.0, 1.3, 2.0).forEach { value ->
            val text = Multiplier.explain(value)
            assertTrue(text.isNotBlank())
            assertTrue(!text.lowercase().contains("bad"))
            assertTrue(!text.lowercase().contains("wrong"))
        }
    }
}
