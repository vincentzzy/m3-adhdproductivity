package com.nowapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BucketRulesTest {

    private fun task(id: Long, bucket: Bucket, createdAt: Long = id) = Task(
        id = id,
        title = "Task $id",
        bucket = bucket,
        createdAt = createdAt,
    )

    @Test
    fun `today never exceeds the hard cap of three`() {
        val tasks = listOf(
            task(1, Bucket.TODAY, 10),
            task(2, Bucket.TODAY, 20),
            task(3, Bucket.TODAY, 30),
            task(4, Bucket.LATER, 40),
        )
        val result = BucketRules.move(tasks, taskId = 4, target = Bucket.TODAY)
        assertEquals(3, result.tasks.count { it.bucket == Bucket.TODAY && !it.isDone })
        // The oldest item is the one that steps aside, not the newest arrival.
        assertEquals(Bucket.LATER, result.tasks.first { it.id == 1L }.bucket)
        assertEquals(Bucket.TODAY, result.tasks.first { it.id == 4L }.bucket)
        assertTrue(result.message!!.contains("Today was full"))
    }

    @Test
    fun `overflow waits silently in later when capture targets a full today`() {
        val tasks = listOf(
            task(1, Bucket.TODAY, 10),
            task(2, Bucket.TODAY, 20),
            task(3, Bucket.TODAY, 30),
        )
        assertEquals(Bucket.LATER, BucketRules.destinationForCapture(tasks, userChoseToday = true))
    }

    @Test
    fun `capture defaults to later so junk does not eat today`() {
        assertEquals(Bucket.LATER, BucketRules.destinationForCapture(emptyList(), userChoseToday = false))
    }

    @Test
    fun `capture goes to today when there is room and the user asked for it`() {
        assertEquals(Bucket.TODAY, BucketRules.destinationForCapture(listOf(task(1, Bucket.TODAY)), true))
    }

    @Test
    fun `now holds exactly one item and pushes the old one to today`() {
        val tasks = listOf(
            task(1, Bucket.NOW, 10),
            task(2, Bucket.LATER, 20),
        )
        val result = BucketRules.move(tasks, taskId = 2, target = Bucket.NOW)
        assertEquals(1, result.tasks.count { it.bucket == Bucket.NOW })
        assertEquals(Bucket.TODAY, result.tasks.first { it.id == 1L }.bucket)
        assertEquals(2L, BucketRules.nowTask(result.tasks)!!.id)
    }

    @Test
    fun `pushing out of now falls back to later when today is full`() {
        val tasks = listOf(
            task(1, Bucket.NOW, 10),
            task(2, Bucket.TODAY, 20),
            task(3, Bucket.TODAY, 30),
            task(4, Bucket.TODAY, 40),
            task(5, Bucket.LATER, 50),
        )
        val result = BucketRules.move(tasks, taskId = 5, target = Bucket.NOW)
        assertEquals(Bucket.LATER, result.tasks.first { it.id == 1L }.bucket)
        assertEquals(3, result.tasks.count { it.bucket == Bucket.TODAY })
    }

    @Test
    fun `moving to later is always allowed and never produces a message`() {
        val tasks = listOf(task(1, Bucket.TODAY))
        val result = BucketRules.move(tasks, 1, Bucket.LATER)
        assertEquals(Bucket.LATER, result.tasks.first().bucket)
        assertNull(result.message)
    }

    @Test
    fun `completed tasks do not occupy capacity`() {
        val done = task(1, Bucket.TODAY).copy(completedAt = 1_000L)
        val tasks = listOf(done, task(2, Bucket.TODAY, 20), task(3, Bucket.TODAY, 30), task(4, Bucket.LATER, 40))
        val result = BucketRules.move(tasks, taskId = 4, target = Bucket.TODAY)
        // Room was available, so nothing was swapped out.
        assertNull(result.message)
        assertEquals(4, result.tasks.count { it.bucket == Bucket.TODAY })
    }

    @Test
    fun `completed today counts only tasks finished after midnight`() {
        val morning = 1_000L
        val tasks = listOf(
            task(1, Bucket.TODAY).copy(completedAt = 900L),
            task(2, Bucket.TODAY).copy(completedAt = 1_500L),
        )
        assertEquals(1, BucketRules.completedTodayCount(tasks, dayStartMillis = 1_000L))
    }
}
