package com.nowapp.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nowapp.data.Prefs
import com.nowapp.data.db.AnchorEntity
import com.nowapp.domain.TimeMath
import com.nowapp.widget.WidgetDiagnostics
import com.nowapp.widget.WidgetUpdater
import kotlinx.coroutines.launch
import java.time.LocalTime

/**
 * Settings. Everything the user can change lives here, and nothing here is
 * required for the app to work — a 30 second glance without opening this sheet
 * is a complete interaction.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    prefs: Prefs,
    anchors: List<AnchorEntity>,
    onDismiss: () -> Unit,
    onCalmMode: (Boolean) -> Unit,
    onToggleQuietToday: () -> Unit,
    onQuietHours: (LocalTime, LocalTime) -> Unit,
    onAwakeWindow: (LocalTime, LocalTime) -> Unit,
    onAnchor: (AnchorEntity) -> Unit,
    onDeleteAnchor: (Long) -> Unit,
    onRequestNotifications: () -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    var editingQuietStart by remember { mutableStateOf(false) }
    var editingQuietEnd by remember { mutableStateOf(false) }
    var editingAwakeStart by remember { mutableStateOf(false) }
    var editingAnchor by remember { mutableStateOf<AnchorEntity?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val widgetStatus by WidgetDiagnostics.status.collectAsState()

    // Probe once when the sheet opens, so the row reads as a live fact rather
    // than a promise. This is the whole point: the widget can fail silently.
    LaunchedEffect(Unit) { WidgetDiagnostics.probe(context) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            SettingRow(
                title = "Calm mode",
                subtitle = "Halves every animation. Useful on loud days.",
                trailing = { Switch(checked = prefs.calmMode, onCheckedChange = onCalmMode) },
            )

            SettingRow(
                title = "Quiet for today",
                subtitle = "Silences pings until tomorrow. The anchor stays.",
                trailing = {
                    Switch(
                        checked = prefs.quietForTodayKey == java.time.LocalDate.now().toString(),
                        onCheckedChange = { onToggleQuietToday() },
                    )
                },
            )

            SettingRow(
                title = "Quiet hours",
                subtitle = "${TimeMath.formatClock(prefs.quietStart)} to ${TimeMath.formatClock(prefs.quietEnd)}",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { editingQuietStart = true }) { Text("Start") }
                        TextButton(onClick = { editingQuietEnd = true }) { Text("End") }
                    }
                },
            )

            SettingRow(
                title = "Awake window",
                subtitle = "${TimeMath.formatClock(prefs.awakeStart)} to ${TimeMath.formatClock(prefs.awakeEnd)}",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { editingAwakeStart = true }) { Text("Start") }
                    }
                },
            )

            Text(
                text = "Anchor times (at most three pings a day)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            anchors.forEach { anchor ->
                SettingRow(
                    title = anchor.label,
                    subtitle = anchor.timeOfDay,
                    trailing = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { editingAnchor = anchor }) { Text("Change") }
                            TextButton(onClick = { onDeleteAnchor(anchor.id) }) { Text("Remove") }
                        }
                    },
                )
            }
            TextButton(onClick = {
                onAnchor(AnchorEntity(label = "New anchor", timeOfDay = "12:00"))
            }) { Text("Add an anchor") }

            SettingRow(
                title = "Notifications",
                subtitle = "Android may be blocking the anchor.",
                trailing = { TextButton(onClick = onRequestNotifications) { Text("Allow") } },
            )

            SettingRow(
                title = "Backup",
                subtitle = "A JSON file you choose. No account, no cloud.",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = onExport) { Text("Export") }
                        TextButton(onClick = onImport) { Text("Import") }
                    }
                },
            )

            SettingRow(
                title = "Home-screen widget",
                subtitle = widgetStatus,
                trailing = {
                    TextButton(onClick = {
                        WidgetUpdater.refresh(context)
                        scope.launch {
                            kotlinx.coroutines.delay(400)
                            WidgetDiagnostics.probe(context)
                        }
                    }) { Text("Refresh") }
                },
            )

            Text(
                text = "Now is offline, single-user, and has no analytics. Your data never leaves this phone unless you export it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp),
            )
        }
    }

    if (editingQuietStart) {
        TimeEditDialog(
            title = "Quiet hours start",
            initial = prefs.quietStart,
            onDismiss = { editingQuietStart = false },
            onConfirm = {
                editingQuietStart = false
                onQuietHours(it, prefs.quietEnd)
            },
        )
    }
    if (editingQuietEnd) {
        TimeEditDialog(
            title = "Quiet hours end",
            initial = prefs.quietEnd,
            onDismiss = { editingQuietEnd = false },
            onConfirm = {
                editingQuietEnd = false
                onQuietHours(prefs.quietStart, it)
            },
        )
    }
    if (editingAwakeStart) {
        TimeEditDialog(
            title = "Wake time",
            initial = prefs.awakeStart,
            onDismiss = { editingAwakeStart = false },
            onConfirm = {
                editingAwakeStart = false
                onAwakeWindow(it, prefs.awakeEnd)
            },
        )
    }
    editingAnchor?.let { anchor ->
        TimeEditDialog(
            title = "Time for ${anchor.label}",
            initial = runCatching { LocalTime.parse(anchor.timeOfDay) }.getOrDefault(LocalTime.of(8, 0)),
            onDismiss = { editingAnchor = null },
            onConfirm = { time ->
                onAnchor(anchor.copy(timeOfDay = TimeMath.formatClock(time)))
                editingAnchor = null
            },
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}

/** Stepper-based time picker: no keyboard, no scrolling wheel, no mis-taps. */
@Composable
fun TimeEditDialog(
    title: String,
    initial: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    var hour by remember { mutableIntStateOf(initial.hour) }
    var minute by remember { mutableIntStateOf(initial.minute) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stepper("−1h") { hour = (hour + 23) % 24 }
                    Text(
                        text = "%02d".format(hour),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Stepper("+1h") { hour = (hour + 1) % 24 }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Stepper("−5m") { minute = (minute + 55) % 60 }
                    Text(
                        text = "%02d".format(minute),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Stepper("+5m") { minute = (minute + 5) % 60 }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(LocalTime.of(hour, minute)) }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Stepper(label: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) { Text(label) }
}
