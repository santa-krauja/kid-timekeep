package lv.zarin.timekeep.ui.timer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.ui.common.compactDuration
import lv.zarin.timekeep.ui.hourglass.drawableRes

/** The reward moment: the finished bottom picture, Again / OK and "Save this look". */
@Composable
fun TimeUpOverlay(
    timer: Timer,
    lookSaved: Boolean,
    onAgain: () -> Unit,
    onOk: () -> Unit,
    onSaveLook: () -> Unit,
) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
        Card {
            Column(
                Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Image(
                    painter = painterResource(timer.look.bottom.drawableRes()),
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                )
                Text(
                    stringResource(R.string.time_up_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                )
                Text(
                    stringResource(R.string.time_up_subtitle, timer.name, compactDuration(timer.durationMs)),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onAgain, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = null)
                        Text(stringResource(R.string.action_again))
                    }
                    Button(onClick = onOk, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
                if (lookSaved) {
                    Text(
                        stringResource(R.string.look_saved),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    TextButton(onClick = onSaveLook) {
                        Icon(Icons.Rounded.StarBorder, contentDescription = null)
                        Text(stringResource(R.string.action_save_look))
                    }
                }
            }
        }
    }
}
