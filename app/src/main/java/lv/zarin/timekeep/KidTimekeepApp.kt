package lv.zarin.timekeep

import android.app.Application
import lv.zarin.timekeep.alarm.Notifications

class KidTimekeepApp : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        if (!::container.isInitialized) {
            container = AppContainer(this)
        }
        Notifications.ensureChannels(this)
    }
}
