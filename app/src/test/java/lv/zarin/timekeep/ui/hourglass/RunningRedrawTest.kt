package lv.zarin.timekeep.ui.hourglass

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RunningRedrawTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun plainClockProgressAdvancesWhileRunning() {
        rule.mainClock.autoAdvance = false
        var plain = 0.1f // deliberately not snapshot state
        rule.setContent {
            Hourglass(
                look = Look(PictureId.HEART, SandColor.LAVENDER, PictureId.STAR, SandColor.SKY),
                progress = { plain },
                running = true,
                modifier = Modifier.size(200.dp, 256.dp).testTag("hg"),
            )
        }
        rule.mainClock.advanceTimeBy(100)
        val before = rule.onNodeWithTag("hg").fetchSemanticsNode().config[HourglassDrawnProgress]
        plain = 0.5f
        rule.mainClock.advanceTimeBy(100)
        val after = rule.onNodeWithTag("hg").fetchSemanticsNode().config[HourglassDrawnProgress]
        assertTrue("before=$before after=$after", after > before + 0.3f)
    }
}
