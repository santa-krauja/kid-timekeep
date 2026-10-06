package lv.zarin.timekeep.domain.hourglass

import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Geometry of the triangular hourglass in a logical 100 x 128 box. Ported from the approved prototype. */
object BulbShape {
    const val WIDTH = 100f
    const val HEIGHT = 128f
    const val CX = 50f
    const val R = 42f
    const val NECK = 2.2f
    const val TOP0 = 8f
    const val TOP1 = 62f
    const val BOT0 = 66f
    const val BOT1 = 120f
    const val FILL0 = 12f
    const val SIG_TOP = 9f
    const val SIG_BOT = 14f

    private const val EXPONENT = 1.12f
    private const val SHOULDER = 0.07f

    /** Half of the glass width at logical height [y]; 0 outside the bulbs. */
    fun halfWidth(y: Float): Float {
        if (y < TOP0 || y > BOT1) return 0f
        if (y >= TOP1 && y <= BOT0) return NECK
        val u = if (y < TOP1) (y - TOP0) / (TOP1 - TOP0) else (BOT1 - y) / (BOT1 - BOT0)
        var f = 1f - u.pow(EXPONENT)
        if (u < SHOULDER) {
            val k = (SHOULDER - u) / SHOULDER
            f *= 0.94f + 0.06f * sqrt(1f - k * k)
        }
        return NECK + (R - NECK) * f
    }

    internal fun gauss(x: Float, s: Float): Float = exp(-(x * x) / (s * s))

    /**
     * Sand surface height at [x]. Top sand dips into a funnel (y = level + amp·g), bottom sand forms
     * a mound (y = level − amp·g). Clamped to the bulb's own half.
     */
    fun surfaceY(top: Boolean, level: Float, amp: Float, x: Float): Float =
        if (top) min(TOP1, level + amp * gauss(x - CX, SIG_TOP))
        else max(BOT0, level - amp * gauss(x - CX, SIG_BOT))
}
