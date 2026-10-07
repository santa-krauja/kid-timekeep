package lv.zarin.timekeep.alarm

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.AppContainer
import lv.zarin.timekeep.AppStartup
import lv.zarin.timekeep.KidTimekeepApp
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.allowNotifications
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class SystemEventsReceiverTest {
    private val clock = FakeClock(0)
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = AppContainer(it, inMemoryDb = true, clock = clock)
    }
    private val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val am = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val look = Look(PictureId.STAR, SandColor.SKY, PictureId.HEART, SandColor.PEACH)

    @Before
    fun setUp() {
        app.allowNotifications()
    }

    @After
    fun tearDown() {
        VisibleTimerTracker.visibleTimerId = null
        SystemEventsReceiver.onHandled = {}
    }

    private fun seedRunning(id: String, durationMs: Long = 10_000) = runBlocking {
        app.container.timerRepository.upsert(
            Timer(id, "Timer $id", durationMs, look, null, RunState.Running(0, 0), 0, 0),
        )
    }

    private fun deliver(action: String) {
        val latch = CountDownLatch(1)
        SystemEventsReceiver.onHandled = { latch.countDown() }
        SystemEventsReceiver().onReceive(app, Intent(action))
        assertTrue("receiver did not finish", latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun bootReschedulesRunning() {
        seedRunning("t1")
        assertTrue(shadowOf(am).scheduledAlarms.isEmpty())
        deliver(Intent.ACTION_BOOT_COMPLETED)
        assertEquals(10_000L, shadowOf(am).scheduledAlarms.single().triggerAtTime)
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }

    @Test
    fun timeSetFinishesOverdueAndNotifies() {
        seedRunning("t1")
        clock.now = 20_000
        deliver(Intent.ACTION_TIME_CHANGED)
        val t = runBlocking { app.container.timerRepository.get("t1") }
        assertTrue(t!!.state is RunState.Finished)
        assertEquals(1, shadowOf(nm).allNotifications.size)
        deliver(Intent.ACTION_TIME_CHANGED)
        assertEquals(1, shadowOf(nm).allNotifications.size)
    }

    @Test
    fun timeSetSkipsNotificationForVisibleTimer() {
        seedRunning("t1")
        clock.now = 20_000
        VisibleTimerTracker.visibleTimerId = "t1"
        deliver(Intent.ACTION_TIMEZONE_CHANGED)
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }

    @Test
    fun exactAlarmPermissionChangeReschedules() {
        seedRunning("t1")
        deliver(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)
        assertEquals(10_000L, shadowOf(am).scheduledAlarms.single().triggerAtTime)
    }

    @Test
    fun packageReplacedReschedules() {
        seedRunning("t1")
        deliver(Intent.ACTION_MY_PACKAGE_REPLACED)
        assertEquals(1, shadowOf(am).scheduledAlarms.size)
    }

    @Test
    fun unknownActionIsIgnored() {
        seedRunning("t1")
        clock.now = 20_000
        SystemEventsReceiver().onReceive(app, Intent("com.example.OTHER"))
        val t = runBlocking { app.container.timerRepository.get("t1") }
        assertTrue(t!!.state is RunState.Running)
    }

    @Test
    fun launchReconcileNotifiesMissedTimers() {
        seedRunning("t1")
        seedRunning("t2", durationMs = 60_000)
        clock.now = 20_000
        runBlocking { AppStartup.reconcile(app.container, app) }
        assertEquals(1, shadowOf(nm).allNotifications.size)
        assertEquals(1, shadowOf(am).scheduledAlarms.size)
        runBlocking { AppStartup.reconcile(app.container, app) }
        assertEquals(1, shadowOf(nm).allNotifications.size)
    }
}
