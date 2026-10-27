package com.nowapp.ui.time

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowapp.domain.BucketRules
import com.nowapp.domain.DayBudgetMath
import com.nowapp.domain.TimeMath
import com.nowapp.ui.NowViewModel
import com.nowapp.ui.components.QuietTag
import com.nowapp.ui.components.SectionHeader
import kotlinx.coroutines.delay
import java.time.LocalTime

/**
 * The Time screen: analog clock, countdown to the next real deadline, a day
 * budget bar, and time since the last break.
 *
 * Everything here is a measurement, not a judgement. Nothing turns red.
 */
@Composable
fun TimeScreen(vm: NowViewModel, contentPadding: PaddingValues, onStartFocus: () -> Unit) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val prefs by vm.prefs.collectAsStateWithLifecycle()

    var now by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalTime.now()
            delay(1_000)
        }
    }

    val todayTasks = BucketRules.todayTasks(tasks)
    val nowTask = BucketRules.nowTask(tasks)
    val estimates = todayTasks.mapNotNull { it.estimateMinutes } + listOfNotNull(nowTask?.estimateMinutes)
    val awakeMinutes = DayBudgetMath.minutesBetween(prefs.awakeStart, prefs.awakeEnd).coerceAtLeast(60)
    val budget = DayBudgetMath.compute(estimates, awakeMinutes)
    val elapsedFraction = DayBudgetMath.elapsedFraction(now, prefs.awakeStart, prefs.awakeEnd)

    val nextDeadline = TimeMath.nextDeadline(tasks)
    val countdown = nextDeadline?.let { TimeMath.countdown(System.currentTimeMillis(), it) }

    val sinceBreakMinutes = if (prefs.lastBreakAt == 0L) {
        null
    } else {
        ((System.currentTimeMillis() - prefs.lastBreakAt) / 60_000L).toInt()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = "%02d:%02d".format(now.hour, now.minute),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            AnalogClock(
                time = now,
                modifier = Modifier.size(220.dp),
                dialColor = MaterialTheme.colorScheme.outline,
                accentColor = MaterialTheme.colorScheme.primary,
                handColor = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Next deadline")
            if (countdown == null) {
                Text(
                    text = "Nothing has a deadline. That is a fine way to live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = countdown.label(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (countdown.isPast) QuietTag("carried over")
                }
                Text(
                    text = tasks.firstOrNull { it.dueAt == nextDeadline }?.title.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Day budget")
            LinearProgressIndicator(
                progress = { budget.usedFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = buildString {
                    append("${DayBudgetMath.formatDuration(budget.plannedMinutes)} planned of ")
                    append(DayBudgetMath.formatDuration(budget.awakeMinutes))
                    append(". ")
                    if (budget.isOverBudget) {
                        append("More than the day holds — Later can take something.")
                    } else {
                        append("${DayBudgetMath.formatDuration(budget.remainingMinutes)} unclaimed.")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "The day itself is ${(elapsedFraction * 100).toInt()}% spent.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader("Since your last break")
            Text(
                text = sinceBreakMinutes?.let { DayBudgetMath.formatDuration(it) } ?: "Not tracked yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (sinceBreakMinutes != null && sinceBreakMinutes >= 90) {
                Text(
                    text = "Over 90 minutes. A two minute stand-up would count.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.markBreak() }) { Text("I moved") }
                Button(onClick = onStartFocus) { Text("Start focus") }
            }
        }

        Box(modifier = Modifier.height(32.dp))
    }
}
