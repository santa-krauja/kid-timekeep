package lv.zarin.timekeep.ui.hourglass

import android.graphics.drawable.VectorDrawable
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import lv.zarin.timekeep.domain.timer.PictureId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class EmojiResourcesTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun everyPictureHasALoadableVector() {
        PictureId.entries.forEach { id ->
            val d = ContextCompat.getDrawable(context, id.drawableRes())
            assertNotNull("${id.name} drawable", d)
            assertTrue("${id.name} is VectorDrawable", d is VectorDrawable)
            assertTrue("${id.name} width", d!!.intrinsicWidth > 0)
            assertTrue("${id.name} height", d.intrinsicHeight > 0)
        }
    }

    @Test
    fun everyVectorKeepsMostOfItsShapes() {
        val dir = listOf("src/main/res/drawable", "app/src/main/res/drawable")
            .map(::File).first { it.isDirectory }
        PictureId.entries.forEach { id ->
            val f = File(dir, "emoji_${id.name.lowercase()}.xml")
            assertTrue("${f.name} exists", f.isFile)
            val paths = Regex("<path").findAll(f.readText()).count()
            assertTrue("${f.name} has >= 3 paths (was $paths)", paths >= 3)
        }
        assertEquals(16, PictureId.entries.size)
    }
}
