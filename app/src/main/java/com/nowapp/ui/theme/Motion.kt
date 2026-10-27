package com.nowapp.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.material3.MaterialTheme

/**
 * Motion policy.
 *
 * Two independent brakes, both respected everywhere:
 *  1. the system "remove animations" setting (ANIMATOR_DURATION_SCALE == 0);
 *  2. the user's own calm-mode toggle.
 * Nothing animates longer than 400 ms.
 */
data class MotionPolicy(
    val animationsEnabled: Boolean,
    val calm: Boolean,
) {
    fun durationMillis(base: Int): Int = when {
        !animationsEnabled -> 0
        calm -> (base * 0.5f).toInt()
        else -> base.coerceAtMost(MAX_DURATION_MS)
    }

    /** Snaps when motion is off, so state still updates without any tween. */
    @Composable
    fun <T> spec(baseMillis: Int): FiniteAnimationSpec<T> =
        if (durationMillis(baseMillis) == 0) snap() else tween(durationMillis = durationMillis(baseMillis))

    companion object {
        const val MAX_DURATION_MS = 400
    }
}

val LocalMotionPolicy = compositionLocalOf { MotionPolicy(animationsEnabled = true, calm = false) }

@Composable
fun rememberMotionPolicy(calmMode: Boolean): MotionPolicy {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var refresh by remember { mutableIntStateOf(0) }
    // Re-read the system animation scale whenever the app comes back to the front,
    // because the user can change it in Developer options at any time.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refresh += 1 }
    DisposableEffect(lifecycleOwner) { onDispose { } }

    val scale = remember(refresh) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
    }
    return MotionPolicy(animationsEnabled = scale > 0f, calm = calmMode)
}

/** Animated progress that obeys [LocalMotionPolicy]. Used by the morphing dial. */
@Composable
fun motionAnimatedFloat(target: Float, baseMillis: Int = 320): State<Float> {
    val policy = LocalMotionPolicy.current
    return animateFloatAsState(
        targetValue = target,
        animationSpec = policy.spec(baseMillis),
        label = "morph",
    )
}

/** Motion scheme accessor, isolated so an API change touches one line. */
@Composable
fun materialMotionSchemeLabel(): String = "expressive"
