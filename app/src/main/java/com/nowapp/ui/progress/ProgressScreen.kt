package com.nowapp.ui.progress

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nowapp.domain.BucketRules
import com.nowapp.domain.DayBudgetMath
import com.nowapp.domain.Multiplier
import com.nowapp.domain.TimeMath
import com.nowapp.ui.NowViewModel
import com.nowapp.ui.components.QuietTag
import com.nowapp.ui.components.SectionHeader
import com.nowapp.ui.components.StatCard
import java.time.LocalDate

/**
 * Progress, restricted to numbers that can only go up.
 *
 * There is no graph with a falling line, no weekly comparison, no "you did worse
 * than last week". Focus minutes, things finished, closest guess and best streak
 * accumulate; a bad week simply does not appear here.
 */
@Composable
fun ProgressScreen(vm: NowViewModel, contentPadding: PaddingValues) {
    val tasks by vm.tasks.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()

    val focusMinutes = sessions.sumOf { it.actualMinutes }
    val finished = tasks.count { it.isDone }
    val multiplier = vm.multiplierFor(tasks)
    val bestMultiplier = vm.bestMultiplierFor(tasks)
    val todayDone = tasks.count {
        it.completedAt != null && it.completedAt >= TimeMath.startOfDayMillis(LocalDate.now())
    }
    val todayPlanned = BucketRules.todayTasks(tasks).size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "Everything here only goes up.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        StatCard(
            label = "Focus minutes",
            value = DayBudgetMath.formatDuration(focusMinutes),
            detail = "${sessions.size} sessions logged",
            modifier = Modifier.fillMaxWidth(),
        )

        StatCard(
            label = "Things finished",
            value = "$finished",
            detail = if (todayDone > 0) {
                "$todayDone done today — that counts as a good day."
            } else {
                "One is enough for today."
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Best time guess",
                value = bestMultiplier?.let { "×${"%.1f".format(it)}" } ?: "—",
                detail = if (bestMultiplier == null) {
                    "Shows after ${Multiplier.MIN_SAMPLES} estimates"
                } else {
                    "Closest you have ever been"
                },
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Your multiplier",
                value = multiplier?.let { "×${"%.1f".format(it)}" } ?: "—",
                detail = multiplier?.let { Multiplier.explain(it) } ?: "Not enough samples yet",
                modifier = Modifier.weight(1f),
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = "Best streak",
                value = "${streak.best}",
                detail = "Kept forever",
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Current streak",
                value = "${streak.current}",
                detail = "${streak.graceDaysRemaining} grace days left this month",
                modifier = Modifier.weight(1f),
            )
        }

        if (streak.lastDayProtected) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuietTag("protected")
                Text(
                    text = "A missed day was covered by grace. Nothing broke.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SectionHeader("Today")
            Text(
                text = if (todayPlanned == 0) {
                    "Nothing planned. That is a complete, valid day."
                } else {
                    "$todayDone of $todayPlanned done."
                },
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        androidx.compose.foundation.layout.Spacer(Modifier.height(32.dp))
    }
}
