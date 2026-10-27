package com.nowapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A six-chip minute picker. Used for two things only: "how long will this take?"
 * and "how long did that take?".
 *
 * Both are optional. Every chip is one tap and 44dp tall, and there is always a
 * way out that costs nothing (Skip / Not now).
 */
@Composable
fun MinutePickerDialog(
    title: String,
    subtitle: String? = null,
    initial: Int? = null,
    options: List<Int> = listOf(5, 10, 15, 25, 45, 60),
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                options.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { minutes ->
                            val selected = initial == minutes
                            FilledTonalButton(
                                onClick = { onPick(minutes) },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = "${minutes}m",
                                    style = MaterialTheme.typography.labelLarge,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Skip") }
        },
        dismissButton = {
            if (onClear != null) {
                TextButton(onClick = onClear) { Text("Clear") }
            }
        },
    )
}
