package lv.zarin.timekeep.ui.hourglass

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import lv.zarin.timekeep.domain.hourglass.BulbShape
import lv.zarin.timekeep.domain.hourglass.SandLevelSolver
import lv.zarin.timekeep.domain.timer.Look

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
 * [flipTrigger] is reserved for the flip animation.
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
    Box(modifier.aspectRatio(BulbShape.WIDTH / BulbShape.HEIGHT).then(semantics)) {
        // Back layer.
        Spacer(
            Modifier.fillMaxSize().graphicsLayer().drawWithCache {
                val s = minOf(size.width / BulbShape.WIDTH, size.height / BulbShape.HEIGHT)
                val ox = (size.width - BulbShape.WIDTH * s) / 2
                val oy = (size.height - BulbShape.HEIGHT * s) / 2
                val topTexture = textures.get(true, this, topPainter, look.top, look.topSand, s)
                val bottomTexture = textures.get(false, this, bottomPainter, look.bottom, look.bottomSand, s)
                onDrawBehind {
                    val levels = SandLevelSolver.levels(progress())
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
                    if (!running) return@onDrawBehind
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
