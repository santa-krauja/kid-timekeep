package lv.zarin.timekeep.domain.ports

import kotlinx.coroutines.flow.Flow
import lv.zarin.timekeep.domain.timer.Timer

interface TimerRepository {
    fun observeAll(): Flow<List<Timer>>
    suspend fun get(id: String): Timer?
    suspend fun getAll(): List<Timer>
    suspend fun upsert(timer: Timer)
    suspend fun delete(id: String)
}
