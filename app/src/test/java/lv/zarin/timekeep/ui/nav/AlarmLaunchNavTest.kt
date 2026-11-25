package lv.zarin.timekeep.ui.nav

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.ui.common.LocalLockScreenAccess
import lv.zarin.timekeep.ui.common.LockScreenAccess
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class AlarmLaunchNavTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = TestAppContainer(it, clock = FakeClock(100_000))
    }

    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    private val lockScreen = object : LockScreenAccess {
        override fun acquire(timerId: String) {
            events += "acquire"
        }

        override fun release(timerId: String) {
            events += "release"
        }
    }

    private fun pump() {
        rule.mainClock.advanceTimeBy(50)
        ShadowLooper.idleMainLooper()
    }

    private fun waitFor(condition: () -> Boolean) = rule.waitUntil(10_000) {
        pump()
        condition()
    }

    @Test
    fun coldAlarmLaunchStartsOnTimerAndBackReleasesBeforeHome() {
        runBlocking {
            app.container.timerRepository.upsert(
                Timer(
                    "t1",
                    "Brush teeth",
                    120_000,
                    Look(PictureId.STAR, SandColor.SKY, PictureId.HEART, SandColor.PEACH),
                    null,
                    RunState.Finished(100_000),
                    0,
                    0
                )
            )
        }
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val nav = rememberNavController()
            DisposableEffect(nav) {
                val listener = NavController.OnDestinationChangedListener { _, d, _ ->
                    events += if (d.hasRoute<TimerRoute>()) {
                        "timer"
                    } else if (d.hasRoute<HomeRoute>()) {
                        "home"
                    } else {
                        "other"
                    }
                }
                nav.addOnDestinationChangedListener(listener)
                onDispose { nav.removeOnDestinationChangedListener(listener) }
            }
            CompositionLocalProvider(LocalLockScreenAccess provides lockScreen) {
                AppNavHost(nav, startTimerId = "t1", initialTimerId = "t1")
            }
        }
        waitFor { rule.onAllNodesWithText("Time's up!").fetchSemanticsNodes().isNotEmpty() }
        assertEquals(listOf("timer"), events.filter { it == "timer" || it == "home" })
        rule.onNodeWithContentDescription("Back").performClick()
        waitFor { "home" in events }
        assertTrue(events.indexOf("release") < events.indexOf("home"))
        assertEquals(listOf("timer", "home"), events.filter { it == "timer" || it == "home" })
    }
}
