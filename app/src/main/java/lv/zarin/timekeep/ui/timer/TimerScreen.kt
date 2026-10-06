package lv.zarin.timekeep.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.delay
import lv.zarin.timekeep.R
import lv.zarin.timekeep.domain.format.formatClock
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.elapsedMs
import lv.zarin.timekeep.domain.timer.progress
import lv.zarin.timekeep.domain.timer.remainingMs
import lv.zarin.timekeep.ui.common.DurationLabel
import lv.zarin.timekeep.ui.common.appContainer
import lv.zarin.timekeep.ui.hourglass.Hourglass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(timerId: String, onBack: () -> Unit) {
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
    if (missing) {
        LaunchedEffect(Unit) { onBack() }
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
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
            )
        },
    ) { padding ->
        val s = state ?: return@Scaffold
        val timer = s.timer
        val paused = timer.state is RunState.Paused
        val finished = timer.state is RunState.Finished
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.weight(1f).fillMaxSize(), contentAlignment = Alignment.Center) {
                Hourglass(
                    look = timer.look,
                    progress = { timer.progress(vm.nowMs()) },
                    running = !paused && !finished,
                    modifier = Modifier.fillMaxHeight(0.95f).aspectRatio(0.62f).alpha(if (paused) 0.55f else 1f),
                    flipTrigger = s.flipTrigger,
                    contentDescription = timer.name,
                )
                if (paused) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
                    ) {
                        Text(
                            stringResource(R.string.paused_badge),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
            if (s.showNumbers) {
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
            Row(
                Modifier.padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = { confirmStartOver = true }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.action_start_over))
                }
                if (s.canPause || paused) {
                    Button(onClick = vm::togglePause) {
                        Icon(if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause, contentDescription = null)
                        Text(stringResource(if (paused) R.string.action_go_on else R.string.action_pause))
                    }
                }
                if (s.showAddMinute) {
                    OutlinedButton(onClick = vm::addMinute, enabled = s.addMinuteEnabled) {
                        Text(stringResource(R.string.action_add_minute), maxLines = 1)
                    }
                }
            }
        }

        if (confirmStartOver) {
            AlertDialog(
                onDismissRequest = { confirmStartOver = false },
                title = { Text(stringResource(R.string.start_over_title)) },
                text = {
                    Text(stringResource(R.string.start_over_message, formatClock(timer.durationMs, roundUp = true)))
                },
                confirmButton = {
                    TextButton(onClick = { confirmStartOver = false; vm.restart() }) {
                        Text(stringResource(R.string.action_ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { confirmStartOver = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                },
            )
        }
        if (finished) {
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
