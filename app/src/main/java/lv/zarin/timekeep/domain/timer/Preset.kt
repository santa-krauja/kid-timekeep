package lv.zarin.timekeep.domain.timer

import kotlinx.serialization.Serializable

/** [pinnedLook] == null means a random look each start. */
@Serializable
data class Preset(
    val id: String,
    val name: String,
    val durationMs: Long,
    val pinnedLook: Look?,
    val lastLook: Look?,
    val sortOrder: Int,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
