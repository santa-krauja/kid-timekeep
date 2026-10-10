package lv.zarin.timekeep.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import lv.zarin.timekeep.data.settings.DataStoreSettingsRepository
import lv.zarin.timekeep.data.settings.settingsDataStore
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.ports.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule
    val tmp = TemporaryFolder()

    private fun TestScope.store(file: File = File(tmp.root, "settings.preferences_pb")): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(scope = backgroundScope) { file }

    @Test
    fun defaultsWhenEmpty() = runTest(UnconfinedTestDispatcher()) {
        val repo = DataStoreSettingsRepository(store())
        assertEquals(Settings(), repo.settings.first())
    }

    @Test
    fun updatePersists() = runTest(UnconfinedTestDispatcher()) {
        val file = File(tmp.root, "persist.preferences_pb")
        val repo = DataStoreSettingsRepository(store(file))
        repo.update { it.copy(showNumbers = false, soundOn = false, vibrateOn = false, keepScreenOn = false) }
        val expected = Settings(showNumbers = false, soundOn = false, vibrateOn = false, keepScreenOn = false)
        assertEquals(expected, repo.settings.first())
    }

    @Test
    fun themeModeRoundTrips() = runTest(UnconfinedTestDispatcher()) {
        val ds = store()
        val repo = DataStoreSettingsRepository(ds)
        for (mode in ThemeMode.entries) {
            repo.update { it.copy(themeMode = mode) }
            assertEquals(mode, repo.settings.first().themeMode)
        }
        ds.edit { it[stringPreferencesKey("theme_mode")] = "BOGUS" }
        assertEquals(ThemeMode.SYSTEM, repo.settings.first().themeMode)
    }

    @Test
    fun corruptFileFallsBackToDefaultsAndIsReplaced() = runTest(UnconfinedTestDispatcher()) {
        val file = File(tmp.root, "corrupt.preferences_pb")
        file.writeBytes(byteArrayOf(0x0A, 0x7F, 0x13, 0x00, 0x55, 0x01, 0x02))
        val repo = DataStoreSettingsRepository(settingsDataStore(backgroundScope) { file })
        assertEquals(Settings(), repo.settings.first())
        repo.update { it.copy(soundOn = false) }
        assertEquals(Settings(soundOn = false), repo.settings.first())
    }

    @Test
    fun readIoErrorEmitsDefaults() = runTest(UnconfinedTestDispatcher()) {
        val failing = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("disk") }
            override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
                throw IOException("disk")
        }
        assertEquals(Settings(), DataStoreSettingsRepository(failing).settings.first())
    }
}
