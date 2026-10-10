package lv.zarin.timekeep.ui.common

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class DurationPhraseTest {
    @get:Rule
    val rule = createComposeRule()

    /** Renders [durationPhrase] for every input in one composition. */
    private fun phrases(vararg ms: Long, roundUp: Boolean = false): List<String> {
        var out = emptyList<String>()
        rule.setContent { out = ms.map { durationPhrase(it, roundUp) } }
        rule.waitForIdle()
        return out
    }

    @Test
    fun englishPhrase() {
        assertEquals(
            listOf("1 minute 30 seconds", "0 seconds", "1 hour 2 minutes", "2 hours 1 second"),
            phrases(90_000, 0, 3_720_000, 7_201_000),
        )
    }

    @Test
    @Config(qualifiers = "lv")
    fun latvianPluralBoundaries() {
        val min = 60_000L
        assertEquals(
            listOf("0 sekunžu", "1 minūte", "2 minūtes", "11 minūšu", "21 minūte", "10 minūšu", "30 minūšu"),
            phrases(0, 1 * min, 2 * min, 11 * min, 21 * min, 10 * min, 30 * min),
        )
    }

    @Test
    @Config(qualifiers = "lv")
    fun latvianSecondsAndHours() {
        assertEquals(
            listOf(
                "1 sekunde", "2 sekundes", "11 sekunžu", "21 sekunde", "20 sekunžu",
                "1 stunda", "2 stundas", "1 minūte 30 sekunžu", "1 stunda 1 minūte 1 sekunde",
            ),
            phrases(1_000, 2_000, 11_000, 21_000, 20_000, 3_600_000, 7_200_000, 90_000, 3_661_000),
        )
    }

    @Test
    @Config(qualifiers = "lv")
    fun latvianZeroMinutesPlural() {
        // durationPhrase never emits "0 minutes"; check the raw plural so the zero form is covered.
        val res = ApplicationProvider.getApplicationContext<Context>().resources
        val r = R.plurals.duration_minutes
        assertEquals("0 minūšu", res.getQuantityString(r, 0, 0))
        assertEquals("1 minūte", res.getQuantityString(r, 1, 1))
        assertEquals("2 minūtes", res.getQuantityString(r, 2, 2))
        assertEquals("11 minūšu", res.getQuantityString(r, 11, 11))
        assertEquals("21 minūte", res.getQuantityString(r, 21, 21))
    }
}
