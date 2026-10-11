package lv.zarin.timekeep.domain.hourglass

import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt
import lv.zarin.timekeep.domain.hourglass.BulbShape.BOT0
import lv.zarin.timekeep.domain.hourglass.BulbShape.BOT1
import lv.zarin.timekeep.domain.hourglass.BulbShape.FILL0
import lv.zarin.timekeep.domain.hourglass.BulbShape.SIG_BOT
import lv.zarin.timekeep.domain.hourglass.BulbShape.SIG_TOP
import lv.zarin.timekeep.domain.hourglass.BulbShape.TOP0
import lv.zarin.timekeep.domain.hourglass.BulbShape.TOP1
import lv.zarin.timekeep.domain.hourglass.BulbShape.halfWidth

data class SandLevels(val topLevel: Float?, val dip: Float, val bottomLevel: Float?, val mound: Float)

object SandLevelSolver {
    private const val DY = 0.25f
    private const val STEPS = 32
    private const val CACHE_STEPS = 4000

    val totalSand: Float = topArea(FILL0, 0f)

    // Benign race: entries are deterministic, so concurrent writers at worst compute one twice.
    private val cache = arrayOfNulls<SandLevels>(CACHE_STEPS + 1)

    fun topArea(level: Float, dip: Float): Float {
        var a = 0f
        var y = TOP0 + DY / 2
        while (y < TOP1) {
            val w = halfWidth(y)
            val t = y - level
            val width = when {
                t <= 0f -> 2 * w
                dip < 1e-6f || t >= dip -> 2 * w
                else -> 2 * max(0f, w - SIG_TOP * sqrt(-ln(t / dip)))
            }
            // Unlike the prototype, the row straddling the surface counts fractionally so the area is
            // continuous in `level`; with whole rows the level can step backwards for tiny dips.
            val cover = (t / DY + 0.5f).coerceIn(0f, 1f)
            a += width * cover * DY
            y += DY
        }
        return a
    }

    fun bottomArea(level: Float, mound: Float): Float {
        var a = 0f
        var y = BOT0 + DY / 2
        while (y < BOT1) {
            val w = halfWidth(y)
            val t = level - y
            val width = when {
                t <= 0f -> 2 * w
                t >= mound -> 0f
                else -> 2 * min(w, SIG_BOT * sqrt(-ln(t / mound)))
            }
            a += width * DY
            y += DY
        }
        return a
    }

    private inline fun solve(target: Float, lo0: Float, hi0: Float, area: (Float) -> Float): Float {
        var lo = lo0
        var hi = hi0
        repeat(STEPS) {
            val m = (lo + hi) / 2
            if (area(m) > target) lo = m else hi = m
        }
        return (lo + hi) / 2
    }

    fun levels(progress: Float): SandLevels {
        val k = round(progress.coerceIn(0f, 1f) * CACHE_STEPS).toInt()
        cache[k]?.let { return it }
        val p = k.toFloat() / CACHE_STEPS
        val dip = 7f * min(1f, p * 12f)
        val mound = 3f + 7f * min(1f, p * 4f)
        val top = if (p < 1f) solve((1 - p) * totalSand, TOP0 - dip - 2f, TOP1 + 1f) { topArea(it, dip) } else null
        val bot = if (p > 0f) solve(p * totalSand, BOT0 - mound - 2f, BOT1 + mound + 2f) { bottomArea(it, mound) } else null
        return SandLevels(top, dip, bot, mound).also { cache[k] = it }
    }
}
