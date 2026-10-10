package lv.zarin.timekeep.ui.nav

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.KidTimekeepApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class StartTimerConsumeTest {
    init {
        ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
            it.container = AppContainer(it, inMemoryDb = true)
        }
    }

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun startTimerIdNavigatesOnceAndIsConsumed() {
        var pending by mutableStateOf<String?>("t1")
        var consumedCalls = 0
        lateinit var nav: NavHostController
        var timerArrivals = 0
        rule.setContent {
            nav = rememberNavController()
            DisposableEffect(nav) {
                val l = NavController.OnDestinationChangedListener { _, d, _ -> if (d.hasRoute<TimerRoute>()) timerArrivals++ }
                nav.addOnDestinationChangedListener(l)
                onDispose { nav.removeOnDestinationChangedListener(l) }
            }
            AppNavHost(nav, startTimerId = pending, onStartTimerConsumed = { consumedCalls++; pending = null })
        }
        rule.mainClock.autoAdvance = false
        repeat(200) {
            if (consumedCalls > 0) return@repeat
            rule.mainClock.advanceTimeBy(50)
            ShadowLooper.idleMainLooper()
            Thread.sleep(5)
        }
        assertEquals(1, consumedCalls)
        assertEquals(1, timerArrivals)
        // Recomposition (as after recreation) with the id already consumed must not navigate again.
        rule.runOnIdle { }
        repeat(5) { rule.mainClock.advanceTimeBy(50) }
        assertEquals(1, consumedCalls)
        assertEquals(1, timerArrivals)
    }
}
