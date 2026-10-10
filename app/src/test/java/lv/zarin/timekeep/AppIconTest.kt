package lv.zarin.timekeep

import android.app.Application
import android.content.Context
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.core.content.res.ResourcesCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = Application::class, qualifiers = "w411dp-h891dp")
class AppIconTest {
    private val context get() = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun launcherIconIsAdaptiveWithMonochromeLayer() {
        for (id in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
            val d = ResourcesCompat.getDrawable(context.resources, id, context.theme)
            assertTrue("icon should be adaptive", d is AdaptiveIconDrawable)
            val adaptive = d as AdaptiveIconDrawable
            assertNotNull(adaptive.foreground)
            assertNotNull(adaptive.background)
            assertNotNull("themed icon needs a monochrome layer", adaptive.monochrome)
        }
    }

    @Test
    fun splashThemeIsDeclared() {
        val id = context.resources.getIdentifier("Theme.KidTimekeep.Starting", "style", context.packageName)
        assertTrue(id != 0)
    }
}
