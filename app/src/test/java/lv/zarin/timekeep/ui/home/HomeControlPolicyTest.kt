package lv.zarin.timekeep.ui.home

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.seedStarterPresets
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.MainActivity
import lv.zarin.timekeep.domain.control.Control
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeControlPolicyTest {
    init {
        val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>()
        app.allowNotifications()
        app.container = TestAppContainer(app, controlPolicy = { it == Control.ADD_MINUTE })
        seedStarterPresets(app.container)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun noPauseButtonWhenPolicyDeniesPause() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            rule.onAllNodesWithText("Left").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Pause").assertDoesNotExist()
    }

    @Test
    fun noEditOrDeleteMenuWhenPolicyDeniesThem() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Reading").performTouchInput { longClick() }
        rule.waitForIdle()
        rule.onNodeWithText("Edit").assertDoesNotExist()
        rule.onNodeWithText("Delete").assertDoesNotExist()
    }
}
