package lv.zarin.timekeep.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor

@Dao
interface TimerDao {
    @Query("SELECT * FROM timers ORDER BY createdAtMs, id")
    fun observeAll(): Flow<List<TimerEntity>>

    @Query("SELECT * FROM timers ORDER BY createdAtMs, id")
    suspend fun getAll(): List<TimerEntity>

    @Query("SELECT * FROM timers WHERE id = :id")
    suspend fun get(id: String): TimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: TimerEntity)

    @Query("DELETE FROM timers WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM presets ORDER BY sortOrder, createdAtMs, id")
    fun observeAll(): Flow<List<PresetEntity>>

    @Query("SELECT * FROM presets WHERE id = :id")
    suspend fun get(id: String): PresetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PresetEntity)

    @Query("DELETE FROM presets WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface FavouriteLookDao {
    @Query("SELECT * FROM favourite_looks ORDER BY createdAtMs DESC, id")
    fun observeAll(): Flow<List<FavouriteLookEntity>>

    @Query(
        "SELECT * FROM favourite_looks WHERE look_topPicture = :topPicture AND look_topSand = :topSand " +
            "AND look_bottomPicture = :bottomPicture AND look_bottomSand = :bottomSand LIMIT 1",
    )
    suspend fun find(
        topPicture: PictureId,
        topSand: SandColor,
        bottomPicture: PictureId,
        bottomSand: SandColor,
    ): FavouriteLookEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: FavouriteLookEntity)

    @Query("DELETE FROM favourite_looks WHERE id = :id")
    suspend fun delete(id: String)
}
