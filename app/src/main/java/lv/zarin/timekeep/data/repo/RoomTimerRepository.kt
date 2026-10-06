package lv.zarin.timekeep.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lv.zarin.timekeep.data.db.TimerDao
import lv.zarin.timekeep.data.db.toDomain
import lv.zarin.timekeep.data.db.toEntity
import lv.zarin.timekeep.domain.ports.TimerRepository
import lv.zarin.timekeep.domain.timer.Timer

class RoomTimerRepository(private val dao: TimerDao) : TimerRepository {
    override fun observeAll(): Flow<List<Timer>> = dao.observeAll().map { list -> list.map { it.toDomain() } }
    override suspend fun get(id: String): Timer? = dao.get(id)?.toDomain()
    override suspend fun getAll(): List<Timer> = dao.getAll().map { it.toDomain() }
    override suspend fun upsert(timer: Timer) = dao.upsert(timer.toEntity())
    override suspend fun delete(id: String) = dao.delete(id)
}
