package lv.zarin.timekeep.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.ports.TimerRepository
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer

data class HomeState(
    val now: List<Timer> = emptyList(),
    val presets: List<Preset> = emptyList(),
    val canPause: Boolean = true,
    val canEdit: Boolean = true,
    val canDelete: Boolean = true,
)

class HomeViewModel(
    private val timers: TimerRepository,
    private val presets: PresetRepository,
    private val service: TimerService,
    @Suppress("unused") private val clock: Clock,
    private val policy: ControlPolicy = AllowAllControlPolicy,
    private val clearNotification: (String) -> Unit = {},
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(timers.observeAll(), presets.observeAll()) { ts, ps ->
        HomeState(
            now = ts.sortedWith(compareBy<Timer> { rank(it.state) }.thenBy { it.createdAtMs }),
            presets = ps,
            canPause = policy.isAllowed(Control.PAUSE),
            canEdit = policy.isAllowed(Control.EDIT),
            canDelete = policy.isAllowed(Control.DELETE),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val _startedId = MutableStateFlow<String?>(null)

    /** Id of the timer just started from a preset, until [consumeStartedId] (the tablet layout selects it, the phone layout opens it). */
    val startedId: StateFlow<String?> = _startedId

    fun startPreset(id: String) {
        viewModelScope.launch { _startedId.value = service.startFromPreset(id).id }
    }

    fun consumeStartedId() {
        _startedId.value = null
    }

    fun togglePause(id: String) {
        viewModelScope.launch {
            val t = timers.get(id) ?: return@launch
            when (t.state) {
                is RunState.Running -> service.pause(id)
                is RunState.Paused -> service.resume(id)
                is RunState.Finished -> Unit
            }
        }
    }

    fun restart(id: String) {
        clearNotification(id)
        viewModelScope.launch { service.restart(id) }
    }

    fun deletePreset(id: String) {
        if (!policy.isAllowed(Control.DELETE)) return
        viewModelScope.launch { presets.delete(id) }
    }

    private fun rank(s: RunState) = when (s) {
        is RunState.Running -> 0
        is RunState.Paused -> 1
        is RunState.Finished -> 2
    }
}
