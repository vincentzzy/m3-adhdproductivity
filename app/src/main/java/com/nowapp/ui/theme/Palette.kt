package com.nowapp.ui.theme

/**
 * One accent hue, no red anywhere.
 *
 * These are plain Long ARGB values (not Compose Colors) so a unit test can assert
 * the "zero red pixels" rule without an Android runtime.
 */
object Palette {
    const val BACKGROUND = 0xFF0B0B0FL
    const val SURFACE = 0xFF14141BL
    const val SURFACE_HIGH = 0xFF1E1E28L
    const val ACCENT = 0xFFB8A1FFL          // the single accent hue (periwinkle)
    const val ON_ACCENT = 0xFF17102BL
    const val ACCENT_SOFT = 0xFF2A2440L
    const val TEXT = 0xFFECECF3L
    const val TEXT_MUTED = 0xFF9A9AAEL
    const val OUTLINE = 0xFF2E2E3CL
    /** The colour other apps would use for errors. Here it is simply neutral. */
    const val NEUTRAL_ALERT = 0xFF9A9AAEL

    val allColors: List<Long> = listOf(
        BACKGROUND, SURFACE, SURFACE_HIGH, ACCENT, ON_ACCENT, ACCENT_SOFT,
        TEXT, TEXT_MUTED, OUTLINE, NEUTRAL_ALERT,
    )

    /** A colour is "red-ish" when its red channel dominates both other channels by a margin. */
    fun isReddish(argb: Long): Boolean {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        return r > 90 && r > g + 40 && r > b + 40
    }
}
