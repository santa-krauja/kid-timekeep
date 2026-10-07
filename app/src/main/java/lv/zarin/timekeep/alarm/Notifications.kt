package lv.zarin.timekeep.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import lv.zarin.timekeep.MainActivity
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.timer.Timer

/**
 * "Time's up" notifications. Notification id = `timerId.hashCode()`.
 *
 * Sound is per channel on Android 8+, so there is one channel with the default sound and one without; the
 * Sound setting picks between them. Vibration is also a channel property and both channels vibrate, so a
 * turned-off Vibrate setting is only honoured when sound is off too ([NotificationCompat.Builder.setSilent]);
 * with sound on and vibrate off the system still vibrates (a limitation of per-channel behaviour).
 */
object Notifications {
    const val CHANNEL_SOUND = "time_up"
    const val CHANNEL_SILENT = "time_up_silent"
    private val VIBRATION = longArrayOf(0, 400, 200, 400)

    /**
     * Context whose resources follow the per-app language. On API 33+ that is already true for any context;
     * below that AppCompat only localises Activity contexts, so the application context needs wrapping.
     */
    private fun localized(context: Context): Context = ContextCompat.getContextForLanguage(context)

    fun ensureChannels(base: Context) {
        val context = localized(base)
        val nm = context.getSystemService(NotificationManager::class.java)
        val sound = NotificationChannel(
            CHANNEL_SOUND, context.getString(R.string.channel_time_up), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            enableVibration(true)
            vibrationPattern = VIBRATION
        }
        val silent = NotificationChannel(
            CHANNEL_SILENT, context.getString(R.string.channel_time_up_silent), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = VIBRATION
        }
        nm.createNotificationChannels(listOf(sound, silent))
    }

    @SuppressLint("MissingPermission") // Checked below; a denied permission just means no notification.
    fun showTimeUp(base: Context, timer: Timer, settings: Settings) {
        val context = localized(base)
        ensureChannels(base)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TIMER_ID, timer.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(
            context, timer.id.hashCode(), open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(
            context, if (settings.soundOn) CHANNEL_SOUND else CHANNEL_SILENT,
        )
            .setSmallIcon(R.drawable.ic_stat_hourglass)
            .setContentTitle(timer.name)
            .setContentText(context.getString(R.string.notification_time_up))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setSilent(!settings.soundOn && !settings.vibrateOn)
            .build()
        NotificationManagerCompat.from(context).notify(timer.id.hashCode(), notification)
    }

    fun cancel(context: Context, timerId: String) {
        NotificationManagerCompat.from(context).cancel(timerId.hashCode())
    }
}
