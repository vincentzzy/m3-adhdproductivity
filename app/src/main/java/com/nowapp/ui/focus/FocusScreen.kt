package com.nowapp.ui.focus

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowapp.platform.BrownNoiseEngine
import com.nowapp.ui.NowViewModel
import com.nowapp.ui.components.MorphSurface
import com.nowapp.ui.components.NowShapeCatalog
import kotlinx.coroutines.delay

/**
 * Focus mode: a timer you can actually see the shape of, plus noise.
 *
 * The dial is a live Morph between two nine-sided polygons whose progress is the
 * session progress — the one intentionally novel element in the app, and the
 * reason opening it again tomorrow feels slightly different.
 */
@Composable
fun FocusScreen(
    vm: NowViewModel,
    taskId: Long?,
    initialMinutes: Int,
    onExit: () -> Unit,
) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val task = tasks.firstOrNull { it.id == taskId }

    var plannedMinutes by remember { mutableIntStateOf(initialMinutes.coerceIn(5, 120)) }
    var running by remember { mutableStateOf(false) }
    var startedAtElapsed by remember { mutableStateOf<Long?>(null) }
    var accumulatedSeconds by remember { mutableIntStateOf(0) }
    var recorded by remember { mutableStateOf(false) }
    var noiseOn by remember { mutableStateOf(false) }
    val sessionStartWallClock = remember { System.currentTimeMillis() }
    val noiseEngine = remember { BrownNoiseEngine() }

    // Elapsed time is derived from the monotonic clock, so backgrounding the app
    // cannot make the timer drift. produceState re-reads it twice a second.
    val liveSeconds by produceState(
        initialValue = accumulatedSeconds,
        running,
        accumulatedSeconds,
        startedAtElapsed,
    ) {
        val start = startedAtElapsed
        if (running && start != null) {
            while (running) {
                value = accumulatedSeconds + ((SystemClock.elapsedRealtime() - start) / 1000L).toInt()
                delay(500)
            }
        } else {
            value = accumulatedSeconds
        }
    }
    val plannedSeconds = plannedMinutes * 60
    val progress = if (plannedSeconds == 0) 0f else (liveSeconds.toFloat() / plannedSeconds).coerceIn(0f, 1f)
    val remaining = (plannedSeconds - liveSeconds).coerceAtLeast(0)

    // Reaching the end stops the timer by itself; nothing pops up and nothing shouts.
    LaunchedEffect(liveSeconds, plannedSeconds) {
        if (liveSeconds >= plannedSeconds && running) {
            running = false
            startedAtElapsed?.let { start ->
                accumulatedSeconds += ((SystemClock.elapsedRealtime() - start) / 1000L).toInt()
            }
            startedAtElapsed = null
        }
    }

    fun pause() {
        if (!running) return
        startedAtElapsed?.let { start ->
            accumulatedSeconds += ((SystemClock.elapsedRealtime() - start) / 1000L).toInt()
        }
        startedAtElapsed = null
        running = false
    }

    fun start() {
        if (running) return
        startedAtElapsed = SystemClock.elapsedRealtime()
        running = true
    }

    fun recordIfNeeded() {
        if (recorded) return
        val minutes = (liveSeconds / 60).coerceAtLeast(if (liveSeconds > 30) 1 else 0)
        if (minutes > 0) {
            vm.recordFocus(
                taskId = taskId,
                plannedMinutes = plannedMinutes,
                actualMinutes = minutes,
                startedAt = sessionStartWallClock,
                endedAt = System.currentTimeMillis(),
            )
            recorded = true
        }
    }

    DisposableEffect(Unit) {
        onDispose { noiseEngine.release() }
    }

    LaunchedEffect(noiseOn) {
        if (noiseOn) noiseEngine.start() else noiseEngine.stop()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Focus",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = task?.title ?: "Free focus",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = {
                recordIfNeeded()
                onExit()
            }) {
                Icon(Icons.Default.Close, contentDescription = "Leave focus")
            }
        }

        Box(modifier = Modifier.size(300.dp), contentAlignment = Alignment.Center) {
            MorphSurface(
                progress = progress,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxSize(),
                resting = NowShapeCatalog.dialResting(),
                full = NowShapeCatalog.dialFull(),
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatMmSs(remaining),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = if (running) "running" else "paused",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (liveSeconds == 0 && !running) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(10, 25, 45, 60).forEach { minutes ->
                    FilterChip(
                        selected = plannedMinutes == minutes,
                        onClick = { plannedMinutes = minutes },
                        label = { Text("${minutes}m") },
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = { if (running) pause() else start() },
                modifier = Modifier.size(width = 160.dp, height = 56.dp),
            ) {
                Text(if (running) "Pause" else "Start")
            }
            FilledTonalButton(
                onClick = { noiseOn = !noiseOn },
                modifier = Modifier.size(width = 140.dp, height = 56.dp),
            ) {
                Text(if (noiseOn) "Noise on" else "Brown noise")
            }
        }

        TextButton(onClick = {
            recordIfNeeded()
            onExit()
        }) {
            Text("Finish and log it")
        }

        Text(
            text = "Leaving this screen stops the noise. The minutes you did are still logged.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun formatMmSs(totalSeconds: Int): String {
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
