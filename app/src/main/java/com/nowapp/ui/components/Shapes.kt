package com.nowapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.nowapp.ui.theme.LocalMotionPolicy
import kotlin.math.min

/**
 * Shape language for the app.
 *
 * Everything expressive and opt-in lives in this one file, so an experimental
 * API change is a one-file fix. Two mechanisms are used:
 *
 *  - [MaterialShapes] polygons as container clips and accents (the cookie, clover,
 *    pill and burst shapes from Material 3 Expressive);
 *  - [Morph] from androidx.graphics.shapes for real shape morphing on the focus
 *    dial and the completion celebration.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
object NowShapeCatalog {

    /** Small accent used behind the NOW label. */
    @Composable
    fun clover(): Shape = MaterialShapes.Clover4Leaf.toShape()

    @Composable
    fun cookie(): Shape = MaterialShapes.Cookie9Sided.toShape()

    @Composable
    fun pill(): Shape = MaterialShapes.Pill.toShape()

    @Composable
    fun burst(): Shape = MaterialShapes.SoftBurst.toShape()

    /** Dial polygons: same vertex count, so the morph is always well formed. */
    fun dialResting(): RoundedPolygon =
        RoundedPolygon.star(numVertices = 9, innerRadius = 0.70f, rounding = CornerRounding(0.30f))

    fun dialFull(): RoundedPolygon =
        RoundedPolygon.star(numVertices = 9, innerRadius = 0.93f, rounding = CornerRounding(0.45f))

    fun celebrationResting(): RoundedPolygon =
        RoundedPolygon.star(numVertices = 12, innerRadius = 0.55f, rounding = CornerRounding(0.20f))

    fun celebrationFull(): RoundedPolygon =
        RoundedPolygon.star(numVertices = 12, innerRadius = 0.95f, rounding = CornerRounding(0.40f))
}

/**
 * Draws a morphing polygon that fills [modifier], with [content] laid out on top.
 *
 * [progress] 0 = resting shape, 1 = full shape. The animation obeys the app's
 * motion policy: calm mode halves it, and "remove animations" makes it instant.
 */
@Composable
fun MorphSurface(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    resting: RoundedPolygon = NowShapeCatalog.dialResting(),
    full: RoundedPolygon = NowShapeCatalog.dialFull(),
    scaleToFill: Float = 0.94f,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val policy = LocalMotionPolicy.current
    val animated = animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = policy.spec(340),
        label = "shape-morph",
    )
    val morph = remember(resting, full) { Morph(resting, full) }
    val paint = remember { Paint().apply { isAntiAlias = true } }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val androidPath = android.graphics.Path()
            morph.toPath(animated.value, androidPath)
            val scale = min(size.width, size.height) / 2f * scaleToFill
            withTransform({
                translate(size.width / 2f, size.height / 2f)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                drawIntoCanvas { canvas ->
                    val frameworkPaint = paint.asFrameworkPaint()
                    frameworkPaint.color = color.toArgbCompat()
                    canvas.nativeCanvas.drawPath(androidPath, frameworkPaint)
                }
            }
        }
        content()
    }
}

/** Compose Color -> ARGB int without depending on newer helper names. */
private fun Color.toArgbCompat(): Int =
    android.graphics.Color.argb((alpha * 255).toInt(), (red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())
