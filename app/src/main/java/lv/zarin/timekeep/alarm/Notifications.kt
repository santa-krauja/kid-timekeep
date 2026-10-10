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
 * Sound and vibration are channel properties on Android 8+, so there is one channel per combination of the
 * Sound and Vibrate settings and [showTimeUp] picks the matching one. Channels we no longer create are deleted.
 */
object Notifications {
    const val CHANNEL_SOUND_VIBRATE = "time_up"
    const val CHANNEL_SOUND_ONLY = "time_up_sound_only"
    const val CHANNEL_VIBRATE_ONLY = "time_up_vibrate_only"
    const val CHANNEL_SILENT = "time_up_silent"
    private val VIBRATION = longArrayOf(0, 400, 200, 400)

    private class Spec(val id: String, val nameRes: Int, val sound: Boolean, val vibrate: Boolean)

    private val SPECS = listOf(
        Spec(CHANNEL_SOUND_VIBRATE, R.string.channel_time_up, sound = true, vibrate = true),
        Spec(CHANNEL_SOUND_ONLY, R.string.channel_time_up_sound_only, sound = true, vibrate = false),
        Spec(CHANNEL_VIBRATE_ONLY, R.string.channel_time_up_vibrate_only, sound = false, vibrate = true),
        Spec(CHANNEL_SILENT, R.string.channel_time_up_silent, sound = false, vibrate = false),
    )

    /** The channel matching the Sound and Vibrate settings. */
    fun channelFor(settings: Settings): String = when {
        settings.soundOn && settings.vibrateOn -> CHANNEL_SOUND_VIBRATE
        settings.soundOn -> CHANNEL_SOUND_ONLY
        settings.vibrateOn -> CHANNEL_VIBRATE_ONLY
        else -> CHANNEL_SILENT
    }

    /**
     * Context whose resources follow the per-app language. On API 33+ that is already true for any context;
     * below that AppCompat only localises Activity contexts, so the application context needs wrapping.
     */
    private fun localized(context: Context): Context = ContextCompat.getContextForLanguage(context)

    fun ensureChannels(base: Context) {
        val context = localized(base)
        val nm = context.getSystemService(NotificationManager::class.java)
        val channels = SPECS.map { spec ->
            NotificationChannel(spec.id, context.getString(spec.nameRes), NotificationManager.IMPORTANCE_HIGH).apply {
                if (!spec.sound) setSound(null, null)
                enableVibration(spec.vibrate)
                vibrationPattern = if (spec.vibrate) VIBRATION else null
            }
        }
        nm.createNotificationChannels(channels)
        val keep = SPECS.map { it.id }.toSet()
        nm.notificationChannels.map { it.id }.filterNot { it in keep }.forEach(nm::deleteNotificationChannel)
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
            context, channelFor(settings),
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
