package lv.zarin.timekeep.data.db

import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.PictureId
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.SandColor
import lv.zarin.timekeep.domain.timer.Timer

private const val RUNNING = "RUNNING"
private const val PAUSED = "PAUSED"
private const val FINISHED = "FINISHED"

private fun pictureOf(name: String): PictureId = PictureId.entries.firstOrNull { it.name == name } ?: PictureId.HEART
private fun sandOf(name: String): SandColor = SandColor.entries.firstOrNull { it.name == name } ?: SandColor.LAVENDER

fun Look.toColumns() = LookColumns(top.name, topSand.name, bottom.name, bottomSand.name)

fun LookColumns.toDomain() = Look(pictureOf(topPicture), sandOf(topSand), pictureOf(bottomPicture), sandOf(bottomSand))

fun Timer.toEntity(): TimerEntity {
    val s = state
    return TimerEntity(
        id = id, name = name, durationMs = durationMs, look = look.toColumns(), presetId = presetId,
        stateType = when (s) {
            is RunState.Running -> RUNNING
            is RunState.Paused -> PAUSED
            is RunState.Finished -> FINISHED
        },
        runningSinceMs = (s as? RunState.Running)?.sinceMs,
        elapsedMs = when (s) {
            is RunState.Running -> s.elapsedBeforeMs
            is RunState.Paused -> s.elapsedMs
            is RunState.Finished -> null
        },
        finishedAtMs = (s as? RunState.Finished)?.finishedAtMs,
        createdAtMs = createdAtMs, updatedAtMs = updatedAtMs,
    )
}

fun TimerEntity.toDomain(): Timer = Timer(
    id = id, name = name, durationMs = durationMs, look = look.toDomain(), presetId = presetId,
    state = when (stateType) {
        RUNNING -> RunState.Running(runningSinceMs ?: createdAtMs, elapsedMs ?: 0L)
        PAUSED -> RunState.Paused(elapsedMs ?: 0L)
        else -> RunState.Finished(finishedAtMs ?: updatedAtMs)
    },
    createdAtMs = createdAtMs, updatedAtMs = updatedAtMs,
)

fun Preset.toEntity() = PresetEntity(
    id, name, durationMs, pinnedLook?.toColumns(), lastLook?.toColumns(), sortOrder, createdAtMs, updatedAtMs,
)

fun PresetEntity.toDomain() = Preset(
    id, name, durationMs, pinnedLook?.toDomain(), lastLook?.toDomain(), sortOrder, createdAtMs, updatedAtMs,
)

fun FavouriteLook.toEntity() = FavouriteLookEntity(id, look.toColumns(), createdAtMs, updatedAtMs)

fun FavouriteLookEntity.toDomain() = FavouriteLook(id, look.toDomain(), createdAtMs, updatedAtMs)
