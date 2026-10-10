package lv.zarin.timekeep.domain.timer

/** Pure state transitions. Inapplicable transitions return the input instance unchanged. */
object TimerActions {
    fun newRun(
        id: String,
        name: String,
        durationMs: Long,
        look: Look,
        presetId: String?,
        nowMs: Long,
    ): Timer = Timer(
        id = id,
        name = name,
        durationMs = durationMs,
        look = look,
        presetId = presetId,
        state = RunState.Running(sinceMs = nowMs, elapsedBeforeMs = 0L),
        createdAtMs = nowMs,
        updatedAtMs = nowMs,
    )

    fun pause(t: Timer, nowMs: Long): Timer =
        if (t.state is RunState.Running) {
            t.copy(state = RunState.Paused(t.elapsedMs(nowMs)), updatedAtMs = nowMs)
        } else t

    fun resume(t: Timer, nowMs: Long): Timer {
        val s = t.state as? RunState.Paused ?: return t
        return t.copy(state = RunState.Running(sinceMs = nowMs, elapsedBeforeMs = s.elapsedMs), updatedAtMs = nowMs)
    }

    fun restart(t: Timer, nowMs: Long): Timer =
        t.copy(state = RunState.Running(sinceMs = nowMs, elapsedBeforeMs = 0L), updatedAtMs = nowMs)

    fun addMinute(t: Timer, nowMs: Long): Timer {
        val newDuration = minOf(t.durationMs + 60_000L, MAX_DURATION_MS)
        if (newDuration == t.durationMs) return t
        val newState = when (val s = t.state) {
            is RunState.Running, is RunState.Paused -> s
            is RunState.Finished -> RunState.Running(sinceMs = nowMs, elapsedBeforeMs = t.durationMs)
        }
        return t.copy(durationMs = newDuration, state = newState, updatedAtMs = nowMs)
    }

    fun finish(t: Timer, nowMs: Long): Timer =
        if (t.state is RunState.Running) {
            t.copy(state = RunState.Finished(nowMs), updatedAtMs = nowMs)
        } else t
}
