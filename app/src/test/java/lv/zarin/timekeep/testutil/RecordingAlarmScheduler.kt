package lv.zarin.timekeep.testutil

import lv.zarin.timekeep.domain.ports.AlarmScheduler

class RecordingAlarmScheduler : AlarmScheduler {
    /** Currently scheduled alarms (id to epoch ms). */
    val scheduled = mutableMapOf<String, Long>()
    val cancelled = mutableListOf<String>()
    var scheduleCalls = 0
        private set

    override fun schedule(timerId: String, atEpochMs: Long) {
        scheduleCalls++
        scheduled[timerId] = atEpochMs
    }

    override fun cancel(timerId: String) {
        cancelled += timerId
        scheduled.remove(timerId)
    }
}
