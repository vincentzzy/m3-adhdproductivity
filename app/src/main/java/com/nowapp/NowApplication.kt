package com.nowapp

import android.app.Application
import com.nowapp.domain.TimeMath
import com.nowapp.platform.DailyWorker
import com.nowapp.platform.NotificationUpdater
import com.nowapp.platform.Notifications
import com.nowapp.platform.PingScheduler
import kotlinx.coroutines.launch

/**
 * Startup does no blocking work on the main thread: the database opens lazily on
 * a background dispatcher, so the first frame of the NOW screen is not waiting on
 * disk I/O.
 */
class NowApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        ServiceLocator.init(this)
        Notifications.ensureChannels(this)
        NotificationUpdater.start(this)

        ServiceLocator.scope.launch {
            runCatching {
                ServiceLocator.anchors.seedDefaultsIfEmpty()
                ServiceLocator.streak.rollForward(TimeMath.today())
                PingScheduler.scheduleNext(this@NowApplication)
                DailyWorker.enqueue(this@NowApplication)
            }
        }
    }
}
