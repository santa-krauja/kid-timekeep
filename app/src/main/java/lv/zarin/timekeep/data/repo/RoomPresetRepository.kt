package lv.zarin.timekeep.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lv.zarin.timekeep.data.db.PresetDao
import lv.zarin.timekeep.data.db.toDomain
import lv.zarin.timekeep.data.db.toEntity
import lv.zarin.timekeep.domain.ports.PresetRepository
import lv.zarin.timekeep.domain.timer.Preset

class RoomPresetRepository(private val dao: PresetDao) : PresetRepository {
    override fun observeAll(): Flow<List<Preset>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun get(id: String): Preset? = dao.get(id)?.toDomain()
    override suspend fun upsert(preset: Preset) = dao.upsert(preset.toEntity())
    override suspend fun delete(id: String) = dao.delete(id)
}
