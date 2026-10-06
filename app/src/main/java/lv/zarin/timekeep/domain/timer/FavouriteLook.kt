package lv.zarin.timekeep.domain.timer

import kotlinx.serialization.Serializable

@Serializable
data class FavouriteLook(
    val id: String,
    val look: Look,
    val createdAtMs: Long,
    val updatedAtMs: Long,
)
