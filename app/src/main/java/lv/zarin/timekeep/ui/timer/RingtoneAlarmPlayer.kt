package lv.zarin.timekeep.ui.timer

import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator

class RingtoneAlarmPlayer(private val context: Context) : AlarmPlayer {
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    override fun start(soundUri: String?, sound: Boolean, vibrate: Boolean) {
        stop()
        if (sound) ringtone = playRingtone(soundUri) ?: soundUri?.let { playRingtone(null) }
        if (vibrate) {
            vibrator = context.getSystemService(Vibrator::class.java)?.also {
                runCatching { vibrate(it) }
            }
        }
    }

    override fun stop() {
        runCatching { ringtone?.stop() }
        runCatching { vibrator?.cancel() }
        ringtone = null
        vibrator = null
    }

    private fun vibrate(vibrator: Vibrator) {
        val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            vibrator.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(effect, ALARM_ATTRIBUTES)
        }
    }

    private fun playRingtone(soundUri: String?): Ringtone? = runCatching { createRingtone(soundUri)?.also { it.play() } }.getOrNull()

    private fun createRingtone(soundUri: String?): Ringtone? {
        val uri = soundUri?.let(Uri::parse) ?: defaultAlarmUri()
        return RingtoneManager.getRingtone(context, uri)?.apply {
            audioAttributes = ALARM_ATTRIBUTES
            isLooping = true
        }
    }

    private fun defaultAlarmUri(): Uri? = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    private companion object {
        val VIBRATION_PATTERN = longArrayOf(0, 600, 400)
        val ALARM_ATTRIBUTES: AudioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
    }
}
