package lv.zarin.timekeep.ui.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.ports.SettingsRepository
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val state = MutableStateFlow(Settings())
    private val repo = object : SettingsRepository {
        override val settings = state
        override suspend fun update(transform: (Settings) -> Settings) {
            state.value = transform(state.value)
        }
    }
    private val applied = mutableListOf<Settings>()

    @Before
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = SettingsViewModel(repo, defaultAlarmUri = DEFAULT, onAlarmSoundChanged = {
        applied += it
    }, workDispatcher = UnconfinedTestDispatcher())

    @Test
    fun pickedSoundIsStoredAndChannelsAreReapplied() = runTest {
        viewModel().setAlarmSound(CUSTOM)
        assertEquals(CUSTOM, repo.settings.first().alarmSoundUri)
        assertEquals(listOf(Settings(alarmSoundUri = CUSTOM)), applied)
    }

    @Test
    fun pickingTheDefaultAlarmStoresNull() = runTest {
        val vm = viewModel()
        vm.setAlarmSound(CUSTOM)
        vm.setAlarmSound(DEFAULT)
        assertEquals(null, repo.settings.first().alarmSoundUri)
        assertEquals(null, applied.last().alarmSoundUri)
    }

    @Test
    fun missingPickResultStoresNull() = runTest {
        val vm = viewModel()
        vm.setAlarmSound(CUSTOM)
        vm.setAlarmSound(null)
        assertEquals(null, repo.settings.first().alarmSoundUri)
    }

    private companion object {
        const val DEFAULT = "content://settings/system/alarm_alert"
        const val CUSTOM = "content://media/internal/audio/media/7"
    }
}
