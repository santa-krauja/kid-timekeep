package lv.zarin.timekeep.alarm

import lv.zarin.timekeep.domain.ports.AlarmScheduler

/** Temporary scheduler until the AlarmManager adapter lands (Task 21). */
object NoOpAlarmScheduler : AlarmScheduler {
    override fun schedule(timerId: String, atEpochMs: Long) = Unit
    override fun cancel(timerId: String) = Unit
}
