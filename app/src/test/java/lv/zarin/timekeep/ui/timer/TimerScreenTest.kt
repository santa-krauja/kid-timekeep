package lv.zarin.timekeep.ui.timer

import android.app.NotificationManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.alarm.Notifications
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.testutil.allowNotifications
import lv.zarin.timekeep.ui.common.LocalLockScreenAccess
import lv.zarin.timekeep.ui.common.LockScreenAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

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
        rule.mainClock.autoAdvance = false
        rule.setContent { TimerScreen("t", onBack = { backs++ }, onEdit = { edited = it }) }
    }

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

    private fun counting(onRelease: () -> Unit) = object : LockScreenAccess {
        override fun acquire(timerId: String) = Unit

        override fun release(timerId: String) = onRelease()
    }

    private fun ring() {
        app.allowNotifications()
        val timer = runBlocking { app.container.timerRepository.get("t")!! }
        Notifications.showTimeUp(app, timer, Settings())
    }

    private fun notification() = shadowOf(app.getSystemService(NotificationManager::class.java)).getNotification("t".hashCode())

    @Test
    fun stopButtonSilencesRingingAndKeepsOverlay() {
        show(RunState.Finished(100_000))
        ring()
        waitForText("Stop")
        assertTrue(Notifications.isRinging(app, "t"))
        rule.onNodeWithText("Stop").performClick()
        rule.waitUntil(10_000) {
            pump()
            !Notifications.isRinging(app, "t")
        }
        assertEquals(Notifications.CHANNEL_SILENT, notification().channelId)
        rule.waitUntil(10_000) {
            pump()
            rule.onAllNodesWithText("Stop").fetchSemanticsNodes().isEmpty()
        }
        rule.onNodeWithText("Time's up!").assertIsDisplayed()
        rule.onNodeWithText("Again").assertIsDisplayed()
        rule.onNodeWithText("OK").assertIsDisplayed()
    }

    @Test
    fun openingFinishedTimerKeepsRingingUntilDismissed() {
        runBlocking {
            app.container.timerRepository.upsert(Timer("t", "Brush teeth", 120_000, look, null, RunState.Finished(100_000), 0, 0))
        }
        ring()
        rule.mainClock.autoAdvance = false
        rule.setContent { TimerScreen("t", onBack = { backs++ }) }
        waitForText("Stop")
        assertTrue(Notifications.isRinging(app, "t"))
        rule.onNodeWithText("OK").performClick()
        rule.waitUntil(10_000) {
            pump()
            backs > 0
        }
        assertNull(notification())
    }

    @Test
    fun stopButtonIsHiddenWhenNotRinging() {
        show(RunState.Finished(100_000))
        waitForText("Time's up!")
        rule.onNodeWithText("Stop").assertDoesNotExist()
    }

    @Test
    fun releasingLockScreenWhenOverlayIsDismissed() {
        var released = 0
        runBlocking {
            app.container.timerRepository.upsert(Timer("t", "Brush teeth", 120_000, look, null, RunState.Finished(100_000), 0, 0))
        }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLockScreenAccess provides counting { released++ }) {
                TimerScreen("t", onBack = {})
            }
        }
        waitForText("OK")
        rule.onNodeWithText("OK").performClick()
        assertEquals(1, released)
    }

    private fun showCountingReleases(state: RunState?, content: @Composable (@Composable () -> Unit) -> Unit = { it() }): () -> Int {
        var released = 0
        if (state != null) {
            runBlocking {
                app.container.timerRepository.upsert(Timer("t", "Brush teeth", 120_000, look, null, state, 0, 0))
            }
        }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLockScreenAccess provides counting { released++ }) {
                content { TimerScreen("t", onBack = {}) }
            }
        }
        return { released }
    }

    @Test
    fun againReleasesLockScreen() {
        var released = 0
        runBlocking {
            app.container.timerRepository.upsert(Timer("t", "Brush teeth", 120_000, look, null, RunState.Finished(100_000), 0, 0))
        }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            CompositionLocalProvider(LocalLockScreenAccess provides counting { released++ }) { TimerScreen("t", onBack = {}) }
        }
        waitForText("Again")
        rule.onNodeWithText("Again").performClick()
        assertTrue(released >= 1)
    }

    @Test
    fun runningTimerReleasesLockScreen() {
        val released = showCountingReleases(RunState.Running(sinceMs = 100_000, elapsedBeforeMs = 0))
        waitForText("Pause")
        assertTrue(released() >= 1)
    }

    @Test
    fun missingTimerReleasesLockScreen() {
        val released = showCountingReleases(null)
        rule.waitUntil(10_000) {
            pump()
            released() >= 1
        }
    }

    @Test
    fun finishedOverlayHoldsLockScreenUntilLeftThenReleases() {
        var show by mutableStateOf(true)
        val released = showCountingReleases(RunState.Finished(100_000)) { screen -> if (show) screen() }
        waitForText("OK")
        assertEquals(0, released())
        show = false
        rule.waitUntil(10_000) {
            pump()
            released() >= 1
        }
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
