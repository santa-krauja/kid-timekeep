package lv.zarin.timekeep.testutil

import android.Manifest
import android.app.Application
import org.robolectric.Shadows.shadowOf

/** Grants POST_NOTIFICATIONS so the first-start permission rationale doesn't intercept Start taps. */
fun Application.allowNotifications() {
    shadowOf(this).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
}
