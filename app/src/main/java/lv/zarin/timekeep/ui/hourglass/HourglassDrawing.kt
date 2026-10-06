package lv.zarin.timekeep.ui.hourglass

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.painter.Painter
import lv.zarin.timekeep.domain.hourglass.BulbShape
import lv.zarin.timekeep.domain.hourglass.BulbShape.BOT0
import lv.zarin.timekeep.domain.hourglass.BulbShape.BOT1
import lv.zarin.timekeep.domain.hourglass.BulbShape.CX
import lv.zarin.timekeep.domain.hourglass.BulbShape.R
import lv.zarin.timekeep.domain.hourglass.BulbShape.TOP0
import lv.zarin.timekeep.domain.hourglass.BulbShape.TOP1
import lv.zarin.timekeep.domain.hourglass.SandLevels
import lv.zarin.timekeep.domain.timer.SandColor
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

/*
 * Port of render() / drawSand() from the approved prototype script. Everything except the emoji is
 * drawn in the logical 100 x 128 box; the caller scales the DrawScope to fit.
 */

/** Theme-dependent glass colours. */
internal data class GlassColors(val fill: Color, val line: Color, val cap: Color) {
    companion object {
        val Light = GlassColors(fill = Color(0x1A78A0C8), line = Color(0xFF8AA4BD), cap = Color(0xFFA87C52))
        val Dark = GlassColors(fill = Color(0x1496BEE6), line = Color(0xFF6F8AA5), cap = Color(0xFF8A6440))
    }
}

/** Sand shades: lighter (×1.12), base, darker (×0.82), as in the prototype's shades(). */
internal data class SandShades(val light: Color, val base: Color, val dark: Color)

private val SHADES: Map<SandColor, SandShades> = SandColor.entries.associateWith { sand ->
    val base = Color(sand.argb)
    fun mul(m: Float) = Color(
        red = (base.red * m).coerceIn(0f, 1f),
        green = (base.green * m).coerceIn(0f, 1f),
        blue = (base.blue * m).coerceIn(0f, 1f),
    )
    SandShades(light = mul(1.12f), base = base, dark = mul(0.82f))
}

internal fun SandColor.shades(): SandShades = SHADES.getValue(this)

/** Bulb outline, offset outwards by [off] logical units (prototype bulbPath()). */
internal fun bulbPath(off: Float): Path = Path().apply {
    var y = TOP0
    moveTo(CX - BulbShape.halfWidth(y) - off, y)
    while (y <= BOT1) {
        lineTo(CX - BulbShape.halfWidth(y) - off, y)
        y += 0.5f
    }
    y = BOT1
    while (y >= TOP0) {
        lineTo(CX + BulbShape.halfWidth(y) + off, y)
        y -= 0.5f
    }
    close()
}

/** Glint stroke down the left side of the top bulb. */
internal fun shinePath(): Path = Path().apply {
    for (y in 16..44) {
        val x = CX - BulbShape.halfWidth(y.toFloat()) + 5f
        if (y == 16) moveTo(x, y.toFloat()) else lineTo(x, y.toFloat())
    }
}

internal fun capPath(top: Float): Path = Path().apply {
    addRoundRect(RoundRect(6f, top, 94f, top + 6f, CornerRadius(3f, 3f)))
}

/** Adds the sand surface polyline (prototype surface()) to [path]. */
private fun Path.surface(top: Boolean, level: Float, amp: Float, start: Boolean) {
    var x = CX - R - 3f
    var first = start
    while (x <= CX + R + 3f) {
        val y = BulbShape.surfaceY(top, level, amp, x)
        if (first) {
            moveTo(x, y)
            first = false
        } else {
            lineTo(x, y)
        }
        x += 0.5f
    }
}

private class Grain(val x: Float, val y: Float, val r: Float, val light: Boolean)

/** 2600 deterministic grains (seed 12345, LCG s = s·1664525 + 1013904223 mod 2^32). */
private val GRAINS: List<Grain> by lazy {
    var s = 12345
    fun next(): Float {
        s = s * 1664525 + 1013904223 // Int overflow wraps mod 2^32
        return (s.toUInt().toDouble() / 4294967296.0).toFloat()
    }
    List(2600) {
        val x = next() * 100f
        val y = next() * 128f
        val r = 0.12f + next() * 0.22f
        Grain(x, y, r, next() < 0.5f)
    }
}

private const val GRAIN_BUCKETS = 3
private const val GRAIN_R_MIN = 0.12f
private const val GRAIN_R_SPAN = 0.22f

