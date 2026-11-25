package lv.zarin.timekeep

import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

@RunWith(AndroidJUnit4::class)
class MainActivityLockScreenTest {
    private fun alarmIntent(timerId: String = "t") = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        .putExtra(MainActivity.EXTRA_TIMER_ID, timerId)
        .putExtra(MainActivity.EXTRA_FROM_ALARM, true)

    @Test
    fun normalLaunchStaysBehindLockScreen() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { assertFalse(it.overLockScreen) }
        }
    }

    @Test
    fun alarmLaunchShowsOverLockScreenUntilReleased() {
        ActivityScenario.launch<MainActivity>(alarmIntent()).use { scenario ->
            scenario.onActivity {
                assertTrue(it.overLockScreen)
                it.release("t")
                assertFalse(it.overLockScreen)
            }
        }
    }

    @Test
    fun alarmDeliveredToRunningActivityShowsOverLockScreen() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.newIntent(alarmIntent())
        assertTrue(controller.get().overLockScreen)
        controller.close()
    }

    @Test
    fun stoppingActivityReleasesLockScreen() {
        ActivityScenario.launch<MainActivity>(alarmIntent()).use { scenario ->
            scenario.moveToState(Lifecycle.State.CREATED)
            scenario.onActivity { assertFalse(it.overLockScreen) }
        }
    }

    @Test
    fun releasingOlderAlarmDoesNotClearNewerAlarm() {
        val controller = Robolectric.buildActivity(MainActivity::class.java, alarmIntent("a")).setup()
        controller.newIntent(alarmIntent("b"))
        val activity = controller.get()
        activity.release("a")
        assertTrue(activity.overLockScreen)
        activity.release("b")
        assertFalse(activity.overLockScreen)
        controller.close()
    }

    @Test
    fun onlyTheAlarmTimerMayAcquire() {
        ActivityScenario.launch<MainActivity>(alarmIntent("a")).use { scenario ->
            scenario.onActivity {
                it.release("a")
                it.acquire("other")
                assertFalse(it.overLockScreen)
                it.acquire("a")
                assertTrue(it.overLockScreen)
            }
        }
    }
}
