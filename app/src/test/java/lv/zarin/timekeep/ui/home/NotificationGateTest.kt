package lv.zarin.timekeep.ui.home

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.testutil.seedStarterPresets
import lv.zarin.timekeep.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class NotificationGateTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = TestAppContainer(it)
        seedStarterPresets(it.container)
    }

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun firstStartShowsRationaleThenStartsOnNotNowAndNeverAsksAgain() {
        rule.waitUntil(5_000) { rule.onAllNodesWithText("Reading").fetchSemanticsNodes().isNotEmpty() }
        rule.mainClock.autoAdvance = false
        rule.onNodeWithContentDescription("Start Brush teeth").performClick()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("Let Kid Timekeep tell you when time is up?").assertExists()
        runBlocking { assertEquals(0, app.container.timerRepository.getAll().size) }
        rule.onNodeWithText("Not now").performClick()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("Let Kid Timekeep tell you when time is up?").assertDoesNotExist()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            ShadowLooper.idleMainLooper()
            Thread.sleep(5)
            runBlocking { app.container.timerRepository.getAll().size } == 1
        }
        rule.onNodeWithContentDescription("Start Get dressed").performClick()
        rule.mainClock.advanceTimeBy(100)
        rule.onNodeWithText("Let Kid Timekeep tell you when time is up?").assertDoesNotExist()
        rule.waitUntil(10_000) {
            rule.mainClock.advanceTimeBy(50)
            ShadowLooper.idleMainLooper()
            Thread.sleep(5)
            runBlocking { app.container.timerRepository.getAll().size } == 2
        }
    }
}
