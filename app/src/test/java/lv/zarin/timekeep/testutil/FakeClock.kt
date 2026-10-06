package lv.zarin.timekeep.testutil

import lv.zarin.timekeep.domain.ports.Clock

class FakeClock(var now: Long = 0L) : Clock {
    override fun nowMs(): Long = now

    fun advance(ms: Long) {
        now += ms
    }
}
