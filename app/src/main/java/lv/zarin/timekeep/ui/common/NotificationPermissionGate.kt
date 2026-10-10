package lv.zarin.timekeep.ui.common

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import lv.zarin.timekeep.R

private const val PREFS = "notification_prefs"
private const val KEY_ASKED = "notif_asked"

/**
 * Returns a gate: `gate { start() }` runs the block straight away, except the first time on Android 13+ when
 * notifications aren't allowed yet. Then a rationale dialog is shown first, the system request follows on
 * "Allow", and the block runs afterwards either way. We only ever ask once ([KEY_ASKED]).
 * The pending block is not saved across recreation; if the screen is rotated mid-dialog the user taps Start again.
 */
@Composable
fun rememberNotificationPermissionGate(): (onProceed: () -> Unit) -> Unit {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showRationale by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        val proceed = pending
        pending = null
        proceed?.invoke()
    }
    if (showRationale) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.notif_rationale_title)) },
            text = { Text(stringResource(R.string.notif_rationale_text)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        markAsked(context)
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    modifier = Modifier.testTag("notif_allow"),
                ) { Text(stringResource(R.string.action_allow)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        markAsked(context)
                        val proceed = pending
                        pending = null
                        proceed?.invoke()
                    },
                    modifier = Modifier.testTag("notif_not_now"),
                ) { Text(stringResource(R.string.action_not_now)) }
            },
        )
    }
    return { onProceed ->
        if (needsAsking(context)) {
            pending = onProceed
            showRationale = true
        } else {
            onProceed()
        }
    }
}

private fun needsAsking(context: Context): Boolean =
    Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED &&
        !context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ASKED, false)

private fun markAsked(context: Context) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ASKED, true).apply()
}
