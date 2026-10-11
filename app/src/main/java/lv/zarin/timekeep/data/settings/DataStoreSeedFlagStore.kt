package lv.zarin.timekeep.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import lv.zarin.timekeep.domain.ports.SeedFlagStore
import java.io.IOException

class DataStoreSeedFlagStore(private val dataStore: DataStore<Preferences>) : SeedFlagStore {
    override suspend fun isSeeded(): Boolean = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .first()[PRESETS_SEEDED] ?: false

    override suspend fun markSeeded() {
        dataStore.edit { it[PRESETS_SEEDED] = true }
    }

    private companion object {
        val PRESETS_SEEDED = booleanPreferencesKey("presets_seeded")
    }
}
