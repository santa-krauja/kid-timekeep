package lv.zarin.timekeep.ui.hourglass

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
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

    private fun drawn(): Float =
        rule.onNodeWithTag("hg").fetchSemanticsNode().config[HourglassDrawnProgress]

    private fun start(progress: () -> Float, trigger: () -> Int) {
        rule.setContent {
            Hourglass(
                look = look,
                progress = progress,
                running = false,
                modifier = Modifier.size(200.dp, 256.dp).testTag("hg"),
                flipTrigger = trigger(),
            )
        }
    }

    private fun startFlipAndSettleFrames() {
        rule.runOnIdle { }
        rule.mainClock.advanceTimeByFrame()
        rule.mainClock.advanceTimeByFrame()
    }

    @Test
    fun flipFreezesPreFlipProgressThenShowsLive() {
        rule.mainClock.autoAdvance = false
        var trigger by mutableIntStateOf(0)
        var p by mutableFloatStateOf(0.6f)
        start({ p }, { trigger })
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithTag("hg").captureToImage() // force a real draw so the back layer records 0.6
        assertEquals(0.6f, drawn(), 0f)

        // Caller resets progress and restarts in the same frame.
        Snapshot.withMutableSnapshot { p = 0f; trigger = 1 }
        startFlipAndSettleFrames()
        rule.mainClock.advanceTimeBy(325)
        assertEquals(0.6f, drawn(), 0f)

        rule.mainClock.advanceTimeBy(1000)
        assertEquals(0f, drawn(), 0f)
    }

    @Test
    fun secondTriggerMidFlipIsIgnored() {
        rule.mainClock.autoAdvance = false
        var trigger by mutableIntStateOf(0)
        start({ 0.5f }, { trigger })
        rule.mainClock.advanceTimeBy(100)
        trigger = 1
        startFlipAndSettleFrames()
        rule.mainClock.advanceTimeBy(200)
        val before = rotation()
        assertTrue(before > 0f)
        trigger = 2
        val samples = mutableListOf<Float>()
        repeat(60) { rule.mainClock.advanceTimeBy(50); samples += rotation() }
        // Monotonic increase to 180 (never snaps back mid-flip), then 0 forever (no second flip).
        var prev = before
        var finished = false
        for (r in samples) {
            if (!finished && r >= prev) prev = r
            else { finished = true; assertEquals(0f, r, 0f) }
        }
        assertTrue("flip completed", finished)
        assertEquals(0f, rotation(), 0f)
    }

    @Test
    fun alphaReturnsToOneAfterSettle() {
        rule.mainClock.autoAdvance = false
        var trigger by mutableIntStateOf(0)
        start({ 0.5f }, { trigger })
        rule.mainClock.advanceTimeBy(100)
        trigger = 1
        startFlipAndSettleFrames()
        rule.mainClock.advanceTimeBy(2000)
        assertEquals(1f, rule.onNodeWithTag("hg").fetchSemanticsNode().config[HourglassAlpha], 0f)
        assertEquals(0f, rotation(), 0f)
        assertEquals(0.5f, drawn(), 0f)
    }
}
