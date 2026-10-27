package com.taskd.app.ui

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taskd.app.data.TaskEntity
import com.taskd.app.data.TaskRepository
import com.taskd.app.domain.Rules
import com.taskd.app.platform.Notifications
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val Ink = Color(0xFF11121A)
private val Panel = Color(0xFF20212B)
private val Accent = Color(0xFFB9A7FF)
private val Soft = Color(0xFFE8E4F4)

@Composable
fun TaskdApp(startCapture: Boolean = false, sharedText: String? = null, startFocus: Boolean = false) {
    val context = LocalContext.current
    val repo = remember { TaskRepository(context) }
    val tasks by repo.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    val completedCount by repo.completedCount.collectAsStateWithLifecycle(initialValue = 0)
    val calm by repo.calmMode.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()
    var tab by remember { mutableIntStateOf(0) }
    var capture by remember { mutableStateOf("") }
    var laterOpen by remember { mutableStateOf(false) }
    var focusOpen by remember { mutableStateOf(startFocus) }
    var focusMinutes by remember { mutableIntStateOf(25) }
    var timerStarted by remember { mutableStateOf(false) }
    var voiceError by remember { mutableStateOf(false) }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) capture = spoken
    }

    LaunchedEffect(startCapture, sharedText) {
        capture = sharedText.orEmpty()
        if (startCapture) tab = 0
    }
    LaunchedEffect(tasks) {
        val today = tasks.filter { it.bucket == "NOW" || it.bucket == "TODAY" }
        Notifications.update(context, today.size, tasks.firstOrNull { it.bucket == "NOW" }?.title.orEmpty(), tasks.firstOrNull { it.bucket == "NOW" }?.id)
    }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, onPrimary = Ink, secondary = Color(0xFF9ED9CD), background = Ink,
        surface = Panel, onSurface = Soft, onBackground = Soft, surfaceVariant = Color(0xFF30313C),
        onSurfaceVariant = Color(0xFFC4C0D1), error = Color(0xFFD7C8FF)
    )) {
        Scaffold(containerColor = Ink, bottomBar = {
            NavigationBar(containerColor = Panel, tonalElevation = 0.dp) {
                listOf("Now", "Time", "Progress").forEachIndexed { index, label ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index },
                        icon = { Text(listOf("●", "◷", "↗")[index], fontSize = 20.sp, color = if (tab == index) Accent else Soft) },
                        label = { Text(label) })
                }
            }
        }) { padding ->
            when (tab) {
                0 -> NowScreen(tasks, capture, { capture = it }, { title ->
                    scope.launch { repo.add(title); capture = "" }
                }, { task -> scope.launch { repo.done(task.id) } }, { task, bucket -> scope.launch { repo.move(task, bucket) } }, laterOpen, { laterOpen = it }, { focusOpen = true }, {
                    val speechIntent = Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Say one thing to remember")
                    }
                    runCatching { speechLauncher.launch(speechIntent) }.onFailure { voiceError = true }
                }, padding)
                1 -> TimeScreen(tasks, calm, { scope.launch { repo.setCalm(it) } }, padding)
                else -> ProgressScreen(completedCount, { focusOpen = true }, padding)
            }
        }
        if (focusOpen) AlertDialog(
            onDismissRequest = { focusOpen = false }, containerColor = Panel,
            title = { Text(if (timerStarted) "Focus is yours" else "Focus", fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val corner by animateDpAsState(if (timerStarted) 36.dp else 88.dp, animationSpec = tween(if (calm) 0 else 320), label = "focus shape")
                    Box(Modifier.size(150.dp).clip(RoundedCornerShape(corner)).background(Accent).semantics { contentDescription = "Focus timer dial, $focusMinutes minutes" }, contentAlignment = Alignment.Center) {
                        Text("$focusMinutes", color = Ink, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(if (timerStarted) "One thing at a time. You can stop whenever." else "Choose a kind-sized block.")
                    Slider(value = focusMinutes.toFloat(), onValueChange = { focusMinutes = it.toInt() }, valueRange = 5f..90f, steps = 16, enabled = !timerStarted,
                        modifier = Modifier.semantics { contentDescription = "Focus duration" })
                    TextButton(onClick = { if (timerStarted) BrownNoise.stop(context) else BrownNoise.start() }) { Text(if (timerStarted) "Stop brown noise" else "Brown noise") }
                }
            }, confirmButton = { TextButton(onClick = { timerStarted = !timerStarted }) { Text(if (timerStarted) "Pause" else "Start focus") } },
            dismissButton = { TextButton(onClick = { timerStarted = false; BrownNoise.stop(context); focusOpen = false }) { Text("Close") } }
        )
        if (voiceError) AlertDialog(onDismissRequest = { voiceError = false }, title = { Text("Voice capture") }, text = { Text("Voice capture needs speech recognition provided by your device. You can type the task here instead.") }, confirmButton = { TextButton(onClick = { voiceError = false }) { Text("OK") } })
    }
}

