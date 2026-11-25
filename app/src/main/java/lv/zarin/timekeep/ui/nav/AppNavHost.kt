package lv.zarin.timekeep.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import lv.zarin.timekeep.ui.common.LocalLockScreenAccess
import lv.zarin.timekeep.ui.edit.EditTimerScreen
import lv.zarin.timekeep.ui.home.HomeScreen
import lv.zarin.timekeep.ui.settings.SettingsScreen
import lv.zarin.timekeep.ui.timer.TimerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startTimerId: String? = null,
    onStartTimerConsumed: () -> Unit = {},
    initialTimerId: String? = null
) {
    val consumed by rememberUpdatedState(onStartTimerConsumed)
    val lockScreen = LocalLockScreenAccess.current
    LaunchedEffect(startTimerId) {
        if (startTimerId != null) {
            if (!navController.isShowingTimer(startTimerId)) {
                navController.navigate(TimerRoute(startTimerId)) {
                    launchSingleTop = true
                    popUpTo<TimerRoute> { inclusive = true }
                }
            }
            consumed()
        }
    }
    val startDestination: Any = remember { initialTimerId?.let(::TimerRoute) ?: HomeRoute }
    NavHost(navController = navController, startDestination = startDestination) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenTimer = { navController.navigate(TimerRoute(it)) },
                onNewTimer = { navController.navigate(EditTimerRoute()) },
                onEditPreset = { navController.navigate(EditTimerRoute(presetId = it)) },
                onEditTimer = { navController.navigate(EditTimerRoute(timerId = it)) }
            )
        }
        composable<TimerRoute> { entry ->
            val timerId = entry.toRoute<TimerRoute>().timerId
            TimerScreen(
                timerId,
                onBack = {
                    lockScreen.release(timerId)
                    if (navController.previousBackStackEntry == null) {
                        navController.navigate(HomeRoute) { popUpTo<TimerRoute> { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
                onEdit = { navController.navigate(EditTimerRoute(timerId = it)) }
            )
        }
        composable<EditTimerRoute> { entry ->
            val route = entry.toRoute<EditTimerRoute>()
            EditTimerScreen(
                presetId = route.presetId,
                timerId = route.timerId,
                onBack = { navController.popBackStack() },
                onStarted = { id ->
                    navController.navigate(TimerRoute(id)) {
                        popUpTo<EditTimerRoute> { inclusive = true }
                    }
                }
            )
        }
        composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}

private fun NavHostController.isShowingTimer(timerId: String): Boolean {
    val current = currentBackStackEntry ?: return false
    return current.destination.hasRoute<TimerRoute>() && current.toRoute<TimerRoute>().timerId == timerId
}
