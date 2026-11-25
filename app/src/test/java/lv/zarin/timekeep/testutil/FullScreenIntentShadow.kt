package lv.zarin.timekeep.testutil

import android.app.NotificationManager
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowNotificationManager

@Implements(NotificationManager::class)
class FullScreenIntentShadow : ShadowNotificationManager() {
    @Implementation
    fun canUseFullScreenIntent(): Boolean = allowed

    companion object {
        var allowed = true
    }
}

fun allowFullScreen(allowed: Boolean) {
    FullScreenIntentShadow.allowed = allowed
}
