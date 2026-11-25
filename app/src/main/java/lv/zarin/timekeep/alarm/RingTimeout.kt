package lv.zarin.timekeep.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock

object RingTimeout {
    const val RING_DURATION_MS = 60_000L

    fun schedule(context: Context, timerId: String, ringForMs: Long = RING_DURATION_MS) {
        val pi = pendingIntent(context, timerId, PendingIntent.FLAG_UPDATE_CURRENT) ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val atElapsedMs = SystemClock.elapsedRealtime() + ringForMs
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        try {
            if (canExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsedMs, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsedMs, pi)
            }
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked between the check and the call.
            alarmManager.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsedMs, pi)
        }
    }

    fun cancel(context: Context, timerId: String) {
        val pi = pendingIntent(context, timerId, PendingIntent.FLAG_NO_CREATE) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pi)
        pi.cancel()
    }

    private fun pendingIntent(context: Context, timerId: String, flag: Int): PendingIntent? {
        val intent = Intent(context, StopRingingReceiver::class.java)
            .setData(Uri.parse("kidtimekeep://${StopRingingReceiver.HOST_RING_TIMEOUT}/$timerId"))
        return PendingIntent.getBroadcast(context, timerId.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or flag)
    }
}
