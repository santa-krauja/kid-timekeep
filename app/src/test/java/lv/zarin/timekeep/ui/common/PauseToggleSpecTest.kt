package lv.zarin.timekeep.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.timer.TimerPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PauseToggleSpecTest {
    @Test
    fun runningOffersPauseWhenAllowed() {
        assertEquals(
            PauseToggleSpec(Icons.Rounded.Pause, R.string.action_pause, R.string.cd_pause),
            pauseToggleSpec(TimerPhase.Running, canPause = true),
        )
    }

    @Test
    fun runningOffersNothingWhenPauseDenied() {
        assertNull(pauseToggleSpec(TimerPhase.Running, canPause = false))
    }

    @Test
    fun pausedAlwaysOffersResume() {
        val resume = PauseToggleSpec(Icons.Rounded.PlayArrow, R.string.action_go_on, R.string.cd_resume)
        assertEquals(resume, pauseToggleSpec(TimerPhase.Paused, canPause = true))
        assertEquals(resume, pauseToggleSpec(TimerPhase.Paused, canPause = false))
    }

    @Test
    fun finishedOffersNothing() {
        assertNull(pauseToggleSpec(TimerPhase.Finished, canPause = true))
    }
}
