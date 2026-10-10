package lv.zarin.timekeep.ui.nav

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class TimerRoute(val timerId: String)

@Serializable
data class EditTimerRoute(val presetId: String? = null, val timerId: String? = null)

@Serializable
data object SettingsRoute
