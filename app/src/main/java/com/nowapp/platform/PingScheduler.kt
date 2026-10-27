package com.nowapp.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.nowapp.ServiceLocator
import com.nowapp.domain.TimeMath
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Schedules the next anchor ping with an exact alarm, falling back to an
 * inexact one when the user has not granted exact alarms. Either way there are
 * never more than three timed pings in a day.
 */
object PingScheduler {

    private const val REQUEST_CODE = 3001

    fun scheduleNext(context: Context) {
        val app = context.applicationContext
        ServiceLocator.scope.launch {
            val anchors = runCatching { ServiceLocator.anchors.anchorTimes() }.getOrElse { emptyList() }
            val times = anchors.ifEmpty {
                listOf(LocalTime.of(8, 30), LocalTime.of(14, 30), LocalTime.of(20, 0))
            }
            val now = LocalDateTime.now()
            val next = times.map { TimeMath.nextOccurrenceMillis(it, now) }.minOrNull() ?: return@launch
            val manager = app.getSystemService(AlarmManager::class.java) ?: return@launch

            val pending = PendingIntent.getBroadcast(
                app,
                REQUEST_CODE,
                Intent(app, PingReceiver::class.java).setAction(PingReceiver.ACTION_PING),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            runCatching {
                if (canExact) {
                    manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
                } else {
                    manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending)
                }
            }
        }
    }
}
