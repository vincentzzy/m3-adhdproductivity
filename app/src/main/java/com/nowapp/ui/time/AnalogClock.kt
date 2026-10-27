package com.nowapp.ui.time

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.LocalTime
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A plain analog clock. Time blindness is not fixed by a clever clock, but a
 * clock you can read at a glance without arithmetic is a genuine help: the hand
 * positions carry the "where am I in the day" signal with no numbers involved.
 */
@Composable
fun AnalogClock(
    time: LocalTime,
    modifier: Modifier = Modifier,
    dialColor: Color = Color(0xFF2E2E3C),
    accentColor: Color = Color(0xFFB8A1FF),
    handColor: Color = Color(0xFFECECF3),
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "Analog clock showing %02d:%02d".format(time.hour, time.minute)
        },
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(color = dialColor, radius = radius - 2f, center = center, style = Stroke(width = 4f))

        // Hour ticks: long at 12/3/6/9 so the quadrants read instantly.
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30.0) - 90.0)
            val outer = radius - 12f
            val inner = if (i % 3 == 0) radius - 32f else radius - 22f
            drawLine(
                color = if (i % 3 == 0) handColor else dialColor,
                start = Offset(center.x + cos(angle).toFloat() * inner, center.y + sin(angle).toFloat() * inner),
                end = Offset(center.x + cos(angle).toFloat() * outer, center.y + sin(angle).toFloat() * outer),
                strokeWidth = if (i % 3 == 0) 4f else 2f,
                cap = StrokeCap.Round,
            )
        }

        val minuteAngle = Math.toRadians(((time.minute * 6) - 90).toDouble())
        val hourAngle = Math.toRadians((((time.hour % 12) * 30) + (time.minute * 0.5)) - 90.0)

        drawLine(
            color = accentColor,
            start = center,
            end = Offset(
                center.x + cos(hourAngle).toFloat() * (radius * 0.48f),
                center.y + sin(hourAngle).toFloat() * (radius * 0.48f),
            ),
            strokeWidth = 10f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = handColor,
            start = center,
            end = Offset(
                center.x + cos(minuteAngle).toFloat() * (radius * 0.72f),
                center.y + sin(minuteAngle).toFloat() * (radius * 0.72f),
            ),
            strokeWidth = 6f,
            cap = StrokeCap.Round,
        )
        drawCircle(color = handColor, radius = 6f, center = center)
    }
}
