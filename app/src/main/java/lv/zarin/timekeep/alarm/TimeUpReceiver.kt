package lv.zarin.timekeep.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import lv.zarin.timekeep.KidTimekeepApp

/** Fires when a timer's alarm goes off: finishes it (once) and posts the time's-up notification. */
class TimeUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.data?.takeIf { it.scheme == "kidtimekeep" }?.lastPathSegment
        val pending = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                if (timerId != null) handle(app, timerId)
            } finally {
                pending?.finish()
                onHandled()
            }
        }
    }

    private suspend fun handle(app: Context, timerId: String) {
        val container = (app as KidTimekeepApp).container
        val timer = container.timerService.finishIfOverdue(timerId) ?: return
        if (VisibleTimerTracker.visibleTimerId == timer.id) return // visible screen plays in-app feedback
        Notifications.showTimeUp(app, timer, container.settingsRepository.settings.first())
    }

    internal companion object {
        /** Test hook: called after every delivery has been handled. */
        @VisibleForTesting
        @Volatile
        var onHandled: () -> Unit = {}
    }
}
