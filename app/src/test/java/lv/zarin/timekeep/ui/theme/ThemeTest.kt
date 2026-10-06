package lv.zarin.timekeep.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.domain.ports.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "night")
class ThemeTest {
    @get:Rule
    val rule = createComposeRule()

    private fun backgroundLuminance(mode: ThemeMode): Float {
        var l = -1f
        rule.setContent {
            KidTimekeepTheme(themeMode = mode) { l = MaterialTheme.colorScheme.background.luminance() }
        }
        rule.waitForIdle()
        return l
    }

    @Test
    fun systemModeFollowsUiMode() {
        assertTrue(backgroundLuminance(ThemeMode.SYSTEM) < 0.2f)
    }

    @Test
    fun lightOverrideBeatsNightUiMode() {
        assertTrue(backgroundLuminance(ThemeMode.LIGHT) > 0.8f)
    }

    @Test
    fun darkOverrideIsDark() {
        assertTrue(backgroundLuminance(ThemeMode.DARK) < 0.2f)
    }

    @Test
    fun usesAppAccentWithoutDynamicColourAndLargeTouchTargets() {
        var primaryLight = Color.Unspecified
        var primaryDark = Color.Unspecified
        var minSize = 0.dp
        rule.setContent {
            KidTimekeepTheme(themeMode = ThemeMode.LIGHT) {
                primaryLight = MaterialTheme.colorScheme.primary
                minSize = LocalMinimumInteractiveComponentSize.current
            }
            KidTimekeepTheme(themeMode = ThemeMode.DARK) { primaryDark = MaterialTheme.colorScheme.primary }
        }
        rule.waitForIdle()
        assertEquals(Color(0xFF5B4BC4), primaryLight)
        assertEquals(56.dp, minSize)
        assertEquals(Color(0xFFA99CF5), primaryDark)
    }
}
