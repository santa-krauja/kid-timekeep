package lv.zarin.timekeep.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.ports.SettingsRepository
import lv.zarin.timekeep.domain.ports.ThemeMode

class SettingsViewModel(
    private val repo: SettingsRepository,
    private val defaultAlarmUri: String? = null,
    private val onAlarmSoundChanged: (Settings) -> Unit = {},
    private val workDispatcher: CoroutineDispatcher = Dispatchers.Default
) : ViewModel() {
    val settings: StateFlow<Settings> =
        repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Settings())

    private fun edit(transform: (Settings) -> Settings) {
        viewModelScope.launch { repo.update(transform) }
    }

    fun setShowNumbers(on: Boolean) = edit { it.copy(showNumbers = on) }
    fun setSound(on: Boolean) = edit { it.copy(soundOn = on) }
    fun setVibrate(on: Boolean) = edit { it.copy(vibrateOn = on) }
    fun setKeepScreenOn(on: Boolean) = edit { it.copy(keepScreenOn = on) }
    fun setTheme(mode: ThemeMode) = edit { it.copy(themeMode = mode) }

    fun setAlarmSound(uri: String?) {
        viewModelScope.launch {
            repo.update { it.copy(alarmSoundUri = uri.takeUnless { picked -> picked == defaultAlarmUri }) }
            val saved = repo.settings.first()
            withContext(workDispatcher) { onAlarmSoundChanged(saved) }
        }
    }
}
