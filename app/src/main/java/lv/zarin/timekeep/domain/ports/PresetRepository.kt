package lv.zarin.timekeep.domain.ports

import kotlinx.coroutines.flow.Flow
import lv.zarin.timekeep.domain.timer.Preset

/** Presets are ordered by sortOrder, then createdAtMs. */
interface PresetRepository {
    fun observeAll(): Flow<List<Preset>>
    suspend fun get(id: String): Preset?
    suspend fun upsert(preset: Preset)
    suspend fun delete(id: String)
}
