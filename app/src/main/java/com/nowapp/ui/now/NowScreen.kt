package com.nowapp.ui.now

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowapp.domain.Bucket
import com.nowapp.domain.BucketRules
import com.nowapp.domain.Task
import com.nowapp.domain.TimeMath
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Intent
import android.speech.RecognizerIntent
import com.nowapp.ui.NowViewModel
import com.nowapp.ui.components.QuietTag
import com.nowapp.ui.components.MinutePickerDialog
import com.nowapp.ui.components.NowShapeCatalog
import com.nowapp.ui.components.SectionHeader
import java.time.LocalDate

/**
 * The NOW screen. It is the whole app for most days:
 *   capture row (pinned) -> NOW -> Today (max 3) -> Later (collapsed).
 *
 * Nothing on this screen is a deadline warning, a backlog count or a red badge.
 */
@Composable
fun NowScreen(
    vm: NowViewModel,
    contentPadding: PaddingValues,
    onStartFocus: (Long?, Int) -> Unit,
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()

    val nowTask = BucketRules.nowTask(tasks)
    val todayTasks = BucketRules.todayTasks(tasks)
    val laterTasks = BucketRules.laterTasks(tasks)
    val dayStart = remember { TimeMath.startOfDayMillis(LocalDate.now()) }

    var captureText by rememberSaveable { mutableStateOf("") }
    var intoToday by rememberSaveable { mutableStateOf(false) }
    var laterExpanded by rememberSaveable { mutableStateOf(false) }
    var hoverZone by remember { mutableStateOf<Bucket?>(null) }
    var estimateTarget by remember { mutableStateOf<Task?>(null) }
    val captureFocus = remember { FocusRequester() }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            captureText = spoken
            runCatching { captureFocus.requestFocus() }
        }
    }
    val zones = remember { mutableStateMapOf<Bucket, Rect>() }

    fun submit() {
        if (captureText.isBlank()) return
        vm.capture(captureText, intoToday)
        captureText = ""
        intoToday = false
    }

    val daysAway = vm.daysAway(prefs)
    val showWelcomeBack = daysAway >= 3 && prefs.welcomeBackAckDay != LocalDate.now().toString()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        CaptureField(
            value = captureText,
            onValueChange = { captureText = it },
            intoToday = intoToday,
            onToggleToday = { intoToday = !intoToday },
            onSubmit = { submit() },
            onVoice = {
                // Offline-capable system recogniser; no RECORD_AUDIO permission is
                // required because the recognition UI owns the microphone.
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the task")
                }
                runCatching { voiceLauncher.launch(intent) }
            },
            focusRequester = captureFocus,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showWelcomeBack) {
                WelcomeBackCard(
                    daysAway = daysAway,
                    todayCount = todayTasks.size + if (nowTask != null) 1 else 0,
                    onDismiss = { vm.ackWelcomeBack() },
                )
            }

            NowCard(
                task = nowTask,
                modifier = Modifier.onGloballyPositioned { zones[Bucket.NOW] = it.boundsInRoot() },
                highlighted = hoverZone == Bucket.NOW,
                onStart = { onStartFocus(nowTask?.id, 25) },
                onDone = { nowTask?.let { vm.complete(it.id) } },
                onSnooze = { nowTask?.let { vm.snooze(it.id) } },
                onBringForward = { laterTasks.firstOrNull()?.let { vm.move(it.id, Bucket.NOW) } },
                hasLaterChoices = laterTasks.isNotEmpty(),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { zones[Bucket.TODAY] = it.boundsInRoot() }
                    .then(
                        if (hoverZone == Bucket.TODAY) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                        } else {
                            Modifier
                        }
                    ),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                SectionHeader(
                    text = "Today",
                    trailing = {
                        Text(
                            text = "${todayTasks.size} of ${BucketRules.TODAY_CAP}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                todayTasks.forEach { task ->
                    TaskRow(
                        task = task,
                        zones = zones,
                        onToggle = { done -> if (done) vm.complete(task.id) else vm.uncomplete(task.id) },
                        onMove = { bucket -> vm.move(task.id, bucket) },
                        onHoverZone = { hoverZone = it },
                        onBreakdown = { vm.applyBreakdown(task.id) },
                        onStartFocus = { onStartFocus(task.id, 25) },
                        onSetEstimate = { estimateTarget = task },
                        onSetDeadline = {
                            // One tap: a hard deadline two hours out. Enough to make
                            // the Time screen and escalation meaningful without a picker.
                            vm.setDue(task.id, System.currentTimeMillis() + 2 * 60 * 60 * 1000L, hard = true)
                        },
                        onDelete = { vm.delete(task.id) },
                        meta = task.estimateMinutes?.let { "~${it}m" },
                        carriedOver = task.createdAt < dayStart,
                    )
                }
                repeat((BucketRules.TODAY_CAP - todayTasks.size).coerceAtLeast(0)) {
                    EmptySlotRow(
                        label = "Add one to today",
                        onClick = {
                            intoToday = true
                            runCatching { captureFocus.requestFocus() }
                        },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { zones[Bucket.LATER] = it.boundsInRoot() }
                    .then(
                        if (hoverZone == Bucket.LATER) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                        } else {
                            Modifier
                        }
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { laterExpanded = !laterExpanded }
                        .semantics {
                            contentDescription = if (laterExpanded) "Collapse Later" else "Expand Later"
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    // Deliberately no count badge: Later never nags.
                    Text(
                        text = "Later",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = if (laterExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (laterExpanded) {
                    if (laterTasks.isEmpty()) {
                        Text(
                            text = "Empty. That is allowed.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                    laterTasks.take(25).forEach { task ->
                        TaskRow(
                            task = task,
                            zones = zones,
                            onToggle = { done -> if (done) vm.complete(task.id) else vm.uncomplete(task.id) },
                            onMove = { bucket -> vm.move(task.id, bucket) },
                            onHoverZone = { hoverZone = it },
                            onBreakdown = { vm.applyBreakdown(task.id) },
                            onStartFocus = { onStartFocus(task.id, 25) },
                            onSetEstimate = { estimateTarget = task },
                            onSetDeadline = {
                                vm.setDue(task.id, System.currentTimeMillis() + 2 * 60 * 60 * 1000L, hard = true)
                            },
                            onDelete = { vm.delete(task.id) },
                            meta = task.estimateMinutes?.let { "~${it}m" },
                        )
                    }
                }
            }

            if (tasks.none { !it.isDone }) {
                Text(
                    text = "Nothing on the list. Add one thing above, or do nothing for now.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 24.dp),
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }

    estimateTarget?.let { target ->
        MinutePickerDialog(
            title = "How long will it take?",
            subtitle = target.title,
            initial = target.estimateMinutes,
            onDismiss = { estimateTarget = null },
            onPick = { minutes ->
                vm.setEstimate(target.id, minutes)
                estimateTarget = null
            },
            onClear = {
                vm.setEstimate(target.id, null)
                estimateTarget = null
            },
        )
    }
}

@Composable
private fun NowCard(
    task: Task?,
    modifier: Modifier,
    highlighted: Boolean,
    onStart: () -> Unit,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onBringForward: () -> Unit,
    hasLaterChoices: Boolean,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)
            .border(
                width = if (highlighted) 2.dp else 1.dp,
                color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.large,
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // A MaterialShape accent: the clover reads as "this one is the one".
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(MaterialTheme.colorScheme.primary, NowShapeCatalog.clover()),
            )
            Text(
                text = "NOW",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (task == null) {
            Text(
                text = "Nothing in Now.",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Pick one thing. Not three.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasLaterChoices) {
                    Button(onClick = onBringForward) { Text("Bring one forward") }
                }
                TextButton(onClick = onStart) { Text("Focus anyway") }
            }
        } else {
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (task.estimateMinutes != null) {
                    QuietTag("~${task.estimateMinutes}m")
                }
                if (task.dueAt != null) {
                    QuietTag(
                        "due " + TimeMath.countdown(System.currentTimeMillis(), task.dueAt).label()
                    )
                }
            }
            SplitActions(onStart = onStart, onDone = onDone, onSnooze = onSnooze)
        }
    }
}

/**
 * SplitButtonLayout, per the expressive component list: one big primary action
 * (start focus) plus one adjacent, less common action (done / snooze).
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SplitActions(onStart: () -> Unit, onDone: () -> Unit, onSnooze: () -> Unit) {
    SplitButtonLayout(
        leadingButton = {
            Button(
                onClick = onStart,
                modifier = Modifier.heightIn(min = 56.dp).fillMaxWidth(),
            ) {
                Text("Start 25 minutes", style = MaterialTheme.typography.titleMedium)
            }
        },
        trailingButton = {
            FilledTonalButton(
                onClick = onDone,
                modifier = Modifier.heightIn(min = 56.dp),
                contentPadding = PaddingValues(horizontal = 20.dp),
            ) {
                Text("Done")
            }
        },
    )
    TextButton(onClick = onSnooze) { Text("Snooze 30m") }
}

@Composable
private fun EmptySlotRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "+  $label",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WelcomeBackCard(daysAway: Long, todayCount: Int, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Welcome back.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = "It has been $daysAway days. Nothing was lost and nothing is piling up. " +
                if (todayCount == 0) {
                    "There is nothing on today, and that is fine."
                } else {
                    "Here ${if (todayCount == 1) "is the one thing" else "are the $todayCount things"} on today."
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = onDismiss) { Text("Good to be back") }
    }
}
