package lv.zarin.timekeep.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import lv.zarin.timekeep.domain.ports.AlarmScheduler

/** One RTC_WAKEUP alarm per timer, keyed by the data URI `kidtimekeep://timer/<id>`. */
class AndroidAlarmScheduler(private val context: Context) : AlarmScheduler {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    override fun schedule(timerId: String, atEpochMs: Long) {
        val pi = pendingIntent(timerId)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pi)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked between the check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atEpochMs, pi)
        }
    }

    override fun cancel(timerId: String) {
        val pi = pendingIntent(timerId)
        alarmManager.cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(timerId: String): PendingIntent {
        val intent = Intent(context, TimeUpReceiver::class.java).setData(Uri.parse("kidtimekeep://timer/$timerId"))
        return PendingIntent.getBroadcast(
            context, timerId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
