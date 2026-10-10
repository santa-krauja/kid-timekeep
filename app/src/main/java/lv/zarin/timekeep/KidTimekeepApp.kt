package lv.zarin.timekeep

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import lv.zarin.timekeep.alarm.Notifications

open class KidTimekeepApp : Application() {
    lateinit var container: AppContainer

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Tests turn this off so the launch reconcile can't race with a container they swap in afterwards. */
    protected open fun reconcileOnLaunch(): Boolean = true

    override fun onCreate() {
        super.onCreate()
        if (!::container.isInitialized) {
            container = AppContainer(this)
        }
        Notifications.ensureChannels(this)
        if (!reconcileOnLaunch()) return
        // The process may have been killed while a timer ran: finish/notify/re-arm from timestamps.
        appScope.launch {
            try {
                AppStartup.reconcile(container, this@KidTimekeepApp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("KidTimekeepApp", "Launch reconcile failed", e)
            }
        }
    }
}
