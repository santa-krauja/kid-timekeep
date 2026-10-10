package lv.zarin.timekeep.ui.nav

import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeShowsTitleAndOpensSettings() {
        rule.onNodeWithText("My timers").assertExists()
        rule.onNodeWithContentDescription("Settings").performClick()
        rule.onNodeWithText("Settings").assertExists()
    }
}
