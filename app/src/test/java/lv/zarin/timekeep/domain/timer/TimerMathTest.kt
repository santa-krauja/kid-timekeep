package lv.zarin.timekeep.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerMathTest {
    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)

    private fun timer(state: RunState, durationMs: Long = 300_000L) =
        Timer("id", "t", durationMs, look, null, state, 0L, 0L)

    @Test
    fun elapsedWhileRunning() {
        val t = timer(RunState.Running(sinceMs = 1_000, elapsedBeforeMs = 0))
        assertEquals(60_000L, t.elapsedMs(61_000))
        assertEquals(240_000L, t.remainingMs(61_000))
    }

    @Test
    fun runningIncludesElapsedBefore() {
        val t = timer(RunState.Running(sinceMs = 1_000, elapsedBeforeMs = 30_000))
        assertEquals(90_000L, t.elapsedMs(61_000))
    }

    @Test
    fun pausedAndFinishedElapsed() {
        assertEquals(42_000L, timer(RunState.Paused(42_000)).elapsedMs(999_999))
        assertEquals(300_000L, timer(RunState.Finished(5)).elapsedMs(0))
        assertEquals(0L, timer(RunState.Finished(5)).remainingMs(0))
    }

    @Test
    fun clockMovedBackwardsClampsToZeroElapsed() {
        val t = timer(RunState.Running(sinceMs = 100_000, elapsedBeforeMs = 0))
        assertEquals(0L, t.elapsedMs(50_000))
        assertEquals(300_000L, t.remainingMs(50_000))
        assertEquals(0f, t.progress(50_000), 0f)
    }

    @Test
    fun elapsedNeverExceedsDuration() {
        val t = timer(RunState.Running(sinceMs = 0, elapsedBeforeMs = 0))
        assertEquals(300_000L, t.elapsedMs(10_000_000))
        assertEquals(0L, t.remainingMs(10_000_000))
        assertEquals(1f, t.progress(10_000_000), 0f)
        assertEquals(300_000L, timer(RunState.Paused(999_999_999)).elapsedMs(0))
    }

    @Test
    fun progressIsFractionOfDuration() {
        val t = timer(RunState.Running(0, 0))
        assertEquals(0.5f, t.progress(150_000), 1e-6f)
    }

    @Test
    fun isOverdueOnlyWhenRunningAndElapsed() {
        val t = timer(RunState.Running(0, 0))
        assertFalse(t.isOverdue(299_999))
        assertTrue(t.isOverdue(300_000))
        assertFalse(timer(RunState.Paused(300_000)).isOverdue(0))
        assertFalse(timer(RunState.Finished(0)).isOverdue(0))
    }

    @Test
    fun finishAtMsMatchesRemaining() {
        val t = timer(RunState.Running(sinceMs = 1_000, elapsedBeforeMs = 30_000))
        val now = 20_000L
        assertEquals(now + t.remainingMs(now), t.finishAtMs())
        assertNull(timer(RunState.Paused(0)).finishAtMs())
        assertNull(timer(RunState.Finished(0)).finishAtMs())
    }
}
