package lv.zarin.timekeep.ui.common

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.format.formatClock
import lv.zarin.timekeep.domain.format.splitDuration

@Composable
fun durationPhrase(ms: Long, roundUp: Boolean): String = joinDuration(
    ms,
    roundUp,
    hours = { pluralStringResource(R.plurals.duration_hours, it, it) },
    minutes = { pluralStringResource(R.plurals.duration_minutes, it, it) },
    seconds = { pluralStringResource(R.plurals.duration_seconds, it, it) },
)

@Composable
fun DurationLabel(
    ms: Long,
    roundUp: Boolean,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
) {
    val phrase = durationPhrase(ms, roundUp)
    Text(
        text = formatClock(ms, roundUp),
        modifier = modifier.semantics { contentDescription = phrase },
        style = style,
    )
}

@Composable
fun compactDuration(ms: Long): String = joinDuration(
    ms,
    roundUp = false,
    hours = { stringResource(R.string.duration_short_hour, it) },
    minutes = { stringResource(R.string.duration_short_min, it) },
    seconds = { stringResource(R.string.duration_short_sec, it) },
)

@Composable
private fun joinDuration(
    ms: Long,
    roundUp: Boolean,
    hours: @Composable (Int) -> String,
    minutes: @Composable (Int) -> String,
    seconds: @Composable (Int) -> String,
): String {
    val (h, m, s) = splitDuration(ms, roundUp)
    return listOfNotNull(
        h.takeIf { it > 0 }?.let { hours(it) },
        m.takeIf { it > 0 }?.let { minutes(it) },
        s.takeIf { it > 0 || (h == 0 && m == 0) }?.let { seconds(it) },
    ).joinToString(" ")
}
