package lv.zarin.timekeep.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.ports.TimerRepository
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer

data class HomeState(val now: List<Timer> = emptyList(), val presets: List<Preset> = emptyList())

class HomeViewModel(
    timers: TimerRepository,
    presets: PresetRepository,
    private val service: TimerService,
    private val clock: Clock,
) : ViewModel() {

    val state: StateFlow<HomeState> = combine(timers.observeAll(), presets.observeAll()) { ts, ps ->
        HomeState(
            now = ts.sortedWith(compareBy<Timer> { rank(it.state) }.thenBy { it.createdAtMs }),
            presets = ps,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    private val presetRepo = presets
    private val timerRepo = timers

    fun startPreset(id: String) {
        viewModelScope.launch { service.startFromPreset(id) }
    }

    fun togglePause(id: String) {
        viewModelScope.launch {
            val t = timerRepo.get(id) ?: return@launch
            when (t.state) {
                is RunState.Running -> service.pause(id)
                is RunState.Paused -> service.resume(id)
                is RunState.Finished -> Unit
            }
        }
    }

    fun restart(id: String) {
        viewModelScope.launch { service.restart(id) }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch { presetRepo.delete(id) }
    }

    private fun rank(s: RunState) = when (s) {
        is RunState.Running -> 0
        is RunState.Paused -> 1
        is RunState.Finished -> 2
    }
}
