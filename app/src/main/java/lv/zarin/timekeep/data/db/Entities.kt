package lv.zarin.timekeep.data.db

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.SandColor

enum class RunStateType { RUNNING, PAUSED, FINISHED }

data class LookColumns(
    val topPicture: PictureId,
    val topSand: SandColor,
    val bottomPicture: PictureId,
    val bottomSand: SandColor,
)

@Entity(tableName = "timers")
data class TimerEntity(
    @PrimaryKey val id: String,
    val name: String,
    val durationMs: Long,
    @Embedded(prefix = "look_") val look: LookColumns,
    val presetId: String?,
    val stateType: RunStateType,
    val runningSinceMs: Long?,
    val elapsedMs: Long?,
    val finishedAtMs: Long?,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(tableName = "presets")
data class PresetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val durationMs: Long,
    @Embedded(prefix = "pinned_") val pinnedLook: LookColumns?,
    @Embedded(prefix = "last_") val lastLook: LookColumns?,
    val sortOrder: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)

@Entity(tableName = "favourite_looks")
data class FavouriteLookEntity(
    @PrimaryKey val id: String,
    @Embedded(prefix = "look_") val look: LookColumns,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
