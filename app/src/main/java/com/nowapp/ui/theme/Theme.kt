package com.nowapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Dark-first, one accent hue, and a ColorScheme whose "error" role is a neutral
 * grey. Overdue tasks therefore cannot render as red even by accident: there is
 * no red in the palette to render.
 */
private fun expressiveDarkScheme() = darkColorScheme(
    primary = Color(Palette.ACCENT),
    onPrimary = Color(Palette.ON_ACCENT),
    primaryContainer = Color(Palette.ACCENT_SOFT),
    onPrimaryContainer = Color(Palette.TEXT),
    inversePrimary = Color(Palette.ACCENT),
    secondary = Color(Palette.ACCENT),
    onSecondary = Color(Palette.ON_ACCENT),
    secondaryContainer = Color(Palette.ACCENT_SOFT),
    onSecondaryContainer = Color(Palette.TEXT),
    tertiary = Color(Palette.ACCENT),
    onTertiary = Color(Palette.ON_ACCENT),
    tertiaryContainer = Color(Palette.ACCENT_SOFT),
    onTertiaryContainer = Color(Palette.TEXT),
    background = Color(Palette.BACKGROUND),
    onBackground = Color(Palette.TEXT),
    surface = Color(Palette.BACKGROUND),
    onSurface = Color(Palette.TEXT),
    surfaceVariant = Color(Palette.SURFACE),
    onSurfaceVariant = Color(Palette.TEXT_MUTED),
    surfaceTint = Color(Palette.ACCENT),
    surfaceContainerLowest = Color(Palette.BACKGROUND),
    surfaceContainerLow = Color(Palette.SURFACE),
    surfaceContainer = Color(Palette.SURFACE),
    surfaceContainerHigh = Color(Palette.SURFACE_HIGH),
    surfaceContainerHighest = Color(Palette.SURFACE_HIGH),
    inverseSurface = Color(Palette.TEXT),
    inverseOnSurface = Color(Palette.BACKGROUND),
    outline = Color(Palette.OUTLINE),
    outlineVariant = Color(Palette.OUTLINE),
    // Deliberately neutral: the app has no error colour.
    error = Color(Palette.NEUTRAL_ALERT),
    onError = Color(Palette.BACKGROUND),
    errorContainer = Color(Palette.SURFACE_HIGH),
    onErrorContainer = Color(Palette.TEXT),
    scrim = Color(0xFF000000),
)

private val NowShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp),
)

/**
 * The single theme entry point.
 *
 * Note the substituted API: material3 1.5.0-beta02 exposes MaterialExpressiveTheme
 * but NOT `dynamicExpressiveDarkColorScheme()`, so the expressive intent (dark,
 * vibrant, high contrast, one hue) is hand-built instead of being derived from the
 * wallpaper. That also keeps one accent hue guaranteed. See DECISIONS.md.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NowTheme(calmMode: Boolean = false, content: @Composable () -> Unit) {
    val motion = rememberMotionPolicy(calmMode)
    CompositionLocalProvider(LocalMotionPolicy provides motion) {
        MaterialExpressiveTheme(
            colorScheme = expressiveDarkScheme(),
            motionScheme = if (motion.animationsEnabled && !motion.calm) {
                MotionScheme.expressive()
            } else {
                MotionScheme.standard()
            },
            shapes = NowShapes,
            typography = Typography(),
            content = content,
        )
    }
}

/** Convenience for headings: heavier weight, no new type styles. */
@Composable
fun headingStyle() = MaterialTheme.typography.headlineMedium.copy(
    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
)
