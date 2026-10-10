package lv.zarin.timekeep.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.launch
import lv.zarin.timekeep.R
import lv.zarin.timekeep.ui.common.appContainer
import lv.zarin.timekeep.ui.common.rememberNotificationPermissionGate
import lv.zarin.timekeep.ui.hourglass.Hourglass

/**
 * Wireframe E1: New timer, Edit preset ([presetId]) or Edit a running timer ([timerId]). "Choose pictures & colours" swaps the form
 * for [LookPickerScreen] (E2) in place; system back closes it again.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimerScreen(
    onBack: () -> Unit,
    onStarted: (timerId: String) -> Unit,
    presetId: String? = null,
    timerId: String? = null,
) {
    val target = timerId?.let(EditTarget::RunTimer) ?: presetId?.let(EditTarget::Preset) ?: EditTarget.New
    val container = appContainer()
    val vm: EditTimerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                EditTimerViewModel(
                    target, container.presetRepository, container.favouriteLookRepository,
                    container.timerRepository, container.timerService, container.lookPicker, container.clock, container.controlPolicy,
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var lookOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val isSaving by vm.isSaving.collectAsStateWithLifecycle()
    val notificationGate = rememberNotificationPermissionGate()

    BackHandler(enabled = lookOpen) { lookOpen = false }

    if (lookOpen) {
        LookPickerScreen(
            look = state.look,
            favourites = state.favourites,
            onLookChange = vm::setLook,
            onSaveFavourite = vm::saveLookAsFavourite,
            onDeleteFavourite = vm::deleteFavourite,
            canDelete = state.canDelete,
            onDone = { lookOpen = false },
        )
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (state.target) {
                                EditTarget.New -> R.string.edit_new_title
                                is EditTarget.Preset -> R.string.edit_preset_title
                                is EditTarget.RunTimer -> R.string.edit_timer_title
                            },
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.cd_close))
                    }
                },
            )
        },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when (state.target) {
                    is EditTarget.Preset -> {
                        if (state.canDelete) {
                            OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.action_delete))
                            }
                        }
                        Button(
                            onClick = { scope.launch { if (vm.savePreset()) onBack() } },
                            modifier = Modifier.weight(1f),
                            enabled = state.canEdit && !isSaving,
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                    is EditTarget.RunTimer -> Button(
                        onClick = { scope.launch { if (vm.saveTimer()) onBack() } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.canEdit && !isSaving,
                    ) {
                        Text(stringResource(R.string.action_save))
                    }
                    EditTarget.New -> Button(
                        onClick = { notificationGate { scope.launch { vm.start()?.let(onStarted) } } },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving,
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Text(stringResource(R.string.action_start), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FieldLabel(stringResource(R.string.edit_name_label))
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::setName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = state.nameError,
                supportingText = (@Composable { Text(stringResource(R.string.edit_name_error)) }).takeIf { state.nameError },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
            )

            FieldLabel(stringResource(R.string.edit_duration_label))
            DurationPicker(state.durationMs, vm::setDuration, enabled = state.durationEditable)
            if (!state.durationEditable) {
                Text(
                    stringResource(R.string.edit_duration_pause_first),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.durationTooShort) {
                Text(
                    stringResource(R.string.edit_duration_too_short),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Column(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Hourglass(
                    look = state.look,
                    progress = { PREVIEW_PROGRESS },
                    running = false,
                    modifier = Modifier.height(150.dp).aspectRatio(0.62f),
                    contentDescription = lookDescription(state.look),
                )
                OutlinedButton(onClick = vm::shuffle) {
                    Icon(Icons.Rounded.Casino, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(stringResource(R.string.edit_shuffle), modifier = Modifier.padding(start = 6.dp))
                }
            }

            HorizontalDivider()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { lookOpen = true }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.edit_choose_look), modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null)
            }
            if (state.target !is EditTarget.RunTimer) {
                val isPreset = state.target is EditTarget.Preset
                if (!isPreset) {
                    SwitchRow(
                        text = stringResource(R.string.edit_save_as_preset),
                        checked = state.saveAsPreset,
                        onChange = vm::setSaveAsPreset,
                        leading = { Icon(Icons.Rounded.StarOutline, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    )
                }
                SwitchRow(
                    text = stringResource(R.string.edit_keep_look),
                    checked = state.keepLook,
                    onChange = vm::setKeepLook,
                    enabled = isPreset || state.saveAsPreset,
                    indent = !isPreset,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.preset_delete_title)) },
            text = { Text(stringResource(R.string.preset_delete_message, state.name.trim())) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch { if (vm.deletePreset()) onBack() }
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SwitchRow(
    text: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    indent: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(start = if (indent) 28.dp else 0.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.let {
            it()
            Spacer(Modifier.size(8.dp))
        }
        Text(
            text,
            modifier = Modifier.weight(1f),
            color = if (indent || !enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}
