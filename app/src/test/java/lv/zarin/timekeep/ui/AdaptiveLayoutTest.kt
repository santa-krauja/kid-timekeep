package lv.zarin.timekeep.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.seedStarterPresets
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.allowNotifications()
        it.container = TestAppContainer(it)
        seedStarterPresets(it.container)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun startBrushTeeth() {
        rule.waitUntil(5_000) {
            rule.onAllNodesWithContentDescription("Start Brush teeth").fetchSemanticsNodes().isNotEmpty()
        }
        // A running hourglass animates forever, so the clock is driven by hand from here on.
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun tabletShowsListAndDetailTogether() {
        startBrushTeeth()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            rule.onAllNodesWithText("Left").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("My timers").assertIsDisplayed()
        rule.onNodeWithText("Left").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun tabletWithoutTimersShowsPlaceholder() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Nothing running").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Pick a timer to see it big").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun phoneStartingPresetOpensTimerScreen() {
        startBrushTeeth()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            rule.onAllNodesWithText("My timers").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("Brush teeth").assertIsDisplayed()
        rule.onNodeWithText("Left").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun phoneLandscapeIsSinglePane() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Nothing running").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("My timers").assertIsDisplayed()
        rule.onNodeWithText("Pick a timer to see it big").assertDoesNotExist()
        rule.onNodeWithText("Left").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w891dp-h411dp")
    fun phoneLandscapeTimerShowsAllControls() {
        startBrushTeeth()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            rule.onAllNodesWithText("+1 min").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Left").assertIsDisplayed()
        rule.onNodeWithText("+1 min").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp")
    fun tabletTappingNowCardSelectsIt() {
        startBrushTeeth()
        waitForCount("Brush teeth", 3) // preset row + card + detail title
        rule.onNodeWithContentDescription("Start Get dressed").performClick()
        waitForCount("Get dressed", 3)
        waitForCount("Brush teeth", 2)
        // The Now cards come before the preset rows; the first one is Brush teeth.
        rule.onAllNodesWithText("Brush teeth")[0].performClick()
        waitForCount("Brush teeth", 3)
        waitForCount("Get dressed", 2)
    }

    private fun waitForCount(text: String, count: Int) = rule.waitUntil(10_000) {
        rule.mainClock.advanceTimeBy(50)
        rule.onAllNodesWithText(text).fetchSemanticsNodes().size == count
    }
}
