package com.nowapp.domain

/**
 * Estimate-vs-actual calibration.
 *
 * The personal multiplier is a derived value, never stored: it is recomputed from
 * completed tasks every time it is shown. It only appears after 10 samples so a
 * single bad estimate never becomes a verdict about the user.
 */
data class EstimateSample(val estimateMinutes: Int, val actualMinutes: Int)

object Multiplier {

    const val MIN_SAMPLES = 10

    /** Mean of actual/estimate. Null until there are enough samples to be fair. */
    fun multiplier(samples: List<EstimateSample>): Double? {
        val usable = usable(samples)
        if (usable.size < MIN_SAMPLES) return null
        return usable.sumOf { it.actualMinutes.toDouble() / it.estimateMinutes.toDouble() } / usable.size
    }

    fun usable(samples: List<EstimateSample>): List<EstimateSample> =
        samples.filter { it.estimateMinutes > 0 && it.actualMinutes > 0 }

    /** Re-expresses an estimate through the user's own multiplier, rounded to 5 minutes. */
    fun adjustedEstimate(estimateMinutes: Int, multiplier: Double?): Int {
        if (multiplier == null) return estimateMinutes
        val raw = estimateMinutes * multiplier.coerceIn(0.25, 4.0)
        return (Math.round(raw / 5.0) * 5).toInt().coerceAtLeast(5)
    }

    /**
     * The best (closest-to-1) multiplier the user has ever achieved over any
     * 10-sample window. Used on Progress as an only-goes-up stat: it can improve
     * but it is never framed as having got worse.
     */
    fun bestMultiplier(samples: List<EstimateSample>): Double? {
        val usable = usable(samples)
        if (usable.size < MIN_SAMPLES) return null
        var best: Double? = null
        for (end in MIN_SAMPLES..usable.size) {
            val window = usable.subList(end - MIN_SAMPLES, end)
            val mean = window.sumOf { it.actualMinutes.toDouble() / it.estimateMinutes.toDouble() } / window.size
            val distance = kotlin.math.abs(mean - 1.0)
            if (best == null || distance < kotlin.math.abs(best - 1.0)) best = mean
        }
        return best
    }

    /** One-line, non-judgemental explanation of the multiplier. */
    fun explain(multiplier: Double): String = when {
        multiplier >= 1.5 -> "Your plans run about ${"%.1f".format(multiplier)}× long. We'll pad estimates."
        multiplier >= 1.15 -> "You usually need ${"%.1f".format(multiplier)}× the time you guess."
        multiplier > 0.85 -> "Your time guesses are close to spot on."
        else -> "You finish faster than you plan. Estimates get trimmed."
    }
}
