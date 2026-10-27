package com.nowapp.platform

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.nowapp.ServiceLocator
import kotlinx.coroutines.launch

/**
 * Share-sheet target. Sharing text into Now captures it straight into LATER and
 * finishes: no form, no confirmation dialog, no app switch.
 */
class ShareCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent?.getStringExtra(Intent.EXTRA_SUBJECT)
        if (text.isNullOrBlank()) {
            finish()
            return
        }
        // Collapse newlines: a task title must stay one line.
        val title = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim().orEmpty()
        lifecycleScope.launch {
            if (title.isNotEmpty()) {
                ServiceLocator.tasks.capture(title.take(200))
                Toast.makeText(this@ShareCaptureActivity, "Saved to Later", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
