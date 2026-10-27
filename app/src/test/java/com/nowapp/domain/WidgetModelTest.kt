package com.nowapp.domain

import com.nowapp.widget.WidgetModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The widget's contents are decided by pure Kotlin, so the two things that used to
 * require a launcher to notice — right rows, right words — are tested here instead.
 */
class WidgetModelTest {

    private fun task(id: Long, title: String, bucket: Bucket = Bucket.TODAY, done: Boolean = false) =
        Task(
            id = id,
            title = title,
            bucket = bucket,
            createdAt = id,
            completedAt = if (done) 1_000L else null,
        )

    @Test
    fun `empty today says so without asking for anything`() {
        val model = WidgetModel.build(emptyList())
        assertTrue(model.rows.isEmpty())
        assertEquals("Today is clear", model.header)
        assertEquals("Tap + to add one.", model.emptyMessage)
    }

    @Test
    fun `a task in Now is surfaced when today is empty`() {
        val model = WidgetModel.build(listOf(task(1, "Email the landlord", Bucket.NOW)))
        assertEquals("Now: Email the landlord", model.emptyMessage)
        assertTrue(model.rows.isEmpty())
    }

    @Test
    fun `today never renders more than three rows`() {
        val tasks = (1L..5L).map { task(it, "task $it") }
        val model = WidgetModel.build(tasks)
        assertEquals(3, model.rows.size)
        assertEquals("and 2 more", model.footer)
    }

    @Test
    fun `completed tasks never occupy a row`() {
        val model = WidgetModel.build(listOf(task(1, "done thing", done = true)))
        assertTrue(model.rows.isEmpty())
        assertNull(model.rows.firstOrNull())
    }

    @Test
    fun `rows keep the order they were created in`() {
        val tasks = listOf(task(3, "third"), task(1, "first"), task(2, "second"))
        val model = WidgetModel.build(tasks)
        assertEquals(listOf("first", "second", "third"), model.rows.map { it.title })
        assertEquals(listOf(1L, 2L, 3L), model.rows.map { it.taskId })
    }

    @Test
    fun `header pluralises at one and stays quiet at zero`() {
        assertEquals("Today: 1 thing", WidgetModel.build(listOf(task(1, "a"))).header)
        assertEquals("Today: 2 things", WidgetModel.build(listOf(task(1, "a"), task(2, "b"))).header)
        assertEquals("Today is clear", WidgetModel.build(emptyList()).header)
    }

    @Test
    fun `later is never counted in the footer`() {
        val tasks = listOf(task(1, "a"), task(2, "b", Bucket.LATER), task(3, "c", Bucket.LATER))
        val model = WidgetModel.build(tasks)
        assertEquals(1, model.rows.size)
        assertEquals("Later stays quiet", model.footer)
    }
}
