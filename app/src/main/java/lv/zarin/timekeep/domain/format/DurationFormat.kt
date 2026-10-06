package lv.zarin.timekeep.domain.format

data class DurationParts(val hours: Int, val minutes: Int, val seconds: Int)

/** Negative input is treated as 0. [roundUp] ceils to whole seconds, otherwise floors. */
fun splitDuration(ms: Long, roundUp: Boolean): DurationParts {
    val clamped = ms.coerceAtLeast(0)
    val total = if (roundUp) (clamped + 999) / 1000 else clamped / 1000
    return DurationParts(
        hours = (total / 3600).toInt(),
        minutes = (total / 60 % 60).toInt(),
        seconds = (total % 60).toInt(),
    )
}

/** `m:ss` under one hour, otherwise `h:mm:ss`. */
fun formatClock(ms: Long, roundUp: Boolean): String {
    val (h, m, s) = splitDuration(ms, roundUp)
    val ss = s.toString().padStart(2, '0')
    return if (h == 0) "$m:$ss" else "$h:${m.toString().padStart(2, '0')}:$ss"
}
