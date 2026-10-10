package lv.zarin.timekeep.domain.timer

/** Elapsed time, always clamped to [0, durationMs]. Safe against wall-clock jumps. */
fun Timer.elapsedMs(nowMs: Long): Long {
    val raw = when (val s = state) {
        is RunState.Running -> s.elapsedBeforeMs + (nowMs - s.sinceMs)
        is RunState.Paused -> s.elapsedMs
        is RunState.Finished -> durationMs
    }
    return raw.coerceIn(0L, durationMs)
}

fun Timer.remainingMs(nowMs: Long): Long = durationMs - elapsedMs(nowMs)

fun Timer.progress(nowMs: Long): Float =
    if (durationMs <= 0L) 1f else (elapsedMs(nowMs).toDouble() / durationMs).toFloat().coerceIn(0f, 1f)

fun Timer.isOverdue(nowMs: Long): Boolean =
    state is RunState.Running && elapsedMs(nowMs) >= durationMs

/** Wall-clock time at which a running timer ends; null unless Running. */
fun Timer.finishAtMs(): Long? = (state as? RunState.Running)?.let { it.sinceMs + durationMs - it.elapsedBeforeMs }
