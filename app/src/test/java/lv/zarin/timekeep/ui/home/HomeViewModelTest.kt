package lv.zarin.timekeep.ui.home

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.InMemoryPresetRepository
import lv.zarin.timekeep.testutil.InMemoryTimerRepository
import lv.zarin.timekeep.testutil.RecordingAlarmScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)

    private fun timer(id: String, state: RunState, created: Long) =
        Timer(id, id, 60_000, look, null, state, created, created)

    @Test
    fun nowSortedRunningPausedFinished() = runTest {
        val timers = InMemoryTimerRepository()
        val clock = FakeClock(1000)
        val presets = InMemoryPresetRepository()
        val service = TimerService(timers, presets, RecordingAlarmScheduler(), clock, LookPicker())
        timers.upsert(timer("fin", RunState.Finished(5), 1))
        timers.upsert(timer("pau", RunState.Paused(10), 2))
        timers.upsert(timer("run", RunState.Running(0, 0), 3))
        val vm = HomeViewModel(timers, presets, service, clock)
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(listOf("run", "pau", "fin"), vm.state.value.now.map { it.id })
    }

    @Test
    fun canPauseFollowsPolicy() = runTest {
        val timers = InMemoryTimerRepository()
        val presets = InMemoryPresetRepository()
        val clock = FakeClock(0)
        val service = TimerService(timers, presets, RecordingAlarmScheduler(), clock, LookPicker())
        val deny = ControlPolicy { it != Control.PAUSE }
        val vm = HomeViewModel(timers, presets, service, clock, deny)
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(false, vm.state.value.canPause)
    }

    @Test
    fun editAndDeleteFollowPolicy() = runTest {
        val timers = InMemoryTimerRepository()
        val presets = InMemoryPresetRepository()
        val clock = FakeClock(0)
        val service = TimerService(timers, presets, RecordingAlarmScheduler(), clock, LookPicker())
        presets.upsert(Preset("p", "Reading", 900_000, null, null, 0, 0, 0))
        val deny = ControlPolicy { it != Control.EDIT && it != Control.DELETE }
        val vm = HomeViewModel(timers, presets, service, clock, deny)
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(false, vm.state.value.canEdit)
        assertEquals(false, vm.state.value.canDelete)
        vm.deletePreset("p")
        runCurrent()
        assertNotNull(presets.get("p"))
    }

    @Test
    fun editAndDeleteAllowedByDefault() = runTest {
        val timers = InMemoryTimerRepository()
        val presets = InMemoryPresetRepository()
        val clock = FakeClock(0)
        val service = TimerService(timers, presets, RecordingAlarmScheduler(), clock, LookPicker())
        presets.upsert(Preset("p", "Reading", 900_000, null, null, 0, 0, 0))
        val vm = HomeViewModel(timers, presets, service, clock)
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(true, vm.state.value.canEdit)
        assertEquals(true, vm.state.value.canDelete)
        vm.deletePreset("p")
        runCurrent()
        assertNull(presets.get("p"))
    }

    @Test
    fun restartClearsNotification() = runTest {
        val timers = InMemoryTimerRepository()
        val clock = FakeClock(0)
        val service = TimerService(timers, InMemoryPresetRepository(), RecordingAlarmScheduler(), clock, LookPicker())
        timers.upsert(timer("fin", RunState.Finished(5), 1))
        val cleared = mutableListOf<String>()
        val vm = HomeViewModel(timers, InMemoryPresetRepository(), service, clock, clearNotification = { cleared += it })
        vm.restart("fin")
        runCurrent()
        assertEquals(listOf("fin"), cleared)
    }
}
