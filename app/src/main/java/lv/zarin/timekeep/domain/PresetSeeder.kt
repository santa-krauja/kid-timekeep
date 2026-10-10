package lv.zarin.timekeep.domain

import kotlinx.coroutines.flow.first
import lv.zarin.timekeep.domain.ports.Clock
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.ports.SeedFlagStore
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.newId

data class StarterPresetNames(val teeth: String, val dressed: String, val reading: String)

class PresetSeeder(
    private val presets: PresetRepository,
    private val flags: SeedFlagStore,
    private val clock: Clock,
    private val ids: () -> String = ::newId,
) {
    suspend fun seedIfNeeded(names: StarterPresetNames) {
        if (flags.isSeeded()) return
        if (presets.observeAll().first().isEmpty()) {
            val now = clock.nowMs()
            listOf(
                names.teeth to 120_000L,
                names.dressed to 600_000L,
                names.reading to 900_000L,
            ).forEachIndexed { index, (name, durationMs) ->
                presets.upsert(Preset(ids(), name, durationMs, null, null, index, now, now))
            }
        }
        flags.markSeeded()
    }
}
