package lv.zarin.timekeep.data.db

import lv.zarin.timekeep.domain.timer.FavouriteLook
import lv.zarin.timekeep.domain.timer.Look
import lv.zarin.timekeep.domain.timer.Preset
import lv.zarin.timekeep.domain.timer.RunState
import lv.zarin.timekeep.domain.timer.Timer

fun Look.toColumns() = LookColumns(top, topSand, bottom, bottomSand)

fun LookColumns.toDomain() = Look(topPicture, topSand, bottomPicture, bottomSand)

fun Timer.toEntity(): TimerEntity {
    val s = state
    return TimerEntity(
        id = id, name = name, durationMs = durationMs, look = look.toColumns(), presetId = presetId,
        stateType = when (s) {
            is RunState.Running -> RunStateType.RUNNING
            is RunState.Paused -> RunStateType.PAUSED
            is RunState.Finished -> RunStateType.FINISHED
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
        RunStateType.RUNNING -> RunState.Running(runningSinceMs ?: createdAtMs, elapsedMs ?: 0L)
        RunStateType.PAUSED -> RunState.Paused(elapsedMs ?: 0L)
        RunStateType.FINISHED -> RunState.Finished(finishedAtMs ?: updatedAtMs)
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
