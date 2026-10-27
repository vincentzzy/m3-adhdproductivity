package com.nowapp.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nowapp.ServiceLocator
import com.nowapp.domain.TimeMath
import kotlinx.coroutines.launch

/** Restores the anchor after a reboot, a package update, or a clock/timezone change. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        ServiceLocator.scope.launch {
            runCatching {
                ServiceLocator.streak.rollForward(TimeMath.today())
                Notifications.ensureChannels(app)
                PingScheduler.scheduleNext(app)
                DailyWorker.enqueue(app)
            }
        }
    }
}
