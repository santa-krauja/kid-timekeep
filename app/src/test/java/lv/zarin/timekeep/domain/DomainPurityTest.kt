package lv.zarin.timekeep.domain

import java.io.File
import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

class DomainPurityTest {
    @Test fun domainHasNoPlatformImports() {
        val dir = File("src/main/java/lv/zarin/timekeep/domain")
        assertTrue("domain dir not found: ${dir.absolutePath}", dir.isDirectory)
        val bad = Regex("""^\s*import\s+(android\.|androidx\.(?!annotation\.)|java\.)""")
        val offences = dir.walkTopDown().filter { it.extension == "kt" }.flatMap { f ->
            f.readLines().mapIndexedNotNull { i, l ->
                if (bad.containsMatchIn(l)) "${f.relativeTo(dir)}:${i + 1}: ${l.trim()}" else null
            }
        }.toList()
        if (offences.isNotEmpty()) fail("Platform imports in domain:\n" + offences.joinToString("\n"))
    }
}
