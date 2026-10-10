package lv.zarin.timekeep.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Test

class TimerPhaseTest {
    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)

    private fun timer(state: RunState) = Timer("id", "t", 60_000L, look, null, state, 0L, 0L)

    @Test
    fun runningBeforeTheEndIsRunning() {
        assertEquals(TimerPhase.Running, timer(RunState.Running(sinceMs = 0, elapsedBeforeMs = 0)).phase(59_999))
    }

    @Test
    fun overdueRunningIsFinished() {
        assertEquals(TimerPhase.Finished, timer(RunState.Running(sinceMs = 0, elapsedBeforeMs = 0)).phase(60_000))
    }

    @Test
    fun pausedIsPausedWhateverTheTime() {
        assertEquals(TimerPhase.Paused, timer(RunState.Paused(elapsedMs = 10_000)).phase(999_999))
    }

    @Test
    fun finishedIsFinished() {
        assertEquals(TimerPhase.Finished, timer(RunState.Finished(finishedAtMs = 5)).phase(0))
    }
}
