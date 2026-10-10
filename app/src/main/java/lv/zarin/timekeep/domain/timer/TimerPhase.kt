package lv.zarin.timekeep.domain.timer

enum class TimerPhase { Running, Paused, Finished }

fun Timer.phase(nowMs: Long): TimerPhase = when (state) {
    is RunState.Running -> if (isOverdue(nowMs)) TimerPhase.Finished else TimerPhase.Running
    is RunState.Paused -> TimerPhase.Paused
    is RunState.Finished -> TimerPhase.Finished
}
