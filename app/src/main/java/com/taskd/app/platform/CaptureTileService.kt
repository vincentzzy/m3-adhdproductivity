package com.taskd.app.platform

import android.content.Intent
import android.service.quicksettings.TileService

class CaptureTileService : TileService() {
    override fun onClick() {
        super.onClick()
        startActivityAndCollapse(Intent(this, QuickCaptureActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
