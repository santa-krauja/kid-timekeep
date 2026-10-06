@file:OptIn(ExperimentalTime::class)

package lv.zarin.timekeep.domain.ports

import kotlin.time.ExperimentalTime
import kotlin.time.Clock as KClock

fun interface Clock {
    fun nowMs(): Long
}

object SystemClock : Clock {
    override fun nowMs(): Long = KClock.System.now().toEpochMilliseconds()
}
