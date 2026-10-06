package lv.zarin.timekeep.alarm

import android.app.AlarmManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(AndroidJUnit4::class)
class AlarmSchedulerTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scheduler = AndroidAlarmScheduler(context)

    private fun alarms() = shadowOf(am).scheduledAlarms.toList()

    @Test
    fun scheduleSetsExactAlarmAtTime() {
        scheduler.schedule("t1", 123_456L)
        val a = alarms().single()
        assertEquals(AlarmManager.RTC_WAKEUP, a.type)
        assertEquals(123_456L, a.triggerAtTime)
        assertTrue(a.allowWhileIdle)
        val intent = shadowOf(a.operation).savedIntent
        assertEquals(TimeUpReceiver::class.java.name, intent.component?.className)
        assertEquals("kidtimekeep://timer/t1", intent.data.toString())
    }

    @Test
    fun cancelRemovesAlarm() {
        scheduler.schedule("t1", 1_000L)
        scheduler.cancel("t1")
        assertTrue(alarms().isEmpty())
    }

    @Test
    fun rescheduleReplacesAlarmOfSameTimer() {
        scheduler.schedule("t1", 1_000L)
        scheduler.schedule("t1", 2_000L)
        assertEquals(2_000L, alarms().single().triggerAtTime)
    }

    @Test
    fun twoTimersGetDistinctPendingIntentsAndCancelOnlyOne() {
        scheduler.schedule("a", 1_000L)
        scheduler.schedule("b", 2_000L)
        val all = alarms()
        assertEquals(2, all.size)
        assertNotEquals(
            shadowOf(all[0].operation).requestCode, shadowOf(all[1].operation).requestCode,
        )
        assertNotEquals(shadowOf(all[0].operation).savedIntent.data, shadowOf(all[1].operation).savedIntent.data)
        scheduler.cancel("a")
        assertEquals(2_000L, alarms().single().triggerAtTime)
    }

    @Test
    fun fallsBackToInexactWhenExactNotAllowed() {
        ShadowAlarmManager.setCanScheduleExactAlarms(false)
        scheduler.schedule("t1", 5_000L)
        val a = alarms().single()
        assertEquals(5_000L, a.triggerAtTime)
        assertTrue(a.allowWhileIdle)
    }
}
