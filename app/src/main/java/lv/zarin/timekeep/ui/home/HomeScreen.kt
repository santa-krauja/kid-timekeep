package lv.zarin.timekeep.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.window.core.layout.WindowWidthSizeClass
import kotlinx.coroutines.delay
import lv.zarin.timekeep.R
import lv.zarin.timekeep.alarm.Notifications
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.domain.timer.isOverdue
import lv.zarin.timekeep.domain.timer.progress
import lv.zarin.timekeep.domain.timer.remainingMs
import lv.zarin.timekeep.ui.common.appContainer
import lv.zarin.timekeep.ui.common.compactDuration
import lv.zarin.timekeep.ui.common.durationPhrase
import lv.zarin.timekeep.ui.common.rememberNotificationPermissionGate
import lv.zarin.timekeep.ui.hourglass.Hourglass
import lv.zarin.timekeep.ui.timer.TimerContent

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenTimer: (String) -> Unit,
    onNewTimer: () -> Unit,
    onEditPreset: (String) -> Unit = {},
) {
    val container = appContainer()
    val vm: HomeViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    container.timerRepository, container.presetRepository,
                    container.timerService, container.clock, container.controlPolicy,
                    clearNotification = { Notifications.cancel(container.appContext, it) },
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val windowSize = currentWindowAdaptiveInfo().windowSizeClass
    // Phones in landscape are wide but short: they keep the single-pane list and a full-screen timer.
    val twoPane = windowSize.windowWidthSizeClass == WindowWidthSizeClass.EXPANDED &&
        windowSize.windowHeightSizeClass != WindowHeightSizeClass.COMPACT
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val startedId by vm.startedId.collectAsStateWithLifecycle()
    LaunchedEffect(startedId) {
        startedId?.let {
            selectedId = it
            vm.consumeStartedId()
        }
    }
    if (!twoPane) {
        HomeList(vm, state, onOpenSettings, onOpenTimer, onNewTimer, onEditPreset)
        return
    }

    // Two-pane: the list on the left, the selected timer big on the right. Nothing navigates.
    val shownId = selectedId?.takeIf { id -> state.now.any { it.id == id } }
        ?: state.now.firstOrNull { it.state is RunState.Running }?.id
        ?: state.now.firstOrNull { it.state is RunState.Paused }?.id
    val navigator = rememberListDetailPaneScaffoldNavigator<String>()
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        ListDetailPaneScaffold(
            directive = navigator.scaffoldDirective,
            value = navigator.scaffoldValue,
            listPane = {
                AnimatedPane(Modifier.preferredWidth(0.45f)) {
                    HomeList(
                        vm, state, onOpenSettings,
                        onOpenTimer = { selectedId = it },
                        onNewTimer = onNewTimer,
                        onEditPreset = onEditPreset,
                        selectedId = shownId,
                    )
                }
            },
            detailPane = {
                AnimatedPane {
                    if (shownId == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(R.string.home_select_timer),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        key(shownId) {
                            TimerContent(shownId, onClose = { selectedId = null }, showBack = false)
                        }
                    }
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeList(
    vm: HomeViewModel,
    state: HomeState,
    onOpenSettings: () -> Unit,
    onOpenTimer: (String) -> Unit,
    onNewTimer: () -> Unit,
    onEditPreset: (String) -> Unit,
    selectedId: String? = null,
) {
    val container = appContainer()
    val clock = container.clock
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val nowMs by produceState(clock.nowMs(), lifecycle, clock) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = clock.nowMs()
                delay(250)
            }
        }
    }
    val notificationGate = rememberNotificationPermissionGate()
    var menuFor by remember { mutableStateOf<Preset?>(null) }
    var confirmDelete by remember { mutableStateOf<Preset?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.cd_settings))
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTimer) {
                Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.cd_new_timer))
            }
        },
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(150.dp),
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { SectionTitle(stringResource(R.string.home_now)) }
            if (state.now.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyNow() }
            } else {
                items(state.now, key = { it.id }) { timer ->
                    NowCard(
                        timer = timer,
                        nowMs = nowMs,
                        clock = clock,
                        canPause = state.canPause,
                        selected = timer.id == selectedId,
                        onOpen = { onOpenTimer(timer.id) },
                        onToggle = { vm.togglePause(timer.id) },
                        onRestart = { vm.restart(timer.id) },
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(stringResource(R.string.home_presets), Modifier.padding(top = 8.dp))
            }
            items(state.presets, key = { "p" + it.id }, span = { GridItemSpan(maxLineSpan) }) { preset ->
                Box {
                    PresetRow(
                        preset = preset,
                        onStart = { notificationGate { vm.startPreset(preset.id) } },
                        onLongPress = { menuFor = preset },
                    )
                    DropdownMenu(expanded = menuFor?.id == preset.id, onDismissRequest = { menuFor = null }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.preset_menu_edit)) },
                            onClick = { menuFor = null; onEditPreset(preset.id) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.preset_menu_delete)) },
                            onClick = { menuFor = null; confirmDelete = preset },
                        )
                    }
                }
            }
        }
    }

    confirmDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text(stringResource(R.string.preset_delete_title)) },
            text = { Text(stringResource(R.string.preset_delete_message, preset.name)) },
            confirmButton = {
                TextButton(
                    onClick = { vm.deletePreset(preset.id); confirmDelete = null },
                    modifier = Modifier.testTag("confirm_delete"),
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyNow() {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.home_nothing_running), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.home_nothing_running_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NowCard(
    timer: Timer,
    nowMs: Long,
    clock: Clock,
    canPause: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onRestart: () -> Unit,
) {
    val done = timer.state is RunState.Finished || timer.isOverdue(nowMs)
    val paused = timer.state is RunState.Paused
    val cardDescription = stringResource(
        R.string.cd_timer_card, timer.name, durationPhrase(timer.remainingMs(nowMs), roundUp = true),
    )
    Card(
        onClick = onOpen,
        modifier = Modifier.fillMaxWidth(),
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Hourglass(
                    look = timer.look,
                    progress = { timer.progress(clock.nowMs()) },
                    running = !paused && !done,
                    modifier = Modifier.height(96.dp).aspectRatio(0.62f),
                    contentDescription = cardDescription,
                )
                if (!done) {
                    if (canPause || paused) {
                        IconButton(
                            onClick = onToggle,
                            modifier = Modifier.align(Alignment.TopEnd),
                        ) {
                            Icon(
                                if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = stringResource(if (paused) R.string.cd_resume else R.string.cd_pause),
                            )
                        }
                    }
                } else {
                    IconButton(onClick = onRestart, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.cd_restart))
                    }
                }
            }
            Text(
                timer.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (done) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        stringResource(R.string.timer_done_badge),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            } else {
                val label = if (paused) R.string.timer_paused_label else R.string.timer_left_label
                val clockText = lv.zarin.timekeep.domain.format.formatClock(timer.remainingMs(nowMs), roundUp = true)
                Text(
                    stringResource(label, clockText),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetRow(preset: Preset, onStart: () -> Unit, onLongPress: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = {}, onLongClick = onLongPress),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val pinned = preset.pinnedLook
            if (pinned != null) {
                Hourglass(
                    look = pinned,
                    progress = { 0f },
                    running = false,
                    modifier = Modifier.height(32.dp).aspectRatio(0.62f),
                )
            } else {
                Icon(
                    Icons.Rounded.Casino,
                    contentDescription = stringResource(R.string.cd_preset_random_look),
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.size(12.dp))
            Text(
                preset.name,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                compactDuration(preset.durationMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = onStart) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.cd_start_preset, preset.name),
                )
            }
        }
    }
}
