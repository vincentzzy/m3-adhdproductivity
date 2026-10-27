package com.taskd.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.taskd.app.ui.TaskdApp
import com.taskd.app.platform.Notifications
import com.taskd.app.data.TaskRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            TaskdApp(sharedText = intent?.getStringExtra(Intent.EXTRA_TEXT), startFocus = intent?.getBooleanExtra("focus", false) == true)
        }
    }
    override fun onResume() {
        super.onResume()
        // Refresh the single anchor when returning; screen state updates it again after task changes.
        kotlinx.coroutines.MainScope().launch {
            val tasks = TaskRepository(this@MainActivity).tasks.first()
            val today = tasks.filter { it.bucket == "TODAY" || it.bucket == "NOW" }
            Notifications.update(this@MainActivity, today.size, tasks.firstOrNull { it.bucket == "NOW" }?.title.orEmpty(), tasks.firstOrNull { it.bucket == "NOW" }?.id)
        }
    }
}
