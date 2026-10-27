package com.nowapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.nowapp.platform.Notifications
import com.nowapp.ui.NowApp
import com.nowapp.ui.NowViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * The only activity for the main UI. No splash screen, no loading gate: the
 * Compose tree is set immediately and the database arrives through flows.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: NowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openFocus = intent?.getBooleanExtra(Notifications.EXTRA_OPEN_FOCUS, false) ?: false
        setContent {
            NowApp(vm = viewModel, openFocusOnLaunch = openFocus)
        }
    }

    override fun onStop() {
        super.onStop()
        // Recorded here rather than on every data change, so the welcome-back
        // message can still tell how long the app went unopened.
        lifecycleScope.launch {
            ServiceLocator.prefs.setLastActiveDay(LocalDate.now().toString())
        }
    }
}
