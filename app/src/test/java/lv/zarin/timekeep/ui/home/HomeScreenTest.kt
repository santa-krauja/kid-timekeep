package lv.zarin.timekeep.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.seedStarterPresets
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = AppContainer(it, inMemoryDb = true)
        seedStarterPresets(it.container)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForPresets() =
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun seededPresetsShownAndStartCreatesRun() {
        waitForPresets()
        rule.onNodeWithText("Nothing running").assertIsDisplayed()
        rule.onNodeWithText("Brush teeth").assertIsDisplayed()
        rule.onNodeWithText("Get dressed").assertIsDisplayed()
        rule.onNodeWithText("Reading").assertIsDisplayed()
        // A running hourglass animates forever, so the clock is driven by hand from here on.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            rule.onAllNodesWithText("Brush teeth").fetchSemanticsNodes().size == 2
        }
        rule.onNodeWithText("Nothing running").assertDoesNotExist()
        runBlocking { check(app.container.timerRepository.getAll().single().name == "Brush teeth") }
    }

    @Test
    fun deletePresetAsksForConfirmation() {
        waitForPresets()
        rule.onNodeWithText("Reading").performTouchInput { longClick() }
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithText("Delete preset?").assertIsDisplayed()
        rule.onNodeWithText("Cancel").performClick()
        rule.onNodeWithText("Reading").assertIsDisplayed()
        rule.onNodeWithText("Reading").performTouchInput { longClick() }
        rule.onNodeWithText("Delete").performClick()
        rule.onNodeWithTag("confirm_delete").performClick()
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isEmpty() }
    }
}
