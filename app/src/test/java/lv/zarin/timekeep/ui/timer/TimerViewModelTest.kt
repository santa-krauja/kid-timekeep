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
import lv.zarin.timekeep.domain.timer.TimerPhase
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.InMemoryPresetRepository
import lv.zarin.timekeep.testutil.InMemoryTimerRepository
import lv.zarin.timekeep.testutil.RecordingAlarmScheduler
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import lv.zarin.timekeep.domain.ports.TimerRepository
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
    private val alarms = RecordingAlarmScheduler()
    private val service = TimerService(timers, InMemoryPresetRepository(), alarms, clock, LookPicker())

    private val cleared = mutableListOf<String>()

    private fun vm(settings: Settings = Settings()) = TimerViewModel(
        "t", timers, service, favourites, FakeSettingsRepository(settings), clock,
        lv.zarin.timekeep.domain.control.AllowAllControlPolicy, feedback,
        clearNotification = { cleared += it },
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
    fun restartDismissAndShowingFinishedClearNotification() = runTest(dispatcher) {
        seed(RunState.Finished(60_000))
        val vm = vm()
        vm.restart()
        runCurrent()
        assertEquals(listOf("t"), cleared)
        vm.dismiss()
        runCurrent()
        assertEquals(listOf("t", "t"), cleared)
        vm.onFinishedShown()
        assertEquals(listOf("t", "t", "t"), cleared)
    }

    @Test
    fun addMinuteDisabledAtMaxDuration() = runTest(dispatcher) {
        seed(RunState.Running(0, 0), duration = 14_400_000)
        val vm = vm()
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(
            TimerControls(canPause = true, showAddMinute = true, addMinuteEnabled = false),
            vm.state.value!!.controls,
        )
    }

    @Test
    fun phaseFollowsTimerState() = runTest(dispatcher) {
        seed(RunState.Paused(10_000))
        val vm = vm()
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(TimerPhase.Paused, vm.state.value!!.phase)
        seed(RunState.Finished(60_000))
        runCurrent()
        assertEquals(TimerPhase.Finished, vm.state.value!!.phase)
        assertFalse(vm.state.value!!.keepScreenOn)
    }

    @Test
    fun overdueRunningTimerIsInFinishedPhase() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        clock.now = 61_000
        val vm = vm()
        vm.state.onEach { }.launchIn(backgroundScope)
        runCurrent()
        assertEquals(TimerPhase.Finished, vm.state.value!!.phase)
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

    @Test
    fun tickerDoesNotTouchServiceUntilOverdue() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        clock.now = 10_000
        val vm = vm()
        backgroundScope.launch { vm.runTicker() }
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(0, alarms.scheduleCalls)
        assertTrue(feedback.calls.isEmpty())
    }

    @Test
    fun alreadyFinishedOnOpenPlaysNoFeedback() = runTest(dispatcher) {
        seed(RunState.Finished(60_000))
        clock.now = 120_000
        val vm = vm()
        backgroundScope.launch { vm.runTicker() }
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(feedback.calls.isEmpty())
    }

    @Test
    fun receiverFinishingVisibleTimerStillPlaysFeedbackOnce() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        val vm = vm()
        backgroundScope.launch { vm.runTicker() }
        advanceTimeBy(500)
        runCurrent()
        assertTrue(feedback.calls.isEmpty())
        // The alarm receiver finishes the timer before the ticker's own overdue check.
        clock.now = 60_000
        service.finishIfOverdue("t")
        advanceTimeBy(100)
        runCurrent()
        assertEquals(1, feedback.calls.size)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(1, feedback.calls.size)
    }

    @Test
    fun finishedWhileStoppedDoesNotReplayOnReturn() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        val vm = vm()
        val first = backgroundScope.launch { vm.runTicker() }
        advanceTimeBy(500)
        runCurrent()
        first.cancel() // screen stopped
        advanceTimeBy(10_000) // WhileSubscribed(5s) lapses; the cached value stays Running
        clock.now = 60_000
        service.finishIfOverdue("t") // receiver finished it and notified
        backgroundScope.launch { vm.runTicker() } // screen started again
        advanceTimeBy(2_000)
        runCurrent()
        assertTrue(feedback.calls.isEmpty())
    }

    @Test
    fun finishedElsewhereFirstPlaysNoFeedback() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        clock.now = 61_000
        service.finishIfOverdue("t") // e.g. the alarm receiver got there first
        val vm = vm()
        backgroundScope.launch { vm.runTicker() }
        advanceTimeBy(1_000)
        runCurrent()
        assertTrue(feedback.calls.isEmpty())
    }

    @Test
    fun finishBeforeFirstLoadStillPlaysFeedback() = runTest(dispatcher) {
        seed(RunState.Running(0, 0))
        // Room's first emission arrives late: until then `loaded` only holds its initial null.
        val firstLoad = CompletableDeferred<Unit>()
        val slowTimers = object : TimerRepository by timers {
            override fun observeAll(): Flow<List<Timer>> = flow {
                firstLoad.await()
                emitAll(timers.observeAll())
            }
        }
        val vm = TimerViewModel(
            "t", slowTimers, service, favourites, FakeSettingsRepository(Settings()), clock,
            lv.zarin.timekeep.domain.control.AllowAllControlPolicy, feedback,
        )
        backgroundScope.launch { vm.runTicker() }
        runCurrent()
        clock.now = 60_000
        service.finishIfOverdue("t") // the alarm receiver finishes it; the screen is visible, so no notification
        firstLoad.complete(Unit)
        advanceTimeBy(100)
        runCurrent()
        assertEquals(1, feedback.calls.size)
    }
}
