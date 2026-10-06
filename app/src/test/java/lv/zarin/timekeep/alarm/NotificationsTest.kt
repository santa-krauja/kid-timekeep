package lv.zarin.timekeep.alarm

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.MainActivity
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class NotificationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun grant() = shadowOf(context as android.app.Application)
        .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    private fun timer(id: String = "t1", name: String = "Brush teeth") = Timer(
        id = id, name = name, durationMs = 120_000, look = LOOK, presetId = null,
        state = RunState.Finished(1_000), createdAtMs = 0, updatedAtMs = 0,
    )

    private fun posted(id: String = "t1"): Notification {
        val all = shadowOf(nm).allNotifications
        return shadowOf(nm).getNotification(id.hashCode()) ?: error("not posted: ${all.size}")
    }

    @Test
    fun channelsCreated() {
        Notifications.ensureChannels(context)
        val sound = nm.getNotificationChannel(Notifications.CHANNEL_SOUND)
        val silent = nm.getNotificationChannel(Notifications.CHANNEL_SILENT)
        assertNotNull(sound)
        assertNotNull(silent)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, sound.importance)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, silent.importance)
        assertNotNull(sound.sound)
        assertNull(silent.sound)
        assertTrue(sound.shouldVibrate())
        assertTrue(silent.shouldVibrate())
        assertEquals("Time's up", sound.name.toString())
        assertEquals("Time's up (silent)", silent.name.toString())
    }

    @Test
    fun timeUpNotificationHasNameAndDeepLink() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings())
        val n = posted()
        assertEquals("Brush teeth", n.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Time's up!", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertEquals(Notifications.CHANNEL_SOUND, n.channelId)
        assertEquals(Notification.CATEGORY_ALARM, n.category)
        assertTrue(n.flags and Notification.FLAG_AUTO_CANCEL != 0)
        val intent = shadowOf(n.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals("t1", intent.getStringExtra(MainActivity.EXTRA_TIMER_ID))
    }

    @Test
    fun silentChannelWhenSoundOff() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings(soundOn = false))
        assertEquals(Notifications.CHANNEL_SILENT, posted().channelId)
    }

    @Test
    fun cancelRemovesNotification() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings())
        Notifications.cancel(context, "t1")
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }

    @Test
    fun deniedPermissionPostsNothingAndDoesNotCrash() {
        Notifications.showTimeUp(context, timer(), Settings())
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }

    private companion object {
        val LOOK = Look(PictureId.STAR, SandColor.SKY, PictureId.HEART, SandColor.PEACH)
    }
}
