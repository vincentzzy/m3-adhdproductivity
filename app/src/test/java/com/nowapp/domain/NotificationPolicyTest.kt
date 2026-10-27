package com.nowapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class NotificationPolicyTest {

    private val today = LocalDate.of(2026, 10, 27)
    private val quiet = QuietHours(LocalTime.of(21, 30), LocalTime.of(8, 0))
    private val daytime = LocalTime.of(14, 0)

    @Test
    fun `quiet hours wrap past midnight`() {
        assertTrue(quiet.contains(LocalTime.of(23, 0)))
        assertTrue(quiet.contains(LocalTime.of(2, 0)))
        assertTrue(quiet.contains(LocalTime.of(7, 59)))
        assertFalse(quiet.contains(LocalTime.of(8, 0)))
        assertFalse(quiet.contains(LocalTime.of(14, 0)))
    }

    @Test
    fun `three timed pings are allowed and the fourth is not`() {
        var quota = PingQuota(dayKey = NotificationPolicy.dayKey(today))
        repeat(3) {
            assertTrue(NotificationPolicy.canSendTimed(quota, quiet, false, daytime))
            quota = NotificationPolicy.recordTimed(quota)
        }
        assertFalse(NotificationPolicy.canSendTimed(quota, quiet, false, daytime))
    }

    @Test
    fun `escalation is capped at two`() {
        var quota = PingQuota(dayKey = NotificationPolicy.dayKey(today))
        repeat(2) {
            assertTrue(NotificationPolicy.canEscalate(quota, quiet, false, daytime))
            quota = NotificationPolicy.recordEscalation(quota)
        }
        assertFalse(NotificationPolicy.canEscalate(quota, quiet, false, daytime))
    }

    @Test
    fun `hard ceiling is five notifications in total`() {
        var quota = PingQuota(dayKey = NotificationPolicy.dayKey(today))
        repeat(3) { quota = NotificationPolicy.recordTimed(quota) }
        repeat(2) { quota = NotificationPolicy.recordEscalation(quota) }
        assertEquals(NotificationPolicy.MAX_PER_DAY, quota.total)
        assertFalse(NotificationPolicy.canSendTimed(quota, quiet, false, daytime))
        assertFalse(NotificationPolicy.canEscalate(quota, quiet, false, daytime))
    }

    @Test
    fun `quiet for today blocks everything even outside quiet hours`() {
        val quota = PingQuota(dayKey = NotificationPolicy.dayKey(today))
        assertFalse(NotificationPolicy.canSendTimed(quota, quiet, quietForToday = true, now = daytime))
        assertFalse(NotificationPolicy.canEscalate(quota, quiet, quietForToday = true, now = daytime))
    }

    @Test
    fun `counters reset on a new day`() {
        val stale = PingQuota(dayKey = "2026-10-26", timedPings = 3, escalations = 2)
        val fresh = NotificationPolicy.normalize(stale, today)
        assertEquals(0, fresh.timedPings)
        assertEquals(NotificationPolicy.dayKey(today), fresh.dayKey)
    }

    @Test
    fun `a task may escalate only twice`() {
        assertTrue(NotificationPolicy.mayEscalateFor(0))
        assertTrue(NotificationPolicy.mayEscalateFor(1))
        assertFalse(NotificationPolicy.mayEscalateFor(2))
    }

    @Test
    fun `next anchor is the next time of day`() {
        val anchors = listOf(LocalTime.of(8, 30), LocalTime.of(14, 30), LocalTime.of(20, 0))
        assertEquals(30, NotificationPolicy.nextAnchorIn(LocalTime.of(14, 0), anchors))
        assertEquals(270, NotificationPolicy.nextAnchorIn(LocalTime.of(9, 30), anchors))
    }
}
