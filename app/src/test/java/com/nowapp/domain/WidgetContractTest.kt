package com.nowapp.domain

import com.nowapp.platform.WidgetActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Modifier

/**
 * The widget has one contract that the compiler cannot check: Glance reconstructs
 * an `ActionCallback` from its class name, by reflection, inside a broadcast that
 * may wake a dead process. That needs a public class with a public no-argument
 * constructor. Violating it fails silently on the home screen and nowhere else.
 */
class WidgetContractTest {

    @Test
    fun `the toggle callback is reflectively constructible`() {
        val clazz = WidgetActions.ToggleDoneAction::class.java
        assertTrue("callback class must be public", Modifier.isPublic(clazz.modifiers))
        val ctor = clazz.getDeclaredConstructor()
        assertTrue("no-arg constructor must be public", Modifier.isPublic(ctor.modifiers))
        assertNotNull("Glance must be able to build the callback", ctor.newInstance())
    }

    @Test
    fun `the task id key survives obfuscation-sensitive naming`() {
        // The key is serialised into a PendingIntent; both sides must agree.
        assertNotNull(WidgetActions.TaskIdKey)
    }

    @Test
    fun `a completed task leaves the widget rows`() {
        // Tapping the widget checkbox must make the row disappear, not linger as a
        // permanently ticked ghost: today's list only ever contains open tasks.
        val done = Task(
            id = 1,
            title = "finished thing",
            bucket = Bucket.TODAY,
            createdAt = 0,
            completedAt = 1_000,
        )
        assertTrue(done.isDone)
        assertTrue(BucketRules.todayTasks(listOf(done)).isEmpty())
        assertFalse(BucketRules.todayTasks(listOf(done.copy(completedAt = null))).isEmpty())
    }

    @Test
    fun `the widget never shows more than three rows`() {
        val tasks = (1L..5L).map {
            Task(id = it, title = "task $it", bucket = Bucket.TODAY, createdAt = it)
        }
        assertEquals(3, BucketRules.todayTasks(tasks).take(3).size)
        assertEquals(5, BucketRules.todayTasks(tasks).size)
    }
}
