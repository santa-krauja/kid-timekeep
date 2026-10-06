package lv.zarin.timekeep.ui.hourglass

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
 * Triangular glass hourglass with smooth coloured sand and an emoji picture inside each sand body.
 *
 * [progress] (elapsed fraction, 0..1) is read only in the draw phase, so animating it redraws without
 * recomposing. [running] and [flipTrigger] are reserved for the falling stream and the flip animation.
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
    val semantics = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            role = Role.Image
        }
    } else {
        Modifier
    }
    Spacer(
        modifier
            .aspectRatio(BulbShape.WIDTH / BulbShape.HEIGHT)
            .then(semantics)
            .drawWithCache {
                val s = minOf(size.width / BulbShape.WIDTH, size.height / BulbShape.HEIGHT)
                val ox = (size.width - BulbShape.WIDTH * s) / 2
                val oy = (size.height - BulbShape.HEIGHT * s) / 2
                onDrawBehind {
                    val levels = SandLevelSolver.levels(progress())
                    translate(ox, oy) {
                        scale(s, s, pivot = Offset.Zero) {
                            drawHourglass(
                                paths = paths,
                                levels = levels,
                                topPainter = topPainter,
                                topSand = look.topSand,
                                bottomPainter = bottomPainter,
                                bottomSand = look.bottomSand,
                                colors = colors,
                                pxPerUnit = s,
                            )
                        }
                    }
                }
            },
    )
}
