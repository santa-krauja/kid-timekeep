package lv.zarin.timekeep.alarm

import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.SystemClock
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import lv.zarin.timekeep.MainActivity
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
class NotificationsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun grant() = shadowOf(context as Application)
        .grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

    private fun timer(id: String = "t1", name: String = "Brush teeth") = Timer(
        id = id,
        name = name,
        durationMs = 120_000,
        look = LOOK,
        presetId = null,
        state = RunState.Finished(1_000),
        createdAtMs = 0,
        updatedAtMs = 0
    )

    private fun posted(id: String = "t1"): Notification {
        val all = shadowOf(nm).allNotifications
        return shadowOf(nm).getNotification(id.hashCode()) ?: error("not posted: ${all.size}")
    }

    private fun channel(settings: Settings = Settings()) = nm.getNotificationChannel(Notifications.alarmChannelId(settings))

    private fun alarms() = shadowOf(context.getSystemService(AlarmManager::class.java)).scheduledAlarms.toList()

    @Test
    fun alarmChannelRingsWithDefaultAlarmSoundAtAlarmUsage() {
        Notifications.ensureChannels(context, Settings())
        val alarm = channel()
        assertTrue(alarm.id, alarm.id.matches(Regex("time_up_alarm_v1_[0-9a-f]{8}")))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, alarm.importance)
        assertEquals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM), alarm.sound)
        assertEquals(AudioAttributes.USAGE_ALARM, alarm.audioAttributes.usage)
        assertEquals(AudioAttributes.CONTENT_TYPE_SONIFICATION, alarm.audioAttributes.contentType)
        assertTrue(alarm.shouldVibrate())
        assertFalse(alarm.canBypassDnd())
        assertEquals("Time's up alarm", alarm.name.toString())
        val silent = nm.getNotificationChannel(Notifications.CHANNEL_SILENT)
        assertEquals("time_up_silent", Notifications.CHANNEL_SILENT)
        assertNull(silent.sound)
        assertFalse(silent.shouldVibrate())
        assertEquals("Time's up (silent)", silent.name.toString())
        assertEquals(2, nm.notificationChannels.size)
    }

    @Test
    fun chosenSoundGoesIntoItsOwnChannel() {
        val custom = Settings(alarmSoundUri = "content://media/internal/audio/media/7")
        Notifications.ensureChannels(context, custom)
        assertEquals(Uri.parse("content://media/internal/audio/media/7"), channel(custom).sound)
        assertNotEquals(Notifications.alarmChannelId(Settings()), Notifications.alarmChannelId(custom))
    }

    @Test
    fun soundOffVibrateOnUsesQuietVibratingAlarmChannel() {
        val vibrateOnly = Settings(soundOn = false, vibrateOn = true)
        Notifications.ensureChannels(context, vibrateOnly)
        assertNull(channel(vibrateOnly).sound)
        assertTrue(channel(vibrateOnly).shouldVibrate())
        assertNotEquals(Notifications.alarmChannelId(Settings()), Notifications.alarmChannelId(vibrateOnly))
    }

    @Test
    fun channelIdFollowsSoundAndVibrationChoiceAndOldChannelsAreDeleted() {
        nm.createNotificationChannel(NotificationChannel("time_up", "old", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel("time_up_sound_only", "old", NotificationManager.IMPORTANCE_HIGH))
        nm.createNotificationChannel(NotificationChannel("unrelated", "other", NotificationManager.IMPORTANCE_LOW))
        val choices = listOf(
            Settings(),
            Settings(vibrateOn = false),
            Settings(alarmSoundUri = "content://media/internal/audio/media/7"),
            Settings(soundOn = false)
        )
        val ids = choices.map { settings ->
            Notifications.ensureChannels(context, settings)
            val id = Notifications.alarmChannelId(settings)
            assertEquals(
                setOf(id, Notifications.CHANNEL_SILENT, "unrelated"),
                nm.notificationChannels.map { it.id }.toSet()
            )
            id
        }
        assertEquals(choices.size, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("time_up_alarm_v") })
        assertTrue(Notifications.alarmChannelId(Settings(vibrateOn = false)).startsWith("time_up_alarm_v0_"))
    }

    @Test
    fun oldAlarmChannelWithActiveNotificationSurvivesUntilItIsQuiet() {
        grant()
        val old = Settings()
        val custom = Settings(alarmSoundUri = "content://media/internal/audio/media/7")
        Notifications.showTimeUp(context, timer(), old)
        Notifications.ensureChannels(context, custom)
        assertEquals(
            setOf(Notifications.alarmChannelId(old), Notifications.alarmChannelId(custom), Notifications.CHANNEL_SILENT),
            nm.notificationChannels.map { it.id }.toSet()
        )
        Notifications.cancel(context, "t1")
        Notifications.ensureChannels(context, custom)
        assertEquals(
            setOf(Notifications.alarmChannelId(custom), Notifications.CHANNEL_SILENT),
            nm.notificationChannels.map { it.id }.toSet()
        )
    }

    @Test
    fun bothOffKeepsOnlySilentChannel() {
        Notifications.ensureChannels(context, Settings())
        Notifications.ensureChannels(context, Settings(soundOn = false, vibrateOn = false))
        assertEquals(listOf(Notifications.CHANNEL_SILENT), nm.notificationChannels.map { it.id })
    }

    @Test
    fun refreshKeepsChosenAlarmChannelAndDropsLegacyOnes() {
        val custom = Settings(alarmSoundUri = "content://media/internal/audio/media/7")
        Notifications.ensureChannels(context, custom)
        nm.createNotificationChannel(NotificationChannel("time_up_vibrate_only", "old", NotificationManager.IMPORTANCE_HIGH))
        Notifications.refreshChannels(context)
        assertEquals(
            setOf(Notifications.alarmChannelId(custom), Notifications.CHANNEL_SILENT),
            nm.notificationChannels.map { it.id }.toSet()
        )
        assertEquals(Uri.parse("content://media/internal/audio/media/7"), channel(custom).sound)
    }

    @Test
    fun refreshOnFreshInstallCreatesDefaultAlarmChannel() {
        Notifications.refreshChannels(context)
        assertNotNull(channel())
        assertNotNull(nm.getNotificationChannel(Notifications.CHANNEL_SILENT))
    }

    @Test
    fun ringingNotificationIsInsistentAlarmWithStopAction() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings())
        val n = posted()
        assertEquals(Notifications.alarmChannelId(Settings()), n.channelId)
        assertEquals(Notification.CATEGORY_ALARM, n.category)
        assertEquals(Notification.PRIORITY_MAX, n.priority)
        assertTrue(n.flags and Notification.FLAG_INSISTENT != 0)
        val stop = n.actions.single()
        assertEquals("Stop", stop.title.toString())
        val stopIntent = shadowOf(stop.actionIntent).savedIntent
        assertEquals(StopRingingReceiver::class.java.name, stopIntent.component?.className)
        assertEquals("kidtimekeep://stop-ringing/t1", stopIntent.data.toString())
    }

    @Test
    fun ringingNotificationOpensTimerFullScreenFromAlarm() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings())
        val n = posted()
        val fullScreen = shadowOf(n.fullScreenIntent)
        assertTrue(fullScreen.isActivity)
        val intent = fullScreen.savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals("t1", intent.getStringExtra(MainActivity.EXTRA_TIMER_ID))
        assertTrue(intent.getBooleanExtra(MainActivity.EXTRA_FROM_ALARM, false))
        assertNotEquals(shadowOf(n.contentIntent).savedIntent.data, intent.data)
        assertFalse(shadowOf(n.contentIntent).savedIntent.getBooleanExtra(MainActivity.EXTRA_FROM_ALARM, false))
    }

    @Test
    fun silentNotificationsHaveNoFullScreenIntent() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings(soundOn = false, vibrateOn = false))
        assertNull(posted().fullScreenIntent)
        Notifications.showTimeUp(context, timer(), Settings())
        Notifications.showSilent(context, timer())
        assertNull(posted().fullScreenIntent)
    }

    @Test
    fun isRingingOnlyWhileInsistentNotificationIsPosted() {
        grant()
        assertFalse(Notifications.isRinging(context, "t1"))
        Notifications.showTimeUp(context, timer(), Settings())
        assertTrue(Notifications.isRinging(context, "t1"))
        Notifications.showSilent(context, timer())
        assertFalse(Notifications.isRinging(context, "t1"))
    }

    @Test
    fun ringingSchedulesExactOneMinuteTimeoutOnElapsedClock() {
        grant()
        ShadowAlarmManager.setCanScheduleExactAlarms(true)
        val before = SystemClock.elapsedRealtime()
        Notifications.showTimeUp(context, timer(), Settings())
        val after = SystemClock.elapsedRealtime()
        val timeout = alarms().single()
        assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeout.type)
        assertTrue(timeout.allowWhileIdle)
        assertEquals(0L, timeout.windowLengthMs)
        assertTrue(timeout.triggerAtTime in (before + 60_000)..(after + 60_000))
        val intent = shadowOf(timeout.operation).savedIntent
        assertEquals(StopRingingReceiver::class.java.name, intent.component?.className)
        assertEquals("kidtimekeep://ring-timeout/t1", intent.data.toString())
    }

    @Test
    fun ringTimeoutFallsBackToInexactWhenExactNotAllowed() {
        grant()
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        try {
            Notifications.showTimeUp(context, timer(), Settings())
            val timeout = alarms().single()
            assertEquals(AlarmManager.ELAPSED_REALTIME_WAKEUP, timeout.type)
            assertTrue(timeout.allowWhileIdle)
            assertNotEquals(0L, timeout.windowLengthMs)
        } finally {
            ShadowAlarmManager.setCanScheduleExactAlarms(true)
        }
    }

    @Test
    fun bothOffPostsSilentNotificationWithoutRinging() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings(soundOn = false, vibrateOn = false))
        val n = posted()
        assertEquals(Notifications.CHANNEL_SILENT, n.channelId)
        assertEquals(0, n.flags and Notification.FLAG_INSISTENT)
        assertTrue(n.actions.isNullOrEmpty())
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun silentReplacementKeepsIdAndContent() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings())
        Notifications.showSilent(context, timer())
        val n = posted()
        assertEquals(1, shadowOf(nm).allNotifications.size)
        assertEquals(Notifications.CHANNEL_SILENT, n.channelId)
        assertEquals(0, n.flags and Notification.FLAG_INSISTENT)
        assertEquals("Brush teeth", n.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("t1", shadowOf(n.contentIntent).savedIntent.getStringExtra(MainActivity.EXTRA_TIMER_ID))
    }

    @Test
    @Config(sdk = [30])
    fun notificationAndChannelsFollowAppLocale() {
        grant()
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity {
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.forLanguageTags("lv")
            )
        }
        try {
            // AppCompat persists the locale on a background executor (API < 33), so wait for it.
            val deadline = System.currentTimeMillis() + 5_000
            while (ContextCompat.getContextForLanguage(context)
                    .getString(R.string.notification_time_up) != "Laiks beidzies!" &&
                System.currentTimeMillis() < deadline
            ) {
                Thread.sleep(20)
            }
            Notifications.refreshChannels(context)
            Notifications.showTimeUp(context, timer(), Settings())
            assertEquals("Laiks beidzies!", posted().extras.getString(Notification.EXTRA_TEXT))
            assertEquals("Laiks beidzies – modinātājs", channel().name.toString())
        } finally {
            scenario.onActivity {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.getEmptyLocaleList()
                )
            }
            scenario.close()
        }
    }

    @Test
    @Config(sdk = [30])
    fun channelsAreRenamedWhenActivityRecreatesAfterLocaleSwitch() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        try {
            assertEquals("Time's up alarm", channel().name.toString())
            scenario.onActivity {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags("lv")
                )
            }
            val deadline = System.currentTimeMillis() + 5_000
            while (ContextCompat.getContextForLanguage(context)
                    .getString(R.string.channel_time_up_alarm) != "Laiks beidzies – modinātājs" &&
                System.currentTimeMillis() < deadline
            ) {
                Thread.sleep(20)
            }
            scenario.recreate()
            assertEquals("Laiks beidzies – modinātājs", channel().name.toString())
        } finally {
            scenario.onActivity {
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.getEmptyLocaleList()
                )
            }
            scenario.close()
        }
    }

    @Test
    fun timeUpNotificationHasNameAndDeepLink() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings())
        val n = posted()
        assertEquals("Brush teeth", n.extras.getString(Notification.EXTRA_TITLE))
        assertEquals("Time's up!", n.extras.getCharSequence(Notification.EXTRA_TEXT).toString())
        assertTrue(n.flags and Notification.FLAG_AUTO_CANCEL != 0)
        val intent = shadowOf(n.contentIntent).savedIntent
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals("t1", intent.getStringExtra(MainActivity.EXTRA_TIMER_ID))
    }

    @Test
    fun cancelRemovesNotificationAndRingTimeout() {
        grant()
        Notifications.showTimeUp(context, timer(), Settings())
        Notifications.cancel(context, "t1")
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
        assertTrue(alarms().isEmpty())
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
