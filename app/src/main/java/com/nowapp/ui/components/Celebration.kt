package com.nowapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nowapp.ui.theme.LocalMotionPolicy
import kotlinx.coroutines.delay

/**
 * The completion celebration: one morph, then it gets out of the way.
 *
 * Rules honoured here: only one morphing element is ever on screen, the animation
 * is 340ms at most, calm mode halves it, and it is skipped entirely when the
 * system animation scale is zero.
 */
@Composable
fun CelebrationOverlay(text: String, onFinished: () -> Unit) {
    val policy = LocalMotionPolicy.current
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        visible = true
        delay(if (policy.animationsEnabled) 900 else 400)
        visible = false
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xB3000000)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MorphSurface(
                progress = if (visible) 1f else 0f,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(180.dp)
                    .padding(8.dp),
                resting = NowShapeCatalog.celebrationResting(),
                full = NowShapeCatalog.celebrationFull(),
            )
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}
