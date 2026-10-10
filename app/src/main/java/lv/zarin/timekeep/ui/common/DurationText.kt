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

/** Spoken-style phrase, e.g. "1 minute 30 seconds". Zero is "0 seconds". */
@Composable
fun durationPhrase(ms: Long, roundUp: Boolean): String {
    val (h, m, s) = splitDuration(ms, roundUp)
    val parts = buildList {
        if (h > 0) add(pluralStringResource(R.plurals.duration_hours, h, h))
        if (m > 0) add(pluralStringResource(R.plurals.duration_minutes, m, m))
        if (s > 0 || (h == 0 && m == 0)) add(pluralStringResource(R.plurals.duration_seconds, s, s))
    }
    return parts.joinToString(" ")
}

/** Visual clock text (m:ss) whose accessibility text is the spoken phrase. */
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

/** Compact "2 min", "30 s", "1 h 30 min". */
@Composable
fun compactDuration(ms: Long): String {
    val (h, m, s) = splitDuration(ms, roundUp = false)
    val parts = buildList {
        if (h > 0) add(stringResource(R.string.duration_short_hour, h))
        if (m > 0) add(stringResource(R.string.duration_short_min, m))
        if (s > 0 || (h == 0 && m == 0)) add(stringResource(R.string.duration_short_sec, s))
    }
    return parts.joinToString(" ")
}
