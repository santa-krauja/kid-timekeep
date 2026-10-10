@file:OptIn(ExperimentalUuidApi::class)

package lv.zarin.timekeep.domain.timer

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

const val MAX_NAME_LENGTH = 40
const val MIN_DURATION_MS = 10_000L
const val MAX_DURATION_MS = 14_400_000L

val QUICK_DURATIONS_MS: List<Long> =
    listOf(30_000L) + listOf(1, 2, 5, 10, 15, 20, 30, 60).map { it * 60_000L }

fun isValidDuration(ms: Long): Boolean = ms in MIN_DURATION_MS..MAX_DURATION_MS

fun newId(): String = Uuid.random().toString()
