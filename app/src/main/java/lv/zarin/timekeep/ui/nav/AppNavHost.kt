package lv.zarin.timekeep.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import lv.zarin.timekeep.ui.edit.EditTimerScreen
import lv.zarin.timekeep.ui.home.HomeScreen
import lv.zarin.timekeep.ui.settings.SettingsScreen
import lv.zarin.timekeep.ui.timer.TimerScreen

@Composable
fun AppNavHost(navController: NavHostController, startTimerId: String? = null) {
    LaunchedEffect(startTimerId) {
        if (startTimerId != null) navController.navigate(TimerRoute(startTimerId))
    }
    NavHost(navController = navController, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenTimer = { navController.navigate(TimerRoute(it)) },
                onNewTimer = { navController.navigate(EditTimerRoute()) },
                onEditPreset = { navController.navigate(EditTimerRoute(it)) },
            )
        }
        composable<TimerRoute> { entry ->
            TimerScreen(entry.toRoute<TimerRoute>().timerId, onBack = { navController.popBackStack() })
        }
        composable<EditTimerRoute> { EditTimerScreen(onBack = { navController.popBackStack() }) }
        composable<SettingsRoute> { SettingsScreen(onBack = { navController.popBackStack() }) }
    }
}
