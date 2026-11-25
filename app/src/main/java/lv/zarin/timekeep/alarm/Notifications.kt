package lv.zarin.timekeep.alarm

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import lv.zarin.timekeep.MainActivity
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.ports.TimerRepository
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer

object Notifications {
    const val CHANNEL_SILENT = "time_up_silent"
    private const val CHANNEL_PREFIX = "time_up"
    private const val ALARM_CHANNEL_PREFIX = "time_up_alarm_"
    private val VIBRATION = longArrayOf(0, 800, 600)
    private val ALARM_AUDIO = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun notificationId(timerId: String): Int = timerId.hashCode()

    // A channel's sound and vibration are fixed once created, so the id embeds both choices:
    // changing either yields a new channel, and the old one is deleted.
    fun alarmChannelId(settings: Settings): String {
        val vibration = if (settings.vibrateOn) 1 else 0
        val sound = if (settings.soundOn) settings.alarmSoundUri ?: "default" else "off"
        return "${ALARM_CHANNEL_PREFIX}v${vibration}_${"%08x".format(sound.hashCode())}"
    }

    private fun rings(settings: Settings) = settings.soundOn || settings.vibrateOn

    // Below API 33 AppCompat only localises Activity contexts, so other contexts need wrapping.
    private fun localized(context: Context): Context = ContextCompat.getContextForLanguage(context)

    fun ensureChannels(base: Context, settings: Settings) {
        val context = localized(base)
        val nm = context.getSystemService(NotificationManager::class.java)
        val channels = listOfNotNull(silentChannel(context), if (rings(settings)) alarmChannel(context, settings) else null)
        nm.createNotificationChannels(channels)
        deleteChannelsExcept(nm, channels.map { it.id }.toSet())
    }

    fun refreshChannels(base: Context) {
        val context = localized(base)
        val nm = context.getSystemService(NotificationManager::class.java)
        val alarms = nm.notificationChannels.filter { it.id.startsWith(ALARM_CHANNEL_PREFIX) }
            .onEach { it.name = context.getString(R.string.channel_time_up_alarm) }
            .ifEmpty { listOf(alarmChannel(context, Settings())) }
        val channels = alarms + silentChannel(context)
        nm.createNotificationChannels(channels)
        deleteChannelsExcept(nm, channels.map { it.id }.toSet())
    }

    private fun deleteChannelsExcept(nm: NotificationManager, keep: Set<String>) {
        // Deleting a channel cancels its notifications, which would silence a ringing alarm.
        val inUse = nm.activeNotifications.map { it.notification.channelId }.toSet()
        nm.notificationChannels.map { it.id }
            .filter { it.startsWith(CHANNEL_PREFIX) && it !in keep && it !in inUse }
            .forEach(nm::deleteNotificationChannel)
    }

    private fun silentChannel(context: Context) = NotificationChannel(
        CHANNEL_SILENT,
        context.getString(R.string.channel_time_up_silent),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        setSound(null, null)
        enableVibration(false)
        vibrationPattern = null
    }

    private fun alarmChannel(context: Context, settings: Settings) = NotificationChannel(
        alarmChannelId(settings),
        context.getString(R.string.channel_time_up_alarm),
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        setSound(if (settings.soundOn) soundUri(settings) else null, ALARM_AUDIO)
        enableVibration(settings.vibrateOn)
        vibrationPattern = if (settings.vibrateOn) VIBRATION else null
        setBypassDnd(false)
    }

    private fun soundUri(settings: Settings): Uri? = settings.alarmSoundUri?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private fun canPost(context: Context) = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // Checked in canPost; a denied permission just means no notification.
    fun showTimeUp(base: Context, timer: Timer, settings: Settings, ringForMs: Long = RingTimeout.RING_DURATION_MS) {
        val context = localized(base)
        ensureChannels(base, settings)
        if (!canPost(context)) return
        if (!rings(settings)) {
            post(context, timer.id, silentBuilder(context, timer).build())
            return
        }
        val notification = builder(context, timer, alarmChannelId(settings))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(R.drawable.ic_stat_hourglass, context.getString(R.string.action_stop), stopIntent(context, timer.id))
            .setFullScreenIntent(fullScreenIntent(context, timer.id), true)
            .build()
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        post(context, timer.id, notification)
        RingTimeout.schedule(context, timer.id, ringForMs)
    }

    @SuppressLint("MissingPermission") // Checked in canPost; a denied permission just means no notification.
    fun showSilent(base: Context, timer: Timer) {
        val context = localized(base)
        if (!canPost(context)) return
        post(context, timer.id, silentBuilder(context, timer).build())
    }

    @SuppressLint("MissingPermission") // Callers check canPost.
    private fun post(context: Context, timerId: String, notification: Notification) {
        NotificationManagerCompat.from(context).notify(notificationId(timerId), notification)
    }

    private fun silentBuilder(context: Context, timer: Timer) = builder(context, timer, CHANNEL_SILENT)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setSilent(true)

    private fun builder(context: Context, timer: Timer, channelId: String) = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_stat_hourglass)
        .setContentTitle(timer.name)
        .setContentText(context.getString(R.string.notification_time_up))
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setAutoCancel(true)
        .setContentIntent(openIntent(context, timer.id))

    private fun openIntent(context: Context, timerId: String): PendingIntent {
        val open = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_TIMER_ID, timerId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            timerId.hashCode(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun fullScreenIntent(context: Context, timerId: String): PendingIntent {
        val open = Intent(context, MainActivity::class.java)
            .setData(Uri.parse("kidtimekeep://time-up/$timerId"))
            .putExtra(MainActivity.EXTRA_TIMER_ID, timerId)
            .putExtra(MainActivity.EXTRA_FROM_ALARM, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            timerId.hashCode(),
            open,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun stopIntent(context: Context, timerId: String): PendingIntent {
        val stop = Intent(context, StopRingingReceiver::class.java)
            .setData(Uri.parse("kidtimekeep://${StopRingingReceiver.HOST_STOP}/$timerId"))
        return PendingIntent.getBroadcast(
            context,
            timerId.hashCode(),
            stop,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun isRinging(context: Context, timerId: String): Boolean =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any {
            it.id == notificationId(timerId) && it.notification.flags and Notification.FLAG_INSISTENT != 0
        }

    suspend fun stopRinging(context: Context, timers: TimerRepository, timerId: String) {
        RingTimeout.cancel(context, timerId)
        if (!isPosted(context, timerId)) return
        val timer = timers.get(timerId)
        if (timer?.state is RunState.Finished) showSilent(context, timer) else cancel(context, timerId)
    }

    private fun isPosted(context: Context, timerId: String) =
        context.getSystemService(NotificationManager::class.java).activeNotifications.any { it.id == notificationId(timerId) }

    fun cancel(context: Context, timerId: String) {
        RingTimeout.cancel(context, timerId)
        NotificationManagerCompat.from(context).cancel(notificationId(timerId))
    }
}
