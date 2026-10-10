package lv.zarin.timekeep.domain.ports

import kotlinx.coroutines.flow.Flow

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class Settings(
    val showNumbers: Boolean = true,
    val soundOn: Boolean = true,
    val vibrateOn: Boolean = true,
    val keepScreenOn: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
)

interface SettingsRepository {
    val settings: Flow<Settings>
    suspend fun update(transform: (Settings) -> Settings)
}
