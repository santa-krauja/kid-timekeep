package lv.zarin.timekeep.domain.ports

import kotlinx.coroutines.flow.Flow
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look

interface FavouriteLookRepository {
    fun observeAll(): Flow<List<FavouriteLook>>

    suspend fun add(look: Look, nowMs: Long): FavouriteLook
    suspend fun delete(id: String)
}