@Composable
private fun NowScreen(tasks: List<TaskEntity>, capture: String, onCaptureChange: (String) -> Unit, add: (String) -> Unit,
                     done: (TaskEntity) -> Unit, move: (TaskEntity, String) -> Unit, laterOpen: Boolean, onLater: (Boolean) -> Unit,
                     onFocus: () -> Unit, onVoice: () -> Unit, padding: PaddingValues) {
    val now = tasks.filter { it.bucket == "NOW" }.firstOrNull()
    val today = tasks.filter { it.bucket == "TODAY" }
    val later = tasks.filter { it.bucket == "LATER" }
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Text("TASKD", color = Accent, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = capture, onValueChange = onCaptureChange, modifier = Modifier.weight(1f).heightIn(min = 58.dp),
                placeholder = { Text("Add one thing…") }, singleLine = true, shape = RoundedCornerShape(22.dp),
                trailingIcon = { IconButton(onClick = onVoice, modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp)) { Icon(Icons.Default.Mic, "Capture by voice") } })
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = { add(capture) }, enabled = capture.isNotBlank(), modifier = Modifier.size(52.dp).semantics { contentDescription = "Add task" }) { Icon(Icons.Default.Add, "Add task") }
        }
        Spacer(Modifier.height(20.dp))
        if (tasks.isEmpty()) {
            Text("A small start is still a start.", color = Soft, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text("Catch a thought above. It can go straight to NOW.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text("NOW", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            if (now != null) {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(now.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onFocus, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Icon(Icons.Default.PlayArrow, null); Spacer(Modifier.width(6.dp)); Text("Start") }
                            OutlinedButton(onClick = { done(now) }, modifier = Modifier.heightIn(min = 52.dp).semantics { contentDescription = "Mark ${now.title} done" }) { Icon(Icons.Default.Check, "Done") }
                        }
                        BucketPicker(now, move)
                    }
                }
            } else {
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Nothing in NOW. That's okay.", Modifier.weight(1f), color = Soft)
                        TextButton(onClick = { today.firstOrNull()?.let { move(it, "NOW") } }) { Text("Start one") }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) { Text("TODAY", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge); Spacer(Modifier.width(8.dp)); Text("${today.size}/3", color = Accent) }
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 8.dp)) {
                items(today, key = { it.id }) { task -> TaskRow(task, done, move) }
                item {
                    TextButton(onClick = { onLater(!laterOpen) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).semantics { contentDescription = if (laterOpen) "Collapse Later" else "Expand Later" }) {
                        Icon(Icons.Default.MoreVert, null); Spacer(Modifier.width(6.dp)); Text(if (laterOpen) "Later · quieter" else "Later")
                    }
                }
                if (laterOpen) items(later, key = { it.id }) { TaskRow(it, done, move) }
                if (today.isNotEmpty()) item { Text("One done is a good day.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp)) }
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, done: (TaskEntity) -> Unit, move: (TaskEntity, String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = false, onCheckedChange = { done(task) }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Complete ${task.title}" })
            Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Box {
                IconButton(onClick = { menu = true }, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = "Move ${task.title} to another bucket" }) { Icon(Icons.Default.Tune, "Move task") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    listOf("NOW", "TODAY", "LATER").forEach { target -> DropdownMenuItem(text = { Text("Move to $target") }, onClick = { move(task, target); menu = false }) }
                }
            }
        }
    }
}

@Composable
private fun BucketPicker(task: TaskEntity, move: (TaskEntity, String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) { Text("Move from NOW") }
        DropdownMenu(expanded, { expanded = false }) {
            listOf("TODAY", "LATER").forEach { target -> DropdownMenuItem(text = { Text("Move to $target") }, onClick = { move(task, target); expanded = false }) }
        }
    }
}

