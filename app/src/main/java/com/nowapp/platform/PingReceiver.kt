package com.nowapp.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nowapp.ServiceLocator
import com.nowapp.domain.BucketRules
import com.nowapp.domain.NotificationPolicy
import com.nowapp.domain.PingQuota
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * Fires an anchor ping at most three times a day, naming exactly one task.
 *
 * All of the restraint lives in [NotificationPolicy]; this receiver only wires
 * it to the database and the notification.
 */
class PingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val app = context.applicationContext
        ServiceLocator.scope.launch {
            try {
                val prefs = ServiceLocator.prefs.prefs.first()
                val today = LocalDate.now()
                val quota = NotificationPolicy.normalize(
                    PingQuota(dayKey = prefs.pingDayKey, timedPings = prefs.timedPings, escalations = prefs.escalations),
                    today,
                )
                val quietForToday = prefs.quietForTodayKey == today.toString()
                val now = LocalTime.now()

                val tasks = ServiceLocator.tasks.allOnce()
                val nowTask = BucketRules.nowTask(tasks)
                val todayTasks = BucketRules.todayTasks(tasks)
                val atRisk = tasks.firstOrNull { it.isAtRisk && it.dueAt != null && it.dueAt < System.currentTimeMillis() + 6 * 3600_000L }
                val candidate = atRisk ?: nowTask ?: todayTasks.firstOrNull()

                if (candidate == null) {
                    ServiceLocator.prefs.setPingCounters(quota.dayKey, quota.timedPings, quota.escalations, prefs.escalationLog)
                } else {
                    val snoozed = prefs.snoozeTaskId == candidate.id && prefs.snoozeUntil > System.currentTimeMillis()
                    val escalationCount = prefs.escalationLog
                        .split(",")
                        .count { it == candidate.id.toString() }

                    when {
                        snoozed -> Unit
                        candidate.isHardDeadline && NotificationPolicy.mayEscalateFor(escalationCount) &&
                            NotificationPolicy.canEscalate(quota, prefs.quietHours, quietForToday, now) -> {
                            Notifications.showPing(app, candidate.title, candidate.id, isEscalation = true)
                            val next = NotificationPolicy.recordEscalation(quota)
                            val log = if (prefs.escalationLog.isBlank()) candidate.id.toString()
                            else prefs.escalationLog + "," + candidate.id
                            ServiceLocator.prefs.setPingCounters(next.dayKey, next.timedPings, next.escalations, log)
                        }
                        NotificationPolicy.canSendTimed(quota, prefs.quietHours, quietForToday, now) -> {
                            Notifications.showPing(app, candidate.title, candidate.id, isEscalation = false)
                            val next = NotificationPolicy.recordTimed(quota)
                            ServiceLocator.prefs.setPingCounters(next.dayKey, next.timedPings, next.escalations, prefs.escalationLog)
                        }
                        else -> ServiceLocator.prefs.setPingCounters(quota.dayKey, quota.timedPings, quota.escalations, prefs.escalationLog)
                    }
                }

                PingScheduler.scheduleNext(app)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_PING = "com.nowapp.action.PING"
    }
}
