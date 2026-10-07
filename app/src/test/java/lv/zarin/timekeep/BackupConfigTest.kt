package lv.zarin.timekeep

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** The MVP is local-only: nothing is backed up or transferred (restored stale timers and prefs misbehave). */
@RunWith(AndroidJUnit4::class)
class BackupConfigTest {
    @Test
    fun backupIsDisabled() {
        val info = ApplicationProvider.getApplicationContext<Context>().applicationInfo
        assertEquals(0, info.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }
}
