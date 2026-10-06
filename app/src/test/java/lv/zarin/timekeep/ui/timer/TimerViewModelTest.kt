package lv.zarin.timekeep.ui.timer

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.ports.Settings
import lv.zarin.timekeep.domain.ports.SettingsRepository
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.InMemoryPresetRepository
import lv.zarin.timekeep.testutil.InMemoryTimerRepository
import lv.zarin.timekeep.testutil.RecordingAlarmScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FakeSettingsRepository(initial: Settings = Settings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    override val settings: Flow<Settings> = state
    override suspend fun update(transform: (Settings) -> Settings) {
        state.value = transform(state.value)
    }
}

class RecordingFeedback : TimeUpFeedback {
    val calls = mutableListOf<Pair<Boolean, Boolean>>()
    override fun play(sound: Boolean, vibrate: Boolean) {
        calls += sound to vibrate
    }
}

class RecordingFavourites : FavouriteLookRepository {
    val added = mutableListOf<Look>()
    override fun observeAll(): Flow<List<FavouriteLook>> = MutableStateFlow(emptyList())
    override suspend fun add(look: Look, nowMs: Long): FavouriteLook {
        added += look
        return FavouriteLook("f", look, nowMs, nowMs)
    }
    override suspend fun delete(id: String) = Unit
}

@OptIn(ExperimentalCoroutinesApi::class)
class TimerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val look = Look(PictureId.HEART, SandColor.SKY, PictureId.STAR, SandColor.MINT)
    private val timers = InMemoryTimerRepository()
    private val clock = FakeClock(0)
    private val feedback = RecordingFeedback()
    private val favourites = RecordingFavourites()
    private val service = TimerService(timers, InMemoryPresetRepository(), RecordingAlarmScheduler(), clock, LookPicker())

    private fun vm(settings: Settings = Settings()) = TimerViewModel(
        "t", timers, service, favourites, FakeSettingsRepository(settings), clock,
        lv.zarin.timekeep.domain.control.AllowAllControlPolicy, feedback,
    )

    private suspend fun seed(state: RunState, duration: Long = 60_000) =
        timers.upsert(Timer("t", "Teeth", duration, look, null, state, 0, 0))

    @Test
    fun tickerFinishesOverdueOnce() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        clock.now = 61_000
        val vm = vm()
        backgroundScope.launch { vm.runTicker() }
        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(listOf(true to true), feedback.calls)
        assertTrue(timers.get("t")!!.state is RunState.Finished)
    }

    @Test
    fun tickerRespectsSoundAndVibrateSettings() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        clock.now = 61_000
        val vm = vm(Settings(soundOn = false, vibrateOn = true))
        backgroundScope.launch { vm.runTicker() }
        advanceTimeBy(500)
        runCurrent()
        assertEquals(listOf(false to true), feedback.calls)
    }

    @Test
    fun restartIncrementsFlipTriggerAndKeepsLook() = runTest(dispatcher) {
        seed(RunState.Finished(60_000))
        val vm = vm()
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(0, vm.state.value!!.flipTrigger)
        vm.restart()
        runCurrent()
        val s = vm.state.value!!
        assertEquals(1, s.flipTrigger)
        assertEquals(look, s.timer.look)
        assertTrue(s.timer.state is RunState.Running)
    }

    @Test
    fun addMinuteDisabledAtMaxDuration() = runTest(dispatcher) {
        seed(RunState.Running(0, 0), duration = 14_400_000)
        val vm = vm()
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertFalse(vm.state.value!!.canAddMinute)
    }

    @Test
    fun missingTimerIsReported() = runTest(dispatcher) {
        val vm = vm()
        vm.state.onEach { }.launchIn(backgroundScope)
        vm.missing.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertTrue(vm.missing.value)
        assertEquals(null, vm.state.value)
    }

    @Test
    fun saveLookAddsFavourite() = runTest(dispatcher) {
        seed(RunState.Finished(60_000))
        val vm = vm()
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        vm.saveLookAsFavourite()
        runCurrent()
        assertEquals(listOf(look), favourites.added)
    }
}
