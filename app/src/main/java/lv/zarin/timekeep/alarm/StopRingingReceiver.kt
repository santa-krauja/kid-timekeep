package lv.zarin.timekeep.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lv.zarin.timekeep.KidTimekeepApp

class StopRingingReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.data
            ?.takeIf { it.scheme == "kidtimekeep" && it.host in HOSTS }
            ?.lastPathSegment
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (timerId != null) Notifications.stopRinging(app, (app as KidTimekeepApp).container.timerRepository, timerId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Stop ringing failed", e)
            } finally {
                pending?.finish()
                onHandled()
            }
        }
    }

    internal companion object {
        const val HOST_STOP = "stop-ringing"
        const val HOST_RING_TIMEOUT = "ring-timeout"
        private val HOSTS = setOf(HOST_STOP, HOST_RING_TIMEOUT)
        private const val TAG = "StopRingingReceiver"

        @VisibleForTesting
        @Volatile
        var onHandled: () -> Unit = {}
    }
}
