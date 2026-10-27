package com.nowapp.ui.now

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.nowapp.domain.Bucket
import com.nowapp.domain.Task
import com.nowapp.ui.components.BigCheckbox
import com.nowapp.ui.components.QuietTag
import com.nowapp.ui.components.TaskTitle
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * One task row. Two ways to move it between buckets:
 *  - drag it vertically (a long, deliberate gesture, for the days motor control works);
 *  - the overflow menu (one tap, and the only route TalkBack can use reliably).
 *
 * Both paths go through the same [onMove] callback, so the hard caps are enforced
 * in exactly one place.
 */
@Composable
fun TaskRow(
    task: Task,
    zones: Map<Bucket, Rect>,
    onToggle: (Boolean) -> Unit,
    onMove: (Bucket) -> Unit,
    onHoverZone: (Bucket?) -> Unit,
    onBreakdown: () -> Unit,
    onStartFocus: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    meta: String? = null,
    carriedOver: Boolean = false,
) {
    var expanded by remember { mutableStateOf(false) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    var dragging by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { bounds = it.boundsInRoot() }
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .zIndex(if (dragging) 2f else 0f)
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    dragging = true
                    offsetY += delta
                    val centerY = bounds.top + bounds.height / 2f + offsetY
                    onHoverZone(nearestZone(zones, centerY))
                },
                onDragStopped = {
                    val centerY = bounds.top + bounds.height / 2f + offsetY
                    val target = nearestZone(zones, centerY)
                    offsetY = 0f
                    dragging = false
                    onHoverZone(null)
                    if (target != null && target != task.bucket) onMove(target)
                },
            )
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BigCheckbox(
            checked = task.isDone,
            onCheckedChange = { onToggle(it) },
            contentDescription = if (task.isDone) "Mark ${task.title} as not done" else "Mark ${task.title} as done",
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            TaskTitle(text = task.title, done = task.isDone)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (carriedOver) QuietTag("carried over")
                if (meta != null) {
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = "More options for ${task.title}" },
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (task.bucket != Bucket.NOW) {
                DropdownMenuItem(text = { Text("Make it Now") }, onClick = { expanded = false; onMove(Bucket.NOW) })
            }
            if (task.bucket != Bucket.TODAY) {
                DropdownMenuItem(text = { Text("Put in Today") }, onClick = { expanded = false; onMove(Bucket.TODAY) })
            }
            if (task.bucket != Bucket.LATER) {
                DropdownMenuItem(text = { Text("Put in Later") }, onClick = { expanded = false; onMove(Bucket.LATER) })
            }
            DropdownMenuItem(text = { Text("Break it down") }, onClick = { expanded = false; onBreakdown() })
            DropdownMenuItem(text = { Text("25 minute focus") }, onClick = { expanded = false; onStartFocus() })
            DropdownMenuItem(text = { Text("Delete") }, onClick = { expanded = false; onDelete() })
        }
    }
}

/** The bucket whose drop zone centre is closest to the dragged row's centre. */
private fun nearestZone(zones: Map<Bucket, Rect>, y: Float): Bucket? =
    zones.minByOrNull { (_, rect) -> abs(rect.center.y - y) }?.key
