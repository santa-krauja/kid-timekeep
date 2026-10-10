package lv.zarin.timekeep.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.seedStarterPresets
import lv.zarin.timekeep.MainActivity
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.testutil.pump
import lv.zarin.timekeep.testutil.pumpUntil
import lv.zarin.timekeep.testutil.pumpUntilDescription
import lv.zarin.timekeep.testutil.pumpUntilNoText
import lv.zarin.timekeep.testutil.pumpUntilText
import lv.zarin.timekeep.testutil.textCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** End-to-end flows through the real MainActivity with a fake clock; time only moves when a test moves it. */
@RunWith(AndroidJUnit4::class)
// A tall phone screen keeps every list row on screen (offscreen rows can't be tapped).
@Config(qualifiers = "w411dp-h891dp")
class FlowsTest {
    private val clock = FakeClock(1_000_000)
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = TestAppContainer(it, clock = clock)
        seedStarterPresets(it.container)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun timers(): List<Timer> = runBlocking { app.container.timerRepository.getAll() }

    /** Running hourglasses never go idle, so frames and the looper are driven by hand from the start. */
    private fun openHome() {
        rule.mainClock.autoAdvance = false
        rule.pumpUntilText("My timers")
        rule.pumpUntilDescription("Start Brush teeth")
    }

    @Test
    fun createStartPauseFinish() {
        openHome()
        rule.onNodeWithContentDescription("New timer").performClick()
        rule.pumpUntilText("Start")
        rule.onNode(hasSetTextAction()).performTextInput("Test")
        rule.onNodeWithText("30 s").performClick()
        rule.pump(2)
        rule.onNodeWithText("Start").performClick()

        rule.pumpUntilText("Pause")
        assertEquals(1, timers().size)
        assertEquals("Test", timers().single().name)
        assertEquals(30_000L, timers().single().durationMs)

        rule.onNodeWithText("Pause").performClick()
        rule.pumpUntilText("Go on")
        assertTrue(timers().single().state is RunState.Paused)

        rule.onNodeWithText("Go on").performClick()
        rule.pumpUntilText("Pause")
        assertTrue(timers().single().state is RunState.Running)

        clock.advance(31_000)
        rule.pumpUntilText("Time's up!")
        rule.onNodeWithText("OK").performClick()
        rule.pumpUntilNoText("Time's up!")
        rule.pumpUntilText("Nothing running")
        assertEquals(emptyList<Timer>(), timers())
    }

    @Test
    fun editRunningTimerRenamesItOnTheTimerScreen() {
        openHome()
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
        rule.pumpUntilText("Pause")
        rule.onNodeWithContentDescription("Edit timer").performClick()
        rule.pumpUntilText("Save")
        rule.onNodeWithText("Pause the timer to change its length").assertExists()
        rule.onNode(hasSetTextAction()).performTextReplacement("Wash hands")
        rule.onNodeWithText("Save").performClick()
        rule.pumpUntilText("Pause")
        rule.pumpUntil("renamed") { timers().single().name == "Wash hands" }
        rule.pumpUntilText("Wash hands")
        assertTrue(timers().single().state is RunState.Running)
    }

    @Test
    fun presetStartTwiceGivesDifferentLooks() {
        openHome()
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
        rule.pumpUntilText("Pause")
        rule.onNodeWithContentDescription("Back").performClick()
        rule.pumpUntilNoText("Pause")
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
        rule.pumpUntil("second run stored") { timers().size == 2 }

        val runs = timers()
        assertEquals(2, runs.size)
        assertNotEquals(runs[0].look, runs[1].look)
    }

    @Test
    fun startOverKeepsLook() {
        openHome()
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
        rule.pumpUntilText("Pause")
        val before = timers().single()

        clock.advance(20_000)
        rule.pump(6)
        rule.onNodeWithContentDescription("Start over").performClick()
        rule.pumpUntilText("Start over?")
        rule.onNodeWithText("OK").performClick()
        rule.pumpUntilNoText("Start over?")
        rule.pumpUntil("run restarted at the new clock time") {
            (timers().single().state as? RunState.Running)?.sinceMs == clock.nowMs()
        }

        val after = timers().single()
        assertEquals(before.id, after.id)
        assertEquals(before.look, after.look)
    }

    @Test
    fun deletePreset() {
        openHome()
        assertEquals(1, rule.textCount("Reading"))
        rule.onNodeWithText("Reading").performTouchInput { longClick() }
        rule.pumpUntilText("Delete")
        rule.onNodeWithText("Delete").performClick()
        rule.pumpUntilText("Delete preset?")
        rule.onNodeWithTag("confirm_delete").performClick()
        rule.pumpUntilNoText("Reading")
        assertEquals(1, rule.textCount("Brush teeth"))
        assertEquals(1, rule.textCount("Get dressed"))
    }
}
