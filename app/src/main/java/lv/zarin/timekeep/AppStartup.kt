package lv.zarin.timekeep

import android.content.Context
import kotlinx.coroutines.flow.first
import lv.zarin.timekeep.alarm.Notifications
import lv.zarin.timekeep.alarm.VisibleTimerTracker

/** Shared reconcile entry point for app launch and system broadcasts. */
object AppStartup {
    suspend fun launch(container: AppContainer, context: Context) {
        container.presetSeeder.seedIfNeeded(container.starterPresetNames())
        reconcile(container, context)
    }

    /**
     * Finishes overdue timers, re-arms alarms of the running ones, and posts a time's-up notification for each
     * timer finished by this call (except the one whose screen is visible).
     */
    suspend fun reconcile(container: AppContainer, context: Context) {
        val finished = container.timerService.reconcile()
        if (finished.isEmpty()) return
        val settings = container.settingsRepository.settings.first()
        for (timer in finished) {
            if (VisibleTimerTracker.visibleTimerId == timer.id) continue
            Notifications.showTimeUp(context, timer, settings)
        }
    }
}
