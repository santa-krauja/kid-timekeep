package lv.zarin.timekeep.domain.timer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface RunState {
    @Serializable @SerialName("running")
    data class Running(val sinceMs: Long, val elapsedBeforeMs: Long) : RunState

    @Serializable @SerialName("paused")
    data class Paused(val elapsedMs: Long) : RunState

    @Serializable @SerialName("finished")
    data class Finished(val finishedAtMs: Long) : RunState
}

@Serializable
data class Timer(
    val id: String,
    val name: String,
    val durationMs: Long,
    val look: Look,
    val presetId: String?,
    val state: RunState,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
