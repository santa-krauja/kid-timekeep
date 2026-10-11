package lv.zarin.timekeep.testutil

import android.Manifest
import android.app.Application
import org.robolectric.Shadows.shadowOf

fun Application.allowNotifications() {
    shadowOf(this).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
}
