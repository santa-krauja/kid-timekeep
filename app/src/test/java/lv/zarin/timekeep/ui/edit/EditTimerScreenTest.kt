package lv.zarin.timekeep.ui.edit

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasSetTextAction
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.testutil.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class EditTimerScreenTest {
    private val clock = FakeClock(100_000)
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = AppContainer(it, inMemoryDb = true, clock = clock)
    }

    @get:Rule
    val rule = createComposeRule()

    private var started: String? = null

    // The preview glasses are not running, so the clock can auto-advance (performScrollTo needs it).
    private fun show() {
        rule.setContent { EditTimerScreen(presetId = null, onBack = {}, onStarted = { started = it }) }
        pump()
    }

    private fun pump() {
        rule.mainClock.advanceTimeBy(50)
        ShadowLooper.idleMainLooper()
    }

    /** Bounded by pump count: with a paused looper, waitUntil's timeout clock may never advance. */
    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            pump()
            if (condition()) return
        }
        throw AssertionError("Timed out waiting for $what")
    }

    private fun waitForText(text: String) =
        waitFor(text) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun pickersHiddenUntilRequested() {
        show()
        waitForText("Choose pictures & colours")
        rule.onNodeWithText("Top picture").assertDoesNotExist()
        rule.onNodeWithText("Choose pictures & colours").performScrollTo().performClick()
        waitForText("Top picture")
        rule.onNodeWithText("Favourite looks").assertExists()
        rule.onNodeWithText("Done").performClick()
        waitForText("Choose pictures & colours")
        rule.onNodeWithText("Top picture").assertDoesNotExist()
    }

    @Test
    fun startCreatesRunAndReportsId() {
        show()
        waitForText("Name")
        rule.onNode(hasSetTextAction()).performTextInput("Tidy up")
        rule.onNodeWithText("30 s").performClick()
        rule.onNodeWithText("Start").performClick()
        waitFor("start") { started != null }
        val run = runBlocking { app.container.timerRepository.get(started!!)!! }
        assertEquals("Tidy up", run.name)
        assertEquals(30_000L, run.durationMs)
    }
}
