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

    protected open fun reconcileOnLaunch(): Boolean = true

    override fun onCreate() {
        super.onCreate()
        if (!::container.isInitialized) {
            container = DefaultAppContainer(this)
        }
        Notifications.ensureChannels(this)
        if (!reconcileOnLaunch()) return
        appScope.launch {
            try {
                AppStartup.launch(container, this@KidTimekeepApp)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("KidTimekeepApp", "Launch startup failed", e)
            }
        }
    }
}
