package lv.zarin.timekeep.ui.timer

import android.content.Context
import android.media.RingtoneManager
import android.os.VibrationEffect
import android.os.Vibrator

/** In-app sound and vibration played once when a timer finishes while the screen is open. */
interface TimeUpFeedback {
    fun play(sound: Boolean, vibrate: Boolean)
}

class AndroidTimeUpFeedback(private val context: Context) : TimeUpFeedback {
    override fun play(sound: Boolean, vibrate: Boolean) {
        if (sound) {
            runCatching {
                RingtoneManager.getRingtone(
                    context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                )?.play()
            }
        }
        if (vibrate) {
            runCatching {
                val vibrator = context.getSystemService(Vibrator::class.java)
                vibrator?.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
