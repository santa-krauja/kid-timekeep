package lv.zarin.timekeep.ui.edit

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
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
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.testutil.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import lv.zarin.timekeep.testutil.pump
import lv.zarin.timekeep.testutil.pumpUntil
import lv.zarin.timekeep.testutil.pumpUntilText

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp")
class EditTimerScreenTest {
    private val clock = FakeClock(100_000)
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = TestAppContainer(it, clock = clock)
    }

    @get:Rule
    val rule = createComposeRule()

    private var started: String? = null

    // The preview glasses are not running, so the clock can auto-advance (performScrollTo needs it).
    private fun show() {
        rule.setContent { EditTimerScreen(presetId = null, onBack = {}, onStarted = { started = it }) }
        rule.pump()
    }

    @Test
    fun pickersHiddenUntilRequested() {
        show()
        rule.pumpUntilText("Choose pictures & colours")
        rule.onNodeWithText("Top picture").assertDoesNotExist()
        rule.onNodeWithText("Choose pictures & colours").performScrollTo().performClick()
        rule.pumpUntilText("Top picture")
        rule.onNodeWithText("Favourite looks").assertExists()
        rule.onNodeWithText("Done").performClick()
        rule.pumpUntilText("Choose pictures & colours")
        rule.onNodeWithText("Top picture").assertDoesNotExist()
    }

    @Test
    fun keepLookIsDisabledUntilSaveAsPresetIsOn() {
        show()
        rule.pumpUntilText("Save as preset")
        rule.onNodeWithText("Save as preset").performScrollTo().assertIsOff()
        rule.onNodeWithText("Keep this look every time").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithText("Save as preset").performClick()
        rule.pump(2)
        rule.onNodeWithText("Save as preset").assertIsOn()
        rule.onNodeWithText("Keep this look every time").assertIsEnabled()
    }

    @Test
    fun startCreatesRunAndReportsId() {
        show()
        rule.pumpUntilText("Name")
        rule.onNode(hasSetTextAction()).performTextInput("Tidy up")
        rule.onNodeWithText("30 s").performClick()
        rule.onNodeWithText("Start").performClick()
        rule.pumpUntil("start") { started != null }
        val run = runBlocking { app.container.timerRepository.get(started!!)!! }
        assertEquals("Tidy up", run.name)
        assertEquals(30_000L, run.durationMs)
    }
}