@Composable
private fun TimeScreen(tasks: List<TaskEntity>, calm: Boolean, setCalm: (Boolean) -> Unit, padding: PaddingValues) {
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(60_000); tick = System.currentTimeMillis() } }
    val planned = tasks.filter { it.bucket == "TODAY" || it.bucket == "NOW" }.sumOf { it.estimateMinutes ?: 25 }
    val budget = Rules.dayBudgetPercent(16 * 60, planned)
    val time = remember(tick) { java.text.SimpleDateFormat("h:mm", java.util.Locale.getDefault()).format(java.util.Date(tick)) }
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("TIME", Modifier.weight(1f), color = Accent, fontWeight = FontWeight.Bold); Text("Calm mode", color = Soft); Switch(calm, setCalm, modifier = Modifier.semantics { contentDescription = "Calm mode" }) }
        Spacer(Modifier.height(24.dp))
        ClockFace(time)
        Spacer(Modifier.height(24.dp))
        Text("No next deadline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("There's time for one small thing.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(30.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("TODAY'S BUDGET", color = Accent, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp)); LinearProgressIndicator(progress = { budget / 100f }, modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape), color = Accent, trackColor = Color(0xFF3A3945))
                Text("About $planned minutes planned · 16 waking hours", Modifier.padding(top = 8.dp), color = Soft)
            }
        }
        Spacer(Modifier.height(18.dp))
        Text("Time since last break", style = MaterialTheme.typography.titleMedium)
        Text("Not tracked yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Text("Time is a guide, not a grade.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ClockFace(time: String) {
    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().semantics { contentDescription = "Analog clock showing $time" }) {
            val center = this.center
            drawCircle(Panel, radius = size.minDimension / 2)
            drawCircle(Accent.copy(alpha = .45f), radius = size.minDimension / 2 - 4.dp.toPx(), style = Stroke(2.dp.toPx()))
            for (i in 0 until 12) {
                val a = (i * 30 - 90) * Math.PI / 180
                val r1 = size.minDimension * .39f; val r2 = size.minDimension * .44f
                drawLine(Soft.copy(alpha = .7f), androidx.compose.ui.geometry.Offset(center.x + cos(a).toFloat() * r1, center.y + sin(a).toFloat() * r1), androidx.compose.ui.geometry.Offset(center.x + cos(a).toFloat() * r2, center.y + sin(a).toFloat() * r2), 3.dp.toPx(), cap = StrokeCap.Round)
            }
            drawLine(Accent, center, androidx.compose.ui.geometry.Offset(center.x, center.y - size.height * .25f), 5.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Soft, center, androidx.compose.ui.geometry.Offset(center.x + size.width * .19f, center.y), 4.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(Accent, 6.dp.toPx(), center)
        }
        Text(time, Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp), color = Soft, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ProgressScreen(doneCount: Int, onFocus: () -> Unit, padding: PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(24.dp)) {
        Text("PROGRESS", color = Accent, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text("Only what you've built. No lost streaks.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        StatCard("Focus minutes", "Not tracked yet", "Focus session logging is not wired yet")
        StatCard("Tasks done", "$doneCount", "One is a good day")
        StatCard("Estimate accuracy", "Learning", "Your personal timing appears after 10 samples")
        Spacer(Modifier.height(16.dp))
        Button(onClick = onFocus, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Start a focus block") }
        Spacer(Modifier.weight(1f))
        Text("Your best can only grow.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun StatCard(label: String, value: String, helper: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp), colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(18.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text(helper, color = Accent) }
    }
}

/** Streaming procedural brown noise: low-cost, no bundled sound file or network access. */
private object BrownNoise {
    @Volatile private var track: AudioTrack? = null
    fun start() {
        stop()
        val rate = 22050
        val size = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT).coerceAtLeast(4096)
        val audio = AudioTrack(AudioManager.STREAM_MUSIC, rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, size, AudioTrack.MODE_STREAM)
        track = audio
        Thread {
            var brown = 0.0
            val buffer = ShortArray(1024)
            audio.play()
            while (track === audio) {
                for (i in buffer.indices) { brown = (brown + (Math.random() * 2 - 1) * .02).coerceIn(-1.0, 1.0); buffer[i] = (brown * 9000).toInt().toShort() }
                audio.write(buffer, 0, buffer.size)
            }
            audio.release()
        }.apply { name = "taskd-brown-noise"; isDaemon = true; start() }
    }
    fun stop(context: Context? = null) { track?.let { runCatching { it.pause(); it.flush(); it.release() } }; track = null }
}
