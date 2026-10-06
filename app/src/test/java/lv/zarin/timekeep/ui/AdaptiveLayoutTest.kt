package lv.zarin.timekeep.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class AdaptiveLayoutTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = AppContainer(it, inMemoryDb = true)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun startBrushTeeth() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }
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
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Pick a timer to see it big").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp")
    fun phoneShowsOnlyList() {
        startBrushTeeth()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            rule.onAllNodesWithText("Brush teeth").fetchSemanticsNodes().size == 2
        }
        rule.onNodeWithText("My timers").assertIsDisplayed()
        rule.onNodeWithText("Left").assertDoesNotExist()
    }
}
