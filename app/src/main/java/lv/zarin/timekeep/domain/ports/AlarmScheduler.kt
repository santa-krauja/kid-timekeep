package lv.zarin.timekeep.domain.ports

interface AlarmScheduler {
    fun schedule(timerId: String, atEpochMs: Long)
    fun cancel(timerId: String)
}
