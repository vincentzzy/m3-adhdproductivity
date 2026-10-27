package com.nowapp.domain

import com.nowapp.ui.theme.Palette
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Guards the "zero red pixels anywhere" rule at the level of the palette itself. */
class PaletteTest {

    @Test
    fun `no colour in the palette is reddish`() {
        Palette.allColors.forEach { colour ->
            assertFalse(
                "Colour ${colour.toString(16)} is too red for this app",
                Palette.isReddish(colour),
            )
        }
    }

    @Test
    fun `the detector does catch a genuinely red colour`() {
        assertTrue(Palette.isReddish(0xFFE53935))
    }

    @Test
    fun `the accent has enough contrast against the background`() {
        fun luminance(argb: Long): Double {
            fun channel(value: Long): Double {
                val s = value / 255.0
                return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
            }
            val r = channel((argb shr 16) and 0xFF)
            val g = channel((argb shr 8) and 0xFF)
            val b = channel(argb and 0xFF)
            return 0.2126 * r + 0.7152 * g + 0.0722 * b
        }
        val contrast = (luminance(Palette.ACCENT) + 0.05) / (luminance(Palette.BACKGROUND) + 0.05)
        assertTrue("contrast was $contrast", contrast >= 4.5)
    }
}
