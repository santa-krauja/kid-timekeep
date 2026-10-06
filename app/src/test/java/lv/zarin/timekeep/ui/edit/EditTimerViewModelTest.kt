package lv.zarin.timekeep.ui.edit

import kotlinx.coroutines.Dispatchers
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
import lv.zarin.timekeep.domain.look.LookPicker
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.MAX_DURATION_MS
import lv.zarin.timekeep.domain.timer.MIN_DURATION_MS
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.SandColor
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

    private fun TestScope.vm(presetId: String? = null): EditTimerViewModel {
        val vm = EditTimerViewModel(presetId, presets, favourites, service, lookPicker, clock)
        runCurrent()
        return vm
    }

    @Test
    fun startWithPresetSavesPresetAndStartsRun() = runTest(dispatcher) {
        val vm = vm()
        assertTrue(vm.state.value.saveAsPreset)
        assertFalse(vm.state.value.isEditingPreset)
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
        vm.setSaveAsPreset(false)
        val id = vm.start()
        assertNotNull(id)
        assertNull(timers.get(id!!)!!.presetId)
        assertTrue(presets.observeAll().first().isEmpty())
    }

    @Test
    fun keepLookPinsLook() = runTest(dispatcher) {
        val vm = vm()
        vm.setName("Bath")
        vm.setLook(pinned)
        vm.setKeepLook(true)
        vm.start()
        assertEquals(pinned, presets.observeAll().first().single().pinnedLook)

        // Editing that preset starts with keepLook on and the pinned look; turning it off unpins.
        val presetId = presets.observeAll().first().single().id
        val edit = vm(presetId)
        assertTrue(edit.state.value.isEditingPreset)
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
}
