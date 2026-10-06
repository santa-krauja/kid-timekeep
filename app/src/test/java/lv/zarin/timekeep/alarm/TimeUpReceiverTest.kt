package lv.zarin.timekeep.alarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import lv.zarin.timekeep.AppContainer
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
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class TimeUpReceiverTest {
    private val clock = FakeClock(0)
    private val app = ApplicationProvider.getApplicationContext<KidTimekeepApp>().also {
        it.container = AppContainer(it, inMemoryDb = true, clock = clock)
    }
    private val nm = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val look = Look(PictureId.STAR, SandColor.SKY, PictureId.HEART, SandColor.PEACH)

    @Before
    fun setUp() {
        app.allowNotifications()
    }

    @After
    fun tearDown() {
        VisibleTimerTracker.visibleTimerId = null
        TimeUpReceiver.onHandled = {}
    }

    private fun seedRunning(id: String, durationMs: Long = 10_000) = runBlocking {
        app.container.timerRepository.upsert(
            Timer(id, "Timer $id", durationMs, look, null, RunState.Running(0, 0), 0, 0),
        )
    }

    private fun deliver(id: String) {
        val latch = CountDownLatch(1)
        TimeUpReceiver.onHandled = { latch.countDown() }
        TimeUpReceiver().onReceive(
            app, Intent(app, TimeUpReceiver::class.java).setData(Uri.parse("kidtimekeep://timer/$id")),
        )
        assertTrue("receiver did not finish", latch.await(10, TimeUnit.SECONDS))
    }

    @Test
    fun receiverFinishesAndNotifiesOnce() {
        seedRunning("t1")
        clock.now = 10_000
        deliver("t1")
        deliver("t1")
        assertEquals(1, shadowOf(nm).allNotifications.size)
        val t = runBlocking { app.container.timerRepository.get("t1") }
        assertTrue(t!!.state is RunState.Finished)
    }

    @Test
    fun earlyFireDoesNotNotify() {
        seedRunning("t1")
        clock.now = 5_000
        deliver("t1")
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }

    @Test
    fun visibleTimerIsFinishedButNotNotified() {
        seedRunning("t1")
        clock.now = 10_000
        VisibleTimerTracker.visibleTimerId = "t1"
        deliver("t1")
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
        val t = runBlocking { app.container.timerRepository.get("t1") }
        assertTrue(t!!.state is RunState.Finished)
    }

    @Test
    fun otherVisibleTimerStillNotifies() {
        seedRunning("t1")
        clock.now = 10_000
        VisibleTimerTracker.visibleTimerId = "other"
        deliver("t1")
        assertEquals(1, shadowOf(nm).allNotifications.size)
    }

    @Test
    fun twoTimersGetDistinctNotifications() {
        seedRunning("a")
        seedRunning("b")
        clock.now = 10_000
        deliver("a")
        deliver("b")
        val n = shadowOf(nm).allNotifications
        assertEquals(2, n.size)
        assertNotEquals(
            shadowOf(n[0].contentIntent).requestCode, shadowOf(n[1].contentIntent).requestCode,
        )
        assertNotEquals(shadowOf(nm).getNotification("a".hashCode()), null)
        assertNotEquals(shadowOf(nm).getNotification("b".hashCode()), null)
    }

    @Test
    fun missingIdOrTimerIsIgnored() {
        deliver("ghost")
        TimeUpReceiver.onHandled = {}
        TimeUpReceiver().onReceive(app, Intent(app, TimeUpReceiver::class.java))
        assertTrue(shadowOf(nm).allNotifications.isEmpty())
    }
}