/** One drawPoints batch: grains of one shade and (approximate) radius. */
internal class GrainBatch(val points: FloatArray, diameter: Float, light: Boolean) {
    val paint = Paint().apply {
        color = if (light) GRAIN_LIGHT else GRAIN_DARK
        style = PaintingStyle.Stroke
        strokeWidth = diameter
        strokeCap = StrokeCap.Round
    }
}

/** Grain batches per bulb, filtered to that bulb's band as the prototype does. */
internal class GrainSet(val top: List<GrainBatch>, val bottom: List<GrainBatch>)

internal val GRAIN_SET: GrainSet by lazy {
    fun batches(lo: Float, hi: Float): List<GrainBatch> {
        val result = ArrayList<GrainBatch>()
        for (light in listOf(true, false)) {
            for (b in 0 until GRAIN_BUCKETS) {
                val sel = GRAINS.filter {
                    it.light == light && it.y in lo..hi &&
                        ((it.r - GRAIN_R_MIN) / GRAIN_R_SPAN * GRAIN_BUCKETS).toInt().coerceAtMost(GRAIN_BUCKETS - 1) == b
                }
                val pts = FloatArray(sel.size * 2)
                sel.forEachIndexed { i, g ->
                    pts[2 * i] = g.x
                    pts[2 * i + 1] = g.y
                }
                val r = GRAIN_R_MIN + GRAIN_R_SPAN * (b + 0.5f) / GRAIN_BUCKETS
                if (sel.isNotEmpty()) result += GrainBatch(pts, 2 * r, light)
            }
        }
        return result
    }
    GrainSet(top = batches(TOP0, TOP1), bottom = batches(BOT0 - 2f, BOT1))
}

private val GRAIN_LIGHT = Color(1f, 1f, 1f, 0.42f)
private val GRAIN_DARK = Color(70 / 255f, 45 / 255f, 20 / 255f, 0.14f)
private val SURFACE_HIGHLIGHT = Color(1f, 1f, 1f, 0.5f)
private val SHINE = Color(1f, 1f, 1f, 0.45f)
private val GLASS_STROKE = Stroke(width = 1.6f, join = StrokeJoin.Round)
private val SHINE_STROKE = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
private val SURFACE_STROKE = Stroke(width = 0.8f)
private val CORE_STROKE = Stroke(width = 0.55f, cap = StrokeCap.Round)
private const val EMOJI_SIZE = 30f
private const val EMOJI_ALPHA = 0.95f
private const val GRAIN_MIN_SCALE = 1.4f

internal val SAND_GRADIENT_TOP = Brush.verticalGradient(
    0f to Color(1f, 1f, 1f, 0.22f), 1f to Color(0f, 0f, 0f, 0.08f), startY = TOP0, endY = TOP1,
)
internal val SAND_GRADIENT_BOTTOM = Brush.verticalGradient(
    0f to Color(1f, 1f, 1f, 0.22f), 1f to Color(0f, 0f, 0f, 0.08f), startY = BOT0, endY = BOT1,
)

/** Size-independent paths, built once per Hourglass and reused every frame. */
internal class HourglassPaths {
    val bulb = bulbPath(0f)
    val glass = bulbPath(1.4f)
    val shine = shinePath()
    val capTop = capPath(2.5f)
    val capBottom = capPath(119.5f)
    val sand = Path()
    val surface = Path()
}

/**
 * Draws the whole hourglass. The receiver must already be scaled so that one unit = one logical
 * unit; [pxPerUnit] is that scale, used for the emoji (drawn at device resolution) and the grain cut-off.
 */
internal fun DrawScope.drawHourglass(
    paths: HourglassPaths,
    levels: SandLevels,
    topPainter: Painter,
    topSand: SandColor,
    bottomPainter: Painter,
    bottomSand: SandColor,
    colors: GlassColors,
    pxPerUnit: Float,
    streamTime: Long = 0L,
    streamRunning: Boolean = false,
    reducedMotion: Boolean = false,
) {
    drawPath(paths.glass, colors.fill)
    levels.topLevel?.let {
        drawSand(paths, top = true, level = it, amp = levels.dip, topPainter, topSand, pxPerUnit)
    }
    levels.bottomLevel?.let {
        drawSand(paths, top = false, level = it, amp = levels.mound, bottomPainter, bottomSand, pxPerUnit)
    }
    if (streamRunning && levels.topLevel != null) {
        drawStream(levels, topSand.shades(), bottomSand.shades(), streamTime, animate = !reducedMotion)
    }
    drawPath(paths.glass, colors.line, style = GLASS_STROKE)
    drawPath(paths.shine, SHINE, style = SHINE_STROKE)
    drawPath(paths.capTop, colors.cap)
    drawPath(paths.capBottom, colors.cap)
}

