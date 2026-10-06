package lv.zarin.timekeep.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class TimerActionsTest {
    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)

    private fun run(now: Long = 1_000L, durationMs: Long = 300_000L) =
        TimerActions.newRun("id", "Tea", durationMs, look, "p1", now)

    @Test
    fun newRunStartsRunning() {
        val t = run(5_000)
        assertEquals(RunState.Running(5_000, 0), t.state)
        assertEquals(5_000L, t.createdAtMs)
        assertEquals(5_000L, t.updatedAtMs)
        assertEquals("p1", t.presetId)
    }

    @Test
    fun pauseFreezesElapsed() {
        val p = TimerActions.pause(run(1_000), 31_000)
        assertEquals(RunState.Paused(30_000), p.state)
        assertEquals(31_000L, p.updatedAtMs)
        assertEquals(30_000L, p.elapsedMs(999_999))
    }

    @Test
    fun resumeContinuesFromPaused() {
        val p = TimerActions.pause(run(1_000), 31_000)
        val r = TimerActions.resume(p, 100_000)
        assertEquals(RunState.Running(100_000, 30_000), r.state)
        assertEquals(40_000L, r.elapsedMs(110_000))
    }

    @Test
    fun pauseOnPausedReturnsSameInstance() {
        val p = TimerActions.pause(run(), 5_000)
        assertSame(p, TimerActions.pause(p, 9_000))
    }

    @Test
    fun inapplicableTransitionsReturnSameInstance() {
        val running = run()
        val finished = TimerActions.finish(running, 400_000)
        assertSame(running, TimerActions.resume(running, 2_000))
        assertSame(finished, TimerActions.pause(finished, 500_000))
        assertSame(finished, TimerActions.resume(finished, 500_000))
        assertSame(finished, TimerActions.finish(finished, 500_000))
        val paused = TimerActions.pause(running, 2_000)
        assertSame(paused, TimerActions.finish(paused, 3_000))
    }

    @Test
    fun finishSetsFinished() {
        val f = TimerActions.finish(run(), 400_000)
        assertEquals(RunState.Finished(400_000), f.state)
        assertEquals(400_000L, f.updatedAtMs)
    }

    @Test
    fun restartKeepsLookAndResets() {
        for (start in listOf(run(), TimerActions.pause(run(), 9_000), TimerActions.finish(run(), 9_000))) {
            val r = TimerActions.restart(start, 50_000)
            assertEquals(look, r.look)
            assertEquals(RunState.Running(50_000, 0), r.state)
            assertEquals(50_000L, r.updatedAtMs)
            assertEquals(start.durationMs, r.durationMs)
        }
    }

    @Test
    fun addMinuteWhileRunningAndPaused() {
        val r = TimerActions.addMinute(run(1_000), 11_000)
        assertEquals(360_000L, r.durationMs)
        assertEquals(RunState.Running(1_000, 0), r.state)
        assertEquals(11_000L, r.updatedAtMs)
        val p = TimerActions.addMinute(TimerActions.pause(run(1_000), 11_000), 12_000)
        assertEquals(360_000L, p.durationMs)
        assertEquals(RunState.Paused(10_000), p.state)
    }

    @Test
    fun addMinuteToFinishedRunsOneMoreMinute() {
        val f = TimerActions.finish(run(), 400_000)
        val r = TimerActions.addMinute(f, 500_000)
        assertEquals(RunState.Running(500_000, 300_000), r.state)
        assertEquals(360_000L, r.durationMs)
        assertEquals(60_000L, r.remainingMs(500_000))
    }

    @Test
    fun addMinuteCappedAtMax() {
        val t = run(durationMs = MAX_DURATION_MS - 30_000)
        assertEquals(MAX_DURATION_MS, TimerActions.addMinute(t, 2_000).durationMs)
        val atMax = run(durationMs = MAX_DURATION_MS)
        assertSame(atMax, TimerActions.addMinute(atMax, 2_000))
        val fin = TimerActions.finish(atMax, 3_000)
        assertSame(fin, TimerActions.addMinute(fin, 4_000))
        assertNotSame(atMax, fin)
    }
}
