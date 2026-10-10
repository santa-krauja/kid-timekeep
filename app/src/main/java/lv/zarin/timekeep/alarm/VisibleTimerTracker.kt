package lv.zarin.timekeep.alarm

/**
 * Id of the timer whose screen is currently started (visible). The time's-up receiver skips the notification
 * for it, because the visible screen already plays the in-app feedback.
 */
object VisibleTimerTracker {
    @Volatile
    var visibleTimerId: String? = null
}
