package lv.zarin.timekeep.alarm

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lv.zarin.timekeep.AppStartup
import lv.zarin.timekeep.KidTimekeepApp

/**
 * Re-syncs alarms after reboot, app update, clock/timezone changes and exact-alarm permission changes
 * (AlarmManager drops alarms on reboot/update), finishing timers that became overdue meanwhile.
 */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in ACTIONS) return
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                AppStartup.reconcile((app as KidTimekeepApp).container, app)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Never crash from a background broadcast; the next launch's reconcile() recovers.
                Log.e(TAG, "Reconcile failed for ${intent.action}", e)
            } finally {
                pending?.finish()
                onHandled()
            }
        }
    }

    internal companion object {
        private const val TAG = "SystemEventsReceiver"

        private val ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
        )

        /** Test hook: called after every handled delivery. */
        @VisibleForTesting
        @Volatile
        var onHandled: () -> Unit = {}
    }
}
