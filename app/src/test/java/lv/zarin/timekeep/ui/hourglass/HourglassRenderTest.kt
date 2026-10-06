package lv.zarin.timekeep.ui.hourglass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.domain.hourglass.BulbShape
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HourglassRenderTest {
    @get:Rule
    val rule = createComposeRule()

    private val background = Color(0xFF102030)
    private val look = Look(PictureId.HEART, SandColor.LAVENDER, PictureId.STAR, SandColor.SKY)

    /** Pixel colour at logical hourglass coordinates; the 200x256 dp box has the exact 100:128 aspect. */
    private fun ImageBitmap.at(x: Float, y: Float): Color {
        val s = minOf(width / BulbShape.WIDTH, height / BulbShape.HEIGHT)
        val ox = (width - BulbShape.WIDTH * s) / 2
        val oy = (height - BulbShape.HEIGHT * s) / 2
        return toPixelMap()[(ox + x * s).toInt(), (oy + y * s).toInt()]
    }

    @Test
    fun rendersAtKeyProgressValues() {
        var p by mutableFloatStateOf(0f)
        rule.setContent {
            Box(Modifier.size(200.dp, 256.dp).background(background).testTag("box")) {
                Hourglass(look = look, progress = { p }, running = false, modifier = Modifier.fillMaxSize())
            }
        }
        val shots = listOf(0f, 0.3f, 0.7f, 1f).associateWith { value ->
            p = value
            rule.waitForIdle()
            rule.onNodeWithTag("box").captureToImage()
        }
        // Outside the glass the box background shows through.
        assertEquals(background, shots.getValue(0f).at(2f, 64f))
        // Full top bulb at p = 0, full bottom bulb at p = 1.
        val topFull = shots.getValue(0f).at(50f, 40f)
        val bottomFull = shots.getValue(1f).at(50f, 110f)
        assertNotEquals(background, topFull)
        assertNotEquals(background, bottomFull)
        // The sand actually moves: the same pixels differ once that bulb is empty.
        assertNotEquals(topFull, shots.getValue(1f).at(50f, 40f))
        assertNotEquals(bottomFull, shots.getValue(0f).at(50f, 110f))
    }
}
