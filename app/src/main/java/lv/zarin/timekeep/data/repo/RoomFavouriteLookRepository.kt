package lv.zarin.timekeep.data.repo

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import lv.zarin.timekeep.data.db.FavouriteLookDao
import lv.zarin.timekeep.data.db.toColumns
import lv.zarin.timekeep.data.db.toDomain
import lv.zarin.timekeep.data.db.toEntity
import lv.zarin.timekeep.domain.ports.FavouriteLookRepository
import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.newId

class RoomFavouriteLookRepository(private val dao: FavouriteLookDao) : FavouriteLookRepository {
    override fun observeAll(): Flow<List<FavouriteLook>> = dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun add(look: Look, nowMs: Long): FavouriteLook {
        val c = look.toColumns()
        dao.find(c.topPicture, c.topSand, c.bottomPicture, c.bottomSand)?.let { return it.toDomain() }
        val fav = FavouriteLook(newId(), look, nowMs, nowMs)
        dao.upsert(fav.toEntity())
        return fav
    }

    override suspend fun delete(id: String) = dao.delete(id)
}