private fun DrawScope.drawSand(
    paths: HourglassPaths,
    top: Boolean,
    level: Float,
    amp: Float,
    painter: Painter,
    sand: SandColor,
    pxPerUnit: Float,
) {
    val floor = if (top) TOP1 else BOT1
    val sandPath = paths.sand.apply {
        rewind()
        surface(top, level, amp, start = true)
        lineTo(CX + R + 3f, floor)
        lineTo(CX - R - 3f, floor)
        close()
    }
    val surfacePath = paths.surface.apply {
        rewind()
        surface(top, level, amp, start = true)
    }
    clipPath(paths.bulb) {
        clipPath(sandPath) {
            val box = Size(BulbShape.WIDTH, BulbShape.HEIGHT)
            drawRect(sand.shades().base, size = box)
            drawRect(if (top) SAND_GRADIENT_TOP else SAND_GRADIENT_BOTTOM, size = box)

            // Emoji: undo the logical scale so the vector rasterises at device resolution.
            val cy = if (top) 29f else 104f
            val left = CX - EMOJI_SIZE / 2
            val topY = cy - EMOJI_SIZE / 2
            scale(1f / pxPerUnit, 1f / pxPerUnit, pivot = Offset.Zero) {
                translate((left * pxPerUnit).roundToInt().toFloat(), (topY * pxPerUnit).roundToInt().toFloat()) {
                    with(painter) {
                        draw(Size(EMOJI_SIZE * pxPerUnit, EMOJI_SIZE * pxPerUnit), alpha = EMOJI_ALPHA)
                    }
                }
            }

            if (pxPerUnit > GRAIN_MIN_SCALE) {
                for (batch in if (top) GRAIN_SET.top else GRAIN_SET.bottom) {
                    drawContext.canvas.drawRawPoints(PointMode.Points, batch.points, batch.paint)
                }
            }

            drawPath(surfacePath, SURFACE_HIGHLIGHT, style = SURFACE_STROKE)
        }
    }
}

private const val STREAM_FADE_START = 0.25f
private const val STREAM_FADE_SPAN = 0.6f
private val CORE_ALPHA = 0.45f

/** Port of the prototype drawStream(): faint core line plus (unless [animate] is false) grains and splash. */
private fun DrawScope.drawStream(levels: SandLevels, ts: SandShades, bs: SandShades, t: Long, animate: Boolean) {
    val bottom = levels.bottomLevel
    val y0 = TOP1 - 1.2f
    val y1 = if (bottom != null) maxOf(BOT0 + 0.5f, bottom - levels.mound) else BOT1
    val len = y1 - y0
    drawLine(
        brush = Brush.verticalGradient(0f to ts.base, 1f to bs.base, startY = y0, endY = y1),
        start = Offset(CX, y0),
        end = Offset(CX, y1),
        strokeWidth = CORE_STROKE.width,
        cap = CORE_STROKE.cap,
        alpha = CORE_ALPHA,
    )
    if (!animate) return
    val tf = t.toDouble()
    val grains = SandParticles.stream
    for (i in grains.indices) {
        val g = grains[i]
        val f = ((tf / g.per + g.ph) % 1.0).toFloat()
        val y = y0 + Math.pow(f.toDouble(), 1.6).toFloat() * len
        val x = CX + g.dx * f * f + sin(tf / 70 + g.wob).toFloat() * 0.1f * f
        val m = ((f - STREAM_FADE_START) / STREAM_FADE_SPAN).coerceIn(0f, 1f)
        drawCircle(lerp(ts.shade(g.sh), bs.shade(g.sh), m), g.r, Offset(x, y))
    }
    if (bottom != null) {
        val splash = SandParticles.splash
        for (i in splash.indices) {
            val s = splash[i]
            val q = ((tf / s.per + s.ph) % 1.0).toFloat()
            val cx = CX + s.dir * s.reach * q
            val cy = y1 - sin(q * PI.toFloat()) * s.hgt + q * q * 1.5f
            drawCircle(bs.shade(s.sh), s.r, Offset(cx, cy), alpha = 1f - q)
        }
    }
}

private fun SandShades.shade(i: Int): Color = when (i) {
    0 -> light
    1 -> base
    else -> dark
}
