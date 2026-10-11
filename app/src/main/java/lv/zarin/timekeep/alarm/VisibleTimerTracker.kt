package lv.zarin.timekeep.alarm

object VisibleTimerTracker {
    @Volatile
    var visibleTimerId: String? = null
}
