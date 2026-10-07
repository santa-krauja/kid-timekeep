package lv.zarin.timekeep.ui.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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

    private fun waitForText(text: String, substring: Boolean = false) =
        waitFor(text) { rule.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForTag(tag: String) =
        waitFor(tag) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

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
        waitForTag("theme_dropdown")
        rule.onNodeWithTag("theme_dropdown").performClick()
        waitForTag("theme_option_DARK")
        rule.onNodeWithTag("theme_option_DARK").performClick()
        waitFor("theme dark") { current().themeMode == ThemeMode.DARK }
        waitForText("Dark")
        rule.onNodeWithTag("theme_dropdown").performClick()
        waitForTag("theme_option_LIGHT")
        rule.onNodeWithTag("theme_option_LIGHT").performClick()
        waitFor("theme light") { current().themeMode == ThemeMode.LIGHT }
        assertEquals(ThemeMode.LIGHT, current().themeMode)
    }

    @Test
    @org.robolectric.annotation.Config(sdk = [30])
    fun languageDropdownIsFirstAndSetsAppLocale() {
        show()
        waitForTag("language_dropdown")
        rule.onNodeWithTag("language_dropdown").performClick()
        waitForTag("language_option_lv")
        rule.onNodeWithTag("language_option_lv").performClick()
        try {
            waitFor("lv locale") {
                androidx.appcompat.app.AppCompatDelegate.getApplicationLocales().toLanguageTags() == "lv"
            }
        } finally {
            androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(
                androidx.core.os.LocaleListCompat.getEmptyLocaleList(),
            )
        }
    }

    @Test
    fun aboutShowsLicense() {
        show()
        waitForText("Emoji: Noto Color Emoji, Apache License 2.0")
        rule.onNodeWithText("Emoji: Noto Color Emoji, Apache License 2.0", substring = false)
            .performScrollTo().performClick()
        waitForText("Licences")
        waitForText("Close")
        // Only present in res/raw/noto_emoji_license.txt, so the file really loaded.
        waitForText("Licensed under the Apache License, Version 2.0", substring = true)
        // Apache-2.0 4(a)/(b): the full licence text and a notice that the files were modified.
        waitForText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION", substring = true)
        waitForText("Converted from SVG to Android VectorDrawable for this app.", substring = true)
    }

    @Test
    fun notificationsRowShowsAllowedOrTurnOn() {
        val nm = app.getSystemService(android.app.NotificationManager::class.java)
        org.robolectric.Shadows.shadowOf(nm).setNotificationsEnabled(true)
        show()
        rule.onNodeWithText("Allowed ✓").assertExists()
    }

    @Test
    fun notificationsRowOffersTurnOnWhenDisabled() {
        val nm = app.getSystemService(android.app.NotificationManager::class.java)
        org.robolectric.Shadows.shadowOf(nm).setNotificationsEnabled(false)
        show()
        rule.onNodeWithText("Turn on").assertExists()
    }
}
