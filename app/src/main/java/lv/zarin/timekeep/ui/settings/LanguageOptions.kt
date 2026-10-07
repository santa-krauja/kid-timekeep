package lv.zarin.timekeep.ui.settings

import android.content.Context
import java.util.Locale
import lv.zarin.timekeep.R
import org.xmlpull.v1.XmlPullParser

/** One row of the language dropdown. `tag == null` means "follow the system language". */
data class LanguageOption(val tag: String?, val label: String)

/**
 * System default first, then every `<locale>` of `res/xml/locales_config.xml`, each labelled in its own
 * language ("English", "Latviešu"), so a language added to the file shows up without any new string.
 */
fun languageOptions(context: Context): List<LanguageOption> {
    val tags = buildList {
        context.resources.getXml(R.xml.locales_config).use { parser ->
            while (parser.next() != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")?.let(::add)
                }
            }
        }
    }
    return listOf(LanguageOption(null, context.getString(R.string.language_system))) +
        tags.map { LanguageOption(it, endonym(it)) }
}

internal fun endonym(tag: String): String {
    val locale = Locale.forLanguageTag(tag)
    return locale.getDisplayName(locale).replaceFirstChar { it.titlecase(locale) }
}
