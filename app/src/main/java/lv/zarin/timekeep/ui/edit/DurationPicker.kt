package lv.zarin.timekeep.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.format.splitDuration
import lv.zarin.timekeep.domain.timer.MAX_DURATION_MS
import lv.zarin.timekeep.domain.timer.QUICK_DURATIONS_MS
import lv.zarin.timekeep.ui.common.compactDuration

private const val MAX_HOURS = 4

/**
 * Quick chips from [QUICK_DURATIONS_MS] plus "h:m:s…", which opens hour / minute / second pickers.
 * The caller clamps the result to the allowed range.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationPicker(durationMs: Long, onDurationChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    var customOpen by rememberSaveable { mutableStateOf(false) }
    val isCustom = durationMs !in QUICK_DURATIONS_MS
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (ms in QUICK_DURATIONS_MS) {
            FilterChip(
                selected = durationMs == ms,
                onClick = { onDurationChange(ms) },
                label = { Text(compactDuration(ms)) },
            )
        }
        FilterChip(
            selected = isCustom,
            onClick = { customOpen = true },
            label = {
                Text(if (isCustom) compactDuration(durationMs) else stringResource(R.string.edit_duration_custom))
            },
        )
    }
    if (customOpen) {
        CustomDurationDialog(
            initialMs = durationMs,
            onDismiss = { customOpen = false },
            onConfirm = {
                customOpen = false
                onDurationChange(it)
            },
        )
    }
}

@Composable
private fun CustomDurationDialog(initialMs: Long, onDismiss: () -> Unit, onConfirm: (Long) -> Unit) {
    val parts = splitDuration(initialMs.coerceAtMost(MAX_DURATION_MS), roundUp = false)
    var h by rememberSaveable { mutableIntStateOf(parts.hours) }
    var m by rememberSaveable { mutableIntStateOf(parts.minutes) }
    var s by rememberSaveable { mutableIntStateOf(parts.seconds) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_duration_label)) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                NumberStepper(stringResource(R.string.edit_duration_hours), h, MAX_HOURS) { h = it }
                NumberStepper(stringResource(R.string.edit_duration_minutes), m, 59) { m = it }
                NumberStepper(stringResource(R.string.edit_duration_seconds), s, 59) { s = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(h * 3_600_000L + m * 60_000L + s * 1_000L) }) {
                Text(stringResource(R.string.action_ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** 0..[max], wrapping around at both ends. */
@Composable
private fun NumberStepper(label: String, value: Int, max: Int, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = { onChange(if (value >= max) 0 else value + 1) }) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = stringResource(R.string.cd_increase, label))
        }
        Text(
            value.toString().padStart(2, '0'),
            modifier = Modifier.semantics { contentDescription = "$label $value" },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        IconButton(onClick = { onChange(if (value <= 0) max else value - 1) }) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.cd_decrease, label))
        }
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
