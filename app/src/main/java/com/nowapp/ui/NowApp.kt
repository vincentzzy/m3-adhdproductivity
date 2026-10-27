package com.nowapp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowapp.data.Prefs
import com.nowapp.ui.components.CelebrationOverlay
import com.nowapp.ui.components.MinutePickerDialog
import com.nowapp.ui.focus.FocusScreen
import com.nowapp.ui.now.NowScreen
import com.nowapp.ui.progress.ProgressScreen
import com.nowapp.ui.settings.SettingsSheet
import com.nowapp.ui.theme.NowTheme
import com.nowapp.ui.time.TimeScreen
import kotlinx.coroutines.launch
import java.time.LocalDate

private data class FocusTarget(val taskId: Long?, val minutes: Int)

/**
 * App shell: three tabs, one focus mode, one settings sheet.
 *
 * The NOW tab is index 0 and is not restored from disk, so the app always opens
 * where the user's attention should be.
 */
@Composable
fun NowApp(vm: NowViewModel, openFocusOnLaunch: Boolean = false) {
    val prefs by vm.prefs.collectAsStateWithLifecycle()
    NowTheme(calmMode = prefs.calmMode) {
        if (!prefs.onboarded) {
            Onboarding(onDone = { vm.setOnboarded() })
        } else {
            MainSurface(vm = vm, prefs = prefs, openFocusOnLaunch = openFocusOnLaunch)
        }
    }
}

@Composable
private fun MainSurface(vm: NowViewModel, prefs: Prefs, openFocusOnLaunch: Boolean) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }

    var tab by rememberSaveable { mutableIntStateOf(0) }
    var focusTarget by remember { mutableStateOf<FocusTarget?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<String?>(null) }

    val anchors by vm.anchors.collectAsStateWithLifecycle()
    val justCompleted by vm.justCompleted.collectAsStateWithLifecycle()
    val allTasks by vm.tasks.collectAsStateWithLifecycle()
    val pendingActual by vm.pendingActual.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            vm.exportJson { json ->
                val ok = runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                }.isSuccess
                scope.launch {
                    snackbar.showSnackbar(if (ok) "Backup saved." else "Could not write that file.")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            if (text != null) pendingImport = text
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        vm.messages.collect { message -> snackbar.showSnackbar(message) }
    }
    LaunchedEffect(openFocusOnLaunch) {
        if (openFocusOnLaunch && focusTarget == null) focusTarget = FocusTarget(null, 25)
    }

    val target = focusTarget
    if (target != null) {
        FocusScreen(
            vm = vm,
            taskId = target.taskId,
            initialMinutes = target.minutes,
            onExit = { focusTarget = null },
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        icon = { Icon(Icons.Default.Home, contentDescription = null) },
                        label = { Text("Now") },
                    )
                    NavigationBarItem(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                        label = { Text("Time") },
                    )
                    NavigationBarItem(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        icon = { Icon(Icons.Default.Star, contentDescription = null) },
                        label = { Text("Progress") },
                    )
                }
            },
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Now",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }

                when (tab) {
                    0 -> NowScreen(
                        vm = vm,
                        contentPadding = PaddingValues(0.dp),
                        onStartFocus = { taskId, minutes -> focusTarget = FocusTarget(taskId, minutes) },
                    )

                    1 -> TimeScreen(
                        vm = vm,
                        contentPadding = PaddingValues(0.dp),
                        onStartFocus = { focusTarget = FocusTarget(null, 25) },
                    )

                    else -> ProgressScreen(vm = vm, contentPadding = PaddingValues(0.dp))
                }
            }
        }

        if (showSettings) {
            SettingsSheet(
                prefs = prefs,
                anchors = anchors,
                onDismiss = { showSettings = false },
                onCalmMode = { vm.setCalmMode(it) },
                onToggleQuietToday = { vm.toggleQuietForToday() },
                onQuietHours = { start, end -> vm.setQuietHours(start, end) },
                onAwakeWindow = { start, end -> vm.setAwakeWindow(start, end) },
                onAnchor = { anchor -> vm.upsertAnchor(anchor) },
                onDeleteAnchor = { id -> vm.deleteAnchor(id) },
                onRequestNotifications = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                onExport = { exportLauncher.launch("now-backup-${LocalDate.now()}.json") },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
            )
        }

        justCompleted?.let {
            CelebrationOverlay(text = "That counts.", onFinished = { vm.clearJustCompleted() })
        }

        // Only ever shown for a task that had an estimate. Skipping is one tap and
        // costs nothing; this is how the personal multiplier earns its samples.
        val actualTarget = allTasks.firstOrNull { it.id == pendingActual }
        if (actualTarget != null && justCompleted == null) {
            MinutePickerDialog(
                title = "How long did that take?",
                subtitle = actualTarget.title,
                initial = actualTarget.estimateMinutes,
                onDismiss = { vm.clearPendingActual() },
                onPick = { minutes -> vm.setActual(actualTarget.id, minutes) },
            )
        }

        pendingImport?.let { text ->
            AlertDialog(
                onDismissRequest = { pendingImport = null },
                title = { Text("Replace everything?") },
                text = {
                    Text("Importing this backup replaces the tasks on this phone with the ones in the file.")
                },
                confirmButton = {
                    Button(onClick = {
                        pendingImport = null
                        vm.importJson(text) { result ->
                            scope.launch {
                                snackbar.showSnackbar(
                                    result.fold(
                                        onSuccess = { "Imported $it tasks." },
                                        onFailure = { "That file could not be read." },
                                    )
                                )
                            }
                        }
                    }) { Text("Import") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingImport = null }) { Text("Cancel") }
                },
            )
        }
        }
    }
}

/**
 * Onboarding: one screen, one button, one optional permission prompt.
 * Three taps maximum, no carousel, no tour.
 */
@Composable
private fun Onboarding(onDone: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { onDone() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Now",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Three buckets. One thing in Now, three in Today, everything else waits quietly in Later.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Nothing here turns red. One done out of three is a good day.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                // Tap 1: finish onboarding. Tap 2: the system permission sheet.
                val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needsPermission) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onDone()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) { Text("Start") }
    }
}
