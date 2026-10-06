package lv.zarin.timekeep.domain.ports

import kotlinx.coroutines.flow.Flow
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look

interface FavouriteLookRepository {
    fun observeAll(): Flow<List<FavouriteLook>>

    /** Returns the existing row if an identical look is already saved. */
    suspend fun add(look: Look, nowMs: Long): FavouriteLook
    suspend fun delete(id: String)
}
