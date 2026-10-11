package lv.zarin.timekeep.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import lv.zarin.timekeep.ui.edit.EditTimerScreen
import lv.zarin.timekeep.ui.home.HomeScreen
import lv.zarin.timekeep.ui.settings.SettingsScreen
import lv.zarin.timekeep.ui.timer.TimerScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    startTimerId: String? = null,
    /** Called once the deep link was navigated, so recreation or recomposition can't navigate again. */
    onStartTimerConsumed: () -> Unit = {},
) {
    val consumed by rememberUpdatedState(onStartTimerConsumed)
    LaunchedEffect(startTimerId) {
        if (startTimerId != null) {
            navController.navigate(TimerRoute(startTimerId)) { launchSingleTop = true }
            consumed()
        }
    }
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenTimer = { navController.navigate(TimerRoute(it)) },
                onNewTimer = { navController.navigate(EditTimerRoute()) },
                onEditPreset = { navController.navigate(EditTimerRoute(presetId = it)) },
                onEditTimer = { navController.navigate(EditTimerRoute(timerId = it)) },
            )
        }
        composable<TimerRoute> { entry ->
            TimerScreen(
                entry.toRoute<TimerRoute>().timerId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(EditTimerRoute(timerId = it)) },
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
                },
            )
        }
        composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
