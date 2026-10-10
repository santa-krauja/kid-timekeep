package lv.zarin.timekeep.ui.timer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowHeightSizeClass
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import lv.zarin.timekeep.R
import lv.zarin.timekeep.alarm.Notifications
import lv.zarin.timekeep.alarm.VisibleTimerTracker
import lv.zarin.timekeep.domain.format.formatClock
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.domain.timer.TimerPhase
import lv.zarin.timekeep.domain.timer.elapsedMs
import lv.zarin.timekeep.domain.timer.progress
import lv.zarin.timekeep.domain.timer.remainingMs
import lv.zarin.timekeep.ui.common.DurationLabel
import lv.zarin.timekeep.ui.common.appContainer
import lv.zarin.timekeep.ui.common.durationPhrase
import lv.zarin.timekeep.ui.common.pauseToggleSpec
import lv.zarin.timekeep.ui.hourglass.Hourglass

@Composable
fun TimerScreen(timerId: String, onBack: () -> Unit) = TimerContent(timerId, onClose = onBack, showBack = true)

/**
 * The big timer. Used as the phone route (`showBack = true`) and as the tablet detail pane
 * (`showBack = false`); [onClose] also runs when the timer is dismissed or no longer exists.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerContent(timerId: String, onClose: () -> Unit, showBack: Boolean) {
    val landscape = currentWindowAdaptiveInfo().windowSizeClass.windowHeightSizeClass == WindowHeightSizeClass.COMPACT
    val container = appContainer()
    val vm: TimerViewModel = viewModel(
        key = "timer-$timerId",
        factory = viewModelFactory {
            initializer {
                TimerViewModel(
                    timerId, container.timerRepository, container.timerService,
                    container.favouriteLookRepository, container.settingsRepository,
                    container.clock, container.controlPolicy,
                    AndroidTimeUpFeedback(container.appContext),
                    clearNotification = { Notifications.cancel(container.appContext, it) },
                )
            }
        },
    )
    val state by vm.state.collectAsStateWithLifecycle()
    val missing by vm.missing.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { vm.runTicker() }
    }
    DisposableEffect(lifecycle, timerId) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> VisibleTimerTracker.visibleTimerId = timerId
                Lifecycle.Event.ON_STOP ->
                    if (VisibleTimerTracker.visibleTimerId == timerId) VisibleTimerTracker.visibleTimerId = null
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (VisibleTimerTracker.visibleTimerId == timerId) VisibleTimerTracker.visibleTimerId = null
        }
    }
    if (state?.phase == TimerPhase.Finished) {
        LaunchedEffect(timerId) { vm.onFinishedShown() }
    }
    if (missing) {
        LaunchedEffect(Unit) { onClose() }
    }
    val nowMs by produceState(vm.nowMs(), lifecycle, vm) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                value = vm.nowMs()
                delay(250)
            }
        }
    }
    val keepOn = state?.keepScreenOn == true
    val view = LocalView.current
    DisposableEffect(view, keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }

    var confirmStartOver by remember { mutableStateOf(false) }
    var lookSaved by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(state?.timer?.name.orEmpty()) },
                navigationIcon = {
                    if (showBack) {
                        IconButton(onClick = onClose) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                        }
                    }
                },
            )
        },
    ) { padding ->
        val s = state ?: return@Scaffold
        val timer = s.timer
        val hourglass: @Composable (Modifier) -> Unit = { mod ->
            TimerHourglass(s, progress = { timer.progress(vm.nowMs()) }, description = bigDescription(s, nowMs), modifier = mod)
        }
        val numbers: @Composable () -> Unit = { if (s.showNumbers) TimerNumbers(timer, nowMs) }
        val controls: @Composable () -> Unit = {
            TimerControlsRow(
                phase = s.phase,
                controls = s.controls,
                onStartOver = { confirmStartOver = true },
                onTogglePause = vm::togglePause,
                onAddMinute = vm::addMinute,
            )
        }
        if (landscape) {
            Row(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                hourglass(Modifier.weight(1f).fillMaxHeight())
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    numbers()
                    controls()
                }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                hourglass(Modifier.weight(1f).fillMaxSize())
                numbers()
                controls()
            }
        }

        if (confirmStartOver) {
            StartOverDialog(
                durationMs = timer.durationMs,
                onConfirm = { confirmStartOver = false; vm.restart() },
                onDismiss = { confirmStartOver = false },
            )
        }
        if (s.phase == TimerPhase.Finished) {
            TimeUpOverlay(
                timer = timer,
                lookSaved = lookSaved,
                onAgain = { lookSaved = false; vm.again() },
                onOk = vm::dismiss,
                onSaveLook = { lookSaved = true; vm.saveLookAsFavourite() },
            )
        }
    }
}

@Composable
private fun bigDescription(s: TimerUiState, nowMs: Long): String = when (s.phase) {
    TimerPhase.Finished -> stringResource(R.string.cd_timer_done, s.timer.name)
    TimerPhase.Running -> timeLeftDescription(s.timer, nowMs)
    TimerPhase.Paused -> timeLeftDescription(s.timer, nowMs) + stringResource(R.string.cd_paused_suffix)
}

@Composable
private fun timeLeftDescription(timer: Timer, nowMs: Long): String = stringResource(
    R.string.cd_timer_big,
    timer.name,
    durationPhrase(timer.remainingMs(nowMs), roundUp = true),
    durationPhrase(timer.durationMs, roundUp = true),
)

@Composable
private fun TimerHourglass(s: TimerUiState, progress: () -> Float, description: String, modifier: Modifier) {
    val paused = s.phase == TimerPhase.Paused
    Box(modifier, contentAlignment = Alignment.Center) {
        Hourglass(
            look = s.timer.look,
            progress = progress,
            running = s.phase == TimerPhase.Running,
            modifier = Modifier.fillMaxHeight(0.95f).aspectRatio(0.62f).alpha(if (paused) 0.55f else 1f),
            flipTrigger = s.flipTrigger,
            contentDescription = description,
            announceDescription = s.phase == TimerPhase.Finished,
        )
        if (paused) PausedBadge()
    }
}

@Composable
private fun PausedBadge() {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
    ) {
        Text(
            stringResource(R.string.paused_badge),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

@Composable
private fun TimerNumbers(timer: Timer, nowMs: Long) {
    Text(
        stringResource(R.string.timer_left),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    DurationLabel(
        ms = timer.remainingMs(nowMs),
        roundUp = true,
        style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.ExtraBold),
    )
    Text(
        stringResource(
            R.string.timer_passed_of,
            formatClock(timer.elapsedMs(nowMs), roundUp = false),
            formatClock(timer.durationMs, roundUp = true),
        ),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimerControlsRow(
    phase: TimerPhase,
    controls: TimerControls,
    onStartOver: () -> Unit,
    onTogglePause: () -> Unit,
    onAddMinute: () -> Unit,
) {
    FlowRow(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        if (phase == TimerPhase.Finished) return@FlowRow
        OutlinedButton(onClick = onStartOver) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_start_over))
        }
        pauseToggleSpec(phase, controls.canPause)?.let { toggle ->
            Button(onClick = onTogglePause) {
                Icon(toggle.icon, contentDescription = null)
                Text(stringResource(toggle.label))
            }
        }
        if (controls.showAddMinute) {
            OutlinedButton(onClick = onAddMinute, enabled = controls.addMinuteEnabled) {
                Text(stringResource(R.string.action_add_minute), maxLines = 1)
            }
        }
    }
}

@Composable
private fun StartOverDialog(durationMs: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.start_over_title)) },
        text = { Text(stringResource(R.string.start_over_message, formatClock(durationMs, roundUp = true))) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
