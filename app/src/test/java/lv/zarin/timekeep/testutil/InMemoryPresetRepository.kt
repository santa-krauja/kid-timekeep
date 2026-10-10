package lv.zarin.timekeep.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.timer.Preset

class InMemoryPresetRepository : PresetRepository {
    private val state = MutableStateFlow<Map<String, Preset>>(emptyMap())

    override fun observeAll(): Flow<List<Preset>> =
        state.map { it.values.sortedWith(compareBy({ p -> p.sortOrder }, { p -> p.createdAtMs })) }

    override suspend fun get(id: String): Preset? = state.value[id]
    override suspend fun upsert(preset: Preset) {
        state.value = state.value + (preset.id to preset)
    }

    override suspend fun delete(id: String) {
        state.value = state.value - id
    }
}
