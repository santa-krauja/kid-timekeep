package lv.zarin.timekeep.ui.common

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.timer.TimerPhase

data class PauseToggleSpec(
    val icon: ImageVector,
    @param:StringRes val label: Int,
    @param:StringRes val contentDescription: Int,
)

fun pauseToggleSpec(phase: TimerPhase, canPause: Boolean): PauseToggleSpec? = when (phase) {
    TimerPhase.Running -> PauseToggleSpec(Icons.Rounded.Pause, R.string.action_pause, R.string.cd_pause).takeIf { canPause }
    TimerPhase.Paused -> PauseToggleSpec(Icons.Rounded.PlayArrow, R.string.action_go_on, R.string.cd_resume)
    TimerPhase.Finished -> null
}
