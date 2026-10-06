package lv.zarin.timekeep.ui.hourglass

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FlipTest {
    @get:Rule
    val rule = createComposeRule()

    private val look = Look(PictureId.HEART, SandColor.LAVENDER, PictureId.STAR, SandColor.SKY)

    private fun rotation(): Float =
        rule.onNodeWithTag("hg").fetchSemanticsNode().config[HourglassRotation]

    @Test
    fun flipRunsOnTriggerChangeOnly() {
        rule.mainClock.autoAdvance = false
        var trigger by mutableIntStateOf(0)
        rule.setContent {
            Hourglass(
                look = look,
                progress = { 0.5f },
                running = false,
                modifier = Modifier.size(200.dp, 256.dp).testTag("hg"),
                flipTrigger = trigger,
            )
        }
        rule.mainClock.advanceTimeBy(500)
        assertEquals(0f, rotation(), 0f)

        trigger = 1
        rule.runOnIdle { } // let the trigger write reach the composition
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeBy(325)
        val mid = rotation()
        assertTrue("mid-flip rotation $mid", mid in 60f..160f)

        rule.mainClock.advanceTimeBy(1000)
        assertEquals(0f, rotation(), 0f)
    }
}
