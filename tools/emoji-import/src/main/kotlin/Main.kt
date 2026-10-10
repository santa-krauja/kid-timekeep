import com.android.ide.common.vectordrawable.Svg2Vector
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.nio.file.Files

// Keep in sync with PictureId in app/.../domain/timer/Look.kt (name, codepoint).
private val EMOJI = listOf(
    "heart" to "2764",
    "star" to "2b50",
    "blossom" to "1f338",
    "smiley" to "1f60a",
    "sun" to "2600",
    "moon" to "1f31d",
    "fish" to "1f41f",
    "cat" to "1f431",
    "dog" to "1f436",
    "car" to "1f697",
    "tree" to "1f333",
    "butterfly" to "1f98b",
    "apple" to "1f34e",
    "rainbow" to "1f308",
    "unicorn" to "1f984",
    "rocket" to "1f680",
)

private const val BASE = "https://raw.githubusercontent.com/googlefonts/noto-emoji/main/2D/svg/"

fun main(args: Array<String>) {
    val outDir = File(args.firstOrNull() ?: "../../app/src/main/res/drawable").also { it.mkdirs() }
    println("REMINDER: the EMOJI list in Main.kt mirrors PictureId (Look.kt). Keep them in sync.")
    val tmp = Files.createTempDirectory("noto-svg").toFile()
    var failed = 0
    for ((name, cp) in EMOJI) {
        val svg = File(tmp, "emoji_u$cp.svg")
        try {
            URI("$BASE${svg.name}").toURL().openStream().use { svg.outputStream().use { o -> it.copyTo(o) } }
            val err = ByteArrayOutputStream()
            val xml = ByteArrayOutputStream()
            val warnings = Svg2Vector.parseSvgToXml(svg.toPath(), xml)
            if (warnings.isNotBlank()) println("[$name] warnings: ${warnings.trim()}")
            val text = xml.toString(Charsets.UTF_8)
            val paths = Regex("<path").findAll(text).count()
            if (paths < 3) { println("[$name] ERROR only $paths paths"); failed++; continue }
            File(outDir, "emoji_$name.xml").writeText(text)
            println("[$name] ok ($paths paths)")
        } catch (e: Exception) {
            println("[$name] FAILED: $e"); failed++
        }
    }
    check(failed == 0) { "$failed emoji failed" }
}
