package lv.zarin.timekeep

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import lv.zarin.timekeep.alarm.Notifications
import lv.zarin.timekeep.alarm.VisibleTimerTracker

object AppStartup {
    suspend fun launch(container: AppContainer, context: Context) = launch(
        seed = { container.presetSeeder.seedIfNeeded(container.starterPresetNames()) },
        reconcile = { reconcile(container, context) }
    )

    internal suspend fun launch(seed: suspend () -> Unit, reconcile: suspend () -> Unit) {
        runLogged("Preset seeding failed", seed)
        runLogged("Launch reconcile failed", reconcile)
    }

    private suspend fun runLogged(failure: String, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e("AppStartup", failure, e)
        }
    }

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
