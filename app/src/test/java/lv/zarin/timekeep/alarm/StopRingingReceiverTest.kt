package lv.zarin.timekeep.alarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.TestAppContainer
import lv.zarin.timekeep.testutil.allowNotifications
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

@RunWith(AndroidJUnit4::class)
class StopRingingReceiverTest {
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = TestAppContainer(it)
    }
    private val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val finished = Timer(
        "t1",
        "Brush teeth",
        120_000,
        Look(PictureId.STAR, SandColor.SKY, PictureId.HEART, SandColor.PEACH),
        null,
        RunState.Finished(1_000),
        0,
        0
    )

    @Before
    fun setUp() {
        app.allowNotifications()
        runBlocking { app.container.timerRepository.upsert(finished) }
    }

    @After
    fun tearDown() {
        StopRingingReceiver.onHandled = {}
    }

    private fun deliver(intent: Intent) {
        val latch = CountDownLatch(1)
        StopRingingReceiver.onHandled = { latch.countDown() }
        StopRingingReceiver().onReceive(app, intent)
        assertTrue("receiver did not finish", latch.await(10, TimeUnit.SECONDS))
    }

    private fun ring(): Notification {
        Notifications.showTimeUp(app, finished, Settings())
        return shadowOf(nm).getNotification("t1".hashCode())
    }

    private fun assertSilentAndTimerUntouched() {
        val n = shadowOf(nm).allNotifications.single()
        assertEquals(n, shadowOf(nm).getNotification("t1".hashCode()))
        assertEquals(Notifications.CHANNEL_SILENT, n.channelId)
        assertEquals(0, n.flags and Notification.FLAG_INSISTENT)
        assertEquals("Brush teeth", n.extras.getString(Notification.EXTRA_TITLE))
        assertTrue(shadowOf(am).scheduledAlarms.isEmpty())
        assertEquals(finished, runBlocking { app.container.timerRepository.get("t1") })
    }

    @Test
    fun ringTimeoutReplacesRingingWithSilentNotification() {
        ring()
        deliver(shadowOf(shadowOf(am).scheduledAlarms.single().operation).savedIntent)
        assertSilentAndTimerUntouched()
    }

    @Test
    fun stopActionReplacesRingingAndCancelsTimeout() {
        val ringing = ring()
        deliver(shadowOf(ringing.actions.single().actionIntent).savedIntent)
        assertSilentAndTimerUntouched()
    }

    @Test
    fun alreadyClearedNotificationIsNotBroughtBack() {
        val ringing = ring()
        Notifications.cancel(app, "t1")
        deliver(shadowOf(ringing.actions.single().actionIntent).savedIntent)
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }

    @Test
    fun timerNoLongerFinishedIsNotNotifiedAgain() {
        val ringing = ring()
        runBlocking { app.container.timerRepository.upsert(finished.copy(state = RunState.Running(5_000, 0))) }
        deliver(shadowOf(shadowOf(am).scheduledAlarms.single().operation).savedIntent)
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
        assertTrue(shadowOf(am).scheduledAlarms.isEmpty())
        assertEquals(1, ringing.actions.size)
    }

    @Test
    fun foreignIntentIsIgnored() {
        ring()
        deliver(Intent(app, StopRingingReceiver::class.java))
        assertEquals(1, shadowOf(nm).allNotifications.size)
        assertTrue(shadowOf(nm).allNotifications.single().flags and Notification.FLAG_INSISTENT != 0)
    }
}
