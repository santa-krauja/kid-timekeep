package lv.zarin.timekeep.ui.timer

import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.timer.Timer

fun interface TimeUpNotifier {
    fun ring(timer: Timer, settings: Settings, ringForMs: Long)
}
