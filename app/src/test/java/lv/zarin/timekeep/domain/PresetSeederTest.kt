package lv.zarin.timekeep.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import lv.zarin.timekeep.domain.ports.SeedFlagStore
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.testutil.FakeClock
import lv.zarin.timekeep.testutil.InMemoryPresetRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetSeederTest {
    private class FakeFlags(var seeded: Boolean = false) : SeedFlagStore {
        override suspend fun isSeeded() = seeded
        override suspend fun markSeeded() {
            seeded = true
        }
    }

    private val names = StarterPresetNames("Brush teeth", "Get dressed", "Reading")
    private val presets = InMemoryPresetRepository()
    private val flags = FakeFlags()
    private var counter = 0
    private val seeder = PresetSeeder(presets, flags, FakeClock(5_000L)) { "id${counter++}" }

    private suspend fun all() = presets.observeAll().first()

    @Test
    fun freshInstallSeedsThreePresets() = runTest {
        seeder.seedIfNeeded(names)
        val seeded = all()
        assertEquals(listOf("Brush teeth", "Get dressed", "Reading"), seeded.map { it.name })
        assertEquals(listOf(120_000L, 600_000L, 900_000L), seeded.map { it.durationMs })
        assertEquals(listOf(0, 1, 2), seeded.map { it.sortOrder })
        assertEquals(listOf(null, null, null), seeded.map { it.pinnedLook })
        assertEquals(listOf(5_000L, 5_000L, 5_000L), seeded.map { it.createdAtMs })
        assertTrue(flags.seeded)
    }

    @Test
    fun deletingAllPresetsDoesNotReseed() = runTest {
        seeder.seedIfNeeded(names)
        all().forEach { presets.delete(it.id) }
        seeder.seedIfNeeded(names)
        assertEquals(emptyList<Preset>(), all())
    }

    @Test
    fun callingTwiceKeepsThree() = runTest {
        seeder.seedIfNeeded(names)
        seeder.seedIfNeeded(names)
        assertEquals(3, all().size)
    }

    @Test
    fun existingPresetsWithoutFlagAreNotDuplicated() = runTest {
        presets.upsert(Preset("old", "Old", 60_000L, null, null, 0, 0, 0))
        seeder.seedIfNeeded(names)
        assertEquals(listOf("old"), all().map { it.id })
        assertTrue(flags.seeded)
    }
}
