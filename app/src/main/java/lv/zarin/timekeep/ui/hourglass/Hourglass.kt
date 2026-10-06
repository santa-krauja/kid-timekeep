package lv.zarin.timekeep.ui.hourglass

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import lv.zarin.timekeep.domain.hourglass.BulbShape
import lv.zarin.timekeep.domain.hourglass.SandLevelSolver
import lv.zarin.timekeep.domain.timer.Look

/** Test-only: current flip rotation in degrees (0 when idle). */
internal val HourglassRotation = SemanticsPropertyKey<Float>("HourglassRotation")

private val FlipEasing = CubicBezierEasing(0.55f, 0f, 0.3f, 1f)
private val SettleEasing = CubicBezierEasing(0f, 0f, 0.58f, 1f) // CSS ease-out

/** Test-only: current flip settle alpha. */
internal val HourglassAlpha = SemanticsPropertyKey<Float>("HourglassAlpha")

/** Test-only: the progress currently drawn (frozen pre-flip value while flipping, else live). */
internal val HourglassDrawnProgress = SemanticsPropertyKey<Float>("HourglassDrawnProgress")

/**
 * Plain (non-state) flip bookkeeping. [lastDrawn] is the last progress the back layer drew; composition
 * runs before the same frame's draw, so reading it there yields the pre-reset value. [handled] is the
 * last trigger value seen; [active] is true while a flip runs (re-triggers are then ignored, like the
 * prototype); [id] keys the flip effect.
 */
private class FlipState(var handled: Int) {
    var lastDrawn = 0f
    var active = false
    var id = 0
}

/**
 * Shared animation clock for every running hourglass: one state, written once per frame. Several
 * hourglasses may each run [run]; each frame callback writes the same value, so only the first write
 * invalidates readers. Read [time] only from draw blocks of streaming hourglasses.
 */
internal object SandClock {
    val time = mutableLongStateOf(0L)

    suspend fun run(): Nothing {
        while (true) withFrameMillis { if (it != time.longValue) time.longValue = it }
    }
}

/**
 * Triangular glass hourglass with smooth coloured sand and an emoji picture inside each sand body.
 *
 * Three stacked layers: back (glass fill + sand from cached textures; redraws only when [progress]
 * changes), stream (the only layer that reads the frame clock) and front (glass stroke, shine, caps;
 * never redraws). [progress] (elapsed fraction, 0..1) is read only in draw blocks. While [running] (and
 * 0 < progress < 1) a falling-sand stream animates; with reduced motion only its faint core line shows.
 * When [flipTrigger] changes (not on first composition) the whole stack rotates 0..180 degrees over 650 ms
 * while still drawing the pre-flip progress, then draws the live progress with an alpha 0.35..1 settle over
 * 260 ms. With reduced motion the switch is instant.
 */
@Composable
fun Hourglass(
    look: Look,
    progress: () -> Float,
    running: Boolean,
    modifier: Modifier = Modifier,
    flipTrigger: Int = 0,
    contentDescription: String? = null,
) {
    val topPainter = rememberVectorPainter(ImageVector.vectorResource(look.top.drawableRes()))
    val bottomPainter = rememberVectorPainter(ImageVector.vectorResource(look.bottom.drawableRes()))
    val colors = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) GlassColors.Dark else GlassColors.Light
    val paths = remember { HourglassPaths() }
    val textures = remember { SandTextures() }
    val palette = remember(look.topSand, look.bottomSand) { StreamPalette(look.topSand, look.bottomSand) }
    val brushes = remember { CoreBrushCache() }
    val resolver = LocalContext.current.contentResolver
    val reducedMotion = remember {
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val rotation = remember { Animatable(0f) }
    val alpha = remember { Animatable(1f) }
    var frozen by remember { mutableStateOf<Float?>(null) }
    val flip = remember { FlipState(flipTrigger) }
    if (flipTrigger != flip.handled) {
        // Triggers arriving mid-flip are ignored but still marked handled so they do not fire later.
        flip.handled = flipTrigger
        if (!reducedMotion && !flip.active) {
            flip.active = true
            frozen = flip.lastDrawn
            flip.id++
        }
    }
    LaunchedEffect(flip.id) {
        if (flip.id == 0) return@LaunchedEffect
        try {
            rotation.snapTo(0f)
            alpha.snapTo(1f)
            rotation.animateTo(180f, tween(650, easing = FlipEasing))
            frozen = null
            rotation.snapTo(0f)
            alpha.snapTo(0.35f)
            alpha.animateTo(1f, tween(260, easing = SettleEasing))
        } finally {
            // Also runs on cancellation: never leave a stale frozen value behind.
            frozen = null
            flip.active = false
        }
    }
    if (running && !reducedMotion) {
        LaunchedEffect(Unit) { SandClock.run() }
    }
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    val rotationSemantics = Modifier.semantics {
        this[HourglassRotation] = rotation.value
        this[HourglassAlpha] = alpha.value
        this[HourglassDrawnProgress] = frozen ?: progress()
    }
    Box(
        modifier.aspectRatio(BulbShape.WIDTH / BulbShape.HEIGHT).then(semantics).then(rotationSemantics)
            .graphicsLayer {
                rotationZ = rotation.value
                this.alpha = alpha.value
            },
    ) {
        // Back layer.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                val s = minOf(size.width / BulbShape.WIDTH, size.height / BulbShape.HEIGHT)
                val ox = (size.width - BulbShape.WIDTH * s) / 2
                val oy = (size.height - BulbShape.HEIGHT * s) / 2
                val topTexture = textures.get(true, this, topPainter, look.top, look.topSand, s)
                val bottomTexture = textures.get(false, this, bottomPainter, look.bottom, look.bottomSand, s)
                onDrawBehind {
                    val p = frozen ?: progress()
                    flip.lastDrawn = p // plain-field side effect, read in composition by the next flip
                    val levels = SandLevelSolver.levels(p)
                    translate(ox, oy) {
                        scale(s, s, pivot = Offset.Zero) {
                            drawHourglassBack(paths, levels, topTexture, bottomTexture, colors, s)
                        }
                    }
                }
            },
        )
        // Stream layer: the only one that reads the frame clock.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                val s = minOf(size.width / BulbShape.WIDTH, size.height / BulbShape.HEIGHT)
                val ox = (size.width - BulbShape.WIDTH * s) / 2
                val oy = (size.height - BulbShape.HEIGHT * s) / 2
                onDrawBehind {
                    if (!running || frozen != null) return@onDrawBehind
                    val p = progress()
                    if (p <= 0f || p >= 1f) return@onDrawBehind
                    val levels = SandLevelSolver.levels(p)
                    if (levels.topLevel == null) return@onDrawBehind
                    val t = if (reducedMotion) 0L else SandClock.time.longValue
                    translate(ox, oy) {
                        scale(s, s, pivot = Offset.Zero) {
                            drawStream(levels, palette, brushes, t, animate = !reducedMotion)
                        }
                    }
                }
            },
        )
        // Front layer: static.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                val s = minOf(size.width / BulbShape.WIDTH, size.height / BulbShape.HEIGHT)
                val ox = (size.width - BulbShape.WIDTH * s) / 2
                val oy = (size.height - BulbShape.HEIGHT * s) / 2
                onDrawBehind {
                    translate(ox, oy) {
                        scale(s, s, pivot = Offset.Zero) { drawHourglassFront(paths, colors) }
                    }
                }
            },
        )
    }
}
