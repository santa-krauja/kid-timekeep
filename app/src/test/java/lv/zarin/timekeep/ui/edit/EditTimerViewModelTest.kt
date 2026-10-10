package lv.zarin.timekeep.ui.edit

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import lv.zarin.timekeep.domain.TimerService
import lv.zarin.timekeep.domain.control.AllowAllControlPolicy
import lv.zarin.timekeep.domain.control.Control
import lv.zarin.timekeep.domain.control.ControlPolicy
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.MAX_DURATION_MS
import lv.zarin.timekeep.domain.timer.MIN_DURATION_MS
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.InMemoryPresetRepository
import lv.zarin.timekeep.testutil.InMemoryTimerRepository
import lv.zarin.timekeep.testutil.RecordingAlarmScheduler
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InMemoryFavourites : FavouriteLookRepository {
    private val state = MutableStateFlow<List<FavouriteLook>>(emptyList())
    private var next = 0
    override fun observeAll(): Flow<List<FavouriteLook>> = state
    override suspend fun add(look: Look, nowMs: Long): FavouriteLook {
        state.value.firstOrNull { it.look == look }?.let { return it }
        val f = FavouriteLook("f${next++}", look, nowMs, nowMs)
        state.value = state.value + f
        return f
    }
    override suspend fun delete(id: String) {
        state.value = state.value.filterNot { it.id == id }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class EditTimerViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    private val pinned = Look(PictureId.SUN, SandColor.SKY, PictureId.MOON, SandColor.NIGHT)
    private val timers = InMemoryTimerRepository()
    private val presets = InMemoryPresetRepository()
    private val favourites = InMemoryFavourites()
    private val clock = FakeClock(1_000)
    private val lookPicker = LookPicker()
    private val service = TimerService(timers, presets, RecordingAlarmScheduler(), clock, lookPicker)

    private fun TestScope.vm(presetId: String? = null, policy: ControlPolicy = AllowAllControlPolicy): EditTimerViewModel {
        return vm(presetId?.let(EditTarget::Preset) ?: EditTarget.New, policy)
    }

    private fun TestScope.vm(target: EditTarget, policy: ControlPolicy = AllowAllControlPolicy): EditTimerViewModel {
        val vm = EditTimerViewModel(target, presets, favourites, timers, service, lookPicker, clock, policy)
        runCurrent()
        return vm
    }

    private suspend fun runningTimer(): Timer = service.startOneOff("Tea", 60_000L, pinned)

    private suspend fun pausedTimer(): Timer = runningTimer().also {
        clock.advance(20_000L)
        service.pause(it.id)
    }

    @Test
    fun startWithPresetSavesPresetAndStartsRun() = runTest(dispatcher) {
        val vm = vm()
        assertFalse(vm.state.value.saveAsPreset)
        assertEquals(EditTarget.New, vm.state.value.target)
        vm.setSaveAsPreset(true)
        vm.setName("  Reading ")
        vm.setDuration(900_000)
        val look = vm.state.value.look
        val id = vm.start()
        assertNotNull(id)
        val run = timers.get(id!!)!!
        assertEquals("Reading", run.name)
        assertEquals(900_000L, run.durationMs)
        assertEquals(look, run.look)
        val preset = presets.observeAll().first().single()
        assertEquals("Reading", preset.name)
        assertEquals(900_000L, preset.durationMs)
        assertEquals(preset.id, run.presetId)
        assertNull(preset.pinnedLook)
        assertEquals(look, preset.lastLook)
    }

    @Test
    fun startWithoutPresetCreatesOneOff() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("Tidy up")
        val id = vm.start()
        assertNotNull(id)
        assertNull(timers.get(id!!)!!.presetId)
        assertTrue(presets.observeAll().first().isEmpty())
    }

    @Test
    fun keepLookPinsLook() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("Bath")
        vm.setSaveAsPreset(true)
        vm.setLook(pinned)
        vm.setKeepLook(true)
        vm.start()
        assertEquals(pinned, presets.observeAll().first().single().pinnedLook)

        // Editing that preset starts with keepLook on and the pinned look; turning it off unpins.
        val presetId = presets.observeAll().first().single().id
        val edit = vm(presetId)
        assertEquals(EditTarget.Preset(presetId), edit.state.value.target)
        assertTrue(edit.state.value.keepLook)
        assertEquals(pinned, edit.state.value.look)
        assertEquals("Bath", edit.state.value.name)
        edit.setKeepLook(false)
        edit.setName("Bath time")
        assertTrue(edit.savePreset())
        val saved = presets.get(presetId)!!
        assertNull(saved.pinnedLook)
        assertEquals("Bath time", saved.name)
    }

    @Test
    fun editPresetWithoutPinUsesSuggestionAndDeletes() = runTest(dispatcher) {
        presets.upsert(Preset("p", "Reading", 900_000, null, null, 0, 0, 0))
        val vm = vm("p")
        assertFalse(vm.state.value.keepLook)
        assertEquals(900_000L, vm.state.value.durationMs)
        vm.deletePreset()
        assertNull(presets.get("p"))
    }

    @Test
    fun emptyNameShowsError() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("   ")
        assertNull(vm.start())
        assertTrue(vm.state.value.nameError)
        assertTrue(timers.getAll().isEmpty())
        vm.setName("A")
        assertFalse(vm.state.value.nameError)
    }

    @Test
    fun nameLimitedTo40Chars() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("x".repeat(40))
        vm.setName("x".repeat(41))
        assertEquals(40, vm.state.value.name.length)
    }

    @Test
    fun customDurationClampedToRange() = runTest(dispatcher) {
        val vm = vm()
        vm.setDuration(5_000)
        assertEquals(MIN_DURATION_MS, vm.state.value.durationMs)
        vm.setDuration(5 * 3_600_000L)
        assertEquals(MAX_DURATION_MS, vm.state.value.durationMs)
        assertFalse(vm.state.value.durationError)
    }

    @Test
    fun shuffleChangesLook() = runTest(dispatcher) {
        val vm = vm()
        repeat(20) {
            val before = vm.state.value.look
            vm.shuffle()
            assertNotEquals(before, vm.state.value.look)
            assertTrue(LookPicker.isGoodLook(vm.state.value.look))
        }
    }

    @Test
    fun favouriteSaveAndDelete() = runTest(dispatcher) {
        val vm = vm()
        vm.setLook(pinned)
        vm.saveLookAsFavourite()
        runCurrent()
        val fav = vm.state.value.favourites.single()
        assertEquals(pinned, fav.look)
        vm.deleteFavourite(fav.id)
        runCurrent()
        assertTrue(vm.state.value.favourites.isEmpty())
    }

    private val denyEditDelete = ControlPolicy { it != Control.EDIT && it != Control.DELETE }

    @Test
    fun deniedEditDoesNotSavePreset() = runTest(dispatcher) {
        presets.upsert(Preset("p", "Reading", 900_000, null, null, 0, 0, 0))
        val vm = vm("p", denyEditDelete)
        assertFalse(vm.state.value.canEdit)
        assertFalse(vm.state.value.canDelete)
        vm.setName("Changed")
        assertFalse(vm.savePreset())
        assertEquals("Reading", presets.get("p")!!.name)
    }

    @Test
    fun deniedDeleteKeepsPresetAndFavourites() = runTest(dispatcher) {
        presets.upsert(Preset("p", "Reading", 900_000, null, null, 0, 0, 0))
        val fav = favourites.add(pinned, 0)
        val vm = vm("p", denyEditDelete)
        vm.deletePreset()
        assertNotNull(presets.get("p"))
        vm.deleteFavourite(fav.id)
        runCurrent()
        assertEquals(listOf(fav), vm.state.value.favourites)
    }

    @Test
    fun startRunsOnceAtATimeAndReportsSaving() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        val slowPresets = object : PresetRepository by presets {
            override suspend fun upsert(preset: Preset) {
                gate.await()
                presets.upsert(preset)
            }
        }
        val vm = EditTimerViewModel(EditTarget.New, slowPresets, favourites, timers, service, lookPicker, clock)
        runCurrent()
        vm.setName("Reading")
        vm.setSaveAsPreset(true)
        assertFalse(vm.isSaving.value)
        val first = async { vm.start() }
        runCurrent()
        assertTrue(vm.isSaving.value)
        assertNull(vm.start())
        gate.complete(Unit)
        assertNotNull(first.await())
        assertFalse(vm.isSaving.value)
        assertEquals(1, timers.observeAll().first().size)
    }

    @Test
    fun editAndDeleteAllowedByDefault() = runTest(dispatcher) {
        val vm = vm()
        assertTrue(vm.state.value.canEdit)
        assertTrue(vm.state.value.canDelete)
    }

    @Test
    fun editTimerLoadsRunValuesAndLocksDurationWhileRunning() = runTest(dispatcher) {
        val t = runningTimer()
        val vm = vm(EditTarget.RunTimer(t.id))
        val s = vm.state.value
        assertEquals(EditTarget.RunTimer(t.id), s.target)
        assertEquals("Tea", s.name)
        assertEquals(60_000L, s.durationMs)
        assertEquals(pinned, s.look)
        assertFalse(s.durationEditable)
    }

    @Test
    fun editTimerAllowsDurationWhenPaused() = runTest(dispatcher) {
        val vm = vm(EditTarget.RunTimer(pausedTimer().id))
        assertTrue(vm.state.value.durationEditable)
    }

    @Test
    fun saveTimerRenamesAndRelooksRunningTimer() = runTest(dispatcher) {
        val t = runningTimer()
        val vm = vm(EditTarget.RunTimer(t.id))
        vm.setName("  Coffee ")
        val look = Look(PictureId.SUN, SandColor.SKY, PictureId.MOON, SandColor.MINT)
        vm.setLook(look)
        assertTrue(vm.saveTimer())
        val r = timers.get(t.id)!!
        assertEquals("Coffee", r.name)
        assertEquals(look, r.look)
        assertEquals(t.state, r.state)
    }

    @Test
    fun saveTimerChangesDurationWhenPaused() = runTest(dispatcher) {
        val t = pausedTimer()
        val vm = vm(EditTarget.RunTimer(t.id))
        vm.setDuration(120_000L)
        assertTrue(vm.saveTimer())
        assertEquals(120_000L, timers.get(t.id)!!.durationMs)
        assertEquals(RunState.Paused(20_000L), timers.get(t.id)!!.state)
    }

    @Test
    fun saveTimerFlagsDurationNotLongerThanElapsed() = runTest(dispatcher) {
        val t = pausedTimer()
        val vm = vm(EditTarget.RunTimer(t.id))
        vm.setDuration(15_000L)
        assertFalse(vm.saveTimer())
        assertTrue(vm.state.value.durationTooShort)
        assertEquals(60_000L, timers.get(t.id)!!.durationMs)
        vm.setDuration(30_000L)
        assertFalse(vm.state.value.durationTooShort)
    }

    @Test
    fun saveTimerFlagsBlankName() = runTest(dispatcher) {
        val t = runningTimer()
        val vm = vm(EditTarget.RunTimer(t.id))
        vm.setName("  ")
        assertFalse(vm.saveTimer())
        assertTrue(vm.state.value.nameError)
        assertEquals("Tea", timers.get(t.id)!!.name)
    }

    @Test
    fun saveTimerIsDeniedByPolicy() = runTest(dispatcher) {
        val t = runningTimer()
        val vm = vm(EditTarget.RunTimer(t.id), ControlPolicy { it != Control.EDIT })
        assertFalse(vm.state.value.canEdit)
        vm.setName("Other")
        assertFalse(vm.saveTimer())
        assertEquals("Tea", timers.get(t.id)!!.name)
    }
}
