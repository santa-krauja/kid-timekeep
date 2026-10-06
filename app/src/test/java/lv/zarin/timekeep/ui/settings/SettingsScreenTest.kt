package lv.zarin.timekeep.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.domain.ports.ThemeMode
import lv.zarin.timekeep.testutil.FakeClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.shadows.ShadowLooper

@RunWith(AndroidJUnit4::class)
class SettingsScreenTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = AppContainer(it, inMemoryDb = true, clock = FakeClock(100_000))
    }
    private val repo get() = app.container.settingsRepository

    @get:Rule
    val rule = createComposeRule()

    private fun pump() {
        rule.mainClock.advanceTimeBy(50)
        ShadowLooper.idleMainLooper()
    }

    private fun waitFor(what: String, condition: () -> Boolean) {
        repeat(200) {
            pump()
            if (condition()) return
            Thread.sleep(5)
        }
        throw AssertionError("Timed out waiting for $what")
    }

    private fun waitForText(text: String) =
        waitFor(text) { rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun current() = runBlocking { repo.settings.first() }

    private fun show() {
        rule.setContent { SettingsScreen(onBack = {}) }
        waitForText("Show numbers")
    }

    @Test
    fun togglesPersist() {
        show()
        rule.onNodeWithText("Show numbers").performClick()
        waitFor("showNumbers off") { !current().showNumbers }
        rule.onNodeWithText("Sound when done").performClick()
        waitFor("sound off") { !current().soundOn }
        rule.onNodeWithText("Vibrate when done").performClick()
        waitFor("vibrate off") { !current().vibrateOn }
        rule.onNodeWithText("Keep screen on").performClick()
        waitFor("keep screen off") { !current().keepScreenOn }
        rule.onNodeWithText("Show numbers").performClick()
        waitFor("showNumbers on") { current().showNumbers }
        assertFalse(current().soundOn)
    }

    @Test
    fun themeDropdownChangesMode() {
        show()
        waitForText("System default")
        rule.onNodeWithText("System default").performClick()
        waitForText("Dark")
        rule.onNodeWithText("Dark").performClick()
        waitFor("theme dark") { current().themeMode == ThemeMode.DARK }
        waitForText("Dark")
        rule.onNodeWithText("Dark").performClick()
        waitForText("Light")
        rule.onNodeWithText("Light").performClick()
        waitFor("theme light") { current().themeMode == ThemeMode.LIGHT }
        assertEquals(ThemeMode.LIGHT, current().themeMode)
    }

    @Test
    fun aboutShowsLicense() {
        show()
        waitForText("Emoji: Noto Color Emoji, Apache License 2.0")
        rule.onNodeWithText("Emoji: Noto Color Emoji, Apache License 2.0", substring = false)
            .performClick()
        waitForText("Licences")
        waitForText("Close")
        waitFor("licence text") {
            rule.onAllNodesWithText("Apache License", substring = true)
                .fetchSemanticsNodes().size >= 2
        }
    }
}
