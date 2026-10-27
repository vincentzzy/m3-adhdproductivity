package com.nowapp.platform

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/**
 * Quick Settings tile: one tap drops the user into the capture field, so a
 * thought can be captured from inside any other app in about a second.
 */
class CaptureTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            label = "Capture"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val intent = Intent(this, QuickCaptureActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { startActivityAndCollapse(intent) }
            .onFailure { runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }
}
