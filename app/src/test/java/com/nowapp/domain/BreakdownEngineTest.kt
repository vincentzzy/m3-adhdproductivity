package com.nowapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreakdownEngineTest {

    @Test
    fun `every breakdown has three to five steps`() {
        listOf(
            "Write the essay",
            "Read chapter 4",
            "Fix the bug in my assignment",
            "Reply to professor's email",
            "Do the dishes",
            "Submit the form",
            "Call the clinic",
            "Buy groceries",
            "Practise the presentation",
            "Go for a walk",
            "something completely unrelated",
        ).forEach { title ->
            val breakdown = BreakdownEngine.forTask(title)
            assertTrue("$title -> ${breakdown.steps.size}", breakdown.steps.size in 3..5)
        }
    }

    @Test
    fun `the first step is always five minutes or less`() {
        listOf(
            "Write the essay",
            "Read chapter 4",
            "Fix the bug",
            "Do the dishes",
            "anything at all",
        ).forEach { title ->
            val first = BreakdownEngine.forTask(title).steps.first()
            assertTrue("$title first step was ${first.minutes}m", first.minutes <= 5)
        }
    }

    @Test
    fun `keyword families pick a matching template`() {
        val coding = BreakdownEngine.forTask("Finish the coding assignment")
        assertTrue(coding.steps.first().text.contains("editor", ignoreCase = true))

        val reading = BreakdownEngine.forTask("Read the reading for tomorrow")
        assertTrue(reading.steps.first().text.contains("read", ignoreCase = true))
    }

    @Test
    fun `unknown tasks fall back to a generic four step plan`() {
        val breakdown = BreakdownEngine.forTask("Zorblax")
        assertEquals(4, breakdown.steps.size)
        assertTrue(breakdown.steps.first().text.contains("done", ignoreCase = true))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a breakdown with a slow first step is rejected by construction`() {
        Breakdown(
            listOf(
                BreakdownStep("Something long", 30),
                BreakdownStep("Second", 10),
                BreakdownStep("Third", 10),
            )
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a two step breakdown is rejected by construction`() {
        Breakdown(listOf(BreakdownStep("One", 2), BreakdownStep("Two", 3)))
    }
}
