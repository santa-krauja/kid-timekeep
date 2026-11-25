package lv.zarin.timekeep.ui.timer

interface AlarmPlayer {
    fun start(soundUri: String?, sound: Boolean, vibrate: Boolean)

    fun stop()
}
