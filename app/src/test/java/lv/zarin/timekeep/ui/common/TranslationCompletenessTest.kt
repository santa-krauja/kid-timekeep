package lv.zarin.timekeep.ui.common

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

/** Plain JVM check that values-lv covers every translatable key in values, with matching placeholders. */
class TranslationCompletenessTest {
    private data class Entry(val type: String, val texts: Map<String, String>, val translatable: Boolean)

    private fun load(dir: String): Map<String, Entry> {
        // Paths are relative to the working directory: Gradle runs unit tests from the module dir
        // (app/), some IDE run configurations use the repo root, so try both.
        val file = listOf(File("src/main/res/$dir/strings.xml"), File("app/src/main/res/$dir/strings.xml"))
            .firstOrNull { it.exists() } ?: error("Missing $dir/strings.xml")
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val out = mutableMapOf<String, Entry>()
        val nodes = root.childNodes
        for (i in 0 until nodes.length) {
            val e = nodes.item(i) as? Element ?: continue
            val texts: Map<String, String> = when (e.tagName) {
                "string" -> mapOf("" to e.textContent)
                "plurals" -> buildMap<String, String> {
                    val items = e.getElementsByTagName("item")
                    for (j in 0 until items.length) {
                        val item = items.item(j) as Element
                        put(item.getAttribute("quantity"), item.textContent)
                    }
                }
                else -> continue
            }
            out[e.getAttribute("name")] = Entry(e.tagName, texts, e.getAttribute("translatable") != "false")
        }
        return out
    }

    private val placeholder = Regex("""%(\d+\$)?[sd]""")

    private fun placeholders(text: String) = placeholder.findAll(text).map { it.value }.sorted().toList()

    @Test
    fun everyTranslatableKeyHasLatvian() {
        val en = load("values")
        val lv = load("values-lv")
        val missing = en.filter { it.value.translatable && it.key !in lv }.keys
        assertTrue("Missing in values-lv: $missing", missing.isEmpty())
        val untranslatable = lv.keys.filter { en[it]?.translatable == false }
        assertTrue("translatable=false keys must not be in values-lv: $untranslatable", untranslatable.isEmpty())
        val extra = lv.keys - en.keys
        assertTrue("Keys only in values-lv: $extra", extra.isEmpty())
    }

    @Test
    fun latvianPlaceholdersAndPluralFormsMatch() {
        val en = load("values")
        val lv = load("values-lv")
        for ((key, l) in lv) {
            val e = en.getValue(key)
            assertEquals("type of $key", e.type, l.type)
            if (l.type == "plurals") {
                assertEquals("LV quantities of $key", setOf("zero", "one", "other"), l.texts.keys)
            }
            val enPh = placeholders(e.texts.getValue("other".takeIf { e.type == "plurals" } ?: ""))
            for ((q, text) in l.texts) assertEquals("placeholders of $key[$q]", enPh, placeholders(text))
        }
    }
}
