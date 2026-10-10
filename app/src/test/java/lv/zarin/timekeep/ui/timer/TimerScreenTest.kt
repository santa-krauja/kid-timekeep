package lv.zarin.timekeep.ui.timer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.robolectric.shadows.ShadowLooper
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TimerScreenTest {
    private val clock = FakeClock(100_000)
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = TestAppContainer(it, clock = clock)
    }
    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)

    @get:Rule
    val rule = createComposeRule()

    private var backs = 0

    private var edited: String? = null

    private fun show(state: RunState) {
        runBlocking {
            app.container.timerRepository.upsert(Timer("t", "Brush teeth", 120_000, look, null, state, 0, 0))
        }
        // Hourglasses animate forever, so the clock is driven by hand.
        rule.mainClock.autoAdvance = false
        rule.setContent { TimerScreen("t", onBack = { backs++ }, onEdit = { edited = it }) }
    }

    /** Drives frames by hand and lets Room / ViewModel work on the main looper run. */
    private fun pump() {
        rule.mainClock.advanceTimeBy(50)
        ShadowLooper.idleMainLooper()
    }

    private fun waitForText(text: String) = rule.waitUntil(10_000) {
        pump()
        rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun finishedShowsTimeUpAndOkRemovesRun() {
        show(RunState.Finished(100_000))
        waitForText("Time's up!")
        rule.onNodeWithText("Again").assertIsDisplayed()
        rule.onNodeWithText("Save this look").assertIsDisplayed()
        rule.onNodeWithText("OK").performClick()
        rule.waitUntil(10_000) {
            pump()
            backs > 0
        }
        assertEquals(1, backs)
    }

    @Test
    fun startOverAsksForConfirmation() {
        show(RunState.Running(sinceMs = 40_000, elapsedBeforeMs = 0))
        waitForText("Pause")
        rule.onNodeWithText("Passed 1:00 of 2:00").assertIsDisplayed()
        rule.onNodeWithContentDescription("Start over").performClick()
        waitForText("Start over?")
        rule.onNodeWithText("Cancel").performClick()
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("Start over?").assertDoesNotExist()
        rule.onNodeWithContentDescription("Start over").performClick()
        waitForText("Start over?")
        rule.onNodeWithText("OK").performClick()
        rule.waitUntil(10_000) {
            pump()
            runBlocking { (app.container.timerRepository.get("t")!!.state as RunState.Running).sinceMs == 100_000L }
        }
    }

    @Test
    fun editButtonOpensEditForThisTimer() {
        show(RunState.Running(sinceMs = 100_000, elapsedBeforeMs = 0))
        waitForText("Pause")
        rule.onNodeWithContentDescription("Edit timer").performClick()
        assertEquals("t", edited)
    }

    @Test
    fun editButtonIsHiddenWhenPolicyDeniesEdit() {
        app.container = TestAppContainer(app, clock = clock, controlPolicy = { it != Control.EDIT })
        show(RunState.Running(sinceMs = 100_000, elapsedBeforeMs = 0))
        waitForText("Pause")
        rule.onNodeWithContentDescription("Edit timer").assertDoesNotExist()
    }
}
