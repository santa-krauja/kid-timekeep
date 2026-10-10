package lv.zarin.timekeep.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.ports.SettingsRepository
import lv.zarin.timekeep.domain.ports.ThemeMode
import java.io.File
import java.io.IOException

/**
 * The settings DataStore. A corrupt file is replaced with empty preferences (defaults) instead of throwing on
 * every read, which would crash the app at launch until its data is cleared.
 */
fun settingsDataStore(
    scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
    produceFile: () -> File,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
    scope = scope,
    produceFile = produceFile,
)

class DataStoreSettingsRepository(private val dataStore: DataStore<Preferences>) : SettingsRepository {
    /** A read error (IOException) falls back to defaults; anything else is a bug and is rethrown. */
    override val settings: Flow<Settings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it.toSettings() }

    override suspend fun update(transform: (Settings) -> Settings) {
        dataStore.edit { prefs ->
            val new = transform(prefs.toSettings())
            prefs[SHOW_NUMBERS] = new.showNumbers
            prefs[SOUND_ON] = new.soundOn
            prefs[VIBRATE_ON] = new.vibrateOn
            prefs[KEEP_SCREEN_ON] = new.keepScreenOn
            prefs[THEME_MODE] = new.themeMode.name
        }
    }

    private fun Preferences.toSettings(): Settings {
        val d = Settings()
        return Settings(
            showNumbers = this[SHOW_NUMBERS] ?: d.showNumbers,
            soundOn = this[SOUND_ON] ?: d.soundOn,
            vibrateOn = this[VIBRATE_ON] ?: d.vibrateOn,
            keepScreenOn = this[KEEP_SCREEN_ON] ?: d.keepScreenOn,
            themeMode = ThemeMode.entries.firstOrNull { it.name == this[THEME_MODE] } ?: ThemeMode.SYSTEM,
        )
    }

    private companion object {
        val SHOW_NUMBERS = booleanPreferencesKey("show_numbers")
        val SOUND_ON = booleanPreferencesKey("sound_on")
        val VIBRATE_ON = booleanPreferencesKey("vibrate_on")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
