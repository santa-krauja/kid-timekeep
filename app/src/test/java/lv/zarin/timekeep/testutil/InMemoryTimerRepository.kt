package lv.zarin.timekeep.testutil

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import lv.zarin.timekeep.domain.ports.TimerRepository
import lv.zarin.timekeep.domain.timer.Timer

class InMemoryTimerRepository : TimerRepository {
    private val state = MutableStateFlow<Map<String, Timer>>(emptyMap())

    override fun observeAll(): Flow<List<Timer>> =
        state.map { it.values.sortedBy { t -> t.createdAtMs } }

    override suspend fun get(id: String): Timer? = state.value[id]
    override suspend fun getAll(): List<Timer> = state.value.values.sortedBy { it.createdAtMs }
    override suspend fun upsert(timer: Timer) {
        state.value = state.value + (timer.id to timer)
    }

    override suspend fun delete(id: String) {
        state.value = state.value - id
    }
}
