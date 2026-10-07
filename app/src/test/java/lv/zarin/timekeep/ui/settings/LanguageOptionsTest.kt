package lv.zarin.timekeep.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class LanguageOptionsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun optionsComeFromLocalesConfig() {
        val options = languageOptions(context)
        assertEquals(listOf<String?>(null, "en", "lv"), options.map { it.tag })
        assertEquals("System default", options.first().label)
    }

    @Test
    fun labelsAreEndonyms() {
        assertEquals(listOf("System default", "English", "Latviešu"), languageOptions(context).map { it.label })
    }

    @Test
    @Config(qualifiers = "lv")
    fun endonymsDoNotChangeWithUiLanguageButSystemDefaultDoes() {
        assertEquals(listOf("Sistēmas valoda", "English", "Latviešu"), languageOptions(context).map { it.label })
    }
}
