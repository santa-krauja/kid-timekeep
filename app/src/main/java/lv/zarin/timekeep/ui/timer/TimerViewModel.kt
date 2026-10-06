package lv.zarin.timekeep.ui.timer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.ports.SettingsRepository
import lv.zarin.timekeep.domain.ports.TimerRepository
import lv.zarin.timekeep.domain.timer.MAX_DURATION_MS
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer

data class TimerUiState(
    val timer: Timer,
    val showNumbers: Boolean,
    val keepScreenOn: Boolean,
    val canPause: Boolean,
    val canAddMinute: Boolean,
    val flipTrigger: Int,
)

class TimerViewModel(
    private val timerId: String,
    timers: TimerRepository,
    private val service: TimerService,
    private val favourites: FavouriteLookRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
    private val policy: ControlPolicy = AllowAllControlPolicy,
    private val feedback: TimeUpFeedback,
) : ViewModel() {

    private val flip = MutableStateFlow(0)

    /** Null until the first load; then the timer (or null inside when it no longer exists). */
    private data class Loaded(val timer: Timer?, val settings: Settings, val flip: Int)

    private val loaded: StateFlow<Loaded?> = combine(
        timers.observeAll().map { all -> all.firstOrNull { it.id == timerId } },
        settings.settings,
        flip,
    ) { t, s, f -> Loaded(t, s, f) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val state: StateFlow<TimerUiState?> = loaded.map { l ->
        val t = l?.timer ?: return@map null
        TimerUiState(
            timer = t,
            showNumbers = l.settings.showNumbers,
            keepScreenOn = l.settings.keepScreenOn && t.state !is RunState.Finished,
            canPause = policy.isAllowed(Control.PAUSE),
            canAddMinute = policy.isAllowed(Control.ADD_MINUTE) && t.durationMs < MAX_DURATION_MS,
            flipTrigger = l.flip,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** True once loaded and the timer is gone (dismissed here or elsewhere). */
    val missing: StateFlow<Boolean> = loaded.map { it != null && it.timer == null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun nowMs(): Long = clock.nowMs()

    /** Runs while the screen is started: finishes an overdue timer and plays the feedback once. */
    suspend fun runTicker() {
        while (true) {
            if (service.finishIfOverdue(timerId) != null) {
                val s = settings.settings.first()
                feedback.play(sound = s.soundOn, vibrate = s.vibrateOn)
            }
            delay(TICK_MS)
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            when (state.value?.timer?.state) {
                is RunState.Running -> service.pause(timerId)
                is RunState.Paused -> service.resume(timerId)
                else -> Unit
            }
        }
    }

    fun restart() {
        flip.value += 1
        viewModelScope.launch { service.restart(timerId) }
    }

    fun again() = restart()

    fun addMinute() {
        viewModelScope.launch { service.addMinute(timerId) }
    }

    fun dismiss() {
        viewModelScope.launch { service.dismiss(timerId) }
    }

    fun saveLookAsFavourite() {
        val look = state.value?.timer?.look ?: return
        viewModelScope.launch { favourites.add(look, clock.nowMs()) }
    }

    private companion object {
        const val TICK_MS = 250L
    }
}
